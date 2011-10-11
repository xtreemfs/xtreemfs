/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;

import com.google.protobuf.Message;

/**
 * <p>Abstract request with common fields and methods for all services.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/05/11
 */
public abstract class AugmentedServiceRequest extends AugmentedRequest {
    
    private static long            rqIdCounter = 1L;
    
    /**
     * <p>Original request received by the server.</p>
     */
    private final RPCServerRequest rpcRequest;
    
    private final long             requestId;
    
    /**
     * <p>Message that belongs to the request.</p>
     */
    private Message                message;
    
    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed and do not have high priority.</p>
     * 
     * @param rpcRequest - the original request received by a client.
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     */
    public AugmentedServiceRequest(RPCServerRequest rpcRequest, int type, long deltaMaxTime) {
        
        this(rpcRequest, type, deltaMaxTime, false);
    }
    
    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed.</p>
     * 
     * @param rpcRequest - the original request received by a client.
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     */
    public AugmentedServiceRequest(RPCServerRequest rpcRequest, int type, long deltaMaxTime, boolean highPriority) {
        
        this(rpcRequest, type, 0L, deltaMaxTime, highPriority);
    }
    
    /**
     * <p>Constructor for requests that will require a specific amount of bandwidth during processing, that is 
     * proportional to the processing time.</p>
     * 
     * @param rpcRequest - the original request received by a client.
     * @param type - identifier for this kind of request.
     * @param size - amount of bandwidth occupied during processing of this request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     */
    public AugmentedServiceRequest(RPCServerRequest rpcRequest, int type, long size, long deltaMaxTime, 
            boolean highPriority) {
        
        super(type, size, deltaMaxTime, highPriority);
        
        this.requestId = rqIdCounter++;
        this.rpcRequest = rpcRequest;
    }
    
    public final RPCServerRequest getRPCRequest() {
        
        return rpcRequest;
    }

    public final Message getRequestArgs() {
        
        return message;
    }

    public final void setRequestArgs(Message requestArgs) {
        
        this.message = requestArgs;
    }
    
    /**
     * @return the requestId
     */
    public long getRequestId() {
        return requestId;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "#" + requestId + "{ " + String.valueOf(rpcRequest) + " }";
    }
}