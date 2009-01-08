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

package org.xtreemfs.new_mrc.volumes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.storage.BackendException;
import org.xtreemfs.new_mrc.MRCException;
import org.xtreemfs.new_mrc.dbaccess.DBAccessResultListener;
import org.xtreemfs.new_mrc.dbaccess.DatabaseException;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

public interface VolumeManager {
    
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
     * @param volumeId
     *            the volume ID
     * @param volumeName
     *            the volume name
     * @param fileAccessPolicyId
     *            the access policy
     * @param osdPolicyId
     *            the OSD selection policy
     * @return a volume info object
     * @throws UserException
     * @throws DatabaseException
     */
    public VolumeInfo createVolume(String volumeId, String volumeName, short fileAccessPolicyId,
        short osdPolicyId, String osdPolicyArgs, String ownerId, String owningGroupId,
        Map<String, Object> defaultStripingPolicy, DBAccessResultListener listener, Object context)
        throws UserException, DatabaseException, MRCException;
    
    /**
     * Checks whether a volume with the given name is known locally.
     * 
     * @param volumeName
     *            the volume name
     * @return
     */
    public boolean hasVolume(String volumeName) throws DatabaseException;
    
    /**
     * Checks whether a volume with the given ID is known locally.
     * 
     * @param volumeId
     *            the volume ID
     * @return
     */
    public boolean hasVolumeWithId(String volumeId) throws DatabaseException;
    
    /**
     * Returns the metadata for the volume with the given name, if such a volume
     * exists locally.
     * 
     * @param volumeName
     *            the volume name
     * @return
     */
    public VolumeInfo getVolumeByName(String volumeName) throws DatabaseException, UserException;
    
    /**
     * Returns the metadata for the volume with the given ID, if such a volume
     * exists locally.
     * 
     * @param volumeId
     *            the volume name
     * @return
     */
    public VolumeInfo getVolumeById(String volumeId) throws DatabaseException, UserException;
    
    /**
     * Returns a list of all locally known volumes.
     * 
     * @return a list of all locally known volumes
     */
    public List<VolumeInfo> getVolumes() throws DatabaseException;
    
    /**
     * Deletes a volume.
     * 
     * @param volumeName
     *            the volume name
     * @throws UserException
     * @throws IOException
     */
    public void deleteVolume(String volumeName, DBAccessResultListener listener, Object context)
        throws DatabaseException, UserException;
    
    public StorageManager getStorageManager(String volumeId);
    
    /**
     * Generates a new unique volume ID.
     * 
     * @return
     */
    public String newVolumeId();
    
    /**
     * Adds a new listener that is notified in response to volume changes.
     * 
     * @param listener
     * @throws IOException
     * @throws BackendException
     */
    public void addVolumeChangeListener(VolumeChangeListener listener) throws IOException,
        DatabaseException;
    
    public void notifyVolumeChangeListeners(int mod, VolumeInfo vol);
    
    public void dumpDB(String dumpFilePath) throws Exception;
    
    public void restoreDBFromDump(String dumpFilePath) throws Exception;
    
}