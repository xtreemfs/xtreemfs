/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.ObjectVersionList;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.stages.Stage.StageRequest;

/**
 *
 * @author bjko
 */
public class ReplicatedFileState {

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

    public enum ReplicaState {
        OPEN,
        RESET,
        WAITING_FOR_LEASE,
        BACKUP,
        PRIMARY
    };

    private final AtomicInteger     queuedData;

    private List<InetSocketAddress> remoteOSDs;

    private ReplicaUpdatePolicy     policy;

    private final String            fileId;

    private ReplicaState            state;

    private Queue<ObjectFetchRecord> objectsToFetch;
    
    private List<StageRequest>      pendingRequests;

    private Flease                  lease;

    private boolean                 localIsPrimary;

    private FileCredentials         credentials;

    private boolean                 cellOpen;

    private int                     numObjectsPending;

    private boolean                 primaryReset;

    private XLocations              loc;



    public ReplicatedFileState(String fileId, XLocations locations, ServiceUUID localUUID, FleaseStage fstage, OSDClient client,
            long maxObjVer) throws UnknownUUIDException, IOException, InterruptedException {
        queuedData = new AtomicInteger();
        pendingRequests = new LinkedList();
        this.fileId = fileId;
        this.state = ReplicaState.OPEN;
        this.primaryReset = false;
        this.loc = locations;
        this.lease = Flease.EMPTY_LEASE;
        
        remoteOSDs = new ArrayList(locations.getNumReplicas()-1);
        for (Replica r : locations.getReplicas()) {
            final ServiceUUID headOSD = r.getHeadOsd();
            if (headOSD.equals(localUUID))
                continue;
            remoteOSDs.add(headOSD.getAddress());
        }

        if (locations.getReplicaUpdatePolicy().equals(Constants.REPL_UPDATE_PC_WARONE)) {
            //FIXME: instantiate the right policy
            policy = new WaR1UpdatePolicy(remoteOSDs, fileId, maxObjVer, client);
        } else {
            throw new IllegalArgumentException("unsupported replica update mode: "+locations.getReplicaUpdatePolicy());
        }
    }

    public int getDataQueueLength() {
        return queuedData.get();
    }

    public ReplicaUpdatePolicy getPolicy() {
        return this.policy;
    }

    public void addPendingRequest(StageRequest request) {
        pendingRequests.add(request);
    }

    public List<StageRequest> getPendingRequests() {
        return pendingRequests;
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
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"fileId %s changed state from: %s to: %s",this.fileId,this.state, state);
        }
        this.state = state;
    }

    /**
     * @return the objectsToFetch
     */
    public Queue<ObjectFetchRecord> getObjectsToFetch() {
        return objectsToFetch;
    }

    /**
     * @param objectsToFetch the objectsToFetch to set
     */
    public void setObjectsToFetch(Queue<ObjectFetchRecord> objectsToFetch) {
        this.objectsToFetch = objectsToFetch;
    }

    /**
     * @return the primary
     */
    public Flease getLease() {
        return lease;
    }

    /**
     * @param primary the primary to set
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



}
