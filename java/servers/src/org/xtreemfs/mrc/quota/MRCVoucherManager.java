/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.quota;

import java.util.Set;

import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.BufferBackedFileVoucherClientInfo;
import org.xtreemfs.mrc.metadata.BufferBackedFileVoucherInfo;
import org.xtreemfs.mrc.metadata.FileVoucherClientInfo;
import org.xtreemfs.mrc.metadata.FileVoucherInfo;

/**
 * This class manages all voucher requested affairs and if necessary, it delegates them to reference classes.
 * 
 * TODO: This class is currently thread-safe, but because the MRC has only one operational thread, there is no need for
 * this. False: XLockCoordinator handling add/remove replica works in a separate thread.
 */
public class MRCVoucherManager {

    private final MRCQuotaManager mrcQuotaManager;

    public MRCVoucherManager(MRCQuotaManager mrcQuotaManager) {
        this.mrcQuotaManager = mrcQuotaManager;
    }

    /**
     * Returns the first voucher for a given file, client and expire time, if no quota is violated. If no quota is set
     * at all, it will return an unlimited voucher.
     * 
     * @param quotaFileInformation
     * @param clientId
     * @param expireTime
     * @param update
     * @return
     * @throws UserException
     */
    public synchronized long getVoucher(QuotaFileInformation quotaFileInformation, String clientId, long expireTime,
            AtomicDBUpdate update) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Client " + clientId + " requests a voucher for file: "
                + quotaFileInformation.getGlobalFileId());

        long newMaxFileSize = 0;

        if (mrcQuotaManager.hasActiveVolumeQuotaManager(quotaFileInformation.getVolumeId())) {

            try {
                VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                        .getVolumeId());
                StorageManager storageManager = volumeQuotaManager.getVolStorageManager();

                FileVoucherInfo fileVoucherInfo = storageManager.getFileVoucherInfo(quotaFileInformation.getFileId());
                FileVoucherClientInfo fileVoucherClientInfo = storageManager.getFileVoucherClientInfo(
                        quotaFileInformation.getFileId(), clientId);

                long voucherSize;
                if (fileVoucherInfo == null) {
                    assert (fileVoucherClientInfo == null); // it has to be null

                    voucherSize = volumeQuotaManager.getVoucher(quotaFileInformation, update);

                    fileVoucherInfo = new BufferBackedFileVoucherInfo(quotaFileInformation.getFileId(),
                            quotaFileInformation.getFilesize(), quotaFileInformation.getReplicaCount(), voucherSize);
                } else {
                    if (fileVoucherClientInfo == null) {
                        fileVoucherInfo.increaseClientCount();
                    }

                    // overwrite replica count, because an added replica don't has to be installed yet, but is covered
                    // by the voucher and quota management.
                    quotaFileInformation.setReplicaCount(fileVoucherInfo.getReplicaCount());
                    voucherSize = volumeQuotaManager.getVoucher(quotaFileInformation, update);

                    fileVoucherInfo.increaseBlockedSpaceByValue(voucherSize);
                }

                if (fileVoucherClientInfo == null) {
                    fileVoucherClientInfo = new BufferBackedFileVoucherClientInfo(quotaFileInformation.getFileId(),
                            clientId, expireTime);
                } else {
                    fileVoucherClientInfo.addExpireTime(expireTime);
                }

                newMaxFileSize = fileVoucherInfo.getFilesize() + fileVoucherInfo.getBlockedSpace();

                storageManager.setFileVoucherInfo(fileVoucherInfo, update);
                storageManager.setFileVoucherClientInfo(fileVoucherClientInfo, update);
            } catch (DatabaseException e) {
                Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

                throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                        "An error occured during the interaction with the database!");
            }
        } else {
            newMaxFileSize = QuotaConstants.unlimitedVoucher;
        }

        return newMaxFileSize;
    }

    /**
     * Checks the voucher availability for a file.
     * 
     * @param quotaFileInformation
     * @throws UserException
     */
    public synchronized void checkVoucherAvailability(QuotaFileInformation quotaFileInformation) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this,
                "Check voucher availability for file: " + quotaFileInformation.getGlobalFileId());

        if (mrcQuotaManager.hasActiveVolumeQuotaManager(quotaFileInformation.getVolumeId())) {
            try {
                VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                        .getVolumeId());
                StorageManager storageManager = volumeQuotaManager.getVolStorageManager();

                FileVoucherInfo fileVoucherInfo;
                fileVoucherInfo = storageManager.getFileVoucherInfo(quotaFileInformation.getFileId());
                if (fileVoucherInfo != null) {
                    // overwrite replica count, because an added replica don't has to be installed yet, but is covered
                    // by the voucher and quota management.
                    quotaFileInformation.setReplicaCount(fileVoucherInfo.getReplicaCount());
                }

                // ignore return value, because if no voucher is available, an exception will be thrown
                volumeQuotaManager.checkVoucherAvailability(quotaFileInformation);
            } catch (DatabaseException e) {
                Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

                throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                        "An error occured during the interaction with the database!");
            }
        }
    }

    /**
     * Clears the voucher for a given client regarding the given set of expire times. Updates the space usage the given
     * filesize, if all issued vouchers has been cleared.
     * 
     * @param quotaFileInformation
     * @param clientId
     * @param expireTimes
     * @param fileSize
     * @param update
     * @throws UserException
     */
    public synchronized void clearVouchers(QuotaFileInformation quotaFileInformation, String clientId,
            Set<Long> expireTimes, long fileSize, AtomicDBUpdate update) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this,
                "Clear voucher for file: " + quotaFileInformation.getGlobalFileId() + ". Client: " + clientId
                        + " fileSize: " + fileSize + " expireTimes: " + expireTimes.toString());

        try {
            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                    .getVolumeId());
            StorageManager storageManager = volumeQuotaManager.getVolStorageManager();

            FileVoucherClientInfo fileVoucherClientInfo = storageManager.getFileVoucherClientInfo(
                    quotaFileInformation.getFileId(), clientId);

            if (fileVoucherClientInfo != null) {
                // clear expire times
                fileVoucherClientInfo.removeExpireTimeSet(expireTimes);
                storageManager.setFileVoucherClientInfo(fileVoucherClientInfo, update);

                // if no expire time remains, update general file voucher info
                if (fileVoucherClientInfo.getExpireTimeSetSize() == 0) {
                    FileVoucherInfo fileVoucherInfo = storageManager.getFileVoucherInfo(quotaFileInformation
                            .getFileId());

                    if (fileVoucherInfo == null) {
                        throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                                "Invalid database structure: no general voucher information saved for fileId:"
                                        + quotaFileInformation.getGlobalFileId());
                    }

                    fileVoucherInfo.decreaseClientCount();

                    // if there is no open voucher anymore, clear general information and update quota information
                    if (fileVoucherInfo.getClientCount() == 0) {
                        int replicaCount = fileVoucherInfo.getReplicaCount();
                        long fileSizeDifference = fileSize - fileVoucherInfo.getFilesize();
                        volumeQuotaManager.updateSpaceUsage(replicaCount * fileSizeDifference, replicaCount
                                * fileVoucherInfo.getBlockedSpace(), update);
                    }

                    storageManager.setFileVoucherInfo(fileVoucherInfo, update);
                }
            } else {
                Logging.logMessage(
                        Logging.LEVEL_WARN,
                        this,
                        "Couldn't clear voucher, because no open voucher was issued for file: "
                                + quotaFileInformation.getGlobalFileId() + ". Client: " + clientId + " fileSize: "
                                + fileSize + " expireTimes: " + expireTimes.toString());
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
    }

    /**
     * Updates the volume quota manager and if there are are open vouchers, they will be deleted.
     * 
     * @param quotaFileInformation
     * @param update
     * @throws UserException
     */
    public synchronized void deleteFile(QuotaFileInformation quotaFileInformation, AtomicDBUpdate update)
            throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Delete file: " + quotaFileInformation.getGlobalFileId()
                + ": Check for open voucher and pass delete to quota manager.");

        DatabaseResultSet<FileVoucherClientInfo> allFileVoucherClientInfo = null;

        try {
            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                    .getVolumeId());
            StorageManager storageManager = volumeQuotaManager.getVolStorageManager();

            FileVoucherInfo fileVoucherInfo = storageManager.getFileVoucherInfo(quotaFileInformation.getFileId());

            int replicaCount = quotaFileInformation.getReplicaCount();
            if (fileVoucherInfo != null) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "Delete file with voucher: " + quotaFileInformation.getGlobalFileId());

                // overwrite replica count, because an added replica don't has to be installed yet, but is covered by
                // the voucher and quota management.
                replicaCount = fileVoucherInfo.getReplicaCount();

                volumeQuotaManager.updateSpaceUsage(-1 * replicaCount * fileVoucherInfo.getFilesize(), replicaCount
                        * fileVoucherInfo.getBlockedSpace(), update);

                // get all open client information and delete them
                allFileVoucherClientInfo = storageManager.getAllFileVoucherClientInfo(quotaFileInformation.getFileId());
                while (allFileVoucherClientInfo.hasNext()) {
                    FileVoucherClientInfo fileVoucherClientInfo = allFileVoucherClientInfo.next();
                    fileVoucherClientInfo.clearExpireTimeSet();
                    storageManager.setFileVoucherClientInfo(fileVoucherClientInfo, update);
                }

            } else {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "Delete file without voucher: " + quotaFileInformation.getGlobalFileId());

                // check for active volume quota manager and reduce used space by file size
                if (volumeQuotaManager.isActive()) {
                    volumeQuotaManager.updateSpaceUsage(-1 * replicaCount * quotaFileInformation.getFilesize(), 0,
                            update);
                }
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        } finally {
            if (allFileVoucherClientInfo != null) {
                allFileVoucherClientInfo.destroy();
            }
        }
    }

    /**
     * Checks whether there is an entry for the fileID, clientID and oldExpireTime and iff so, it get's a new voucher
     * for the newExpireTime
     * 
     * @param quotaFileInformation
     * @param clientId
     * @param oldExpireTime
     * @param newExpireTime
     * @param update
     * @return the new voucher size
     * @throws UserException
     *             if parameter couldn't be found or if no new voucher could be acquired
     */
    public synchronized long checkAndRenewVoucher(QuotaFileInformation quotaFileInformation, String clientId,
            long oldExpireTime, long newExpireTime, AtomicDBUpdate update) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this,
                "Renew voucher for file: " + quotaFileInformation.getGlobalFileId() + ": client: " + clientId
                        + ", oldExpireTime: " + oldExpireTime + ", newExpireTime: " + newExpireTime);

        long newMaxFileSize = 0;

        try {
            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                    .getVolumeId());
            StorageManager storageManager = volumeQuotaManager.getVolStorageManager();

            FileVoucherClientInfo fileVoucherClientInfo = storageManager.getFileVoucherClientInfo(
                    quotaFileInformation.getFileId(), clientId);

            if (fileVoucherClientInfo != null) {
                if (fileVoucherClientInfo.hasExpireTime(oldExpireTime)) {
                    FileVoucherInfo fileVoucherInfo = storageManager.getFileVoucherInfo(quotaFileInformation
                            .getFileId());

                    if (fileVoucherInfo != null) {
                        // overwrite replica count, because an added replica don't has to be installed yet, but is
                        // covered by the voucher and quota management.
                        quotaFileInformation.setReplicaCount(fileVoucherInfo.getReplicaCount());
                        long voucherSize = volumeQuotaManager.getVoucher(quotaFileInformation, update);

                        fileVoucherInfo.increaseBlockedSpaceByValue(voucherSize);
                        storageManager.setFileVoucherInfo(fileVoucherInfo, update);

                        fileVoucherClientInfo.addExpireTime(newExpireTime);
                        storageManager.setFileVoucherClientInfo(fileVoucherClientInfo, update);

                        newMaxFileSize = fileVoucherInfo.getFilesize() + fileVoucherInfo.getBlockedSpace();

                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Renew voucher to " + newMaxFileSize
                                + ". fileId: " + quotaFileInformation.getFileId() + ", client: " + clientId
                                + ", oldExpireTime: " + oldExpireTime + ", newExpireTime: " + newExpireTime);
                    } else {
                        throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                                "Invalid database structure: no general voucher information saved for fileId:"
                                        + quotaFileInformation.getGlobalFileId());
                    }
                } else {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "Former expire time: " + oldExpireTime
                            + " couldn't be found for fileId:" + quotaFileInformation.getGlobalFileId());
                }
            } else {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "No open voucher for global fileId "
                        + quotaFileInformation.getGlobalFileId());
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }

        return newMaxFileSize;
    }

    /**
     * Used for periodic xcap renewal to avoid an increase of the voucher
     * 
     * @param quotaFileInformation
     * @param clientId
     * @param oldExpireTime
     * @param newExpireTime
     * @param update
     * @throws UserException
     */
    public synchronized void addRenewedTimestamp(QuotaFileInformation quotaFileInformation, String clientId,
            long oldExpireTime, long newExpireTime, AtomicDBUpdate update) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this,
                "Add renewed timestamp for file: " + quotaFileInformation.getGlobalFileId() + ": client: " + clientId
                        + ", oldExpireTime: " + oldExpireTime + ", newExpireTime: " + newExpireTime);

        try {
            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                    .getVolumeId());
            StorageManager storageManager = volumeQuotaManager.getVolStorageManager();

            FileVoucherClientInfo fileVoucherClientInfo = storageManager.getFileVoucherClientInfo(
                    quotaFileInformation.getFileId(), clientId);

            if (fileVoucherClientInfo != null) {
                if (fileVoucherClientInfo.hasExpireTime(oldExpireTime)) {
                    fileVoucherClientInfo.addExpireTime(newExpireTime);
                    storageManager.setFileVoucherClientInfo(fileVoucherClientInfo, update);

                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "Added new expireTime: " + newExpireTime
                            + " for fileId: " + quotaFileInformation.getFileId() + " and client: " + clientId);
                } else {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "Former expire time: " + oldExpireTime
                            + " couldn't be found for fileId:" + quotaFileInformation.getGlobalFileId());
                }
            } else {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "No open voucher for global fileId "
                        + quotaFileInformation.getGlobalFileId());
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
    }

    /**
     * Tries to add a new replica for a file to the space usage using the stats of the open FileVoucherInfo or if it has
     * no open voucher, the saved MRC values.
     * 
     * @param quotaFileInformation
     * @param update
     * @throws UserException
     */
    public synchronized void addReplica(QuotaFileInformation quotaFileInformation, AtomicDBUpdate update)
            throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Add replica for file: " + quotaFileInformation.getGlobalFileId());

        try {
            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                    .getVolumeId());
            StorageManager storageManager = volumeQuotaManager.getVolStorageManager();

            FileVoucherInfo fileVoucherInfo = storageManager.getFileVoucherInfo(quotaFileInformation.getFileId());

            long filesize = quotaFileInformation.getFilesize();
            long blockedSpace = 0;
            if (fileVoucherInfo != null) {
                filesize = fileVoucherInfo.getFilesize();
                blockedSpace = fileVoucherInfo.getBlockedSpace();

                // update file voucher info
                fileVoucherInfo.increaseReplicaCount();
                storageManager.setFileVoucherInfo(fileVoucherInfo, update);
            }

            volumeQuotaManager.addReplica(quotaFileInformation, filesize, blockedSpace, update);
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
    }

    /**
     * Removes a replica
     * 
     * @param quotaFileInformation
     * @param update
     * @throws UserException
     */
    public synchronized void removeReplica(QuotaFileInformation quotaFileInformation, AtomicDBUpdate update)
            throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this,
                "Remove replica for file: " + quotaFileInformation.getGlobalFileId());

        try {
            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                    .getVolumeId());
            StorageManager storageManager = volumeQuotaManager.getVolStorageManager();

            FileVoucherInfo fileVoucherInfo = storageManager.getFileVoucherInfo(quotaFileInformation.getFileId());

            long filesizeDifference = -1 * quotaFileInformation.getFilesize();
            long clearBlockedSpace = 0;
            if (fileVoucherInfo != null) {
                filesizeDifference = -1 * fileVoucherInfo.getFilesize();
                clearBlockedSpace = fileVoucherInfo.getBlockedSpace();

                // update file voucher info
                fileVoucherInfo.decreaseReplicaCount();
                storageManager.setFileVoucherInfo(fileVoucherInfo, update);
            }

            volumeQuotaManager.updateSpaceUsage(filesizeDifference, clearBlockedSpace, update);
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
    }

    /**
     * General check, whether it is manageable at all, because e.g. read access won't be managed
     * 
     * @param flags
     *            access flags
     * @return true, if the flags indicate a voucher management, regardless of a real active quota
     */
    public static boolean checkManageableAccess(int flags) {

        boolean create = (flags & FileAccessManager.O_CREAT) != 0;
        boolean truncate = (flags & FileAccessManager.O_TRUNC) != 0;
        boolean write = (flags & (FileAccessManager.O_WRONLY | FileAccessManager.O_RDWR)) != 0;

        return create || truncate || write;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MRCVoucherManager [mrcQuotaManager=").append(mrcQuotaManager).append("]");
        return builder.toString();
    }

    // public static void main(String[] args) throws Exception {
    // // FIXME(baerhold): Export to test
    // // VolumeQuotaManager volumeQuotaManager = new VolumeQuotaManager("blubb", 150 * 1024 * 1024);
    //
    // MRCQuotaManager mrcQuotaManager_ = new MRCQuotaManager();
    //
    // // mrcQuotaManager_.addVolumeQuotaManager(volumeQuotaManager);
    //
    // MRCVoucherManager mrcVoucherManager = new MRCVoucherManager(mrcQuotaManager_);
    //
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client1", 0, 123456789));
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client2", 0, 1234567890));
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client1", 0, 1234567891));
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file2", "client1", 0, 1234567892));
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file2", "client1", 10, 1234567893));
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file3", "client1", 0, 1234567894));
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file4", "client1", 0, 1234567895));
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client1", 0, 1234567896));
    // System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client2", 0, 1234567897));
    //
    // System.out.println(mrcVoucherManager.toString());
    // System.out.println(mrcQuotaManager_.toString());
    //
    // // curBlockedSpace: 47158920 -> file blocked space 1-4: (26214400, 10485760, 5242880, 5242880)
    //
    // mrcVoucherManager.clearVouchers("file4", "client1", 3 * 1024 * 1024,
    // new HashSet<Long>(Arrays.asList(new Long(1234567895))));
    // mrcVoucherManager.clearVouchers("file2", "client1", 5 * 1024 * 1024,
    // new HashSet<Long>(Arrays.asList(new Long(1234567893))));
    // mrcVoucherManager.clearVouchers("file3", "client1", 1 * 1024 * 1024,
    // new HashSet<Long>(Arrays.asList(new Long(1234567894))));
    // mrcVoucherManager.clearVouchers("file2", "client1", 7 * 1024 * 1024,
    // new HashSet<Long>(Arrays.asList(new Long(1234567892))));
    // mrcVoucherManager.clearVouchers("file1", "client1", 4 * 1024 * 1024,
    // new HashSet<Long>(Arrays.asList(new Long(1234567891))));
    // mrcVoucherManager.clearVouchers("file1", "client2", 8 * 1024 * 1024,
    // new HashSet<Long>(Arrays.asList(new Long(1234567897), new Long(1234567890))));
    // mrcVoucherManager.clearVouchers("file1", "client1", 20 * 1024 * 1024,
    // new HashSet<Long>(Arrays.asList(new Long(123456789), new Long(1234567896))));
    //
    // System.out.println(mrcVoucherManager.toString());
    // System.out.println(mrcQuotaManager_.toString());
    //
    // // blockSpace = 0
    // // curVolSpace = 32505856
    // }
}
