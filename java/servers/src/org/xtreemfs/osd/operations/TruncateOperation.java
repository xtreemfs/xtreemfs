/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.truncateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class TruncateOperation extends OSDOperation {

    private final String sharedSecret;
    private final ServiceUUID localUUID;

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
    public ErrorResponse startRequest(final OSDRequest rq, final RPCRequestCallback callback) {
        
        final truncateRequest args = (truncateRequest) rq.getRequestArgs();

        if (args.getNewFileSize() < 0) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "new_file_size for truncate must be >= 0");
        }

        if (!rq.getLocationList().getLocalReplica().getHeadOsd().equals(localUUID)) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "truncate must be executed at the head OSD (first OSD in replica)");
        }

        if (rq.getLocationList().getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)) {
            
            // file is read only
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EPERM, 
                    "Cannot write on read-only files.");
        }

        if ((rq.getLocationList().getReplicaUpdatePolicy().length() == 0)
            || (rq.getLocationList().getNumReplicas() == 1)) {

            master.getStorageStage().truncate(args.getNewFileSize(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(),
                rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
                null, false, rq, new AbstractRPCRequestCallback(callback) {
                    
                    @Override
                    public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                            throws ErrorResponseException {
                        
                        return step2(rq, args, (OSDWriteResponse) result, callback);
                    }
                });
        } else {
            
            rwReplicatedTruncate(rq, args, callback);
        }
        
        return null;
    }

    private void rwReplicatedTruncate(final OSDRequest rq, final truncateRequest args, 
            final RPCRequestCallback callback) {
        
        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(), 0, 0, 
                RWReplicationStage.Operation.TRUNCATE, new AbstractRPCRequestCallback(callback) {
              
            @Override
            public <S extends StageRequest<?>> boolean success(final Object newObjectVersion, final S rwrStageRequest)
                    throws ErrorResponseException {

                // final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy(); 
                // XXX dead code

                master.getStorageStage().truncate(args.getNewFileSize(),
                     rq.getLocationList().getLocalReplica().getStripingPolicy(),
                     rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
                     (Long) newObjectVersion, true, rq, new AbstractRPCRequestCallback(callback) {
                         

                    @SuppressWarnings("unchecked")
                    @Override
                    public <T extends StageRequest<?>> boolean success(Object result, T stageRequest)
                            throws ErrorResponseException {
 
                        replicateTruncate((OLPStageRequest<OSDRequest>) rwrStageRequest, (Long) newObjectVersion, args, 
                                (OSDWriteResponse) result, callback);
                        return true;
                    }
                    
                    /* (non-Javadoc)
                     * @see org.xtreemfs.common.stage.AbstractRPCRequestCallback#failed(java.lang.Throwable)
                     */
                    @SuppressWarnings("unchecked")
                    @Override
                    public void failed(Throwable error) {
                        
                        // clean-up the pending RWR request
                        ((OLPStageRequest<AugmentedRequest>) rwrStageRequest).voidMeasurments();
                        master.getRWReplicationStage().exit((StageRequest<AugmentedRequest>) rwrStageRequest);
                        super.failed(error);
                    }
                });
                return false;
            }
        }, rq);
    }

    private void replicateTruncate(final OLPStageRequest<OSDRequest> rwrStageRequest, final long newObjVersion, 
            final truncateRequest args, final OSDWriteResponse result, final RPCRequestCallback callback) {

        master.getRWReplicationStage().replicateTruncate(args.getFileCredentials(),
                rwrStageRequest.getRequest().getLocationList(),args.getNewFileSize(), newObjVersion,
                new AbstractRPCRequestCallback(callback) {
            
            @Override
            public <S extends StageRequest<?>> boolean success(Object result2, S stageRequest)
                    throws ErrorResponseException {
                
                return step2(rwrStageRequest.getRequest(), args, result, callback);
            }
        }, rwrStageRequest);
    }

    private boolean step2(OSDRequest rq, truncateRequest args, OSDWriteResponse result, RPCRequestCallback callback) 
            throws ErrorResponseException {

        //check for striping
        if (rq.getLocationList().getLocalReplica().isStriped()) {
            
            //disseminate internal truncate to all other OSDs
            return disseminateTruncates(rq, args, result, callback);
        } else {
            
            //non-striped
            return callback.success(result);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean disseminateTruncates(OSDRequest rq, truncateRequest args, final OSDWriteResponse result, 
            final RPCRequestCallback callback) throws ErrorResponseException {
        
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
            waitForResponses(gmaxRPCs, new ResponsesListener() {

                @Override
                public void responsesAvailable() {
                    analyzeTruncateResponses(result, gmaxRPCs, callback);
                }
            });
            return true;
        } catch (Throwable ex) {
            
            throw new ErrorResponseException(ex);
        }
    }

    @SuppressWarnings("rawtypes")
    private void analyzeTruncateResponses(OSDWriteResponse result, RPCResponse[] gmaxRPCs, 
            RPCRequestCallback callback) {
        
        //analyze results
        try {
            
            for (int i = 0; i < gmaxRPCs.length; i++) {
                gmaxRPCs[i].get();
            }
            callback.success(result);
        } catch (Throwable ex) {
            
            callback.failed(ex);
        } finally {
            
            for (RPCResponse r : gmaxRPCs) {
                r.freeBuffers();
            }
        }
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