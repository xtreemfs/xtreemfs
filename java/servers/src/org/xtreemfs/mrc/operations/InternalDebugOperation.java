/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.util.concurrent.atomic.AtomicBoolean;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.stringMessage;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class InternalDebugOperation extends MRCOperation {
    
    private Thread              bgrChkptr;
    
    private final AtomicBoolean asyncChkptRunning;
    
    public InternalDebugOperation(MRCRequestDispatcher master) {
        super(master);
        asyncChkptRunning = new AtomicBoolean(false);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final stringMessage rqArgs = (stringMessage) rq.getRequestArgs();
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword().length() > 0
            && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "invalid password");
        
        if (rqArgs.getAString().equals("shutdown_babudb")) {
            master.getVolumeManager().checkpointDB();
            master.getVolumeManager().shutdown();
            
            // set the response
            rq.setResponse(stringMessage.newBuilder().setAString("ok").build());
        } else if (rqArgs.getAString().equals("startup_babudb")) {
            master.getVolumeManager().init();
            rq.setResponse(stringMessage.newBuilder().setAString("ok").build());
        } else if (rqArgs.getAString().equals("async_checkpoint")) {
            Runnable asynChkpt = new Runnable() {
                
                @Override
                public void run() {
                    try {
                        asyncChkptRunning.set(true);
                        final long tStart = System.currentTimeMillis();
                        master.getVolumeManager().checkpointDB();
                        final long tEnd = System.currentTimeMillis();
                        asyncChkptRunning.set(false);
                        Logging.logMessage(Logging.LEVEL_INFO, this, "checkpoint took " + (tEnd - tStart)
                            + " ms ");
                    } catch (DatabaseException ex) {
                        Logging.logError(Logging.LEVEL_ERROR, this, ex);
                    }
                }
            };
            bgrChkptr = new Thread(asynChkpt);
            bgrChkptr.start();
            rq.setResponse(stringMessage.newBuilder().setAString("ok").build());
        } else if (rqArgs.getAString().equals("checkpoint_done")) {
            if (asyncChkptRunning.get() == false)
                rq.setResponse(stringMessage.newBuilder().setAString("yes").build());
            else
                rq.setResponse(stringMessage.newBuilder().setAString("no").build());
        } else {
            rq.setResponse(stringMessage.newBuilder().setAString("unknown command").build());
        }
        finishRequest(rq);
        
    }
    
}
