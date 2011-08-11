/*
 * Copyright (c) 2008-2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;


public interface MRCStatusListener {
    public void MRCConfigChanged(MRCConfig config);
    
    public void volumeCreated();
    
    public void volumeDeleted();
    
    /**
     * Called when DIR is shutting down.
     */
	public void shuttingDown();
    
}
