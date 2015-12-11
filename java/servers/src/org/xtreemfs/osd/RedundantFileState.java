/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.rwre.ReplicaUpdatePolicy;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * @author Jan Fajerski
 *
 * This class provides necessary state and acompanying methods to provide master-slave replication of information based on Flease
 */
public abstract class RedundantFileState {

    public enum LocalState {
        INITIALIZING,
        OPEN,
        RESET,
        RESET_COMPLETE,
        WAITING_FOR_LEASE,
        BACKUP,
        PRIMARY,
        INVALIDATED
    }

    private boolean                     cellOpen;
    private FileCredentials             credentials;
    private final String                fileId;
    private boolean                     forceReset;
    private boolean                     invalidated;
    private boolean                     invalidatedReset;
    private Flease                      lease;
    private XLocations                  loc;
    private boolean                     localIsPrimary;
    private long                        masterEpoch;
    private List<StageRequest>          pendingRequests;
    private ReplicaUpdatePolicy         policy;
    private boolean                     primaryReset;
    private LocalState                  state;

    public RedundantFileState(String fileId, XLocations locations, ServiceUUID localUUID, OSDServiceClient client) {
        this.pendingRequests = new LinkedList<StageRequest>();
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
        this.loc = locations;

        List<ServiceUUID> remoteOSDs = new ArrayList<ServiceUUID>(locations.getNumReplicas() - 1);
        for (Replica r : locations.getReplicas()) {
            final ServiceUUID headOSD = r.getHeadOsd();
            if (headOSD.equals(localUUID))
                continue;
            remoteOSDs.add(headOSD);
        }

        policy = ReplicaUpdatePolicy.newReplicaUpdatePolicy(locations.getReplicaUpdatePolicy(), remoteOSDs, localUUID.toString(),
                fileId, client);
    }


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
     * @param error the ErrorResponse to send
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

    /**
     * @return the StripingPolicy
     */
    public StripingPolicyImpl getStripingPolicy() {
        return loc.getLocalReplica().getStripingPolicy();
    }

    public Replica getLocalReplica() {
        return loc.getLocalReplica();
    }

    public ReplicaUpdatePolicy getPolicy() {
        return this.policy;
    }

    /**
     * @return the XLocations
     */
    public XLocations getLocations() {
        return loc;
    }

}
