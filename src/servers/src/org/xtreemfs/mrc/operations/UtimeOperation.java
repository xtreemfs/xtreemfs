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
import org.xtreemfs.interfaces.MRCInterface.utimensRequest;
import org.xtreemfs.interfaces.MRCInterface.utimensResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * Updates the file's timestamps
 * 
 * @author bjko
 */
public class UtimeOperation extends MRCOperation {
    
    public UtimeOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final utimensRequest rqArgs = (utimensRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        Path p = new Path(rqArgs.getPath());
        
        VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
        StorageManager sMan = vMan.getStorageManager(volume.getId());
        PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        // retrieve and prepare the metadata to return
        FileMetadata file = res.getFile();
        
        // if the file refers to a symbolic link, resolve the link
        String target = sMan.getSoftlinkTarget(file.getId());
        if (target != null) {
            rqArgs.setPath(target);
            p = new Path(target);
            
            // if the local MRC is not responsible, send a redirect
            if (!vMan.hasVolume(p.getComp(0))) {
                finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrNo.ENOENT, "link target "
                    + target + " does not exist"));
                return;
            }
            
            volume = vMan.getVolumeByName(p.getComp(0));
            sMan = vMan.getStorageManager(volume.getId());
            res = new PathResolver(sMan, p);
            file = res.getFile();
        }
        
        // check whether write permissions are granted to file
        // faMan.checkPermission("w", sMan, file, res.getParentDirId(),
        // rq.getDetails().userId, rq
        // .getDetails().superUser, rq.getDetails().groupIds);
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        if (rqArgs.getAtime_ns() != 0)
            file.setAtime((int) (rqArgs.getAtime_ns() / (long) 1e9));
        if (rqArgs.getCtime_ns() != 0)
            file.setCtime((int) (rqArgs.getCtime_ns() / (long) 1e9));
        if (rqArgs.getMtime_ns() != 0)
            file.setMtime((int) (rqArgs.getMtime_ns() / (long) 1e9));
        
        // update POSIX timestamps
        sMan.setMetadata(file, FileMetadata.FC_METADATA, update);
        
        // set the response
        rq.setResponse(new utimensResponse());
        
        update.execute();
        
    }
    
}
