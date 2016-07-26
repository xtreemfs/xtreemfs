/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.exceptions;

import org.xtreemfs.common.libxtreemfs.UUIDIterator;

/**
 * This exception is thrown, when a request was denied due to an outdated view (XLocSet). <br>
 * The client should reload the view (XLocSet), refresh {@link UUIDIterator}s based on it and retry the request.
 **/
public class InvalidViewException extends XtreemFSException {

    private static final long serialVersionUID = 1L;

    public InvalidViewException(String errorMsg) {
        super(errorMsg);
    }

}
