/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
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
    public void startRequest(final MRCRequest rq, final RPCRequestCallback callback) throws Exception {
        
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
        if (master.getConfig().getAdminPassword() == null)
            master.getFileAccessManager().checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
                    rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // deregister the volume from the Directory Service
        master.getDirClient().xtreemfs_service_deregister(null, rq.getDetails().auth,
                RPCAuthentication.userService, volume.getId(), new Callback() {
                                       
            @Override
            public void failed(Throwable e) {
                
                callback.failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, e);
            }

            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                processStep2(rqArgs, volume.getId(), rq, callback);
                return true;
            }
        }, rq.getRemainingProcessingTime(), rq.hasHighPriority());
    }
    
    private void processStep2(xtreemfs_rmvolRequest rqArgs, String volumeId, MRCRequest rq, 
            RPCRequestCallback callback) {
        
        try {
            // delete the volume from the local database
            master.getVolumeManager().deleteVolume(volumeId);
            master.notifyVolumeDeleted();
            
            // set the response
            callback.success(emptyResponse.getDefaultInstance());
            
        } catch (UserException exc) {
            
            if (Logging.isDebug()) Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
            callback.failed(ErrorType.ERRNO, exc.getErrno(), exc.getMessage(), exc);
        } catch (Throwable exc) {
            
            callback.failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, exc);
        }
    }   
}