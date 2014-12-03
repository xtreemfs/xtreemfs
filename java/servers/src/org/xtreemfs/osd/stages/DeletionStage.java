/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
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
    
    public DeletionStage(OSDRequestDispatcher master, MetadataCache cache, StorageLayout layout, int maxRequestsQueueLength) {
        
        super("OSD DelSt", maxRequestsQueueLength);
        
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
            final boolean deleteMetadata, DeleteObjectsCallback listener) {
        this.enqueueOperation(STAGEOP_DELETE_OBJECTS, new Object[] { fileId, isCow, fi, deleteMetadata }, request,
                listener);
    }

    /**
     * @return the numFilesDeleted
     */
    public long getNumFilesDeleted() {
        return numFilesDeleted;
    }
    
    public static interface DeleteObjectsCallback {
        
        public void deleteComplete(ErrorResponse error);
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
    
    private void processDeleteObjects(StageRequest rq) {
        
        final DeleteObjectsCallback cback = (DeleteObjectsCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final boolean cow = (Boolean) rq.getArgs()[1];
        FileMetadata fi = (FileMetadata) rq.getArgs()[2];
        final boolean deleteMetadata = (Boolean) rq.getArgs()[3];
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "deleting objects of file %s",
                fileId);
        
        if (fi == null)
            fi = cache.getFileInfo(fileId);
        
        // remove the file info from the storage cache
        cache.removeFileInfo(fileId);
        
        // remove all local objects
        if (layout.fileExists(fileId))
            deletor.enqueueFileForDeletion(fileId, cow, fi, deleteMetadata);
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
        
        public void enqueueFileForDeletion(String fileID, boolean cow, FileMetadata fi, boolean deleteMetadata) {
            assert (this.isAlive());
            assert (fileID != null);
            files.add(new Object[] { fileID, cow, fi, deleteMetadata });
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
                    final boolean deleteMetadata = (Boolean) file[3];
                    
                    try {
                        if (Logging.isDebug())
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                                "deleting objects for %s", fileId);
                        
                        // if copy-on-write is enabled ...
                        if (cow) {
                            
                            if (fi == null) {
                                Logging.logMessage(
                                        Logging.LEVEL_ERROR,
                                        this,
                                        "Deleting objects failed for COW enabled file %s, because FileMetadata is missing.",
                                        fileId);
                                continue;
                            }

                            // if no previous versions exist, delete the file
                            // including all its metadata if requested
                            if (fi.getVersionTable().getVersionCount() == 0)
                                layout.deleteFile(fileId, deleteMetadata);
                            
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
                            layout.deleteFile(fileId, deleteMetadata);
                        
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
