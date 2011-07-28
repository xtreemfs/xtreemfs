/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public class WaRaUpdatePolicy extends CoordinatedReplicaUpdatePolicy {

    final int numResponses;

    public WaRaUpdatePolicy(List<ServiceUUID> remoteOSDUUIDs, String localUUID, String fileId, OSDServiceClient client) throws IOException {
        super(remoteOSDUUIDs, localUUID, fileId, client);
        this.numResponses = remoteOSDUUIDs.size() - 1;
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
