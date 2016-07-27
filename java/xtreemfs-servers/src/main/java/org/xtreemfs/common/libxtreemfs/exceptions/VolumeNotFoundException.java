/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.exceptions;

/**
 * 
 * <br>
 * Sep 2, 2011
 */
@SuppressWarnings("serial")
public class VolumeNotFoundException extends XtreemFSException {

    /**
 * 
 */
    public VolumeNotFoundException(String volumeName) {
        super("Volume not found: " + volumeName);
    }

}
