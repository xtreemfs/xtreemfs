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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.slices;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.VolumeChangeListener;
import org.xtreemfs.mrc.brain.storage.BackendException;
import org.xtreemfs.mrc.brain.storage.SliceID;
import org.xtreemfs.mrc.brain.storage.StorageManager;
import org.xtreemfs.mrc.brain.storage.StorageManager.RestoreState;

/**
 * Manages all locally known slices of all volumes.
 * 
 * <p>
 * Each slice represents a certain part of all metadata stored in a volume. For
 * each slice, a separate database is maintained, which can be accessed by its
 * own <code>StorageManager</code> instance. If metadata partitioning is used,
 * several slices may constitute a single volume.
 * 
 * @author bjko, stender
 */
public class SliceManager {
    
    /** filename in which the settings are stored */
    private static final String                FILENAME         = "slices.dat";
    
    /** for crash recovery first a the tmp is written */
    private static final String                TEMP_FILENAME    = "slices.tmp";
    
    private static final String                CP_LOCK_FILENAME = ".lock";
    
    /** maps all locally known slice IDs to the corresponding slice info objects */
    private Map<SliceID, SliceInfo>            slicesById;
    
    /**
     * maps the names of all locally known volumes to the corresponding volume
     * info objects
     */
    private Map<String, VolumeInfo>            volumesByName;
    
    /**
     * maps the IDs of all locally known volumes to the corresponding volume
     * info objects
     */
    private Map<String, VolumeInfo>            volumesById;
    
    /**
     * contains all listeners that are notified when slices are added, removed
     * or changed
     */
    private final List<SliceEventListener>     listeners;
    
    /** the MRC configuration */
    private final MRCConfig                    config;
    
    /** the database directory */
    private final String                       dbDir;
    
    /**
     * maps the IDs of all locally known slices to their storage managers
     */
    private final Map<SliceID, StorageManager> mngrMap;
    
    /**
     * the partitioning manager
     */
    private final PartitioningManager          partMan;
    
    /**
     * contains all listeners that are notified when volumes are changed
     */
    private final List<VolumeChangeListener>   vcListeners;
    
    /**
     * Creates a new instance of SliceManager
     */
    public SliceManager(MRCConfig config) {
        
        this.config = config;
        this.mngrMap = new HashMap<SliceID, StorageManager>();
        this.listeners = new LinkedList<SliceEventListener>();
        this.partMan = new PartitioningManager(this);
        this.vcListeners = new LinkedList<VolumeChangeListener>();
        
        if (!config.getDbDir().endsWith("/")) {
            dbDir = config.getDbDir() + "/";
        } else {
            dbDir = config.getDbDir();
        }
    }
    
    /**
     * Initializes the slice manager. This causes all meta information about
     * volumes and slices to be loaded from disk.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void init() throws IOException, ClassNotFoundException {
        
        // check whether a local database exists; if so, load all metadata
        // about slices and volumes
        File status = new File(dbDir + FILENAME);
        if (status.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(status));
            volumesByName = (HashMap) ois.readObject();
            volumesById = (HashMap) ois.readObject();
            ois.close();
        } else {
            volumesByName = new HashMap<String, VolumeInfo>();
            volumesById = new HashMap<String, VolumeInfo>();
        }
        listeners.clear();
        mngrMap.clear();
        
        // create the 'slices by ID' mapping on-the-fly
        slicesById = new HashMap<SliceID, SliceInfo>();
        for (VolumeInfo vol : volumesById.values())
            for (SliceInfo slice : vol.getSlices())
                slicesById.put(slice.sliceID, slice);
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Slices on this server:");
        for (SliceInfo info : slicesById.values()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "\t" + info.sliceID + " mechanism="
                + info.getReplicationMechanism().getClass().getSimpleName());
        }
    }
    
    /**
     * Creates a new volume.
     * 
     * @param volumeId
     *            the volume ID
     * @param volumeName
     *            the volume name
     * @param ownerId
     *            the owner of the volume
     * @param groupId
     *            the owning group of the volume
     * @param fileAccessPolicyId
     *            the access policy
     * @param osdPolicyId
     *            the OSD selection policy
     * @param partitioningPolicyId
     *            the metadata partitioning policy
     * @param registerAtDS
     *            a flag indicating whether the volume will be registered at the
     *            directory service
     * @param createSlices
     *            a flag indicating whether the set of initial slices needs to
     *            be created
     * @return a volume info object
     * @throws UserException
     * @throws IOException
     * @throws BackendException
     */
    public VolumeInfo createVolume(String volumeId, String volumeName, long fileAccessPolicyId,
        long osdPolicyId, String osdPolicyArgs, long partitioningPolicyId, boolean registerAtDS,
        boolean createSlices) throws UserException, IOException, BackendException {
        
        if (volumeName.indexOf('/') != -1 || volumeName.indexOf('\\') != -1)
            throw new UserException(ErrNo.EINVAL, "volume name must not contain '/' or '\\'");
        
        if (volumesByName.containsKey(volumeName))
            throw new UserException(ErrNo.EEXIST, "volume ' " + volumeName
                + "' already exists locally");
        
        // create the volume
        VolumeInfo volume = new VolumeInfo(volumeId, volumeName, fileAccessPolicyId, osdPolicyId,
            partitioningPolicyId, registerAtDS);
        volume.setOsdPolicyArgs(osdPolicyArgs);
        
        volumesByName.put(volumeName, volume);
        volumesById.put(volumeId, volume);
        
        // create the initial slices
        if (createSlices) {
            for (SliceInfo slice : partMan.getInitialSlices(volumeId)) {
                
                volume.setSlice(slice.sliceID, slice);
                slicesById.put(slice.sliceID, slice);
                
                for (SliceEventListener l : listeners)
                    l.event(SliceEventListener.EventType.SLICE_ADDED, slice);
            }
        }
        
        notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, volume);
        
