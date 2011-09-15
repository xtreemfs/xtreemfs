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
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.util.OutputUtils;

import com.google.protobuf.Message;

/**
 * <p>Callback for RPC requests. Includes methods for responses in case of success or failure.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/12/2011
 */
public class RPCRequestCallback implements Callback {

    private final RPCServerRequest request;
    
    /**
     * @param request - original request to respond.
     */
    public RPCRequestCallback(RPCServerRequest request) {
        
        this.request = request;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Callback#success(java.lang.Object)
     */
    @Override
    public void success(Object result) {
        success((Message) result);
    }
    
    /**
     * <p>Sends a response to the sender of the request.</p>
     * 
     * @param response
     */
    public void success(Message response) {
        
        success(response, null);
    }

    /**
     * <p>Sends a response together with the given data to the sender of the request.</p>
     * 
     * @param response
     * @param data
     */
    public void success(Message response, ReusableBuffer data) {
        
        try {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "sending response: %s",
                        response.getClass().getSimpleName());
            }
            request.sendResponse(response, data);
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Callback#failed(java.lang.Exception)
     */
    @Override
    public void failed(Exception error) {
        
        failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, error);
    }

    /**
     * <p>Sends an error with given errType, posixErr and message to the sender of the request.</p>
     * 
     * @param errType
     * @param posixErr
     * @param message
     */
    public void failed(ErrorType errType, POSIXErrno posixErr, String message) {
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending errno exception %s/%s/%s", errType, 
                    posixErr, message);
        }
        request.sendError(errType, posixErr, message);
    }
    
    /**
     * <p>Sends an error with given errType, posixErr and messages retrieved from e to the sender of the request.</p>
     * 
     * @param errType
     * @param posixErr
     * @param e
     */
    public void failed(ErrorType errType, POSIXErrno posixErr, Exception e) {
        
        request.sendError(errType, posixErr, "internal server error: " + e.getMessage(), 
                OutputUtils.stackTraceToString(e));
    }
}
