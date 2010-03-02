/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;

public class DeletionStage extends Stage {
    
    public static final int      STAGEOP_DELETE_OBJECTS = 0;
    
    private MetadataCache        cache;
    
    private StorageLayout        layout;
    
    private OSDRequestDispatcher master;
    
    private DeleteThread         deletor;
    
    private long                 numFilesDeleted;
    
    public DeletionStage(OSDRequestDispatcher master, MetadataCache cache, StorageLayout layout) {
        
        super("OSD DelSt");
        
        this.master = master;
        this.cache = cache;
        this.layout = layout;
        
        deletor = new DeleteThread(layout);
    }
    
    public void start() {
        super.start();
        deletor.start();
        deletor.setPriority(MIN_PRIORITY);
    }
    
    public void shutdown() {
        super.shutdown();
        deletor.shutdown();
    }
    
    public void deleteObjects(String fileId, FileMetadata fi, boolean isCow, OSDRequest request,
        DeleteObjectsCallback listener) {
        this.enqueueOperation(STAGEOP_DELETE_OBJECTS, new Object[] { fileId, isCow, fi }, request,
            listener);
    }
    
    /**
     * @return the numFilesDeleted
     */
    public long getNumFilesDeleted() {
        return numFilesDeleted;
    }
    
    public static interface DeleteObjectsCallback {
        
        public void deleteComplete(Exception error);
    }
    
    @Override
    protected void processMethod(StageRequest method) {
        
        try {
            switch (method.getStageMethod()) {
            case STAGEOP_DELETE_OBJECTS:
                numFilesDeleted++;
                processDeleteObjects(method);
                break;
            default:
                method.sendInternalServerError(new RuntimeException("unknown stage op request"));
            }
            
        } catch (Throwable exc) {
            Logging.logError(Logging.LEVEL_ERROR, this, exc);
            method.sendInternalServerError(exc);
            return;
        }
    }
    
    private void processDeleteObjects(StageRequest rq) throws OSDException {
        
        final DeleteObjectsCallback cback = (DeleteObjectsCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final boolean cow = (Boolean) rq.getArgs()[1];
        FileMetadata fi = (FileMetadata) rq.getArgs()[2];
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "deleting objects of file %s",
                fileId);
        
        if (fi == null)
            fi = cache.getFileInfo(fileId);
        
        // remove the file info from the storage cache
        cache.removeFileInfo(fileId);
        
        // remove all local objects
        if (layout.fileExists(fileId))
            deletor.enqueueFileForDeletion(fileId, cow, fi);
        cback.deleteComplete(null);
    }
    
    private final static class DeleteThread extends Thread {
        
        private transient boolean                   quit;
        
        private final StorageLayout                 layout;
        
        private final LinkedBlockingQueue<Object[]> files;
        
        public DeleteThread(StorageLayout layout) {
            quit = false;
            this.layout = layout;
            files = new LinkedBlockingQueue<Object[]>();
        }
        
        public void shutdown() {
            this.quit = true;
            this.interrupt();
        }
        
        public void enqueueFileForDeletion(String fileID, boolean cow, FileMetadata fi) {
            assert (this.isAlive());
            assert (fileID != null);
            files.add(new Object[] { fileID, cow, fi });
        }
        
        public void run() {
            try {
                do {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.lifecycle, this,
                            "DeleteThread started");
                    
                    final Object[] file = files.take();
                    final String fileId = (String) file[0];
                    final boolean cow = (Boolean) file[1];
                    final FileMetadata fi = (FileMetadata) file[2];
                    
                    try {
                        if (Logging.isDebug())
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                                "deleting objects for %s", fileId);
                        
                        // if copy-on-write is enabled ...
                        if (cow) {
                            
                            // if no previous versions exist, delete the file
                            // including all its metadata
                            if (fi.getVersionTable().getVersionCount() == 0)
                                layout.deleteFile(fileId, true);
                            
                            // if other versions exist, only delete those
                            // objects that make up the latest version of the
                            // file and are not part of former file versions
                            else {
                                
                                for (Entry<Long, Long> entry : fi.getLatestObjectVersions()) {
                                    long objNo = entry.getKey();
                                    long objVer = entry.getValue();
                                    if (!fi.getVersionTable().isContained(objNo, objVer))
                                        layout.deleteObject(fileId, fi, objNo, objVer);
                                }
                                
                                layout.updateCurrentVersionSize(fileId, 0);
                            }
                            
                        }

                        // otherwise ...
                        else
                            layout.deleteFile(fileId, true);
                        
                        yield();
                        
                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_ERROR, this, ex);
                    }
                } while (!quit);
            } catch (InterruptedException ex) {
                // idontcare
            }
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.lifecycle, this, "DeleteThread finished");
        }
        
    }
    
}
