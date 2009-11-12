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

package org.xtreemfs.mrc.database;

import java.util.Collection;

import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.metadata.FileMetadata;

public interface VolumeManager {
    
    public static final short[] DEFAULT_OSD_POLICY       = {
        (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue(),
        (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_RANDOM.intValue() };
    
    public static final short[] DEFAULT_REPL_POLICY      = {};
    
    public static final int     DEFAULT_AUTO_REPL_FACTOR = 1;
    
    public static final boolean DEFAULT_AUTO_REPL_FULL   = false;
    
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
     * @return a volume info object
     * @throws UserException
     * @throws DatabaseException
     */
    public VolumeInfo createVolume(FileAccessManager faMan, String volumeId, String volumeName,
        short fileAccessPolicyId, String ownerId, String owningGroupId, StripingPolicy defaultStripingPolicy,
        int initialAccessMode) throws UserException, DatabaseException;
    
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
     * Returns a version string for the database backend.
     * 
     * @return the database version of the backend
     */
    public String getDBVersion();
    
    /**
     * @return the replication manager of the DBS.
     */
    public ReplicationManager getReplicationManager();
}