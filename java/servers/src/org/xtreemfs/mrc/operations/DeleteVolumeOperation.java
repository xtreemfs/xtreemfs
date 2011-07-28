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
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_rmvolRequest;

import com.google.protobuf.Message;

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
        RPCResponse<Message> response = master.getDirClient().xtreemfs_service_deregister(null,
            rq.getDetails().auth, RPCAuthentication.userService, volume.getId());
        response.registerListener(new RPCResponseAvailableListener<Message>() {
            
            @Override
            public void responseAvailable(RPCResponse<Message> r) {
                processStep2(rqArgs, volume.getId(), rq, r);
            }
        });
    }
    
    private void processStep2(xtreemfs_rmvolRequest rqArgs, final String volumeId, final MRCRequest rq,
        final RPCResponse<Message> rpcResponse) {
        
        try {
            
            // check whether an exception has occured; if so, an exception is
            // thrown when trying to parse the response
            rpcResponse.get();
            
            // delete the volume from the local database
            master.getVolumeManager().deleteVolume(volumeId, master, rq);
            
            // set the response
            rq.setResponse(emptyResponse.getDefaultInstance());
            finishRequest(rq);
            
        } catch (UserException exc) {
            if (Logging.isDebug())
                Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorType.ERRNO, exc.getErrno(), exc.getMessage(), exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                "an error has occurred", exc));
        } finally {
            rpcResponse.freeBuffers();
        }
    }
    
}
