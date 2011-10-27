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
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.statvfsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.timestampResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
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
    private final java.util.concurrent.locks.Lock fileHandleLock = new ReentrantLock();

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
    public void flush() throws IOException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.common.libxtreemfs.FileHandle#truncate(org.xtreemfs.foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int)
     */
    @Override
    public void truncate(UserCredentials userCredentials, int newfileSize) throws IOException {
        // TODO Auto-generated method stub

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
            boolean exclusive, boolean waitForLock) throws IOException {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#releaseLockOfProcess(int)
     */
    @Override
    public void releaseLockOfProcess(int processId) throws IOException {
        // TODO Auto-generated method stub

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

}
