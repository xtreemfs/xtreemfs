/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;

import com.google.protobuf.Message;


public abstract class OSDOperation {

    protected OSDRequestDispatcher master;
    
    public OSDOperation(OSDRequestDispatcher master) {
        this.master = master;
    }

    public abstract int getProcedureId();

    /**
     * called after request was parsed and operation assigned.
     * @param rq the new request
     * 
     * @return {@link ErrorResponse} if request could not be processed, null otherwise.
     */
    public abstract ErrorResponse startRequest(OSDRequest rq, RPCRequestCallback callback);

    public abstract void startInternalEvent(Object[] args);

    /**
     * Parses the request. Should also set XLocs, XCap and fileID.
     * @param rq the request
     * @return null if successful, error message otherwise
     */
    public abstract ErrorResponse parseRPCMessage(OSDRequest rq);

    public abstract boolean requiresCapability();

    public final static void waitForResponses(final RPCResponse<Message>[] responses, 
            final ResponsesListener listener) {

        assert(responses.length > 0);

        final AtomicInteger count = new AtomicInteger(0);
        final RPCResponseAvailableListener<Message> l = new RPCResponseAvailableListener<Message>() {

            @Override
            public void responseAvailable(RPCResponse<Message> r) {
                if (count.incrementAndGet() == responses.length) {
                    listener.responsesAvailable();
                }
            }
        };

        for (RPCResponse<Message> r : responses) {
            r.registerListener(l);
        }
    }

    public static interface ResponsesListener {

        public void responsesAvailable();

    }
}