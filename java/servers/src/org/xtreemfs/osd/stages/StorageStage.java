/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck, Eugenio Cesario
 *               Zuse Institute Berlin, Consiglio Nazionale delle Ricerche
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.util.Map;

import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.common.olp.PerformanceInformationReceiver;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.StorageThread;

import static org.xtreemfs.osd.storage.StorageThread.*;

/**
 * <p>Facade to {@link StorageThread}s that will do all the work.</p>
 * 
 */
public class StorageStage {
    
    private StorageThread[]                      storageThreads;
    private final StorageLayout                  layout;
    private final PerformanceInformationReceiver defaultPredecessor;
    
    public StorageStage(OSDRequestDispatcher master, MetadataCache cache, StorageLayout layout, int numOfThreads) 
            throws IOException {

        this.layout = layout;
        defaultPredecessor = master.getPreprocStage();
        
        int numberOfThreads = 5;
        if (numOfThreads > 0)
            numberOfThreads = numOfThreads;
        
        storageThreads = new StorageThread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++)
            storageThreads[i] = new StorageThread(i, master, cache, layout);
    }

    public StorageLayout getStorageLayout() {
        return layout;
    }
    
    public void readObject(long objNo, StripingPolicyImpl sp, int offset, int length, long versionTimestamp, 
            OSDRequest rq, AbstractRPCRequestCallback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = new StageInternalRequest(rq, null);
        request.getMetadata().updateSize(length);
        enqueueOperation(STAGEOP_READ_OBJECT, new Object[] { rq.getFileId(), objNo, sp, offset, length, versionTimestamp 
                }, request, callback);
    }
    
    public void getFilesize(StripingPolicyImpl sp, long versionTimestamp, OSDRequest rq,
        AbstractRPCRequestCallback callback) {
        
        enqueueOperation(STAGEOP_GET_FILE_SIZE, new Object[] { rq.getFileId(), sp, versionTimestamp }, 
                new StageInternalRequest(rq, defaultPredecessor), callback);
    }
    
    public void writeObject(long objNo, StripingPolicyImpl sp, int offset,
        ReusableBuffer data, CowPolicy cow, XLocations xloc, boolean sync, Long newVersion,
        OSDRequest rq, AbstractRPCRequestCallback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = new StageInternalRequest(rq, null);
        request.getMetadata().updateSize(data.remaining());
        enqueueOperation(STAGEOP_WRITE_OBJECT, new Object[] { rq.getFileId(), objNo, sp, offset, data, cow, xloc, false, 
                sync, newVersion }, request, callback);
    }
    
    public void insertPaddingObject(String fileId, long objNo, StripingPolicyImpl sp, int size, Callback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = null; //new StageInternalRequest(size, null);
        enqueueOperation(STAGEOP_INSERT_PADDING_OBJECT, new Object[] { fileId, objNo, sp, size }, request, callback);
    }
    
    /*
     * currently only used for replication
     */
    public void writeObjectWithoutGMax(String fileId, long objNo, StripingPolicyImpl sp, int offset, 
            ReusableBuffer data, CowPolicy cow, XLocations xloc, boolean sync, Long newVersion, Callback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = null; //new StageInternalRequest(data.remaining, null);
        enqueueOperation(STAGEOP_WRITE_OBJECT, new Object[] { fileId, objNo, sp, offset, data, cow, xloc, true, sync, 
                newVersion }, request, callback);
    }
    
    public void truncate(long newFileSize, StripingPolicyImpl sp, Replica currentReplica, long truncateEpoch, 
            CowPolicy cow, Long newObjVer, Boolean createTruncateLogEntry, OSDRequest rq, 
            AbstractRPCRequestCallback callback) {
       
        // TODO find predecessors
        StageInternalRequest request = new StageInternalRequest(rq, null);
        enqueueOperation(STAGEOP_TRUNCATE, new Object[] { rq.getFileId(), newFileSize, sp, currentReplica, 
                truncateEpoch, cow, newObjVer, createTruncateLogEntry }, request, callback);
    }

    public void deleteObjects(String fileId, StripingPolicyImpl sp, long truncateEpoch, 
            Map<Long,Long> objectVersionsToBeDeleted, Callback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = null; //new StageInternalRequest(data.remaining, null);
        enqueueOperation(STAGEOP_DELETE_OBJECTS, new Object[] { fileId, sp, truncateEpoch, 
                objectVersionsToBeDeleted }, request, callback);
    }
    
    public void flushCaches(String fileId, Callback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = null; //new StageInternalRequest(data.remaining, null);
        enqueueOperation(STAGEOP_FLUSH_CACHES, new Object[] { fileId }, request, callback);
    }
    
    public void receivedGMAX_ASYNC(String fileId, long epoch, long lastObject) {
        
        // TODO find predecessors
        StageInternalRequest request = null; //new StageInternalRequest(data.remaining, null);
        enqueueOperation(STAGEOP_GMAX_RECEIVED, new Object[] { fileId, epoch, lastObject }, request, null);
    }
    
    public void internalGetGmax(StripingPolicyImpl sp, long snapTimestamp, OSDRequest rq, 
            RPCRequestCallback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = new StageInternalRequest(rq, null);
        enqueueOperation(STAGEOP_GET_GMAX, new Object[] { rq.getFileId(), sp, snapTimestamp }, request, callback);
    }

    public void internalGetMaxObjectNo(String fileId, StripingPolicyImpl sp, Callback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = null; //new StageInternalRequest(..., null);
        enqueueOperation(STAGEOP_GET_MAX_OBJNO, new Object[] { fileId, sp }, request, callback);
    }

    public void internalGetReplicaState(OSDRequest rq, StripingPolicyImpl sp, long remoteMaxObjVersion, 
            Callback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = new StageInternalRequest(rq, null);
        enqueueOperation(STAGEOP_GET_REPLICA_STATE, new Object[] { rq.getFileId(), sp, remoteMaxObjVersion }, request, 
                callback);
    }
    
    public void internalGetReplicaState(String fileId, StripingPolicyImpl sp, long remoteMaxObjVersion, 
            Callback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = null; //new StageInternalRequest(..., null);
        enqueueOperation(STAGEOP_GET_REPLICA_STATE, new Object[] { fileId, sp, remoteMaxObjVersion }, request, 
                callback);
    }

    public void getObjectSet(StripingPolicyImpl sp, OSDRequest rq, AbstractRPCRequestCallback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = new StageInternalRequest(rq, null);
        enqueueOperation(STAGEOP_GET_OBJECT_SET, new Object[] { rq.getFileId(), sp }, request, callback);
    }
    
    public void createFileVersion(String fileId, FileMetadata fi, Callback callback) {

        // TODO find predecessors
        StageInternalRequest request = null; //new StageInternalRequest(..., null);
        enqueueOperation(STAGEOP_CREATE_FILE_VERSION, new Object[] { fileId, fi }, request, callback);
    }
    
    public void getFileIDList(OSDRequest rq, AbstractRPCRequestCallback callback) {
        
        // TODO find predecessors
        StageInternalRequest request = new StageInternalRequest(rq, null);
        enqueueOperation(StorageThread.STAGEOP_GET_FILEID_LIST, new Object[] {}, request, callback);
    }
    
    private void enqueueOperation(int stageOp, Object[] args, AugmentedRequest request, Callback callback) {
        
        String fileId = (String) args[0];
        
        // choose the thread the new request has to be
        // assigned to, for its execution
        int taskId = getTaskId(fileId);
        
        // add the new request to the storageTask,
        // in order to start/schedule its execution
        // concurrently with other threads assigned to other
        // storageTasks
        storageThreads[taskId].enter(stageOp, args, request, callback);
    }
    
    public void start() {
        // start all storage threads
        for (StorageThread th : storageThreads) {
            th.start();
        }
    }
    
    public void setLifeCycleListener(LifeCycleListener listener) {
        for (StorageThread th : storageThreads) {
            th.setLifeCycleListener(listener);
        }        
    }
    
    public void shutdown() throws Exception {
        for (StorageThread th : storageThreads) {
            th.shutdown();
        }
    }
    
    public void waitForStartup() throws Exception {
        // wait for all storage threads to be ready
        for (StorageThread th : storageThreads)
            th.waitForStartup();
    }
    
    public void waitForShutdown() throws Exception {
        // wait for all storage threads to be shut down
        for (StorageThread th : storageThreads)
            th.waitForShutdown();
    }
    
    private int getTaskId(String fileId) {
        
        // calculate a hash value from the file ID and return the responsible
        // thread
        assert (fileId != null);
        int key = Math.abs(fileId.hashCode());
        int index = (key % storageThreads.length);
        
        // String objId = rq.getDetails().getFileId()
        // + rq.getDetails().getObjectNumber();
        // int key = Math.abs(objId.hashCode());
        // int index = (key % storageThreads.length);
        
        return index;
    }

    public int getQueueLength() {
        
        int len = 0;
        for(StorageThread th: storageThreads) {
            len += th.getNumberOfRequests();
        }
        return len;
    }
}