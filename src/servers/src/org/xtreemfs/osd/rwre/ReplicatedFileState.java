/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.osd.client.OSDClient;

/**
 *
 * @author bjko
 */
public class ReplicatedFileState {    

    private final AtomicInteger queuedData;

    private List<InetSocketAddress> remoteOSDs;

    private final List<ReplicatedOperation> pendingWrites;

    private ReplicaUpdatePolicy   policy;

    private final String          fileId;

    public ReplicatedFileState(String fileId, XLocations locations, ServiceUUID localUUID, FleaseStage fstage, OSDClient client) throws UnknownUUIDException, IOException, InterruptedException {
        queuedData = new AtomicInteger();
        pendingWrites = new ArrayList<ReplicatedOperation>(10);
        this.fileId = fileId;
        
        remoteOSDs = new ArrayList(locations.getNumReplicas()-1);
        for (Replica r : locations.getReplicas()) {
            final ServiceUUID headOSD = r.getHeadOsd();
            if (headOSD.equals(localUUID))
                continue;
            remoteOSDs.add(headOSD.getAddress());
        }

        if (locations.getReplicaUpdatePolicy().equals("RW/WaR1")) {
            //FIXME: instantiate the right policy
            policy = new WaR1UpdatePolicy(remoteOSDs, fileId, fstage, client);
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

    public List<ReplicatedOperation> getPendingWrites() {
        return this.pendingWrites;
    }



}
