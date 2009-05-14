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

import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_check_file_existsRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_check_file_existsResponse;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.volumes.VolumeManager;

/**
 * 
 * @author stender
 */
public class CheckFileListOperation extends MRCOperation {
    
    public static final int OP_ID = 23;
    
    public CheckFileListOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_check_file_existsRequest rqArgs = (xtreemfs_check_file_existsRequest) rq
                .getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        StorageManager sMan = vMan.getStorageManager(rqArgs.getVolume_id());
        
        String response = sMan == null ? "2" : "";
        if (sMan != null)
            try {
                if (rqArgs.getFile_ids().size() == 0)
                    throw new UserException(ErrNo.EINVAL, "fileList was empty!");
                for (String fileId : rqArgs.getFile_ids()) {
                    if (fileId == null)
                        throw new MRCException("file ID was null!");
                    response += sMan.getMetadata(Long.parseLong(fileId)) != null ? "1" : "0";
                }
            } catch (UserException ue) {
                response = "2";
            } catch (MRCException be) {
                throw new MRCException("checkFileList caused an Exception: " + be.getMessage());
            }
        
        // set the response
        rq.setResponse(new xtreemfs_check_file_existsResponse(response));
        finishRequest(rq);
    }
    
}
