/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.RedundantFileState;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public class ReplicatedFileState extends RedundantFileState {

    public enum ReplicaState {
        INITIALIZING, 
        OPEN, 
        RESET, 
        WAITING_FOR_LEASE, 
        BACKUP, 
        PRIMARY, 
        INVALIDATED
    }


    private ReplicaUpdatePolicy        policy;

    private final String               fileId;

    private ReplicaState               state;

    private List<ObjectVersionMapping> objectsToFetch;

    private Flease                     lease;

    private boolean                    localIsPrimary;

    private FileCredentials            credentials;

    private boolean                    cellOpen;

    private int                        numObjectsPending;

    private boolean                    primaryReset;

    private boolean                    forceReset;

    private XLocations                 loc;

    private long                       masterEpoch;

    private boolean                    invalidated;

    private boolean                    invalidatedReset;

    public ReplicatedFileState(String fileId, XLocations locations, ServiceUUID localUUID,
                               OSDServiceClient client) throws UnknownUUIDException, IOException {
        super();
        this.fileId = fileId;
        this.state = ReplicaState.INITIALIZING;
        this.primaryReset = false;
        this.loc = locations;
        this.lease = Flease.EMPTY_LEASE;
        this.forceReset = false;
        this.masterEpoch = FleaseMessage.IGNORE_MASTER_EPOCH;
        this.invalidated = false;
        this.invalidatedReset = false;

        List<ServiceUUID> remoteOSDs = new ArrayList(locations.getNumReplicas() - 1);
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
     * @return the fileId
     */
    public String getFileId() {
        return fileId;
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
     * @return the sPolicy
     */
    public StripingPolicyImpl getsPolicy() {
        return loc.getLocalReplica().getStripingPolicy();
    }

    public Replica getLocalReplica() {
        return loc.getLocalReplica();
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

    public ReplicaUpdatePolicy getPolicy() {
        return this.policy;
    }

    /**
     * @return the state
     */
    public ReplicaState getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(ReplicaState state) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"fileId %s changed state from: %s to: %s",this.fileId,this.state, state);
        }
        this.state = state;
    }

    /**
     * @return the objectsToFetch
     */
    public List<ObjectVersionMapping> getObjectsToFetch() {
        return objectsToFetch;
    }

    /**
     * @param objectsToFetch the objectsToFetch to set
     */
    public void setObjectsToFetch(List<ObjectVersionMapping> objectsToFetch) {
        this.objectsToFetch = objectsToFetch;
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
     * @return the XLocations
     */
    public XLocations getLocations() {
        return loc;
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
