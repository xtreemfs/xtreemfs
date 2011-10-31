package org.xtreemfs.common.libxtreemfs;

/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;

/**
 * 
 * <br>
 * Oct 27, 2011
 */
public class AsyncWriteHandler {

    /**
     * Possible states of this object.
     */
    private enum State {
        IDLE, WRITES_PENDING
    }

    /**
     * Use this when modifying the object.
     */
    private java.util.concurrent.locks.Lock asyncWriteHandlerLock;

    /**
     * State of this object.
     */
    private State                           state;

    /**
     * List of pending writes.
     */
    // TODO(mberlin): Limit the size of writes in flight to avoid flooding.
    private List<AsyncWriteBuffer>          writesInFlight;

    /**
     * Number of pending bytes.
     */
    private int                             pendingBytes;

    /**
     * Set by WaitForPendingWrites{NonBlocking}() to true if there are temporarily no new async writes allowed
     * and will be set to false again once the state IDLE is reached.
     */
    private boolean                         writingPaused;

    /**
     * Used to notify blocked WaitForPendingWrites() callers for the state change back to IDLE.
     */
    private Condition                       allPendingWritesDidComplete;

    /**
     * Number of threads blocked by WaitForPendingWrites() waiting on all_pending_writes_did_complete_ for a
     * state change back to IDLE.
     * 
     * This does not include the number of waiting threads which did call WaitForPendingWritesNonBlocking().
     * Therefore, see "waiting_observers_". The total number of all waiting threads is:
     * waiting_blocking_threads_count_ + waiting_observers_.size()
     */
    private int                             waitingBlockingThreadsCount;

    /**
     * Used to notify blocked write() callers that the number of pending bytes has decreased.
     */
    private Condition                       pendingBytesWereDecreased;

    /**
     * List of waitForPendingWritesNonBlocking() observers (specified by their boost::condition variable and
     * their bool value which will be set to true if the state changed back to IDLE).
     */
    // TODO: DO IT.
    // List<WaitForCompletionObserver> waitingObservers;

    /**
     * FileInfo object to which this AsyncWriteHandler does belong. Accessed for file size updates.
     */
    private FileInfo                        fileInfo;

    /**
     * Pointer to the UUIDIterator of the FileInfo object.
     */
    private UUIDIterator                    uuidIterator;

    /**
     * Required for resolving UUIDs to addresses.
     */
    private UUIDResolver                    uuidResolver;

    /**
     * Client which is used to send out the writes.
     */
    OSDServiceClient                        osdServiceClient;

    /**
     * Auth needed for ServiceClients. Always set to AUTH_NONE by Volume.
     */
    private Auth                            authBogus;

    /**
     * For same reason needed as authBogus. Always set to user "xtreemfs".
     */
    private UserCredentials                 userCredentialsBogus;

    /**
     * Maximum number in bytes which may be pending.
     */
    private int                             maxWriteahead;

    /**
     * Maximum number of pending write requests.
     */
    private int                             maxWriteaheadRequests;

    /**
     * Maximum number of attempts a write will be tried.
     */
    private int                             maxWriteTries;

    protected AsyncWriteHandler(FileInfo fileInfo, UUIDIterator uuidIterator, UUIDResolver uuidResolver,
            OSDServiceClient osdServiceClient, Auth authBogus, UserCredentials userCredentialsBogus,
            int maxWriteahead, int maxWriteaheadRequests, int maxWriteTries) {

        this.fileInfo = fileInfo;
        this.uuidIterator = uuidIterator;
        this.uuidResolver = uuidResolver;
        this.osdServiceClient = osdServiceClient;
        this.authBogus = authBogus;
        this.userCredentialsBogus = userCredentialsBogus;
        this.maxWriteahead = maxWriteahead;
        this.maxWriteaheadRequests = maxWriteaheadRequests;
        this.maxWriteTries = maxWriteTries;
        
        
        asyncWriteHandlerLock = new ReentrantLock();
        allPendingWritesDidComplete = asyncWriteHandlerLock.newCondition();

    }

    /**
     * Adds write_buffer to the list of pending writes and sends it to the OSD specified by
     * writeBuffer.uuidIterator (or write_buffer.osdUuid if writeBuffer.useUuidIterator is false).
     * 
     * Blocks if the number of pending bytes exceeds the maximum write-ahead or
     * waitForPendingWrites{NonBlocking}() was called beforehand.
     */
    protected void write(AsyncWriteBuffer writeBuffer) {
        // TODO: Implement.
    }

    /**
     * Blocks until state changes back to IDLE and prevents allowing new writes. by blocking further write()
     * calls.
     */
    protected void waitForPendingWrites() {
        asyncWriteHandlerLock.lock();
        try {
            if (state != State.IDLE) {
                writingPaused = false;
                waitingBlockingThreadsCount++;
                while (state != State.IDLE) {
                    try {
                        allPendingWritesDidComplete.wait();    
                    } catch (InterruptedException e) {
                        // TODO: REALLY handle exception.
                    }            
                }
                waitingBlockingThreadsCount--;
            }
        } finally {
            asyncWriteHandlerLock.unlock();
        }
    }

    /**
     * If waiting for pending writes would block, it returns true and adds the parameters to the list
     * waitingObservers and calls notifyOne() on "conditionVariable" once "state" changed back to IDLE.
     */
    protected boolean waitForPendingWritesNonBlocking(Condition conditionVariable, boolean waitCompleted,
            Lock waitCompletedMutex) {
        // TODO: Implement.

        return false;
    }

