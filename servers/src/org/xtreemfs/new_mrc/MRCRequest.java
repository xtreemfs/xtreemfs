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

package org.xtreemfs.new_mrc;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;

/**
 * 
 * @author bjko
 */
public class MRCRequest {
    
    private final PinkyRequest  pinkyRequest;
    
    private ErrorRecord         error;
    
    private ReusableBuffer      data;
    
    private HTTPUtils.DATA_TYPE dataType;
    
    private Object              requestArgs;
    
    private String              requestId;
    
    private RequestDetails      details;
    
    private HTTPHeaders         additionalResponseHeaders;
    
    public MRCRequest() {
        this(null);
    }
    
    public MRCRequest(PinkyRequest request) {
        pinkyRequest = request;
        details = new RequestDetails();
    }
    
    public PinkyRequest getPinkyRequest() {
        return pinkyRequest;
    }
    
    public ErrorRecord getError() {
        return error;
    }
    
    public void setError(ErrorRecord error) {
        this.error = error;
    }
    
    public ReusableBuffer getData() {
        return data;
    }
    
    public void setData(ReusableBuffer data) {
        this.data = data;
    }
    
    public HTTPUtils.DATA_TYPE getDataType() {
        return dataType;
    }
    
    public void setDataType(HTTPUtils.DATA_TYPE dataType) {
        this.dataType = dataType;
    }
    
    public Object getRequestArgs() {
        return requestArgs;
    }
    
    public void setRequestArgs(Object requestArgs) {
        this.requestArgs = requestArgs;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public RequestDetails getDetails() {
        return details;
    }
    
    public void setDetails(RequestDetails details) {
        this.details = details;
    }
    
    public HTTPHeaders getAdditionalResponseHeaders() {
        return additionalResponseHeaders;
    }
    
    public void setAdditionalResponseHeaders(HTTPHeaders additionalResponseHeaders) {
        this.additionalResponseHeaders = additionalResponseHeaders;
    }
    
}
