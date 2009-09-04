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

/**
 * This interface defines how volume-related metadata is accessed.
 * 
 * XtreemFS file system content is arranged in volumes, with each volume having
 * its own directory tree. A volume has a globally unique name and id.
 * 
 * A volume holds different policies. The OSD policy determines which OSDs may
 * by default be allocated to files. The behavior of this policy may depend on
 * the policy's arguments, which are represented by an opaque string. The access
 * control policy defines the circumstances under which users are allowed to
 * access the volume.
 * 
 * @author stender
 * 
 */
public interface VolumeInfo {
    
    /**
     * Returns the volume's ID
     * 
     * @return the volume's ID
     */
    public String getId();
    
    /**
     * Returns the volume's name.
     * 
     * @return the volume's name
     */
    public String getName();
    
    /**
     * Returns the volume's OSD selection policy.
     * 
     * @return the volume's OSD selection policy
     */
    public short[] getOsdPolicy();
    
    /**
     * Returns the volume's replica selection policy.
     * 
     * @return the volume's replica selection policy
     */
    public short[] getReplicaPolicy();
    
    /**
     * Returns the preferred replication factor for automatic on-close
     * replication.
     * 
     * @return the replication factor
     */
    public int getAutoReplFactor();
    
    /**
     * Returns the volume's access control policy ID.
     * 
     * @return the volume's access control policy ID.
     */
    public short getAcPolicyId();
    
    /**
     * Returns the current approximate size of all files in the volume in bytes.
     * 
     * @return the volume's approximate size
     */
    public long getVolumeSize() throws DatabaseException;
    
    /**
     * Returns the number of files currently stored in the volume.
     * 
     * @return the number of files
     */
    public long getNumFiles() throws DatabaseException;
    
    /**
     * Returns the number of directories currently stored in the volume.
     * 
     * @return the number of directories
     */
    public long getNumDirs() throws DatabaseException;
    
    /**
     * Sets the volume's OSD selection policy.
     * 
     * @param osdPolicy
     *            the new OSD selection policy for the volume
     */
    public void setOsdPolicy(short[] osdPolicy, AtomicDBUpdate update) throws DatabaseException;
    
    /**
     * Sets the volume's replica selection policy.
     * 
     * @param replicaPolicy
     *            the new replica selection policy for the volume
     */
    public void setReplicaPolicy(short[] replicaPolicy, AtomicDBUpdate update) throws DatabaseException;
    
    /**
     * Sets the preferred replication factor for automatic on-close replication.
     * 
     * @param replFactor
     *            the replication factor
     */
    public void setAutoReplFactor(int replFactor, AtomicDBUpdate update) throws DatabaseException;
    
    /**
     * Adds <code>diff</code> to the current volume size.
     * 
     * @param diff
     *            the difference between the new and the old volume size
     */
    public void updateVolumeSize(long diff, AtomicDBUpdate update) throws DatabaseException;
    
}