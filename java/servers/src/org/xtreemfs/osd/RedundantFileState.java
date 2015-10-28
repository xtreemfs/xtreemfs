/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.rwre.ReplicaUpdatePolicy;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Jan Fajerski
 */
public abstract class RedundantFileState {

    public enum LocalState {
        INITIALIZING,
        OPEN,
        RESET,
        WAITING_FOR_LEASE,
        BACKUP,
        PRIMARY,
        INVALIDATED
    }

    private List<StageRequest> pendingRequests;

    private LocalState state;
    private boolean                    invalidated;
    private boolean                    cellOpen;
    private final String               fileId;
    private boolean                    localIsPrimary;
    private Flease                     lease;
    private FileCredentials credentials;
    private int                        numObjectsPending;
    private boolean                    primaryReset;
    private boolean                    forceReset;
    private long                       masterEpoch;
    private boolean                    invalidatedReset;

    public RedundantFileState(String fileId) {
        this.pendingRequests = new LinkedList();
        this.state = LocalState.INITIALIZING;
        this.invalidated = false;
        this.cellOpen = false;
        this.fileId = fileId;
        this.localIsPrimary = false;
        this.lease = Flease.EMPTY_LEASE;
        this.primaryReset = false;
        this.forceReset = false;
        this.masterEpoch = FleaseMessage.IGNORE_MASTER_EPOCH;
        this.invalidatedReset = false;
    }


    public abstract XLocations getLocations();

    public abstract ReplicaUpdatePolicy getPolicy();

    public abstract StripingPolicyImpl getStripingPolicy();

    /**
     * Appends the request to the end of the list of pending requests.
     */
    public void addPendingRequest(StageRequest request) {
        pendingRequests.add(request);
    }

    /**
     * Removes all of the requests from this list of pending requests and sends them an error response, if a
     * callback of type FileOperationCallback exists.
     *
     * @param error
     *            to respond with or null
     */
    public void clearPendingRequests(ErrorResponse error) {
        if (error != null) {
            // Respond with the error, if a callback of type FileOperationCallback exists.
            for (StageRequest rq : pendingRequests) {
                Object callback = rq.getCallback();
                if (callback != null && callback instanceof FileOperationCallback) {
                    ((FileOperationCallback) callback).failed(error);
                }
            }
        }

        pendingRequests.clear();
    }

    /**
     * Retrieves and removes the head (first element) of the list of pending requests.
     *
     * @return request or null if none exists
     */
    public StageRequest removePendingRequest() {
        StageRequest req = null;
        if (hasPendingRequests()) {
            req = pendingRequests.remove(0);
        }

        return req;
    }

    /**
     * Returns true if there are pending requests.
     */
    public boolean hasPendingRequests() {
        return (!pendingRequests.isEmpty());
    }

    /**
     * Returns the number of pending requests.
     */
    public int sizeOfPendingRequests() {
        return pendingRequests.size();
    }

    /**
     * Set the files' invalidated state. Once a file has been invalidated any other requests will be rejected.
     */
    public void setInvalidated(boolean invalidated) {
        this.invalidated = invalidated;
    }

    /**
     * @return True if the file has been invalidated.
     */
    public boolean isInvalidated() {
        return invalidated;
    }

    /**
     * @return the cellOpen
     */
    public boolean isCellOpen() {
        return cellOpen;
    }

    /**
     * @param cellOpen the cellOpen to set
     */
    public void setCellOpen(boolean cellOpen) {
        this.cellOpen = cellOpen;
    }

    /**
     * @return the fileId
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * @return the localIsPrimary
     */
    public boolean isLocalIsPrimary() {
        return localIsPrimary;
    }

    /**
     * @param localIsPrimary the localIsPrimary to set
     */
    public void setLocalIsPrimary(boolean localIsPrimary) {
        this.localIsPrimary = localIsPrimary;
    }

    /**
     * @return the lease
     */
    public Flease getLease() {
        return lease;
    }

    /**
     * @param lease the lease to set
     */
    public void setLease(Flease lease) {
        this.lease = lease;
    }

    /**
     * @return the state
     */
    public LocalState getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(LocalState state) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication,
                    this,"fileId %s changed state from: %s to: %s", this.getFileId(), this.state, state);
        }
        this.state = state;
    }

    /**
     * @return the forceReset
     */
    public boolean isForceReset() {
        return forceReset;
    }

    /**
     * @param forceReset the forceReset to set
     */
    public void setForceReset(boolean forceReset) {
        this.forceReset = forceReset;
    }

    /**
     * @return the masterEpoch
     */
    public long getMasterEpoch() {
        return masterEpoch;
    }

    /**
     * @param masterEpoch the masterEpoch to set
     */
    public void setMasterEpoch(long masterEpoch) {
        this.masterEpoch = masterEpoch;
    }

    /**
     * @return the credentials
     */
    public FileCredentials getCredentials() {
        return credentials;
    }

    /**
     * @param credentials the credentials to set
     */
    public void setCredentials(FileCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * @return the numObjectsPending
     */
    public int getNumObjectsPending() {
        return numObjectsPending;
    }

    /**
     * @param numObjectsPending the numObjectsPending to set
     */
    public void setNumObjectsPending(int numObjectsPending) {
        this.numObjectsPending = numObjectsPending;
    }

    /**
     * @return the primaryReset
     */
    public boolean isPrimaryReset() {
        return primaryReset;
    }

    /**
     * @param primaryReset the primaryReset to set
     */
    public void setPrimaryReset(boolean primaryReset) {
        this.primaryReset = primaryReset;
    }

    /**
     * InvalidatedReset should be set true when the file is invalidated but a reset is forced.
     */
    public void setInvalidatedReset(boolean invalidatedReset) {
        this.invalidatedReset = invalidatedReset;
    }

    /**
     * Returns true if the file is in an forced reset.
     */
    public boolean isInvalidatedReset() {
        return invalidatedReset;
    }
}
