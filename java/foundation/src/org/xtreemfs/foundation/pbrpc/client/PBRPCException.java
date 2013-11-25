/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.client;

import java.io.IOException;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;

/**
 *
 * @author bjko
 */
public class PBRPCException extends IOException {

    final ErrorResponse response;

    final String        message;

    public PBRPCException(String ioError) {
        super();
        this.message = ioError;
        response = null;
    }

    public PBRPCException(ErrorResponse response) {
        super();
        this.response = response;
        this.message = null;
    }

    public RPC.ErrorType getErrorType() {
        if (response != null)
            return response.getErrorType();
        else
            return RPC.ErrorType.IO_ERROR;
    }
    
    public RPC.POSIXErrno getPOSIXErrno() {
        if (response != null)
            return response.getPosixErrno();
        else
            return RPC.POSIXErrno.POSIX_ERROR_NONE;
    }

    public String getErrorMessage() {
        if (response != null)
            return response.getErrorMessage();
        else
            return this.message;
    }

    public String getDebugInfo() {
        if (response != null)
            return response.getDebugInfo();
        else
            return "";
    }

    public String getRedirectToServerUUID() {
        if (response != null)
            return response.getRedirectToServerUuid();
        else
            return "";
    }

    public ErrorResponse getErrorResponse() {
        return response;
    }

    @Override
    public String getMessage() {
        if (response != null) {
            return response.getErrorType().toString()+"/"+response.getPosixErrno().toString()+": "+response.getErrorMessage()+" / "+response.getDebugInfo();
        } else {
            return this.message;
        }
    }

}
