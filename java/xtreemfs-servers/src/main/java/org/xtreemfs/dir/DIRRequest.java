/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;

import com.google.protobuf.Message;
import java.io.IOException;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.RequestDetails;

/**
 * 
 * @author bjko
 */
public class DIRRequest {
    
    private final RPCServerRequest rpcRequest;
    
    private Message        requestMessage;
    
    private RequestDetails      details;
    
    public DIRRequest(RPCServerRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
        details = new RequestDetails();
    }
    
    public void deserializeMessage(Message message) throws IOException {
        final ReusableBuffer payload = rpcRequest.getMessage();
        if (message != null) {
            if (payload != null) {
                ReusableBufferInputStream istream = new ReusableBufferInputStream(payload);
                requestMessage = message.newBuilderForType().mergeFrom(istream).build();
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "parsed request: %s",message.getClass().getSimpleName());
                }
            } else {
                requestMessage = message.getDefaultInstanceForType();
            }
        } else {
            requestMessage = null;
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "parsed request: empty message (emptyRequest)");
            }
        }

    }
    
    public Message getRequestMessage() {
        return requestMessage;
    }
    
    public void sendSuccess(Message response) {
        try {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "sending response: %s",response.getClass().getSimpleName());
            }
            rpcRequest.sendResponse(response,null);
        } catch (IOException ex) {
            //not much we can do if error occurs during send
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    public void sendInternalServerError(Throwable rootCause) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "sending internal server error: "+rootCause.toString());
        }
        rpcRequest.sendError(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, "internal server error: "+rootCause.toString(), OutputUtils.stackTraceToString(rootCause));
    }
    
    public void sendRedirectException(String addr, int port) {
        rpcRequest.sendRedirect(addr+":"+port);
    }
    
    public void sendError(ErrorType type, POSIXErrno error, String message) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "sending error return value: "+type+"/"+error+"/"+message);
        }
        rpcRequest.sendError(type, error, message);
    }
    
    
    public RequestDetails getDetails() {
        return details;
    }
    
    public void setDetails(RequestDetails details) {
        this.details = details;
    }

    public RPCServerRequest getRPCRequest() {
        return rpcRequest;
    }
}
