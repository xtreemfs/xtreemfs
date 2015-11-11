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
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.FileOperationCallback;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.StorageStage.TruncateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.truncateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class TruncateOperation extends OSDOperation {

    final String sharedSecret;

    final ServiceUUID localUUID;

    public TruncateOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_TRUNCATE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final truncateRequest args = (truncateRequest) rq.getRequestArgs();

        if (args.getNewFileSize() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "new_file_size for truncate must be >= 0");
            return;
        }

        if (!rq.getLocationList().getLocalReplica().getHeadOsd().equals(localUUID)) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "truncate must be executed at the head OSD (first OSD in replica)");
            return;
        }

        if (rq.getLocationList().getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)) {
            // file is read only
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EPERM, "Cannot write on read-only files.");
            return;
        }

        // TODO(jdillmann): Use centralized method to check if a lease is required.
        if (rq.getLocationList().getNumReplicas() > 1
                && ReplicaUpdatePolicies.isRwReplicated(rq.getLocationList().getReplicaUpdatePolicy())) {
            rwReplicatedTruncate(rq, args);
        } else {

            master.getStorageStage().truncate(args.getFileId(), args.getNewFileSize(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(),
                rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
                null, false, rq,
                new TruncateCallback() {

                    @Override
                    public void truncateComplete(OSDWriteResponse result, ErrorResponse error) {
                        step2(rq, args, result, error);
                    }
            });
        }
    }

    public void rwReplicatedTruncate(final OSDRequest rq,
            final truncateRequest args) {
        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(), 0, 0, RWReplicationStage.Operation.TRUNCATE, rq, new FileOperationCallback() {

            @Override
            public void success(final long newObjectVersion) {
                final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

                //FIXME: ignore canExecOperation for now...
               master.getStorageStage().truncate(args.getFileId(), args.getNewFileSize(),
                    rq.getLocationList().getLocalReplica().getStripingPolicy(),
                    rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
                    newObjectVersion, true, rq, new TruncateCallback() {

                    @Override
                    public void truncateComplete(OSDWriteResponse result, ErrorResponse error) {
                        replicateTruncate(rq, newObjectVersion, args, result, error);
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
        });
    }

    public void replicateTruncate(final OSDRequest rq,
            final long newObjVersion,
            final truncateRequest args,
            final OSDWriteResponse result, ErrorResponse error) {
            if (error != null)
                step2(rq, args, result, error);
            else {
                master.getRWReplicationStage().replicateTruncate(args.getFileCredentials(),
                    rq.getLocationList(),args.getNewFileSize(), newObjVersion,
                    new FileOperationCallback() {

                    @Override
                    public void success(long newObjectVersion) {
                        step2(rq, args, result, null);
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
    }

    public void step2(final OSDRequest rq,
            final truncateRequest args,
            OSDWriteResponse result, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
        } else {
            //check for striping
            if (rq.getLocationList().getLocalReplica().isStriped()) {
                //disseminate internal truncate to all other OSDs
                disseminateTruncates(rq, args, result);
            } else {
                //non-striped
                sendResponse(rq, result);
            }

        }
    }

    private void disseminateTruncates(final OSDRequest rq,
            final truncateRequest args,
            final OSDWriteResponse result) {
        try {
            final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
            final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
            int cnt = 0;
            for (ServiceUUID osd : osds) {
                if (!osd.equals(localUUID)) {
                    gmaxRPCs[cnt++] = master.getOSDClient().xtreemfs_internal_truncate(osd.getAddress(),
                            RPCAuthentication.authNone,RPCAuthentication.userService,
                            args.getFileCredentials(), args.getFileId(), args.getNewFileSize());
                }

            }
            this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                @Override
                public void responsesAvailable() {
                    analyzeTruncateResponses(rq, result, gmaxRPCs);
                }
            });
        } catch (IOException ex) {
            rq.sendInternalServerError(ex);
        } catch (Throwable ex) {
            rq.sendInternalServerError(ex);
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            return;

        }


    }

    private void analyzeTruncateResponses(OSDRequest rq, OSDWriteResponse result, RPCResponse[] gmaxRPCs) {
        //analyze results
        try {
            for (int i = 0; i <
                    gmaxRPCs.length; i++) {
                gmaxRPCs[i].get();
            }

            sendResponse(rq, result);
        } catch (IOException ex) {
            rq.sendInternalServerError(ex);
        } catch (Throwable ex) {
            rq.sendInternalServerError(ex);
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        } finally {
            for (RPCResponse r : gmaxRPCs) {
                r.freeBuffers();
            }
        }

    }

    public void sendResponse(OSDRequest rq, OSDWriteResponse result) {
        rq.sendSuccess(result,null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            truncateRequest  rpcrq = (truncateRequest)rq.getRequestArgs();
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
