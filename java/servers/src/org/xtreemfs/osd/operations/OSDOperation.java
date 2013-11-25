/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

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

    /**
     * By default the view contained in the location list stored in a {@link OSDRequest} will be validated (if it is
     * present). If it is required an operation can be allowed to run, even if the view is outdated or the replica is
     * invalidated. The view validation will be bypassed by returning true.
     * 
     * @return false [default]
     */
    public boolean bypassViewValidation() {
        return false;
    }

    public void waitForResponses(final RPCResponse[] responses, final ResponsesListener listener) {

        assert(responses.length > 0);

        final AtomicInteger count = new AtomicInteger(0);
        
        final AtomicReferenceArray<StackTraceElement[]> stackTracePerRequest = new AtomicReferenceArray<StackTraceElement[]>(responses.length);
        
        final RPCResponseAvailableListener l = new RPCResponseAvailableListener() {

            @Override
            public void responseAvailable(RPCResponse r) {
                // TODO(mberlin): Remove this once the cause for possible duplicate calls of responseAvailable() is found.
                for (int i = 0; i < responses.length; i++) {
                    if (responses[i] == r) {
                        if (!stackTracePerRequest.compareAndSet(i, null, Thread.currentThread().getStackTrace())) {
                            StackTraceElement[] previousStackTrace = stackTracePerRequest.get(i);
                            
                            StringBuffer strace = new StringBuffer();
                            for (int i1 = previousStackTrace.length-1; i1 >= 0; i1--) {
                                strace.append("\t");
                                strace.append(previousStackTrace[i1].toString());
                            }
                            throw new RuntimeException("responseAvailable() was already called here:\n"+strace.toString());
                        }
                        
                        break;
                    }
                }
                
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
