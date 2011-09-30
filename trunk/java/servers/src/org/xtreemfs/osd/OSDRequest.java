/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import com.google.protobuf.Message;
import java.io.IOException;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * Request object.
 * 
 * @author bjko
 */
public final class OSDRequest {

    private final RPCServerRequest rpcRequest;
    private Message requestArgs;
    /**
     * Request operation which contains state machine.
     */
    private OSDOperation operation;
    private Object attachment;
    private long requestId;
    private static long rqIdCounter = 1;
    private RPCResponse[] pendingRequests;
    private String fileId;
    private Capability capability;
    private XLocations locationList;
    private CowPolicy cowPolicy;
    /**
     * true, if this is the first call to a file
     * (i.e. no entry in OFT)
     */
    private boolean fileOpen;

    public OSDRequest(RPCServerRequest request) {
        this.rpcRequest = request;
        this.requestId = rqIdCounter++;
    }

    public RPCServerRequest getRPCRequest() {
        return this.getRpcRequest();
    }

    public void sendSuccess(Message response, ReusableBuffer data) {
        try {
            rpcRequest.sendResponse(response, data);
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void sendInternalServerError(Throwable cause) {
        if (getRpcRequest() != null) {
            rpcRequest.sendError(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "internal server error:" + cause, OutputUtils.stackTraceToString(cause));
        } else {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "internal server error on internal request: %s",
                    cause.toString());
            Logging.logError(Logging.LEVEL_ERROR, this, cause);
        }
    }

    public void sendError(ErrorType type, POSIXErrno errno, String message) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending errno exception %s/%s/%s", type, errno, message);
        }
        rpcRequest.sendError(type, errno, message);
    }

    public void sendError(ErrorType type, POSIXErrno errno, String message, String debugInfo) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending errno exception %s/%s/%s", type, errno, message);
        }
        rpcRequest.sendError(type, errno, message, debugInfo);
    }


    /**
     * @return the rpcRequest
     */
    public RPCServerRequest getRpcRequest() {
        return rpcRequest;
    }

    /**
     * @return the requestArgs
     */
    public Message getRequestArgs() {
        return requestArgs;
    }

    /**
     * @param requestArgs the requestArgs to set
     */
    public void setRequestArgs(Message requestArgs) {
        this.requestArgs = requestArgs;
    }

    /**
     * @return the operation
     */
    public OSDOperation getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(OSDOperation operation) {
        this.operation = operation;
    }

    /**
     * @return the attachment
     */
    public Object getAttachment() {
        return attachment;
    }

    /**
     * @param attachment the attachment to set
     */
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    /**
     * @return the requestId
     */
    public long getRequestId() {
        return requestId;
    }

    /**
     * @return the pendingRequests
     */
    public RPCResponse[] getPendingRequests() {
        return pendingRequests;
    }

    /**
     * @param pendingRequests the pendingRequests to set
     */
    public void setPendingRequests(RPCResponse[] pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    /**
     * @return the fileId
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * @param fileId the fileId to set
     */
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    /**
     * @return the capability
     */
    public Capability getCapability() {
        return capability;
    }

    /**
     * @param capability the capability to set
     */
    public void setCapability(Capability capability) {
        this.capability = capability;
    }

    /**
     * @return the locationList
     */
    public XLocations getLocationList() {
        return locationList;
    }

    /**
     * @param locationList the locationList to set
     */
    public void setLocationList(XLocations locationList) {
        this.locationList = locationList;
    }

    /**
     * @return the cowPolicy
     */
    public CowPolicy getCowPolicy() {
        return cowPolicy;
    }

    /**
     * @param cowPolicy the cowPolicy to set
     */
    public void setCowPolicy(CowPolicy cowPolicy) {
        this.cowPolicy = cowPolicy;
    }

    /**
     * @return the fileOpen
     */
    public boolean isFileOpen() {
        return fileOpen;
    }

    /**
     * @param fileOpen the fileOpen to set
     */
    public void setFileOpen(boolean fileOpen) {
        this.fileOpen = fileOpen;
    }

    public void sendError(ErrorResponse error) {
        this.getRPCRequest().sendError(error);
    }
}
