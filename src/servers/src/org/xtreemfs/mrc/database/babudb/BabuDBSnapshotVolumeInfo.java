/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
Grid Operating System, see <http://www.xtreemos.eu> for more details.
The XtreemOS project has been developed with the financial support of the
European Commission's IST program under contract #FP6-033576.

XtreemFS is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation, either version 2 of the License, or (at your option)
any later version.

XtreemFS is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.database.babudb;

import org.xtreemfs.include.common.logging.Logging;
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
public class BabuDBSnapshotVolumeInfo implements VolumeInfo {
    
    private String  id;
    
    private String  name;
    
    private short[] osdPolicy;
    
    private short[] replicaPolicy;
    
    private short   acPolicy;
    
    private int     replFactor;
    
    private boolean replFull;
    
    private long creationTimestamp;
    
    private BabuDBSnapshotStorageManager sMan;
    
    public BabuDBSnapshotVolumeInfo(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }
    
    public void init(BabuDBSnapshotStorageManager sMan) throws DatabaseException {
        
        this.sMan = sMan;
        
        try {
            id = sMan.getXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_ID_ATTR_NAME);
            name = sMan.getVolumeName();
            osdPolicy = Converter.stringToShortArray(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.OSD_POL_ATTR_NAME));
            replicaPolicy = Converter.stringToShortArray(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.REPL_POL_ATTR_NAME));
            acPolicy = Short.parseShort(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.AC_POL_ATTR_NAME));
            replFactor = Integer.parseInt(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.AUTO_REPL_FACTOR_ATTR_NAME));
            replFull = Boolean.getBoolean(sMan.getXAttr(1, StorageManager.SYSTEM_UID,
                BabuDBStorageManager.AUTO_REPL_FULL_ATTR_NAME));
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
        sMan.throwException();
    }
    
    @Override
    public void setReplicaPolicy(short[] replicaPolicy, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }
    
    @Override
    public void setAutoReplFactor(int replFactor, AtomicDBUpdate update) throws DatabaseException {
        sMan.throwException();
    }
    
    @Override
    public void setAutoReplFull(boolean replFull, AtomicDBUpdate update) throws DatabaseException {
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
    public int getAutoReplFactor() {
        return replFactor;
    }
    
    @Override
    public boolean getAutoReplFull() {
        return replFull;
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
