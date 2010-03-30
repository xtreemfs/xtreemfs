/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;

/**
 *
 * @author bjko
 */
public class WaR1UpdatePolicy extends CoordinatedReplicaUpdatePolicy {

    final int numResponses;

    public WaR1UpdatePolicy(List<InetSocketAddress> remoteOSDs, String fileId, OSDClient client) throws IOException {
        super(remoteOSDs, fileId, client);
        this.numResponses = remoteOSDs.size();
    }

    @Override
    protected int getNumRequiredAcks(Operation operation) {
        return numResponses;
    }

    @Override
    protected boolean backupCanRead() {
        return false;
    }

}
