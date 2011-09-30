/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import java.util.Iterator;

/**
 * Interface for accessing an X-Locations List. X-Locations Lists contain
 * information about storage locations (OSDs) used for different replicas of a
 * file.
 */
public interface XLocList {
    
    /**
     * Returns the number of replicas currently stored in the X-Locations List.
     * 
     * @return the number of replicas
     */
    public int getReplicaCount();
    
    /**
     * Returns the replica at the given index position.
     * 
     * @param index
     *            position for the replica to return
     * @return the replica at position <code>index</code>
     */
    public XLoc getReplica(int index);
    
    /**
     * Returns the version number associated with the X-Locations List.
     * 
     * @return the version number
     */
    public int getVersion();
    
    /**
     * Returns an iterator for all replicas.
     * 
     * @return an iterator
     */
    public Iterator<XLoc> iterator();
    
    /**
     * Returns the replica update policy.
     * 
     * @return the replica update policy
     */
    public String getReplUpdatePolicy();
    
}