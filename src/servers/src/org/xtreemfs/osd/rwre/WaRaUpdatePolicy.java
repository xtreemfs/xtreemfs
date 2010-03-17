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
public class WaRaUpdatePolicy extends CoordinatedReplicaUpdatePolicy {

    final int numResponses;

    public WaRaUpdatePolicy(List<InetSocketAddress> remoteOSDs, String fileId, long maxObjVerOnDisk, OSDClient client) throws IOException, InterruptedException {
        super(remoteOSDs, fileId, maxObjVerOnDisk, client);
        this.numResponses = remoteOSDs.size();
    }

    @Override
    protected int getNumRequiredAcks(Operation operation) {
        return numResponses;
    }

    @Override
    protected boolean backupCanRead() {
        return true;
    }

}
