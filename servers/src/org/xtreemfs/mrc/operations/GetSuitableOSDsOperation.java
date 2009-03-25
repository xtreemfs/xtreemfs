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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_get_suitable_osdsRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_get_suitable_osdsResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;

/**
 * 
 * @author stender
 */
public class GetSuitableOSDsOperation extends MRCOperation {
    
    public static final int OP_ID = 24;
    
    public GetSuitableOSDsOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            final xtreemfs_get_suitable_osdsRequest rqArgs = (xtreemfs_get_suitable_osdsRequest) rq
                    .getRequestArgs();
            
            String volumeId = null;
            try {
                String globalFileId = rqArgs.getFile_id();
                int i = globalFileId.indexOf(':');
                volumeId = globalFileId.substring(0, i);
            } catch (Exception exc) {
                throw new UserException("invalid global file ID: " + rqArgs.getFile_id()
                    + "; expected pattern: <volume_ID>:<local_file_ID>");
            }
            
            ServiceSet usableOSDs = master.getOSDStatusManager().getUsableOSDs(volumeId);
            StringSet uuids = new StringSet();
            for (int i = 0; i < usableOSDs.calculateSize(); i++)
                uuids.add(usableOSDs.get(i).getUuid());
            
            // set the response
            rq.setResponse(new xtreemfs_get_suitable_osdsResponse(uuids));
            finishRequest(rq);
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        }
    }
    
}
