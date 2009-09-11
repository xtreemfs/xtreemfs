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

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.BabuDBException.ErrorCode;
import org.xtreemfs.babudb.log.DiskLogger.SyncMode;
import org.xtreemfs.babudb.lsmdb.BabuDBInsertGroup;
import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.babudb.lsmdb.DatabaseManager;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.include.common.config.BabuDBConfig;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.ac.FileAccessPolicy;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeChangeListener;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.metadata.ACLEntry;

public class BabuDBVolumeManager implements VolumeManager {
    
    private static final String               VERSION_DB_NAME = "V";
    
    private static final int                  VERSION_INDEX   = 0;
    
    private static final String               VERSION_KEY     = "v";
    
    /** the volume database */
    private BabuDB                            database;
    
    /** the database directory */
    private final String                      dbDir;
    
    /** the database log directory */
    private final String                      dbLogDir;
    
    /** maps the IDs of all locally known volumes to their managers */
    private final Map<String, StorageManager> volsById;
    
    /** maps the names of all locally known volumes to their managers */
    private final Map<String, StorageManager> volsByName;
    
    private final Collection<VolumeChangeListener> listeners;
    
    /** contains all snapshots */
    private final Set<String>                 snapshots;
    
    public BabuDBVolumeManager(MRCRequestDispatcher master) {
        dbDir = master.getConfig().getDbDir();
        dbLogDir = master.getConfig().getDbLogDir();
        volsById = new HashMap<String, StorageManager>();
        volsByName = new HashMap<String, StorageManager>();
        snapshots = new HashSet<String>();
        listeners = new LinkedList<VolumeChangeListener>();
    }
    
    @Override
    public void init() throws DatabaseException {
        
        try {
            
            // try to create a new database
            database = BabuDBFactory.createBabuDB(new BabuDBConfig(dbDir, dbLogDir, 2, 1024 * 1024 * 16,
                5 * 60, SyncMode.ASYNC, 0, 1000));
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
        try {
            Database volDB = database.getDatabaseManager().createDatabase(VERSION_DB_NAME, 3);
            
            // if the creation succeeds, set the version number to the current
            // MRC DB version
            byte[] verBytes = ByteBuffer.wrap(new byte[4])
                    .putInt((int) VersionManagement.getMrcDataVersion()).array();
            BabuDBInsertGroup ig = volDB.createInsertGroup();
            ig.addInsert(VERSION_INDEX, VERSION_KEY.getBytes(), verBytes);
            volDB.directInsert(ig);
            
        } catch (BabuDBException e) {
            
            if (e.getErrorCode() == ErrorCode.DB_EXISTS) {
                
                Database volDB = null;
                try {
                    volDB = database.getDatabaseManager().getDatabase(VERSION_DB_NAME);
                } catch (BabuDBException e1) {
                    throw new DatabaseException(e1);
                }
                
                // database already exists
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.db, this, "database loaded from '%s'",
                        dbDir);
                
                try {
                    
                    // retrieve the database version number
                    byte[] verBytes = volDB.directLookup(VERSION_INDEX, VERSION_KEY.getBytes());
                    int ver = ByteBuffer.wrap(verBytes).getInt();
                    
                    // check the database version number
                    if (ver != VersionManagement.getMrcDataVersion()) {
                        
                        String errMsg = "Wrong database version. Expected version = "
                            + VersionManagement.getMrcDataVersion() + ", version on disk = " + ver;
                        
                        Logging.logMessage(Logging.LEVEL_CRIT, this, errMsg);
                        if (VersionManagement.getMrcDataVersion() > ver)
                            Logging
                                    .logMessage(
                                        Logging.LEVEL_CRIT,
                                        this,
                                        "Please create an XML dump with the old MRC version and restore the dump with this MRC, or delete the database if the file system is no longer needed.");
                        
                        throw new DatabaseException(errMsg, ExceptionType.WRONG_DB_VERSION);
                    }
                    
                } catch (BabuDBException exc) {
                    Logging.logMessage(Logging.LEVEL_CRIT, this,
                        "The MRC database is either corrupted or outdated. The expected database version for this server is "
                            + VersionManagement.getMrcDataVersion());
                    throw new DatabaseException(exc);
                }
                
                try {
                    
                    // iterate over the list of databases in the db manager
                    for (Entry<String, Database> dbEntry : database.getDatabaseManager().getDatabases()
                            .entrySet()) {
                        
                        // ignore the volume database
                        if (dbEntry.getKey().equals("V"))
                            continue;
                        
                        BabuDBStorageManager sMan = new BabuDBStorageManager(database, dbEntry.getValue());
                        VolumeInfo vol = sMan.getVolumeInfo();
                        
                        volsById.put(vol.getId(), sMan);
                        volsByName.put(vol.getName(), sMan);
                        for (String snapName : sMan.getAllSnapshots())
                            registerSnapshot(vol.getId(), snapName);
                        
                    }
                    
                } catch (BabuDBException exc) {
                    throw new DatabaseException(exc);
                }
                
            } else
                Logging.logError(Logging.LEVEL_ERROR, this, e);
        }
    }
    
