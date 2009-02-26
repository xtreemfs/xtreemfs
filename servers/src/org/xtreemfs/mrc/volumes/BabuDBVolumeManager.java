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

package org.xtreemfs.mrc.volumes;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.BabuDBInsertGroup;
import org.xtreemfs.babudb.log.DiskLogger.SyncMode;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.ac.FileAccessPolicy;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.babudb.BabuDBRequestListenerWrapper;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageManager;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.volumes.metadata.BufferBackedVolumeInfo;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

public class BabuDBVolumeManager implements VolumeManager {
    
    private static final String               VOLUME_DB_NAME = "V";
    
    private static final int                  VOL_INDEX      = 0;
    
    private static final int                  VOL_NAME_INDEX = 1;
    
    /** the volume database */
    private BabuDB                            database;
    
    /** the database directory */
    private final String                      dbDir;
    
    /** the database log directory */
    private final String                      dbLogDir;
    
    /** maps the IDs of all locally known volumes to their storage managers */
    private final Map<String, StorageManager> mngrMap;
    
    /** maps the IDs of all locally known volumes to their info objects */
    private final Map<String, VolumeInfo>     volIdMap;
    
    /** maps the names of all locally known volumes to their info objects */
    private final Map<String, VolumeInfo>     volNameMap;
    
    /**
     * contains all listeners that are notified when volumes are changed
     */
    private final List<VolumeChangeListener>  vcListeners;
    
    public BabuDBVolumeManager(MRCRequestDispatcher master) {
        
        this.vcListeners = new LinkedList<VolumeChangeListener>();
        
        dbDir = master.getConfig().getDbDir();
        dbLogDir = master.getConfig().getDbDir(); // TODO: introduce separate
        // parameter
        mngrMap = new HashMap<String, StorageManager>();
        volIdMap = new HashMap<String, VolumeInfo>();
        volNameMap = new HashMap<String, VolumeInfo>();
    }
    
