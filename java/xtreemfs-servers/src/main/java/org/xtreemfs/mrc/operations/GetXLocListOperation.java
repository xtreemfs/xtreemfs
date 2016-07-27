/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_listRequest;

/**
 * @deprecated Replaced by {@link GetXLocSetOperation}
 * @author stender
 */
@Deprecated
public class GetXLocListOperation extends MRCOperation {
    
    public GetXLocListOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_replica_listRequest rqArgs = (xtreemfs_replica_listRequest) rq.getRequestArgs();
        
        final FileAccessManager faMan = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();
        
        validateContext(rq);
        
        StorageManager sMan = null;
        FileMetadata file = null;
        
        if (rqArgs.hasFileId()) {
            
            // parse volume and file ID from global file ID
            GlobalFileIdResolver idRes = new GlobalFileIdResolver(rqArgs.getFileId());
            
            sMan = vMan.getStorageManager(idRes.getVolumeId());
            
            // retrieve the file metadata
            file = sMan.getMetadata(idRes.getLocalFileId());
            if (file == null)
                throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + idRes.getLocalFileId()
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
            
        } else
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                "either file ID or volume name + path required");
        
        if (file.isDirectory())
            throw new UserException(POSIXErrno.POSIX_ERROR_EISDIR, file.getId() + " is a directory");
        
        if (sMan.getSoftlinkTarget(file.getId()) != null)
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "file '" + rqArgs.getFileId()
                + "' is a symbolic link");
        
        XLocList xloc = file.getXLocList();
        assert (xloc != null);
        
        // get the replicas from the X-Loc list
        Replicas.Builder replicas = Replicas.newBuilder();
        for (Replica repl : Converter.xLocListToXLocSet(xloc).getReplicasList())
            replicas.addReplicas(repl);
        
        // set the response
        rq.setResponse(replicas.build());
        finishRequest(rq);
    }
    
}
