/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;

import com.google.protobuf.Message;

import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.mrc.RequestDetails;

/**
 * 
 * @author bjko
 */
public class DIRRequest extends AugmentedRequest {
        
    private Message        requestMessage;
    
    private RequestDetails details;
    
    public DIRRequest(RPCServerRequest rpcRequest, int type, long deltaMaxTime, boolean highPriority) {
        super(rpcRequest, type, deltaMaxTime, highPriority);
        details = new RequestDetails();
    }
        
    public Message getRequestMessage() {
        return requestMessage;
    }
    
    public RequestDetails getDetails() {
        return details;
    }
    
    public void setDetails(RequestDetails details) {
        this.details = details;
    }
}
