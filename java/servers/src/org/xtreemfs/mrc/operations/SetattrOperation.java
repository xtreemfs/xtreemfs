/*
 * Copyright (c) 2010-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.TimeSync;
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
import org.xtreemfs.mrc.quota.QuotaFileInformation;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.setattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.timestampResponse;

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
        
        Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        
        StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        // retrieve and prepare the metadata to return
        FileMetadata file = res.getFile();
        int time = 0;
        
        // determine which attributes to set
        boolean setMode = (rqArgs.getToSet() & MRC.Setattrs.SETATTR_MODE.getNumber()) == MRC.Setattrs.SETATTR_MODE
                .getNumber();
        boolean setUID = (rqArgs.getToSet() & MRC.Setattrs.SETATTR_UID.getNumber()) == MRC.Setattrs.SETATTR_UID
                .getNumber();
        boolean setGID = (rqArgs.getToSet() & MRC.Setattrs.SETATTR_GID.getNumber()) == MRC.Setattrs.SETATTR_GID
                .getNumber();
        boolean setSize = (rqArgs.getToSet() & MRC.Setattrs.SETATTR_SIZE.getNumber()) == MRC.Setattrs.SETATTR_SIZE
                .getNumber();
        boolean setAtime = (rqArgs.getToSet() & MRC.Setattrs.SETATTR_ATIME.getNumber()) == MRC.Setattrs.SETATTR_ATIME
                .getNumber();
        boolean setCtime = (rqArgs.getToSet() & MRC.Setattrs.SETATTR_CTIME.getNumber()) == MRC.Setattrs.SETATTR_CTIME
                .getNumber();
        boolean setMtime = (rqArgs.getToSet() & MRC.Setattrs.SETATTR_MTIME.getNumber()) == MRC.Setattrs.SETATTR_MTIME
                .getNumber();
        boolean setAttributes = (rqArgs.getToSet() & MRC.Setattrs.SETATTR_ATTRIBUTES.getNumber()) == MRC.Setattrs.SETATTR_ATTRIBUTES
                .getNumber();
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // if MODE bit is set, peform 'chmod'
        if (setMode) {
            
            // check whether the access mode may be changed
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);
            
            // change the access mode; only bits 0-12 may be changed
            faMan.setPosixAccessMode(sMan, file, res.getParentDirId(), rq.getDetails().userId, rq
                    .getDetails().groupIds, (file.getPerms() & 0xFFFFF000)
                | (rqArgs.getStbuf().getMode() & 0xFFF), rq.getDetails().superUser, update);
            
            // update POSIX timestamps
            if (time == 0)
                time = (int) (TimeSync.getGlobalTime() / 1000);
            MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, time, update);
        }
        
        // if USER_ID or GROUP_ID are set, perform 'chown'
        if (setUID || setGID) {
            
            // check whether the owner may be changed
            
            if (setUID) {
                
                // if a UID is supposed to be set, check if the operation needs
                // to be restricted to root users
                byte[] value = sMan.getXAttr(1, StorageManager.SYSTEM_UID, "xtreemfs."
                    + MRCHelper.VOL_ATTR_PREFIX + ".chown_non_root");
                
                // check permission
                if (value != null && new String(value).equals("true")) {
                    faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
                        rq.getDetails().superUser, rq.getDetails().groupIds);
                    
                } else if (!rq.getDetails().superUser)
                    throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                        "changing owners is restricted to superusers");

                // transfer file space information from old owner to new owner
                if (!file.isDirectory() && file.getXLocList() != null) {
                    QuotaFileInformation quotaFileInformation = new QuotaFileInformation(sMan.getVolumeInfo().getId(),
                            file);
                    master.getMrcVoucherManager().transferOwnerSpace(quotaFileInformation,
                            rqArgs.getStbuf().getUserId(), update);
                }
            }
            
            if (setGID) {
                // if a GID is provided, restrict the op to a privileged
                // user that is either root or in the group that is supposed to
                // be assigned
                faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
                    rq.getDetails().superUser, rq.getDetails().groupIds);
                if (!(rq.getDetails().superUser || rq.getDetails().groupIds.contains(rqArgs.getStbuf()
                        .getGroupId())))
                    throw new UserException(
                        POSIXErrno.POSIX_ERROR_EPERM,
                        "changing owning groups is restricted to superusers or file owners who are in the group that is supposed to be assigned");

                // transfer file space information from old owner group to new owner group
                if (!file.isDirectory()) {
                    QuotaFileInformation quotaFileInformation = new QuotaFileInformation(sMan.getVolumeInfo().getId(),
                            file);
                    master.getMrcVoucherManager().transferOwnerGroupSpace(quotaFileInformation,
                            rqArgs.getStbuf().getGroupId(), update);
                }
            }
            
            // change owner and owning group
            file.setOwnerAndGroup(setUID ? rqArgs.getStbuf().getUserId() : file.getOwnerId(), setGID ? rqArgs
                    .getStbuf().getGroupId() : file.getOwningGroupId());
            
            // update POSIX timestamps
            if (time == 0)
                time = (int) (TimeSync.getGlobalTime() / 1000);
            MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, time, update);
        }
        
        // if SIZE bit is set, peform 'xtreemfs_updateFileSize'
        if (setSize) {
            
            long newFileSize = rqArgs.getStbuf().getSize();
            int epochNo = rqArgs.getStbuf().getTruncateEpoch();
            
            // only accept valid file size updates
            if (epochNo >= file.getEpoch()) {
                
                boolean epochChanged = epochNo > file.getEpoch();
                
                // accept any file size in a new epoch but only larger file
                // sizes in
                // the current epoch
                if (epochChanged || newFileSize > file.getSize()) {
                    
                    long oldFileSize = file.getSize();
                    if (time == 0)
                        time = (int) (TimeSync.getGlobalTime() / 1000);
                    
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
                file.setAtime((int) (rqArgs.getStbuf().getAtimeNs() / (long) 1e9));
            if (setCtime)
                file.setCtime((int) (rqArgs.getStbuf().getCtimeNs() / (long) 1e9));
            if (setMtime)
                file.setMtime((int) (rqArgs.getStbuf().getMtimeNs() / (long) 1e9));
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
        rq.setResponse(timestampResponse.newBuilder().setTimestampS(time).build());
        
        update.execute();
        
    }
}
