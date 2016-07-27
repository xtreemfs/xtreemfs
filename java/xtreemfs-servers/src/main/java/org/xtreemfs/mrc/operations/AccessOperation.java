/*
 * Copyright (c) 2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.ACCESS_FLAGS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.accessRequest;

/**
 * 
 * @author stender
 */
public class AccessOperation extends MRCOperation {
    
    public AccessOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final accessRequest rqArgs = (accessRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        final Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        
        final StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        final PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // F_OK(==0) is always set, check if the file exists
        if (res.getFile() == null) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EACCES, "file or directory '" + rqArgs.getPath()
                + "' does not exist");
        }

        // in any other case, check if the file grants the respective access
        else {
            
            if ((rqArgs.getFlags() & ACCESS_FLAGS.ACCESS_FLAGS_R_OK.getNumber()) != 0)
                faMan.checkPermission("r", sMan, res.getFile(), res.getParentDirId(), rq.getDetails().userId,
                    rq.getDetails().superUser, rq.getDetails().groupIds);

            if ((rqArgs.getFlags() & ACCESS_FLAGS.ACCESS_FLAGS_W_OK.getNumber()) != 0)
                faMan.checkPermission("w", sMan, res.getFile(), res.getParentDirId(), rq.getDetails().userId,
                    rq.getDetails().superUser, rq.getDetails().groupIds);
            
            if ((rqArgs.getFlags() & ACCESS_FLAGS.ACCESS_FLAGS_X_OK.getNumber()) != 0)
                faMan.checkPermission("x", sMan, res.getFile(), res.getParentDirId(), rq.getDetails().userId,
                    rq.getDetails().superUser, rq.getDetails().groupIds);
            
        }
        
        // set the response
        rq.setResponse(emptyResponse.getDefaultInstance());
        
        finishRequest(rq);
    }
}
