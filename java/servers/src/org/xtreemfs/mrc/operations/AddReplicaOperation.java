/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_addRequest;

/**
 * 
 * @author stender
 */
public class AddReplicaOperation extends MRCOperation {
    
    public AddReplicaOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_replica_addRequest rqArgs = (xtreemfs_replica_addRequest) rq.getRequestArgs();
        
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
            
        } else
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                "either file ID or volume name + path required");
        
        if (file.isDirectory())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "replicas may only be added to files");
        
        if (sMan.getSoftlinkTarget(file.getId()) != null)
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "file '" + rqArgs.getFileId()
                + "' is a symbolic link");
        
        // check whether privileged permissions are granted for adding
        // replicas
        faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        Replica newRepl = rqArgs.getNewReplica();
        org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy sp = newRepl.getStripingPolicy();
        
        StripingPolicy sPol = sMan.createStripingPolicy(sp.getType().toString(), sp.getStripeSize(), sp
                .getWidth());
        
        // check whether the new replica relies on a set of OSDs which
        // hasn't been used yet
        XLocList xLocList = file.getXLocList();
        assert (xLocList != null);
        
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(xLocList.getReplUpdatePolicy()))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                "missing replica update policy - needs to be specified before adding replicas");
        
        if (!MRCHelper.isResolvable(newRepl.getOsdUuidsList()))
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                "replica contains unresolvable OSD UUIDs in '" + newRepl.getOsdUuidsList() + "'");
        
        if (xLocList.getReplica(0).getStripingPolicy().getStripeSize() != newRepl.getStripingPolicy()
                .getStripeSize())
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid stripe size; must be "
                + xLocList.getReplica(0).getStripingPolicy().getStripeSize());
        
        if (!MRCHelper.isAddable(xLocList, newRepl.getOsdUuidsList()))
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                "at least one OSD already used in current X-Locations list '"
                    + Converter.xLocListToXLocSet(xLocList).toString() + "'");
        
        // create a new replica and add it to the client's X-Locations list
        // (this will automatically increment the X-Locations list version)
        XLoc replica = sMan.createXLoc(sPol, newRepl.getOsdUuidsList().toArray(
            new String[newRepl.getOsdUuidsCount()]), newRepl.getReplicationFlags());
        
        XLoc[] repls = new XLoc[xLocList.getReplicaCount() + 1];
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            
            XLoc repl = xLocList.getReplica(i);
            
            // in case of read-only replication, mark the first replica in the
            // list as 'complete'
            if (i == 0 && ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(xLocList.getReplUpdatePolicy()))
                repl.setReplicationFlags(ReplicationFlags.setFullReplica(ReplicationFlags
                        .setReplicaIsComplete(repl.getReplicationFlags())));
            
            repls[i] = repl;
        }
        
        repls[repls.length - 1] = replica;
        xLocList = sMan.createXLocList(repls, xLocList.getReplUpdatePolicy(), xLocList.getVersion() + 1);
        file.setXLocList(xLocList);
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // update the X-Locations list
        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        
        // set the response
        rq.setResponse(emptyResponse.getDefaultInstance());
        
        update.execute();
    }
    
}
