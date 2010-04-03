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
import org.xtreemfs.interfaces.MRCInterface.linkRequest;
import org.xtreemfs.interfaces.MRCInterface.linkResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;

/**
 * 
 * @author stender
 */
public class CreateLinkOperation extends MRCOperation {
        
    public CreateLinkOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final linkRequest rqArgs = (linkRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        final Path lp = new Path(rqArgs.getVolume_name(), rqArgs.getLink_path());
        final Path tp = new Path(rqArgs.getVolume_name(), rqArgs.getTarget_path());
        
        if (!lp.getComp(0).equals(tp.getComp(0)))
            throw new UserException(ErrNo.EXDEV, "cannot create hard links across volume boundaries");
        
        final StorageManager sMan = vMan.getStorageManagerByName(lp.getComp(0));
        final PathResolver lRes = new PathResolver(sMan, lp);
        final PathResolver tRes = new PathResolver(sMan, tp);
        
        // check whether the link's path prefix is searchable
        faMan.checkSearchPermission(sMan, lRes, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether the link's parent directory grants write access
        faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, lRes.getParentDir(), 0,
            rq.getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // check whether the link exists already
        lRes.checkIfFileExistsAlready();
        
        // check whether the target path prefix is searchable
        faMan.checkSearchPermission(sMan, tRes, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether the target exists
        tRes.checkIfFileDoesNotExist();
        
        FileMetadata target = tRes.getFile();
        
        if (target.isDirectory())
            throw new UserException(ErrNo.EPERM, "no support for links to directories");
        
        // check whether the target file grants write access
        faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, target, tRes.getParentDirId(), rq
                .getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // prepare file creation in database
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // create the link
        sMan.link(target, lRes.getParentDirId(), lRes.getFileName(), update);
        
        // update POSIX timestamps
        MRCHelper.updateFileTimes(lRes.getParentsParentId(), lRes.getParentDir(), false, true, true, sMan,
            update);
        MRCHelper.updateFileTimes(tRes.getParentDirId(), target, false, true, false, sMan, update);
        
        // set the response
        rq.setResponse(new linkResponse());
        
        update.execute();
    }
    
}
