/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;

/**
 *
 * @author bjko
 */
public class WaRaUpdatePolicy extends WaR1UpdatePolicy {

    public WaRaUpdatePolicy(List<InetSocketAddress> remoteOSDs, String fileId, FleaseStage fstage, OSDClient client) throws IOException, InterruptedException {
        super(remoteOSDs,fileId,fstage, client);
    }

    public void prepareOperation(long objNo, long objVersion, Operation operation,final PrepareOperationCallback callback) {
        if (operation == Operation.READ) {
            callback.prepareOperationComplete(true, null, objVersion, null);
        } else {
            super.prepareOperation(objNo, objVersion, operation, callback);
        }
    }
}
