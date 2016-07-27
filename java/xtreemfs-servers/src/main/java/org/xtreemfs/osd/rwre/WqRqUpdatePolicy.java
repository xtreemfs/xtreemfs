/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public class WqRqUpdatePolicy extends CoordinatedReplicaUpdatePolicy {

    final int numResponses;

    public WqRqUpdatePolicy(List<ServiceUUID> remoteOSDUUIDs, String localUUID, String fileId, OSDServiceClient client) {
        super(remoteOSDUUIDs, localUUID, fileId, client);
        this.numResponses = (int) Math.ceil((double)(remoteOSDUUIDs.size())/ 2.0);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"remote majority (excluding local replica) for %s is %d",fileId,numResponses);
    }

    @Override
    public int getNumRequiredAcks(Operation operation) {
        return numResponses;
    }

    @Override
    public boolean backupCanRead() {
        return false;
    }

}
