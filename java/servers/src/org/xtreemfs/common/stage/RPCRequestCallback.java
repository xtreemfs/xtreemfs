/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;

import com.google.protobuf.Message;

/**
 * <p>Callback for RPC requests. Includes methods for responses in case of success or failure.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/12/2011
 */
public class RPCRequestCallback extends AbstractRPCRequestCallback {
    
    /**
     * @param request - original request to respond.
     */
    public RPCRequestCallback(RPCServerRequest request) {
        
        super(request);
    }
    
    /**
     * <p>Method to return an empty response.</p>
     * 
     * @return true, if callback execution could process the request successfully. false, if an error occurred.
     */
    public boolean success() {
        
        return success(null);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Callback#success(java.lang.Object)
     */
    @Override
    public boolean success(Object result) {
        
        return success((Message) result);
    }
}