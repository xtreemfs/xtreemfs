/*
 * Copyright (c) 2009-2011 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * Writes an object to disk without sending GMax-messages. 
 *
 * 01.04.2009
 */
public class EventWriteObject extends OSDOperation {

    public EventWriteObject(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ErrorResponse startRequest(OSDRequest rq, RPCRequestCallback callback) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean requiresCapability() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startInternalEvent(Object[] args) {
        final String fileId = (String) args[0];
        final long objectNo = (Long) args[1];
        final ReusableBuffer data = (ReusableBuffer) args[2];
        final XLocations xloc = (XLocations) args[3];
        final CowPolicy cow = (CowPolicy) args[4];

        master.objectReplicated();
        master.replicatedDataReceived(data.capacity());

        master.getStorageStage().writeObjectWithoutGMax(fileId, objectNo,
            xloc.getLocalReplica().getStripingPolicy(), 0, data, cow, xloc, false, null, new Callback() {

            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                    
                triggerReplication(fileId);
                return true;
            }
                
            @Override
            public void failed(Throwable error) {
                
                Logging.logMessage(Logging.LEVEL_ERROR, this, "exception in internal event: %s", 
                        error.getMessage());
            }
        });
    }
    
    public void triggerReplication(String fileId) {
        
        // cancel replication of file
        master.getReplicationStage().triggerReplicationForFile(fileId);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
