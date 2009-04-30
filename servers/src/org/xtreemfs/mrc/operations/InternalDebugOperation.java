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

import org.xtreemfs.interfaces.MRCInterface.xtreemfs_internal_debugRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_internal_debugResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;

/**
 * 
 * @author bjko
 */
public class InternalDebugOperation extends MRCOperation {
    
    public static final int OP_ID = 100;
    
    public InternalDebugOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_internal_debugRequest rqArgs = (xtreemfs_internal_debugRequest) rq.getRequestArgs();
        
        if (rqArgs.getCmd().equals("shutdown_babudb")) {
            master.getVolumeManager().checkpointDB();
            
            master.getVolumeManager().shutdown();
            
            // set the response
            rq.setResponse(new xtreemfs_internal_debugResponse("ok"));
        } else if (rqArgs.getCmd().equals("startup_babudb")) {
            master.getVolumeManager().init();
            rq.setResponse(new xtreemfs_internal_debugResponse("ok"));
        } else {
            rq.setResponse(new xtreemfs_internal_debugResponse("unknown command"));
        }
        finishRequest(rq);
        
    }
    
}
