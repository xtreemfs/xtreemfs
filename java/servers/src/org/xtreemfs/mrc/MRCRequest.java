/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;

/**
 * 
 * @author bjko
 */
public class MRCRequest extends AugmentedRequest {
            
    private RequestDetails         details;
        
    public MRCRequest(RPCServerRequest rpcRequest, int type, long deltaMaxTime, boolean highPriority) {
        super(rpcRequest, type, deltaMaxTime, highPriority);
        details = new RequestDetails();
    }
        
    public RequestDetails getDetails() {
        return details;
    }
    
    public void setDetails(RequestDetails details) {
        this.details = details;
    }
    
    @Override
    public String toString() {
        
        if (getRPCRequest() == null)
            return null;
        
        return getRPCRequest().toString();
    }
}
