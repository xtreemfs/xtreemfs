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
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public class WaR1UpdatePolicy extends CoordinatedReplicaUpdatePolicy {

    final int numResponses;

    public WaR1UpdatePolicy(List<ServiceUUID> remoteOSDUUIDs, String localUUID, String fileId, OSDServiceClient client) {
        super(remoteOSDUUIDs, localUUID, fileId, client);
        this.numResponses = remoteOSDUUIDs.size();
    }

    @Override
    public int getNumRequiredAcks(Operation operation) {
        return numResponses;
    }

    @Override
    public boolean backupCanRead() {
        return true;
    }

}
