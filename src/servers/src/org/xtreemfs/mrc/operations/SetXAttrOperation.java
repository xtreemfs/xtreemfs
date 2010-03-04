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
import org.xtreemfs.interfaces.MRCInterface.setxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.setxattrResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
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
public class SetXAttrOperation extends MRCOperation {
    
    private static final int XATTR_CREATE  = 1;
    
    private static final int XATTR_REPLACE = 2;
    
    public SetXAttrOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final setxattrRequest rqArgs = (setxattrRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        Path p = new Path(rqArgs.getVolume_name() + "/" + rqArgs.getPath());
        
        StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
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
            
            sMan = vMan.getStorageManagerByName(p.getComp(0));
            res = new PathResolver(sMan, p);
            file = res.getFile();
        }
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // if the attribute is a system attribute, set it
        
        final String attrKey = rqArgs.getName();
        final String attrVal = rqArgs.getValue();
        
        // set a system attribute
        if (attrKey.startsWith(StorageManager.SYS_ATTR_KEY_PREFIX)) {
            
            // check whether the user has privileged permissions to set
            // system attributes
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);
            
            MRCHelper.setSysAttrValue(sMan, vMan, faMan, res.getParentDirId(), file, attrKey
                    .substring(StorageManager.SYS_ATTR_KEY_PREFIX.length()), attrVal, update);
        }

        // set a user attribute
        else {
            
            // first, check the flags to ensure that the op can be executed
            
            boolean exists = sMan.getXAttr(file.getId(), rq.getDetails().userId, attrKey) != null;
            if (exists && rqArgs.getFlags() == XATTR_CREATE)
                throw new UserException(ErrNo.EEXIST, "attribte exists already");
            if (!exists && rqArgs.getFlags() == XATTR_REPLACE)
                throw new UserException(ErrNo.ENODATA, "attribte does not exist");
            
            sMan.setXAttr(file.getId(), rq.getDetails().userId, attrKey, attrVal.length() == 0 ? null
                : attrVal, update);
        }
        
        // update POSIX timestamps
        MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, update);
        
        // set the response
        rq.setResponse(new setxattrResponse());
        
        update.execute();
        
    }
    
}
