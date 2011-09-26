/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;

/**
 * 
 * <br>
 * Sep 12, 2011
 */
public class FileInfo {
    /** Different states regarding osdWriteResponse and its write back. */
    enum FilesizeUpdateStatus {
        kClean, kDirty, kDirtyAndAsyncPending, kDirtyAndSyncPending
    };

    /** Volume which did open this file. */
    private VolumeImplementation volume;

    /** XtreemFS File ID of this file (does never change). */
    long                         fileId;

    /**
     * Path of the File, used for debug output and writing back the OSDWriteResponse to the MetadataCache.
     */
    private String               path;

    /**
     * Extracted from the FileHandle's XCap: true if an explicit close() has to be send to the MRC in order to
     * trigger the on close replication.
     */
    boolean                      replicateOnClose;

    /** Number of file handles which hold a pointer on this object. */
    private AtomicInteger        referenceCount;

    /** List of corresponding OSDs. */
    private XLocSet              xlocset;

    /**
     * UUIDIterator which contains the UUIDs of all replicas.
     * 
     * If striping is used, replication is not possible. Therefore, for striped files the UUID Iterator will
     * contain only the head OSD.
     */
    private UUIDIterator         osdUuidIterator;

    /** Use this to protect xlocset_ and replicate_on_close_. */
    // boost::mutex xlocset_mutex_;

    /**
     * List of active locks (acts as a cache). The OSD allows only one lock per (client UUID, PID) tuple.
     */
    private Map<Integer, Lock>   activeLocks;
    // Map<int, Lock> active_locks_;

    /** Use this to protect active_locks_. */
    // boost::mutex active_locks_mutex_;

    /** Random UUID of this client to distinguish them while locking. */
    private String               clientUuid;

    /** List of open FileHandles for this file. */
    private ConcurrentLinkedQueue<FileHandle>     openFileHandles;

    /** Use this to protect open_file_handles_. */
    // boost::mutex open_file_handles_mutex_;

    /**
     * List of open FileHandles which solely exist to propagate a pending file size update (a OSDWriteResponse
     * object) to the MRC.
     * 
     * This extra list is needed to distinguish between the regular file handles (see open_file_handles_) and
     * the ones used for file size updates. The intersection of both lists is empty.
     */
    private List<FileHandle>     pendingFilesizeUpdates;

    /**
     * Pending file size update after a write() operation, may be NULL.
     * 
     * If osdWriteResponse != NULL, the fileSize and truncateEpoch of the referenced OSDWriteResponse have to
     * be respected, e.g. when answering a GetAttr request. This osdWriteResponse also corresponds to the
     * "maximum" of all known OSDWriteReponses. The maximum has the highest truncate_epoch, or if equal
     * compared to another response, the higher sizeInBytes value.
     */
    private OSDWriteResponse     osdWriteResponse;

    /** Denotes the state of the stored osd_write_response_ object. */
    private FilesizeUpdateStatus osdWriteResponseStatus;

    /** XCap required to send an OSDWriteResponse to the MRC. */
    private XCap                 osdWriteResponseXcap;

    /**
     * Always lock to access osd_write_response_, osd_write_response_status_, osd_write_response_xcap_ or
     * pending_filesize_updates_.
     */
    // boost::mutex osd_write_response_mutex_;

    /** Used by NotifyFileSizeUpdateCompletition() to notify waiting threads. */
    // boost::condition osd_write_response_cond_;

    /**
     * Proceeds async writes, handles the callbacks and provides a WaitForPendingWrites() method for barrier
     * operations like read.
     */
    // AsyncWriteHandler async_write_handler_;

    /**
     * 
     */
    public FileInfo(VolumeImplementation volume, long fileId, String path, boolean replicateOnClose,
            XLocSet xlocset, String clientUuid) {
        this.volume = volume;
        this.fileId = fileId;
        this.path = path;
        this.replicateOnClose = replicateOnClose;
        this.xlocset = xlocset;
        this.clientUuid = clientUuid;

        referenceCount = new AtomicInteger(0);
        osdWriteResponse = null;
        osdWriteResponseStatus = FilesizeUpdateStatus.kClean;
        // TODO: Initialize async write handler.
        
        openFileHandles = new ConcurrentLinkedQueue<FileHandle>();
    }

    public void renamePath(String path, String newPath) {

    }

    public void UpdateXLocSetAndRest(XLocSet newXlocset, boolean replicateOnClose) {

        xlocset = XLocSet.newBuilder(newXlocset).build();

        this.replicateOnClose = replicateOnClose;
    }

    /**
     * Returns a new FileHandle object to which xcap belongs.
     * 
     */
    FileHandleImplementation createFileHandle(XCap xcap, boolean asyncWritesEnabled) {
        return createFileHandle(xcap, asyncWritesEnabled, false);
    }

    /**
     * See CreateFileHandle(xcap). Does not add fileHandle to list of open file handles if
     * usedForPendingFilesizeUpdate=true.
     * 
     * This function will be used if a FileHandle was solely created to asynchronously write back a dirty file
     * size update (osdWriteResponse).
     * 
     * @remark Ownership is transferred to the caller.
     */
    FileHandleImplementation createFileHandle(XCap xcap, boolean asyncWritesEnabled,
            boolean usedForPendingFilesizeUpdate) {

        FileHandleImplementation fileHandleImplementation = new FileHandleImplementation(clientUuid, this,
                xcap, volume.getMrcUuidIterator(), osdUuidIterator, volume.getUUIDResolver(),
                volume.getMrcServiceClient(), volume.getOsdServiceClient(), asyncWritesEnabled,
                volume.getOptions(), volume.getAuthBogus(), volume.getUserCredentialsBogus());
        
        //increase reference count and add it to openFileHandles
        referenceCount.incrementAndGet();
        openFileHandles.add(fileHandleImplementation);        
        
        return fileHandleImplementation;
    }
    
    /** 
     * Deregisters a closed FileHandle. Called by FileHandle::Close(). 
     * 
     * */
    void closeFileHandle(FileHandleImplementation fileHandle) {
        //TODO: Autogenerated stub.
    }
    
    
}
