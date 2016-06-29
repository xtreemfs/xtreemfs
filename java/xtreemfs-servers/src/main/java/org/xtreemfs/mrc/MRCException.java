/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

public class MRCException extends Exception {

    public MRCException(String message, Exception cause) {
        super(message, cause);
    }
    
    public MRCException(String message) {
        super(message);
    }

    public MRCException(Exception cause) {
        super(cause);
    }
    
}
