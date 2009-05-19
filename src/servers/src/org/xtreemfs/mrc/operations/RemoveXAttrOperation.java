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
import org.xtreemfs.interfaces.MRCInterface.removexattrRequest;
import org.xtreemfs.interfaces.MRCInterface.removexattrResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
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
public class RemoveXAttrOperation extends MRCOperation {
    
    public RemoveXAttrOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final removexattrRequest rqArgs = (removexattrRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        Path p = new Path(rqArgs.getPath());
        
        validateContext(rq);
        
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
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // if the attribute is a system attribute, set it
        
        final String attrKey = rqArgs.getName();
        
        // set a system attribute
        if (attrKey.startsWith("xtreemfs.")) {
            
            // check whether the user has privileged permissions to set
            // system attributes
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);
            
            MRCHelper.setSysAttrValue(sMan, vMan, volume, res.getParentDirId(), file, attrKey.substring(9),
                "", update);
        }

        // set a user attribute
        else {
            
            sMan.setXAttr(file.getId(), rq.getDetails().userId, attrKey, null, update);
        }
        
        // update POSIX timestamps
        MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, update);
        
        // set the response
        rq.setResponse(new removexattrResponse());
        
        update.execute();
    }
    
}
