/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_updateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

import com.google.protobuf.Message;

public final class InternalRWRUpdateOperation extends OSDOperation {

    private final String        sharedSecret;
    private final ServiceUUID   localUUID;

    public InternalRWRUpdateOperation(OSDRequestDispatcher master) {
        super(master);
        
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_UPDATE;
    }

    @Override
    public ErrorResponse startRequest(OSDRequest rq, RPCRequestCallback callback) {
        
        final xtreemfs_rwr_updateRequest args = (xtreemfs_rwr_updateRequest)rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "RWR update for file %s-%d", args.getFileId(),
                    args.getObjectNumber());
        }

       prepareLocalWrite(rq, args, callback);
       
       return null;
    }
    
    private void localWrite(OSDRequest rq, xtreemfs_rwr_updateRequest args, RPCRequestCallback callback) {
        
         master.replicatedDataReceived(rq.getRPCRequest().getData().capacity());
         master.getStorageStage().writeObject(args.getObjectNumber(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(), args.getOffset(),
                rq.getRPCRequest().getData().createViewBuffer(), CowPolicy.PolicyNoCow, rq.getLocationList(),
                false, args.getObjectVersion(), rq, new AbstractRPCRequestCallback(callback) {
                    
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                return success((Message) null);
            }
        });
    }

    private void prepareLocalWrite(final OSDRequest rq, final xtreemfs_rwr_updateRequest args, 
            final RPCRequestCallback callback) {
        
        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(),
                args.getObjectNumber(), args.getObjectVersion(), RWReplicationStage.Operation.INTERNAL_UPDATE, 
                new AbstractRPCRequestCallback(callback) {
               
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {

                localWrite(rq, args, callback);
                return true;
            }
        }, rq);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        
        try {
            xtreemfs_rwr_updateRequest rpcrq = (xtreemfs_rwr_updateRequest)rq.getRequestArgs();
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