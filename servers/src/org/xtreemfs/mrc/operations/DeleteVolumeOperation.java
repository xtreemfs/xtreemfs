/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_rmvolRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_rmvolResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class DeleteVolumeOperation extends MRCOperation {
    
    public static final int OP_ID = 16;
    
    public DeleteVolumeOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(final MRCRequest rq) throws Throwable {
        
        final xtreemfs_rmvolRequest rqArgs = (xtreemfs_rmvolRequest) rq.getRequestArgs();
        
        final VolumeInfo volume = master.getVolumeManager().getVolumeByName(rqArgs.getVolume_name());
        final StorageManager sMan = master.getVolumeManager().getStorageManager(volume.getId());
        
        // get the volume's root directory
        FileMetadata file = sMan.getMetadata(0, volume.getName());
        
        // check whether privileged permissions are granted for deleting the
        // volume
        master.getFileAccessManager().checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
            rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // deregister the volume from the Directory Service
        RPCResponse response = master.getDirClient().xtreemfs_service_deregister(null, volume.getId());
        response.registerListener(new RPCResponseAvailableListener() {
            
            @Override
            public void responseAvailable(RPCResponse r) {
                processStep2(rqArgs, volume.getId(), rq, r);
            }
        });
    }
    
    private void processStep2(xtreemfs_rmvolRequest rqArgs, final String volumeId, final MRCRequest rq,
        final RPCResponse rpcResponse) {
        
        try {
            
            // check whether an exception has occured; if so, an exception is
            // thrown when trying to parse the response
            rpcResponse.get();
            
            // delete the volume from the local database
            master.getVolumeManager().deleteVolume(volumeId, master, rq);
            
            // set the response
            rq.setResponse(new xtreemfs_rmvolResponse());
            finishRequest(rq);
            
        } catch (UserException exc) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        } finally {
            rpcResponse.freeBuffers();
        }
    }
    
}
