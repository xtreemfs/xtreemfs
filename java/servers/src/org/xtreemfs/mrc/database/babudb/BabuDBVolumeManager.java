/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database.babudb;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.api.BabuDB;
import org.xtreemfs.babudb.api.DatabaseManager;
import org.xtreemfs.babudb.api.SnapshotManager;
import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.babudb.api.exception.BabuDBException.ErrorCode;
import org.xtreemfs.babudb.api.transaction.Operation;
import org.xtreemfs.babudb.api.transaction.Transaction;
import org.xtreemfs.babudb.api.transaction.TransactionListener;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeChangeListener;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;

public class BabuDBVolumeManager implements VolumeManager {
    
    private static final String                    VERSION_DB_NAME       = "V";
    
    private static final String                    SNAP_VERSIONS_DB_NAME = "snapVers";
    
    private static final int                       VERSION_INDEX         = 0;
    
    private static final String                    VERSION_KEY           = "v";
    
    /** the volume database */
    private BabuDB                                 database;
    
    private Database                               snapVersionDB;
    
    private AtomicBoolean                          initialized;
    
    /** maps the IDs of all locally known volumes to their managers */
    private final Map<String, StorageManager>      volsById;
    
    /** maps the names of all locally known volumes to their managers */
    private final Map<String, StorageManager>      volsByName;
    
    private final Collection<VolumeChangeListener> listeners;
    
    private final BabuDBConfig                     config;
    
    private final AtomicBoolean                    waitLock;
    
