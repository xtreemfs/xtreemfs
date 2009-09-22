/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;

/**
 * 
 * @author bjko
 */
public class MRCRequest {
    
    private final ONCRPCRequest rpcRequest;
    
    private ErrorRecord         error;
    
    private yidl.runtime.Object         response;
    
    private Object              requestArgs;
    
    private RequestDetails      details;
    
    public MRCRequest() {
        this(null);
    }
    
    public MRCRequest(ONCRPCRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
        details = new RequestDetails();
    }
    
    public ONCRPCRequest getRPCRequest() {
        return rpcRequest;
    }
    
    public ErrorRecord getError() {
        return error;
    }
    
    public void setError(ErrorRecord error) {
        this.error = error;
    }
    
    public yidl.runtime.Object getResponse() {
        return response;
    }
    
    public void setResponse(yidl.runtime.Object response) {
        this.response = response;
    }
    
    public Object getRequestArgs() {
        return requestArgs;
    }
    
    public void setRequestArgs(Object requestArgs) {
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
