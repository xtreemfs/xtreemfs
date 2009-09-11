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
public class BabuDBVolumeInfo implements VolumeInfo {
    
    private BabuDBStorageManager sMan;
    
    private String               id;
    
    private String               name;
    
    private short[]              osdPolicy;
    
    private short[]              replicaPolicy;
    
    private short                acPolicy;
    
    private int                  replFactor;
    
    private boolean              replFull;
    
    public void init(BabuDBStorageManager sMan, String id, String name, short[] osdPolicy,
        short[] replicaPolicy, short acPolicy, int replFactor, boolean replFull, AtomicDBUpdate update)
        throws DatabaseException {
        
        this.sMan = sMan;
        this.id = id;
        this.name = name;
        this.osdPolicy = osdPolicy;
        this.replicaPolicy = replicaPolicy;
        this.acPolicy = acPolicy;
        this.replFactor = replFactor;
        this.replFull = replFull;
        
        // set the policies
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.VOL_ID_ATTR_NAME, id, update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.OSD_POL_ATTR_NAME, Converter
                .shortArrayToString(osdPolicy), update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.REPL_POL_ATTR_NAME, Converter
                .shortArrayToString(replicaPolicy), update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.AUTO_REPL_FACTOR_ATTR_NAME, String
                .valueOf(replFactor), update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.AC_POL_ATTR_NAME, String
                .valueOf(acPolicy), update);
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.AUTO_REPL_FULL_ATTR_NAME, String
                .valueOf(replFull), update);
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
    public void setAutoReplFactor(int replFactor, AtomicDBUpdate update) throws DatabaseException {
        this.replFactor = replFactor;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.AUTO_REPL_FACTOR_ATTR_NAME, String
                .valueOf(replFactor), update);
        sMan.notifyVolumeChange(this);
    }
    
    @Override
    public void setAutoReplFull(boolean replFull, AtomicDBUpdate update) throws DatabaseException {
        this.replFull = replFull;
        sMan.setXAttr(1, StorageManager.SYSTEM_UID, BabuDBStorageManager.AUTO_REPL_FULL_ATTR_NAME, String
                .valueOf(replFull), update);
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
    public int getAutoReplFactor() {
        return replFactor;
    }
    
    @Override
    public boolean getAutoReplFull() {
        return replFull;
    }
    
}
