/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.exceptions;

/**
 *
 * <br>Nov 22, 2011
 */
public class InternalServerErrorException extends XtreemFSException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public InternalServerErrorException(String errorMsg) {
        super(errorMsg);
    }
}
