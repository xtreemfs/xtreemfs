/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;

/**
 * This exception is thrown if something
 * 
 * @author bjko, stender
 */
public class UserException extends java.lang.Exception {
    
    private POSIXErrno errno;
    
    /**
     * Creates a new instance of <code>XtreemFSException</code> without detail
     * message.
     */
    public UserException() {
        this.errno = POSIXErrno.POSIX_ERROR_NONE;
    }
    
    public UserException(POSIXErrno errno) {
        this.errno = errno;
    }
    
    public UserException(POSIXErrno errno, String message) {
        super(message + " (errno=" + errno + ")");
        this.errno = errno;
    }
    
    public POSIXErrno getErrno() {
        return this.errno;
    }
}