    @Override
    public void shutdown() {
        
        try {
            database.shutdown();
        } catch (BabuDBException exc) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.lifecycle, this,
                "could not shut down volume manager");
            Logging.logError(Logging.LEVEL_WARN, this, exc);
        }
    }
    
    @Override
    public VolumeInfo createVolume(FileAccessManager faMan, String volumeId, String volumeName,
        short fileAccessPolicyId, String ownerId, String owningGroupId, StripingPolicy defaultStripingPolicy,
        int initialAccessMode) throws UserException, DatabaseException {
        
        final DatabaseManager dbMan = database.getDatabaseManager();
        
        if (volumeName.indexOf('/') != -1 || volumeName.indexOf('\\') != -1)
            throw new UserException(ErrNo.EINVAL, "volume name must not contain '/' or '\\'");
        
        if (hasVolume(volumeName))
            throw new UserException(ErrNo.EEXIST, "volume ' " + volumeName + "' already exists locally");
        
        // make sure that no volume database with the given name exists
        try {
            dbMan.deleteDatabase(volumeId);
        } catch (BabuDBException exc) {
            // ignore
        }
        
        // instantiate the storage manager for the new volume
        BabuDBStorageManager sMan = new BabuDBStorageManager(database, volumeId);
        volsById.put(volumeId, sMan);
        volsByName.put(volumeName, sMan);
        
        // get the default permissions and ACL
        FileAccessPolicy policy = faMan.getFileAccessPolicy(fileAccessPolicyId);
        ACLEntry[] acl = policy.getDefaultRootACL(sMan);
        
        // initialize the storage manager for the new volume
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
        sMan.init(volumeName, fileAccessPolicyId, DEFAULT_OSD_POLICY, DEFAULT_REPL_POLICY,
            DEFAULT_AUTO_REPL_FACTOR, DEFAULT_AUTO_REPL_FULL, ownerId, owningGroupId, initialAccessMode, acl, defaultStripingPolicy,
            update);
        update.execute();
        
        for(VolumeChangeListener l: listeners)
            sMan.addVolumeChangeListener(l);
        
        return sMan.getVolumeInfo();
    }
    
    @Override
    public boolean hasVolume(String volumeName) throws DatabaseException {
        return volsByName.containsKey(volumeName);
    }
    
    @Override
    public boolean hasVolumeWithId(String volumeId) throws DatabaseException {
        return volsById.containsKey(volumeId);
    }
    
    @Override
    public void deleteVolume(String volumeId, DBAccessResultListener listener, Object context)
        throws DatabaseException, UserException {
        
        // check if the volume exists
        StorageManager sMan = getStorageManager(volumeId);
        
        // remove the volume from the maps
        volsById.remove(volumeId);
        volsByName.remove(sMan.getVolumeInfo().getName());
        
        // delete the volume's database
        sMan.deleteDatabase();
        
        // TODO: delete snapshots
    }
    
    @Override
    public StorageManager getStorageManager(String volumeId) throws UserException {
        
        StorageManager sMan = volsById.get(volumeId);
        if (sMan == null)
            throw new UserException(ErrNo.ENOENT, "volume '" + volumeId + "' not found on this MRC");
        
        return sMan;
    }
    
    @Override
    public StorageManager getStorageManagerByName(String volumeName) throws UserException {
        
        StorageManager sMan = volsByName.get(volumeName);
        if (sMan == null)
            throw new UserException(ErrNo.ENOENT, "volume '" + volumeName + "' not found on this MRC");
        
        return sMan;
    }
    
    @Override
    public void checkpointDB() throws DatabaseException {
        try {
            database.getCheckpointer().checkpoint();
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public String newVolumeId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public Collection<StorageManager> getStorageManagers() {
        return volsByName.values();
    }
    
    @Override
    public void addVolumeChangeListener(VolumeChangeListener listener) {
        this.listeners.add(listener);
        for(StorageManager sMan: volsById.values())
            sMan.addVolumeChangeListener(listener);
    }
    
    public void createSnapshot(String volumeId, String snapName, long parentId, String dirName,
        boolean recursive) throws UserException, DatabaseException {
        
        // create a snapshot
        volsById.get(volumeId).snapshot(snapName, parentId, dirName, recursive);
        
        // register the new snapshot
        registerSnapshot(volumeId, snapName);
    }
    
    private void registerSnapshot(String volumeId, String snapName) {
        snapshots.add(volumeId + "*" + snapName);
    }
    
}