    public BabuDBVolumeManager(MRCRequestDispatcher master, BabuDBConfig dbconfig) {
        initialized = new AtomicBoolean(false);
        volsById = Collections.synchronizedMap(new HashMap<String, StorageManager>());
        volsByName = Collections.synchronizedMap(new HashMap<String, StorageManager>());
        listeners = new LinkedList<VolumeChangeListener>();
        config = dbconfig;
        waitLock = new AtomicBoolean(false);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.mrc.database.VolumeManager#init()
     */
    @Override
    public void init() throws DatabaseException {
        try {
            
            // try to create a new database
            database = BabuDBFactory.createBabuDB(config);
            
            database.getDatabaseManager().addTransactionListener(new TransactionListener() {
                public void transactionPerformed(Transaction txn) {
                    
                    Operation op = txn.getOperations().get(0);
                    
                    if ((op.getType() == Operation.TYPE_CREATE_DB || op.getType() == Operation.TYPE_DELETE_DB)
                            && !VERSION_DB_NAME.equals(op.getDatabaseName())
                            && !SNAP_VERSIONS_DB_NAME.equals(op.getDatabaseName())) {
                        
                        // register/deregister the volume
                        try {
                            if (op.getType() == Operation.TYPE_CREATE_DB) {
                                
                                registerVolume(op.getDatabaseName());
                                
                                synchronized (waitLock) {
                                    waitLock.set(true);
                                    waitLock.notify();
                                }
                                
                            } else
                                deregisterVolume(op.getDatabaseName());
                        } catch (Exception exc) {
                            Logging.logError(Logging.LEVEL_ERROR, this, exc);
                        }
                    }
                }
            });
            
            initDB(database.getDatabaseManager(), database.getSnapshotManager());
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    @Override
    public void shutdown() {
        
        try {
            database.shutdown();
        } catch (BabuDBException exc) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.lifecycle, this, "could not shut down volume manager");
            Logging.logError(Logging.LEVEL_WARN, this, exc);
        }
    }
    
    @Override
    public void createVolume(FileAccessManager faMan, String volumeId, String volumeName, short fileAccessPolicyId,
            String ownerId, String owningGroupId, StripingPolicy defaultStripingPolicy, int initialAccessMode,
            long volumeQuota, int priority, List<KeyValuePair> attrs) throws UserException, DatabaseException {
        
        waitLock.set(false);
        
        if (volumeName.indexOf(SNAPSHOT_SEPARATOR) != -1 || volumeName.indexOf('/') != -1
                || volumeName.indexOf('\\') != -1)
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "volume name must not contain '"
                    + SNAPSHOT_SEPARATOR + "', '/' or '\\'");
        
        if (hasVolume(volumeName))
            throw new UserException(POSIXErrno.POSIX_ERROR_EEXIST, "volume ' " + volumeName
                    + "' already exists locally");
        
        // get the default permissions and ACL for the new volume
        ACLEntry[] acl = faMan.getFileAccessPolicy(fileAccessPolicyId).getDefaultRootACL();
        
        // create the new volume (local registration will be initiated through
        // the transaction listener)
        new BabuDBStorageManager(database, volumeId, volumeName, fileAccessPolicyId, DEFAULT_OSD_POLICY,
                DEFAULT_REPL_POLICY, ownerId, owningGroupId, initialAccessMode, acl, defaultStripingPolicy,
                DEFAULT_ALLOW_SNAPS, volumeQuota, priority, KeyValuePairs.toMap(attrs));
        
        // wait for the notification from the transaction listener before
        // continuing
        synchronized (waitLock) {
            while (!waitLock.get())
                try {
                    waitLock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
        }
        
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
    public void deleteVolume(String volumeId, DBAccessResultListener<Object> listener, Object context)
            throws DatabaseException, UserException {
        
        // check if the volume exists
        StorageManager sMan = getStorageManager(volumeId);
        
        // remove the volume from the maps
        volsById.remove(volumeId);
        volsByName.remove(sMan.getVolumeInfo().getName());
        
        // delete the volume's database
        sMan.deleteDatabase();
    }
    
    @Override
    public StorageManager getStorageManager(String volumeId) throws UserException {
        
        StorageManager sMan = volsById.get(volumeId);
        if (sMan == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "volume '" + volumeId + "' not found on this MRC");
        
        return sMan;
    }
    
    @Override
    public StorageManager getStorageManagerByName(String volumeName) throws UserException {
        
        StorageManager sMan = volsByName.get(volumeName);
        if (sMan == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "volume '" + volumeName + "' not found on this MRC");
        
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
        
        if (!initialized.get())
            return null;
        
        final Collection<StorageManager> result = new HashSet<StorageManager>();
        final Collection<StorageManager> sMans = volsById.values();
        synchronized (volsById) {
            for (StorageManager sMan : sMans) {
                result.add(sMan);
            }
        }
        return result;
    }
    
    @Override
    public void addVolumeChangeListener(VolumeChangeListener listener) {
        this.listeners.add(listener);
        for (StorageManager sMan : volsById.values())
            sMan.addVolumeChangeListener(listener);
    }
    
    @Override
    public void createSnapshot(String volumeId, String snapName, long parentId, FileMetadata dir, boolean recursive)
            throws UserException, DatabaseException {
        
        try {
            // check if the volume exists
            StorageManager sMan = getStorageManager(volumeId);
            
            if (!snapName.equals(".dump") && !sMan.getVolumeInfo().isSnapshotsEnabled())
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                        "snapshot operations are not allowed on this volume");
            
            // get the time for the snapshot; this will be needed to attach the
            // snapshot to file content snapshots
            long currentTime = TimeSync.getGlobalTime();
            byte[] currentTimeAndParentAsBytes = ByteBuffer.wrap(new byte[2 * Long.SIZE / 8]).putLong(currentTime)
                    .putLong(parentId).array();
            
            // if no snapshot name was passed, use the current time as the name
            if ("".equals(snapName))
                snapName = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date(currentTime)) + "";
            
            // determine the unique identifier for the snapshot
            String snapVolName = sMan.getVolumeInfo().getName() + SNAPSHOT_SEPARATOR + snapName;
            
            // update the snapshot version table in order to persistently store
            // the snapshot time stamp
            try {
                snapVersionDB.singleInsert(0, snapVolName.getBytes(), currentTimeAndParentAsBytes, null).get();
            } catch (BabuDBException exc2) {
                throw new DatabaseException(exc2);
            }
            
            // create the snapshot
            sMan.createSnapshot(snapName, parentId, dir.getFileName(), recursive);
            volsByName.put(snapVolName, new BabuDBSnapshotStorageManager(database.getSnapshotManager(), sMan
                    .getVolumeInfo().getName(), volumeId, snapName, currentTime, parentId));
            
        } catch (DatabaseException exc) {
            
            if (((BabuDBException) exc.getCause()).getErrorCode() == ErrorCode.SNAP_EXISTS)
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, exc.getMessage());
            else
                throw exc;
        }
    }
    
    @Override
    public void deleteSnapshot(String volumeId, FileMetadata dir, String snapName) throws UserException,
            DatabaseException {
        
        try {
            
            // check if the volume exists
            StorageManager sMan = getStorageManager(volumeId);
            
            // determine the unique identifier for the snapshot
            String snapVolName = sMan.getVolumeInfo().getName() + SNAPSHOT_SEPARATOR + snapName;
            
            // delete the snapshot
            volsByName.remove(snapVolName);
            sMan.deleteSnapshot(snapName);
            
            // update the snapshot version table
            try {
                snapVersionDB.singleInsert(0, snapVolName.getBytes(), null, null).get();
            } catch (BabuDBException exc) {
                throw new DatabaseException(exc);
            }
            
        } catch (DatabaseException exc) {
            
            if (((BabuDBException) exc.getCause()).getErrorCode() == ErrorCode.NO_SUCH_SNAPSHOT)
                throw new UserException(POSIXErrno.POSIX_ERROR_ENODEV, exc.getMessage());
            else
                throw exc;
        }
    }
    
    @Override
    public Collection<Long> getSnapTimestamps(String volName) throws UserException, DatabaseException {
        
        try {
            
            Collection<Long> result = new LinkedList<Long>();
            
            byte[] prefix = (volName + SNAPSHOT_SEPARATOR).getBytes();
            
            ResultSet<byte[], byte[]> it = null;
            try {
                it = snapVersionDB.prefixLookup(0, prefix, null).get();
                
                while (it.hasNext()) {
                    byte[] bytes = it.next().getValue();
                    long ts = ByteBuffer.wrap(bytes).getLong();
                    result.add(ts);
                }
                
            } catch (BabuDBException exc) {
                throw new DatabaseException(exc);
            } finally {
                if (it != null)
                    it.free();
            }
            
            return result;
            
        } catch (DatabaseException exc) {
            
            if (((BabuDBException) exc.getCause()).getErrorCode() == ErrorCode.SNAP_EXISTS)
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, exc.getMessage());
            else
                throw exc;
        }
    }
    
    @Override
    public String getDBVersion() {
        return BabuDBFactory.BABUDB_VERSION;
    }
    
    @Override
    public Map<String, Object> getDBStatus() {
        return database == null ? null : database.getRuntimeState();
    }
    
    private void initDB(DatabaseManager dbMan, SnapshotManager snapMan) throws DatabaseException {
        
        // check if the snapshot version DB exists; if not, make sure that it is
        // created
        if (snapVersionDB == null)
            try {
                snapVersionDB = dbMan.createDatabase(SNAP_VERSIONS_DB_NAME, 1);
            } catch (BabuDBException exc) {
                
                if (exc.getErrorCode() == ErrorCode.DB_EXISTS)
                    try {
                        snapVersionDB = dbMan.getDatabase(SNAP_VERSIONS_DB_NAME);
                    } catch (BabuDBException exc2) {
                        throw new DatabaseException(exc2);
                    }
                
                // if a replication failure occurred, wait and retry
                // else if (exc.getErrorCode() == ErrorCode.REPLICATION_FAILURE)
                // {
                //
                // Logging.logMessage(
                // Logging.LEVEL_INFO,
                // Category.storage,
                // this,
                // "could not initialize database because of a replication failure: %s, will retry after %d ms",
                // exc.toString(), RETRY_PERIOD);
                //
                // try {
                // Thread.sleep(RETRY_PERIOD);
                // initDB(dbMan, snapMan);
                // } catch (InterruptedException e1) {
                // }
                //
                // }
                
                else
                    throw new DatabaseException(exc);
            }
        
        assert (snapVersionDB != null);
        
        try {
            
            Transaction txn = dbMan.createTransaction();
            txn.createDatabase(VERSION_DB_NAME, 3);
            
            // if the creation succeeds, set the version number to the current
            // MRC DB version
            byte[] verBytes = ByteBuffer.wrap(new byte[4]).putInt((int) VersionManagement.getMrcDataVersion()).array();
            txn.insertRecord(VERSION_DB_NAME, VERSION_INDEX, VERSION_KEY.getBytes(), verBytes);
            dbMan.executeTransaction(txn);
            
        } catch (BabuDBException e) {
            
            if (e.getErrorCode() == ErrorCode.DB_EXISTS) {
                
                Database verDB = null;
                try {
                    verDB = dbMan.getDatabase(VERSION_DB_NAME);
                } catch (BabuDBException e1) {
                    throw new DatabaseException(e1);
                }
                
                // database already exists
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "database loaded from '%s'",
                            config.getBaseDir());
                
                try {
                    
                    // retrieve the database version number
                    byte[] verBytes = verDB.lookup(VERSION_INDEX, VERSION_KEY.getBytes(), null).get();
                    int ver = ByteBuffer.wrap(verBytes).getInt();
                    
                    // check the database version number
                    if (ver != VersionManagement.getMrcDataVersion()) {
                        
                        String errMsg = "Wrong database version. Expected version = "
                                + VersionManagement.getMrcDataVersion() + ", version on disk = " + ver;
                        
                        Logging.logMessage(Logging.LEVEL_CRIT, this, errMsg);
                        if (VersionManagement.getMrcDataVersion() > ver)
                            Logging.logMessage(
                                    Logging.LEVEL_CRIT,
                                    this,
                                    "Please create an XML dump with the old MRC version and restore the dump with this MRC, or delete the database if the file system is no longer needed.");
                        
                        throw new DatabaseException(errMsg, ExceptionType.WRONG_DB_VERSION);
                    }
                    
                } catch (Exception exc) {
                    Logging.logMessage(Logging.LEVEL_CRIT, this,
                            "The MRC database is either corrupted or outdated. The expected database version for this server is "
                                    + VersionManagement.getMrcDataVersion());
                    throw new DatabaseException(exc);
                }
                
                // initialize all volumes
                initVolumes(dbMan, snapMan);
                
            }
            
            else
                throw new DatabaseException(e);
            
        } finally {
            initialized.set(true);
        }
    }
    
    private void initVolumes(DatabaseManager dbMan, SnapshotManager snapMan) throws DatabaseException {
        
        // iterate over the list of databases in the storage manager
        for (Entry<String, Database> dbEntry : dbMan.getDatabases().entrySet()) {
            
            // ignore the volume database
            if (dbEntry.getKey().equals(VERSION_DB_NAME) || dbEntry.getKey().equals(SNAP_VERSIONS_DB_NAME))
                continue;
            
            BabuDBStorageManager sMan = new BabuDBStorageManager(dbMan, snapMan, dbEntry.getValue());
            VolumeInfo vol = sMan.getVolumeInfo();
            
            volsById.put(vol.getId(), sMan);
            volsByName.put(vol.getName(), sMan);
            
            // initialize all snapshots
            for (String snapName : sMan.getAllSnapshots()) {
                
                // ignore snapshots that have been created for MRC
                // dumps
                if (snapName.equals(".dump"))
                    continue;
                
                String snapVolName = vol.getName() + SNAPSHOT_SEPARATOR + snapName;
                
                // retrieve the version time stamp from the snap
                // version database
                try {
                    byte[] timeStampAndParentBytes = snapVersionDB.lookup(0, snapVolName.getBytes(), null).get();
                    if (timeStampAndParentBytes == null)
                        Logging.logMessage(Logging.LEVEL_WARN, Category.storage, this,
                                "no version mapping exists for snapshot %s; file contents may be corrupted",
                                snapVolName);
                    long snapTime = timeStampAndParentBytes == null ? 0 : ByteBuffer.wrap(timeStampAndParentBytes)
                            .getLong();
                    long parentId = timeStampAndParentBytes == null ? 0 : ByteBuffer.wrap(timeStampAndParentBytes)
                            .getLong(8);
                    
                    volsByName.put(snapVolName, new BabuDBSnapshotStorageManager(snapMan, vol.getName(), vol.getId(),
                            snapName, snapTime, parentId));
                    
                } catch (BabuDBException exc) {
                    throw new DatabaseException(exc);
                }
            }
            
            for (VolumeChangeListener l : listeners)
                sMan.addVolumeChangeListener(l);
            
        }
    }
    
    private void registerVolume(String volumeId) throws DatabaseException {
        
        DatabaseManager dbMan = database.getDatabaseManager();
        
        try {
            
            BabuDBStorageManager sMan = new BabuDBStorageManager(dbMan, database.getSnapshotManager(),
                    dbMan.getDatabase(volumeId));
            
            VolumeInfo vol = sMan.getVolumeInfo();
            
            volsById.put(vol.getId(), sMan);
            volsByName.put(vol.getName(), sMan);
            
            for (VolumeChangeListener l : listeners)
                sMan.addVolumeChangeListener(l);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
    }
    
    private void deregisterVolume(String volumeId) {
        
        // deregister the volumes if necessary
        StorageManager sMan = volsById.remove(volumeId);
        if (sMan != null)
            volsByName.remove(sMan.getVolumeInfo().getName());
    }
    
}
