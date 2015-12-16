/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCException;
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

    public final static long          DEFAULT_VOUCHER_SIZE    = QuotaConstants.DEFAULT_VOUCHER_SIZE;
    public final static long          DEFAULT_USER_QUOTA      = QuotaConstants.UNLIMITED_QUOTA; // no limit
    public final static long          DEFAULT_GROUP_QUOTA     = QuotaConstants.UNLIMITED_QUOTA; // no limit

    private final StorageManager      volStorageManager;
    private final QuotaChangeListener quotaChangeListener;
    private final QuotaManager        mrcQuotaManager;

    private final String              volumeId;

    private long                      volumeQuota             = 0;
    private long                      volumeVoucherSize       = 0;
    private long                      volumeDefaultUserQuota  = 0;
    private long                      volumeDefaultGroupQuota = 0;

    /**
     * Creates the volume quota manager and register at the mrc quota manager. Add a change listener to the volume info
     * to get up to date information.
     * 
     * @throws MRCException
     */
    public VolumeQuotaManager(QuotaManager mrcQuotaManager, StorageManager volStorageManager, String volumeId)
            throws MRCException {

        this.mrcQuotaManager = mrcQuotaManager;
        this.volStorageManager = volStorageManager;
        this.volumeId = volumeId;

        mrcQuotaManager.addVolumeQuotaManager(this);

        quotaChangeListener = new QuotaChangeListener(this);
        volStorageManager.addVolumeChangeListener(quotaChangeListener);
    }

    /**
     * Sets the volume specific members with the database values instead of pulling them on every request. They will be
     * uptodate due to the change listener on the volume.
     */
    public void init() {
        try {
            setVolumeQuota(volStorageManager.getVolumeQuota());
            setVolumeVoucherSize(volStorageManager.getVoucherSize());
            setVolumeDefaultGroupQuota(volStorageManager.getDefaultGroupQuota());
            setVolumeDefaultUserQuota(volStorageManager.getDefaultUserQuota());
        } catch (DatabaseException e) {
            e.printStackTrace();
        }

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager loaded for volume: " + volumeId
                + ". [volumeQuota=" + volumeQuota + ", volumeVoucherSize=" + volumeVoucherSize
                + ", volumeDefaultGroupQuota=" + volumeDefaultGroupQuota + ", volumeDefaultUserQuota="
                + volumeDefaultUserQuota + "]");
    }

    public boolean checkVoucherAvailability(QuotaFileInformation quotaFileInformation) throws UserException {
        long voucherSize = getVoucher(quotaFileInformation, true, null);
        return voucherSize > 0 || voucherSize == QuotaConstants.UNLIMITED_VOUCHER;
    }

    public long getVoucher(QuotaFileInformation quotaFileInformation, AtomicDBUpdate update) throws UserException {
        return getVoucher(quotaFileInformation, false, update);
    }

    /**
     * Checks the active quota and returns a voucher, if no exception occured
     * 
     * @param quotaFileInformation
     * @param test
     * @param update
     * @return
     * @throws UserException
     */
    private synchronized long getVoucher(QuotaFileInformation quotaFileInformation, boolean test, AtomicDBUpdate update)
            throws UserException {

        int replicaCount = quotaFileInformation.getReplicaCount();
        QuotaInformation quotaInformation = getAndApplyQuotaInformation(quotaFileInformation, !test, update);
        long freeSpace = quotaInformation.getFreeSpace();

        long voucherSize = volumeVoucherSize;
        if (quotaInformation.getVolumeQuota() == QuotaConstants.UNLIMITED_QUOTA
                && quotaInformation.getUserQuota() == QuotaConstants.UNLIMITED_QUOTA
                && quotaInformation.getGroupQuota() == QuotaConstants.UNLIMITED_QUOTA) {
            // no quota set at all: unlimited voucher
            voucherSize = QuotaConstants.UNLIMITED_VOUCHER;
        } else if (freeSpace / replicaCount == 0) { // can't get negative
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOSPC, "The " + quotaInformation.getQuotaType()
                    + " quota has been reached!");
        } else if ((replicaCount * voucherSize) > freeSpace) {
            voucherSize = freeSpace / replicaCount;
        }

        // save voucherSize as blocked, if it isn't just a check
        if (!test) {
            updateSpaceUsage(quotaFileInformation, quotaInformation, 0, replicaCount * voucherSize, update);
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
     * @throws UserException
     */
    public synchronized void updateSpaceUsage(QuotaFileInformation quotaFileInformation, long fileSizeDifference,
            long blockedSpaceDifference, AtomicDBUpdate update) throws UserException {

        QuotaInformation quotaInformation = getAndApplyQuotaInformation(quotaFileInformation, true, update);

        updateSpaceUsage(quotaFileInformation, quotaInformation, fileSizeDifference, blockedSpaceDifference, update);

    }

    public synchronized void updateSpaceUsage(QuotaFileInformation quotaFileInformation,
            QuotaInformation quotaInformation, long filesizeDifference, long blockedSpaceDifference,
            AtomicDBUpdate update) throws UserException {

        updateVolumeSpaceUsage(quotaFileInformation, quotaInformation, filesizeDifference, blockedSpaceDifference,
                update);
        updateUserSpaceUsage(quotaFileInformation, quotaInformation, filesizeDifference, blockedSpaceDifference, update);
        updateGroupSpaceUsage(quotaFileInformation, quotaInformation, filesizeDifference, blockedSpaceDifference,
                update);
    }

    public synchronized void updateVolumeSpaceUsage(QuotaFileInformation quotaFileInformation,
            QuotaInformation quotaInformation, long filesizeDifference, long blockedSpaceDifference,
            AtomicDBUpdate update) throws UserException {
        try {
            if (filesizeDifference != 0) {
                long volumeUsedSpace = quotaInformation.getVolumeUsedSpace() + filesizeDifference;
                checkNegativeValue(volumeUsedSpace, "volume", true);
                volStorageManager.setVolumeUsedSpace(volumeUsedSpace, update);

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                        + ") changed volume used space by: " + filesizeDifference + " to: " + volumeUsedSpace);
            }

            if (blockedSpaceDifference != 0) {
                if (quotaInformation.getVolumeQuota() != QuotaConstants.UNLIMITED_QUOTA) {
                    long volumeBlockedSpace = quotaInformation.getVolumeBlockedSpace() + blockedSpaceDifference;
                    checkNegativeValue(volumeBlockedSpace, "volume", false);
                    volStorageManager.setVolumeBlockedSpace(volumeBlockedSpace, update);

                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                            + ") changed volume blocked space by: " + blockedSpaceDifference + " to: "
                            + volumeBlockedSpace);
                }
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
    }

    public synchronized void updateUserSpaceUsage(QuotaFileInformation quotaFileInformation,
            QuotaInformation quotaInformation, long filesizeDifference, long blockedSpaceDifference,
            AtomicDBUpdate update) throws UserException {

        String ownerId = quotaFileInformation.getOwnerId();
        try {
            if (filesizeDifference != 0) {
                long userUsedSpace = quotaInformation.getUserUsedSpace() + filesizeDifference;
                checkNegativeValue(userUsedSpace, "ownerId: " + ownerId, true);
                volStorageManager.setUserUsedSpace(ownerId, userUsedSpace, update);

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId + ") changed owner ("
                        + ownerId + ") used space by: " + filesizeDifference + " to: " + userUsedSpace);
            }

            if (blockedSpaceDifference != 0) {
                if (quotaInformation.getUserQuota() != QuotaConstants.UNLIMITED_QUOTA) {
                    long userBlockedSpace = quotaInformation.getUserBlockedSpace() + blockedSpaceDifference;
                    checkNegativeValue(userBlockedSpace, "ownerId: " + ownerId, false);
                    volStorageManager.setUserBlockedSpace(ownerId, userBlockedSpace, update);

                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                            + ") changed owner (" + ownerId + ") blocked space by: " + blockedSpaceDifference + " to: "
                            + userBlockedSpace);
                }
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
    }

    public synchronized void updateGroupSpaceUsage(QuotaFileInformation quotaFileInformation,
            QuotaInformation quotaInformation, long filesizeDifference, long blockedSpaceDifference,
            AtomicDBUpdate update) throws UserException {

        String ownerGroupId = quotaFileInformation.getOwnerGroupId();
        try {
            if (filesizeDifference != 0) {
                long groupUsedSpace = quotaInformation.getGroupUsedSpace() + filesizeDifference;
                checkNegativeValue(groupUsedSpace, "ownerGroupId: " + ownerGroupId, true);
                volStorageManager.setGroupUsedSpace(ownerGroupId, groupUsedSpace, update);

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                        + ") changed owner group (" + ownerGroupId + ") used space by: " + filesizeDifference + " to: "
                        + groupUsedSpace);
            }

            if (blockedSpaceDifference != 0) {
                if (quotaInformation.getGroupQuota() != QuotaConstants.UNLIMITED_QUOTA) {
                    long groupBlockedSpace = quotaInformation.getGroupBlockedSpace() + blockedSpaceDifference;
                    checkNegativeValue(groupBlockedSpace, "ownerGroupId: " + ownerGroupId, false);
                    volStorageManager.setGroupBlockedSpace(ownerGroupId, groupBlockedSpace, update);

                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                            + ") changed owner group (" + ownerGroupId + ") blocked space by: "
                            + blockedSpaceDifference + " to: " + groupBlockedSpace);
                }
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }
    }

    /**
     * Checks the quota and adds the replica values, if enough space is avilable
     * 
     * @param quotaFileInformation
     * @param filesize
     * @param blockedSpace
     * @param update
     * @throws UserException
     */
    public synchronized void addReplica(QuotaFileInformation quotaFileInformation, long filesize, long blockedSpace,
            AtomicDBUpdate update) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                + ") tries to add a replica to current space information.");

        QuotaInformation quotaInformation = getAndApplyQuotaInformation(quotaFileInformation, true, update);

        if (quotaInformation.getFreeSpace() < (filesize + blockedSpace)) {
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOSPC, "Not enough space for a new replica! The "
                    + quotaInformation.getQuotaType() + " quota has been reached!");
        }

        updateSpaceUsage(quotaFileInformation, quotaInformation, filesize, blockedSpace, update);
    }

    /**
     * Transfers the space information (used & blocked) for a file to the new owner
     * 
     * @param quotaFileInformation
     * @param newOwnerId
     * @param filesize
     * @param blockedSpace
     * @param update
     * @throws UserException
     */
    public synchronized void transferOwnerSpace(QuotaFileInformation quotaFileInformation, String newOwnerId,
            long filesize, long blockedSpace, AtomicDBUpdate update) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                + ") tries transfer the space information to the new owner.");

        QuotaFileInformation newQuotaFileInformation = new QuotaFileInformation(quotaFileInformation);
        newQuotaFileInformation.setOwnerId(newOwnerId);

        QuotaInformation quotaInformationOldOwner = getAndApplyUserQuotaInformation(null, quotaFileInformation, true,
                update);
        QuotaInformation quotaInformationNewOwner = getAndApplyUserQuotaInformation(null, newQuotaFileInformation,
                true, update);

        // SuppressWarning(unused): Due to checkQuotaOnChown, which is currently a hardcoded switch
        if (QuotaConstants.CHECK_QUOTA_ON_CHOWN && quotaInformationNewOwner.getFreeSpace() < (filesize + blockedSpace)) {
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOSPC, "Not enough space the transfer ownership! The "
                    + quotaInformationNewOwner.getQuotaType() + " quota has been reached!");
        }

        // remove space from old owner
        updateUserSpaceUsage(quotaFileInformation, quotaInformationOldOwner, -1 * filesize, -1 * blockedSpace, update);

        // add space to new owner
        updateUserSpaceUsage(newQuotaFileInformation, quotaInformationNewOwner, filesize, blockedSpace, update);
    }

    /**
     * Transfers the space information (used & blocked) for a file to the new owner group
     * 
     * @param quotaFileInformation
     * @param newOwnerGroupId
     * @param filesize
     * @param blockedSpace
     * @param update
     * @throws UserException
     */
    public synchronized void transferOwnerGroupSpace(QuotaFileInformation quotaFileInformation, String newOwnerGroupId,
            long filesize, long blockedSpace, AtomicDBUpdate update) throws UserException {

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                + ") tries transfer the space information to the new owner group.");

        QuotaFileInformation newQuotaFileInformation = new QuotaFileInformation(quotaFileInformation);
        newQuotaFileInformation.setOwnerGroupId(newOwnerGroupId);

        QuotaInformation quotaInformationOldOwnerGroup = getAndApplyGroupQuotaInformation(null, quotaFileInformation,
                true, update);
        QuotaInformation quotaInformationNewOwnerGroup = getAndApplyGroupQuotaInformation(null,
                newQuotaFileInformation, true, update);

        // SuppressWarning(unused): Due to checkQuotaOnChown, which is currently a hardcoded switch
        if (QuotaConstants.CHECK_QUOTA_ON_CHOWN
                && quotaInformationNewOwnerGroup.getFreeSpace() < (filesize + blockedSpace)) {
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOSPC, "Not enough space the transfer ownership! The "
                    + quotaInformationNewOwnerGroup.getQuotaType() + " quota has been reached!");
        }

        // remove space from old owner group
        updateGroupSpaceUsage(quotaFileInformation, quotaInformationOldOwnerGroup, -1 * filesize, -1 * blockedSpace,
                update);

        // add space to new owner group
        updateGroupSpaceUsage(newQuotaFileInformation, quotaInformationNewOwnerGroup, filesize, blockedSpace, update);
    }

    /**
     * Gets all quota information, applies the default user and group quota and saves them, if it has to.
     * 
     * @param quotaFileInformation
     * @param saveAppliedDefaultQuota
     * @param update
     * @return
     * @throws UserException
     */
    private QuotaInformation getAndApplyQuotaInformation(QuotaFileInformation quotaFileInformation,
            boolean saveAppliedDefaultQuota, AtomicDBUpdate update) throws UserException {

        QuotaInformation quotaInformation = new QuotaInformation(volumeQuota, 0, 0);

        quotaInformation = getAndApplyVolumeQuotaInformation(quotaInformation, quotaFileInformation,
                saveAppliedDefaultQuota, update);
        quotaInformation = getAndApplyUserQuotaInformation(quotaInformation, quotaFileInformation,
                saveAppliedDefaultQuota, update);
        quotaInformation = getAndApplyGroupQuotaInformation(quotaInformation, quotaFileInformation,
                saveAppliedDefaultQuota, update);

        return quotaInformation;
    }

    private QuotaInformation getAndApplyVolumeQuotaInformation(QuotaInformation quotaInformation,
            QuotaFileInformation quotaFileInformation, boolean saveAppliedDefaultQuota, AtomicDBUpdate update)
            throws UserException {

        if (quotaInformation == null) {
            quotaInformation = new QuotaInformation(volumeQuota, 0, 0);
        }

        try {
            long volumeUsedSpace = volStorageManager.getVolumeUsedSpace();
            quotaInformation.setVolumeUsedSpace(volumeUsedSpace);

            // check volume quota
            if (volumeQuota != QuotaConstants.UNLIMITED_QUOTA) {
                long volumeBlockedSpace = volStorageManager.getVolumeBlockedSpace();
                quotaInformation.setVolumeBlockedSpace(volumeBlockedSpace);

                long volumeFreeSpace = volumeQuota - (volumeUsedSpace + volumeBlockedSpace);
                if (volumeFreeSpace < quotaInformation.getFreeSpace()) {
                    quotaInformation.setFreeSpace(volumeFreeSpace);
                    quotaInformation.setQuotaType("volume");
                }
            }

        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }

        return quotaInformation;
    }

    private QuotaInformation getAndApplyUserQuotaInformation(QuotaInformation quotaInformation,
            QuotaFileInformation quotaFileInformation, boolean saveAppliedDefaultQuota, AtomicDBUpdate update)
            throws UserException {

        if (quotaInformation == null) {
            quotaInformation = new QuotaInformation(volumeQuota, 0, 0);
        }

        try {
            boolean userQuotaDefined = false;
            String ownerId = quotaFileInformation.getOwnerId();

            long userQuota = volStorageManager.getUserQuota(ownerId);
            quotaInformation.setUserQuota(userQuota);

            // check user quota
            if (userQuota == QuotaConstants.NO_QUOTA) {
                // set default user quota as new owner quota, if it had no value
                userQuota = volumeDefaultUserQuota;
                quotaInformation.setUserQuota(userQuota);
                userQuotaDefined = true;
            }

            long userUsedSpace = volStorageManager.getUserUsedSpace(ownerId);
            quotaInformation.setUserUsedSpace(userUsedSpace);

            if (userQuota != QuotaConstants.UNLIMITED_QUOTA) {
                long userBlockedSpace = volStorageManager.getUserBlockedSpace(ownerId);
                quotaInformation.setUserBlockedSpace(userBlockedSpace);

                long userFreeSpace = userQuota - (userUsedSpace + userBlockedSpace);
                if (userFreeSpace < quotaInformation.getFreeSpace()) {
                    quotaInformation.setFreeSpace(userFreeSpace);
                    quotaInformation.setQuotaType("file owner");
                }
            }

            // apply newly set quota
            if (saveAppliedDefaultQuota) {
                if (userQuota != QuotaConstants.UNLIMITED_QUOTA && userQuotaDefined) {
                    volStorageManager.setUserQuota(ownerId, userQuota, update);
                }
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }

        return quotaInformation;
    }

    private QuotaInformation getAndApplyGroupQuotaInformation(QuotaInformation quotaInformation,
            QuotaFileInformation quotaFileInformation, boolean saveAppliedDefaultQuota, AtomicDBUpdate update)
            throws UserException {

        if (quotaInformation == null) {
            quotaInformation = new QuotaInformation(volumeQuota, 0, 0);
        }

        try {
            boolean groupQuotaDefined = false;
            String ownerGroupId = quotaFileInformation.getOwnerGroupId();

            long groupQuota = volStorageManager.getGroupQuota(ownerGroupId);
            quotaInformation.setGroupQuota(groupQuota);

            // check group quota
            if (groupQuota == QuotaConstants.NO_QUOTA) {
                // set default group quota as new owner group quota, if it had no value
                groupQuota = volumeDefaultGroupQuota;
                quotaInformation.setGroupQuota(groupQuota);
                groupQuotaDefined = true;
            }

            long groupUsedSpace = volStorageManager.getGroupUsedSpace(ownerGroupId);
            quotaInformation.setGroupUsedSpace(groupUsedSpace);

            if (groupQuota != QuotaConstants.UNLIMITED_QUOTA) {
                long groupBlockedSpace = volStorageManager.getGroupBlockedSpace(ownerGroupId);
                quotaInformation.setGroupBlockedSpace(groupBlockedSpace);

                long groupFreeSpace = groupQuota - (groupUsedSpace + groupBlockedSpace);
                if (groupFreeSpace < quotaInformation.getFreeSpace()) {
                    quotaInformation.setFreeSpace(groupFreeSpace);
                    quotaInformation.setQuotaType("file owner group");
                }
            }

            // apply newly set quota
            if (saveAppliedDefaultQuota) {
                if (groupQuota != QuotaConstants.UNLIMITED_QUOTA && groupQuotaDefined) {
                    volStorageManager.setGroupQuota(ownerGroupId, groupQuota, update);
                }
            }
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, "An error occured during the interaction with the database!", e);

            throw new UserException(POSIXErrno.POSIX_ERROR_EIO,
                    "An error occured during the interaction with the database!");
        }

        return quotaInformation;
    }

    /**
     * Checks, whether a value is negative and iff so, a warning will be logged which type of space and whose space got
     * negative.
     * 
     * @param value
     * @param errorHint
     * @param isUsedSpace
     */
    private void checkNegativeValue(long value, String errorHint, boolean isUsedSpace) {
        String spaceType = isUsedSpace ? "used" : "blocked";
        if (value < 0) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "VolumeQuotaManager(" + volumeId
                    + ") got negative space consumption for the " + spaceType + " space of the " + errorHint
                    + "! Please check the functionality!");
        }
    }

    /**
     * Deletes the volumes quota manager by unregister itself at the mrc quota manager.
     * 
     * @throws Exception
     */
    public synchronized void delete() throws Exception {
        mrcQuotaManager.removeVolumeQuotaManager(this);
    }

    // Getter & Setter

    public String getVolumeId() {
        return volumeId;
    }

    public StorageManager getVolStorageManager() {
        return volStorageManager;
    }

    public void setVolumeQuota(long volumeQuota) {
        this.volumeQuota = volumeQuota;

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId + ") changed quota to: "
                + volumeQuota);
    }

    public void setVolumeVoucherSize(long volumeVoucherSize) {
        this.volumeVoucherSize = volumeVoucherSize;

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId + ") set voucher size to: "
                + volumeVoucherSize);
    }

    public void setVolumeDefaultUserQuota(long volumeDefaultUserQuota) {
        this.volumeDefaultUserQuota = volumeDefaultUserQuota;

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                + ") set volume default user quota to: " + volumeDefaultUserQuota);
    }

    public void setVolumeDefaultGroupQuota(long volumeDefaultGroupQuota) {
        this.volumeDefaultGroupQuota = volumeDefaultGroupQuota;

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "VolumeQuotaManager(" + volumeId
                + ") set volume default group quota to: " + volumeDefaultGroupQuota);
    }

    @Override
    public String toString() {
        return "VolumeQuotaManager [volumeId=" + volumeId + ", volumeQuota=" + volumeQuota + ", volumeVoucherSize="
                + volumeVoucherSize + ", volumeDefaultUserQuota=" + volumeDefaultUserQuota
                + ", volumeDefaultGroupQuota=" + volumeDefaultGroupQuota + "]";
    }

    private class QuotaInformation {

        private long   freeSpace          = Long.MAX_VALUE;
        private String quotaType          = null;

        private long   volumeQuota        = 0;
        private long   userQuota          = 0;
        private long   groupQuota         = 0;

        private long   volumeUsedSpace    = 0;
        private long   userUsedSpace      = 0;
        private long   groupUsedSpace     = 0;

        private long   volumeBlockedSpace = 0;
        private long   userBlockedSpace   = 0;
        private long   groupBlockedSpace  = 0;

        public QuotaInformation(long volumeQuota, long userQuota, long groupQuota) {
            this.volumeQuota = volumeQuota;
            this.userQuota = userQuota;
            this.groupQuota = groupQuota;
        }

        // Getter

        public long getFreeSpace() {
            return freeSpace;
        }

        public String getQuotaType() {
            return quotaType;
        }

        public long getVolumeQuota() {
            return volumeQuota;
        }

        public long getUserQuota() {
            return userQuota;
        }

        public long getGroupQuota() {
            return groupQuota;
        }

        public long getVolumeUsedSpace() {
            return volumeUsedSpace;
        }

        public long getUserUsedSpace() {
            return userUsedSpace;
        }

        public long getGroupUsedSpace() {
            return groupUsedSpace;
        }

        public long getVolumeBlockedSpace() {
            return volumeBlockedSpace;
        }

        public long getUserBlockedSpace() {
            return userBlockedSpace;
        }

        public long getGroupBlockedSpace() {
            return groupBlockedSpace;
        }

        // Setter

        public void setUserQuota(long userQuota) {
            this.userQuota = userQuota;
        }

        public void setGroupQuota(long groupQuota) {
            this.groupQuota = groupQuota;
        }

        public void setVolumeUsedSpace(long volumeUsedSpace) {
            this.volumeUsedSpace = volumeUsedSpace;
        }

        public void setUserUsedSpace(long userUsedSpace) {
            this.userUsedSpace = userUsedSpace;
        }

        public void setGroupUsedSpace(long groupUsedSpace) {
            this.groupUsedSpace = groupUsedSpace;
        }

        public void setVolumeBlockedSpace(long volumeBlockedSpace) {
            this.volumeBlockedSpace = volumeBlockedSpace;
        }

        public void setUserBlockedSpace(long userBlockedSpace) {
            this.userBlockedSpace = userBlockedSpace;
        }

        public void setGroupBlockedSpace(long groupBlockedSpace) {
            this.groupBlockedSpace = groupBlockedSpace;
        }

        public void setFreeSpace(long freeSpace) {
            this.freeSpace = freeSpace;
        }

        public void setQuotaType(String quotaType) {
            this.quotaType = quotaType;
        }
    }
}
