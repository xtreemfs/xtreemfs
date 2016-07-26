/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.io.IOException;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class MRCRequest {
    
    private final RPCServerRequest rpcRequest;
    
    private Message                requestArgs;
    
    private GeneratedMessage       response;
    
    private ErrorRecord            error;
    
    private RequestDetails         details;
    
    public MRCRequest() {
        this(null);
    }
    
    public MRCRequest(RPCServerRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
        details = new RequestDetails();
    }
    
    public RPCServerRequest getRPCRequest() {
        return rpcRequest;
    }
    
    public ErrorRecord getError() {
        return error;
    }
    
    public void deserializeMessage(Message message) throws IOException {
        final ReusableBuffer payload = rpcRequest.getMessage();
        requestArgs = message.newBuilderForType().mergeFrom(new ReusableBufferInputStream(payload)).build();
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "parsed request: %s", requestArgs.toString());
        }
    }
    
    public void setError(ErrorType type, POSIXErrno errno, String message, Throwable th) {
        this.error = new ErrorRecord(type, errno, message, th);
    }
    
    public void setError(ErrorType type, String message, Throwable th) {
        this.error = new ErrorRecord(type, POSIXErrno.POSIX_ERROR_NONE, message, th);
    }
    
    public void setError(ErrorType type, POSIXErrno errno, String message) {
        this.error = new ErrorRecord(type, errno, message);
    }
    
    public void setError(ErrorType type, String message) {
        this.error = new ErrorRecord(type, POSIXErrno.POSIX_ERROR_NONE, message);
    }
    
    public void setError(ErrorRecord error) {
        this.error = error;
    }
    
    public GeneratedMessage getResponse() {
        return response;
    }
    
    public void setResponse(GeneratedMessage response) {
        this.response = response;
    }
    
    public Message getRequestArgs() {
        return requestArgs;
    }
    
    public void setRequestArgs(Message requestArgs) {
        this.requestArgs = requestArgs;
    }
    
    public RequestDetails getDetails() {
        return details;
    }
    
    public void setDetails(RequestDetails details) {
        this.details = details;
    }
    
    public String toString() {
        
        if (rpcRequest == null)
            return null;
        
        return rpcRequest.toString();
    }
}
