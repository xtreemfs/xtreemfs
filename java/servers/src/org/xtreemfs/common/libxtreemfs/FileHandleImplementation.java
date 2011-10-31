/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.statvfsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.timestampResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.lockRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.truncateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * 
 * <br>
 * Sep 12, 2011
 */
public class FileHandleImplementation extends FileHandle {

    /**
     * UUID of the Client (needed to distinguish Locks of different clients).
     */
    private String                                clientUuid;

    /**
     * UUIDIterator of the MRC.
     */
    private UUIDIterator                          mrcUuidIterator;

    /**
     * UUIDIterator which contains the UUIDs of all replicas.
     */
    private UUIDIterator                          osdUuidIterator;

    /**
     * Needed to resolve UUIDs.
     */
    private UUIDResolver                          uuidResolver;

    /**
     * Multiple FileHandle may refer to the same File and therefore unique file properties (e.g. Path, FileId,
     * XlocSet) are stored in a FileInfo object.
     */
    private FileInfo                              fileInfo;

    // TODO(mberlin): Add flags member.

    /**
     * Capabilitiy for the file, used to authorize against services
     */
    private XCap                                  xcap;

    /**
     * True if there is an outstanding xcap_renew callback.
     */
    private boolean                               xcapRenewalPending;

    /**
     * Used to wait for pending XCap renewal callbacks.
     */
    // boost::condition xcap_renewal_pending_cond_;

    /**
     * Contains a file size update which has to be written back (or NULL).
     */
    private OSDWriteResponse                      osdWriteResponseForAsyncWriteBack;

    /**
     * MRCServiceClient from the VolumeImplemention
     */
    private MRCServiceClient                      mrcServiceClient;

    /**
     * Pointer to object owned by VolumeImplemention
     */
    private OSDServiceClient                      osdServiceClient;

    // TODO: Figure out if this is needed and add it to constructor if neccessary.
    // const std::map<xtreemfs::pbrpc::StripingPolicyType,
    // StripeTranslator*>& stripe_translators_;

    /**
     * Set to true if async writes (max requests > 0, no O_SYNC) are enabled.
     */
    private boolean                               asyncWritesEnabled;

    /**
     * Set to true if an async write of this file_handle failed. If true, this file_handle is broken and no
     * further writes/reads/truncates are possible.
     */
    private boolean                               asyncWritesFailed;

    private Options                               volumeOptions;

    /**
     * Auth needed for ServiceClients. Always set to AUTH_NONE by Volume.
     */
    private Auth                                  authBogus;

    /**
     * For same reason needed as authBogus. Always set to user "xtreemfs".
     */
    private UserCredentials                       userCredentialsBogus;

    /**
     * All modifications to this object must aquire a Lock first.
     */
    private java.util.concurrent.locks.Lock fileHandleLock;

