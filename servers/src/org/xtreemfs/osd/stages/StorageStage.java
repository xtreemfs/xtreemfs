/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
 Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB), Eugenio Cesario (CNR)
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.InternalGmax;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.StorageThread;

public class StorageStage extends Stage {

    private StorageThread[] storageThreads;


    /** Creates a new instance of MultithreadedStorageStage */
    public StorageStage(OSDRequestDispatcher master, MetadataCache cache,
            StorageLayout layout, int numOfThreads)
        throws IOException {

        super("OSD Storage Stage");

        int numberOfThreads = 5;
        if (numOfThreads > 0)
            numberOfThreads = numOfThreads;

        storageThreads = new StorageThread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++)
            storageThreads[i] = new StorageThread(i, master, cache,
                layout);
    }

    public void readObject(String fileId, long objNo, StripingPolicyImpl sp,
            int offset, int length, OSDRequest request, ReadObjectCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_READ_OBJECT, new Object[]{fileId,objNo,sp,offset,length}, request, listener);
    }

    public static interface ReadObjectCallback {

        public void readComplete(ObjectInformation result, Exception error);
    }

    public void getFilesize(String fileId, StripingPolicyImpl sp,
            OSDRequest request, GetFileSizeCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GET_FILE_SIZE, new Object[]{fileId,sp}, request, listener);
    }

    public static interface GetFileSizeCallback {

        public void getFileSizeComplete(long fileSize, Exception error);
    }

    public void writeObject(String fileId, long objNo, StripingPolicyImpl sp,
            int offset, ReusableBuffer data, CowPolicy cow, XLocations xloc,
            OSDRequest request, WriteObjectCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_WRITE_OBJECT, new Object[]{fileId,objNo,sp,offset,data,cow,xloc,false}, request, listener);
    }

    /*
     * currently only used for replication
     */
    public void writeObjectWithoutGMax(String fileId, long objNo, StripingPolicyImpl sp,
            int offset, ReusableBuffer data, CowPolicy cow, XLocations xloc,
            OSDRequest request, WriteObjectCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_WRITE_OBJECT, new Object[]{fileId,objNo,sp,offset,data,cow,xloc,true}, request, listener);
    }

    public static interface WriteObjectCallback {

        public void writeComplete(OSDWriteResponse result, Exception error);
    }

    public void truncate(String fileId, long newFileSize, StripingPolicyImpl sp,
            Replica currentReplica, long truncateEpoch, OSDRequest request, TruncateCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_TRUNCATE, new Object[]{fileId,newFileSize,sp,currentReplica,truncateEpoch}, request, listener);
    }

    public static interface TruncateCallback {

        public void truncateComplete(OSDWriteResponse result, Exception error);
    }

    public void flushCaches(String fileId, CachesFlushedCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_FLUSH_CACHES, new Object[]{fileId}, null, listener);
    }

    public static interface CachesFlushedCallback {

        public void cachesFlushed(Exception error);
    }

    public void receivedGMAX_ASYNC(String fileId, long epoch, long lastObject) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GMAX_RECEIVED, new Object[]{fileId,epoch,lastObject}, null, null);
    }

    public void internalGetGmax(String fileId, StripingPolicyImpl sp,
            OSDRequest request, InternalGetGmaxCallback listener) {
        this.enqueueOperation(fileId, StorageThread.STAGEOP_GET_GMAX, new Object[]{fileId,sp}, request, listener);
    }

    public static interface InternalGetGmaxCallback {

        public void gmaxComplete(InternalGmax result, Exception error);
    }


    public void enqueueOperation(String fileId, int stageOp, Object[] args, OSDRequest request, Object callback) {

        //rq.setEnqueueNanos(System.nanoTime());
        
        // choose the thread the new request has to be
        // assigned to, for its execution
        int taskId = getTaskId(fileId);

        // add the new request to the storageTask,
        // in order to start/schedule its execution
        // concurrently with other threads assigned to other
        // storageTasks
        storageThreads[taskId].enqueueOperation(stageOp, args, request, callback);
    }

    public void run() {
        // start all storage threads
        for (StorageThread th : storageThreads)
            th.start();
    }

    public void shutdown() {
        for (StorageThread th : storageThreads)
            th.shutdown();
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
        int key = Math.abs(fileId.hashCode());
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

}
