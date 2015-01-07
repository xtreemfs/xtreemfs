/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.mrc.MRCRequest;

/**
 * This class is used internally to execute callbacks ({@link InternalCallbackInterface}) in the context of the
 * ProcessingStage.
 * 
 * @see ProcessingStage#enqueueInternalCallbackOperation(MRCRequest, InternalCallbackInterface)
 */
class InternalCallbackMRCRequest extends MRCRequest {

    private final InternalCallbackInterface callback;

    public InternalCallbackMRCRequest(InternalCallbackInterface callback) {
        this((RPCServerRequest) null, callback);
    }

    public InternalCallbackMRCRequest(MRCRequest rq, InternalCallbackInterface callback) {
        this(rq.getRPCRequest(), callback);
    }

    public InternalCallbackMRCRequest(RPCServerRequest rpcRequest, InternalCallbackInterface callback) {
        super(rpcRequest);
        this.callback = callback;
    }

    public InternalCallbackInterface getCallback() {
        return callback;
    }

}
