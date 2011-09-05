/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

import java.io.IOException;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.util.OutputUtils;

import com.google.protobuf.Message;

/**
 * <p>Abstract request with common fields and methods for all services.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/05/11
 */
public abstract class Request {

    /**
     * <p>Original request received by the server.</p>
     */
    private final RPCServerRequest rpcRequest;
    
    /**
     * <p>Message that belongs to the request.</p>
     */
    private Message requestArgs;
    
    /**
     * <p>Constructor to initialize a new request.</p>
     * 
     * @param rpcRequest - the request received by the server.
     */
    public Request(RPCServerRequest rpcRequest) {
        
        this.rpcRequest = rpcRequest;
    }
    
    public final RPCServerRequest getRPCRequest() {
        
        return rpcRequest;
    }

    public final Message getRequestArgs() {
        
        return requestArgs;
    }

    public final void setRequestArgs(Message requestArgs) {
        
        this.requestArgs = requestArgs;
    }

    public final void sendSuccess(Message response, ReusableBuffer data) {
        
        try {
            rpcRequest.sendResponse(response, data);
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    public final void sendRedirectException(String addr, int port) {
        
        rpcRequest.sendRedirect(addr+":"+port);
    }

    public final void sendInternalServerError(Throwable cause) {
        
        if (rpcRequest != null) {
            rpcRequest.sendError(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "internal server error:" 
                    + cause, OutputUtils.stackTraceToString(cause));
        } else {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "internal server error on internal request: %s",
                    cause.toString());
            Logging.logError(Logging.LEVEL_ERROR, this, cause);
        }
    }

    public final void sendError(ErrorType type, POSIXErrno errno, String message) {
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending errno exception %s/%s/%s", type, 
                    errno, message);
        }
        rpcRequest.sendError(type, errno, message);
    }

    public final void sendError(ErrorType type, POSIXErrno errno, String message, String debugInfo) {
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending errno exception %s/%s/%s", type, 
                    errno, message);
        }
        rpcRequest.sendError(type, errno, message, debugInfo);
    }
    
    public final void sendError(ErrorResponse error) {
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending exception %s", error.toString());
        }
        rpcRequest.sendError(error);
    }
}
