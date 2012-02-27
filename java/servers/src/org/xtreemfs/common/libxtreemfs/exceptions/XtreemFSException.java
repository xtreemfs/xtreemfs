/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.exceptions;

import java.io.IOException;

/**
 * 
 * <br>
 * Nov 22, 2011
 */
public class XtreemFSException extends IOException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
 * 
 */
    public XtreemFSException(String errorMsg) {
        super(errorMsg);
    }
}
