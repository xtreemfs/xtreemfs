/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

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

        System.out.println(getClass() + "Constructor" + "[id " + volumeId + "]"); // FIXME(remove)
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

        System.out.println(getClass() + "init " + "[curVolumeSpace=" + curUsedSpace + ", curBlockedSpace="
                + curBlockedSpace + ", maxVolumeSpace=" + volumeQuota + "]"); // FIXME(remove)
    }

    private long getFreeSpace() {
        return volumeQuota - (curUsedSpace + curBlockedSpace);
    }

    public long checkVoucherAvailability() throws UserException {
        return getVoucher(true);
    }

    public long getVoucher() throws UserException {
        return getVoucher(false);
    }

    // TODO: pass user and user group to calculate over all voucher
    public synchronized long getVoucher(boolean test) throws UserException {

        if (!active) {
            return 0;
        }

        System.out.println(getClass() + " getVoucher: test=" + test);// FIXME(remove)

        long currentFreeSpace = getFreeSpace();
        if (currentFreeSpace <= 0) {
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOSPC, "The quota of the volume \"" + volumeId
                    + "\" is reached");
        }

        long voucherSize = volumeVoucherSize;
        if (volumeVoucherSize > currentFreeSpace) {
            voucherSize = currentFreeSpace;
        }

        // block voucherSize to
        if (!test) {
            curBlockedSpace += voucherSize;

            try {
                AtomicDBUpdate update = volStorageManager.createAtomicDBUpdate(null, null);

                volStorageManager.setVolumeBlockedSpace(curBlockedSpace, update);

                update.execute();

            } catch (DatabaseException e) {
                // FIXME(baerhold): perform DB update outside
                e.printStackTrace();
            }

        }

        System.out.println(getClass() + " getVoucher: voucherSize=" + voucherSize + " [curVolumeSpace=" + curUsedSpace
                + ", curBlockedSpace=" + curBlockedSpace + ", volumeQuota=" + volumeQuota + "]"); // FIXME(remove)

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
    public synchronized void updateSpaceUsage(long fileSizeDifference, long clearBlockedSpace) {
        if (!active) {
            return;
        }

        curUsedSpace += fileSizeDifference;
        curBlockedSpace -= clearBlockedSpace;

        System.out.println(getClass() + " updateSpaceUsage: " + "[curVolumeSpace=" + curUsedSpace
                + ", curBlockedSpace=" + curBlockedSpace + ", volumeQuota=" + volumeQuota + "]"); // FIXME(remove)

        try {
            AtomicDBUpdate update = volStorageManager.createAtomicDBUpdate(null, null);

            volStorageManager.setVolumeBlockedSpace(curBlockedSpace, update);
            volStorageManager.setVolumeUsedSpace(curUsedSpace, update);

            update.execute();

        } catch (DatabaseException e) {
            // FIXME(baerhold): perform DB update outside
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

        System.out.println(getClass() + " setVolumeQuota: " + volumeQuota + " active: " + (volumeQuota != 0)); // FIXME(remove)
    }
    
    public void setVolumeVoucherSize(long volumeVoucherSize) {
        this.volumeVoucherSize = volumeVoucherSize;

        System.out.println(getClass() + " setVolumeVoucherSize: " + volumeVoucherSize); // FIXME(remove)
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
