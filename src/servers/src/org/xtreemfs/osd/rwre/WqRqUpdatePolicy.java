/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;

/**
 *
 * @author bjko
 */
public class WqRqUpdatePolicy extends CoordinatedReplicaUpdatePolicy {

    final int numResponses;

    public WqRqUpdatePolicy(List<InetSocketAddress> remoteOSDs, String fileId, long maxObjVerOnDisk, OSDClient client) throws IOException, InterruptedException {
        super(remoteOSDs, fileId, maxObjVerOnDisk, client);
        this.numResponses = (int) Math.floor((double)(remoteOSDs.size())/ 2.0) + 1;
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"majority for %s is %d",fileId,numResponses);
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
