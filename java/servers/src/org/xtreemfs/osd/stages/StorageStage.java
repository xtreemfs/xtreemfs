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
import org.xtreemfs.common.olp.AugmentedInternalRequest;
import org.xtreemfs.common.olp.OLPStageRequest;
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
 * <p>Facade to {@link StorageThread}s which will do all the work.</p>
 * 
 */
public class StorageStage {
    
    private final StorageThread[]       storageThreads;
    private final StorageLayout         layout;
    private final OSDRequestDispatcher  dispatcher;
    
    public StorageStage(OSDRequestDispatcher master, MetadataCache cache, StorageLayout layout, int numOfThreads) 
            throws IOException {

        this.layout = layout;
        this.dispatcher = master;
        
        int numberOfThreads = 5;
        if (numOfThreads > 0)
            numberOfThreads = numOfThreads;
        
        storageThreads = new StorageThread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            storageThreads[i] = new StorageThread(i, master, cache, layout);
        }
    }
    
    public void registerPerformanceInformationReceiver(PerformanceInformationReceiver[] receiver) {
        
        for (StorageThread st : storageThreads) {
            st.registerPerformanceInformationReceiver(receiver);
        }
    }

    public StorageLayout getStorageLayout() {
        return layout;
    }
    
    public void readObject(long objNo, StripingPolicyImpl sp, int offset, int length, long versionTimestamp, 
            OSDRequest rq, AbstractRPCRequestCallback callback) {
                
        enqueueOperation(length, STAGEOP_READ_OBJECT, new Object[] { rq.getFileId(), objNo, sp, offset, length, 
                versionTimestamp }, rq, callback, new PerformanceInformationReceiver[] { dispatcher.getPreprocStage(), 
                dispatcher.getRWReplicationStage() });
    }
    
    public void readObject(long objNo, StripingPolicyImpl sp, int offset, int length, long versionTimestamp, 
            OLPStageRequest<OSDRequest> stageRequest, AbstractRPCRequestCallback callback) {
        
        requeueOperation(stageRequest, STAGEOP_READ_OBJECT, new Object[] { stageRequest.getRequest().getFileId(), objNo, 
                sp, offset, length, versionTimestamp }, callback);
    }
    
    public void getFilesize(StripingPolicyImpl sp, long versionTimestamp, OSDRequest rq,
        AbstractRPCRequestCallback callback) {
        
        enqueueOperation(STAGEOP_GET_FILE_SIZE, new Object[] { rq.getFileId(), sp, versionTimestamp }, rq, callback, 
                dispatcher.getPreprocStage());
    }
    
    public void writeObject(long objNo, StripingPolicyImpl sp, int offset,
        ReusableBuffer data, CowPolicy cow, XLocations xloc, boolean sync, Long newVersion,
        OSDRequest rq, AbstractRPCRequestCallback callback) {
        
        enqueueOperation(data.remaining(), STAGEOP_WRITE_OBJECT, new Object[] { rq.getFileId(), objNo, sp, offset, data, 
            cow, xloc, false, sync, newVersion }, rq, callback, new PerformanceInformationReceiver[] { 
            dispatcher.getPreprocStage(), dispatcher.getRWReplicationStage()});
    }
    
    public void insertPaddingObject(String fileId, long objNo, StripingPolicyImpl sp, int size, Callback callback) {
        
        enqueueOperation(size, STAGEOP_INSERT_PADDING_OBJECT, new Object[] { fileId, objNo, sp, size }, 
                new AugmentedInternalRequest(STAGEOP_INSERT_PADDING_OBJECT), callback);
    }
    
    /*
     * currently only used for replication
     */
    public void writeObjectWithoutGMax(String fileId, long objNo, StripingPolicyImpl sp, int offset, 
            ReusableBuffer data, CowPolicy cow, XLocations xloc, boolean sync, Long newVersion, Callback callback) {
        
        enqueueOperation(STAGEOP_WRITE_OBJECT, new Object[] { fileId, objNo, sp, offset, data, cow, xloc, true, sync, 
                newVersion }, new AugmentedInternalRequest(STAGEOP_INTERNAL_WRITE_OBJECT), callback);
    }
    
    public void truncate(long newFileSize, StripingPolicyImpl sp, Replica currentReplica, long truncateEpoch, 
            CowPolicy cow, Long newObjVer, Boolean createTruncateLogEntry, OSDRequest rq, 
            AbstractRPCRequestCallback callback) {
    
        enqueueOperation(STAGEOP_TRUNCATE, new Object[] { rq.getFileId(), newFileSize, sp, currentReplica, 
                truncateEpoch, cow, newObjVer, createTruncateLogEntry }, rq, callback, 
                new PerformanceInformationReceiver[] { dispatcher.getRWReplicationStage(), 
                dispatcher.getPreprocStage()});
    }

    public void deleteObjects(String fileId, StripingPolicyImpl sp, long truncateEpoch, 
            Map<Long,Long> objectVersionsToBeDeleted, Callback callback) {
        
        enqueueOperation(STAGEOP_DELETE_OBJECTS, new Object[] { fileId, sp, truncateEpoch, 
                objectVersionsToBeDeleted }, new AugmentedInternalRequest(STAGEOP_DELETE_OBJECTS), callback);
    }
    
    public void flushCaches(String fileId, Callback callback) {
        
        enqueueOperation(STAGEOP_FLUSH_CACHES, new Object[] { fileId }, 
                new AugmentedInternalRequest(STAGEOP_FLUSH_CACHES), callback);
    }
    
    public void receivedGMAX_ASYNC(String fileId, long epoch, long lastObject) {
        
        enqueueOperation(STAGEOP_GMAX_RECEIVED, new Object[] { fileId, epoch, lastObject }, 
                new AugmentedInternalRequest(STAGEOP_GMAX_RECEIVED), null);
    }
    
    public void internalGetGmax(StripingPolicyImpl sp, long snapTimestamp, OSDRequest rq, 
            RPCRequestCallback callback) {
        
        enqueueOperation(STAGEOP_GET_GMAX, new Object[] { rq.getFileId(), sp, snapTimestamp }, rq, callback, 
                dispatcher.getPreprocStage());
    }

    public void internalGetMaxObjectNo(String fileId, StripingPolicyImpl sp, Callback callback) {
        
        enqueueOperation(STAGEOP_GET_MAX_OBJNO, new Object[] { fileId, sp }, 
                new AugmentedInternalRequest(STAGEOP_GET_MAX_OBJNO), callback);
    }

    public void internalGetReplicaState(OSDRequest rq, StripingPolicyImpl sp, long remoteMaxObjVersion, 
            RPCRequestCallback callback) {
        
        enqueueOperation(STAGEOP_GET_REPLICA_STATE, new Object[] { rq.getFileId(), sp, remoteMaxObjVersion }, rq, 
                callback, dispatcher.getPreprocStage());
    }
    
    public void internalGetReplicaState(String fileId, StripingPolicyImpl sp, long remoteMaxObjVersion, 
            Callback callback) {
        
        enqueueOperation(STAGEOP_GET_REPLICA_STATE, new Object[] { fileId, sp, remoteMaxObjVersion }, 
                new AugmentedInternalRequest(STAGEOP_INTERNAL_GET_REPLICA_STATE), callback);
    }

    public void getObjectSet(StripingPolicyImpl sp, OSDRequest rq, AbstractRPCRequestCallback callback) {
        
        enqueueOperation(STAGEOP_GET_OBJECT_SET, new Object[] { rq.getFileId(), sp }, rq, callback, 
                dispatcher.getPreprocStage());
    }
    
    public void getObjectSet(StripingPolicyImpl sp, OLPStageRequest<OSDRequest> stageRequest, 
            AbstractRPCRequestCallback callback) {
        
        requeueOperation(stageRequest, STAGEOP_GET_OBJECT_SET, new Object[] { stageRequest.getRequest().getFileId(), 
                sp }, callback);
    }
    
    public void createFileVersion(String fileId, FileMetadata fi, Callback callback) {

        enqueueOperation(STAGEOP_CREATE_FILE_VERSION, new Object[] { fileId, fi }, 
                new AugmentedInternalRequest(STAGEOP_CREATE_FILE_VERSION), callback);
    }
    
    public void createFileVersion(String fileId, FileMetadata fi, OLPStageRequest<AugmentedRequest> stageRequest, 
            Callback callback) {

        requeueOperation(stageRequest, STAGEOP_CREATE_FILE_VERSION, new Object[] { fileId, fi }, callback);
    }
    
    public void getFileIDList(OSDRequest rq, AbstractRPCRequestCallback callback) {
        
        enqueueOperation(StorageThread.STAGEOP_GET_FILEID_LIST, new Object[] {}, rq, callback, 
                dispatcher.getPreprocStage());
    }
    
    private void enqueueOperation(int stageOp, Object[] args, OSDRequest request, AbstractRPCRequestCallback callback, 
            PerformanceInformationReceiver[] predecessor) {
        
        enqueueOperation(0L, stageOp, args, request, callback, predecessor);
    }
    
    private void enqueueOperation(long size, int stageOp, Object[] args, OSDRequest request, AbstractRPCRequestCallback callback, 
            PerformanceInformationReceiver[] predecessor) {
        
        final String fileId = (String) args[0];
        
        // choose the thread the new request has to be
        // assigned to, for its execution
        final int taskId = getTaskId(fileId);
        
        // add the new request to the storageTask,
        // in order to start/schedule its execution
        // concurrently with other threads assigned to other
        // storageTasks
        storageThreads[taskId].enter(size, stageOp, args, request, callback, predecessor);
    }
    
    private void enqueueOperation(int stageOp, Object[] args, OSDRequest request, AbstractRPCRequestCallback callback, 
            PerformanceInformationReceiver predecessor) {
        
        final String fileId = (String) args[0];
        
        // choose the thread the new request has to be
        // assigned to, for its execution
        final int taskId = getTaskId(fileId);
        
        // add the new request to the storageTask,
        // in order to start/schedule its execution
        // concurrently with other threads assigned to other
        // storageTasks
        storageThreads[taskId].enter(stageOp, args, request, callback, predecessor);
    }
    
    private void enqueueOperation(int stageOp, Object[] args, AugmentedInternalRequest request, Callback callback) {
        enqueueOperation(0L, stageOp, args, request, callback);
    }
    
    private void enqueueOperation(long size, int stageOp, Object[] args, AugmentedInternalRequest request, Callback callback) {
        
        final String fileId = (String) args[0];
        
        // choose the thread the new request has to be
        // assigned to, for its execution
        final int taskId = getTaskId(fileId);
        
        // add the new request to the storageTask,
        // in order to start/schedule its execution
        // concurrently with other threads assigned to other
        // storageTasks
        storageThreads[taskId].enter(size, stageOp, args, request, callback, new PerformanceInformationReceiver[0]);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void requeueOperation(OLPStageRequest stageRequest, int newMethodId, Object[] newArgs, 
            Callback newCallback) {
        
        final String fileId = (String) newArgs[0];
        
        // choose the thread the new request has to be
        // assigned to, for its execution
        final int taskId = getTaskId(fileId);
        
        // add the new request to the storageTask,
        // in order to start/schedule its execution
        // concurrently with other threads assigned to other
        // storageTasks
        storageThreads[taskId].recycle(stageRequest, newMethodId, newArgs, newCallback, false);
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
    
    public PerformanceInformationReceiver[] getThreads() {
        
        return storageThreads;
    }
}