/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_mrc;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;

/**
 *
 * @author bjko
 */
public class MRCRequest {

    private final PinkyRequest pinkyRequest;
    
    private ErrorRecord        error;
    
    private ReusableBuffer     data;
    
    private HTTPUtils.DATA_TYPE dataType;
    
    private Object              requestArgs;
    
    private String              requestId;
            
    
    public MRCRequest() {
        this(null);
    }
    
    public MRCRequest(PinkyRequest request) {
        pinkyRequest = request;
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
    
}
