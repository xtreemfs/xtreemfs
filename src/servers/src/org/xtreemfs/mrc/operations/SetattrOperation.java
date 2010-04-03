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

import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.MRCInterface.setattrRequest;
import org.xtreemfs.interfaces.MRCInterface.setattrResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;

/**
 * Sets attributes of a file.
 * 
 * @author stender, bjko
 */
public class SetattrOperation extends MRCOperation {
    
    public SetattrOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final setattrRequest rqArgs = (setattrRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        Path p = new Path(rqArgs.getVolume_name(), rqArgs.getPath());
        
        StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        // retrieve and prepare the metadata to return
        FileMetadata file = res.getFile();
        
        // if the file refers to a symbolic link, resolve the link
        String target = sMan.getSoftlinkTarget(file.getId());
        if (target != null) {
            rqArgs.setPath(target);
            p = new Path(target);
            
            // if the local MRC is not responsible, send a redirect
            if (!vMan.hasVolume(p.getComp(0))) {
                finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrNo.ENOENT, "link target "
                    + target + " does not exist"));
                return;
            }
            
            sMan = vMan.getStorageManagerByName(p.getComp(0));
            res = new PathResolver(sMan, p);
            file = res.getFile();
        }
        
        // determine which attributes to set
        boolean setMode = (rqArgs.getTo_set() & MRCInterface.SETATTR_MODE) == MRCInterface.SETATTR_MODE;
        boolean setUID = (rqArgs.getTo_set() & MRCInterface.SETATTR_UID) == MRCInterface.SETATTR_UID;
        boolean setGID = (rqArgs.getTo_set() & MRCInterface.SETATTR_GID) == MRCInterface.SETATTR_GID;
        boolean setSize = (rqArgs.getTo_set() & MRCInterface.SETATTR_SIZE) == MRCInterface.SETATTR_SIZE;
        boolean setAtime = (rqArgs.getTo_set() & MRCInterface.SETATTR_ATIME) == MRCInterface.SETATTR_ATIME;
        boolean setCtime = (rqArgs.getTo_set() & MRCInterface.SETATTR_CTIME) == MRCInterface.SETATTR_CTIME;
        boolean setMtime = (rqArgs.getTo_set() & MRCInterface.SETATTR_MTIME) == MRCInterface.SETATTR_MTIME;
        boolean setAttributes = (rqArgs.getTo_set() & MRCInterface.SETATTR_ATTRIBUTES) == MRCInterface.SETATTR_ATTRIBUTES;
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // if MODE bit is set, peform 'chmod'
        if (setMode) {
            
            // check whether the access mode may be changed
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);
            
            // change the access mode; only bits 0-11 may be changed
            faMan.setPosixAccessMode(sMan, file, res.getParentDirId(), rq.getDetails().userId, rq
                    .getDetails().groupIds, (file.getPerms() & 0xFFFFF800)
                | (rqArgs.getStbuf().getMode() & 0x7FF), update);
            
            // update POSIX timestamps
            MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, update);
        }
        
        // if USER_ID or GROUP_ID are set, perform 'chown'
        if (setUID || setGID) {
            
            // check whether the owner may be changed
            
            if (setUID) {
                // if a UID is supposed to be set, restrict operation to root
                // user
                if (!rq.getDetails().superUser)
                    throw new UserException(ErrNo.EPERM, "changing owners is restricted to superusers");
                
            } else {
                // if only a GID is provided, restrict the op to a privileged
                // user
                // that is either root or in the group that is supposed to be
                // assigned
                faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
                    rq.getDetails().superUser, rq.getDetails().groupIds);
                if (!(rq.getDetails().superUser || rq.getDetails().groupIds.contains(rqArgs.getStbuf()
                        .getGroup_id())))
                    throw new UserException(
                        ErrNo.EPERM,
                        "changing owning groups is restricted to superusers or file owners who are in the group that is supposed to be assigned");
            }
            
            // change owner and owning group
            file.setOwnerAndGroup(setUID ? rqArgs.getStbuf().getUser_id() : file.getOwnerId(),
                setGID ? rqArgs.getStbuf().getGroup_id() : file.getOwningGroupId());
            
            // update POSIX timestamps
            MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, update);
        }
        
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
        
        // if ATTRIBUTES bit is set, peform 'setattr' for Win32 attributes
        if (setAttributes) {
            
            // check whether write permissions are granted to the parent
            // directory
            faMan.checkPermission("w", sMan, file, res.getParentDirId(), rq.getDetails().userId, rq
                    .getDetails().superUser, rq.getDetails().groupIds);
            
            file.setW32Attrs(rqArgs.getStbuf().getAttributes());
        }
        
        if (setUID || setGID || setAttributes)
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        
        if (setAtime || setCtime || setMtime || setSize)
            sMan.setMetadata(file, FileMetadata.FC_METADATA, update);
        
        // set the response
        rq.setResponse(new setattrResponse());
        
        update.execute();
        
    }
}
