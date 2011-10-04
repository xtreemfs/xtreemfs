/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.writeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class WriteOperation extends OSDOperation {

    private final String        sharedSecret;
    private final ServiceUUID   localUUID;

    public WriteOperation(OSDRequestDispatcher master) {
        super(master);
        
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        
        return OSDServiceConstants.PROC_ID_WRITE;
    }

    @Override
    public ErrorResponse startRequest(final OSDRequest rq, RPCRequestCallback callback) {
        
        final writeRequest args = (writeRequest) rq.getRequestArgs();

        if (args.getObjectNumber() < 0) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "object number must be >= 0");
        }

        if (args.getOffset() < 0) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "offset must be >= 0");
        }

        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        if (args.getOffset() >= sp.getStripeSizeForObject(args.getObjectNumber())) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "offset must be < stripe size");
        }
        
        if (rq.getLocationList().getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)) {
            
            // file is read only
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EPERM, 
                    "Cannot write on read-only files.");
        } else {

            final boolean syncWrite = 
                (rq.getCapability().getAccessMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_SYNC.getNumber()) > 0;

            master.objectReceived();
            master.dataReceived(rq.getRPCRequest().getData().capacity());

            if ( (rq.getLocationList().getReplicaUpdatePolicy().length() == 0)
               || (rq.getLocationList().getNumReplicas() == 1) ){

                master.getStorageStage().writeObject(args.getObjectNumber(), sp,
                        args.getOffset(), rq.getRPCRequest().getData().createViewBuffer(), rq.getCowPolicy(),
                        rq.getLocationList(), syncWrite, null, rq, callback);
            } else {
                replicatedWrite(rq, args, syncWrite, callback);
            }
            
            return null;
        }
    }

    public void replicatedWrite(final OSDRequest rq, final writeRequest args, final boolean syncWrite, 
            final RPCRequestCallback callback) {
        
        //prepareWrite first
        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(),args.getObjectNumber(),
                args.getObjectVersion(), RWReplicationStage.Operation.WRITE,
                new AbstractRPCRequestCallback(callback) {
                    
            @Override
            public <S extends StageRequest<?>> boolean success(final Object newObjectVersion, S stageRequest)
                    throws ErrorResponseException {

                assert(((Long) newObjectVersion) > 0L);

                master.getStorageStage().writeObject(args.getObjectNumber(),
                        rq.getLocationList().getLocalReplica().getStripingPolicy(),
                        args.getOffset(), rq.getRPCRequest().getData().createViewBuffer(), rq.getCowPolicy(),
                        rq.getLocationList(), syncWrite, (Long) newObjectVersion, rq, 
                        new AbstractRPCRequestCallback(callback) {
                   
                    @SuppressWarnings("unchecked")
                    @Override
                    public <T extends StageRequest<?>> boolean success(Object result, T stageRequest)
                            throws ErrorResponseException {
                        
                        return sendUpdates((OLPStageRequest<OSDRequest>) stageRequest, args,(OSDWriteResponse) result, 
                                (Long) newObjectVersion, callback);
                    }
                });
                
                return true;
            }
        }, rq);
    }

    public boolean sendUpdates(OLPStageRequest<OSDRequest> stageRequest, final writeRequest args, 
            final OSDWriteResponse result, final long newObjVersion, final RPCRequestCallback callback) {
        
        final OSDRequest rq = stageRequest.getRequest();
        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();
        if (rq.getRPCRequest().getData().remaining() == sp.getStripeSizeForObject(args.getObjectNumber())) {

            sendUpdates2(rq, args, result, newObjVersion, new InternalObjectData(args.getObjectData(), 
                    rq.getRPCRequest().getData().createViewBuffer()), callback);
            
            return true;
        } else {

            master.getStorageStage().readObject(args.getObjectNumber(), sp, 0, -1, 0l, stageRequest, 
                    new AbstractRPCRequestCallback(callback) {
                
                @Override
                public <S extends StageRequest<?>> boolean success(Object result2, S stageRequest)
                        throws ErrorResponseException {
                    
                    InternalObjectData od = ((ObjectInformation) result2).getObjectData(false, 0, 
                            sp.getStripeSizeForObject(args.getObjectNumber()));
                    sendUpdates2(rq, args, result, newObjVersion, od, callback);
                    return true;
                }
            });
            
            return false;
        }
    }
    public void sendUpdates2(final OSDRequest rq, final writeRequest args, final OSDWriteResponse result, 
            final long newObjVersion, final InternalObjectData data, final RPCRequestCallback callback) {
        
        master.getRWReplicationStage().replicatedWrite(args.getFileCredentials(),rq.getLocationList(),
                    args.getObjectNumber(), newObjVersion, data, new AbstractRPCRequestCallback(callback) {
              
            @Override
            public <S extends StageRequest<?>> boolean success(Object newObjectVersion, S stageRequest)
                    throws ErrorResponseException {
                
                return success(result);
            }
        }, rq);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        
        try {
            writeRequest rpcrq = (writeRequest) rq.getRequestArgs();
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