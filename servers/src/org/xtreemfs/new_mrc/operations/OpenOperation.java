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

package org.xtreemfs.new_mrc.operations;

import java.util.List;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.ac.FileAccessManager;
import org.xtreemfs.new_mrc.dbaccess.AtomicDBUpdate;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.XLocList;
import org.xtreemfs.new_mrc.operations.MRCOpHelper.AccessMode;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class OpenOperation extends MRCOperation {
    
    static class Args {
        
        public String path;
        
        public String accessMode;
        
    }
    
    public static final String RPC_NAME = "open";
    
    public OpenOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public boolean hasArguments() {
        return true;
    }
    
    @Override
    public boolean isAuthRequired() {
        return true;
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            Args rqArgs = (Args) rq.getRequestArgs();
            
            final VolumeManager vMan = master.getVolumeManager();
            final FileAccessManager faMan = master.getFileAccessManager();
            
            Path p = new Path(rqArgs.path);
            
            VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
            StorageManager sMan = vMan.getStorageManager(volume.getId());
            PathResolver res = new PathResolver(sMan, p);
            
            // check whether the path prefix is searchable
            faMan.checkSearchPermission(sMan, res.getPathPrefix(), rq.getDetails().userId, rq
                    .getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the file/directory exists
            res.checkIfFileDoesNotExist();
            
            FileMetadata file = res.getFile();
            
            // if the file refers to a symbolic link, resolve the link
            String target = sMan.getSoftlinkTarget(file.getId());
            if (target != null) {
                rqArgs.path = target;
                p = new Path(rqArgs.path);
                
                // if the local MRC is not responsible, send a redirect
                if (!vMan.hasVolume(p.getComp(0))) {
                    finishRequest(rq, new ErrorRecord(ErrorClass.REDIRECT, target));
                    return;
                }
                
                volume = vMan.getVolumeByName(p.getComp(0));
                sMan = vMan.getStorageManager(volume.getId());
                res = new PathResolver(sMan, p);
                file = res.getFile();
            }
            
            if (file.isDirectory())
                throw new UserException(ErrNo.EISDIR, "open is restricted to files");
            
            AccessMode mode = null;
            try {
                mode = AccessMode.valueOf(rqArgs.accessMode);
            } catch (IllegalArgumentException exc) {
                throw new UserException(ErrNo.EINVAL, "invalid access mode for 'open': "
                    + rqArgs.accessMode);
            }
            
            // check whether the file is marked as 'read-only'; in this
            // case, throw an exception if write access is requested
            if ((mode == AccessMode.w || mode == AccessMode.a || mode == AccessMode.ga || mode == AccessMode.t)
                && file.isReadOnly())
                throw new UserException(ErrNo.EPERM, "read-only files cannot be written");
            
            AtomicDBUpdate update = null;
            
            // get the current epoch, use (and increase) the truncate number if
            // the open mode is truncate
            if (mode == AccessMode.t) {
                update = sMan.createAtomicDBUpdate(master, rq);
                file.setIssuedEpoch(file.getIssuedEpoch() + 1);
                sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            }
            
            // check whether the permission is granted
            faMan.checkPermission(rqArgs.accessMode, sMan, file, res.getParentDirId(), rq
                    .getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
            
            // create the capability
            String capability = MRCOpHelper.createCapability(rqArgs.accessMode, volume.getId(),
                file.getId(), file.getIssuedEpoch(), master.getConfig().getCapabilitySecret())
                    .toString();
            
            // get the list of replicas associated with the file
            XLocList xLocList = file.getXLocList();
            
            // if no replica exists yet, create one using the default striping
            // policy together with a set of feasible OSDs from the OSD status
            // manager
            if (xLocList == null || !xLocList.iterator().hasNext()) {
                
                xLocList = MRCOpHelper.createXLocList(xLocList, sMan, master.getOSDStatusManager(),
                    res.toString(), file.getId(), res.getParentDirId(), volume, rq
                            .getPinkyRequest().getClientAddress());
                
                file.setXLocList(xLocList);
                
                if (update == null)
                    update = sMan.createAtomicDBUpdate(master, rq);
                sMan.setMetadata(file, FileMetadata.XLOC_METADATA, update);
            }
            
            HTTPHeaders headers = MRCOpHelper.createXCapHeaders(capability, xLocList);
            rq.setAdditionalResponseHeaders(headers);
            
            // FIXME: this line is needed due to a BUG in the client which
            // expects some useless return value
            rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(null).getBytes()));
            
            if (update == null)
                update = sMan.createAtomicDBUpdate(master, rq);
            
            // update POSIX timestamps of parent directory
            if (!master.getConfig().isNoAtime())
                MRCOpHelper.updateFileTimes(res.getParentsParentId(), res.getParentDir(), true,
                    false, false, sMan, update);
            
            if (update != null)
                update.execute();
            else
                finishRequest(rq);
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc
                    .getMessage(), exc));
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
                "an error has occurred", exc));
        }
    }
    
    @Override
    public ErrorRecord parseRPCBody(MRCRequest rq, List<Object> arguments) {
        
        Args args = new Args();
        
        try {
            
            args.path = (String) arguments.get(0);
            args.accessMode = (String) arguments.get(1);
            if (arguments.size() == 2)
                return null;
            
            throw new Exception();
            
        } catch (Exception exc) {
            try {
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "': " + JSONParser.writeJSON(arguments));
            } catch (JSONException je) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "'");
            }
        } finally {
            rq.setRequestArgs(args);
        }
    }
    
}
