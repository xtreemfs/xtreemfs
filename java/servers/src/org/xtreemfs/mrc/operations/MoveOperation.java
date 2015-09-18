/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;

import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.quota.QuotaFileInformation;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.MRCHelper.FileType;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.renameRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.renameResponse;

/**
 * 
 * @author stender
 */
public class MoveOperation extends MRCOperation {
    
    public MoveOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);
        
        final renameRequest rqArgs = (renameRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        final Path sp = new Path(rqArgs.getVolumeName(), rqArgs.getSourcePath());
        
        final StorageManager sMan = vMan.getStorageManagerByName(sp.getComp(0));
        final PathResolver sRes = new PathResolver(sMan, sp);
        final VolumeInfo volume = sMan.getVolumeInfo();
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, sRes, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether the parent directory grants write access
        faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, sRes.getParentDir(), sRes
                .getParentsParentId(), rq.getDetails().userId, rq.getDetails().superUser,
            rq.getDetails().groupIds);
        
        Path tp = new Path(rqArgs.getVolumeName(), rqArgs.getTargetPath());
        
        // check arguments
        if (sp.getCompCount() == 1)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "cannot move a volume");
        
        if (!sp.getComp(0).equals(tp.getComp(0)))
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "cannot move between volumes");
        
        // check whether the file/directory exists
        sRes.checkIfFileDoesNotExist();
        
        // check whether the entry itself can be moved (this is e.g.
        // important w/ POSIX access control if the sticky bit is set)
        faMan.checkPermission(FileAccessManager.NON_POSIX_RM_MV_IN_DIR, sMan, sRes.getFile(), sRes
                .getParentDirId(), rq.getDetails().userId, rq.getDetails().superUser,
            rq.getDetails().groupIds);
        
        FileMetadata source = sRes.getFile();
        
        // find out what the source path refers to (1 = directory, 2 = file)
        FileType sourceType = source.isDirectory() ? FileType.dir : FileType.file;
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        PathResolver tRes = null;
        //        
        // // check if the file is a fuse-hidden file; if so, move it to the
        // // .fuse-hidden diretory
        // if (tp.getLastComp(0).startsWith(".fuse_hidden")) {
        //            
        // // generate the new path
        // tp = MRCHelper.getFuseHiddenPath(tp);
        //            
        // // check if the fuse-hidden directory exists
        // try {
        // tRes = new PathResolver(sMan, tp);
        //                
        // } catch (UserException exc) {
        //                
        // // if no fuse-hidden directory exists yet ...
        // if (exc.getErrno() == ErrNo.ENOENT) {
        //                    
        // // get the next free file ID
        // long fileId = sMan.getNextFileId();
        //                    
        // // create fuse-hidden directory
        // FileMetadata dir = sMan.createDir(fileId, 1, tp.getComp(1), 0, 0, 0,
        // "", "", 0777, 0, update);
        //                    
        // // set the file ID as the last one
        // sMan.setLastFileId(fileId, update);
        //
        // // re-initialize the path resolver
        // tRes = new PathResolver(tp, dir, null);
        // }
        //                
        // else
        // throw exc;
        // }
        //            
        // }
        //
        // // in the normal case, resolve the target directory
        // else
        tRes = new PathResolver(sMan, tp);
        
        FileMetadata targetParentDir = tRes.getParentDir();
        if (targetParentDir == null || !targetParentDir.isDirectory())
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOTDIR, "'"
                + tp.getComps(0, tp.getCompCount() - 2) + "' is not a directory");
        
        FileMetadata target = tRes.getFile();
        
        // find out what the target path refers to (0 = does not exist, 1 =
        // directory, 2 = file)
        FileType targetType = tp.getCompCount() == 1 ? FileType.dir : target == null ? FileType.nexists
            : target.isDirectory() ? FileType.dir : FileType.file;
        
        FileCredentials.Builder creds = null;
        
        // if both the old and the new directory point to the same
        // entity, do nothing
        if (sp.equals(tp)) {
            rq.setResponse(buildResponse(0, creds));
            finishRequest(rq);
            return;
        }
        
        // check whether the path prefix of the target file is
        // searchable
        faMan.checkSearchPermission(sMan, tRes, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether the parent directory of the target file grants
        // write access
        faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, tRes.getParentDir(), tRes
                .getParentsParentId(), rq.getDetails().userId, rq.getDetails().superUser,
            rq.getDetails().groupIds);
        
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        
        switch (sourceType) {
        
        // source is a directory
        case dir: {
            
            // check whether the target is a subdirectory of the
            // source directory; if so, throw an exception
            if (tp.isSubDirOf(sp))
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "cannot move '" + sp
                    + "' to one of its own subdirectories");
            
            switch (targetType) {
            
            case nexists: // target does not exist
            {
                // relink the metadata object to the parent directory of
                // the target path and remove the former link
                relink(sMan, sRes.getParentDirId(), sRes.getFileName(), source, tRes.getParentDirId(), tRes
                        .getFileName(), update);
                
                break;
            }
                
            case dir: // target is a directory
            {
                
                // check whether the target directory may be overwritten; if
                // so, delete it
                faMan.checkPermission(FileAccessManager.NON_POSIX_DELETE, sMan, target,
                    tRes.getParentDirId(), rq.getDetails().userId, rq.getDetails().superUser,
                    rq.getDetails().groupIds);
                
                DatabaseResultSet<FileMetadata> children = sMan.getChildren(target.getId(), 0, Integer.MAX_VALUE);
                boolean hasChildren = children.hasNext();
                children.destroy();
                
                if (hasChildren)
                    throw new UserException(POSIXErrno.POSIX_ERROR_ENOTEMPTY, "target directory '" + tRes
                        + "' is not empty");
                else {
                    // cause it's a directory, no quota information has to be adjusted

                    // delete file
                    sMan.delete(tRes.getParentDirId(), tRes.getFileName(), update);
                }
                
                // relink the metadata object to the parent directory of
                // the target path and remove the former link
                relink(sMan, sRes.getParentDirId(), sRes.getFileName(), source, tRes.getParentDirId(), tRes
                        .getFileName(), update);
                
                break;
            }
                
            case file: // target is a file
                throw new UserException(POSIXErrno.POSIX_ERROR_ENOTDIR, "cannot rename directory '" + sRes
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
                relink(sMan, sRes.getParentDirId(), sRes.getFileName(), source, tRes.getParentDirId(), tRes
                        .getFileName(), update);
                
                break;
            }
                
            case dir: // target is a directory
            {
                throw new UserException(POSIXErrno.POSIX_ERROR_EISDIR, "cannot rename file '" + sRes
                    + "' to directory '" + tRes + "'");
            }
                
            case file: // target is a file
            {

                // check whether the target file may be overwritten
                // important w/ POSIX access control if the sticky bit is set)
                faMan.checkPermission(FileAccessManager.NON_POSIX_RM_MV_IN_DIR, sMan, tRes.getFile(), tRes
                        .getParentDirId(), rq.getDetails().userId, rq.getDetails().superUser,
                    rq.getDetails().groupIds);
                
                // if the file is not a symbolic link, and unless there is still
                // another link to the target file, i.e. the target file must
                // not be deleted yet, create a 'delete' capability and include
                // the XCapability and XLocationsList headers in the response
                if (sMan.getSoftlinkTarget(target.getId()) == null && target.getLinkCount() == 1) {
                    
                    // create a deletion capability for the file
                    Capability cap = new Capability(MRCHelper.createGlobalFileId(volume, target),
                        FileAccessManager.NON_POSIX_DELETE, master.getConfig().getCapabilityTimeout(),
                        Integer.MAX_VALUE, ((InetSocketAddress) rq.getRPCRequest().getSenderAddress())
                                .getAddress().getHostAddress(), target.getEpoch(), false, !volume
                                .isSnapshotsEnabled() ? SnapConfig.SNAP_CONFIG_SNAPS_DISABLED : volume
                                .isSnapVolume() ? SnapConfig.SNAP_CONFIG_ACCESS_SNAP
                            : SnapConfig.SNAP_CONFIG_ACCESS_CURRENT, volume.getCreationTime(), master
                                .getConfig().getCapabilitySecret());
                    
                    creds = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(
                        Converter.xLocListToXLocSet(target.getXLocList()));
                }
                
                // delete quota information
                FileMetadata metadata = sMan.getMetadata(tRes.getParentDirId(), tRes.getFileName());
                QuotaFileInformation quotaFileInformation = new QuotaFileInformation(volume.getId(), metadata);
                master.getMrcVoucherManager().deleteFile(quotaFileInformation, update);

                // delete the target
                sMan.delete(tRes.getParentDirId(), tRes.getFileName(), update);
                
                // relink the metadata object to the parent directory of
                // the target path and remove the former link
                relink(sMan, sRes.getParentDirId(), sRes.getFileName(), source, tRes.getParentDirId(), tRes
                        .getFileName(), update);
                
                break;
            }
                
            }
            
        }
        }
        
        // update POSIX timestamps of source and target parent directories
        MRCHelper.updateFileTimes(sRes.getParentsParentId(), sRes.getParentDir(), false, true, true, sMan,
            time, update);
        MRCHelper.updateFileTimes(tRes.getParentsParentId(), tRes.getParentDir(), false, true, true, sMan,
            time, update);
        
        // set the response
        rq.setResponse(buildResponse(time, creds));
        
        update.execute();
        
    }
    
    private static void relink(StorageManager sMan, long sourceParentDirId, String sourceFileName,
        FileMetadata source, long targetParentDirId, String targetFileName, AtomicDBUpdate update)
        throws DatabaseException {
        
        short newLinkCount = sMan.unlink(sourceParentDirId, sourceFileName, update);
        source.setLinkCount(newLinkCount);
        source.setCtime((int) (TimeSync.getGlobalTime() / 1000));
        sMan.link(source, targetParentDirId, targetFileName, update);
    }
    
    private static renameResponse buildResponse(int time, FileCredentials.Builder creds) {
        
        renameResponse.Builder resp = renameResponse.newBuilder();
        if (creds != null)
            resp.setCreds(creds);
        resp.setTimestampS(time);
        
        return resp.build();
    }
    
}
