/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.operations;

import org.xtreemfs.mrc.MRCCallbackRequest;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.stages.InternalCallbackInterface;

public class InternalCallbackOperation extends MRCOperation {

    public InternalCallbackOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        if (!(rq instanceof MRCCallbackRequest)) {
            // TODO(jdillmann): check if this exception is the right one
            throw new MRCException("InternalCallbackOperation must be called with an MRCCallbackRequest");
        }

        InternalCallbackInterface callback = ((MRCCallbackRequest) rq).getCallback();
        callback.startCallback(rq);
    }

}
