package org.xtreemfs.common.libxtreemfs;

/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.UUIDIteratorListIsEmpyException;
import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import com.google.protobuf.Descriptors.EnumValueDescriptor;

//JCIP import net.jcip.annotations.GuardedBy;

/**
 * Handles asynchronous writes. Used only internally.
 */
public class AsyncWriteHandler {

    /**
     * Possible states of this object.
     */
    private enum State {
        IDLE, WRITES_PENDING
    }

    /**
     * State of this object.
     */
// JCIP     @GuardedBy("this")
    private State                  state;

    /**
     * List of pending writes.
     */
    // TODO(mberlin): Limit the size of writes in flight to avoid flooding.
// JCIP     @GuardedBy("this")
    private List<AsyncWriteBuffer> writesInFlight;

    /**
     * Number of pending bytes.
     */
// JCIP     @GuardedBy("this")
    private int                    pendingBytes;

    /**
     * Set by WaitForPendingWrites() to true if there are temporarily no new async writes allowed and will be
     * set to false again once the state IDLE is reached.
     */
// JCIP     @GuardedBy("this")
    private boolean                writingPaused;

    /**
     * Used to notify blocked WaitForPendingWrites() callers for the state change back to IDLE.
     */
    private Object                 allPendingWritesDidComplete;

    /**
     * Number of threads blocked by WaitForPendingWrites() waiting on allPendingWritesDidComplete for a state
     * change back to IDLE.
     */
// JCIP     @GuardedBy("this")
    private int                    waitingBlockingThreadsCount;

    /**
     * FileInfo object to which this AsyncWriteHandler does belong. Accessed for file size updates.
     */
    private FileInfo               fileInfo;

    /**
     * Pointer to the UUIDIterator of the FileInfo object.
     */
    private UUIDIterator           uuidIterator;

    /**
     * Required for resolving UUIDs to addresses.
     */
    private UUIDResolver           uuidResolver;

    /**
     * Client which is used to send out the writes.
     */
    OSDServiceClient               osdServiceClient;

    /**
     * Auth needed for ServiceClients. Always set to AUTH_NONE by Volume.
     */
    private Auth                   authBogus;

    /**
     * For same reason needed as authBogus. Always set to user "xtreemfs".
     */
    private UserCredentials        userCredentialsBogus;

    /**
     * Maximum number in bytes which may be pending.
     */
    private int                    maxWriteahead;

    /**
     * Maximum number of pending write requests.
     */
    private int                    maxWriteaheadRequests;

    /**
     * Maximum number of attempts a write will be tried.
     */
    private int                    maxWriteTries;

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

