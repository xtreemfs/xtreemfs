/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.exceptions;

/**
 * {@link Exception} that is thrown when the UUIDIterator list reached its end.
 * <br>Sep 3, 2011
 */
@SuppressWarnings("serial")
public class UUIDIteratorListIsEmpyException extends XtreemFSException {

    public UUIDIteratorListIsEmpyException(String message) {
        super(message);
    }
}
