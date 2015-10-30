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
import org.xtreemfs.osd.RedundancyStage.Operation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 * 
 * @deprecated In XtreemFS 1.3.0 the policy WaR1 was accidentally named WaRa.
 * 
 * This will be fixed in 1.3.1 and therefore the unnecessary WaRa policy is marked as deprecated.
 * It is unnecessary because there is no sense to read from all replicas if the data is always written to all replicas. Instead, it suffices to read the local replica.
 */
public class WaRaUpdatePolicy extends CoordinatedReplicaUpdatePolicy {

    final int numResponses;

    public WaRaUpdatePolicy(List<ServiceUUID> remoteOSDUUIDs, String localUUID, String fileId, OSDServiceClient client) {
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
