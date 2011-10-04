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
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
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
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.unlink_osd_Request;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

import com.google.protobuf.Message;

public final class DeleteOperation extends OSDOperation {

    private final String sharedSecret;

    private final ServiceUUID localUUID;

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
    public ErrorResponse startRequest(final OSDRequest rq, final RPCRequestCallback callback) {
        
        final unlink_osd_Request args = (unlink_osd_Request) rq.getRequestArgs();
        
        // file is not open and can be deleted immediately
        if (!master.getPreprocStage().doCheckDeleteOnClose(args.getFileId())) {

            // cancel replication of file
            if (rq.getLocationList().getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)) {
                master.getReplicationStage().cancelReplicationForFile(args.getFileId());
            }
            
            master.getDeletionStage().deleteObjects(args.getFileId(), null, rq.getCowPolicy().cowEnabled(), rq, 
                    new AbstractRPCRequestCallback(callback) {
                
                // executed by DeletionStage
                @Override
                public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                        throws ErrorResponseException {

                    disseminateDeletes(rq, args, callback);
                    return true;
                }
            });
        } else {
            
            //file marked for delete on close, send ok to client
            try {
                
                disseminateDeletes(rq, args, callback);
            } catch (ErrorResponseException e) {
                
                return e.getRPCError();
            }
        }
        
        return null;
    }

    @SuppressWarnings("unchecked")
    public void disseminateDeletes(OSDRequest rq, unlink_osd_Request args, final RPCRequestCallback callback) 
            throws ErrorResponseException {
        
        Replica localReplica = rq.getLocationList().getLocalReplica();
        if (localReplica.isStriped() && localReplica.getHeadOsd().equals(localUUID)) {
            
            //striped replica, disseminate unlink requests
            try {
                
                List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
                final RPCResponse<Message>[] gmaxRPCs = new RPCResponse[osds.size() - 1];
                int cnt = 0;
                for (ServiceUUID osd : osds) {
                    if (!osd.equals(localUUID)) {
                        gmaxRPCs[cnt++] = master.getOSDClient().unlink(osd.getAddress(), RPCAuthentication.authNone, 
                                RPCAuthentication.userService, args.getFileCredentials(), args.getFileId());
                    }
                }
                waitForResponses(gmaxRPCs, new ResponsesListener() {

                    // executed by OSDClient
                    @Override
                    public void responsesAvailable() {
                        
                        ErrorResponse err = analyzeUnlinkReponses(gmaxRPCs);
                        if (err != null) {
                            callback.failed(err);
                        } else {
                            try {
                                callback.success();
                            } catch (ErrorResponseException e) {
                                callback.failed(e.getRPCError());
                            }
                        }
                    }
                });
            } catch (IOException ex) {
                
                throw new ErrorResponseException(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, 
                        POSIXErrno.POSIX_ERROR_NONE, "internal server error:" + ex.getMessage(), 
                        OutputUtils.stackTraceToString(ex)));
            }
        } 
    }

    public ErrorResponse analyzeUnlinkReponses(RPCResponse<Message>[] gmaxRPCs) {
        
        //analyze results
        try {
            
            for (int i = 0; i < gmaxRPCs.length; i++) {
                gmaxRPCs[i].get();
            }
            return null;
        } catch (Exception ex) {
            
            return ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, 
                    "internal server error:" + ex.getMessage(), OutputUtils.stackTraceToString(ex));
        } finally {
            
            for (RPCResponse<Message> r : gmaxRPCs) {
                if (r != null) r.freeBuffers();
            }
        }
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