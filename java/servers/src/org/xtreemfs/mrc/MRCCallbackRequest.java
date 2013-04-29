/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc;

import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.mrc.stages.InternalCallbackInterface;

public class MRCCallbackRequest extends MRCRequest {

    private final InternalCallbackInterface callback;

    public MRCCallbackRequest(InternalCallbackInterface callback) {
        this(null, callback);
    }

    public MRCCallbackRequest(RPCServerRequest rpcRequest, InternalCallbackInterface callback) {
        super(rpcRequest);
        this.callback = callback;
    }

    public InternalCallbackInterface getCallback() {
        return callback;
    }

}
