/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.utils;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.util.OutputUtils;

/**
 *
 * @author bjko
 */
public class ErrorUtils {

    public static ErrorResponse getErrorResponse(ErrorType type, POSIXErrno errno, String message, String debug) {
        return ErrorResponse.newBuilder().setErrorType(type).setPosixErrno(errno).setErrorMessage(message).setDebugInfo(debug).build();
    }

    public static ErrorResponse getErrorResponse(ErrorType type, POSIXErrno errno, String message, Throwable cause) {
        return ErrorResponse.newBuilder().setErrorType(type).setPosixErrno(errno).setErrorMessage(message).setDebugInfo(OutputUtils.stackTraceToString(cause)).build();
    }

    public static ErrorResponse getErrorResponse(ErrorType type, POSIXErrno errno, String message) {
        return ErrorResponse.newBuilder().setErrorType(type).setPosixErrno(errno).setErrorMessage(message).build();
    }

    public static ErrorResponse getInternalServerError(Throwable cause) {
        return ErrorResponse.newBuilder().setErrorType(ErrorType.INTERNAL_SERVER_ERROR).setPosixErrno(POSIXErrno.POSIX_ERROR_EIO).
                setErrorMessage(cause.toString()).setDebugInfo(OutputUtils.stackTraceToString(cause)).build();
    }
    
    public static ErrorResponse getInternalServerError(Throwable cause, String additionalErrorMessage) {
        return ErrorResponse.newBuilder().setErrorType(ErrorType.INTERNAL_SERVER_ERROR).setPosixErrno(POSIXErrno.POSIX_ERROR_EIO).
                setErrorMessage(additionalErrorMessage + "; " + cause.toString()).setDebugInfo(OutputUtils.stackTraceToString(cause)).build();
    }

    public static String formatError(ErrorResponse error) {
        if (error == null)
            return "no error";
        return error.getErrorType()+"/"+error.getPosixErrno()+": "+error.getErrorMessage() +(error.hasDebugInfo() ? ";\n"+error.getDebugInfo() : "");
    }

}
