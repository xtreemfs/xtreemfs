/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.removexattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.timestampResponse;

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
        
        Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        
        validateContext(rq);
        
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
        
        // set a system attribute
        if (attrKey.startsWith("xtreemfs.")) {
            
            // check whether the user has privileged permissions to set
            // system attributes
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);
            
            MRCHelper.setSysAttrValue(master, sMan, res.getParentDirId(), file, attrKey.substring(9), "", update);
        }

        // set a user attribute
        else {
            
            sMan.setXAttr(file.getId(), rq.getDetails().userId, attrKey, null, update);
        }
        
        // update POSIX timestamps
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, time, update);
        
        // set the response
        rq.setResponse(timestampResponse.newBuilder().setTimestampS(time).build());
        
        update.execute();
    }
    
}
