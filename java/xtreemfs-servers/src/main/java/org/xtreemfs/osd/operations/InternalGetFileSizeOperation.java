/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.StorageStage.GetFileSizeCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalGmax;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_file_sizeResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class InternalGetFileSizeOperation extends OSDOperation {

    final String      sharedSecret;

    final ServiceUUID localUUID;

    public InternalGetFileSizeOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_INTERNAL_GET_FILE_SIZE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_internal_get_file_sizeRequest args = (xtreemfs_internal_get_file_sizeRequest) rq
                .getRequestArgs();

        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        final String replUpdatePolicy = args.getFileCredentials().getXlocs().getReplicaUpdatePolicy();
        
        if (ReplicaUpdatePolicies.isNONE(replUpdatePolicy) || ReplicaUpdatePolicies.isRO(replUpdatePolicy)) {
            master.getStorageStage().getFilesize(args.getFileId(), sp, rq.getCapability().getSnapTimestamp(), rq,
                    new GetFileSizeCallback() {

                        @Override
                        public void getFileSizeComplete(long fileSize, ErrorResponse error) {
                            step2(rq, args, fileSize, error);
                        }
                    });
        } else if (ReplicaUpdatePolicies.isRW(replUpdatePolicy)) {
            rwReplicatedGetFS(rq, args);
        } else {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL,
                    "Invalid ReplicaUpdatePolicy: " + replUpdatePolicy);
        }

    }

    public void step2(final OSDRequest rq, xtreemfs_internal_get_file_sizeRequest args, long localFS,
            ErrorResponse error) {
        if (error != null) {
            rq.sendError(error);
        } else {
            if (rq.getLocationList().getLocalReplica().isStriped()) {
                // striped read
                stripedGetFS(rq, args, localFS);
            } else {
                // non-striped case
                sendResponse(rq, localFS);
            }
        }
    }

    private void stripedGetFS(final OSDRequest rq, final xtreemfs_internal_get_file_sizeRequest args, final long localFS) {
        try {
            final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
            final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
            int cnt = 0;
            for (ServiceUUID osd : osds) {
                if (!osd.equals(localUUID)) {
                    gmaxRPCs[cnt++] = master.getOSDClient().xtreemfs_internal_get_gmax(osd.getAddress(),
                            RPCAuthentication.authNone, RPCAuthentication.userService, args.getFileCredentials(),
                            args.getFileId());
                }
            }
            this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                @Override
                public void responsesAvailable() {
                    stripedReadAnalyzeGmax(rq, args, localFS, gmaxRPCs);
                }
            });
        } catch (IOException ex) {
            rq.sendInternalServerError(ex);
            return;
        }

    }

    private void stripedReadAnalyzeGmax(final OSDRequest rq, final xtreemfs_internal_get_file_sizeRequest args,
            final long localFS, RPCResponse[] gmaxRPCs) {
        long maxFS = localFS;

        try {
            for (int i = 0; i < gmaxRPCs.length; i++) {
                InternalGmax gmax = (InternalGmax) gmaxRPCs[i].get();
                if (gmax.getFileSize() > maxFS) {
                    // found new max
                    maxFS = gmax.getFileSize();
                }
            }
            sendResponse(rq, maxFS);
        } catch (Exception ex) {
            rq.sendInternalServerError(ex);
        } finally {
            for (RPCResponse r : gmaxRPCs)
                r.freeBuffers();
        }
    }

    private void rwReplicatedGetFS(final OSDRequest rq, final xtreemfs_internal_get_file_sizeRequest args) {
        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(), 0, 0,
                RWReplicationStage.Operation.READ,
                new RWReplicationStage.RWReplicationCallback() {

                    @Override
                    public void success(long newObjectVersion) {
                        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

                        final long snapVerTS = rq.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP ? rq
                                .getCapability().getSnapTimestamp() : 0;

                        master.getStorageStage().getFilesize(args.getFileId(), sp, snapVerTS, rq,
                                new GetFileSizeCallback() {
                                    @Override
                                    public void getFileSizeComplete(long fileSize, ErrorResponse error) {
                                        if (error != null) {
                                            rq.sendError(error);
                                        } else {
                                            sendResponse(rq, fileSize);
                                        }
                                    }
                                });
                    }

                    @Override
                    public void redirect(String redirectTo) {
                        rq.getRPCRequest().sendRedirect(redirectTo);
                    }

                    @Override
                    public void failed(ErrorResponse err) {
                        rq.sendError(err);
                    }
                }, rq);
    }

    public void sendResponse(OSDRequest rq, long fileSize) {
        xtreemfs_internal_get_file_sizeResponse response = xtreemfs_internal_get_file_sizeResponse.newBuilder()
                .setFileSize(fileSize).build();
        rq.sendSuccess(response, null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_internal_get_file_sizeRequest rpcrq = (xtreemfs_internal_get_file_sizeRequest) rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileId());
            rq.setCapability(new Capability(rpcrq.getFileCredentials().getXcap(), sharedSecret));
            rq.setLocationList(new XLocations(rpcrq.getFileCredentials().getXlocs(), localUUID));

            return null;
        } catch (InvalidXLocationsException ex) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, ex.toString());
        } catch (Throwable ex) {
            return ErrorUtils.getInternalServerError(ex);
        }
    }

    @Override
    public boolean requiresCapability() {
        return true;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}