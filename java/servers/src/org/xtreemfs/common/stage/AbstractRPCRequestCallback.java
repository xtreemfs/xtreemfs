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
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.foundation.util.OutputUtils;

import com.google.protobuf.Message;

/**
 * <p>Callback for RPC requests. Includes methods for responses in case of success or failure.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/12/2011
 */
public abstract class AbstractRPCRequestCallback implements Callback {
    
    private final RPCServerRequest request;
    
    /**
     * @param request - original request to respond.
     */
    public AbstractRPCRequestCallback(RPCServerRequest request) {
        
        this.request = request;
    }
    
    /**
     * <p>Constructor to wrap an existing request callback into a new one.</p>
     * 
     * @param callback
     */
    public AbstractRPCRequestCallback(AbstractRPCRequestCallback callback) {
        
        this.request = callback.request;
    }
    
    /**
     * <p>Sends a response to the sender of the request.</p>
     * 
     * @param response
     */
    public boolean success(Message response) {
        
        return success(response, null);
    }

    /**
     * <p>Sends a response together with the given data to the sender of the request.</p>
     * 
     * @param response
     * @param data
     * 
     * @return true, if callback execution could process the request successfully. false, if an error occurred.
     */
    public boolean success(Message response, ReusableBuffer data) {
        
        try {
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "sending response: %s",
                        response.getClass().getSimpleName());
            }
            request.sendResponse(response, data);
            return true;
        } catch (IOException ex) {
            
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Callback#failed(java.lang.Throwable)
     */
    @Override
    public void failed(Throwable error) {
        
        if (error instanceof ErrorResponseException) {
            failed((ErrorResponseException) error);
        } else {
            failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, error);
        }
    }
    
    /**
     * <p>Method to send error as a reply.</p>
     * 
     * @param error
     */
    public void failed(ErrorResponse error) {
        
        request.sendError(error);
    }

    /**
     * <p>Sends an error with given errType, posixErr and message to the sender of the request.</p>
     * 
     * @param errType
     * @param posixErr
     * @param exc.
     */
    public void failed(ErrorType errType, POSIXErrno posixErr, Throwable exc) {
        
        failed(errType, posixErr, exc.getMessage(), OutputUtils.stackTraceToString(exc));
    }
    
    /**
     * <p>Sends an error with given errType, posixErr and messages retrieved from e to the sender of the request.</p>
     * 
     * @param errType
     * @param posixErr
     * @param message
     * @param sTrace
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
     * @param message
     * @param sTrace
     */
    public void failed(ErrorType errType, POSIXErrno posixErr, String message, Throwable e) {
        
        failed(errType, posixErr, message, OutputUtils.stackTraceToString(e));
    }
    
    /**
     * <p>Sends an error with given errType, posixErr and messages retrieved from e to the sender of the request.</p>
     * 
     * @param errType
     * @param posixErr
     * @param message
     * @param sTrace
     */
    public void failed(ErrorType errType, POSIXErrno posixErr, String message, String sTrace) {
        
        if (sTrace != null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending errno exception %s/%s/%s/%s", errType, 
                        posixErr, message, sTrace);
            }
            request.sendError(errType, posixErr, message, sTrace);
        } else {
            failed(errType, posixErr, message);
        }
    }
    
    /**
     * <p>Sends a redirect-response together with a given targetUUID back to the sender of the request.</p>
     * 
     * @param targetUUID
     */
    public void redirect(String targetUUID) {
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending redirect to %s", targetUUID);
        }
        request.sendRedirect(targetUUID);
    }
}
