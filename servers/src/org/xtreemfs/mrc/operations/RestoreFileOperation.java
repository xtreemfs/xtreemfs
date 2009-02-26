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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.volumes.VolumeManager;

/**
 * 
 * @author stender
 */
public class RestoreFileOperation extends MRCOperation {
    
    static class Args {
        
        public String              filePath;
        
        public long                fileId;
        
        public long                fileSize;
        
        public Map<String, Object> xAttrs;
        
        public String              osd;
        
        public int                 stripeSize;
        
        public String              volumeId;
        
    }
    
    public static final String RPC_NAME = "restoreFile";
    
    public RestoreFileOperation(MRCRequestDispatcher master) {
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
            
            if (!rq.getDetails().superUser)
                throw new UserException(ErrNo.EACCES, "operation is restricted to superusers");
            
            Args rqArgs = (Args) rq.getRequestArgs();
            
            final VolumeManager vMan = master.getVolumeManager();
            final Path p = new Path(vMan.getVolumeById(rqArgs.volumeId).getName() + "/"
                + rqArgs.filePath);
            final StorageManager sMan = vMan.getStorageManager(rqArgs.volumeId);
            
            // prepare file creation in database
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            int time = (int) (TimeSync.getGlobalTime() / 1000);
            long nextFileId = sMan.getNextFileId();
            
            // create parent directories if necessary
            FileMetadata[] path = sMan.resolvePath(p);
            long parentId = 1;
            for (int i = 0; i < p.getCompCount(); i++)
                try {
                    if (path[i] != null)
                        parentId = path[i].getId();
                    
                    else {
                        sMan.createDir(nextFileId, parentId, p.getComp(i), time, time, time, rq
                                .getDetails().userId, rq.getDetails().groupIds.get(0), 509, 0,
                            update);
                        parentId = nextFileId;
                        nextFileId++;
                        
                        // set the file ID as the last one
                        sMan.setLastFileId(nextFileId, update);
                    }
                } catch (DatabaseException exc) {
                    if (exc.getType() != ExceptionType.FILE_EXISTS)
                        throw exc;
                }
            
            // create the metadata object
            FileMetadata file = sMan.createFile(rqArgs.fileId, parentId, rqArgs.volumeId + ":"
                + rqArgs.fileId, time, time, time, rq.getDetails().userId, rq.getDetails().groupIds
                    .get(0), 511, 0, rqArgs.fileSize, false, 0, 0, update);
            
            int size = (rqArgs.stripeSize < 1024 ? 1
                : (rqArgs.stripeSize % 1024 != 0) ? rqArgs.stripeSize / 1024 + 1
                    : rqArgs.stripeSize / 1024);
            
            // create and assign the new XLocList
            StripingPolicy sp = sMan.createStripingPolicy("RAID0", size, 1);
            XLoc replica = sMan.createXLoc(sp, new String[] { rqArgs.osd });
            XLocList xLocList = sMan.createXLocList(new XLoc[] { replica }, 0);
            
            file.setXLocList(xLocList);
            sMan.setMetadata(file, FileMetadata.XLOC_METADATA, update);
            
            // create the user attributes
            if (rqArgs.xAttrs != null) {
                for (Entry<String, Object> attr : rqArgs.xAttrs.entrySet())
                    sMan.setXAttr(file.getId(), rq.getDetails().userId, attr.getKey(), attr
                            .getValue().toString(), update);
            }
            
            update.execute();
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc
                    .getMessage(), exc));
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
                "an error has occurred", exc));
        }
    }
    
    @Override
    public ErrorRecord parseRPCBody(MRCRequest rq, List<Object> arguments) {
        
        Args args = new Args();
        
        try {
            
            args.filePath = (String) arguments.get(0);
            args.fileId = (Long) arguments.get(1);
            args.fileSize = (Long) arguments.get(2);
            args.xAttrs = (Map<String, Object>) arguments.get(3);
            args.osd = (String) arguments.get(4);
            args.stripeSize = ((Long) arguments.get(5)).intValue();
            args.volumeId = (String) arguments.get(6);
            if (arguments.size() == 7)
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
