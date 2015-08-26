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
 * The FileVoucherManager manages the different ClientVoucherManager regarding the different files by the file ID.
 * 
 * This class is not thread-safe.
 */
public class FileVoucherManager {

    private final VolumeQuotaManager                volumeQuotaManager;
    private final String                            fileID;

    // client id <> ClientVoucherManager
    private final Map<String, ClientVoucherManager> openVoucherMap = new HashMap<String, ClientVoucherManager>();

    private long                                    fileSize;
    private long                                    blockedSpace;

    /**
     * 
     */
    public FileVoucherManager(VolumeQuotaManager volumeQuotaManager, String fileID, long fileSize) {
        this.volumeQuotaManager = volumeQuotaManager;
        this.fileID = fileID;
        this.fileSize = fileSize;
    }

    public long addVoucher(String clientID, long expireTime, long voucherSize) {

        ClientVoucherManager clientVoucherManager = openVoucherMap.get(clientID);

        if (clientVoucherManager == null) {
            clientVoucherManager = new ClientVoucherManager(clientID, fileID);
            openVoucherMap.put(clientID, clientVoucherManager);
        }

        blockedSpace += voucherSize;
        clientVoucherManager.addVoucher(expireTime);

        return fileSize + blockedSpace;
    }

    public void clearVouchers(String clientID, long fileSize, Set<Long> expireTimes, AtomicDBUpdate update) {

        ClientVoucherManager clientVoucherManager = openVoucherMap.get(clientID);

        if (clientVoucherManager != null) {

            clientVoucherManager.clearVouchers(expireTimes);

            if (clientVoucherManager.isEmpty()) {
                openVoucherMap.remove(clientID);
            }

            if (openVoucherMap.isEmpty()) {
                // all vouchers regarding this file have been cleared

                // clear blocked space
                if ((this.fileSize + blockedSpace) - fileSize >= 0) {
                    long fileSizeDifference = fileSize - this.fileSize;

                    volumeQuotaManager.updateSpaceUsage(fileSizeDifference, blockedSpace, update);
                    this.fileSize = fileSize;
                } else {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "New filesize exceeds the issued space! fileID: "
                            + fileID + ", client: " + clientID + ", maximum filesize: "
                            + (this.fileSize + blockedSpace) + ", new filesize: " + fileSize);
                }
            }
        } else {
            Logging.logMessage(Logging.LEVEL_WARN, this, "No ClientVoucherManager is registered for client: "
                    + clientID + "! fileID: " + fileID + ", new filesize: " + fileSize);
        }
    }

    /**
     * Deleting a file will result in a clearance of blocked space and a negative file sizes update
     */
    public void deleteFile(AtomicDBUpdate update) {
        openVoucherMap.clear();
        volumeQuotaManager.updateSpaceUsage(-1 * fileSize, blockedSpace, update);
    }

    /**
     * Checks whether there is an entry for the clientID and oldExpireTime and iff so, issue a new voucher for the
     * newExpireTime
     * 
     * @param clientID
     * @param oldExpireTime
     * @param newExpireTime
     * @return the new voucher size
     * @throws UserException
     *             if parameter couldn't be found or if no voucher could be acquired
     */
    public long checkAndRenewVoucher(String clientID, long oldExpireTime, long newExpireTime, AtomicDBUpdate update)
            throws UserException {

        long newMaxFileSize = fileSize + blockedSpace;
        boolean error = false;

        ClientVoucherManager clientVoucherManager = openVoucherMap.get(clientID);
        if (clientVoucherManager != null) {

            boolean hasActiveExpireTime = clientVoucherManager.hasActiveExpireTime(oldExpireTime);
            if (hasActiveExpireTime) {
                long voucherSize = volumeQuotaManager.getVoucher(update);

                newMaxFileSize = addVoucher(clientID, newExpireTime, voucherSize);

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Renew voucher to " + newMaxFileSize + ". fileID: "
                        + fileID + ", client: " + clientID + ", oldExpireTime: " + oldExpireTime + ", newExpireTime: "
                        + newExpireTime);
            } else {
                error = true;
            }
        } else {
            error = true;
        }

        if (error) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "Couldn't find voucher for ClientID: " + clientID
                    + " and expire time: " + oldExpireTime);
        }

        return newMaxFileSize;
    }

    /**
     * Add a new timestamp for an open voucher after a periodic capability renew.
     * 
     * FIXME(baerhold): merge with checkAndRenew somehow...
     * 
     * @param clientID
     * @param oldExpireTime
     * @param newExpireTime
     * @throws UserException
     *             if parameter couldn't be found
     */
    public void addRenewedTimestamp(String clientID, long oldExpireTime, long newExpireTime) throws UserException {
        boolean error = false;

        ClientVoucherManager clientVoucherManager = openVoucherMap.get(clientID);
        if (clientVoucherManager != null) {

            boolean hasActiveExpireTime = clientVoucherManager.hasActiveExpireTime(oldExpireTime);
            if (hasActiveExpireTime) {
                clientVoucherManager.addVoucher(newExpireTime);

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Added new expireTime: " + newExpireTime
                        + " for fileID: " + fileID + " and client: " + clientID);
            } else {
                error = true;
            }
        } else {
            error = true;
        }

        if (error) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "Couldn't find voucher for ClientID: " + clientID
                    + " and expire time: " + oldExpireTime);
        }
    }

    /**
     * @return
     */
    public boolean isCleared() {
        return openVoucherMap.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "FileVoucherManager [volumeQuotaManager=" + volumeQuotaManager + ", fileID=" + fileID
                + ", openVoucherMap=" + openVoucherMap + ", fileSize=" + fileSize + ", blockedSpace=" + blockedSpace
                + "]";
    }
}
