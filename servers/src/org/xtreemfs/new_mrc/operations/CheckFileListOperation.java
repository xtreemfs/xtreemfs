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

package org.xtreemfs.new_mrc.operations;

import java.util.List;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.mrc.brain.BrainException;
import org.xtreemfs.mrc.brain.storage.BackendException;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.volumes.VolumeManager;

/**
 * 
 * @author stender
 */
public class CheckFileListOperation extends MRCOperation {
    
    static class Args {
        
        public String       volumeID;
        
        public List<String> fileIDs;
        
    }
    
    public static final String RPC_NAME = "checkFileList";
    
    public CheckFileListOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public boolean hasArguments() {
        return true;
    }
    
    @Override
    public boolean isAuthRequired() {
        return true;
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            Args rqArgs = (Args) rq.getRequestArgs();
            
            final VolumeManager vMan = master.getVolumeManager();
            StorageManager sMan = vMan.getStorageManager(rqArgs.volumeID);
            
            String response = sMan == null ? "2" : "";
            if (sMan != null)
                try {
                    if (rqArgs.fileIDs == null || rqArgs.fileIDs.size() == 0)
                        throw new UserException("fileList was empty!");
                    for (String fileId : rqArgs.fileIDs) {
                        if (fileId == null)
                            throw new BackendException("file ID was null!");
                        response += sMan.getMetadata(Long.parseLong(fileId)) != null ? "1" : "0";
                    }
                } catch (UserException ue) {
                    response = "2";
                } catch (BackendException be) {
                    throw new BrainException("checkFileList caused an Exception: "
                        + be.getMessage());
                }
            
            rq.setData(ReusableBuffer.wrap(response.getBytes()));
            finishRequest(rq);
            
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
                "an error has occurred", exc));
        }
    }
    
    @Override
    public ErrorRecord parseRPCBody(MRCRequest rq, List<Object> arguments) {
        
        Args args = new Args();
        
        try {
            
            args.volumeID = (String) arguments.get(0);
            args.fileIDs = (List<String>) arguments.get(1);
            
            if (arguments.size() == 2)
                return null;
            
            throw new Exception();
            
        } catch (Exception exc) {
            try {
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "': " + JSONParser.writeJSON(arguments));
            } catch (JSONException je) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "'");
            }
        } finally {
            rq.setRequestArgs(args);
        }
    }
    
}
