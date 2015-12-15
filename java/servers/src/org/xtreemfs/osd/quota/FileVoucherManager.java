/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.quota;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.storage.StorageLayout;

/**
 * Manages all vouchers regarding open(ed) files.
 * 
 * This class doesn't have to be thread-safe, cause it's only accessible via the OSDVoucherManager, which is already
 * thread-safe.
 */
public class FileVoucherManager {

    private final String      fileId;
    private final StorageLayout storageLayout;

    private final Set<String> clientExpireTimeSet        = new HashSet<String>();
    private final Set<String> invalidClientExpireTimeSet;

    private long              voucherSizeMax             = 0;
    private long              latestExpireTime           = 0;

    public FileVoucherManager(String fileId, StorageLayout storageLayout) throws IOException {
        this.fileId = fileId;
        this.storageLayout = storageLayout;

        invalidClientExpireTimeSet = storageLayout.getInvalidClientExpireTimeSet(fileId);
        removeObsoleteInvalidTimes();
    }

    /**
     * 
     * @param clientId
     * @param expireTime
     * @param voucherSize
     * @throws VoucherErrorException
     */
    public void addVoucher(String clientId, long expireTime, long voucherSize) throws VoucherErrorException {
        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                "Add Voucher! [FileID: %s, ClientId %s, expireTime: %s, voucherSize: %s]", fileId, clientId,
                expireTime, voucherSize);

        if (!invalidClientExpireTimeSet.contains(expireTime + "." + clientId)) {
            if (!clientExpireTimeSet.contains(expireTime + "." + clientId)) {
                clientExpireTimeSet.add(expireTime + "." + clientId);
                voucherSizeMax = (voucherSizeMax < voucherSize) ? voucherSize : voucherSizeMax;
                latestExpireTime = (latestExpireTime < expireTime) ? expireTime : latestExpireTime;

                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "Register Voucher! [FileID: %s, ClientId %s, latestExpireTime: %d, voucherSizeMaz: %d]",
                        fileId, clientId, latestExpireTime, voucherSizeMax);
            } else {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "Voucher already registered! [FileID: %s, ClientId %s, expireTime: %d, voucherSize: %d]",
                        fileId, clientId, expireTime, voucherSize);
            }
        } else {
            throw new VoucherErrorException(String.format(
                    "The given xcap has already been invalidated! [FielID: %s, ClientId: %s, expireTime: %s]", fileId,
                    clientId, expireTime));
        }
    }

    /**
     * Checks, whether a given extend operation (e.g. write, truncate) is executable regarding the registered vouchers.
     * There is no need to block them, because it only checks the limit from the whole beginning. Concurrent access
     * can't effect this.
     * 
     * @param clientId
     * @param expireTime
     * @param newFileSize
     * @return
     * @throws VoucherErrorException
     */
    public boolean checkMaxVoucherSize(String clientId, long expireTime, long newFileSize) throws VoucherErrorException {

        if (!clientExpireTimeSet.contains(expireTime + "." + clientId) &&
                invalidClientExpireTimeSet.contains(expireTime + "." + clientId)) {
            throw new VoucherErrorException(String.format(
                    "The given xcap has already been invalidated! [FielID: %s, ClientId: %s, expireTime: %s]",
                    fileId, clientId, expireTime));
            // TODO: Only the first condition becomes true very frequently, check this!
        }

        // check for maximum allowed size
        if (clientExpireTimeSet.isEmpty() || newFileSize <= voucherSizeMax) {
            return true;
        } else {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "New file size is less than the maximum allowed: "
                            + "[FileID: %s, ClientId %s, newFileSize: %s, maxFileSize: %s]", fileId, clientId,
                    newFileSize, voucherSizeMax);

            return false;
        }
    }

    /**
     * Invalidates client expire times and saves all invalid times locally
     * 
     * @param clientId
     * @param expireTimeSet
     * @throws IOException
     */
    public void invalidateVouchers(String clientId, Set<Long> expireTimeSet) throws IOException {

        removeObsoleteInvalidTimes();

        long currentTime = TimeSync.getGlobalTime();
        for (Long expireTime : expireTimeSet) {
            if (currentTime > expireTime) {
                continue;
            }

            clientExpireTimeSet.remove(expireTime + "." + clientId);
            invalidClientExpireTimeSet.add(expireTime + "." + clientId);

            // unseen vouchers have to be recognized for the latestExpireTime as well
            latestExpireTime = (latestExpireTime < expireTime) ? expireTime : latestExpireTime;
        }

        storageLayout.setInvalidClientExpireTimeSet(fileId, invalidClientExpireTimeSet);

        return;
    }

    /**
     * removes invalid client expire times, which are already obsolete
     */
    private void removeObsoleteInvalidTimes() {
        long currentTime = TimeSync.getGlobalTime();

        // delete expired expire times
        Iterator<String> it = invalidClientExpireTimeSet.iterator();
        while (it.hasNext()) {
            String invalidClientEntry = it.next();
            String expireTime = invalidClientEntry.substring(0, invalidClientEntry.indexOf("."));
            if (currentTime > Long.valueOf(expireTime)) {
                it.remove();
            } else {
                break;
            }
        }
    }

    /**
     * Checks, whether all voucher information regarding this file are invalid by themselves
     * 
     * @return
     */
    public boolean isObsolete() {
        return TimeSync.getGlobalTime() > latestExpireTime;
    }

    public void delete() throws IOException {
        removeObsoleteInvalidTimes();
        storageLayout.setInvalidClientExpireTimeSet(fileId, invalidClientExpireTimeSet);
    }
}
