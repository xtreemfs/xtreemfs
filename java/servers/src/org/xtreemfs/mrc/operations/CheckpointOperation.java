/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class CheckpointOperation extends MRCOperation {
    
    public CheckpointOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            // check password to ensure that user is authorized
            if (master.getConfig().getAdminPassword().length() > 0
                && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "invalid password");
            
            master.getVolumeManager().checkpointDB();
            
            // set the response
            rq.setResponse(emptyResponse.getDefaultInstance());
            finishRequest(rq);
            
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                "an error has occurred", exc));
        }
    }
    
}
