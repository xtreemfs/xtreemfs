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
import org.xtreemfs.osd.RedundantFileState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public class ReplicatedFileState extends RedundantFileState {


    private ReplicaUpdatePolicy        policy;

    private List<ObjectVersionMapping> objectsToFetch;

    private XLocations                 loc;


    public ReplicatedFileState(String fileId, XLocations locations, ServiceUUID localUUID,
                               OSDServiceClient client) throws UnknownUUIDException, IOException {
        super(fileId);
        this.loc = locations;

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
     * @return the sPolicy
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
     * @return the XLocations
     */
    public XLocations getLocations() {
        return loc;
    }

}
