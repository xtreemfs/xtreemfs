/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;

/**
 * Writes an object to disk without sending GMax-messages. 
 *
 * 01.04.2009
 * 
 * @deprecated currently unused.
 */
@Deprecated
public class EventPingFile extends OSDOperation {

    public EventPingFile(OSDRequestDispatcher master) {
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
        
        // final String fileId = (String) args[0];

        // XXX master.getPreprocStage().pingFile(fileId);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
