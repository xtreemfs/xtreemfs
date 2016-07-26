/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.InternalServerErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;

//JCIP import net.jcip.annotations.GuardedBy;
/**
 * Stores metadata of the the file as well as the current size. One FileInfo is responsible for all open FileHandles.
 */
public class FileInfo {
    /**
     * Different states regarding osdWriteResponse and its write back.
     */
    enum FilesizeUpdateStatus {
        kClean, kDirty, kDirtyAndAsyncPending, kDirtyAndSyncPending
    };

    /**
     * Volume which did open this file.
     */
    private VolumeImplementation                            volume;

    /**
     * XtreemFS File ID of this file (does never change).
     */
    long                                                    fileId;

    /**
     * Path of the File, used for debug output and writing back the OSDWriteResponse to the MetadataCache.
     */
    // JCIP @GuardedBy("pathLock")
    private String                                          path;

    /**
     * Used to protect "path".
     */
    private final Object                                    pathLock;

    /**
     * Extracted from the FileHandle's XCap: true if an explicit close() has to be send to the MRC in order to
     * trigger the on close replication.
     */
    boolean                                                 replicateOnClose;

    /**
     * Number of file handles which hold a pointer on this object.
     */
    private AtomicInteger                                   referenceCount;

    /**
     * List of corresponding OSDs.
     */
    // JCIP @GuardedBy("xLocSetLock")
    private XLocSet                                         xlocset;

    /**
     * Use this to protect "xlocset" and "replicateOnClose".
     */
    Object                                                  xLocSetLock;
    
    /**
     * Use this to protect the renewal of xLocSets.
     */
    Object                                                  xLocSetRenewalLock;

    /**
     * UUIDIterator which contains the UUIDs of all replicas.
     * 
     * If striping is used, replication is not possible. Therefore, for striped files the UUID Iterator will
     * contain only the head OSD.
     */
    private UUIDIterator                                    osdUuidIterator;

    /**
     * List of active locks (acts as a cache). The OSD allows only one lock per (client UUID, PID) tuple.
     */
    private ConcurrentHashMap<Integer, Lock>                activeLocks;

    /**
     * Random UUID of this client to distinguish them while locking.
     */
    private String                                          clientUuid;

    /**
     * List of open FileHandles for this file.
     */
    private ConcurrentLinkedQueue<FileHandleImplementation> openFileHandles;

    /**
     * List of open FileHandles which solely exist to propagate a pending file size update (a OSDWriteResponse
     * object) to the MRC.
     * 
     * This extra list is needed to distinguish between the regular file handles (see open_file_handles_) and
     * the ones used for file size updates. The intersection of both lists is empty.
     */
    // JCIP @GuardedBy("osdWriteResponseLock")
    private List<FileHandle>                                pendingFilesizeUpdates;

    /**
     * Pending file size update after a write() operation, may be NULL.
     * 
     * If osdWriteResponse != NULL, the fileSize and "truncateEpoch" of the referenced
     * {@link OSDWriteResponse} have to be respected, e.g. when answering a {@link getattrRequest}. This
     * "osdWriteResponse" also corresponds to the "maximum" of all known OSDWriteReponses. The maximum has the
     * highest "truncateEpoch", or if equal compared to another response, the higher "sizeInBytes" value.
     */
    // JCIP @GuardedBy("osdWriteResponseLock")
    private OSDWriteResponse                                osdWriteResponse;

    /**
     * Denotes the state of the stored "osdWriteResponse" object.
     */
    // JCIP @GuardedBy("osdWriteResponseLock")
    private FilesizeUpdateStatus                            osdWriteResponseStatus;

    /**
     * XCap required to send an OSDWriteResponse to the MRC.
     */
    // JCIP @GuardedBy("osdWriteResponseLock")
    private XCap                                            osdWriteResponseXcap;

    /**
     * Always lock to access "osdWriteResponse", "osdWriteResponseStatus", "osdWriteResponseXcap" or
     * "pendingFilesizeUpdates".
     */
    private final Object                                    osdWriteResponseLock;

    /**
     * Proceeds async writes, handles the callbacks and provides a waitForPendingWrites() method for barrier
     * operations like read. Can be null if the Volume has been initialized with async writes disabled.
     */
    AsyncWriteHandler                                       asyncWriteHandler;

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
        osdWriteResponseLock = new Object();

