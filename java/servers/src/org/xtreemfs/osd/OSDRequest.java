/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.stage.AugmentedServiceRequest;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * Request object.
 * 
 * @author bjko
 */
@SuppressWarnings("rawtypes")
public final class OSDRequest extends AugmentedServiceRequest {

    /**
     * Request operation which contains state machine.
     */
    private OSDOperation  operation;
    private Object        attachment;
    private long          requestId;
    private static long   rqIdCounter           = 1;
    private RPCResponse[] pendingRequests;
    private String        fileId;
    private Capability    capability;
    private XLocations    locationList;
    private CowPolicy     cowPolicy;
    /**
     * true, if this is the first call to a file
     * (i.e. no entry in OFT)
     */
    private boolean fileOpen;

    public OSDRequest(RPCServerRequest request, int type, long deltaMaxTime, boolean highPriority) {
        super(request, type, deltaMaxTime, highPriority);
        this.requestId = rqIdCounter++;
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
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getRequestId() + " of file " + getFileId();
    }
}