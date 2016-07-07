/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck, Eugenio Cesario
 *               Zuse Institute Berlin, Consiglio Nazionale delle Ricerche
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.StorageThread;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDFinalizeVouchersResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalGmax;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;

public class StorageStage extends Stage {
    
    private final StorageThread[] storageThreads;
    private final StorageLayout layout;
    
    /** Creates a new instance of MultithreadedStorageStage */
    public StorageStage(OSDRequestDispatcher master, MetadataCache cache, StorageLayout layout,
        int numOfThreads, int maxRequestsQueueLength) throws IOException {
        
        super("OSD Storage Stage", maxRequestsQueueLength);

        this.layout = layout;

        int numberOfThreads = 5;
        if (numOfThreads > 0)
            numberOfThreads = numOfThreads;
        
        storageThreads = new StorageThread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            // Each storage thread gets the max. queue length as it is possible that one thread gets the whole load
            storageThreads[i] = new StorageThread(i, master, cache, layout, maxRequestsQueueLength);
            storageThreads[i].setLifeCycleListener(master);
        }
    }

    public StorageLayout getStorageLayout() {
        return layout;
    }

    
    public void readObject(String fileId, long objNo, StripingPolicyImpl sp, int offset, int length,
        long versionTimestamp, OSDRequest request, ReadObjectCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_READ_OBJECT, new Object[] { fileId, objNo, sp,
            offset, length, versionTimestamp }, request, listener);
    }
    
    public static interface ReadObjectCallback {
        
        public void readComplete(ObjectInformation result, ErrorResponse error);
    }
    
    public void getFilesize(String fileId, StripingPolicyImpl sp, long versionTimestamp, OSDRequest request,
        GetFileSizeCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GET_FILE_SIZE, new Object[] { fileId, sp, versionTimestamp },
            request, listener);
    }
    
    public static interface GetFileSizeCallback {
        
        public void getFileSizeComplete(long fileSize, ErrorResponse error);
    }
    
    public void writeObject(String fileId, long objNo, StripingPolicyImpl sp, int offset,
            ReusableBuffer data, CowPolicy cow, XLocations xloc, boolean sync, Long newVersion,
        OSDRequest request, ReusableBuffer createdViewBuffer, WriteObjectCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_WRITE_OBJECT, new Object[] { fileId, objNo, sp,
            offset, data, cow, xloc, false, sync, newVersion }, request, createdViewBuffer, listener);
    }
    
    public void insertPaddingObject(String fileId, long objNo, StripingPolicyImpl sp, int size,
        OSDRequest request, WriteObjectCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_INSERT_PADDING_OBJECT, new Object[] { fileId,
            objNo, sp, size }, request, listener);
    }
    
    /*
     * currently only used for replication
     */
    public void writeObjectWithoutGMax(String fileId, long objNo, StripingPolicyImpl sp, int offset,
        ReusableBuffer data, CowPolicy cow, XLocations xloc, boolean sync, Long newVersion,
        OSDRequest request, WriteObjectCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_WRITE_OBJECT, new Object[] { fileId, objNo, sp,
            offset, data, cow, xloc, true, sync, newVersion }, request, listener);
    }
    
    public static interface WriteObjectCallback {
        
        public void writeComplete(OSDWriteResponse result, ErrorResponse error);
    }
    
    public void truncate(String fileId, long newFileSize, StripingPolicyImpl sp, Replica currentReplica,
        long truncateEpoch, CowPolicy cow, Long newObjVer, Boolean createTruncateLogEntry, OSDRequest request, TruncateCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_TRUNCATE, new Object[] { fileId, newFileSize, sp,
            currentReplica, truncateEpoch, cow, newObjVer, createTruncateLogEntry }, request, listener);
    }
    
    public static interface TruncateCallback {
        
        public void truncateComplete(OSDWriteResponse result, ErrorResponse error);
    }

    public void finalizeVouchers(String fileId, String clientId, StripingPolicyImpl sp, Set<Long> expireTimeSet,
            OSDRequest request, FinalizeVoucherCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_FINALIZE_VOUCHERS, new Object[] { fileId, clientId, sp,
                expireTimeSet }, request, listener);
    }

    public static interface FinalizeVoucherCallback {
        public void finalizeVoucherComplete(OSDFinalizeVouchersResponse result, ErrorResponse error);
    }

    public void deleteObjects(String fileId, StripingPolicyImpl sp,
        long truncateEpoch, Map<Long,Long> objectVersionsToBeDeleted, DeleteObjectsCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_DELETE_OBJECTS, new Object[] { fileId, sp,
            truncateEpoch, objectVersionsToBeDeleted }, null, listener);
    }

    public static interface DeleteObjectsCallback {

        public void deleteObjectsComplete(ErrorResponse error);
    }
    
    public void flushCaches(String fileId, CachesFlushedCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_FLUSH_CACHES, new Object[] { fileId }, null,
            listener);
    }
    
    public static interface CachesFlushedCallback {
        public void cachesFlushed(ErrorResponse error, FileMetadata md);
    }
    
    public void receivedGMAX_ASYNC(String fileId, long epoch, long lastObject) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GMAX_RECEIVED, new Object[] { fileId, epoch,
            lastObject }, null, null);
    }
    
    public void internalGetGmax(String fileId, StripingPolicyImpl sp, long snapTimestamp, OSDRequest request,
        InternalGetGmaxCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GET_GMAX, new Object[] { fileId, sp,
            snapTimestamp }, request, listener);
    }
    
    public static interface InternalGetGmaxCallback {
        
        public void gmaxComplete(InternalGmax result, ErrorResponse error);
    }

    public void internalGetMaxObjectNo(String fileId, StripingPolicyImpl sp, InternalGetMaxObjectNoCallback callback) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GET_MAX_OBJNO, new Object[]{fileId,sp}, null, callback);
    }

    public static interface InternalGetMaxObjectNoCallback {

        public void maxObjectNoCompleted(long maxObjNo, long fileSize, long truncateEpoch, ErrorResponse error);
    }

    public void internalGetReplicaState(String fileId, StripingPolicyImpl sp, long remoteMaxObjVersion,
            InternalGetReplicaStateCallback callback) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GET_REPLICA_STATE, new Object[]{fileId,sp,remoteMaxObjVersion}, null, callback);
    }

    public static interface InternalGetReplicaStateCallback {

        public void getReplicaStateComplete(ReplicaStatus localState, ErrorResponse error);

    }

    public void getObjectSet(String fileId, StripingPolicyImpl sp, OSDRequest request,
            GetObjectListCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GET_OBJECT_SET, new Object[] { fileId,sp },
                request, listener);
    }
    
    public void createFileVersion(String fileId, FileMetadata fi, OSDRequest request, CreateFileVersionCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_CREATE_FILE_VERSION, new Object[] { fileId, fi },
            request, listener);
    }
    
    public static interface GetObjectListCallback {
        
        public void getObjectSetComplete(ObjectSet result, ErrorResponse error);
    }
    
    public static interface CreateFileVersionCallback {
        
        public void createFileVersionComplete(long fileSize, ErrorResponse error);
    }
    
    public void getFileIDList(OSDRequest request, GetFileIDListCallback listener) {
        this.enqueueOperation("foobar", StorageThread.STAGEOP_GET_FILEID_LIST, new Object[] {}, request, listener);
    }
    
    public static interface GetFileIDListCallback {
        
        public void createGetFileIDListComplete(ArrayList<String> fileIDList, ErrorResponse Error);
    }
    
    public void ecGetVectors(String fileId, OSDRequest request, ECGetVectorsCallback callback) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_EC_GET_VECTORS, new Object[] { fileId }, request, callback);
    }

    public static interface ECGetVectorsCallback {
        public void ecGetVectorsComplete(IntervalVector curVector, IntervalVector nextVector, ErrorResponse error);
    }

    public void ecCommitVector(String fileId, StripingPolicyImpl sp, List<Interval> commitIntervals, OSDRequest request,
            ECCommitVectorCallback callback) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_EC_COMMIT_VECTOR,
                new Object[] { fileId, sp, commitIntervals }, request, callback);
    }

    public static interface ECCommitVectorCallback {
        public void ecCommitVectorComplete(boolean needsReconstruct, ErrorResponse error);
    }

    /**
     * 
     * @param fileId
     * @param sp
     * @param objNo
     * @param offset
     * @param reqInterval
     * @param commitIntervals
     * @param data
     *            Will be freed by this method
     * @param request
     * @param callback
     */
    public void ecWriteInterval(String fileId, StripingPolicyImpl sp, long objNo, int offset, Interval reqInterval,
            List<Interval> commitIntervals, ReusableBuffer data, OSDRequest request, ECWriteIntervalCallback callback) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_EC_WRITE_INTERVAL, 
                new Object[] {fileId, sp, objNo, offset, reqInterval, commitIntervals, data}, 
                request, data, callback);
    }

    public static interface ECWriteIntervalCallback {
        public void ecWriteIntervalComplete(ReusableBuffer diff, boolean needsReconstruct, ErrorResponse error);
    }
    
    public void ecWriteDiff(String fileId, StripingPolicyImpl sp, long objNo, int offset, Interval diffInterval,
            Interval stripeInterval, List<Interval> commitIntervals, ReusableBuffer data, OSDRequest request,
            ECWriteDiffCallback callback) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_EC_WRITE_DIFF,
                new Object[] { fileId, sp, objNo, offset, diffInterval, stripeInterval, commitIntervals, data },
                request, data, callback);
    }

    public static interface ECWriteDiffCallback {
        public void ecWriteDiffComplete(boolean stripeComplete, boolean needsReconstruct, ErrorResponse error);
    }

    public void ecReadData(String fileId, StripingPolicyImpl sp, long objNo, int offset, int length,
            List<Interval> intervals, OSDRequest request, ECReadDataCallback callback) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_EC_READ_DATA,
                new Object[] { fileId, sp, objNo, offset, length, intervals }, request, null, callback);
    }

    public static interface ECReadDataCallback {
        public void ecReadDataComplete(ObjectInformation result, boolean needsReconstruct, ErrorResponse error);
    }

    @Override
    public void enqueueOperation(int stageOp, Object[] args, OSDRequest request, Object callback) {
        notifyCrashed(new Exception(
                "wrong method call: use enqueueOperation(String fileId, int stageOp, Object[] args, OSDRequest request, Object callback) instead!"));
    }
    
    public void enqueueOperation(String fileId, int stageOp, Object[] args, OSDRequest request,
        Object callback) {
        enqueueOperation(fileId, stageOp, args, request, null, callback);
    }
    
    public void enqueueOperation(String fileId, int stageOp, Object[] args, OSDRequest request,
            ReusableBuffer createdViewBuffer, Object callback) {
            
            // rq.setEnqueueNanos(System.nanoTime());
            
            // choose the thread the new request has to be
            // assigned to, for its execution
            int taskId = getTaskId(fileId);
            
            // add the new request to the storageTask,
            // in order to start/schedule its execution
            // concurrently with other threads assigned to other
            // storageTasks
            storageThreads[taskId].enqueueOperation(stageOp, args, request, createdViewBuffer, callback);
        }
    
    @Override
    public void run() {
        // start all storage threads
        for (StorageThread th : storageThreads)
            th.start();
    }
    
    @Override
    public void shutdown() {
        for (StorageThread th : storageThreads)
            th.shutdown();
    }
    
    @Override
    public void waitForStartup() throws Exception {
        // wait for all storage threads to be ready
        for (StorageThread th : storageThreads)
            th.waitForStartup();
    }
    
    @Override
    public void waitForShutdown() throws Exception {
        // wait for all storage threads to be shut down
        for (StorageThread th : storageThreads)
            th.waitForShutdown();
    }
    
    private int getTaskId(String fileId) {
        
        // calculate a hash value from the file ID and return the responsible
        // thread
        assert (fileId != null);
        int hash = fileId.hashCode();
        if (hash == Integer.MIN_VALUE) {
            return 0;
        }
        int key = Math.abs(hash);
        int index = (key % storageThreads.length);
        
        // String objId = rq.getDetails().getFileId()
        // + rq.getDetails().getObjectNumber();
        // int key = Math.abs(objId.hashCode());
        // int index = (key % storageThreads.length);
        
        return index;
    }
    
    @Override
    protected void processMethod(StageRequest method) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public int getQueueLength() {
        
        int len = 0;
        for(StorageThread th: storageThreads)
            len += th.getQueueLength();
        
        return len;
    }
    
}
