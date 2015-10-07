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
public class BabuDBVolumeInfo implements VolumeInfo {
    
    private BabuDBStorageManager sMan;
    
    private String               id;
    
    private String               name;
    
    private short[]              osdPolicy;
    
    private short[]              replicaPolicy;
    
    private short                acPolicy;
    
    private boolean              allowSnaps;

    private long                 quota;

    private int                  priority;

    public void init(BabuDBStorageManager sMan, String id, String name, short[] osdPolicy, short[] replicaPolicy,
            short acPolicy, boolean allowSnaps, long quota, int priority, AtomicDBUpdate update) throws DatabaseException {
        
        this.sMan = sMan;
        this.id = id;
        this.name = name;
        this.osdPolicy = osdPolicy;
        this.replicaPolicy = replicaPolicy;
        this.acPolicy = acPolicy;
        this.allowSnaps = allowSnaps;
        this.quota = quota;
        this.priority = priority;
        
        // set the policies
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_ID_ATTR_NAME, id.getBytes(), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.OSD_POL_ATTR_NAME, Converter
                .shortArrayToString(osdPolicy).getBytes(), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.REPL_POL_ATTR_NAME, Converter
                .shortArrayToString(replicaPolicy).getBytes(), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.AC_POL_ATTR_NAME, String.valueOf(acPolicy)
                .getBytes(), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.ALLOW_SNAPS_ATTR_NAME,
                String.valueOf(allowSnaps).getBytes(), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_QUOTA_ATTR_NAME, String.valueOf(quota)
                .getBytes(), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_PRIORITY_ATTR_NAME, String.valueOf(priority)
                .getBytes(), true, update);
    }
    
    public void init(BabuDBStorageManager sMan) throws DatabaseException {
        
        this.sMan = sMan;
        
        try {
            
            // retrieve volume attributes
            byte[] idAttr = sMan.getXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_ID_ATTR_NAME);
            byte[] acPolicyAttr = sMan.getXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.AC_POL_ATTR_NAME);
            byte[] allowSnapsAttr = sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                    BabuDBStorageManager.ALLOW_SNAPS_ATTR_NAME);
            byte[] osdPolicyAttr = sMan.getXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.OSD_POL_ATTR_NAME);
            byte[] replicaPolicyAttr = sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                    BabuDBStorageManager.REPL_POL_ATTR_NAME);
            byte[] quotaAttr = sMan.getXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_QUOTA_ATTR_NAME);
            
            if (idAttr != null)
                id = new String(idAttr);
            else
                throw new DatabaseException("database corrupted: volume parameter 'id' not found",
                        ExceptionType.INTERNAL_DB_ERROR);
            
            name = sMan.getMetadata(1).getFileName();
            
            if (osdPolicyAttr != null)
                osdPolicy = Converter.stringToShortArray(new String(osdPolicyAttr));
            else
                throw new DatabaseException("database corrupted: volume parameter 'osdPolicy' not found",
                        ExceptionType.INTERNAL_DB_ERROR);
            
            if (replicaPolicyAttr != null)
                replicaPolicy = Converter.stringToShortArray(new String(replicaPolicyAttr));
            else
                throw new DatabaseException("database corrupted: volume parameter 'replicaPolicy' not found",
                        ExceptionType.INTERNAL_DB_ERROR);
            
            if (acPolicyAttr != null)
                acPolicy = Short.parseShort(new String(acPolicyAttr));
            else
                throw new DatabaseException("database corrupted: volume parameter 'acPolicy' not found",
                        ExceptionType.INTERNAL_DB_ERROR);
            
            if (allowSnapsAttr != null)
                allowSnaps = "true".equalsIgnoreCase(new String(allowSnapsAttr));

            if (quotaAttr != null)
                quota = Long.valueOf(new String(quotaAttr));

        } catch (NumberFormatException exc) {
            Logging.logError(Logging.LEVEL_ERROR, this, exc);
            throw new DatabaseException("corrupted MRC database", ExceptionType.INTERNAL_DB_ERROR);
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
    public short getAcPolicyId() {
        return acPolicy;
    }
    
    @Override
    public long getVolumeQuota() throws DatabaseException {
        return quota;
    }

    @Override
    public void setOsdPolicy(short[] osdPolicy, AtomicDBUpdate update) throws DatabaseException {
        this.osdPolicy = osdPolicy;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.OSD_POL_ATTR_NAME, Converter
                .shortArrayToString(osdPolicy).getBytes(), update);
        sMan.notifyVolumeChange(this);
    }
    
    @Override
    public void setReplicaPolicy(short[] replicaPolicy, AtomicDBUpdate update) throws DatabaseException {
        this.replicaPolicy = replicaPolicy;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.REPL_POL_ATTR_NAME, Converter
                .shortArrayToString(replicaPolicy).getBytes(), update);
        sMan.notifyVolumeChange(this);
    }
    
    @Override
    public void setAllowSnaps(boolean allowSnaps, AtomicDBUpdate update) throws DatabaseException {
        this.allowSnaps = allowSnaps;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.ALLOW_SNAPS_ATTR_NAME,
                String.valueOf(allowSnaps).getBytes(), update);
        sMan.notifyVolumeChange(this);
    }
    
    @Override
    public void setVolumeQuota(long quota, AtomicDBUpdate update) throws DatabaseException {
        this.quota = quota;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_QUOTA_ATTR_NAME, String.valueOf(quota)
                .getBytes(), update);
        sMan.notifyVolumeChange(this);
    }

    @Override
    public void updateVolumeSize(long diff, AtomicDBUpdate update) throws DatabaseException {
        sMan.updateVolumeSize(diff, update);
    }

    @Override
    public void setRequestPriority(int priority, AtomicDBUpdate update) throws DatabaseException {
        this.priority = priority;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_PRIORITY_ATTR_NAME, String.valueOf(priority)
                .getBytes(), update);
        sMan.notifyVolumeChange(this);
    }

    @Override
    public long getNumFiles() throws DatabaseException {
        return sMan.getNumFiles();
    }
    
    @Override
    public long getNumDirs() throws DatabaseException {
        return sMan.getNumDirs();
    }
    
    @Override
    public long getVolumeSize() throws DatabaseException {
        return sMan.getVolumeSize();
    }
    
    @Override
    public boolean isSnapVolume() throws DatabaseException {
        return false;
    }
    
    @Override
    public boolean isSnapshotsEnabled() throws DatabaseException {
        return allowSnaps;
    }
    
    @Override
    public long getCreationTime() throws DatabaseException {
        return 0;
    }

    @Override
    public int getRequestPriority() throws DatabaseException {
        return priority;
    }
}
