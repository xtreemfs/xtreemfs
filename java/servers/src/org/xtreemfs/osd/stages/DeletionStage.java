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
import org.xtreemfs.common.olp.AugmentedInternalRequest;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.olp.OverloadProtectedStage;
import org.xtreemfs.common.olp.PerformanceInformationReceiver;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.SimpleStageQueue;
import org.xtreemfs.common.stage.Stage;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;

public class DeletionStage extends OverloadProtectedStage<AugmentedRequest> {
        
    private final static int     NUM_RQ_TYPES               = 1;
    private final static int     NUM_INTERNAL_RQ_TYPES      = 1;
    private final static int     STAGE_ID                   = 1;
    
    public final static int      STAGEOP_DELETE             = -1;
    public final static int      STAGEOP_INT_DELETE         = 0;
    
    private MetadataCache        cache;
    
    private StorageLayout        layout;
    
    private DeleteThread         deletor;
    
    private final AtomicLong     numFilesDeleted = new AtomicLong(0);
    
    public DeletionStage(MetadataCache cache, StorageLayout layout, PerformanceInformationReceiver predecessor) {
        super("OSD DelSt", STAGE_ID, NUM_RQ_TYPES, NUM_INTERNAL_RQ_TYPES, new PerformanceInformationReceiver[] { 
                predecessor });
        
        this.cache = cache;
        this.layout = layout;
        
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
        
        deletor.shutdown();
        super.shutdown();
    }
    
    public void internalDeleteObjects(String fileId, FileMetadata fi, boolean isCow, Callback callback) {
        
        enter(STAGEOP_INT_DELETE, new Object[] { fileId, isCow, fi }, new AugmentedInternalRequest(STAGEOP_INT_DELETE), 
                callback);
    }
    
    public void deleteObjects(String fileId, FileMetadata fi, boolean isCow, OSDRequest request, 
            AbstractRPCRequestCallback callback) {
        
        enter(STAGEOP_DELETE, new Object[] { fileId, isCow, fi }, request, callback, getInitialPredecessors());
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectedStage#_processMethod(org.xtreemfs.common.olp.OLPStageRequest)
     */
    @Override
    protected boolean _processMethod(OLPStageRequest<AugmentedRequest> stageRequest) {
        
        numFilesDeleted.incrementAndGet();
        
        final Callback callback = stageRequest.getCallback();
        final String fileId = (String) stageRequest.getArgs()[0];
        final boolean cow = (Boolean) stageRequest.getArgs()[1];
        FileMetadata fi = (FileMetadata) stageRequest.getArgs()[2];
        
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
        
        try {
        
            return callback.success(null, stageRequest);
        } catch (ErrorResponseException e) {
            
            stageRequest.voidMeasurments();
            callback.failed(e);
            return true;
        }
    }
    

    /**
     * <p>Request details for removing file objects from storage.</p>
     * 
     * @author fx.langner
     * @version 1.00, 09/26/2011
     */
    private final static class DeleteRequest {
        
        private final String fileId;
        private final boolean cow;
        private final FileMetadata fileMetadata;
        
        private DeleteRequest(String fileId, boolean cow, FileMetadata fileMetadata) {
            
            this.fileId = fileId;
            this.cow = cow;
            this.fileMetadata = fileMetadata;
        }
    }
    
    /**
     * <p>Background thread for deleting files from storage.</p>
     * 
     * @author fx.langner
     * @version 1.00, 09/26/2011
     */
    private final static class DeleteThread extends Stage<DeleteRequest> {
        
        private final static int    MAX_QUEUE_LENGTH = 1000;
        
        private final StorageLayout layout;
        
        private DeleteThread(StorageLayout layout) {
            super("OSD DelThr", new SimpleStageQueue<DeleteRequest>(MAX_QUEUE_LENGTH));
            
            this.layout = layout;
        }
        
        private void enqueueFileForDeletion(String fileID, boolean cow, FileMetadata fileMetadata) {

            enter(new DeleteRequest(fileID, cow, fileMetadata), null);
        }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.Stage#processMethod(org.xtreemfs.common.stage.StageRequest)
         */
        @Override
        protected <S extends StageRequest<DeleteRequest>> boolean processMethod(S stageRequest) {
            
            final String fileId = stageRequest.getRequest().fileId;
            final FileMetadata fi = stageRequest.getRequest().fileMetadata;
            
            try {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "deleting objects for %s", fileId);
                
                // if copy-on-write is enabled ...
                if (stageRequest.getRequest().cow) {
                    
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
            
            return true;
        }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.Stage#generateStageRequest(int, java.lang.Object[], java.lang.Object, org.xtreemfs.common.stage.Callback)
         */
        @SuppressWarnings("unchecked")
        @Override
        protected <S extends StageRequest<DeleteRequest>> S generateStageRequest(
                int stageMethodId, Object[] args, DeleteRequest request,
                Callback callback) {
            
            return (S) new StageRequest<DeleteRequest>(stageMethodId, args, request, callback) {};
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