/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_restore_fileRequest;

/**
 * 
 * @author stender
 */
public class RestoreFileOperation extends MRCOperation {
    
    public RestoreFileOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null && !master.getReplMasterUUID().equals(master.getConfig().getUUID()))
            throw new DatabaseException(ExceptionType.REDIRECT);
        
        final xtreemfs_restore_fileRequest rqArgs = (xtreemfs_restore_fileRequest) rq.getRequestArgs();
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword().length() > 0
            && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "invalid password");
        
        final VolumeManager vMan = master.getVolumeManager();
        
        // parse volume and file ID from global file ID
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(rqArgs.getFileId());
        
        final Path p = new Path(vMan.getStorageManager(idRes.getVolumeId()).getVolumeInfo().getName(), rqArgs
                .getFilePath());
        final StorageManager sMan = vMan.getStorageManager(idRes.getVolumeId());
        
        // prepare file creation in database
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        long nextFileId = sMan.getNextFileId();
        
        // create parent directories if necessary
        FileMetadata[] path = sMan.resolvePath(p);
        long parentId = 1;
        for (int i = 0; i < p.getCompCount(); i++)
            try {
                if (path[i] != null)
                    parentId = path[i].getId();
                
                else {
                    sMan.createDir(nextFileId, parentId, p.getComp(i), time, time, time,
                        rq.getDetails().userId, rq.getDetails().groupIds.get(0), 509, 0, update);
                    parentId = nextFileId;
                    nextFileId++;
                    
                    // set the file ID as the last one
                    sMan.setLastFileId(nextFileId, update);
                }
            } catch (DatabaseException exc) {
                if (exc.getType() != ExceptionType.FILE_EXISTS)
                    throw exc;
            }
        
        // create the metadata object
        FileMetadata file = sMan.createFile(idRes.getLocalFileId(), parentId, rqArgs.getFileId(), time, time,
            time, rq.getDetails().userId, rq.getDetails().groupIds.get(0), 511, 0, rqArgs.getFileSize(),
            false, 0, 0, update);
        
        int size = (rqArgs.getStripeSize() < 1024 ? 1 : (rqArgs.getStripeSize() % 1024 != 0) ? rqArgs
                .getStripeSize() / 1024 + 1 : rqArgs.getStripeSize() / 1024);
        
        // create and assign the new XLocList
        StripingPolicy sp = sMan.createStripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0.name(), size,
            1);
        XLoc replica = sMan.createXLoc(sp, new String[] { rqArgs.getOsdUuid() }, 0);
        XLocList xLocList = sMan.createXLocList(new XLoc[] { replica }, ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, 0);
        
        file.setXLocList(xLocList);
        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        
        // set the response
        rq.setResponse(emptyResponse.getDefaultInstance());
        
        update.execute();
        
    }

}
