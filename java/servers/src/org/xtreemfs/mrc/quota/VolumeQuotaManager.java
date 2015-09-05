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

    public final static long          defaultVoucherSize = 250 * 1024 * 1024; // 250 MB

    private final StorageManager      volStorageManager;
    private final QuotaChangeListener quotaChangeListener;

    private final String              volumeId;

    private boolean                   active            = false;

    private long                      volumeQuota       = 0;
    private long                      volumeVoucherSize  = 0;

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
            setVolumeQuota(volStorageManager.getVolumeQuota());
        } catch (DatabaseException e) {
            e.printStackTrace();
        }

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager loaded for volume: " + volumeId
                + ". [maxVolumeSpace=" + volumeQuota + "]");
    }

    public boolean checkVoucherAvailability() throws UserException {
        return getVoucher(true, null) > -1;
    }

    public long getVoucher(AtomicDBUpdate update) throws UserException {
        return getVoucher(false, update);
    }

    // TODO: pass user and user group to calculate over all voucher
    private synchronized long getVoucher(boolean test, AtomicDBUpdate update) throws UserException {

        if (!active) {
            return 0;
        }

        try {
            long usedSpace = volStorageManager.getVolumeUsedSpace();
            long blockedSpace = volStorageManager.getVolumeBlockedSpace();

            long currentFreeSpace = volumeQuota - (usedSpace + blockedSpace);
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
                blockedSpace += voucherSize;
                volStorageManager.setVolumeBlockedSpace(blockedSpace, update);

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                        + ") increased blocked space by: " + voucherSize + " to: " + blockedSpace);
            }

            return voucherSize;
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
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
     * @throws UserException
     */
    public synchronized void updateSpaceUsage(long fileSizeDifference, long clearBlockedSpace, AtomicDBUpdate update)
            throws UserException {
        if (!active) {
            return;
        }

        try {
            long usedSpace = volStorageManager.getVolumeUsedSpace();
            long blockedSpace = volStorageManager.getVolumeBlockedSpace();

            usedSpace += fileSizeDifference;
            blockedSpace -= clearBlockedSpace;

            volStorageManager.setVolumeUsedSpace(usedSpace, update);
            volStorageManager.setVolumeBlockedSpace(blockedSpace, update);

            if (usedSpace < 0) {
                Logging.logMessage(Logging.LEVEL_WARN, this, "VolumeQuotaManager(" + volumeId
                        + ") got negative space consumption! Please check the functionality!");
            }

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                    + ") updated space usage: usedSpace=" + usedSpace + ", blockedSpace=" + blockedSpace
                    + ", volumeQuota=" + volumeQuota);

        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
    }

    /**
     * @return the volumeName
     */
    public String getVolumeId() {
        return volumeId;
    }

    /**
     * @return the volStorageManager
     */
    public StorageManager getVolStorageManager() {
        return volStorageManager;
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

    @Override
    public String toString() {
        return "VolumeQuotaManager [volumeId=" + volumeId + ", active=" + active + ", volumeQuota=" + volumeQuota
                + ", volumeVoucherSize=" + volumeVoucherSize + "]";
    }
}
