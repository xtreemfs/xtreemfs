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
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.ac.FileAccessManager;
import org.xtreemfs.new_mrc.dbaccess.AtomicDBUpdate;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class DeleteOperation extends MRCOperation {
    
    static class Args {
        public String path;
    }
    
    public static final String RPC_NAME = "delete";
    
    public DeleteOperation(MRCRequestDispatcher master) {
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
            final FileAccessManager faMan = master.getFileAccessManager();
            
            final Path p = new Path(rqArgs.path);
            
            final VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
            final StorageManager sMan = vMan.getStorageManager(volume.getId());
            final PathResolver res = new PathResolver(sMan, p);
            
            // check whether the path prefix is searchable
            faMan.checkSearchPermission(volume.getId(), res.getPathPrefix(),
                rq.getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the parent directory grants write access
            faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(), res
                    .getParentDirId(), 0, rq.getDetails().userId, rq.getDetails().superUser, rq
                    .getDetails().groupIds);
            
            // check whether the file/directory exists
            res.checkIfFileDoesNotExist();
            
            FileMetadata file = res.getFile();
            
            // check whether the entry itself can be deleted (this is e.g.
            // important w/ POSIX access control if the sticky bit is set)
            faMan.checkPermission(FileAccessManager.RM_MV_IN_DIR_ACCESS, volume.getId(), file
                    .getId(), res.getParentDirId(), rq.getDetails().userId,
                rq.getDetails().superUser, rq.getDetails().groupIds);
            
            if (file.isDirectory() && sMan.getChildren(file.getId()).hasNext())
                throw new UserException(ErrNo.ENOTEMPTY, "'" + p + "' is not empty");
            
            HTTPHeaders xCapHeaders = null;
            
            // unless the file is a directory, retrieve X-headers for file
            // deletion on OSDs; if the request was authorized before,
            // assume that a capability has been issued already.
            if (!file.isDirectory()) {
                
                // obtain a deletion capability for the file
                String aMode = faMan.translateAccessMode(volume.getId(),
                    FileAccessManager.DELETE_ACCESS);
                String capability = MRCOpHelper.createCapability(aMode, volume.getId(),
                    file.getId(), Integer.MAX_VALUE, master.getConfig().getCapabilitySecret())
                        .toString();
                
                // set the XCapability and XLocationsList headers
                xCapHeaders = MRCOpHelper.createXCapHeaders(capability, file.getXLocList());
            }
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            // unlink the file; if there are still links to the file, reset the
            // X-headers to null, as the file content must not be deleted
            sMan.delete(res.getParentDirId(), res.getFileName(), update);
            if (file.getLinkCount() > 1)
                xCapHeaders = null;
            
            rq.setAdditionalResponseHeaders(xCapHeaders);
            
            // update POSIX timestamps of parent directory
            MRCOpHelper.updateFileTimes(res.getParentsParentId(), res.getParentDir(), false, true,
                true, sMan, update);
            
            // FIXME: this line is needed due to a BUG in the client which
            // expects some useless return value
            rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(null).getBytes()));
            
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
            
            args.path = (String) arguments.get(0);
            if (arguments.size() == 1)
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
