/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
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
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.setxattrRequest;

/**
 * 
 * @author stender
 */
public class SetXAttrOperation extends MRCOperation {
        
    public SetXAttrOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final setxattrRequest rqArgs = (setxattrRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        
        StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        // retrieve and prepare the metadata to return
        FileMetadata file = res.getFile();
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // if the attribute is a system attribute, set it
        
        final String attrKey = rqArgs.getName();
        final byte[] attrVal = rqArgs.hasValueBytesString() ? rqArgs.getValueBytesString().toByteArray()
                : rqArgs.hasValue() ? rqArgs.getValue().getBytes() : null;
        
        // set a system attribute
        if (attrKey.startsWith(StorageManager.SYS_ATTR_KEY_PREFIX)) {
                        
            // check for the admin password to match the provided password
            boolean privileged = false;
            if (master.getConfig().getAdminPassword().length() > 0
                 && master.getConfig().getAdminPassword().equals(rq.getDetails().password)) {
                privileged = true;
            }
            
            // whether the user has privileged permissions to set system attributes
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, 
                rq.getDetails().superUser || privileged, // the admin password accounts for priviledged access
                rq.getDetails().groupIds);
            
            MRCHelper.setSysAttrValue(master, sMan, res.getParentDirId(), file,
                    attrKey.substring(StorageManager.SYS_ATTR_KEY_PREFIX.length()), new String(attrVal), update);
        }

        // set a user attribute
        else {
            
            // first, check the flags to ensure that the op can be executed
            
            boolean exists = sMan.getXAttr(file.getId(), rq.getDetails().userId, attrKey) != null;
            if (exists && rqArgs.getFlags() == XATTR_FLAGS.XATTR_FLAGS_CREATE.getNumber())
                throw new UserException(POSIXErrno.POSIX_ERROR_EEXIST, "attribute exists already");
            if (!exists && rqArgs.getFlags() == XATTR_FLAGS.XATTR_FLAGS_REPLACE.getNumber())
                throw new UserException(POSIXErrno.POSIX_ERROR_ENODATA, "attribute does not exist");
            
            sMan.setXAttr(file.getId(), rq.getDetails().userId, attrKey, attrVal.length == 0 ? null
                : attrVal, update);
        }
        
        // update POSIX timestamps
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, time, update);
        
        // set the response
        rq.setResponse(emptyResponse.getDefaultInstance());
        
        update.execute();
        
    }
    
}
