/*
 * Copyright (c) 2008-2011 by Jan Stender,
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
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_addRequest;

/**
 * 
 * @author stender
 */
public class AddReplicaOperation extends MRCOperation {
    
    public AddReplicaOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);

        validateContext(rq);

        final xtreemfs_replica_addRequest rqArgs = (xtreemfs_replica_addRequest) rq.getRequestArgs();

        final FileAccessManager faMan = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();

        StorageManager sMan = null;
        FileMetadata file = null;
        String fileId;


        if (rqArgs.hasFileId()) {

            fileId = rqArgs.getFileId();

            // parse volume and file ID from global file ID
            GlobalFileIdResolver idRes = new GlobalFileIdResolver(fileId);

            sMan = vMan.getStorageManager(idRes.getVolumeId());

            // retrieve the file metadata
            file = sMan.getMetadata(idRes.getLocalFileId());
            if (file == null)
                throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + rqArgs.getFileId()
                        + "' does not exist");

        } else if (rqArgs.hasVolumeName() && rqArgs.hasPath()) {

            final Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());

            sMan = vMan.getStorageManagerByName(p.getComp(0));
            final PathResolver res = new PathResolver(sMan, p);

            res.checkIfFileDoesNotExist();
            file = res.getFile();

            // TODO (jdillmann): Move to MRCHelper.createGlobalFileId(VolumeInfo volume, FileMetadata file)
            fileId = sMan.getVolumeInfo().getId() + ":" + file.getId();

            // check whether the path prefix is searchable
            faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser,
                    rq.getDetails().groupIds);

        } else
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "either file ID or volume name + path required");

        if (file.isDirectory())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "replicas may only be added to files");

        if (sMan.getSoftlinkTarget(file.getId()) != null)
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "file '" + rqArgs.getFileId()
                    + "' is a symbolic link");

        // check whether privileged permissions are granted for adding
        // replicas
        faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);

        // handover control to the XLocSetCoordinator
        master.getXLocSetCoordinator().addReplicas(fileId, this, rq);
    }

}