    // TODO: DO i need this? oO
    // /** Contains information about observer who has to be notified once all
    // * currently pending writes have finished. */
    // struct WaitForCompletionObserver {
    // WaitForCompletionObserver(boost::condition* condition_variable,
    // bool* wait_completed,
    // boost::mutex* wait_completed_mutex)
    // : condition_variable(condition_variable),
    // wait_completed(wait_completed),
    // wait_completed_mutex(wait_completed_mutex) {
    // assert(condition_variable && wait_completed && wait_completed_mutex);
    // }
    // boost::condition* condition_variable;
    // bool* wait_completed;
    // boost::mutex* wait_completed_mutex;
    // };
    //
    // /** Implements callback for an async write request. */
    // virtual void CallFinished(xtreemfs::pbrpc::OSDWriteResponse* response_message,
    // char* data, boost::uint32_t data_length,
    // xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
    // void* context);

    /**
     * Helper function which adds "writeBuffer" to the list "writesInFlight", increases the number of pending
     * bytes and takes care of state changes.
     * 
     * @remark Ownership is not transferred to the caller.
     * @remark Requires a lock on "lock".
     */
    protected void increasePendingBytesHelper(AsyncWriteBuffer writeBuffer) {

        assert (writeBuffer != null);

        pendingBytes += writeBuffer.getDataLength();
        writesInFlight.add(writeBuffer);

        assert (writesInFlight.size() <= maxWriteaheadRequests);

        state = State.WRITES_PENDING;
    }

    /**
     * Helper function which removes "writeBuffer" from the list "writesInFlight", deletes "writeBuffer",
     * reduces the number of pending bytes and takes care of state changes.
     * 
     * @remark Ownership of "writeBuffer" is transferred to the caller.
     * @remark Requires a lock on "asyncWriteHandlerLock".
     */
    private void decreasePendingBytesHelper(AsyncWriteBuffer writeBuffer) {
        assert (writeBuffer != null);

        writesInFlight.remove(writeBuffer);
        pendingBytes -= writeBuffer.getDataLength();

        if (pendingBytes == 0) {
            state = State.IDLE;
            if (writingPaused) {
                writingPaused = false;
                notifyWaitingObserversAndClearAll();
            }
            // Issue notifyAll() as long as there are remaining blocked threads.
            //
            // Please note the following here: After the two notifyAll()s on the
            // condition variables "allPendingWritesDidComplete and
            // pendingBytesWereDecreased, two different thread types
            // (waiting blocked ones AND further waiting writes) do race for
            // re-acquiring the lock on mutex_.
            // Example:
            // T1: write1 "state" = PENDING
            // T2: getattr "writingPaused" = true => blocked as "state" != IDLE
            // T1: write2 => blocked as "writingPaused" = true
            // Tx: write1 callback: "state" = IDLE, writing_paused_ = false
            // T1: write2 succeeds to obtain lock on mutex_ *before* getattr
            // => state = IDLE (writing_paused_ remains false)
            // Tx: write2 callback: state = IDLE, writing paused remains false
            // - however its necessary to notify the blocked getattr.
            // As you can see the order of concurrent writes and reads/getattrs
            // is undefined and we don't enforce any order as it's up to the user to
            // synchronize his threads himself when working on the same file.
            if (waitingBlockingThreadsCount > 0) {
                allPendingWritesDidComplete.notifyAll();
            }
        }
        // Tell blocked writers there may be enough space/writing was unpaused now.
        pendingBytesWereDecreased.notifyAll();
    }

    /**
     * Calls notify_one() on all observers in waiting_observers_, frees each element in the list and clears
     * the list afterwards.
     * 
     * @remark Requires a lock on "lock".
     */
    private void notifyWaitingObserversAndClearAll() {

        // TODO: Implement.
        // Tell waiting observers, that the write did finish.

    }

    protected void callFinished(OSDWriteResponse responseMessage, char[] data, int dataLengt,
            ErrorResponse error, AsyncWriteBuffer context) {

        AsyncWriteBuffer writeBuffer = context;

        if (error != null) {
            // An error occured.
            // No retry supported yet, just acknowledge the write as failed.

            // Tell fileHandle that its async write status is broken from now on.
            writeBuffer.getFileHandle().markAsyncWritesAsFailed();

            // Log error
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                    "An async write sent to the server %s failed. Error type: %s  "
                            + "Error message %s   Complete error header: %s", writeBuffer.getOsdUuid(), error
                            .getErrorType().toString(), error.getErrorMessage(), error.getDebugInfo());
            
            asyncWriteHandlerLock.lock();
            try {
                decreasePendingBytesHelper(writeBuffer);
            } finally {
                asyncWriteHandlerLock.unlock();
            }
        } else {
            // Write was successful 
            
            // Tell FileInfo about the OSDWriteResponse.
            if (responseMessage.hasSizeInBytes()) {
                XCap xcap = writeBuffer.getFileHandle().getXcap();
                
                fileInfo.tryToUpdateOSDWriteResponse(responseMessage, xcap);
            }
            
            asyncWriteHandlerLock.lock();
            try {
                decreasePendingBytesHelper(writeBuffer);
            } finally {
                asyncWriteHandlerLock.unlock();
            }
        }
    }
}
