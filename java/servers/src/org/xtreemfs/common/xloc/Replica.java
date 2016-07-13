/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.xloc;

import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;

/**
 *
 * @author bjko
 */
public class Replica {

    List<ServiceUUID>  osds;

    StripingPolicyImpl stripingPolicy;

    ServiceUUID localOSD;

    org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica replica;

    public Replica(org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica replica, ServiceUUID localOSD) {
        this.replica = replica;
        this.localOSD = localOSD;
    }

    public StripingPolicyImpl getStripingPolicy() {
        if (stripingPolicy == null) {
            int relOsdPosition = (localOSD == null) ? -1 : getOSDs().indexOf(localOSD);
            stripingPolicy = StripingPolicyImpl.getPolicy(replica,relOsdPosition);
        }
        return stripingPolicy;
    }

    public List<ServiceUUID> getOSDs() {
        if (osds == null) {
            osds = new ArrayList<ServiceUUID>(replica.getOsdUuidsCount());
            for (String osd : replica.getOsdUuidsList()) {
                ServiceUUID uuid = new ServiceUUID(osd);
                osds.add(uuid);
            }
        }
        return osds;
    }

    public boolean isStriped() {
        return getStripingPolicy().getWidth() > 1;
    }

    public ServiceUUID getHeadOsd() {
        return new ServiceUUID(replica.getOsdUuids(0));
    }

    public String toString() {
        return "Replica " + stripingPolicy + ", " + replica + ", Flags: " + replica.getReplicationFlags()
                + ", OSD: " + osds;
    }

    /**
     * Provides the responsible OSD for this object.
     * 
     * @param objectNo
     * @return
     */
    public ServiceUUID getOSDForObject(long objectNo) {
        return getOSDs().get(getStripingPolicy().getOSDforObject(objectNo));
    }
    
    /**
     * Provides the responsible OSD for this offset.
     * 
     * @param objectID
     * @return
     */
    public ServiceUUID getOSDForOffset(long offset) {
        return getOSDs().get(getStripingPolicy().getOSDforOffset(offset));
    }

    /**
     * Returns the OSD at the given position in the stripe.
     */
    public ServiceUUID getOSDByPos(int osdIdx) {
        return getOSDs().get(osdIdx);
    }

    /**
     * Checks if the given OSD is part of this Replica.
     * @param osd
     * @return
     */
    public boolean containsOSD(ServiceUUID osd) {
        boolean contained = false;
        for(ServiceUUID o : getOSDs()) {
            contained = osd.equals(o);
            if(contained)
                break;
        }
        return contained;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Replica) {
            return this.toString().equals(((Replica) obj).toString());
        } else
            return false;
    }

    /**
     * Returns the replication flags. These could be used with the <code>ReplicationFlags</code> methods to identify which
     * strategy is set.
     * 
     * @return
     */
    public int getTransferStrategyFlags() {
        return replica.getReplicationFlags();
    }

    /**
     * checks if this replica is complete, so it contains ALL objects of the file
     * 
     * @return
     */
    public boolean isComplete() {
        return ReplicationFlags.isReplicaComplete(replica.getReplicationFlags());
    }

    /**
     * Resets the complete Flag and restores the strategy flag.
     * 
     */
    public void resetCompleteFlagAndRestoreStrategyFlag() {
    	int replFlags = replica.getReplicationFlags();
    	
        // Reset complete Flag
    	replFlags &= ~REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber();
    	
        // Restore strategy flag from replication flags
        if (isPartialReplica()) {
            // Assumption: partial replica -> sequential prefetching
            replFlags = ReplicationFlags.setSequentialPrefetchingStrategy(replFlags);
        } else {
            // Assumption: full replica -> rarest first strategy
            replFlags = ReplicationFlags.setRarestFirstStrategy(replFlags);
        }
    	
    	//set new replication flags
    	replica = replica.toBuilder().setReplicationFlags(replFlags).build();
    }

    /**
     * checks if this replica is a partial (ondemand) or full replica
     * @return true, if partial; false if full
     */
    public boolean isPartialReplica() {
        return ReplicationFlags.isPartialReplica(replica.getReplicationFlags());
    }
}
