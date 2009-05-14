/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.osd;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.interfaces.Exceptions.ProtocolException;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * Request object.
 * 
 * @author bjko
 */
public final class OSDRequest {

    private final ONCRPCRequest rpcRequest;
    
    private Serializable        requestArgs;
    
    /**
     * Request operation which contains state machine.
     */
    private OSDOperation        operation;
    
    private Object              attachment;
    
    private long                requestId;
    
    private static long         rqIdCounter = 1;
    
    private RPCResponse[]       pendingRequests;
    
    private String              fileId;
    
    private Capability          capability;
    
    private XLocations          locationList;
    
    private CowPolicy           cowPolicy;


	public OSDRequest(ONCRPCRequest request) {
		this.rpcRequest = request;
        this.requestId = rqIdCounter++;
	}

    public ONCRPCRequest getRPCRequest() {
        return this.getRpcRequest();
    }

    public void sendProtocolException(ProtocolException protocolException) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending protocol exception %s",
                protocolException.toString());
        }
        getRpcRequest().sendProtocolException(protocolException);
    }

    public void sendSuccess(Serializable response) {
        getRpcRequest().sendResponse(response);
    }

    public void sendInternalServerError(Throwable cause) {
        if (getRpcRequest() != null) {
            getRpcRequest().sendInternalServerError(cause);
        } else {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "internal server error on internal request: %s",
                cause.toString());
            Logging.logError(Logging.LEVEL_ERROR, this, cause);
        }
    }

    public void sendOSDException(int errno, String message) {
        OSDException ex = new OSDException(errno, message, "");
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending errno exception %s", ex
                    .toString());
        }
        getRpcRequest().sendGenericException(ex);
    }

    public void sendException(Exception ex) {
        if (ex instanceof ONCRPCException) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending exception %s", ex
                        .toString());
            }
            getRpcRequest().sendGenericException((ONCRPCException)ex);
        } else {
            sendInternalServerError(ex);
        }
    }

    /**
     * @return the rpcRequest
     */
    public ONCRPCRequest getRpcRequest() {
        return rpcRequest;
    }

    /**
     * @return the requestArgs
     */
    public Serializable getRequestArgs() {
        return requestArgs;
    }

    /**
     * @param requestArgs the requestArgs to set
     */
    public void setRequestArgs(Serializable requestArgs) {
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

	
}
