/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd;

import org.xtreemfs.common.util.OutputUtils;

/**
 * Encapsuls all types of errors.
 */
public final class ErrorRecord {
    
    public enum ErrorClass {
        /**
         * any internal server error condition
         */
        INTERNAL_SERVER_ERROR,
        /**
         * indicates a malformed request
         */
        BAD_REQUEST,
        /**
         * a user exception for which a defined error code exists in the
         * protocol
         */
        USER_EXCEPTION,
        /**
         * redirect to another server
         */
        REDIRECT
    }
    
    /**
     * error type
     */
    private final ErrorClass errorClass;
    
    /**
     * error code (as defined in the protocol)
     */
    private final int        errorCode;
    
    /**
     * an error message, can include a stack trace
     */
    private final String     errorMessage;
    
    /**
     * the throwable thrown in the service that caused the error
     */
    private final Throwable  throwable;
    
    public ErrorRecord(ErrorClass errorClass, String errorMessage) {
        this(errorClass, 0, errorMessage, null);
    }
    
    public ErrorRecord(ErrorClass errorClass, String errorMessage, Throwable throwable) {
        this(errorClass, 0, errorMessage, throwable);
    }
    
    public ErrorRecord(ErrorClass errorClass, int errorCode, String errorMessage) {
        this(errorClass, errorCode, errorMessage, null);
    }
    
    public ErrorRecord(ErrorClass errorClass, int errorCode, String errorMessage,
        Throwable throwable) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorClass = errorClass;
        this.throwable = throwable;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public ErrorClass getErrorClass() {
        return errorClass;
    }
    
    public String toString() {
        
        String stackTrace = OutputUtils.stackTraceToString(throwable);
        return this.errorClass + "." + this.errorCode + ":" + this.errorMessage
            + (stackTrace == null ? "" : ", caused by: " + stackTrace);
    }
    
    public String toJSON() {
        return "[ \"error-code\": " + this.errorCode + ", \"error-message\" : \""
            + (this.errorMessage.replace("\"", "\\\"")) + "\" ]";
    }
    
}
