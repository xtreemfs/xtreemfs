/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd.operations;

import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.utils.ONCRPCException;
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
    public abstract yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception;

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

    public void sendError(OSDRequest rq, Exception error) {
        if (error instanceof ONCRPCException) {
            rq.sendException((ONCRPCException) error);
        } else {
            rq.sendInternalServerError(error);
        }
    }

    public static interface ResponsesListener {

        public void responsesAvailable();

    }

}
