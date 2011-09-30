/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_read_only_xattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_read_only_xattrResponse;

public class SetReadOnlyXattrOperation extends MRCOperation {

    public SetReadOnlyXattrOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {

        final xtreemfs_set_read_only_xattrRequest rqArgs = (xtreemfs_set_read_only_xattrRequest) rq
                .getRequestArgs();

        final FileAccessManager fam = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();

        validateContext(rq);

        GlobalFileIdResolver gfr = new GlobalFileIdResolver(rqArgs.getFileId());

        final String volId = gfr.getVolumeId();
        final Long localFileID = gfr.getLocalFileId();

        StorageManager sMan = vMan.getStorageManager(volId);

        FileMetadata file = sMan.getMetadata(localFileID);
        

        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + rqArgs.getFileId()
                    + "' does not exist");

        if (file.isDirectory())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                    "replica-update policies may only be set on files");

        // check whether privileged permissions are granted for editing Xattr
        fam.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);

        Boolean currentMode = file.isReadOnly();

        if (currentMode == rqArgs.getValue()) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_INFO, Category.storage, (Object) null,
                        "File with fileID %s has already set Xattr xtreemfs.read_only=%s", localFileID,
                        currentMode.toString());
            }
            rq.setResponse(xtreemfs_set_read_only_xattrResponse.newBuilder().setWasSet(false).build());
            finishRequest(rq);
            
        } else {
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);

            file.setReadOnly(rqArgs.getValue());
            
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            rq.setResponse(xtreemfs_set_read_only_xattrResponse.newBuilder().setWasSet(true).build());
            update.execute();            
        }
     
    }
}
