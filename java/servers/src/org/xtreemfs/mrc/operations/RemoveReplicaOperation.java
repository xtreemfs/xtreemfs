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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_removeRequest;

/**
 * 
 * @author stender
 */
public class RemoveReplicaOperation extends MRCOperation {
    
    public RemoveReplicaOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_replica_removeRequest rqArgs = (xtreemfs_replica_removeRequest) rq.getRequestArgs();
        
        final FileAccessManager faMan = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();
        
        validateContext(rq);
        
        StorageManager sMan;
        FileMetadata file;
        String volumeId;
        
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
            volumeId = sMan.getVolumeInfo().getId();
            
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
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "replicas may only be removed from files");
        
        if (sMan.getSoftlinkTarget(file.getId()) != null)
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "file '" + rqArgs.getFileId()
                + "' is a symbolic link");
        
        // check whether privileged permissions are granted for removing
        // replicas
        faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        XLocList oldXLocList = file.getXLocList();
        assert (oldXLocList != null);
        
        // find and remove the replica from the X-Locations list
        int i = 0;
        for (; i < oldXLocList.getReplicaCount(); i++) {
            
            XLoc replica = oldXLocList.getReplica(i);
            
            // compare the first elements from the lists; since an OSD may
            // only occur once in each X-Locations list, it is not necessary
            // to go through the entire list
            if (replica.getOSD(0).equals(rqArgs.getOsdUuid()))
                break;
        }
        
        // create and assign a new X-Locations list that excludes the
        // replica to remove
        XLoc[] newReplList = new XLoc[oldXLocList.getReplicaCount() - 1];
        for (int j = 0, count = 0; j < oldXLocList.getReplicaCount(); j++)
            if (j != i)
                newReplList[count++] = oldXLocList.getReplica(j);
        XLocList newXLocList = sMan.createXLocList(newReplList, oldXLocList.getReplUpdatePolicy(),
            oldXLocList.getVersion() + 1);
        file.setXLocList(newXLocList);
        
        // remove the read-only flag if only one replica remains
        if (newXLocList.getReplicaCount() == 1)
            file.setReadOnly(false);
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // update the X-Locations list
        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        
        // create a deletion capability for the replica
        Capability deleteCap = new Capability(volumeId + ":" + file.getId(),
            FileAccessManager.NON_POSIX_DELETE, master.getConfig().getCapabilityTimeout(), Integer.MAX_VALUE,
            ((InetSocketAddress) rq.getRPCRequest().getSenderAddress()).getAddress().getHostAddress(), file
                    .getEpoch(), false,
            !sMan.getVolumeInfo().isSnapshotsEnabled() ? SnapConfig.SNAP_CONFIG_SNAPS_DISABLED : sMan
                    .getVolumeInfo().isSnapVolume() ? SnapConfig.SNAP_CONFIG_ACCESS_SNAP
                : SnapConfig.SNAP_CONFIG_ACCESS_CURRENT, sMan.getVolumeInfo().getCreationTime(), master
                    .getConfig().getCapabilitySecret());
        
        // convert xloc list
        XLocSet.Builder xLocSet = Converter.xLocListToXLocSet(oldXLocList);
        
        // wrap xcap and xloc list
        FileCredentials fc = FileCredentials.newBuilder().setXcap(deleteCap.getXCap()).setXlocs(xLocSet)
                .build();
        
        // set the response
        rq.setResponse(fc);
        
        update.execute();
    }
    
}
