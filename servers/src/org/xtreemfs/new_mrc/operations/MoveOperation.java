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
import org.xtreemfs.new_mrc.operations.MRCOpHelper.FileType;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class MoveOperation extends MRCOperation {
    
    static class Args {
        
        public String sourcePath;
        
        public String targetPath;
        
    }
    
    public static final String RPC_NAME = "move";
    
    public MoveOperation(MRCRequestDispatcher master) {
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
            
            final Path sp = new Path(rqArgs.sourcePath);
            
            final VolumeInfo volume = vMan.getVolumeByName(sp.getComp(0));
            final StorageManager sMan = vMan.getStorageManager(volume.getId());
            final PathResolver sRes = new PathResolver(sMan, sp);
            
            // check whether the path prefix is searchable
            faMan.checkSearchPermission(sMan, sRes.getPathPrefix(), rq.getDetails().userId, rq
                    .getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the parent directory grants write access
            faMan.checkPermission(FileAccessManager.WRITE_ACCESS, sMan, sRes.getParentDir(), sRes
                    .getParentsParentId(), rq.getDetails().userId, rq.getDetails().superUser, rq
                    .getDetails().groupIds);
            
            final Path tp = new Path(rqArgs.targetPath);
            
            // check arguments
            if (sp.getCompCount() == 1)
                throw new UserException(ErrNo.ENOENT, "cannot move a volume");
            
            if (!sp.getComp(0).equals(tp.getComp(0)))
                throw new UserException(ErrNo.ENOENT, "cannot move between volumes");
            
            // check whether the file/directory exists
            sRes.checkIfFileDoesNotExist();
            
            // check whether the entry itself can be moved (this is e.g.
            // important w/ POSIX access control if the sticky bit is set)
            faMan.checkPermission(FileAccessManager.RM_MV_IN_DIR_ACCESS, sMan, sRes.getFile(), sRes
                    .getParentDirId(), rq.getDetails().userId, rq.getDetails().superUser, rq
                    .getDetails().groupIds);
            
            FileMetadata source = sRes.getFile();
            
            // find out what the source path refers to (1 = directory, 2 = file)
            FileType sourceType = source.isDirectory() ? FileType.dir : FileType.file;
            
            final PathResolver tRes = new PathResolver(sMan, tp);
            
            FileMetadata targetParentDir = tRes.getParentDir();
            if (!targetParentDir.isDirectory())
                throw new UserException(ErrNo.ENOTDIR, "'" + tp.getComps(0, tp.getCompCount() - 2)
                    + "' is not a directory");
            
            FileMetadata target = tRes.getFile();
            
            // find out what the target path refers to (0 = does not exist, 1 =
            // directory, 2 = file)
            FileType targetType = tp.getCompCount() == 1 ? FileType.dir
                : target == null ? FileType.nexists : target.isDirectory() ? FileType.dir
                    : FileType.file;
            
            // if both the old and the new directory point to the same
            // entity, do nothing
            if (sp.equals(tp)) {
                finishRequest(rq);
                return;
            }
            
            // check whether the path prefix of the target file is
            // searchable
            faMan.checkSearchPermission(sMan, tRes.getPathPrefix(), rq.getDetails().userId, rq
                    .getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the parent directory of the target file grants
            // write access
            faMan.checkPermission(FileAccessManager.WRITE_ACCESS, sMan, tRes.getParentDir(), tRes
                    .getParentsParentId(), rq.getDetails().userId, rq.getDetails().superUser, rq
                    .getDetails().groupIds);
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            switch (sourceType) {
            
            // source is a directory
            case dir: {
                
                // check whether the target is a subdirectory of the
                // source directory; if so, throw an exception
                if (tp.isSubDirOf(sp))
                    throw new UserException(ErrNo.EINVAL, "cannot move '" + sp
                        + "' to one of its own subdirectories");
                
                switch (targetType) {
                
                case nexists: // target does not exist
                {
                    // relink the metadata object to the parent directory of
                    // the target path and remove the former link
                    sMan.link(sRes.getParentDirId(), sRes.getFileName(), tRes.getParentDirId(),
                        tRes.getFileName(), update);
                    sMan.delete(sRes.getParentDirId(), sRes.getFileName(), update);
                    
                    break;
                }
                    
                case dir: // target is a directory
                {
                    
                    // check whether the target directory may be overwritten; if
                    // so, delete it
                    faMan.checkPermission(FileAccessManager.DELETE_ACCESS, sMan, target, tRes
                            .getParentDirId(), rq.getDetails().userId, rq.getDetails().superUser,
                        rq.getDetails().groupIds);
                    
                    if (sMan.getChildren(target.getId()).hasNext())
                        throw new UserException(ErrNo.ENOTEMPTY, "target directory '" + tRes
                            + "' is not empty");
                    else
                        sMan.delete(tRes.getParentDirId(), tRes.getFileName(), update);
                    
                    // relink the metadata object to the parent directory of
                    // the target path and remove the former link
                    sMan.link(sRes.getParentDirId(), sRes.getFileName(), tRes.getParentDirId(),
                        tRes.getFileName(), update);
                    sMan.delete(sRes.getParentDirId(), sRes.getFileName(), update);
                    
                    break;
                }
                    
                case file: // target is a file
                    throw new UserException(ErrNo.ENOTDIR, "cannot rename directory '" + sRes
                        + "' to file '" + tRes + "'");
                    
                }
                
                break;
                
            }
                
                // source is a file
            case file: {
                
                switch (targetType) {
                
                case nexists: // target does not exist
                {
                    
                    // relink the metadata object to the parent directory of
                    // the target path and remove the former link
                    sMan.link(sRes.getParentDirId(), sRes.getFileName(), tRes.getParentDirId(),
                        tRes.getFileName(), update);
                    sMan.delete(sRes.getParentDirId(), sRes.getFileName(), update);
                    
                    break;
                }
                    
                case dir: // target is a directory
                {
                    throw new UserException(ErrNo.EISDIR, "cannot rename file '" + sRes
                        + "' to directory '" + tRes + "'");
                }
                    
                case file: // target is a file
                {
                    
                    // obtain a deletion capability for the file
                    String aMode = faMan.translateAccessMode(volume.getId(),
                        FileAccessManager.DELETE_ACCESS);
                    
                    // unless there is still another link to the target file,
                    // i.e. the target file must not be deleted yet, create a
                    // 'delete' capability and include the XCapability and
                    // XLocationsList headers in the response
                    if (target.getLinkCount() == 1) {
                        
                        String capability = MRCOpHelper.createCapability(aMode, volume.getId(),
                            target.getId(), Integer.MAX_VALUE,
                            master.getConfig().getCapabilitySecret()).toString();
                        
                        HTTPHeaders xCapHeaders = MRCOpHelper.createXCapHeaders(capability, target
                                .getXLocList());
                        
                        rq.setAdditionalResponseHeaders(xCapHeaders);
                    }
                    
                    // delete the target
                    sMan.delete(tRes.getParentDirId(), tRes.getFileName(), update);
                    
                    // relink the metadata object to the parent directory of
                    // the target path and remove the former link
                    sMan.link(sRes.getParentDirId(), sRes.getFileName(), tRes.getParentDirId(),
                        tRes.getFileName(), update);
                    sMan.delete(sRes.getParentDirId(), sRes.getFileName(), update);
                    
                    break;
                }
                    
                }
                
            }
            }
            
            // update POSIX timestamps of parent directories
            MRCOpHelper.updateFileTimes(sRes.getParentsParentId(), sRes.getParentDir(), false,
                true, true, sMan, update);
            MRCOpHelper.updateFileTimes(tRes.getParentsParentId(), tRes.getParentDir(), false,
                true, true, sMan, update);
            
            // FIXME: this line is needed due to a BUG in the client which
            // expects some useless return value
            rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(null).getBytes()));
            
            update.execute();
            
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
            
            args.sourcePath = (String) arguments.get(0);
            args.targetPath = (String) arguments.get(1);
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