        pathLock = new Object();
        xLocSetLock = new Object();

        xLocSetRenewalLock = new Object();

        openFileHandles = new ConcurrentLinkedQueue<FileHandleImplementation>();
        activeLocks = new ConcurrentHashMap<Integer, Lock>();

        // Add the UUIDs of all replicas to the UUID Iterator.
        osdUuidIterator = new UUIDIterator();
        osdUuidIterator.addUUIDs(Helper.getOSDUUIDsFromXlocSet(xlocset));

        if (volume.getOptions().isEnableAsyncWrites()) {
            asyncWriteHandler = new AsyncWriteHandler(this, osdUuidIterator, volume.getUUIDResolver(),
                    volume.getOsdServiceClient(), volume.getAuthBogus(), volume.getUserCredentialsBogus(), volume
                            .getOptions().getMaxWriteahead(), volume.getOptions().getMaxWriteaheadRequests(), volume
                            .getOptions().getMaxWriteTries());
        }

        pendingFilesizeUpdates = new ArrayList<FileHandle>(volume.getOptions().getMaxWriteahead());
    }

    /**
     * @see FileInfo#updateXLocSetAndRest(XLocSet, boolean)
     */


    /**
     * Create a copy of "newXlocSet" and save it to the "xlocset" member. "replicateOnClose" will be save in
     * the corresponding member, too.
     * 
     * @param newXlocset
     *            XlocSet that will be copied and set.
     * @param replicateOnClose
     *            true if replicate on close is used. false otherwise.
     * 
     */
    protected void updateXLocSetAndRest(XLocSet newXlocset, boolean replicateOnClose) {
        synchronized (xLocSetLock) {
            xlocset = XLocSet.newBuilder(newXlocset).build();
            this.replicateOnClose = replicateOnClose;
        }

        // Update the osdUuidIterator to reflect the changes in the xlocset.
        osdUuidIterator.clearAndAddUUIDs(Helper.getOSDUUIDsFromXlocSet(newXlocset));
    }

    /**
     * @see FileInfo#updateXLocSetAndRest(XLocSet, boolean)
     */
    protected void updateXLocSetAndRest(XLocSet newXlocset) {
        synchronized (xLocSetLock) {
            xlocset = XLocSet.newBuilder(newXlocset).build();
        }

        // Update the osdUuidIterator to reflect the changes in the xlocset.
        osdUuidIterator.clearAndAddUUIDs(Helper.getOSDUUIDsFromXlocSet(newXlocset));
    }

    /**
     * Returns a new FileHandle object to which xcap belongs.
     * 
     */
    FileHandleImplementation createFileHandle(XCap xcap, boolean asyncWritesEnabled) {
        return createFileHandle(xcap, asyncWritesEnabled, false);
    }

    /**
     * See createFileHandle(xcap). Does not add fileHandle to list of open file handles if
     * usedForPendingFilesizeUpdate=true.
     * 
     * This function will be used if a FileHandle was solely created to asynchronously write back a dirty file
     * size update (osdWriteResponse).
     * 
     * @remark Ownership is transferred to the caller.
     */
    FileHandleImplementation createFileHandle(XCap xcap, boolean asyncWritesEnabled,
            boolean usedForPendingFilesizeUpdate) {
        FileHandleImplementation fileHandleImplementation = new FileHandleImplementation(volume, clientUuid, this,
                xcap, volume.getMrcUuidIterator(), osdUuidIterator, volume.getUUIDResolver(),
                volume.getMrcServiceClient(), volume.getOsdServiceClient(), volume.getStripeTranslators(),
                asyncWritesEnabled, volume.getOptions(), volume.getAuthBogus(),
                volume.getUserCredentialsBogus());

        // increase reference count and add it to openFileHandles
        referenceCount.incrementAndGet();
        openFileHandles.add(fileHandleImplementation);

        return fileHandleImplementation;
    }

    /**
     * Deregisters a closed FileHandle. Called by FileHandle.close().
     */
    protected void closeFileHandle(FileHandleImplementation fileHandle) {
        // Pending async writes and file size updates have already been flushed
        // by fileHandle.

        // remove file handle.
        boolean removed = openFileHandles.remove(fileHandle);
        if (!removed) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "FileHandle for fileId %s has been closed multiple times.",
                    fileId);
        }

        // Decreasing reference count is handle by Volume.closeFile().
        volume.closeFile(fileId, this, fileHandle);
    }

    /**
     * Decreases the reference count and returns the current value.
     */
    protected int decreaseReferenceCount() {
        int count = referenceCount.decrementAndGet();
        assert (count >= 0);
        return count;
    }

    /**
     * Returns a copy of "osdWriteResponse" if not NULL.
     */
    protected OSDWriteResponse getOSDWriteResponse() {
        synchronized (osdWriteResponseLock) {
            if (osdWriteResponse == null) {
                return null;
            } else {
                return osdWriteResponse.toBuilder().build();
            }
        }
    }

    /**
     * Returns path.
     */
    protected String getPath() {
        synchronized (pathLock) {
            return path;
        }
    }

    /**
     * Changes path to newPath if this.path == path.
     */
    protected void renamePath(String path, String newPath) {
        synchronized (pathLock) {
            if (this.path.equals(path)) {
                this.path = newPath;
            }
        }
    }

    /**
     * Compares "response" against the current "osdWriteResponse". Returns true if response is newer and
     * assigns "response" to "osdWriteResponse".
     * 
     * If successful, a new file handle will be created and xcap is required to send the osdWriteResponse to
     * the MRC in the background.
     * 
     */
    protected boolean tryToUpdateOSDWriteResponse(OSDWriteResponse response, XCap xcap) {
        assert (response != null);

        synchronized (osdWriteResponseLock) {
            if (Helper.compareOSDWriteResponses(response, osdWriteResponse) == 1) {
                // update osdWriteResponse
                osdWriteResponse = response.toBuilder().build();
                osdWriteResponseXcap = xcap.toBuilder().build();
                osdWriteResponseStatus = FilesizeUpdateStatus.kDirty;
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Merge into a possibly outdated Stat object (e.g. from the StatCache) the current file size and
     * truncateEpoch from a stored OSDWriteResponse.
     */
    protected Stat mergeStatAndOSDWriteResponse(Stat stat) {
        synchronized (osdWriteResponseLock) {
            if (osdWriteResponse != null) {
                // Check if information in Stat is newer than
                // osdWriteResponse_.
                if (stat.getTruncateEpoch() < osdWriteResponse.getTruncateEpoch()
                        || stat.getTruncateEpoch() == osdWriteResponse.getTruncateEpoch()
                        && stat.getSize() < osdWriteResponse.getSizeInBytes()) {
                    // Information from "osdWriteResponse" are newer.
                    stat = stat.toBuilder().setSize(osdWriteResponse.getSizeInBytes())
                            .setTruncateEpoch(osdWriteResponse.getTruncateEpoch()).build();

                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "getattr: merged infos from osdWriteResponse, size: %s", stat.getSize());
                    }
                }
            }
        }
        return stat;
    }

    /**
     * Sends pending file size updates to the MRC asynchronously.
     */
    protected void writeBackFileSizeAsync() throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        synchronized (osdWriteResponseLock) {
            // Only update pending file size updates.
            if (osdWriteResponse != null && osdWriteResponseStatus == FilesizeUpdateStatus.kDirty) {
                FileHandleImplementation fileHandle = createFileHandle(osdWriteResponseXcap, false, true);
                pendingFilesizeUpdates.add(fileHandle);
                osdWriteResponseStatus = FilesizeUpdateStatus.kDirtyAndAsyncPending;

                fileHandle.setOsdWriteResponseForAsyncWriteBack(osdWriteResponse);
                fileHandle.writeBackFileSizeAsync();
            }
        }
    }

    /**
     * Renews xcap of all file handles of this file asynchronously.
     */
    protected void renewXCapsAsync() throws AddressToUUIDNotFoundException {
        Iterator<FileHandleImplementation> fhiIterator = openFileHandles.iterator();
        try {
            while (fhiIterator.hasNext()) {
                fhiIterator.next().renewXCapAsync();
            }
        } catch (IOException ioe) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "renewXcapsSync: Failed to renew XCap for fileHandles. Reason: %s", ioe.getCause());
            }
        }
    }

    /**
     * Releases all locks of processId using fileHandle to issue ReleaseLock().
     */
    protected void releaseLockOfProcess(FileHandleImplementation fileHandle, int processId)
            throws PosixErrorException, AddressToUUIDNotFoundException {
        Lock lock = activeLocks.get(processId);

        if (lock != null) {
            try {
                // releaseLock deletes Lock from activeLocks
                fileHandle.releaseLock(lock);
            } catch (IOException ioe) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "releaseLock: Failed to release Lock for processID: %s. Reason: %s", processId,
                            ioe.getCause());
                }
            }
        }
    }

    /**
     * Uses fileHandle to release all known local locks.
     */
    protected void releaseAllLocks(FileHandleImplementation fileHandle) throws PosixErrorException,
            AddressToUUIDNotFoundException {
        for (Lock lock : activeLocks.values()) {
            // FileHandleImplementation.releaseLock(lock) will delete the lock
            // from activeLocks.
            try {
                fileHandle.releaseLock(lock);
            } catch (IOException ioe) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "releaseAllLocks: Failed to release for some Locks. Reason: %s", ioe.getCause());
                }
            }
        }
    }

    /**
     * Blocks until all asynchronous file size updates are completed.
     */
    protected void waitForPendingFileSizeUpdates() {
        synchronized (osdWriteResponseLock) {
            while (pendingFilesizeUpdates.size() > 0) {
                try {
                    osdWriteResponseLock.wait();
                } catch (InterruptedException e) {
                    // TODO: handle exception and figure out what happens if thread
                    // gets interrupted.
                    return;
                }
            }
        }
    }

    /**
     * Called by the file size update callback of FileHandle.
     */
    protected void asyncFileSizeUpdateResponseHandler(OSDWriteResponse owr,
            FileHandleImplementation fileHandle, boolean success) {
        synchronized (osdWriteResponseLock) {
            // Only change the status of the OSDWriteResponse has not changed
            // meanwhile.
            if (Helper.compareOSDWriteResponses(owr, osdWriteResponse) == 0) {
                // The status must not have changed.
                assert (osdWriteResponseStatus == FilesizeUpdateStatus.kDirtyAndAsyncPending);
                if (success) {
                    osdWriteResponseStatus = FilesizeUpdateStatus.kClean;
                } else {
                    osdWriteResponseStatus = FilesizeUpdateStatus.kDirty; // Still dirty.
                }
            }
            // Always remove the temporary FileHandle.
            pendingFilesizeUpdates.remove(fileHandle);
            decreaseReferenceCount();
            if (pendingFilesizeUpdates.size() == 0) {
                osdWriteResponseLock.notifyAll();
            }
        }
    }

    /**
     * Compares "lock" against list of active locks.
     * 
     * Returns a {@link Tupel} where the first elements is the conflicting lock if such exits or null. The
     * second element is a array of three boolean value in the following order: conflictFound,
     * lockForPidCached, cachedLockForPidEqual.
     * 
     * conflictFound is set to true and the conflicting, active lock return if there is a conflict. If no
     * conflict was found, lockForPidCached is set to true if there exists already a lock for
     * lock.getClientPid(). Additionally, cachedLockForPidEqual" will be set to true, lock is equal to the
     * lock active for this pid.
     * 
     * 
     * @param lock
     *            The {@link Lock} the locks in the list of active locks should be compared with.
     * 
     * @return A Tupel<Lock, boolean[]>.
     */
    protected Tupel<Lock, boolean[]> checkLock(Lock lock) {
        assert (lock.getClientUuid().equals(clientUuid));

        boolean conflictFound = false;
        boolean lockForPidCached = false;
        boolean cachedLockForPidEqual = false;

        Lock conflictionLock = null;

        for (Entry<Integer, Lock> entry : activeLocks.entrySet()) {

            if (entry.getKey() == lock.getClientPid()) {
                lockForPidCached = true;
                if (Helper.checkIfLocksAreEqual(lock, entry.getValue())) {
                    cachedLockForPidEqual = true;
                }
                continue;
            }

            if (Helper.checkIfLocksDoConflict(lock, entry.getValue())) {
                conflictFound = true;
                conflictionLock = entry.getValue();
                // A conflicting lock has a higher priority than a cached lock with the same PID.
                break;
            }
        }

        boolean[] bools = new boolean[] { conflictFound, lockForPidCached, cachedLockForPidEqual };
        return new Tupel<Lock, boolean[]>(conflictionLock, bools);
    }

    /**
     * Returns true if a lock for "processId" is known.
     */
    protected boolean checkIfProcessHasLocks(int processId) {
        return activeLocks.containsKey(processId);
    }

    /**
     * Add a copy of "lock" to list of active locks.
     */
    protected void putLock(Lock lock) {
        assert (lock.getClientUuid().equals(clientUuid));

        // Delete lock if it already exists.
        activeLocks.remove(lock.getClientPid());

        // Insert copy of lock.
        Lock newLock = lock.toBuilder().build();
        activeLocks.put(newLock.getClientPid(), newLock);
    }

    /**
     * Remove locks equal to "lock" from list of active locks.
     */
    protected void delLock(Lock lock) {
        assert (lock.getClientUuid().equals(clientUuid));

        // There is only up to one element per PID. Just find and delete it.
        activeLocks.remove(lock.getClientPid());
    }

    /**
     * Flushes pending async writes and file size updates.
     */
    protected void flush(FileHandleImplementation fileHandle) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        flush(fileHandle, false);
    }

    /**
     * Same as Flush(), takes special actions if called by FileHandle.close().
     */
    protected void flush(FileHandleImplementation fileHandle, boolean closeFile) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        // We don't wait only for fileHandle's pending writes but for all writes of this file.
        waitForPendingAsyncWrites();
        flushPendingFileSizeUpdate(fileHandle, closeFile);
    }

    /**
     * Flushes a pending file size update.
     */
    protected void flushPendingFileSizeUpdate(FileHandleImplementation fileHandle) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        flushPendingFileSizeUpdate(fileHandle, false);
    }

    /**
     * Calls asyncWriteHandler.write().
     */
    void asyncWrite(AsyncWriteBuffer writeBuffer) throws XtreemFSException {
        if (asyncWriteHandler == null) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "Async writes are disabled for this volume.");
            throw new InternalServerErrorException("Async writes are disabled for this volume.");
        }

        asyncWriteHandler.write(writeBuffer);
    }

    /**
     * Calls asyncWriteHandler.waitForPendingWrites() (resulting in blocking until all pending async writes
     * are finished).
     */
    protected void waitForPendingAsyncWrites() {
        if (asyncWriteHandler != null) {
            asyncWriteHandler.waitForPendingWrites();
        }
    }

    /**
     * Same as flushPendingFileSizeUpdate(), takes special actions if called by close().
     * 
     * @throws IOException
     * @throws PosixErrorException
     */
    private void flushPendingFileSizeUpdate(FileHandleImplementation fileHandle, boolean closeFile)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // File size write back.
        OSDWriteResponse responseCopy = null;
        synchronized (osdWriteResponseLock) {
            if (osdWriteResponse != null) {
                waitForPendingFileSizeUpdates();
                if (osdWriteResponseStatus == FilesizeUpdateStatus.kDirty) {
                    osdWriteResponseStatus = FilesizeUpdateStatus.kDirtyAndSyncPending;
                    // Create a copy of OSDWriteResponse to pass to FileHandle.
                    responseCopy = osdWriteResponse.toBuilder().build();
                } else {
                    return;
                }
            } else {
                return;
            }
        }

        try {
            fileHandle.writeBackFileSize(responseCopy, closeFile);
        } catch (IOException e) {
            synchronized (osdWriteResponseLock) {
                osdWriteResponseStatus = FilesizeUpdateStatus.kDirty;
            }
            throw e;
        }

        synchronized (osdWriteResponseLock) {
            // Only update the status if the response object has not changed meanwhile.
            if (Helper.compareOSDWriteResponses(osdWriteResponse, responseCopy) == 0) {
                osdWriteResponseStatus = FilesizeUpdateStatus.kClean;
            }

            boolean replicateOnCloseValue = false;
            synchronized (xLocSetLock) {
                replicateOnCloseValue = this.replicateOnClose; 
            }
            if (closeFile && replicateOnCloseValue) {
                // Send an explicit close only if the on-close-replication should be
                // triggered. Use an empty OSDWriteResponse object therefore.
                fileHandle.writeBackFileSize(OSDWriteResponse.getDefaultInstance(), closeFile);
            }
        }
    }

    /**
     * Returns a new copy of xlocSet.
     */
    protected XLocSet getXLocSet() {
        synchronized (xLocSetLock) {
            return xlocset.toBuilder().build();
        }
    }
}
