/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_rmvolRequest;

/**
 * 
 * @author stender
 */
public class DeleteVolumeOperation extends MRCOperation {
    
    public DeleteVolumeOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(final MRCRequest rq) throws Throwable {
        
        // perform master redirect if replicated and required
        String replMasterUUID = master.getReplMasterUUID();
        if (replMasterUUID != null && !replMasterUUID.equals(master.getConfig().getUUID().toString())) {
            ServiceUUID uuid = new ServiceUUID(replMasterUUID);
            throw new DatabaseException(ExceptionType.REDIRECT, uuid.getAddress().getHostName() + ":"
                    + uuid.getAddress().getPort());
        }
        
        final xtreemfs_rmvolRequest rqArgs = (xtreemfs_rmvolRequest) rq.getRequestArgs();
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword().length() > 0
                && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "invalid password");
        
        final StorageManager sMan = master.getVolumeManager().getStorageManagerByName(rqArgs.getVolumeName());
        final VolumeInfo volume = sMan.getVolumeInfo();
        
        // get the volume's root directory
        FileMetadata file = sMan.getMetadata(0, volume.getName());
        
        // if no admin password has been set, check whether privileged
        // permissions are granted for deleting the volume
        if (master.getConfig().getAdminPassword().length() == 0)
            master.getFileAccessManager().checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
                    rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // Pause the HeartbeatThread while we are deleting the volume. Otherwise, the thread may re-register a
        // volume at the DIR which was already deregistered.
        master.pauseHeartbeatThread();

        // Delete the volume from the local database.
        try {
            master.getVolumeManager().deleteVolume(volume.getId(), master, rq);

            master.notifyVolumeDeleted();   
        } finally {
            master.resumeHeartbeatThread();
        }
        

        // Deregister the volume from the Directory Service asynchronously.
        // Ugly workaround for async call.
        Runnable asyncDeregisterRunner = new Runnable() {
            @Override
            public void run() {
                try {
                    // Deregister from the DIR.
                    master.getDirClient().xtreemfs_service_deregister(null, rq.getDetails().auth,
                            RPCAuthentication.userService, volume.getId());
                    
                    // Set the response.
                    rq.setResponse(emptyResponse.getDefaultInstance());
                    finishRequest(rq);
                    
                } catch (Exception exc) {
                    finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                            "An error has occurred at the MRC. Details: " + exc.getMessage(), exc));
                }
            }
        };
        (new Thread(asyncDeregisterRunner)).start();
    }
}
