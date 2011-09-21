/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.stage.BabuDBPostprocessing;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.BabuDBComponent.BabuDBRequest;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.linkRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.timestampResponse;

import com.google.protobuf.Message;

/**
 * 
 * @author stender
 */
public class CreateLinkOperation extends MRCOperation {
    
    public CreateLinkOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq, RPCRequestCallback callback) throws Exception {
        
        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);
        
        final linkRequest rqArgs = (linkRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        final Path lp = new Path(rqArgs.getVolumeName(), rqArgs.getLinkPath());
        final Path tp = new Path(rqArgs.getVolumeName(), rqArgs.getTargetPath());
        
        if (!lp.getComp(0).equals(tp.getComp(0)))
            throw new UserException(POSIXErrno.POSIX_ERROR_EXDEV,
                "cannot create hard links across volume boundaries");
        
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
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "no support for links to directories");
        
        // check whether the target file grants write access
        faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, target, tRes.getParentDirId(), rq
                .getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        final int time = (int) (TimeSync.getGlobalTime() / 1000);
        
        // prepare file creation in database
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(new BabuDBPostprocessing<Object>() {
            
            @Override
            public Message execute(Object result, BabuDBRequest request) throws Exception {
                
                // set the response
                return timestampResponse.newBuilder().setTimestampS(time).build();
            }
        });
        
        // create the link
        sMan.link(target, lRes.getParentDirId(), lRes.getFileName(), update);
        
        // update POSIX timestamps
        MRCHelper.updateFileTimes(lRes.getParentsParentId(), lRes.getParentDir(), false, true, true, sMan,
            time, update);
        MRCHelper.updateFileTimes(tRes.getParentDirId(), target, false, true, false, sMan, time, update);
        
        update.execute(callback, rq.getMetadata());
    }
    
}