        writesInFlight = new ArrayList<AsyncWriteBuffer>();
        allPendingWritesDidComplete = new Object();
        state = State.IDLE;
    }

    /**
     * Adds writeBuffer to the list of pending writes and sends it to the OSD specified by
     * writeBuffer.uuidIterator (or write_buffer.osdUuid if writeBuffer.useUuidIterator is false).
     * 
     * Blocks if the number of pending bytes exceeds the maximum write-ahead or waitForPendingWrites() was
     * called beforehand.
     */
    protected void write(AsyncWriteBuffer writeBuffer) throws AddressToUUIDNotFoundException,
            XtreemFSException {
        assert (writeBuffer != null);

        if (writeBuffer.getDataLength() > maxWriteahead) {
            throw new XtreemFSException("The maximum allowed writeahead size: " + maxWriteahead
                    + " is smaller than the size of this write request: " + writeBuffer.getDataLength());
        }

        // append to the list of write in flight
        synchronized (this) {
            while (writingPaused || (pendingBytes + writeBuffer.getDataLength()) > maxWriteahead
                    || writesInFlight.size() == maxWriteaheadRequests) {
                // TODO: Allow interruption and set the write status of the FileHandle of the
                // interrupted write to an error state.
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // TODO: handle exception
                }
            }
            increasePendingBytesHelper(writeBuffer);
        }

        String osdUuid = retrieveOSDUuidAndSetItInWriteBuffer(writeBuffer);
        String osdAddress = uuidResolver.uuidToAddress(osdUuid);
        InetSocketAddress osdInetSocketAddress =
                Helper.stringToInetSocketAddress(osdAddress,
                        GlobalTypes.PORTS.OSD_PBRPC_PORT_DEFAULT.getNumber());

        // Sending request
        final AsyncWriteBuffer finalWriteBufferForCallback = writeBuffer;
        RPCResponse<OSDWriteResponse> response;
        try {
            response =
                    osdServiceClient.write(osdInetSocketAddress, authBogus, userCredentialsBogus,
                            writeBuffer.getWriteRequest(), writeBuffer.getData());

            response.registerListener(new RPCResponseAvailableListener<OSDWriteResponse>() {
                @Override
                public void responseAvailable(RPCResponse<OSDWriteResponse> r) {
                    try {
                        OSDWriteResponse osdWriteResponse = r.get();
                        writeFinished(osdWriteResponse, r.getData(), finalWriteBufferForCallback);
                    } catch (PBRPCException e) {
                        String errorTypeName = e.getErrorType().toString();
                        EnumValueDescriptor enumDescriptor =
                                ErrorType.getDescriptor().findValueByNumber(e.getErrorType().getNumber());
                        if (enumDescriptor != null) {
                            errorTypeName = enumDescriptor.getName();
                        }
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                                "An async write sent to the server %s failed."
                                        + " Error type:  %s Error message: %s Complete error header: %s",
                                finalWriteBufferForCallback.getOsdUuid(), errorTypeName, e.getErrorMessage(),
                                e.getDebugInfo());
                        System.out.println("CLASSNAME: " + this.toString());
                        decreasePendingBytesHelper(finalWriteBufferForCallback);
                    } catch (Exception e) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, "asyncWrite:"
                                + " failed due to the following reasons ", e.getMessage());
                        decreasePendingBytesHelper(finalWriteBufferForCallback);

                    } finally {
                        r.freeBuffers();
                    }
                }
            });
        } catch (IOException e1) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, "asyncWrite:"
                    + " failed due to the following reasons ", e1.getMessage());
            decreasePendingBytesHelper(finalWriteBufferForCallback);
        }
    }

    private String retrieveOSDUuidAndSetItInWriteBuffer(AsyncWriteBuffer writeBuffer)
            throws UUIDIteratorListIsEmpyException {
        String osdUuid;
        if (writeBuffer.isUsingUuidIterator()) {
            osdUuid = uuidIterator.getUUID();
            // Store used OSD in writeBuffer for the callback.
            writeBuffer.setOsdUuid(osdUuid);
        } else {
            osdUuid = writeBuffer.getOsdUuid();
        }
        return osdUuid;
    }

    /**
     * Blocks until state changes back to IDLE and prevents allowing new writes. by blocking further write()
     * calls.
     */
    protected void waitForPendingWrites() {
        synchronized (this) {
            if (state != State.IDLE) {
                writingPaused = false;
                waitingBlockingThreadsCount++;
            } else {
                return;
            }
        }

        while (state != State.IDLE) {
            synchronized (allPendingWritesDidComplete) {
                try {
                    allPendingWritesDidComplete.wait();
                } catch (InterruptedException e) {
                    // TODO: REALLY handle exception.
                    e.printStackTrace();
                }
            }
        }

        synchronized (this) {
            waitingBlockingThreadsCount--;
        }
    }

    /**
     * Implements callback for an async write request.
     */
    private void writeFinished(OSDWriteResponse response, ReusableBuffer data, AsyncWriteBuffer writeBuffer) {
        // Tell FileInfo about the OSDWriteResponse.
        if (response.hasSizeInBytes()) {
            XCap xcap = writeBuffer.getFileHandle().getXcap();
            fileInfo.tryToUpdateOSDWriteResponse(response, xcap);
        }

        decreasePendingBytesHelper(writeBuffer);
    }

    /**
     * Helper function which adds "writeBuffer" to the list "writesInFlight", increases the number of pending
     * bytes and takes care of state changes.
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
    private synchronized void decreasePendingBytesHelper(AsyncWriteBuffer writeBuffer) {
        assert (writeBuffer != null);

        writesInFlight.remove(writeBuffer);
        pendingBytes -= writeBuffer.getDataLength();

        if (pendingBytes == 0) {
            state = State.IDLE;
            if (writingPaused) {
                writingPaused = false;
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
                synchronized (allPendingWritesDidComplete) {
                    allPendingWritesDidComplete.notifyAll();
                }
            }
        }
        // Tell blocked writers there may be enough space/writing was unpaused now.
        this.notifyAll();
    }
}
