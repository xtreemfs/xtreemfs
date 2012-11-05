/*
 * Copyright (c) 2012 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.exceptions;

public class InvalidChecksumException extends XtreemFSException {

    private static final long serialVersionUID = 1L;

    public InvalidChecksumException(String errorMsg) {
        super(errorMsg);
    }

}
