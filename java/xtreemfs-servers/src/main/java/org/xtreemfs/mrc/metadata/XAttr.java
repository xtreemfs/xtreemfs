/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;


/**
 * Represents a single extended attribute.
 */
public interface XAttr {
    
    public String getKey();
    
    public byte[] getValue();
    
    public String getOwner();
}