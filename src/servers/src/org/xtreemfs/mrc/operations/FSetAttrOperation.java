/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.MRCInterface.fsetattrRequest;
import org.xtreemfs.interfaces.MRCInterface.fsetattrResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;

/**
 * Sets attributes of a file.
 * 
 * @author stender
 */
public class FSetAttrOperation extends MRCOperation {
    
    public FSetAttrOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final fsetattrRequest rqArgs = (fsetattrRequest) rq.getRequestArgs();
        
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
        
        // determine which attributes to set
        boolean setMode = (rqArgs.getTo_set() & MRCInterface.SETATTR_MODE) == MRCInterface.SETATTR_MODE;
        boolean setUID = (rqArgs.getTo_set() & MRCInterface.SETATTR_UID) == MRCInterface.SETATTR_UID;
        boolean setGID = (rqArgs.getTo_set() & MRCInterface.SETATTR_GID) == MRCInterface.SETATTR_GID;
        boolean setSize = (rqArgs.getTo_set() & MRCInterface.SETATTR_SIZE) == MRCInterface.SETATTR_SIZE;
        boolean setAtime = (rqArgs.getTo_set() & MRCInterface.SETATTR_ATIME) == MRCInterface.SETATTR_ATIME;
        boolean setCtime = (rqArgs.getTo_set() & MRCInterface.SETATTR_CTIME) == MRCInterface.SETATTR_CTIME;
        boolean setMtime = (rqArgs.getTo_set() & MRCInterface.SETATTR_MTIME) == MRCInterface.SETATTR_MTIME;
        boolean setAttributes = (rqArgs.getTo_set() & MRCInterface.SETATTR_ATTRIBUTES) == MRCInterface.SETATTR_ATTRIBUTES;
        
        if (setMode || setUID || setGID || setAttributes)
            throw new UserException(ErrNo.EINVAL, "setting modes, UIDs, GIDs and Win32 attributes not allowed");
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // if SIZE bit is set, peform 'xtreemfs_updateFileSize'
        if (setSize) {
            
            long newFileSize = rqArgs.getStbuf().getSize();
            int epochNo = rqArgs.getStbuf().getTruncate_epoch();
            
            // only accept valid file size updates
            if (epochNo >= file.getEpoch()) {
                
                boolean epochChanged = epochNo > file.getEpoch();
                
                // accept any file size in a new epoch but only larger file
                // sizes in
                // the current epoch
                if (epochChanged || newFileSize > file.getSize()) {
                    
                    long oldFileSize = file.getSize();
                    int time = (int) (TimeSync.getGlobalTime() / 1000);
                    
                    file.setSize(newFileSize);
                    file.setEpoch(epochNo);
                    file.setCtime(time);
                    file.setMtime(time);
                    
                    if (epochChanged)
                        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
                    
                    // update the volume size
                    sMan.getVolumeInfo().updateVolumeSize(newFileSize - oldFileSize, update);
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
        }
        
        // if ATIME, CTIME or MTIME bits are set, peform 'utimens'
        if (setAtime || setCtime || setMtime) {
            
            // check whether write permissions are granted to file
            // faMan.checkPermission("w", sMan, file, res.getParentDirId(),
            // rq.getDetails().userId, rq
            // .getDetails().superUser, rq.getDetails().groupIds);
            
            if (setAtime)
                file.setAtime((int) (rqArgs.getStbuf().getAtime_ns() / (long) 1e9));
            if (setCtime)
                file.setCtime((int) (rqArgs.getStbuf().getCtime_ns() / (long) 1e9));
            if (setMtime)
                file.setMtime((int) (rqArgs.getStbuf().getMtime_ns() / (long) 1e9));
        }
                
        if (setUID || setGID || setAttributes)
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        
        if (setAtime || setCtime || setMtime || setSize)
            sMan.setMetadata(file, FileMetadata.FC_METADATA, update);
        
        // set the response
        rq.setResponse(new fsetattrResponse());
        
        update.execute();
        
    }
}
