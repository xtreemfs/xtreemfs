/*
 * Copyright (c) 2010-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;


/**
 * Interface for accessing a replication policy.
 */
public interface ReplicationPolicy {
    
    /**
     * Returns the policy name.
     * 
     * @return the policy name
     */
    public String getName();
    
    /**
     * Returns the number of replicas.
     * 
     * @return the number of replicas
     */
    public int getFactor();
    
    /**
     * Returns a bit mask of replication flags.
     * 
     * @return a bit mask of replication flags
     */
    public int getFlags();
    
}