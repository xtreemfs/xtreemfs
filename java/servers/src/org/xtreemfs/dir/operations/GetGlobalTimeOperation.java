/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.globalTimeSGetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

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
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) {

        callback.success(globalTimeSGetResponse.newBuilder().setTimeInSeconds(System.currentTimeMillis()).build());
    }
}
