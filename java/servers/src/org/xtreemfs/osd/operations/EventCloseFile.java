/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;

/**
 * 
 * @author bjko
 */
public class EventCloseFile extends OSDOperation {
    
    public EventCloseFile(OSDRequestDispatcher master) {
        
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
    
    @SuppressWarnings("unchecked")
    @Override
    public void startInternalEvent(Object[] args) {
        
        final String fileId = (String) args[0];
        final Boolean isDeleteOnClose = (Boolean) args[1];
        final FileMetadata fi = (FileMetadata) args[2];
        final CowPolicy cow = (CowPolicy) args[3];
        final LRUCache<String, Capability> cachedCaps = (LRUCache<String, Capability>) args[4];
        
        master.getStorageStage().flushCaches(fileId, new Callback() {

            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                // if COW is enabled, create a new version
                if (cow.cowEnabled()) {
                    return createNewVersion(fileId, fi, cachedCaps, isDeleteOnClose, 
                            (OLPStageRequest<AugmentedRequest>) stageRequest);
                
                // if the file is marked for deletion, delete it
                } else if (isDeleteOnClose) {
                    deleteObjects(fileId, fi, cow.cowEnabled()); 
                }
                
                return true;
            }

            @Override
            public void failed(Throwable error) {
                
                Logging.logMessage(Logging.LEVEL_ERROR, this, error.getMessage());
            }
        });
        
        master.getRWReplicationStage().fileClosed(fileId);      
    }
    
    private void deleteObjects(String fileId, FileMetadata fi, boolean isCow) {
        
        // cancel replication of file
        master.getReplicationStage().cancelReplicationForFile(fileId);
        
        master.getDeletionStage().internalDeleteObjects(fileId, fi, isCow, new Callback() {

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
    
    private boolean createNewVersion(final String fileId, final FileMetadata fi, LRUCache<String, Capability> cachedCaps, 
            final boolean isDeleteOnClose, OLPStageRequest<AugmentedRequest> stageRequest) {
        
        // first, check if there are any write capabilities among the cached
        // capabilities
        boolean writeCap = false;
        if (cachedCaps != null)
            for (Capability cap : cachedCaps.values()) {
                int accessMode = cap.getAccessMode();
                if ((accessMode & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()) != 0
                    || (accessMode & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()) != 0
                    || (accessMode & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber()) != 0
                    || (accessMode & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_APPEND.getNumber()) != 0) {
                    writeCap = true;
                    break;
                }
            }
        
        // if there are no write capabilities, there is no need to create a new
        // version
        if (!writeCap)
            return true;
        
        master.getStorageStage().createFileVersion(fileId, fi, stageRequest, new Callback() {


            @Override
            public <S extends StageRequest<?>> boolean success(Object fileSize, S stageRequest)
                    throws ErrorResponseException {
                
                if (isDeleteOnClose) {
                    deleteObjects(fileId, fi, true);
                }
                return true;
            }
            
            @Override
            public void failed(Throwable error) {

                Logging.logMessage(Logging.LEVEL_ERROR, this, "exception in internal event: %s", error.getMessage());
            }
        });
        
        return false;
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }
}