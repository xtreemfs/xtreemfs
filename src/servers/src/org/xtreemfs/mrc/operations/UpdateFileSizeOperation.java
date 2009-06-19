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

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.NewFileSize;
import org.xtreemfs.interfaces.NewFileSizeSet;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_update_file_sizeResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;

/**
 * 
 * @author stender
 */
public class UpdateFileSizeOperation extends MRCOperation {
    
    public UpdateFileSizeOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_update_file_sizeRequest rqArgs = (xtreemfs_update_file_sizeRequest) rq
                .getRequestArgs();
        
        Capability cap = new Capability(rqArgs.getXcap(), master.getConfig().getCapabilitySecret());
        
        // check whether the capability has a valid signature
        if (!cap.hasValidSignature())
            throw new UserException(ErrNo.EPERM, cap + " does not have a valid signature");
        
        // check whether the capability has expired
        if (cap.hasExpired())
            throw new UserException(ErrNo.EPERM, cap + " has expired");
        
        // parse volume and file ID from global file ID
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(cap.getFileId());
        
        StorageManager sMan = master.getVolumeManager().getStorageManager(idRes.getVolumeId());
        
        FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
        if (file == null)
            throw new UserException(ErrNo.ENOENT, "file '" + cap.getFileId() + "' does not exist");
        
        NewFileSizeSet newFSSet = rqArgs.getOsd_write_response().getNew_file_size();
        
        if (newFSSet.isEmpty())
            throw new UserException(ErrNo.EINVAL, "invalid file size: empty");
        
        NewFileSize newFS = newFSSet.get(0);
        long newFileSize = newFS.getSize_in_bytes();
        int epochNo = newFS.getTruncate_epoch();
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // only accept valid file size updates
        if (epochNo >= file.getEpoch()) {
            
            // accept any file size in a new epoch but only larger file
            // sizes in
            // the current epoch
            if (epochNo > file.getEpoch() || newFileSize > file.getSize()) {
                
                long oldFileSize = file.getSize();
                int time = (int) (TimeSync.getGlobalTime() / 1000);
                
                file.setSize(newFileSize);
                file.setEpoch(epochNo);
                file.setCtime(time);
                file.setMtime(time);
                
                sMan.setMetadata(file, FileMetadata.FC_METADATA, update);
                sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
                
                // update the volume size
                sMan.setVolumeSize(sMan.getVolumeSize() + newFileSize - oldFileSize, update);
            }

            else if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "received update for outdated file size: " + newFileSize + ", current file size="
                        + file.getSize());
        }

        else {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "received file size update w/ outdated epoch: " + epochNo + ", current epoch="
                        + file.getEpoch());
        }
        
        // set the response
        rq.setResponse(new xtreemfs_update_file_sizeResponse());
        
        update.execute();
        
    }
}
