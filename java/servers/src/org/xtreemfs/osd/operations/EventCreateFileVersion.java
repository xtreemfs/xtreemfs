/*
 * Copyright (c) 2009-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.FileMetadata;

/**
 * 
 * @author bjko
 */
public class EventCreateFileVersion extends OSDOperation {
    
    public EventCreateFileVersion(OSDRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public int getProcedureId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public ErrorResponse startRequest(OSDRequest rq, final RPCRequestCallback callback) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    

    @Override
    public boolean requiresCapability() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void startInternalEvent(Object[] args) {
        
        final String fileId = (String) args[0];
        final FileMetadata fi = (FileMetadata) args[1];
        
        master.getStorageStage().createFileVersion(fileId, fi, new Callback() {
            

            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                return true;
            }
            
            @Override
            public void failed(Throwable error) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "exception in internal event: %s", error.getMessage());
            }
        });
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
