/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.quota;

import java.util.HashSet;
import java.util.Set;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * Manages all vouchers regarding open(ed) files.
 * 
 * This class doesn't have to be thread-safe, cause it's only accessible via the OSDVoucherManager, which is already
 * thread-safe.
 */
public class FileVoucherManager {

    private final String      fileId;

    private final Set<String> clientExpireTimeSet        = new HashSet<String>();
    private final Set<String> invalidClientExpireTimeSet = new HashSet<String>();

    private long              voucherSizeMax             = 0;
    private long              latestExpireTime           = 0;

    public FileVoucherManager(String fileId) {
        this.fileId = fileId;
    }

    /**
     * @param expireTime
     * @param voucherSize
     */
    public void addVoucher(String clientId, long expireTime, long voucherSize) {
        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                "Add Voucher! [FileID: %s, ClientId %s, expireTime: %s, voucherSize: %s]", fileId, clientId,
                expireTime, voucherSize);
        if (!clientExpireTimeSet.contains(clientId + expireTime)) {
            clientExpireTimeSet.add(clientId + expireTime);
            voucherSizeMax = (voucherSizeMax < voucherSize) ? voucherSize : voucherSizeMax;
            latestExpireTime = (latestExpireTime < expireTime) ? expireTime : latestExpireTime;

            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "Register Voucher! [FileID: %s, ClientId %s, latestExpireTime: %d, voucherSizeMaz: %d]", fileId,
                    clientId, latestExpireTime, voucherSizeMax);
        } else {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "Didn't register Voucher! [FileID: %s, ClientId %s, expireTime: %d, voucherSize: %d]", fileId,
                    clientId, expireTime, voucherSize);
        }
    }

    /**
     * Checks, whether a given extend operation (e.g. write, truncate) is executable regarding the registered vouchers.
     * There is no need to block them, because it only checks the limit from the whole beginning. Concurrent access
     * can't effect this.
     * 
     * @param newFileSize
     */
    public boolean checkMaxVoucherSize(String clientId, long expireTime, long newFileSize) throws VoucherErrorException {

        // FIXME switch to regular Exception: UserException are only used on MRC!
        if (!clientExpireTimeSet.contains(clientId + expireTime)) {
            if (invalidClientExpireTimeSet.contains(clientId + expireTime)) {
                throw new VoucherErrorException(String.format(
                        "The given voucher has already been invalidated! [FielID: %s, ClientId: %s, expireTime: %s]",
                        fileId, clientId, expireTime));
            } else {
                Logging.logMessage(Logging.LEVEL_WARN, Category.proc, this,
                        "Unregistered voucher! [FileID: %s, ClientId %s, expireTime: %s]", fileId, clientId, expireTime);
            }
        }

        // check for maximum allowed size
        if (newFileSize <= voucherSizeMax) {
            System.out.println(getClass() + " checkMaxVoucherSize: " + newFileSize + " # " + voucherSizeMax); // FIXME(remove)
            return true;
        } else {
            return false;
        }
    }

    /**
     * TODO: save invalidated Vouchers locally in case of a crash approach
     * 
     * @param clientId
     * @param expireTimeSet
     */
    public void invalidateVouchers(String clientId, Set<Long> expireTimeSet) {

        for (Long expireTime : expireTimeSet) {
            clientExpireTimeSet.remove(clientId + expireTime);
            invalidClientExpireTimeSet.add(clientId + expireTime);

            // unseen vouchers have to be recognized for the latestExpireTime as well
            latestExpireTime = (latestExpireTime < expireTime) ? expireTime : latestExpireTime;
        }

        return;
    }

    /**
     * Checks, whether all voucher information regarding this file are invalid by themselves
     * 
     * @return
     */
    public boolean isObsolete() {
        return (TimeSync.getGlobalTime() / 1000) > latestExpireTime;
    }
}
