/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.client;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;


/**
 *
 * @author bjko
 */
public class RPCRequest {

    private int       rpcTransId;

    private ReusableBuffer  requestPayload;

    private ReusableBuffer  requestHeaders;

    private ReusableBuffer  responsePayload;

    private ONCRPCResponseHeader  responseHeaders;

    private final int       programId;

    private final int       operationId;

    private final int       versionNumber;

    private final RPCResponseListener listener;

    private long            timeQueued;

    public RPCRequest(RPCResponseListener listener, ReusableBuffer requestPayload,
            int programId, int operationId, int versionNumber) {
        this.requestPayload = requestPayload;
        this.programId = programId;
        this.operationId = operationId;
        this.versionNumber = versionNumber;
        this.listener = listener;
        
        assembleHeader();
    }

    void sent() {
        this.timeQueued = TimeSync.getLocalSystemTime();
    }
    
    long getTimeQueued() {
        return this.timeQueued;
    }

    RPCResponseListener getListener() {
        return listener;
    }

    private void assembleHeader() {
        ONCRPCRequestHeader hdr = new ONCRPCRequestHeader();
        //FIXME
    }

    public void freeBuffers() {
        BufferPool.free(requestPayload);
        requestPayload = null;
        BufferPool.free(requestHeaders);
        requestHeaders = null;
        BufferPool.free(responsePayload);
        responsePayload = null;
    }

    /**
     * @return the rpcTransId
     */
    public int getRpcTransId() {
        return rpcTransId;
    }

    /**
     * @return the requestPayload
     */
    public ReusableBuffer getRequestPayload() {
        return requestPayload;
    }

    /**
     * @return the programId
     */
    public int getProgramId() {
        return programId;
    }

    /**
     * @return the operationId
     */
    public int getOperationId() {
        return operationId;
    }

    /**
     * @return the versionNumber
     */
    public int getVersionNumber() {
        return versionNumber;
    }

    /**
     * @param requestPayload the requestPayload to set
     */
    void setRequestPayload(ReusableBuffer requestPayload) {
        this.requestPayload = requestPayload;
    }

    /**
     * @return the requestHeaders
     */
    ReusableBuffer getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * @param requestHeaders the requestHeaders to set
     */
    void setRequestHeaders(ReusableBuffer requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    /**
     * @return the responsePayload
     */
    public ReusableBuffer getResponsePayload() {
        return responsePayload;
    }

    /**
     * @param responsePayload the responsePayload to set
     */
    void setResponsePayload(ReusableBuffer responsePayload) {
        this.responsePayload = responsePayload;
    }

    /**
     * @return the responseHeaders
     */
    ONCRPCResponseHeader getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * @param responseHeaders the responseHeaders to set
     */
    void setResponseHeaders(ONCRPCResponseHeader responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * @param rpcTransId the rpcTransId to set
     */
    void setRpcTransId(int rpcTransId) {
        this.rpcTransId = rpcTransId;
    }
    

}
