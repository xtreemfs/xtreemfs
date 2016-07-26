/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.exceptions;

/**
 *
 * Thrown if a given UUID was not found in the xlocset of a file.
 */
public class UUIDNotInXlocSetException extends XtreemFSException {

    /**
     * @param errorMsg
     */
    public UUIDNotInXlocSetException(String errorMsg) {
        super(errorMsg);
    }

    private static final long serialVersionUID = 1L;

}
