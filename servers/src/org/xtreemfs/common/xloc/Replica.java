/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.xloc;

import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.common.uuids.ServiceUUID;

/**
 *
 * @author bjko
 */
public class Replica {

    List<ServiceUUID>  osds;

    StripingPolicyImpl stripingPolicy;

    org.xtreemfs.interfaces.Replica replica;

    public Replica(org.xtreemfs.interfaces.Replica replica) {
        this.replica = replica;
    }

    public StripingPolicyImpl getStripingPolicy() {
        if (stripingPolicy == null)
            stripingPolicy = StripingPolicyImpl.getPolicy(replica);
        return stripingPolicy;
    }

    public List<ServiceUUID> getOSDs() {
        if (osds == null) {
            osds = new ArrayList(replica.getOsd_uuids().size());
            for (String osd : replica.getOsd_uuids()) {
                ServiceUUID uuid = new ServiceUUID(osd);
                osds.add(uuid);
            }
        }
        return osds;
    }

    public int getReplicationFlags() {
        return replica.getReplication_flags();
    }

    public boolean isStriped() {
        return this.osds.size() > 1;
    }

    public boolean isHeadOsd(ServiceUUID localOSD) {
        return this.osds.get(0).equals(localOSD);
    }

    public String toString() {
        return "Replica "+stripingPolicy+", "+replica+" "+
                "OSD: "+osds;
    }

}
