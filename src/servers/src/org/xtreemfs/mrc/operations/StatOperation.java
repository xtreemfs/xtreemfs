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

import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.MRCInterface.getattrRequest;
import org.xtreemfs.interfaces.MRCInterface.getattrResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;

/**
 * 
 * @author stender
 */
public class StatOperation extends MRCOperation {
    
    public StatOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final getattrRequest rqArgs = (getattrRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        final Path p = new Path(rqArgs.getPath());
        
        final StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        final PathResolver res = new PathResolver(sMan, p);
        final VolumeInfo volume = sMan.getVolumeInfo();
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        // retrieve and prepare the metadata to return
        FileMetadata file = res.getFile();
        
        String linkTarget = sMan.getSoftlinkTarget(file.getId());
        int mode = faMan.getPosixAccessMode(sMan, file, rq.getDetails().userId, rq.getDetails().groupIds);
        mode |= linkTarget != null ? Constants.SYSTEM_V_FCNTL_H_S_IFLNK
            : file.isDirectory() ? Constants.SYSTEM_V_FCNTL_H_S_IFDIR : Constants.SYSTEM_V_FCNTL_H_S_IFREG;
        long size = linkTarget != null ? linkTarget.length() : file.isDirectory() ? 0 : file.getSize();
        int blkSize = 0;
        if ( (linkTarget == null) && (!file.isDirectory()) ) {
            XLocList xlocList = file.getXLocList();
            if ((xlocList != null) && (xlocList.getReplicaCount() > 0))
                blkSize = xlocList.getReplica(0).getStripingPolicy().getStripeSize()*1024;
        }
        Stat stat = new Stat(volume.getId().hashCode(), file.getId(), mode, file.getLinkCount(), 1, 1, 0,
            size, blkSize, (long) file.getAtime() * (long) 1e9, (long) file.getMtime() * (long) 1e9, (long) file
                    .getCtime()
                * (long) 1e9, volume.getId() + ":" + file.getId(), file.getOwnerId(), file.getOwningGroupId(),
            linkTarget, file.isDirectory() ? 0 : file.getEpoch(),
            (int) file.getW32Attrs());
        
        // set the response
        rq.setResponse(new getattrResponse(stat));
        finishRequest(rq);
        
    }
    
}
