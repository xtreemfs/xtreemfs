/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;

/**
 * This class contains all relevant information regarding the quota of an volume.
 * 
 * It also allocates resources for requesting parties and blocks them, until they get freed.
 */
public class VolumeQuotaManager {

    private final StorageManager      volStorageManager;
    private final QuotaChangeListener quotaChangeListener;

    private final String              volumeId;

    private boolean                   active             = false;

    private long                      volumeQuota        = 0;
    private long                      curBlockedSpace    = 0;
    private long                      curUsedSpace       = 0;

    private long                      volumeVoucherSize = 250 * 1024 * 1024; // 100 MB

    /**
     * 
     */
    public VolumeQuotaManager(StorageManager volStorageManager, String volumeId) {

        this.volStorageManager = volStorageManager;
        this.volumeId = volumeId;

        quotaChangeListener = new QuotaChangeListener(this);
        volStorageManager.addVolumeChangeListener(quotaChangeListener);
    }

    public void init() {
        try {
            volumeQuota = volStorageManager.getVolumeQuota();
            curBlockedSpace = volStorageManager.getVolumeBlockedSpace();
            curUsedSpace = volStorageManager.getVolumeUsedSpace();
            // defaultVoucherSize = volStorageManager.
        } catch (DatabaseException e) {
            e.printStackTrace();
        }

        if (volumeQuota > 0) {
            setActive(true);
        }

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager loaded for volume: " + volumeId
                + ". [curVolumeSpace=" + curUsedSpace + ", curBlockedSpace=" + curBlockedSpace + ", maxVolumeSpace="
                + volumeQuota + "]");
    }

    private long getFreeSpace() {
        return volumeQuota - (curUsedSpace + curBlockedSpace);
    }

    public boolean checkVoucherAvailability() throws UserException {
        return getVoucher(true, null) > -1;
    }

    public long getVoucher(AtomicDBUpdate update) throws UserException {
        return getVoucher(false, update);
    }

    // TODO: pass user and user group to calculate over all voucher
    public synchronized long getVoucher(boolean test, AtomicDBUpdate update) throws UserException {

        if (!active) {
            return 0;
        }

        long currentFreeSpace = getFreeSpace();
        if (currentFreeSpace <= 0) {
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOSPC, "The quota of the volume \"" + volumeId
                    + "\" is reached");
        }

        long voucherSize = volumeVoucherSize;
        if (volumeVoucherSize > currentFreeSpace) {
            voucherSize = currentFreeSpace;
        }

        // save voucherSize as blocked, if it isn't just a check
        if (!test) {
            curBlockedSpace += voucherSize;

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                    + ") increased blocked space by: " + voucherSize + " to: " + curBlockedSpace);
            try {
                volStorageManager.setVolumeBlockedSpace(curBlockedSpace, update);
            } catch (DatabaseException e) {
                // this should never occure here, cause it will be executed outside
                // FIXME(baerhold): Use Logging?
                e.printStackTrace();
            }
        }

        return voucherSize;
    }

    /**
     * Updates the volume space statistics
     * 
     * Remove all blocked space regarding the file, because the filesize difference contains all the relevant space
     * information for future requests
     * 
     * @param usedSpace
     *            updated file size difference
     * @param clearBlockedSpace
     *            unused blocked space
     */
    public synchronized void updateSpaceUsage(long fileSizeDifference, long clearBlockedSpace, AtomicDBUpdate update) {
        if (!active) {
            return;
        }

        curUsedSpace += fileSizeDifference;
        curBlockedSpace -= clearBlockedSpace;

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                + ") updated space usage: curVolumeSpace=" + curUsedSpace + ", curBlockedSpace=" + curBlockedSpace
                + ", volumeQuota=" + volumeQuota);

        try {
            volStorageManager.setVolumeBlockedSpace(curBlockedSpace, update);
            volStorageManager.setVolumeUsedSpace(curUsedSpace, update);
        } catch (DatabaseException e) {
            // this should never occure here, cause it will be executed outside
            // FIXME(baerhold): Use Logging?
            e.printStackTrace();
        }

    }

    /**
     * 
     * @return whether the volume has full or not
     */
    public boolean isVolumeFull() {
        return (getFreeSpace() <= 0);
    }

    /**
     * @return the volumeName
     */
    public String getVolumeId() {
        return volumeId;
    }

    /**
     * @param volumeQuota
     *            the volumeQuota to set
     */
    public void setVolumeQuota(long volumeQuota) {
        this.volumeQuota = volumeQuota;

        setActive(volumeQuota != 0);

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId + ") changed quota to: "
                + volumeQuota);
    }
    
    public void setVolumeVoucherSize(long volumeVoucherSize) {
        this.volumeVoucherSize = volumeVoucherSize;

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId + ") set voucher size to: "
                + volumeVoucherSize);
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active
     *            the active to set
     */
    public void setActive(boolean active) {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId + ") changed active state to: "
                + active);

        this.active = active;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "VolumeQuotaManager [volumeId=" + volumeId + ", active=" + active + ", volumeQuota=" + volumeQuota
                + ", curVolumeSpace=" + curUsedSpace + ", curBlockedSpace=" + curBlockedSpace + "]";
    }
}
