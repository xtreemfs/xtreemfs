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
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;



/**
 *
 * @author bjko
 */
public class XLocations {

    private final XLocSet xloc;

    private Replica localReplica;

    private List<Replica> replicas;

    public XLocations(XLocSet xloc, ServiceUUID localOSD) throws InvalidXLocationsException {
        this.xloc = xloc;
        replicas = new ArrayList<Replica>(xloc.getReplicasCount());
        for (org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica r : xloc.getReplicasList()) {
            replicas.add(new Replica(r, localOSD));
        }
        if (localOSD != null) {
            for (Replica r : replicas) {
                if (r.getOSDs().contains(localOSD)) {
                    localReplica = r;
                    break;
                }
            }
            if (localReplica == null)
                throw new InvalidXLocationsException("local OSD (" + localOSD + ") is not in any replica in XLocations list: " + xloc);
        }
    }
   

    public XLocSet getXLocSet() {
        return xloc;
    }

    public int getVersion() {
        return xloc.getVersion();
    }

    public int getNumReplicas() {
        return xloc.getReplicasCount();
    }

    public List<Replica> getReplicas() {
        return replicas;
    }

    public String getReplicaUpdatePolicy() {
        return xloc.getReplicaUpdatePolicy();
    }

    public Replica getLocalReplica() {
        return localReplica;
    }

    public Replica getReplica(int replicaNo) {
        return replicas.get(replicaNo);
    }

    /**
     * returns replica which contains the given OSD
     * @param osd
     * @return null, of no replica contains the given OSD, otherwise the replica
     */
    public Replica getReplica(ServiceUUID osd) {
        for (Replica replica : getReplicas())
            for (ServiceUUID osd2 : replica.getOSDs())
                if (osd.equals(osd2))
                    return replica;
        return null;
    }

    /**
     * Checks if the given OSD is already used for this file.
     * @param osd
     * @return
     */
    public boolean containsOSD(ServiceUUID osd) {
        boolean contained = false;
        for(Replica r : getReplicas()) {
            contained = r.containsOSD(osd);
            if(contained)
                break;
        }
        return contained;
    }

    /**
     * Provides a list of OSDs which are containing replicas of the given object.
     * NOTE: If the replicas use different striping policies the same object must not contain the same data.
     * @param objectNo
     * @return
     */
    public List<ServiceUUID> getOSDsForObject(long objectNo, Replica localReplica){
        List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
        if (localReplica == null) {
            for(Replica replica : replicas){
                osds.add(replica.getOSDForObject(objectNo));
            }
        } else {
            for(Replica replica : replicas){
                if (localReplica == replica)
                    continue;
                final ServiceUUID osd = replica.getOSDForObject(objectNo);
                osds.add(osd);
            }
        }
        return osds;
    }

    public List<ServiceUUID> getOSDsForObject(long objectNo) {
        return getOSDsForObject(objectNo, null);
    }
    
    @Override
    public String toString() {
        return "local replica: " + localReplica + ", other replicas: " + replicas;
    }
}
