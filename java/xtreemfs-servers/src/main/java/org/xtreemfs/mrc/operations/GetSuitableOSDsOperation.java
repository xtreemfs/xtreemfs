/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_suitable_osdsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_suitable_osdsResponse;

/**
 * 
 * @author stender
 */
public class GetSuitableOSDsOperation extends MRCOperation {
    
    public GetSuitableOSDsOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_get_suitable_osdsRequest rqArgs = (xtreemfs_get_suitable_osdsRequest) rq
                .getRequestArgs();
        
        final FileAccessManager faMan = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();
        
        StorageManager sMan;
        String volumeId;
        FileMetadata file;
        if (rqArgs.hasFileId()) {
            
            // parse volume and file ID from global file ID
            GlobalFileIdResolver idRes = new GlobalFileIdResolver(rqArgs.getFileId());
            volumeId = idRes.getVolumeId();
            
            sMan = vMan.getStorageManager(idRes.getVolumeId());
            
            // retrieve the file metadata
            file = sMan.getMetadata(idRes.getLocalFileId());
            if (file == null)
                throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + rqArgs.getFileId()
                    + "' does not exist");
            
        } else if (rqArgs.hasVolumeName() && rqArgs.hasPath()) {
            
            final Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
            
            sMan = vMan.getStorageManagerByName(p.getComp(0));
            final PathResolver res = new PathResolver(sMan, p);
            
            res.checkIfFileDoesNotExist();
            file = res.getFile();
            
            // check whether the path prefix is searchable
            faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                    .getDetails().groupIds);
            
            volumeId = sMan.getVolumeInfo().getId();
            
        } else
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                "either file ID or volume name + path required");
        
        if (file.isDirectory())
            throw new UserException(POSIXErrno.POSIX_ERROR_EISDIR,
                    "xtreemfs_get_suitable_osds must be invoked on a file");

        String path = null;
        if (rqArgs.hasPath()) {
            path = rqArgs.getPath();
        }
        
        // retrieve the set of OSDs for the new replica
        ServiceSet.Builder usableOSDs = master.getOSDStatusManager().getUsableOSDs(volumeId,
            ((InetSocketAddress) rq.getRPCRequest().getSenderAddress()).getAddress(), null,
            file.getXLocList(), rqArgs.getNumOsds(), path);
        
        xtreemfs_get_suitable_osdsResponse.Builder resp = xtreemfs_get_suitable_osdsResponse.newBuilder();
        for (int i = 0; i < usableOSDs.getServicesCount(); i++)
            resp.addOsdUuids(usableOSDs.getServices(i).getUuid());
        
        // set the response
        rq.setResponse(resp.build());
        finishRequest(rq);
    }
    
}
