/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.mrc.database;


public interface VolumeChangeListener {
        
    public void volumeChanged(VolumeInfo vol);
    
    public void volumeDeleted(String volumeId);
    
    public void attributeSet(String volumeId, String key, String value);
    
}
