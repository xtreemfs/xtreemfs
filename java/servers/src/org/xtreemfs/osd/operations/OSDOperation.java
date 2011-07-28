/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import com.google.protobuf.Message;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;


public abstract class OSDOperation {


    protected OSDRequestDispatcher master;


    public OSDOperation(OSDRequestDispatcher master) {
        this.master = master;
    }

    public abstract int getProcedureId();

    /**
     * called after request was parsed and operation assigned.
     * @param rq the new request
     */
    public abstract void startRequest(OSDRequest rq);

    public abstract void startInternalEvent(Object[] args);

    /**
     * Parses the request. Should also set XLocs, XCap and fileID.
     * @param rq the request
     * @return null if successful, error message otherwise
     */
    public abstract ErrorResponse parseRPCMessage(OSDRequest rq);

    public abstract boolean requiresCapability();

    public void waitForResponses(final RPCResponse[] responses, final ResponsesListener listener) {

        assert(responses.length > 0);

        final AtomicInteger count = new AtomicInteger(0);
        final RPCResponseAvailableListener l = new RPCResponseAvailableListener() {

            @Override
            public void responseAvailable(RPCResponse r) {
                if (count.incrementAndGet() == responses.length) {
                    listener.responsesAvailable();
                }
            }
        };

        for (RPCResponse r : responses) {
            r.registerListener(l);
        }

    }

    /*public void sendError(OSDRequest rq, Exception error) {
        if (error instanceof ONCRPCException) {
            rq.sendException((ONCRPCException) error);
        } else {
            rq.sendInternalServerError(error);
        }
    }*/

    public static interface ResponsesListener {

        public void responsesAvailable();

    }

}
