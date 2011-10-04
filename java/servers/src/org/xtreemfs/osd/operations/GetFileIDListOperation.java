/*
 * Copyright (c) 2009-2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.util.ArrayList;

import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_fileid_listResponse;

public class GetFileIDListOperation extends OSDOperation {
    
//    private final String sharedSecret; XXX dead code
//    private final ServiceUUID localUUID;

    public GetFileIDListOperation(OSDRequestDispatcher master) {
        super(master);
        
//        sharedSecret = master.getConfig().getCapabilitySecret();
//        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        
        return OSDServiceConstants.PROC_ID_XTREEMFS_INTERNAL_GET_FILEID_LIST;
    }
    
    @Override
    public ErrorResponse startRequest(OSDRequest rq, final RPCRequestCallback callback) {

        master.getStorageStage().getFileIDList(rq, new AbstractRPCRequestCallback(callback) {
            
            @SuppressWarnings("unchecked")
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                return postGetFileIDList((ArrayList<String>) result, callback);
            }
        });
        
        return null;
    }

    private boolean postGetFileIDList(ArrayList<String> fileIDList, RPCRequestCallback callback) 
            throws ErrorResponseException {
      
        final xtreemfs_internal_get_fileid_listResponse.Builder responseBuilder = 
            xtreemfs_internal_get_fileid_listResponse.newBuilder();
        for (String fileID : fileIDList) {
            responseBuilder.addFileIds(fileID);
        }
        return callback.success(responseBuilder.build());
    }
    
    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        
        return null;
    }

    @Override
    public boolean requiresCapability() {
        
        return false;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }
}