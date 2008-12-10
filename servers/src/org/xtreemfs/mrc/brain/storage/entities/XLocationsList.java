/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.brain.storage.entities;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class XLocationsList implements Serializable {

    private XLocation[] replicas;

    private long        version;

    public XLocationsList() {
    }

    public XLocationsList(XLocation[] replicas, long version) {
        this.replicas = replicas;
        this.version = version;
    }

    public XLocation[] getReplicas() {
        return replicas;
    }

    public void setReplicas(XLocation[] replicas) {
        this.replicas = replicas;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void addReplica(XLocation replica) {

        // set the new X-Locations list
        if (this.replicas != null) {
            XLocation[] replicas = new XLocation[this.replicas.length + 1];
            System.arraycopy(this.replicas, 0, replicas, 0, this.replicas.length);
            replicas[this.replicas.length] = replica;
            this.replicas = replicas;
        } else
            this.replicas = new XLocation[] { replica };

        // increment the version
        version++;
    }

    public void removeReplica(XLocation replica) {

        if (replica == null)
            return;

        List<XLocation> newRepls = new LinkedList<XLocation>();
        for (int i = 0; i < replicas.length; i++)
            if (!replicas[i].equals(replica))
                newRepls.add(replicas[i]);

        if (newRepls.size() != replicas.length) {
            replicas = newRepls.toArray(new XLocation[newRepls.size()]);
            version++;
        }
    }

    public void addReplicaWithoutVersionChange(XLocation replica) {

        // set the new X-Locations list
        if (this.replicas != null) {
            XLocation[] replicas = new XLocation[this.replicas.length + 1];
            System.arraycopy(this.replicas, 0, replicas, 0, this.replicas.length);
            replicas[this.replicas.length] = replica;
            this.replicas = replicas;
        } else
            this.replicas = new XLocation[] { replica };
    }

}
