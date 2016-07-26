/*
 * Copyright (c) 2008-2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;


public interface DIRStatusListener {
    
    public void addressMappingAdded();
    
    public void addressMappingDeleted();
    
    public void DIRConfigChanged(DIRConfig config);
    
    public void serviceRegistered();
    
    public void serviceDeregistered();

    /**
     * Called when DIR is shutting down.
     */
	public void shuttingDown();
}
