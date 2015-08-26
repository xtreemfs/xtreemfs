/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.quota;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;

/**
 * This class manages all voucher requested affairs and if necessary, it delegates them to reference classes.
 * 
 * TODO: This class is currently thread-safe, but because the MRC has only one operational thread, there is no need for
 * this. --- really? Open & Auth vs. e.g. FileSizeUpdater or something like that
 */
public class MRCVoucherManager {

    private final static long                     noVoucherValue = -1;
    
    private final MRCQuotaManager                 mrcQuotaManager;

    // TODO: split by volume UUID, if possible
    // maps the file id to a file voucher manager
    private final Map<String, FileVoucherManager> fileVoucherMap = new HashMap<String, FileVoucherManager>();

    /**
     * 
     */
    public MRCVoucherManager(MRCQuotaManager mrcQuotaManager) {
        this.mrcQuotaManager = mrcQuotaManager;
    }

    /**
     * 
     * @param volumeId
     * @param fileID
     * @param clientID
     * @param fileSize
     * @param expireTime
     * @return voucher size
     * @throws UserException
     *             iff an error occured at getting the voucher
     */
    public long getVoucher(QuotaFileInformation quotaFileInformation, String clientID, long expireTime,
            AtomicDBUpdate update)
            throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Client " + clientID + " requests a voucher for file: "
                + quotaFileInformation.getGlobalFileID());

        long newMaxFileSize = quotaFileInformation.getFileSize();

        if (mrcQuotaManager.hasActiveVolumeQuotaManager(quotaFileInformation.getVolumeId())) {

            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                    .getVolumeId());
            long voucherSize = volumeQuotaManager.getVoucher(update);

            synchronized (fileVoucherMap) {
                FileVoucherManager fileVoucherManager = fileVoucherMap.get(quotaFileInformation.getGlobalFileID());
                if (fileVoucherManager == null) {
                    fileVoucherManager = new FileVoucherManager(volumeQuotaManager,
                            quotaFileInformation.getGlobalFileID(), quotaFileInformation.getFileSize());
                    fileVoucherMap.put(quotaFileInformation.getGlobalFileID(), fileVoucherManager);
                }
                newMaxFileSize = fileVoucherManager.addVoucher(clientID, expireTime, voucherSize);
            }

        } else {
            newMaxFileSize = noVoucherValue; // FIXME(baerhold): export default for "unlimited" to proper place
        }

        return newMaxFileSize;
    }

    /**
     * TODO: adapt to user and group quota
     * 
     * @param quotaFileInformation
     * @throws UserException
     */
    public void checkVoucherAvailability(QuotaFileInformation quotaFileInformation) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this,
                "Check voucher availability for file: " + quotaFileInformation.getGlobalFileID());

        if (mrcQuotaManager.hasActiveVolumeQuotaManager(quotaFileInformation.getVolumeId())) {

            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(quotaFileInformation
                    .getVolumeId());

            // ignore return value, because if no voucher is available, an exception will be thrown
            volumeQuotaManager.checkVoucherAvailability();
        }
    }

    public void clearVouchers(String globalFileID, String clientID, Set<Long> expireTimes,
            long fileSize, AtomicDBUpdate update) {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Clear voucher for file: " + globalFileID + ". Client: "
                + clientID + " fileSize: " + fileSize + " expireTimes: " + expireTimes.toString());

        synchronized (fileVoucherMap) {
            FileVoucherManager fileVoucherManager = fileVoucherMap.get(globalFileID);

            if (fileVoucherManager != null) {
                fileVoucherManager.clearVouchers(clientID, fileSize, expireTimes, update);

                if (fileVoucherManager.isCleared()) {
                    fileVoucherMap.remove(globalFileID);
                }
            } else {
                Logging.logMessage(Logging.LEVEL_WARN, this,
                        "Couldn't clear voucher, because no open voucher was issued for file: " + globalFileID
                                + ". Client: " + clientID + " fileSize: " + fileSize + " expireTimes: "
                                + expireTimes.toString());
            }
        }
    }

    /**
     * Clear open vouchers and delete file via file voucher manager or directly on the volume quota manager, if no
     * vouchers are currently active.
     * 
     * @param quotaFileInformation
     * @param update
     */
    public void deleteFile(QuotaFileInformation quotaFileInformation, AtomicDBUpdate update) {

        synchronized (fileVoucherMap) {
            FileVoucherManager fileVoucherManager = fileVoucherMap.get(quotaFileInformation.getGlobalFileID());

            if (fileVoucherManager != null) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "Delete file with voucher: " + quotaFileInformation.getGlobalFileID());

                // mark file as deleted and remove file voucher manager
                fileVoucherManager.deleteFile(update);
                fileVoucherMap.remove(quotaFileInformation.getGlobalFileID());
            } else {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "Delete file without voucher: " + quotaFileInformation.getGlobalFileID());

                // check for active volume quota manager and reduce used space by file size
                if (mrcQuotaManager.hasActiveVolumeQuotaManager(quotaFileInformation.getVolumeId())) {
                    VolumeQuotaManager volumeQuotaManager = mrcQuotaManager
                            .getVolumeQuotaManagerById(quotaFileInformation.getVolumeId());
                    volumeQuotaManager.updateSpaceUsage(-1 * quotaFileInformation.getFileSize(), 0, update);
                }
            }

        }
    }

    /**
     * Checks whether there is an entry for the fileID, clientID and oldExpireTime and iff so, it get's a new voucher
     * for the newExpireTime
     * 
     * @param fileID
     * @param clientID
     * @param oldExpireTime
     * @param newExpireTime
     * @return the new voucher size
     * @throws UserException
     *             if parameter couldn't be found or if no new voucher could be acquired
     */
    public long checkAndRenewVoucher(String fileID, String clientID, long oldExpireTime, long newExpireTime,
            AtomicDBUpdate update)
            throws UserException {

        long voucherSize = 0;

        synchronized (fileVoucherMap) {
            FileVoucherManager fileVoucherManager = fileVoucherMap.get(fileID);

            try {

                if (fileVoucherManager != null) {
                    voucherSize = fileVoucherManager.checkAndRenewVoucher(clientID, oldExpireTime, newExpireTime,
                            update);
                } else {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "No open voucher for fileID " + fileID);
                }
            } catch (UserException userException) {
                if (userException.getErrno() == POSIXErrno.POSIX_ERROR_EINVAL) {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "Cap with expire time " + oldExpireTime
                            + " has already been cleared!");
                } else {
                    // unexpected Error
                    throw userException;
                }
            }
        }

        return voucherSize;
    }

    /**
     * Used for periodic xcap renewal to avoid an increase of the voucher
     * 
     * @param fileID
     * @param clientID
     * @param oldExpireTime
     * @param newExpireTime
     * @throws UserException
     */
    public void addRenewedTimestamp(String fileID, String clientID, long oldExpireTime, long newExpireTime)
            throws UserException {

        synchronized (fileVoucherMap) {
            FileVoucherManager fileVoucherManager = fileVoucherMap.get(fileID);

            try {

                if (fileVoucherManager != null) {
                    fileVoucherManager.addRenewedTimestamp(clientID, oldExpireTime, newExpireTime);
                } else {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "No open voucher for fileID " + fileID);
                }
            } catch (UserException userException) {
                if (userException.getErrno() == POSIXErrno.POSIX_ERROR_EINVAL) {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "Cap with expire time " + oldExpireTime
                            + " has already been cleared!");
                } else {
                    // unexpected Error
                    throw userException;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MRCVoucherManager [mrcQuotaManager=").append(mrcQuotaManager).append(", fileVoucherMap=")
                .append(fileVoucherMap).append("]");
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
