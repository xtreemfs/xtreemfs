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
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.common.olp.OverloadProtectedStage;
import org.xtreemfs.common.olp.PerformanceInformationReceiver;
import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.Request;
import org.xtreemfs.common.stage.SimpleStageQueue;
import org.xtreemfs.common.stage.Stage;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;

public class DeletionStage extends OverloadProtectedStage<AugmentedRequest> {
        
    private final static int     NUM_RQ_TYPES               = 1;
    private final static int     STAGE_ID                   = 0;
    private final static long    DELTA_MAX_DELETION         = 60 * 1000;
    
    private MetadataCache        cache;
    
    private StorageLayout        layout;
    
    private DeleteThread         deletor;
    
    private final AtomicLong     numFilesDeleted = new AtomicLong(0);
    
    private final PerformanceInformationReceiver predecessor;
    
    public DeletionStage(MetadataCache cache, StorageLayout layout, PerformanceInformationReceiver predecessor) {
        
        super("OSD DelSt", STAGE_ID, NUM_RQ_TYPES, new PerformanceInformationReceiver[] { predecessor });
        
        this.cache = cache;
        this.layout = layout;
        this.predecessor = predecessor;
        
        this.deletor = new DeleteThread(layout);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#start()
     */
    @Override
    public void start() {
        
        super.start();
        deletor.start();
        deletor.setPriority(MIN_PRIORITY);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectedStage#shutdown()
     */
    @Override
    public void shutdown() throws Exception {
        
        super.shutdown();
        deletor.shutdown();
    }
    
    public void deleteObjects(String fileId, FileMetadata fi, boolean isCow, Callback callback) {
        
        // TODO fix request metadata
        enter(new Object[] { fileId, isCow, fi }, new StageInternalRequest(0, 0, DELTA_MAX_DELETION, false, null), 
                callback);
    }
    
    public void deleteObjects(String fileId, FileMetadata fi, boolean isCow, OSDRequest request, Callback callback) {
        
        enter(new Object[] { fileId, isCow, fi }, new StageInternalRequest(request, predecessor), 
                callback);
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectedStage#_processMethod(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    protected void _processMethod(StageRequest<AugmentedRequest> m) {
        
        numFilesDeleted.incrementAndGet();
        
        final Callback callback = m.getCallback();
        final String fileId = (String) m.getArgs()[0];
        final boolean cow = (Boolean) m.getArgs()[1];
        FileMetadata fi = (FileMetadata) m.getArgs()[2];
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "deleting objects of file %s", fileId);
        
        if (fi == null)
            fi = cache.getFileInfo(fileId);
        
        // remove the file info from the storage cache
        cache.removeFileInfo(fileId);
        
        // remove all local objects
        if (layout.fileExists(fileId)) {
            deletor.enqueueFileForDeletion(fileId, cow, fi);
        }
        
        if (!callback.success(null)) {
            m.getRequest().getMonitoring().voidMeasurments();
        }
    }
    
    /**
     * <p>Background thread for deleting files from storage.</p>
     * 
     * @author fx.langner
     * @version 1.00, 09/26/2011
     */
    private final static class DeleteThread extends Stage<Request> {
        
        private final static int    MAX_QUEUE_LENGTH = 1000;
        
        private final StorageLayout layout;
        
        private DeleteThread(StorageLayout layout) {
            super("OSD DelThr", new SimpleStageQueue<Request>(MAX_QUEUE_LENGTH));
            
            this.layout = layout;
        }
        
        private void enqueueFileForDeletion(String fileID, boolean cow, FileMetadata fileMetadata) {

            enter(new DeleteRequest(fileID, cow, fileMetadata));
        }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.Stage#processMethod(org.xtreemfs.common.stage.StageRequest)
         */
        @Override
        public void processMethod(StageRequest<Request> method) {
            
            final String fileId = ((DeleteRequest) method.getRequest()).fileId;
            final FileMetadata fi = ((DeleteRequest) method.getRequest()).fileMetadata;
            
            try {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "deleting objects for %s", fileId);
                
                // if copy-on-write is enabled ...
                if (((DeleteRequest) method.getRequest()).cow) {
                    
                    // if no previous versions exist, delete the file
                    // including all its metadata
                    if (fi.getVersionTable().getVersionCount() == 0) {
                        layout.deleteFile(fileId, true);
                    
                    // if other versions exist, only delete those
                    // objects that make up the latest version of the
                    // file and are not part of former file versions
                    } else {
                        
                        for (Entry<Long, Long> entry : fi.getLatestObjectVersions()) {
                            long objNo = entry.getKey();
                            long objVer = entry.getValue();
                            if (!fi.getVersionTable().isContained(objNo, objVer))
                                layout.deleteObject(fileId, fi, objNo, objVer);
                        }
                        
                        layout.updateCurrentVersionSize(fileId, 0);
                    }

                // otherwise ...
                } else {
                    layout.deleteFile(fileId, true);
                }
                
            } catch (IOException ex) {
                Logging.logError(Logging.LEVEL_ERROR, this, ex);
            }
        }
        
        
        /**
         * <p>Request details for removing file objects from storage.</p>
         * 
         * @author fx.langner
         * @version 1.00, 09/26/2011
         */
        private final static class DeleteRequest implements Request {
            
            private final String fileId;
            private final boolean cow;
            private final FileMetadata fileMetadata;
            
            private DeleteRequest(String fileId, boolean cow, FileMetadata fileMetadata) {
                
                this.fileId = fileId;
                this.cow = cow;
                this.fileMetadata = fileMetadata;
            }
        }
    }
    
/*
 * Monitoring
 */
    
    /**
     * @return the numFilesDeleted
     */
    public long getNumFilesDeleted() {
        
        return numFilesDeleted.get();
    }
}