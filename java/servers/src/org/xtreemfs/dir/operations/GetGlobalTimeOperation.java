/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.globalTimeSGetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;
/**
 *
 * @author bjko
 */
public class GetGlobalTimeOperation extends DIROperation {


    public GetGlobalTimeOperation(DIRRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_GLOBAL_TIME_S_GET;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        requestFinished(null, rq);
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    protected Message getRequestMessagePrototype() {
        return emptyRequest.getDefaultInstance();
    }

    /*
     * (non-Javadoc)
     * @see org.xtreemfs.dir.operations.DIROperation#requestFinished(java.lang.Object, org.xtreemfs.dir.DIRRequest)
     */
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        globalTimeSGetResponse resp = globalTimeSGetResponse.newBuilder().setTimeInSeconds(System.currentTimeMillis()).build();
        rq.sendSuccess(resp);
    }
}
