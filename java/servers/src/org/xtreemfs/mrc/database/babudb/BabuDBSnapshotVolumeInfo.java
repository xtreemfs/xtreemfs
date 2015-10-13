/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database.babudb;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.utils.Converter;

/**
 * A <code>VolumeInfo</code> implementation backed by a byte buffer.
 * 
 * @author stender
 * 
 */
public class BabuDBSnapshotVolumeInfo implements VolumeInfo {
    
    private String  id;
    
    private String  name;
    
    private short[] osdPolicy;
    
    private short[] replicaPolicy;
    
    private short   acPolicy;

    private final long creationTimestamp;
    
    private BabuDBSnapshotStorageManager sMan;
    
    public BabuDBSnapshotVolumeInfo(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }
    
    public void init(BabuDBSnapshotStorageManager sMan) throws DatabaseException {
        
        this.sMan = sMan;
        
        try {
            id = new String(sMan.getXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_ID_ATTR_NAME));
            name = sMan.getVolumeName();
            osdPolicy = Converter.stringToShortArray(new String(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                    BabuDBStorageManager.OSD_POL_ATTR_NAME)));
            replicaPolicy = Converter.stringToShortArray(new String(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                    BabuDBStorageManager.REPL_POL_ATTR_NAME)));
            acPolicy = Short.parseShort(new String(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                    BabuDBStorageManager.AC_POL_ATTR_NAME)));
        } catch (NumberFormatException exc) {
            Logging.logError(Logging.LEVEL_ERROR, this, exc);
            throw new DatabaseException("currpted MRC database", ExceptionType.INTERNAL_DB_ERROR);
        }
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public short[] getOsdPolicy() {
        return osdPolicy;
    }
    
    @Override
    public short[] getReplicaPolicy() {
        return replicaPolicy;
    }
    
    @Override
    public long getVolumeQuota() throws DatabaseException {
        return sMan.getVolumeQuota();
    }

    @Override
    public long getVoucherSize() throws DatabaseException {
        return sMan.getVoucherSize();
    }

    @Override
    public long getDefaultGroupQuota() throws DatabaseException {
        return sMan.getDefaultGroupQuota();
    }

    @Override
    public long getDefaultUserQuota() throws DatabaseException {
        return sMan.getDefaultUserQuota();
    }

    @Override
    public short getAcPolicyId() {
        return acPolicy;
    }
    
    @Override
    public void setOsdPolicy(short[] osdPolicy, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }
    
    @Override
    public void setReplicaPolicy(short[] replicaPolicy, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }
    
    @Override
    public void setVolumeQuota(long quota, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }

    @Override
    public void setVoucherSize(long voucherSize, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }

    @Override
    public void setDefaultGroupQuota(long defaultGroupQuota, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }

    @Override
    public void setDefaultUserQuota(long defaultUserQuota, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }

    @Override
    public void setAllowSnaps(boolean allowSnaps, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }
    
    @Override
    public void updateVolumeSize(long diff, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }

    @Override
    public long getNumFiles() throws DatabaseException {
        return 0;
    }
    
    @Override
    public long getNumDirs() throws DatabaseException {
        return 0;
    }
    
    @Override
    public long getVolumeSize() throws DatabaseException {
        return 0;
    }
    
    @Override
    public boolean isSnapVolume() throws DatabaseException {
        return true;
    }

    @Override
    public boolean isSnapshotsEnabled() throws DatabaseException {
        return true;
    }
    
    @Override
    public long getCreationTime() throws DatabaseException {
        return creationTimestamp;
    }
}
