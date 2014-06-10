/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;

public interface VolumeManager {
    
    public static final short[] DEFAULT_OSD_POLICY       = {
        (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.getNumber(),
        (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_RANDOM.getNumber() };
    
    public static final short[] DEFAULT_REPL_POLICY      = {};
    
    public static final boolean DEFAULT_ALLOW_SNAPS      = false;
    
    public static final char    SNAPSHOT_SEPARATOR       = '@';
    
    /**
     * Initializes the volume manager, including all volume databases.
     * 
     * @throws DatabaseException
     */
    public void init() throws DatabaseException;
    
    /**
     * Shuts down the volume manager.
     */
    public void shutdown();
    
    /**
     * Creates a new volume.
     * 
     * @param faMan
     *            the file access manager
     * @param volumeId
     *            the volume ID
     * @param volumeName
     *            the volume name
     * @param fileAccessPolicyId
     *            the access policy
     * @param ownerId
     *            the owner ID
     * @param owningGroupId
     *            the owning group ID
     * @param defaultStripingPolicy
     *            the default striping policy
     * @param initialAccessMode
     *            the initial access mode for the volume's root directory
     * @param volumeQuota
     *            the volume quota
     * @param attrs
     *            a list of user-defined attributes for the volume
     * @throws UserException
     * @throws DatabaseException
     */
    public void createVolume(FileAccessManager faMan, String volumeId, String volumeName,
        short fileAccessPolicyId, String ownerId, String owningGroupId, StripingPolicy defaultStripingPolicy,
 int initialAccessMode,
            long volumeQuota, List<KeyValuePair> attrs) throws UserException, DatabaseException;
    
    /**
     * Checks whether a volume with the given name is known locally.
     * 
     * @param volumeName
     *            the volume name
     * @return <code>true</code>, if the volume is known locally,
     *         <code>false</code>, otherwise
     */
    public boolean hasVolume(String volumeName) throws DatabaseException;
    
    /**
     * Checks whether a volume with the given ID is known locally.
     * 
     * @param volumeId
     *            the volume ID
     * @return <code>true</code>, if the volume is known locally,
     *         <code>false</code>, otherwise
     */
    public boolean hasVolumeWithId(String volumeId) throws DatabaseException;
    
    /**
     * Deletes a volume.
     * 
     * @param volumeName
     *            the volume name
     * @throws UserException
     * @throws DatabaseException
     */
    public void deleteVolume(String volumeName, DBAccessResultListener<Object> listener, Object context)
        throws DatabaseException, UserException;
    
    /**
     * Returns the storage manager for a given volume.
     * 
     * @param volumeId
     *            the volume ID
     * @return the storage manager
     * @throws UserException
     *             if the volume does not exist
     */
    public StorageManager getStorageManager(String volumeId) throws UserException;
    
    /**
     * Returns the storage manager for a given volume.
     * 
     * @param volumeName
     *            the volume name
     * @return the storage manager
     * @throws UserException
     *             if the volume does not exist
     */
    public StorageManager getStorageManagerByName(String volumeName) throws UserException;
    
    /**
     * Returns a collection of all storage managers.
     * 
     * @return a collection of all storage managers
     */
    public Collection<StorageManager> getStorageManagers();
    
    /**
     * Enforces a database checkpoint and blocks until the checkpoint is
     * complete.
     */
    public void checkpointDB() throws DatabaseException;
    
    /**
     * Generates a new unique volume ID.
     * 
     * @return a unique volume ID
     */
    public String newVolumeId();
    
    /**
     * Adds a new listener to all volumes that responds to volume changes.
     * 
     * @param listener
     *            the listener to add
     */
    public void addVolumeChangeListener(VolumeChangeListener listener);
    
    /**
     * Creates a new snapshot of the given directory in the given volume.
     * 
     * @param volumeId
     *            the volume ID
     * @param snapName
     *            the name to be assigned to the new snapshot
     * @param parentId
     *            the directory's parent directory ID
     * @param dir
     *            the directory
     * @param recursive
     *            specifies whether the snapshot will only contain the nested
     *            files in the directory, or the whole tree with all
     *            subdirectories
     * @throws UserException
     *             if the volume does not exist, or the snapshot exists already
     * @throws DatabaseException
     *             if a database error occurs
     */
    public void createSnapshot(String volumeId, String snapName, long parentId, FileMetadata dir,
        boolean recursive) throws UserException, DatabaseException;
    
    /**
     * Deletes the snapshot with the given name in the given directory.
     * 
     * @param volumeId
     *            the volume ID
     * @param dir
     *            the directory
     * @param snapName
     *            the name of the snapshot to delete
     * @throws UserException
     *             if the volume does not exist, or the snapshot does not exist
     * @throws DatabaseException
     *             if a database error occurs
     */
    public void deleteSnapshot(String volumeId, FileMetadata dir, String snapName) throws UserException,
        DatabaseException;
    
    /**
     * Resturns a collection of timestamps for all snapshots of the given
     * volume.
     * 
     * @param volName
     *            the volume name
     * @return a collection of snapshot timestamps
     * @throws UserException
     *             if the volume does not exist, or the snapshot does not exist
     * @throws DatabaseException
     *             if a database error occurs
     */
    public Collection<Long> getSnapTimestamps(String volName) throws UserException, DatabaseException;
    
    /**
     * Returns a version string for the database backend.
     * 
     * @return the database version of the backend
     */
    public String getDBVersion();
    
    /**
     * Returns the runtime status of the internal database as a collection of key-value pairs.
     * 
     * @return the database status
     */
    public Map<String, Object> getDBStatus();
    
}