    public void init() throws DatabaseException {
        
        try {
            database = BabuDBFactory.getBabuDB(dbDir, dbLogDir, 2, 1024 * 1024 * 16, 5 * 60,
                SyncMode.ASYNC, 0, 1000);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
        try {
            database.createDatabase(VOLUME_DB_NAME, 2);
        } catch (BabuDBException e) {
            // database already exists
            Logging.logMessage(Logging.LEVEL_TRACE, this, "database loaded from '" + dbDir + "'");
            
            try {
                
                // retrieve the list of volumes from the database
                Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(VOLUME_DB_NAME,
                    VOL_INDEX, new byte[0]);
                List<VolumeInfo> list = new LinkedList<VolumeInfo>();
                while (it.hasNext())
                    list.add(new BufferBackedVolumeInfo(it.next().getValue()));
                
                for (VolumeInfo vol : list) {
                    mngrMap.put(vol.getId(), new BabuDBStorageManager(database, vol.getName(), vol
                            .getId()));
                    volIdMap.put(vol.getId(), vol);
                    volNameMap.put(vol.getName(), vol);
                }
                
            } catch (BabuDBException exc) {
                throw new DatabaseException(exc);
            }
        }
    }
    
    public void shutdown() {
        database.shutdown();
    }
    
    public VolumeInfo createVolume(FileAccessManager faMan, String volumeId, String volumeName,
        short fileAccessPolicyId, short osdPolicyId, String osdPolicyArgs, String ownerId,
        String owningGroupId, Map<String, Object> defaultStripingPolicy) throws UserException,
        DatabaseException {
        
        if (volumeName.indexOf('/') != -1 || volumeName.indexOf('\\') != -1)
            throw new UserException(ErrNo.EINVAL, "volume name must not contain '/' or '\\'");
        
        if (hasVolume(volumeName))
            throw new UserException(ErrNo.EEXIST, "volume ' " + volumeName
                + "' already exists locally");
        
        // create the volume
        BufferBackedVolumeInfo volume = new BufferBackedVolumeInfo(volumeId, volumeName,
            fileAccessPolicyId, osdPolicyId, osdPolicyArgs);
        
        // make sure that no volume database with the given name exists
        try {
            database.deleteDatabase(volumeId, true);
        } catch (BabuDBException exc) {
            // ignore
        }
        
        // create the volume database
        try {
            database.createDatabase(volumeId, 6);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
        BabuDBStorageManager sMan = new BabuDBStorageManager(database, volumeName, volumeId);
        mngrMap.put(volumeId, sMan);
        
        // get the default permissions and ACL
        FileAccessPolicy policy = faMan.getFileAccessPolicy(fileAccessPolicyId);
        int perms = policy.getDefaultRootRights();
        ACLEntry[] acl = policy.getDefaultRootACL(sMan);
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
        sMan.init(ownerId, owningGroupId, perms, acl, defaultStripingPolicy, update);
        update.execute();
        
        try {
            BabuDBInsertGroup ig = database.createInsertGroup(VOLUME_DB_NAME);
            ig.addInsert(VOL_INDEX, volumeId.getBytes(), volume.getBuffer());
            ig.addInsert(VOL_NAME_INDEX, volumeName.getBytes(), volumeId.getBytes());
            database.directInsert(ig);
            // database.asyncInsert(ig, new
            // BabuDBRequestListenerWrapper(listener), context);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
        volIdMap.put(volumeId, volume);
        volNameMap.put(volumeName, volume);
        
        notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, volume);
        
        return volume;
        
    }
    
    public boolean hasVolume(String volumeName) throws DatabaseException {
        
        return volNameMap.containsKey(volumeName);
        
        // try {
        // byte[] result = database.syncLookup(VOLUME_DB_NAME, VOL_NAME_INDEX,
        // volumeName
        // .getBytes());
        // return result != null;
        // } catch (BabuDBException exc) {
        // throw new DatabaseException(exc);
        // }
    }
    
    public boolean hasVolumeWithId(String volumeId) throws DatabaseException {
        
        return volIdMap.containsKey(volumeId);
        
        // try {
        // byte[] result = database.syncLookup(VOLUME_DB_NAME, VOL_INDEX,
        // volumeId.getBytes());
        // return result != null;
        // } catch (BabuDBException exc) {
        // throw new DatabaseException(exc);
        // }
    }
    
    public VolumeInfo getVolumeByName(String volumeName) throws DatabaseException, UserException {
        
        VolumeInfo info = volNameMap.get(volumeName);
        if (info == null)
            throw new UserException(ErrNo.ENOENT, "volume '" + volumeName
                + "' not found on this MRC");
        
        return info;
        
        // try {
        //            
        // byte[] volId = database.syncLookup(VOLUME_DB_NAME, VOL_NAME_INDEX,
        // volumeName
        // .getBytes());
        // if (volId == null)
        // throw new UserException(ErrNo.ENOENT, "volume '" + volumeName
        // + "' not found on this MRC");
        //            
        // byte[] volumeData = database.syncLookup(VOLUME_DB_NAME, VOL_INDEX,
        // volId);
        //            
        // return new BufferBackedVolumeInfo(volumeData);
        //            
        // } catch (BabuDBException exc) {
        // throw new DatabaseException(exc);
        // }
    }
    
    public VolumeInfo getVolumeById(String volumeId) throws DatabaseException, UserException {
        
        VolumeInfo volume = volIdMap.get(volumeId);
        if (volume == null)
            throw new UserException(ErrNo.ENOENT, "volume with id " + volumeId
                + " not found on this MRC");
        
        return volume;
        
        // try {
        //            
        // byte[] volumeData = database.syncLookup(VOLUME_DB_NAME, VOL_INDEX,
        // volumeId.getBytes());
        // if (volumeData == null)
        // throw new UserException(ErrNo.ENOENT, "volume with id " + volumeId
        // + " not found on this MRC");
        // return new BufferBackedVolumeInfo(volumeData);
        //            
        // } catch (BabuDBException exc) {
        // throw new DatabaseException(exc);
        // }
    }
    
    public Collection<VolumeInfo> getVolumes() throws DatabaseException {
        
        return volNameMap.values();
        
        // try {
        //            
        // Iterator<Entry<byte[], byte[]>> it =
        // database.syncPrefixLookup(VOLUME_DB_NAME,
        // VOL_INDEX, new byte[0]);
        // List<VolumeInfo> list = new LinkedList<VolumeInfo>();
        // while (it.hasNext())
        // list.add(new BufferBackedVolumeInfo(it.next().getValue()));
        //            
        // return list;
        //            
        // } catch (BabuDBException exc) {
        // throw new DatabaseException(exc);
        // }
    }
    
    public void updateVolume(VolumeInfo volume) throws DatabaseException {
        
        assert (volume instanceof BufferBackedVolumeInfo);
        
        try {
            BabuDBInsertGroup ig = database.createInsertGroup(VOLUME_DB_NAME);
            ig.addInsert(VOL_INDEX, volume.getId().getBytes(), ((BufferBackedVolumeInfo) volume)
                    .getBuffer());
            database.syncInsert(ig);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
        volIdMap.put(volume.getId(), volume);
        volNameMap.put(volume.getName(), volume);
        
        notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, volume);
    }
    
    public void deleteVolume(String volumeId, DBAccessResultListener listener, Object context)
        throws DatabaseException, UserException {
        
        try {
            
            VolumeInfo volume = getVolumeById(volumeId);
            mngrMap.remove(volumeId);
            volIdMap.remove(volumeId);
            volNameMap.remove(volume.getName());
            
            BabuDBInsertGroup ig = database.createInsertGroup(VOLUME_DB_NAME);
            ig.addDelete(VOL_INDEX, volumeId.getBytes());
            ig.addDelete(VOL_NAME_INDEX, volume.getName().getBytes());
            
            database.asyncInsert(ig, new BabuDBRequestListenerWrapper(listener), context);
            
            notifyVolumeChangeListeners(VolumeChangeListener.MOD_DELETED, volume);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public StorageManager getStorageManager(String volumeId) {
        return mngrMap.get(volumeId);
    }
    
    // /**
    // *
    // * @param volumeID
    // * @param fileID
    // * @return true, if the file with the given ID exists, false otherwise.
    // * @throws UserException
    // * - if volume does not exist
    // * @throws BackendException
    // * - if a backendError occur
    // */
    // public boolean exists(String volumeID, String fileID) throws
    // UserException, BackendException {
    //        
    // // check the volume - if not available throw UserException
    // VolumeInfo volume = volumesById.get(volumeID);
    // if (volume == null)
    // throw new UserException("could not find volume '" + volumeID);
    //        
    // // get the sliceID - if not available return false.
    // SliceID sliceId = null;
    //        
    // sliceId = partMan.getSlice(volumeID, Long.valueOf(fileID));
    //        
    // // check sliceID for info objects - if not available return false.
    // if (slicesById.get(sliceId) == null)
    // return false;
    //        
    // // get the responsible StorageManager - if not available return
    // false.
    // StorageManager mngr = mngrMap.get(sliceId);
    // if (mngr != null)
    // return mngr.exists(fileID);
    // else
    // return getSliceDB(sliceId, 'r').exists(fileID);
    // return false;
    // }
    
    public void addVolumeChangeListener(VolumeChangeListener listener) throws IOException,
        DatabaseException {
        
        vcListeners.add(listener);
        
        for (VolumeInfo vol : getVolumes())
            notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, vol);
    }
    
    @Override
    public String newVolumeId() {
        return UUID.randomUUID().toString();
    }
    
    private void notifyVolumeChangeListeners(int mod, VolumeInfo vol) {
        for (VolumeChangeListener listener : vcListeners)
            listener.volumeChanged(mod, vol);
    }
    
}
