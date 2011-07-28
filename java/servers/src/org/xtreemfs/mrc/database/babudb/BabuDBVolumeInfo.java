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
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
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
    
    public void init(BabuDBStorageManager sMan, String id, String name, short[] osdPolicy,
        short[] replicaPolicy, short acPolicy, boolean allowSnaps, AtomicDBUpdate update)
        throws DatabaseException {
        
        this.sMan = sMan;
        this.id = id;
        this.name = name;
        this.osdPolicy = osdPolicy;
        this.replicaPolicy = replicaPolicy;
        this.acPolicy = acPolicy;
        this.allowSnaps = allowSnaps;
        
        // set the policies
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_ID_ATTR_NAME, id, true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.OSD_POL_ATTR_NAME, Converter
                .shortArrayToString(osdPolicy), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.REPL_POL_ATTR_NAME, Converter
                .shortArrayToString(replicaPolicy), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.AC_POL_ATTR_NAME, String
                .valueOf(acPolicy), true, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.ALLOW_SNAPS_ATTR_NAME, String
                .valueOf(allowSnaps), true, update);
    }
    
    public void init(BabuDBStorageManager sMan) throws DatabaseException {
        
        this.sMan = sMan;
        
        try {
            id = sMan.getXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_ID_ATTR_NAME);
            name = sMan.getMetadata(1).getFileName();
            osdPolicy = Converter.stringToShortArray(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.OSD_POL_ATTR_NAME));
            replicaPolicy = Converter.stringToShortArray(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.REPL_POL_ATTR_NAME));
            acPolicy = Short.parseShort(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.AC_POL_ATTR_NAME));
            allowSnaps = "true".equalsIgnoreCase(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.ALLOW_SNAPS_ATTR_NAME));
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
    public short getAcPolicyId() {
        return acPolicy;
    }
    
    @Override
    public void setOsdPolicy(short[] osdPolicy, AtomicDBUpdate update) throws DatabaseException {
        this.osdPolicy = osdPolicy;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.OSD_POL_ATTR_NAME, Converter
                .shortArrayToString(osdPolicy), update);
        sMan.notifyVolumeChange(this);
    }
    
    @Override
    public void setReplicaPolicy(short[] replicaPolicy, AtomicDBUpdate update) throws DatabaseException {
        this.replicaPolicy = replicaPolicy;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.REPL_POL_ATTR_NAME, Converter
                .shortArrayToString(replicaPolicy), update);
        sMan.notifyVolumeChange(this);
    }
    
    @Override
    public void setAllowSnaps(boolean allowSnaps, AtomicDBUpdate update) throws DatabaseException {
        this.allowSnaps = allowSnaps;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.ALLOW_SNAPS_ATTR_NAME, String
                .valueOf(allowSnaps), update);
        sMan.notifyVolumeChange(this);
    }
    
    @Override
    public void updateVolumeSize(long diff, AtomicDBUpdate update) throws DatabaseException {
        sMan.updateVolumeSize(diff, update);
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
    
}
