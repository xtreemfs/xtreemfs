/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;

/**
 * Writes an object to disk without sending GMax-messages. 
 *
 * 01.04.2009
 */
public class EventRWRStatus extends OSDOperation {

    public EventRWRStatus(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startRequest(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");

    }


    @Override
    public boolean requiresCapability() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startInternalEvent(Object[] args) {
        final String fileId = (String) args[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) args[1];

        master.getStorageStage().internalGetReplicaState(fileId, sp, 0, new InternalGetReplicaStateCallback() {

            @Override
            public void getReplicaStateComplete(ReplicaStatus localState, ErrorResponse error) {
                master.getRWReplicationStage().eventReplicaStateAvailable(fileId, localState, error);
            }
        });
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
