/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;

/**
 * Writes an object to disk without sending GMax-messages. 
 *
 * 01.04.2009
 */
public class EventInsertPaddingObject extends OSDOperation {

    public EventInsertPaddingObject(OSDRequestDispatcher master) {
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
        final XLocations xloc = (XLocations) args[2];
        final int size = (Integer) args[3];

        master.objectReplicated();

        master.getStorageStage().insertPaddingObject(fileId, objectNo, xloc.getLocalReplica().getStripingPolicy(), size, 
                new Callback() {
                
                @Override
                public boolean success(Object result) {
                    
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