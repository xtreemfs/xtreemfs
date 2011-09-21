/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;

/**
 * 
 * @author bjko
 */
public class ShutdownOperation extends MRCOperation {
        
    public ShutdownOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq, RPCRequestCallback callback) throws Exception {
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword().length() > 0
            && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "invalid password");
        
        callback.success(emptyResponse.getDefaultInstance());
        
        master.asyncShutdown();
    }
}