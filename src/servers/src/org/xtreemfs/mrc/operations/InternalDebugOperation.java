/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.mrc.operations;

import java.util.concurrent.atomic.AtomicBoolean;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_internal_debugRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_internal_debugResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.DatabaseException;

/**
 * 
 * @author bjko
 */
public class InternalDebugOperation extends MRCOperation {

    private Thread bgrChkptr;

    private final AtomicBoolean asyncChkptRunning;
    
    public InternalDebugOperation(MRCRequestDispatcher master) {
        super(master);
        asyncChkptRunning = new AtomicBoolean(false);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_internal_debugRequest rqArgs = (xtreemfs_internal_debugRequest) rq.getRequestArgs();
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword() != null
            && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(ErrNo.EPERM, "invalid password pwd="+rq.getDetails().password);
        
        if (rqArgs.getOperation().equals("shutdown_babudb")) {
            master.getVolumeManager().checkpointDB();
            
            master.getVolumeManager().shutdown();
            
            // set the response
            rq.setResponse(new xtreemfs_internal_debugResponse("ok"));
        } else if (rqArgs.getOperation().equals("startup_babudb")) {
            master.getVolumeManager().init();
            rq.setResponse(new xtreemfs_internal_debugResponse("ok"));
        } else if (rqArgs.getOperation().equals("async_checkpoint")) {
            Runnable asynChkpt = new Runnable() {

                @Override
                public void run() {
                    try {
                        asyncChkptRunning.set(true);
                        final long tStart = System.currentTimeMillis();
                        master.getVolumeManager().checkpointDB();
                        final long tEnd = System.currentTimeMillis();
                        asyncChkptRunning.set(false);
                        Logging.logMessage(Logging.LEVEL_INFO, this,"checkpoint took "+(tEnd-tStart)+" ms ");
                    } catch (DatabaseException ex) {
                        Logging.logError(Logging.LEVEL_ERROR, this, ex);
                    }
                }
            };
            bgrChkptr = new Thread(asynChkpt);
            bgrChkptr.start();
            rq.setResponse(new xtreemfs_internal_debugResponse("ok"));
        } else if (rqArgs.getOperation().equals("checkpoint_done")) {
            if (asyncChkptRunning.get() == false)
                rq.setResponse(new xtreemfs_internal_debugResponse("yes"));
            else
                rq.setResponse(new xtreemfs_internal_debugResponse("no"));
        } else {
            rq.setResponse(new xtreemfs_internal_debugResponse("unknown command"));
        }
        finishRequest(rq);
        
    }
    
}
