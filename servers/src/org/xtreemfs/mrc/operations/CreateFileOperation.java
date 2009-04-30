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

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.MRCInterface.createRequest;
import org.xtreemfs.interfaces.MRCInterface.createResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class CreateFileOperation extends MRCOperation {
    
    public static final int OP_ID = 4;
    
    public CreateFileOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final createRequest rqArgs = (createRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        final Path p = new Path(rqArgs.getPath());
        
        final VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
        final StorageManager sMan = vMan.getStorageManager(volume.getId());
        final PathResolver res = new PathResolver(sMan, p);
        
        // check if file == volume
        if (res.getParentDir() == null)
            throw new UserException(ErrNo.EEXIST, "file or directory '" + res.getFileName()
                + "' exists already");
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether the parent directory grants write access
        faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, res.getParentDir(), res.getParentsParentId(),
            rq.getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // check whether the file/directory exists already
        res.checkIfFileExistsAlready();
        
        // prepare file creation in database
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // get the next free file ID
        long fileId = sMan.getNextFileId();
        
        // atime, ctime, mtime
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        
        // create the metadata object
        sMan.createFile(fileId, res.getParentDirId(), res.getFileName(), time, time, time,
            rq.getDetails().userId, rq.getDetails().groupIds.get(0), rqArgs.getMode(), 0, 0, false, 0, 0,
            update);
        
        // set the file ID as the last one
        sMan.setLastFileId(fileId, update);
        
        // set the response
        rq.setResponse(new createResponse());
        
        update.execute();
    }
    
}
