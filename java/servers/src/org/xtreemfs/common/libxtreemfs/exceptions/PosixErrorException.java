/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.exceptions;

import java.io.IOException;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;

public class PosixErrorException extends XtreemFSException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private POSIXErrno posixError;
   
    public PosixErrorException(String message) {
        super(message);
    }
    public PosixErrorException(POSIXErrno posixError, String message) {
        super(message);
        this.posixError = posixError;
    }
    
    public POSIXErrno getPosixError() {
        return this.posixError;
    }
}
