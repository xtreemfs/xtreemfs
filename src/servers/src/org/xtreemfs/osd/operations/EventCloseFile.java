/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */
package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.LRUCache;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.DeletionStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.StorageStage.CachesFlushedCallback;
import org.xtreemfs.osd.stages.StorageStage.CreateFileVersionCallback;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.FileMetadata;

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
    public void startRequest(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");
        
    }
    
    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public boolean requiresCapability() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void startInternalEvent(Object[] args) {
        
        final String fileId = (String) args[0];
        final Boolean isDeleteOnClose = (Boolean) args[1];
        final FileMetadata fi = (FileMetadata) args[2];
        final CowPolicy cow = (CowPolicy) args[3];
        final LRUCache<String, Capability> cachedCaps = (LRUCache<String, Capability>) args[4];
        
        master.getStorageStage().flushCaches(fileId, new CachesFlushedCallback() {
            
            @Override
            public void cachesFlushed(Exception error) {
                
                if (error != null)
                    Logging.logError(Logging.LEVEL_ERROR, this, error);
                
                // if COW is enabled, create a new version
                if (cow.cowEnabled())
                    createNewVersion(fileId, fi, cachedCaps, isDeleteOnClose);
                
                // if the file is marked for deletion, delete it
                else if (isDeleteOnClose)
                    deleteObjects(fileId, fi, cow.cowEnabled());
                
            }
        });

        master.getRWReplicationStage().fileClosed(fileId);
        
    }
    
    public void deleteObjects(String fileId, FileMetadata fi, boolean isCow) {
        
        // cancel replication of file
        master.getReplicationStage().cancelReplicationForFile(fileId);
        
        master.getDeletionStage().deleteObjects(fileId, fi, isCow, null, new DeleteObjectsCallback() {
            
            @Override
            public void deleteComplete(Exception error) {
                if (error != null) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "exception in internal event: %s", error
                            .toString());
                    Logging.logError(Logging.LEVEL_ERROR, this, error);
                }
                
            }
        });
    }
    
    public void createNewVersion(final String fileId, final FileMetadata fi,
        LRUCache<String, Capability> cachedCaps, final boolean isDeleteOnClose) {
        
        // first, check if there are any write capabilities among the cached
        // capabilities
        boolean writeCap = false;
        for (Capability cap : cachedCaps.values()) {
            int accessMode = cap.getAccessMode();
            if ((accessMode & Constants.SYSTEM_V_FCNTL_H_O_RDWR) != 0
                || (accessMode & Constants.SYSTEM_V_FCNTL_H_O_TRUNC) != 0
                || (accessMode & Constants.SYSTEM_V_FCNTL_H_O_WRONLY) != 0
                || (accessMode & Constants.SYSTEM_V_FCNTL_H_O_APPEND) != 0) {
                writeCap = true;
                break;
            }
        }
        
        // if there are no write capabilities, there is no need to create a new
        // version
        if (!writeCap)
            return;
        
        master.getStorageStage().createFileVersion(fileId, fi, null, new CreateFileVersionCallback() {
            public void createFileVersionComplete(long fileSize, Exception error) {
                
                if (error != null) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "exception in internal event: %s", error
                            .toString());
                    Logging.logError(Logging.LEVEL_ERROR, this, error);
                }
                
                // if the file is marked for deletion, delete it
                if (isDeleteOnClose)
                    deleteObjects(fileId, fi, true);
            }
        });
        
    }
}
