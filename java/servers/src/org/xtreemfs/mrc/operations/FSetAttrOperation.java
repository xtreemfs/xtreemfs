/*
 * Copyright (c) 2010-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.fsetattrRequest;

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
        
        Capability cap = new Capability(rqArgs.getCap(), master.getConfig().getCapabilitySecret());
        
        // check whether the capability has a valid signature
        if (!cap.hasValidSignature())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " does not have a valid signature");
        
        // check whether the capability has expired
        if (cap.hasExpired())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " has expired");
        
        // parse volume and file ID from global file ID
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(cap.getFileId());
        
        StorageManager sMan = master.getVolumeManager().getStorageManager(idRes.getVolumeId());
        
        FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + cap.getFileId()
                + "' does not exist");
        
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
        
        if (setSize)
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                "setting file sizes not allowed on open files; use 'xtreemfs_update_file_size' instead");
        
        if (setMode || setUID || setGID || setAttributes)
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                "setting modes, UIDs, GIDs and Win32 attributes not allowed on open files");
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
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
        
        if (setUID || setGID || setAttributes)
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        
        if (setAtime || setCtime || setMtime || setSize)
            sMan.setMetadata(file, FileMetadata.FC_METADATA, update);
        
        // set the response
        rq.setResponse(emptyResponse.getDefaultInstance());
        
        update.execute();
        
    }
    
}
