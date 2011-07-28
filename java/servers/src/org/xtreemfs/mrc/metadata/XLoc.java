/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;


/**
 * Interface for accessing information about single replica (X-Location) of a
 * file. X-Locations are stored in a file's X-Locations List and contain
 * information about file data storage locations.
 */
public interface XLoc {
    
    /**
     * The number of OSDs in the replica.
     *  
     * @return the number of OSDs
     */
    public short getOSDCount();
    
    /**
     * Returns the OSD UUID at the given index position.
     * 
     * @param index the index of the OSD UUID
     * @return the OSD UUID at index position <code>position</code>
     */
    public String getOSD(int index);
    
    /**
     * Returns the striping policy assigned to the replica.
     * 
     * @return the striping policy
     */
    public StripingPolicy getStripingPolicy();
    
    /**
     * Returns the replication flags assigned to the replica
     * 
     * @return the replication flags
     */
    public int getReplicationFlags();
    
    /**
     * Assigns new replication flags to the replica.
     * 
     * @param replFlags the replication flags
     */
    public void setReplicationFlags(int replFlags);
}