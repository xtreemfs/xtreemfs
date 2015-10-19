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
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.quota.QuotaFileInformation;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.rmdirRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.timestampResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.unlinkRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.unlinkResponse;

/**
 * 
 * @author stender
 */
public class DeleteOperation extends MRCOperation {
    
    public DeleteOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        Path p = new Path((rq.getRequestArgs() instanceof unlinkRequest ? ((unlinkRequest) rq
                .getRequestArgs()).getVolumeName() : ((rmdirRequest) rq.getRequestArgs()).getVolumeName()),
            (rq.getRequestArgs() instanceof unlinkRequest ? ((unlinkRequest) rq.getRequestArgs()).getPath()
                : ((rmdirRequest) rq.getRequestArgs()).getPath()));
        
        final StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        final PathResolver res = new PathResolver(sMan, p);
        final VolumeInfo volume = sMan.getVolumeInfo();
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether the parent directory grants write access
        faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, res.getParentDir(), 0,
            rq.getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // check whether the file/directory exists
        res.checkIfFileDoesNotExist();
        
        FileMetadata file = res.getFile();
        
        // check whether the entry itself can be deleted (this is e.g.
        // important w/ POSIX access control if the sticky bit is set)
        faMan.checkPermission(FileAccessManager.NON_POSIX_RM_MV_IN_DIR, sMan, file, res.getParentDirId(), rq
                .getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        DatabaseResultSet<FileMetadata> children = sMan.getChildren(file.getId(), 0, Integer.MAX_VALUE);
        boolean hasChildren = children.hasNext();
        children.destroy();
        
        if (file.isDirectory() && hasChildren)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOTEMPTY, "'" + p + "' is not empty");
        
        FileCredentials.Builder creds = null;
        
        // unless the file is a directory, retrieve X-headers for file
        // deletion on OSDs; if the request was authorized before,
        // assume that a capability has been issued already.
        if (!file.isDirectory()) {
            
            // create a deletion capability for the file
            Capability cap = new Capability(MRCHelper.createGlobalFileId(volume, file),
                FileAccessManager.NON_POSIX_DELETE, master.getConfig().getCapabilityTimeout(),
                Integer.MAX_VALUE, ((InetSocketAddress) rq.getRPCRequest().getSenderAddress()).getAddress()
                        .getHostAddress(), file.getEpoch(), false,
                !volume.isSnapshotsEnabled() ? SnapConfig.SNAP_CONFIG_SNAPS_DISABLED
                    : volume.isSnapVolume() ? SnapConfig.SNAP_CONFIG_ACCESS_SNAP
                        : SnapConfig.SNAP_CONFIG_ACCESS_CURRENT, volume.getCreationTime(), master.getConfig()
                        .getCapabilitySecret());
            
            // set the XCapability and XLocationsList headers
            XLocList xloc = file.getXLocList();
            if (xloc != null && xloc.getReplicaCount() > 0)
                creds = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(
                    Converter.xLocListToXLocSet(xloc));
        }
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // unlink the file; if there are still links to the file, reset the
        // X-headers to null, as the file content must not be deleted
        sMan.delete(res.getParentDirId(), res.getFileName(), update);
        if (file.getLinkCount() > 1)
            creds = null;
        
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        
        // update POSIX timestamps of parent directory
        MRCHelper.updateFileTimes(res.getParentsParentId(), res.getParentDir(), false, true, true, sMan,
            time, update);
        
        if (file.getLinkCount() > 1)
            MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, time, update);
        
        // remove file size from quota manager
        if (!file.isDirectory() && file.getSize() > 0 && file.getXLocList() != null && file.getLinkCount() == 1) {
            QuotaFileInformation quotaFileInformation = new QuotaFileInformation(volume.getId(), file);
            master.getMrcVoucherManager().deleteFile(quotaFileInformation, update);
        }

        // set the response, depending on whether the request was for
        // deleting a
        // file or directory
        if (rq.getRequestArgs() instanceof unlinkRequest) {
            
            unlinkResponse.Builder builder = unlinkResponse.newBuilder().setTimestampS(time);
            if (creds != null)
                builder.setCreds(creds);
            rq.setResponse(builder.build());
        }

        else {
            rq.setResponse(timestampResponse.newBuilder().setTimestampS(time).build());
        }
        
        update.execute();
        
    }
}
