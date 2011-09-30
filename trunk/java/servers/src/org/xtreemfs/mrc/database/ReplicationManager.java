/*
 * Copyright (c) 2009-2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database;

/**
 * Interface to manipulate the replication-setup of the MRC DB.
 * 
 * @author flangner
 * @since 10/19/2009
 */

public interface ReplicationManager {
    
    /**
     * <p>
     * Changes the database replication master. Uses this, if
     * your {@link BabuDBRequestListener} recognizes an failure due
     * the replication and want to help BabuDB to recognize it.
     * </p>
     */
    public void manualFailover();
}