    /**
     * 
     */
    public FileHandleImplementation(String clientUuid, FileInfo fileInfo, XCap xcap,
            UUIDIterator mrcUuidIterator, UUIDIterator osdUuidIterator, UUIDResolver uuidResolver,
            MRCServiceClient mrcServiceClient, OSDServiceClient osdServiceClient, boolean asyncWritesEnabled,
            Options options, Auth authBogus, UserCredentials userCredentialsBogus) {

        this.clientUuid = clientUuid;
        this.fileInfo = fileInfo;
        this.xcap = xcap;
        this.mrcUuidIterator = mrcUuidIterator;
        this.osdUuidIterator = osdUuidIterator;
        this.uuidResolver = uuidResolver;
        this.asyncWritesEnabled = asyncWritesEnabled;
        this.volumeOptions = options;
        this.authBogus = authBogus;
        this.userCredentialsBogus = userCredentialsBogus;
        
        fileHandleLock = new ReentrantLock();

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#read(org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC
     * .UserCredentials, org.xtreemfs.foundation.buffer.ReusableBuffer, int, int)
     */
    @Override
    public int read(UserCredentials userCredentials, ReusableBuffer buf, int count, int offset)
            throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#write(org.xtreemfs.foundation.pbrpc.generatedinterfaces.
     * RPC.UserCredentials, org.xtreemfs.foundation.buffer.ReusableBuffer, int, int)
     */
    @Override
    public synchronized int write(UserCredentials userCredentials, ReusableBuffer buf, int count, int offset)
            throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#flush()
     */
    @Override
    public void flush() throws IOException, PosixErrorException {
        flush(false);
    }

    protected void flush(boolean closeFile) throws IOException, PosixErrorException {
        fileInfo.flush(this, closeFile);

        fileHandleLock.lock();
        try {
            if (asyncWritesFailed) {

                String error = "Flush for file " + fileInfo.getPath() + "did not succeed flushing "
                        + "all pending writes as at least one asynchronous write did fail";

                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, error);
                throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EIO, error);
            }
        } finally {
            fileHandleLock.unlock();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#truncate(org.xtreemfs.foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int)
     */
    @Override
    public void truncate(UserCredentials userCredentials, long newFileSize) throws IOException,
            PosixErrorException {

        fileInfo.waitForPendingAsyncWrites();

        XCap xcapCopy;

        fileHandleLock.lock();
        try {
            if (asyncWritesFailed) {
                throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EIO, "A previous asynchronous "
                        + "write did fail.No further action on this file handle are allowed.");
            }
            xcapCopy = xcap.toBuilder().build();

        } finally {
            fileHandleLock.unlock();
        }

        // 1. Call truncate at the MRC (in order to increase the trunc epoch).
        try {
            Method m = MRCServiceClient.class.getDeclaredMethod("ftruncate", new Class<?>[] {
                    InetSocketAddress.class, Auth.class, UserCredentials.class, XCap.class });

            RPCCaller.<MRCServiceClient, XCap, XCap> makeCall(mrcServiceClient, m, userCredentialsBogus,
                    authBogus, xcapCopy, mrcUuidIterator, uuidResolver, volumeOptions.getMaxTries(),
                    volumeOptions, false);
        } catch (NoSuchMethodException nsm) {
            // should never happen unless there is a programming error
            nsm.printStackTrace();
        } catch (SecurityException se) {
            // should never happen unless there is a programming error
            se.printStackTrace();
        } catch (Exception e) {
            throw new IOException(e);
        }

        truncatePhaseTwoAndThree(userCredentials, newFileSize);
    }

    /**
     * Used by truncate() and Volume.openFile() to truncate the file to "newFileSize" on the OSD and update
     * the file size at the MRC.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * @throws UnknownAddressSchemeException
     **/
    private void truncatePhaseTwoAndThree(UserCredentials userCredentials, long newFileSize)
            throws IOException {
        // 2. Call truncate at the head OSD.
        truncateRequest.Builder requestBuilder = truncateRequest.newBuilder();
        FileCredentials.Builder fileCredentialsBuilder = FileCredentials.newBuilder();
        // TODO: Must toBuilder.build() be called???
        fileCredentialsBuilder.setXlocs(fileInfo.getXLocSet().toBuilder().build());

        fileHandleLock.lock();
        try {
            fileCredentialsBuilder.setXcap(xcap.toBuilder().build());
        } finally {
            fileHandleLock.unlock();
        }

        requestBuilder.setFileId(fileCredentialsBuilder.getXcap().getFileId());
        requestBuilder.setFileCredentials(fileCredentialsBuilder.build());
        requestBuilder.setNewFileSize(newFileSize);

        OSDWriteResponse response = null;
        try {
            Method m = OSDServiceClient.class.getDeclaredMethod("truncate", new Class<?>[] {
                    InetSocketAddress.class, Auth.class, UserCredentials.class, truncateRequest.class });

            response = RPCCaller.<OSDServiceClient, truncateRequest, OSDWriteResponse> makeCall(
                    osdServiceClient, m, userCredentialsBogus, authBogus, requestBuilder.build(),
                    osdUuidIterator, uuidResolver, volumeOptions.getMaxTries(), volumeOptions, false);
        } catch (NoSuchMethodException nsm) {
            // should never happen unless there is a programming error
            nsm.printStackTrace();
        } catch (SecurityException se) {
            // should never happen unless there is a programming error
            se.printStackTrace();
        } catch (Exception e) {
            throw new IOException(e);
        }

        assert (response != null);
        assert (response.hasSizeInBytes());

        // register the new OSDWriteResponse to this file's FileInfo.
        // TODO: This must not be locked?! oO In cpp implementation it isn't.
        fileInfo.tryToUpdateOSDWriteResponse(response, xcap);

        // 3. Update the file size at the MRC.
        fileInfo.flushPendingFileSizeUpdate(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#getAttr(org.xtreemfs.foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials)
     */
    @Override
    public Stat getAttr(UserCredentials userCredentials) throws IOException {
        return fileInfo.getAttr(userCredentials);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#acquireLock(org.xtreemfs.foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int, long, long, boolean, boolean)
     */
    @Override
    public Lock acquireLock(UserCredentials userCredentials, int processId, long offset, long length,
            boolean exclusive, boolean waitForLock) throws IOException, PosixErrorException {

        // Create Lock object for the acquire lock request.
        Lock.Builder lockBuilder = Lock.newBuilder();
        lockBuilder.setClientUuid(clientUuid);
        lockBuilder.setClientPid(processId);
        lockBuilder.setOffset(offset);
        lockBuilder.setLength(length);
        lockBuilder.setExclusive(exclusive);

        Lock lock = lockBuilder.build();

        // Check active locks first.
        Tupel<Lock, boolean[]> checkLockReturn = fileInfo.checkLock(lock);
        boolean conflictFound = checkLockReturn.getSecond()[0];
        boolean cachedLockForPidEqual = checkLockReturn.getSecond()[2];

        if (conflictFound) {
            throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EAGAIN, "conflicting lock");
        }

        // We allow only one lock per PID, i.e. an existing lock can be always
        // overwritten. In consequence, acquireLock() always has to be executed except
        // the new lock is equal to the current lock.
        if (cachedLockForPidEqual) {
            return lock;
        }

        // Cache could not be used. Create FileCredentials, complete lockRequest and send to OSD.
        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        fcBuilder.setXlocs(fileInfo.getXLocSet());
        fileHandleLock.lock();
        try {
            fcBuilder.setXcap(xcap.toBuilder().build());
        } finally {
            fileHandleLock.unlock();
        }

        lockRequest request = lockRequest.newBuilder().setLockRequest(lock)
                .setFileCredentials(fcBuilder.build()).build();

        Lock response = null;
        if (!waitForLock) {
            try {
                Method m = OSDServiceClient.class.getDeclaredMethod("xtreemfs_lock_acquire", new Class<?>[] {
                        InetSocketAddress.class, Auth.class, UserCredentials.class, lockRequest.class });

                response = RPCCaller.<OSDServiceClient, lockRequest, Lock> makeCall(osdServiceClient, m,
                        userCredentialsBogus, authBogus, request, osdUuidIterator, uuidResolver,
                        volumeOptions.getMaxTries(), volumeOptions, false);
            } catch (NoSuchMethodException nsm) {
                // should never happen unless there is a programming error
                nsm.printStackTrace();
            } catch (SecurityException se) {
                // should never happen unless there is a programming error
                se.printStackTrace();
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            // Retry to obtain the lock in case of EAGAIN responses.\
            int retriesLeft = volumeOptions.getMaxTries();
            while (retriesLeft >= 0) {
                retriesLeft--;
                try {
                    Method m = OSDServiceClient.class.getDeclaredMethod("xtreemfs_lock_acquire",
                            new Class<?>[] { InetSocketAddress.class, Auth.class, UserCredentials.class,
                                    lockRequest.class });

                    response = RPCCaller.<OSDServiceClient, lockRequest, Lock> makeCall(osdServiceClient, m,
                            userCredentialsBogus, authBogus, request, osdUuidIterator, uuidResolver,
                            volumeOptions.getMaxTries(), volumeOptions, false, true);

                    // break if there is no error.
                    break;
                } catch (NoSuchMethodException nsm) {
                    // should never happen unless there is a programming error
                    nsm.printStackTrace();
                } catch (SecurityException se) {
                    // should never happen unless there is a programming error
                    se.printStackTrace();
                } catch (PosixErrorException pe) {
                    if (!pe.getPosixError().equals(POSIXErrno.POSIX_ERROR_EAGAIN)) {
                        // TODO: makeCall() does not throw PosixError yet. Find out how to find out
                        // if there was an error and extract posix error code from the error response.

                        // Only retry if there exists a conflicting lock and the server did
                        // return an EAGAIN - otherwise rethrow the exception.
                        throw pe;
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        }

        // "Cache" new lock.
        fileInfo.putLock(response);

        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#checkLock(org.xtreemfs.foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int, long, long, boolean)
     */
    @Override
    public Lock checkLock(UserCredentials userCredentials, int processId, long offset, long length,
            boolean exclusive) throws IOException {

        // Create lock object for the check lock request.
        Lock.Builder lockBuilder = Lock.newBuilder();
        lockBuilder.setClientUuid(clientUuid);
        lockBuilder.setClientPid(processId);
        lockBuilder.setOffset(offset);
        lockBuilder.setLength(length);
        lockBuilder.setExclusive(exclusive);

        Lock lock = lockBuilder.build();

        // Check active locks first.
        Tupel<Lock, boolean[]> checkLockReturn = fileInfo.checkLock(lock);
        Lock conflictingLock = checkLockReturn.getFirst();
        boolean conflictFound = checkLockReturn.getSecond()[0];
        boolean lockForPidCached = checkLockReturn.getSecond()[1];

        if (conflictFound) {
            return conflictingLock;
        }

        // We allow only one lock per PID, i.e. an existing lock can be always
        // overwritten.
        if (lockForPidCached) {
            return lock;
        }

        // Cache could not be used. Create lockRequest and send to OSD.
        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        fcBuilder.setXlocs(fileInfo.getXLocSet());
        fileHandleLock.lock();
        try {
            fcBuilder.setXcap(xcap.toBuilder().build());
        } finally {
            fileHandleLock.unlock();
        }

        lockRequest request = lockRequest.newBuilder().setLockRequest(lock)
                .setFileCredentials(fcBuilder.build()).build();

        Lock response = null;
        try {
            Method m = OSDServiceClient.class.getDeclaredMethod("xtreemfs_lock_check", new Class<?>[] {
                    InetSocketAddress.class, Auth.class, UserCredentials.class, lockRequest.class });

            response = RPCCaller.<OSDServiceClient, lockRequest, Lock> makeCall(osdServiceClient, m,
                    userCredentialsBogus, authBogus, request, osdUuidIterator, uuidResolver,
                    volumeOptions.getMaxTries(), volumeOptions, false);
        } catch (NoSuchMethodException nsm) {
            // should never happen unless there is a programming error
            nsm.printStackTrace();
        } catch (SecurityException se) {
            // should never happen unless there is a programming error
            se.printStackTrace();
        } catch (Exception e) {
            throw new IOException(e);
        }

        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#releaseLock(org.xtreemfs.foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int, long, long, boolean)
     */
    @Override
    public void releaseLock(UserCredentials userCredentials, int processId, long offset, long length,
            boolean exclusive) throws IOException {

        Lock.Builder lockBuilder = Lock.newBuilder();
        lockBuilder.setClientUuid(clientUuid);
        lockBuilder.setClientPid(processId);
        lockBuilder.setOffset(offset);
        lockBuilder.setLength(length);
        lockBuilder.setExclusive(exclusive);
        releaseLock(userCredentials, lockBuilder.build());

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#releaseLock(org.xtreemfs.foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock)
     */
    @Override
    public void releaseLock(UserCredentials userCredentials, Lock lock) throws IOException {
        // Only release locks which are known to this client.
        if (!fileInfo.checkIfProcessHasLocks(lock.getClientPid())) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "FileHandleImplementation.releaseLock(): Skipping unlock request as there"
                                + " is no lock known for PID: %s (Lock description: %s, %s ,%s)",
                        lock.getClientPid(), lock.getOffset(), lock.getLength(), lock.getExclusive());
            }
            return;
        }

        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        fcBuilder.setXlocs(fileInfo.getXLocSet());
        fileHandleLock.lock();
        try {
            fcBuilder.setXcap(xcap.toBuilder().build());
        } finally {
            fileHandleLock.unlock();
        }

        lockRequest unlockRequest = lockRequest.newBuilder().setFileCredentials(fcBuilder.build())
                .setLockRequest(lock).build();
        try {
            Method m = OSDServiceClient.class.getDeclaredMethod("xtreemfs_lock_release", new Class<?>[] {
                    InetSocketAddress.class, Auth.class, UserCredentials.class, lockRequest.class });

            RPCCaller.<OSDServiceClient, lockRequest, emptyResponse> makeCall(osdServiceClient, m,
                    userCredentialsBogus, authBogus, unlockRequest, osdUuidIterator, uuidResolver,
                    volumeOptions.getMaxTries(), volumeOptions, false);
        } catch (NoSuchMethodException nsm) {
            // should never happen unless there is a programming error
            nsm.printStackTrace();
        } catch (SecurityException se) {
            // should never happen unless there is a programming error
            se.printStackTrace();
        } catch (Exception e) {
            throw new IOException(e);
        }
        
        fileInfo.delLock(lock);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#releaseLockOfProcess(int)
     */
    @Override
    public void releaseLockOfProcess(int processId) throws IOException {
        fileInfo.releaseLockOfProcess(this, processId);

    }

    /**
     * Releases "lock" with userCredentials from this fileHandle object.
     * 
     * @param lock
     */
    protected void releaseLock(Lock lock) throws IOException {
        releaseLock(userCredentialsBogus, lock);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#pingReplica(org.xtreemfs.foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, java.lang.String)
     */
    @Override
    public void pingReplica(UserCredentials userCredentials, String osdUuid) throws IOException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#close()
     */
    @Override
    public void close() throws IOException {
        // TODO Do real implementation, this is just to get a test working.

    }

    protected void writeBackFileSizeAsync() throws IOException {
        xtreemfs_update_file_sizeRequest.Builder rqBuilder = xtreemfs_update_file_sizeRequest.newBuilder();

        fileHandleLock.lock();
        try {
            if (osdWriteResponseForAsyncWriteBack == null) {
                return;
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "updateFileSize: %s "
                        + "#bytes: %s", fileInfo.getPath(),
                        osdWriteResponseForAsyncWriteBack.getSizeInBytes());
            }

            rqBuilder.setXcap(xcap).setOsdWriteResponse(osdWriteResponseForAsyncWriteBack.toBuilder());
        } finally {
            fileHandleLock.unlock();
        }

        // set close file to false because true implies synchronous call.
        rqBuilder.setCloseFile(false);

        try {
            Method m = MRCServiceClient.class.getDeclaredMethod("xtreemfs_update_filesize", new Class<?>[] {
                    InetSocketAddress.class, Auth.class, UserCredentials.class,
                    xtreemfs_update_file_sizeRequest.class });

            RPCCaller.<MRCServiceClient, xtreemfs_update_file_sizeRequest, timestampResponse> makeCall(
                    mrcServiceClient, m, userCredentialsBogus, authBogus, rqBuilder.build(), mrcUuidIterator,
                    uuidResolver, volumeOptions.getMaxTries(), volumeOptions, false);
        } catch (NoSuchMethodException nsm) {
            // should never happen unless there is a programming error
            nsm.printStackTrace();
        } catch (SecurityException se) {
            // should never happen unless there is a programming error
            se.printStackTrace();
        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    protected void setOsdWriteResponseForAsyncWriteBack(OSDWriteResponse osdwr) {

        fileHandleLock.lock();
        try {
            assert (osdWriteResponseForAsyncWriteBack == null);
            this.osdWriteResponseForAsyncWriteBack = osdwr.toBuilder().build();
        } finally {
            fileHandleLock.unlock();
        }

    }

    protected void renewXCapAsync() throws IOException {
        XCap xcapCopy;

        fileHandleLock.lock();
        try {
            // TODO: Only renew after some time has elapsed.
            // TODO: Cope with local clocks which have high clock skew.
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "Renew SCap for fileId: %s  Expiration in: %s", Helper.extractFileIdFromXcap(xcap),
                        xcap.getExpireTimeoutS() - System.currentTimeMillis() / 1000);
            }

            xcapCopy = this.xcap.toBuilder().build();
            xcapRenewalPending = true;
        } finally {
            fileHandleLock.unlock();
        }

        try {
            Method m = MRCServiceClient.class.getDeclaredMethod("xtreemfs_renew_capability", new Class<?>[] {
                    InetSocketAddress.class, Auth.class, UserCredentials.class, XCap.class });

            RPCCaller.<MRCServiceClient, XCap, XCap> makeCall(mrcServiceClient, m, userCredentialsBogus,
                    authBogus, xcapCopy, mrcUuidIterator, uuidResolver, volumeOptions.getMaxTries(),
                    volumeOptions, false);
        } catch (NoSuchMethodException nsm) {
            // should never happen unless there is a programming error
            nsm.printStackTrace();
        } catch (SecurityException se) {
            // should never happen unless there is a programming error
            se.printStackTrace();
        } catch (Exception e) {

            throw new IOException(e);
        }
    }

    /**
     * Sets asyncWritesFailed to true.
     */
    protected void markAsyncWritesAsFailed() {
        fileHandleLock.lock();
        try {
            asyncWritesFailed = true;
        } finally {
            fileHandleLock.unlock();
        }
    }

    protected XCap getXcap() {
        fileHandleLock.lock();
        try {
            return xcap.toBuilder().build();
        } finally {
            fileHandleLock.unlock();
        }
    }

    /**
     * Sends pending file size updates synchronous (needed for flush/close).
     * 
     * @throws IOException
     */
    protected void writeBackFileSize(OSDWriteResponse response, boolean closeFile) throws IOException {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "WriteBackFileSize: fileId: %s  #bytes: %s  close file?: %s", getFileId(),
                    response.getSizeInBytes(), closeFile);
        }

        xtreemfs_update_file_sizeRequest.Builder requestBuilder = xtreemfs_update_file_sizeRequest
                .newBuilder();

        fileHandleLock.lock();
        try {
            requestBuilder.setXcap(xcap.toBuilder().build());
        } finally {
            fileHandleLock.unlock();
        }

        requestBuilder.setOsdWriteResponse(response.toBuilder().build());
        requestBuilder.setCloseFile(closeFile);

        try {
            Method m = MRCServiceClient.class.getDeclaredMethod("xtreemfs_update_file_size", new Class<?>[] {
                    InetSocketAddress.class, Auth.class, UserCredentials.class,
                    xtreemfs_update_file_sizeRequest.class });

            RPCCaller.<MRCServiceClient, xtreemfs_update_file_sizeRequest, timestampResponse> makeCall(
                    mrcServiceClient, m, userCredentialsBogus, authBogus, requestBuilder.build(),
                    mrcUuidIterator, uuidResolver, volumeOptions.getMaxTries(), volumeOptions, false);
        } catch (NoSuchMethodException nsm) {
            // should never happen unless there is a programming error
            nsm.printStackTrace();
        } catch (SecurityException se) {
            // should never happen unless there is a programming error
            se.printStackTrace();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Extracts the fileId from the stored xcap.
     */
    protected long getFileId() {
        fileHandleLock.lock();
        try {
            return Helper.extractFileIdFromXcap(xcap);
        } finally {
            fileHandleLock.unlock();
        }
    }

}
