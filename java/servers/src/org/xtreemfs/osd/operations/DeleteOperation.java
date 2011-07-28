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
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.DeletionStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.PreprocStage.DeleteOnCloseCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.unlink_osd_Request;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class DeleteOperation extends OSDOperation {


    final String sharedSecret;

    final ServiceUUID localUUID;

    public DeleteOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_UNLINK;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final unlink_osd_Request args = (unlink_osd_Request) rq.getRequestArgs();
        master.getPreprocStage().checkDeleteOnClose(args.getFileId(), new DeleteOnCloseCallback() {

            @Override
            public void deleteOnCloseResult(boolean isDeleteOnClose, ErrorResponse error) {
                step2(rq, isDeleteOnClose, args, error);
            }
        });
    }

    public void step2(final OSDRequest rq, boolean isDeleteOnClose, final unlink_osd_Request args, ErrorResponse error) {
        if (error != null) {
            rq.sendError(error);
            return;
        }

        if (!isDeleteOnClose) {
            // file is not open and can be deleted immediately

            // cancel replication of file
            if (rq.getLocationList().getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY))
                master.getReplicationStage().cancelReplicationForFile(args.getFileId());
            
            master.getDeletionStage().deleteObjects(args.getFileId(), null, rq.getCowPolicy().cowEnabled(), rq, new DeleteObjectsCallback() {

                @Override
                public void deleteComplete(ErrorResponse error) {
                    disseminateDeletes(rq, args);
                }
            });
        } else {
            //file marked for delete on close, send ok to client
            disseminateDeletes(rq, args);
        }
    }

    public void disseminateDeletes(final OSDRequest rq, final unlink_osd_Request args) {
        final Replica localReplica = rq.getLocationList().getLocalReplica();
        if (localReplica.isStriped() && localReplica.getHeadOsd().equals(localUUID)) {
            //striped replica, dissmeninate unlink requests
            try {
                final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
                final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
                int cnt = 0;
                for (ServiceUUID osd : osds) {
                    if (!osd.equals(localUUID)) {
                        gmaxRPCs[cnt++] = master.getOSDClient().unlink(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                args.getFileCredentials(), args.getFileId());
                    }
                }
                this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                    @Override
                    public void responsesAvailable() {
                        analyzeUnlinkReponses(rq, gmaxRPCs);
                    }
                });
            } catch (IOException ex) {
                rq.sendInternalServerError(ex);
                return;
            }
        } else {
            //non striped replica, fini
            sendResponse(rq);
        }
    }

    public void analyzeUnlinkReponses(final OSDRequest rq,RPCResponse[] gmaxRPCs) {
        //analyze results
        try {
            for (int i = 0; i < gmaxRPCs.length; i++) {
                gmaxRPCs[i].get();
            }
            sendResponse(rq);
        } catch (Exception ex) {
            rq.sendInternalServerError(ex);
        } finally {
            for (RPCResponse r : gmaxRPCs)
                r.freeBuffers();
        }
    }
    public void sendResponse(OSDRequest rq) {
        rq.sendSuccess(null,null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            unlink_osd_Request rpcrq = (unlink_osd_Request)rq.getRequestArgs();
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