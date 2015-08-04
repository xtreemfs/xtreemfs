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

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.UserException;

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
        clientVoucherManager.addVoucher(expireTime, fileSize + blockedSpace);

        return fileSize + blockedSpace;
    }

    public void clearVouchers(String clientID, long fileSize, Set<Long> expireTimes) {

        System.out.println(getClass() + " clearVoucher");

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

                    volumeQuotaManager.updateSpaceUsage(fileSizeDifference, blockedSpace);
                    this.fileSize = fileSize;
                } else {
                    System.out.println("ERROR - FIXME given");
                    // FIXME: throw exception? should not be possible by definition
                }
            }
        } else {
            System.out.println("ERROR - FIXME given");
            // FIXME: throw exception? should not be possible
        }
    }

    /**
     * Checks whether there is an entry for the clientID and oldExpireTime and iff so, it get's a new voucher for the
     * newExpireTime
     * 
     * @param clientID
     * @param oldExpireTime
     * @param newExpireTime
     * @return the new voucher size
     * @throws UserException
     *             if parameter couldn't be found or if no voucher could be acquired
     */
    public long checkAndRenewVoucher(String clientID, long oldExpireTime, long newExpireTime) throws UserException {

        long newMaxFileSize = fileSize + blockedSpace;
        boolean error = false;

        ClientVoucherManager clientVoucherManager = openVoucherMap.get(clientID);
        if (clientVoucherManager != null) {

            boolean hasActiveExpireTime = clientVoucherManager.hasActiveExpireTime(oldExpireTime);
            if (hasActiveExpireTime) {
                long voucherSize = volumeQuotaManager.getVoucher();

                newMaxFileSize = addVoucher(clientID, newExpireTime, voucherSize);
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
