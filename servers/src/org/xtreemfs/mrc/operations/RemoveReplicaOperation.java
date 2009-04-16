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

package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_removeRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_removeResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class RemoveReplicaOperation extends MRCOperation {
    
    public static final int OP_ID = 27;
    
    public RemoveReplicaOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            final xtreemfs_replica_removeRequest rqArgs = (xtreemfs_replica_removeRequest) rq
                    .getRequestArgs();
            
            final FileAccessManager faMan = master.getFileAccessManager();
            final VolumeManager vMan = master.getVolumeManager();
            
            validateContext(rq);
            
            // parse volume and file ID from global file ID
            GlobalFileIdResolver idRes = new GlobalFileIdResolver(rqArgs.getFile_id());
            
            StorageManager sMan = vMan.getStorageManager(idRes.getVolumeId());
            
            // retrieve the file metadata
            FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file '" + rqArgs.getFile_id() + "' does not exist");
            
            // if the file refers to a symbolic link, resolve the link
            String target = sMan.getSoftlinkTarget(file.getId());
            if (target != null) {
                String path = target;
                Path p = new Path(path);
                
                // if the local MRC is not responsible, send a redirect
                if (!vMan.hasVolume(p.getComp(0))) {
                    finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrNo.ENOENT, "link target "
                        + target + " does not exist"));
                    return;
                }
                
                VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
                sMan = vMan.getStorageManager(volume.getId());
                PathResolver res = new PathResolver(sMan, p);
                file = res.getFile();
            }
            
            if (file.isDirectory())
                throw new UserException(ErrNo.EPERM, "replicas may only be removed from files");
            
            // check whether privileged permissions are granted for removing
            // replicas
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);
            
            XLocList xLocList = file.getXLocList();
            
            // find and remove the replica from the X-Locations list
            int i = 0;
            for (; i < xLocList.getReplicaCount(); i++) {
                
                XLoc replica = xLocList.getReplica(i);
                
                // compare the first elements from the lists; since an OSD may
                // only occur once in each X-Locations list, it is not necessary
                // to go through the entire list
                if (replica.getOSD(0).equals(rqArgs.getOsd_uuid()))
                    break;
            }
            
            // create and assign a new X-Locations list that excludes the
            // replica to remove
            XLoc[] newReplList = new XLoc[xLocList.getReplicaCount() - 1];
            for (int j = 0, count = 0; j < xLocList.getReplicaCount(); j++)
                if (j != i)
                    newReplList[count++] = xLocList.getReplica(j);
            xLocList = sMan.createXLocList(newReplList, xLocList.getReplUpdatePolicy(),
                xLocList.getVersion() + 1);
            file.setXLocList(xLocList);
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            // update the X-Locations list
            sMan.setMetadata(file, FileMetadata.XLOC_METADATA, update);
            
            // create a deletion capability for the replica
            Capability deleteCap = new Capability(idRes.getVolumeId() + ":" + file.getId(),
                FileAccessManager.NON_POSIX_DELETE, Integer.MAX_VALUE, ((InetSocketAddress) rq
                        .getRPCRequest().getClientIdentity()).getAddress().getHostAddress(), file.getEpoch(),
                master.getConfig().getCapabilitySecret());
            
            // set the response
            rq.setResponse(new xtreemfs_replica_removeResponse(deleteCap.getXCap()));
            
            update.execute();
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        }
    }
    
}
