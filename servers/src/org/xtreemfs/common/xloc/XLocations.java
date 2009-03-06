/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.xloc;

import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.interfaces.XLocSet;


/**
 *
 * @author bjko
 */
public class XLocations {

    private final XLocSet xloc;

    private Replica localReplica;

    private List<Replica> replicas;

    public XLocations(XLocSet xloc) {
        this.xloc = xloc;
        replicas = new ArrayList(xloc.getReplicas().size());
        for (org.xtreemfs.interfaces.Replica r : xloc.getReplicas()) {
            replicas.add(new Replica(r));
        }
    }

    public XLocations(XLocSet xloc, ServiceUUID localOSD) throws InvalidXLocationsException {
        this(xloc);
        for (Replica r : replicas) {
            if (r.getOSDs().contains(localOSD)) {
                localReplica = r;
                break;
            }
        }
        if (localReplica == null)
            throw new InvalidXLocationsException("local OSD is not in any replica in XLocations list");
    }

    public XLocSet getXLocSet() {
        return xloc;
    }

    public int getVersion() {
        return xloc.getVersion();
    }

    public int getNumReplicas() {
        return xloc.getReplicas().size();
    }

    public List<Replica> getReplicas() {
        return replicas;
    }

    public String getReplicaUpdatePolicy() {
        return xloc.getRepUpdatePolicy();
    }

    public Replica getLocalReplica() {
        return localReplica;
    }

    public Replica getReplica(int replicaNo) {
        return replicas.get(replicaNo);
    }


}
