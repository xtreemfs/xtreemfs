/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;

import com.google.protobuf.GeneratedMessage;

/**
 * 
 * @author bjko
 */
public class MRCRequest extends AugmentedRequest {
        
    private GeneratedMessage       response;
    
    private ErrorRecord            error;
    
    private RequestDetails         details;
        
    public MRCRequest(RPCServerRequest rpcRequest, int type, long deltaMaxTime, boolean highPriority) {
        super(rpcRequest, type, deltaMaxTime, highPriority);
        details = new RequestDetails();
    }
        
    public ErrorRecord getError() {
        return error;
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
    
    public RequestDetails getDetails() {
        return details;
    }
    
    public void setDetails(RequestDetails details) {
        this.details = details;
    }
    
    public String toString() {
        
        if (getRPCRequest() == null)
            return null;
        
        return getRPCRequest().toString();
    }
}
