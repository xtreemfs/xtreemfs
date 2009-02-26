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

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class CreateLinkOperation extends MRCOperation {
    
    static class Args {
        
        public String linkPath;
        
        public String targetPath;
        
    }
    
    public static final String RPC_NAME = "createLink";
    
    public CreateLinkOperation(MRCRequestDispatcher master) {
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
            
            final Path lp = new Path(rqArgs.linkPath);
            final Path tp = new Path(rqArgs.targetPath);
            
            if (!lp.getComp(0).equals(tp.getComp(0)))
                throw new UserException(ErrNo.EXDEV,
                    "cannot create hard links across volume boundaries");
            
            final VolumeInfo volume = vMan.getVolumeByName(lp.getComp(0));
            final StorageManager sMan = vMan.getStorageManager(volume.getId());
            final PathResolver lRes = new PathResolver(sMan, lp);
            final PathResolver tRes = new PathResolver(sMan, tp);
            
            // check whether the link's path prefix is searchable
            faMan.checkSearchPermission(sMan, lRes, rq.getDetails().userId, rq
                    .getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the link's parent directory grants write access
            faMan.checkPermission(FileAccessManager.WRITE_ACCESS, sMan, lRes.getParentDir(), 0, rq
                    .getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the link exists already
            lRes.checkIfFileExistsAlready();
            
            // check whether the target path prefix is searchable
            faMan.checkSearchPermission(sMan, tRes, rq.getDetails().userId, rq
                    .getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the target exists
            tRes.checkIfFileDoesNotExist();
            
            FileMetadata target = tRes.getFile();
            
            if (target.isDirectory())
                throw new UserException(ErrNo.EPERM, "no support for links to directories");
            
            // check whether the target file grants write access
            faMan.checkPermission(FileAccessManager.WRITE_ACCESS, sMan, target, tRes
                    .getParentDirId(), rq.getDetails().userId, rq.getDetails().superUser, rq
                    .getDetails().groupIds);
            
            // prepare file creation in database
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            // create the link
            sMan.link(target, lRes.getParentDirId(), lRes.getFileName(), update);
            
            // update POSIX timestamps
            MRCHelper.updateFileTimes(lRes.getParentsParentId(), lRes.getParentDir(), false,
                true, true, sMan, update);
            MRCHelper.updateFileTimes(tRes.getParentDirId(), target, false, true, false, sMan,
                update);
            
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
            
            args.linkPath = (String) arguments.get(0);
            args.targetPath = (String) arguments.get(1);
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
