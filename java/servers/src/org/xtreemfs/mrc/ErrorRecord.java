/*
 * Copyright (c) 2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.util.OutputUtils;

/**
 * Encapsuls all types of errors.
 */
public final class ErrorRecord {
    
    /**
     * error type
     */
    private final ErrorType  type;
    
    /**
     * error code (as defined in the protocol)
     */
    private final POSIXErrno errno;
    
    /**
     * an error message, can include a stack trace
     */
    private final String     message;
    
    /**
     * the throwable thrown in the service that caused the error
     */
    private final Throwable  throwable;
    
    public ErrorRecord(ErrorType type, POSIXErrno errno, String message) {
        this(type, errno, message, null);
    }
    
    public ErrorRecord(ErrorType type, POSIXErrno errno, String message, Throwable throwable) {
        this.type = type;
        this.errno = errno;
        this.message = message;
        this.throwable = throwable;
    }
    
    public POSIXErrno getErrorCode() {
        return errno;
    }
    
    public String getErrorMessage() {
        return message;
    }
    
    public ErrorType getErrorType() {
        return type;
    }
    
    public Throwable getThrowable() {
        return throwable;
    }
    
    public String getStackTrace() {
        return OutputUtils.stackTraceToString(throwable);
    }
    
    public String toString() {
        String stackTrace = OutputUtils.stackTraceToString(throwable);
        return this.type + "." + this.errno + ":" + this.message
            + (stackTrace == null ? "" : ", caused by: " + stackTrace);
    }
    
    public String toJSON() {
        return "{ \"errno\": " + this.errno + ", \"errorMessage\" : \""
            + (this.message.replace("\"", "\\\"")) + "\" }";
    }
    
}