        return volume;
    }
    
    /**
     * Creates a new slice.
     * 
     * @param slice
     *            slice metadata
     * @throws IOException
     * @throws BackendException
     */
    public void createSlice(SliceInfo slice, boolean sync) throws IOException, BackendException {
        
        assert (slice.sliceID != null);
        
        // add the slice to the 'slicesbyVolume' map for a faster retrieval
        VolumeInfo volume = volumesById.get(slice.sliceID.getVolumeId());
        if (volume == null)
            throw new BackendException("could not find local volume for slice '" + slice.sliceID
                + "'");
        
        slicesById.put(slice.sliceID, slice);
        volume.setSlice(slice.sliceID, slice);
        
        if (sync)
            syncVolumeAndSliceMetadata();
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "created new slice " + slice.sliceID);
        
        // notify listeners
        for (SliceEventListener l : listeners)
            l.event(SliceEventListener.EventType.SLICE_ADDED, slice);
        
    }
    
    /**
     * Deletes a slice.
     * 
     * @param id
     *            the slice ID
     * @throws IOException
     */
    public void deleteSlice(SliceID id) throws IOException {
        
        SliceInfo info = slicesById.get(id);
        if (info == null)
            return;
        
        // remove the slice from the map
        slicesById.remove(id);
        
        // remove the slice from the volume
        VolumeInfo volume = volumesById.get(info.sliceID.getVolumeId());
        volume.setSlice(id, null);
        
        // dispose of the slice database
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "deleted slice " + id);
        
        // notify listeners
        for (SliceEventListener l : listeners)
            l.event(SliceEventListener.EventType.SLICE_REMOVED, info);
    }
    
    /**
     * Modifies a slice.
     * 
     * @param info
     *            slice metadata
     * @throws IOException
     */
    public void modifySlice(SliceInfo info) throws IOException {
        // slices.put(info.sliceID,info);
        
        // notify listeners
        for (SliceEventListener l : listeners) {
            l.event(SliceEventListener.EventType.SLICE_MODIFIED, info);
        }
    }
    
    /**
     * Returns the metadata of a slice.
     * 
     * @param id
     *            the slice ID
     * @return
     */
    public synchronized SliceInfo getSliceInfo(SliceID id) {
        SliceInfo info = slicesById.get(id);
        
        if (info == null) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "no info for slice " + id);
        }
        
        return info;
    }
    
    /**
     * Registers a new listener for slice events.
     * 
     * @param l
     *            the listener
     */
    public void registerListener(SliceEventListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }
    
    /**
     * Unregisters a listener for slice events.
     * 
     * @param l
     *            the listener
     */
    public void unregisterListener(SliceEventListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }
    
    /**
     * Persistently stores all slice and volume metadata.
     * 
     * @throws IOException
     */
    private void syncVolumeAndSliceMetadata() throws IOException {
        
        File dbDirFile = new File(dbDir);
        if (!dbDirFile.exists())
            dbDirFile.mkdirs();
        
        /**
         * first the tmp file is written. If we crash while overwriting the TMP
         * file we still have the old .DAT file. If the tmp was written
         * successfully we can start overwriting the .DAT file. If we crash
         * then, we still have a working copy in TMP. This still requires manual
         * intervention but no data is lost.
         */
        
        FileOutputStream fos = new FileOutputStream(dbDir + TEMP_FILENAME);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(volumesByName);
        oos.writeObject(volumesById);
        oos.writeObject(slicesById);
        oos.flush();
        fos.getFD().sync();
        oos.close();
        
        fos = new FileOutputStream(dbDir + FILENAME);
        oos = new ObjectOutputStream(fos);
        oos.writeObject(volumesByName);
        oos.writeObject(volumesById);
        oos.writeObject(slicesById);
        oos.flush();
        fos.getFD().sync();
        oos.close();
    }
    
    /**
     * Compacts all slice databases.
     * 
     * @throws BackendException
     */
    public void compactDB() throws BackendException {
        
        try {
            
            Logging.logMessage(Logging.LEVEL_INFO, this, "creating database checkpoint");
            
            // create a new file to indicate that checkpointing is in
            // progress (this should be an atomic operation)
            new File(dbDir).mkdirs();
            new File(dbDir + CP_LOCK_FILENAME).createNewFile();
            
            for (VolumeInfo vol : volumesById.values()) {
                
                for (SliceInfo info : vol.getSlices()) {
                    
                    final File sliceDBDir = new File(dbDir + info.sliceID);
                    final File sliceBackupDir = new File(dbDir + info.sliceID + ".backup");
                    
                    if (!sliceDBDir.exists())
                        sliceDBDir.mkdirs();
                    
                    // create a backup of the slice database
                    FSUtils.copyTree(sliceDBDir, sliceBackupDir);
                    
                    getSliceDB(info.sliceID, '*').sync();
                    
                    info.setLastAvailSqID(info.getCurrentSequenceID());
                }
            }
            
            syncVolumeAndSliceMetadata();
            
        } catch (Exception exc) {
            throw new BackendException(exc);
        }
    }
    
    public void completeDBCompaction() throws BackendException {
        
        // delete all backup files
        File[] backupDirs = new File(dbDir).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".backup");
            }
        });
        
        for (File dir : backupDirs)
            FSUtils.delTree(dir);
        
        // delete the lock file in order to indicate that checkpointing is
        // complete
        new File(dbDir + CP_LOCK_FILENAME).delete();
        
        Logging.logMessage(Logging.LEVEL_INFO, this, "database checkpointing complete");
    }
    
    public void restoreDB() {
        
        File lock = new File(dbDir + CP_LOCK_FILENAME);
        
        // return if checkpoint has been created w/o problems
        if (!lock.exists())
            return;
        
        // otherwise, something went wrong when checkpointing; in this case, all
        // backups need to be restored
        File[] backupDirs = new File(dbDir).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".backup");
            }
        });
        
        for (File dir : backupDirs) {
            
            final String originalDir = dir.getAbsolutePath().substring(0,
                dir.getAbsolutePath().length() - ".backup".length());
            final File original = new File(originalDir);
            
            FSUtils.delTree(original);
            dir.renameTo(original);
        }
        
        // finally, delete the checkpointing lock file
        lock.delete();
        
    }
    
    /**
     * Returns a list of all locally known slices.
     * 
     * @return
     */
    public List<SliceID> getSliceList() {
        List<SliceID> ids = new LinkedList<SliceID>(slicesById.keySet());
        return ids;
    }
    
    /**
     * Returns a list of all locally known slices from a certain volume.
     * 
     * @param volumeId
     *            the volume ID
     * @return
     */
    public Collection<SliceInfo> getSlicesByVolume(String volumeId) {
        return volumesById.get(volumeId).getSlices();
    }
    
    /**
     * Checks if an operation can be performed on a slice.
     * 
     * @param modifying
     *            true if operation writes data, false if read-only operation
     */
    protected boolean canPerformOperation(SliceInfo info, boolean modifying) {
        if (info == null)
            return false;
        if (info.getStatus() == SliceInfo.SliceStatus.ONLINE)
            return true;
        else if ((info.getStatus() == SliceInfo.SliceStatus.READONLY) && !modifying)
            return true;
        else
            return false;
        
    }
    
    /**
     * Reloads the content of the slice with the given ID from the local
     * database.
     * 
     * @param sliceId
     *            the slice ID
     * @throws BackendException
     */
    public void reInitSlice(SliceID sliceId) throws BackendException {
        StorageManager sm = mngrMap.remove(sliceId);
        if (sm != null)
            sm.shutdown();
    }
    
    /**
     * Discards all persistent and non-persistent state and invokes
     * <code>init</code>.
     * 
     * @throws BackendException
     */
    public void reset() throws BackendException {
        
        try {
            closeSliceDBs();
            FSUtils.delTree(new File(dbDir));
            init();
            
        } catch (IOException exc) {
            throw new BackendException(exc);
        } catch (ClassNotFoundException exc) {
            throw new BackendException(exc);
        }
    }
    
    /**
     * Checks whether a volume with the given name is known locally.
     * 
     * @param volumeName
     *            the volume name
     * @return
     */
    public boolean hasVolume(String volumeName) {
        return volumesByName.containsKey(volumeName);
    }
    
    /**
     * Checks whether a volume with the given ID is known locally.
     * 
     * @param volumeId
     *            the volume ID
     * @return
     */
    public boolean hasVolumeWithId(String volumeId) {
        return volumesById.containsKey(volumeId);
    }
    
    /**
     * Returns the metadata for the volume with the given name, if such a volume
     * exists locally.
     * 
     * @param volumeName
     *            the volume name
     * @return
     */
    public VolumeInfo getVolumeByName(String volumeName) throws UserException {
        
        VolumeInfo volume = volumesByName.get(volumeName);
        if (volume == null)
            throw new UserException(ErrNo.ENOENT, "volume '" + volumeName
                + "' not found on this MRC");
        
        return volume;
    }
    
    /**
     * Returns the metadata for the volume with the given ID, if such a volume
     * exists locally.
     * 
     * @param volumeId
     *            the volume name
     * @return
     */
    public VolumeInfo getVolumeById(String volumeId) throws UserException {
        
        VolumeInfo volume = volumesById.get(volumeId);
        if (volume == null)
            throw new UserException(ErrNo.ENOENT, "volume with id " + volumeId
                + " not found on this MRC");
        
        return volume;
    }
    
    /**
     * Returns a list of all locally known volumes.
     * 
     * @return a list of all locally known volumes
     */
    public List<VolumeInfo> getVolumes() {
        return new ArrayList<VolumeInfo>(volumesById.values());
    }
    
    /**
     * Deletes a volume.
     * 
     * @param volumeName
     *            the volume name
     * @throws UserException
     * @throws IOException
     */
    public void deleteVolume(String volumeName) throws UserException, IOException, BackendException {
        
        VolumeInfo volume = volumesByName.get(volumeName);
        if (volume == null)
            throw new UserException(ErrNo.ENOENT, "volume '" + volumeName
                + "' not found on this MRC");
        
        for (SliceInfo slice : volume.getSlices()) {
            
            slicesById.get(slice.sliceID).setDeleted();
            try {
                StorageManager sMan = getSliceDB(slice.sliceID, '*');
                mngrMap.remove(slice.sliceID);
                sMan.cleanup();
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_WARN, this, "slice " + slice.sliceID
                    + " could not be cleaned up");
            }
        }
        
        volumesByName.remove(volumeName);
        volumesById.remove(volume.getId());
    }
    
    /**
     * Removes a single slice from the slices index. This method can be invoked
     * to dispose of a deleted slice in a deferred fashion, if slice information
     * needs to be accessed after the deletion has taken place.
     * 
     * @param sliceId
     *            the ID of the slice to delete
     */
    public void removeSliceFromIndex(SliceID sliceId) {
        slicesById.remove(sliceId);
    }
    
    /**
     * Returns the slice database that stores the given path.
     * 
     * @param volumeId
     *            the volume ID
     * @param relPath
     *            the path to the resource within the volume
     * @param accessMode
     *            the internal access mode to the database
     * @return
     * @throws UserException
     * @throws BackendException
     */
    public StorageManager getSliceDB(String volumeId, String relPath, char accessMode)
        throws UserException, BackendException {
        
        VolumeInfo volume = volumesById.get(volumeId);
        if (volume == null)
            throw new UserException("could not find volume '" + volumeId);
        
        SliceID sliceId = partMan.getSlice(volumeId, relPath);
        
        return getSliceDB(sliceId, accessMode);
    }
    
    /**
     * Returns the slice database that is responsible for the given file ID.
     * 
     * @param volumeId
     *            the volume ID
     * @param fileId
     *            the file ID
     * @param accessMode
     *            the internal access mode to the database
     * @return
     * @throws UserException
     * @throws BackendException
     */
    public StorageManager getSliceDB(String volumeId, long fileId, char accessMode)
        throws UserException, BackendException {
        
        VolumeInfo volume = volumesById.get(volumeId);
        if (volume == null)
            throw new UserException("could not find volume '" + volumeId);
        
        SliceID sliceId = partMan.getSlice(volumeId, fileId);
        
        return getSliceDB(sliceId, accessMode);
    }
    
    /**
     * Returns the slice database for the given slice ID.
     * 
     * @param sliceId
     *            the slice ID
     * @param accessMode
     *            the internal access mode to the database
     * @return
     * @throws UserException
     * @throws BackendException
     */
    public StorageManager getSliceDB(SliceID sliceId, char accessMode) throws UserException,
        BackendException {
        
        StorageManager mngr = mngrMap.get(sliceId);
        if (mngr != null)
            return mngr;
        
        SliceInfo slice = slicesById.get(sliceId);
        if (slice == null)
            throw new UserException("could not find slice '" + sliceId);
        
        // check if the slice is accessible
        if (accessMode != '*' && !canPerformOperation(slice, accessMode == 'w'))
            throw new UserException("slice '" + sliceId + "' is "
                + (accessMode == 'w' ? "read-only or " : "") + "currently unavailable");
        
        mngr = new StorageManager(dbDir + "/" + sliceId, sliceId);
        mngr.startup();
        
        mngrMap.put(sliceId, mngr);
        return mngr;
    }
    
    /**
     * 
     * @param volumeID
     * @param fileID
     * @return true, if the file with the given ID exists, false otherwise.
     * @throws UserException
     *             - if volume does not exist
     * @throws BackendException
     *             - if a backendError occur
     */
    public boolean exists(String volumeID, String fileID) throws UserException, BackendException {
        // check the volume - if not available throw UserException
        VolumeInfo volume = volumesById.get(volumeID);
        if (volume == null)
            throw new UserException("could not find volume '" + volumeID);
        
        // get the sliceID - if not available return false.
        SliceID sliceId = null;
        
        sliceId = partMan.getSlice(volumeID, Long.valueOf(fileID));
        
        // check sliceID for info objects - if not available return false.
        if (slicesById.get(sliceId) == null)
            return false;
        
        // get the responsible StorageManager - if not available return false.
        StorageManager mngr = mngrMap.get(sliceId);
        if (mngr != null)
            return mngr.exists(fileID);
        else
            return getSliceDB(sliceId, 'r').exists(fileID);
    }
    
    /**
     * Syncs all slice databases.
     * 
     * @throws BackendException
     */
    public void syncSliceDBs() throws BackendException {
        for (StorageManager mngr : mngrMap.values())
            mngr.sync();
    }
    
    /**
     * Closes all slice databases.
     * 
     * @throws BackendException
     */
    public void closeSliceDBs() throws BackendException {
        for (StorageManager mngr : mngrMap.values())
            mngr.shutdown();
    }
    
    /**
     * Adds a new listener that is notified in response to volume changes.
     * 
     * @param listener
     * @throws IOException
     * @throws BackendException
     */
    public void addVolumeChangeListener(VolumeChangeListener listener) throws IOException,
        BackendException {
        
        vcListeners.add(listener);
        
        for (VolumeInfo vol : getVolumes())
            notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, vol);
    }
    
    public void notifyVolumeChangeListeners(int mod, VolumeInfo vol) throws IOException {
        
        try {
            for (VolumeChangeListener listener : vcListeners)
                listener.volumeChanged(mod, vol);
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    /**
     * Generates a new globally unique volume ID
     * 
     * @return
     * @throws SocketException
     */
    public static String generateNewVolumeId() throws SocketException {
        return new SliceID(1).getVolumeId();
    }
    
    public void dumpDB(String dumpFilePath) throws Exception {
        
        BufferedWriter xmlWriter = new BufferedWriter(new FileWriter(dumpFilePath));
        xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlWriter.write("<filesystem dbversion=\"" + VersionManagement.getMrcDataVersion()
            + "\">\n");
        
        for (VolumeInfo volume : volumesById.values()) {
            xmlWriter.write("<volume id=\""
                + volume.getId()
                + "\" name=\""
                + volume.getName()
                + "\" acPolicy=\""
                + volume.getAcPolicyId()
                + "\" osdPolicy=\""
                + volume.getOsdPolicyId()
                + (volume.getOsdPolicyArgs() != null ? "\" osdPolicyArgs=\""
                    + volume.getOsdPolicyArgs() : "") + "\" partPolicy=\""
                + volume.getPartitioningPolicyId() + "\">\n");
            
            for (SliceInfo slice : volume.getSlices()) {
                xmlWriter.write("<slice id=\"" + slice.sliceID + "\">\n");
                StorageManager sMan = getSliceDB(slice.sliceID, 'r');
                sMan.dumpDB(xmlWriter);
                xmlWriter.write("</slice>\n");
            }
            
            xmlWriter.write("</volume>\n");
        }
        
        xmlWriter.write("</filesystem>\n");
        xmlWriter.close();
    }
    
    public void restoreDBFromDump(String dumpFilePath) throws Exception {
        
        // First, check if any volume exists already. If so, deny the operation
        // for security reasons.
        if (!volumesById.isEmpty())
            throw new Exception(
                "Restoring from a dump is only possible on an MRC with no database. Please delete the existing MRC database on the server and restart the MRC!");
        
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        sp.parse(new File(dumpFilePath), new DefaultHandler() {
            
            private StorageManager sMan;
            
            private RestoreState   state;
            
            private int            dbVersion = 1;
            
            public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
                
                try {
                    
                    if (qName.equals("volume")) {
                        String id = attributes.getValue(attributes.getIndex("id"));
                        String name = attributes.getValue(attributes.getIndex("name"));
                        long acPol = Long.parseLong(attributes.getValue(attributes
                                .getIndex("acPolicy")));
                        long osdPol = Long.parseLong(attributes.getValue(attributes
                                .getIndex("osdPolicy")));
                        long partPol = Long.parseLong(attributes.getValue(attributes
                                .getIndex("partPolicy")));
                        String osdPolArgs = attributes.getIndex("osdPolicyArgs") == -1 ? null
                            : attributes.getValue(attributes.getIndex("osdPolicyArgs"));
                        
                        createVolume(id, name, acPol, osdPol, osdPolArgs, partPol, true, false);
                    }

                    else if (qName.equals("slice")) {
                        SliceID id = new SliceID(attributes.getValue(attributes.getIndex("id")));
                        
                        createSlice(new SliceInfo(id, null), false);
                        
                        sMan = getSliceDB(id, '*');
                        state = new StorageManager.RestoreState();
                    }

                    else if (qName.equals("filesystem"))
                        try {
                            dbVersion = Integer.parseInt(attributes.getValue(attributes
                                    .getIndex("dbversion")));
                        } catch (Exception exc) {
                            Logging.logMessage(Logging.LEVEL_WARN, this,
                                "restoring database with invalid version number");
                        }
                    
                    else
                        sMan.restoreDBFromDump(qName, attributes, state, true, dbVersion);
                    
                } catch (Exception exc) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this,
                        "could not restore DB from XML dump: " + exc);
                }
            }
            
            public void endElement(String uri, String localName, String qName) throws SAXException {
                
                try {
                    if (qName.equals("volume") || qName.equals("slice")
                        || qName.equals("filesystem"))
                        return;
                    
                    sMan.restoreDBFromDump(qName, null, state, false, dbVersion);
                } catch (Exception exc) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this,
                        "could not restore DB from XML dump");
                    Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                }
            }
            
            public void endDocument() throws SAXException {
                try {
                    compactDB();
                    completeDBCompaction();
                } catch (Exception exc) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this,
                        "could not restore DB from XML dump");
                    Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                }
            }
            
        });
    }
}