/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

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
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrResponse;

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
        
        Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        
        // if(p.getLastComp(0).startsWith(".fuse_hidden"))
        // p = MRCHelper.getFuseHiddenPath(p);
        
        final StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        final PathResolver res = new PathResolver(sMan, p);
        final VolumeInfo volume = sMan.getVolumeInfo();
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        FileMetadata file = res.getFile();
        
        final long knownEtag = rqArgs.getKnownEtag();
        final long newEtag = file.getCtime() + file.getMtime();
        
        getattrResponse.Builder stat = getattrResponse.newBuilder();
        
        // retrieve and prepare the metadata to return
        if (knownEtag != newEtag) {
            
            String linkTarget = sMan.getSoftlinkTarget(file.getId());
            int mode = faMan.getPosixAccessMode(sMan, file, rq.getDetails().userId, rq.getDetails().groupIds);
            mode |= linkTarget != null ? GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFLNK.getNumber()
                : file.isDirectory() ? GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()
                    : ((file.getPerms() & GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFIFO.getNumber()) != 0) ? GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFIFO
                            .getNumber()
                        : GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFREG.getNumber();
            
            long size = linkTarget != null ? linkTarget.length() : file.isDirectory() ? 0 : file.getSize();
            int blkSize = 0;
            if ((linkTarget == null) && (!file.isDirectory())) {
                XLocList xlocList = file.getXLocList();
                if ((xlocList != null) && (xlocList.getReplicaCount() > 0))
                    blkSize = xlocList.getReplica(0).getStripingPolicy().getStripeSize() * 1024;
            }
            
            stat.setStbuf(Stat.newBuilder().setDev(volume.getId().hashCode()).setIno(file.getId()).setMode(
                mode).setNlink(file.getLinkCount()).setUserId(file.getOwnerId()).setGroupId(
                file.getOwningGroupId()).setSize(size).setAtimeNs((long) file.getAtime() * (long) 1e9)
                    .setCtimeNs((long) file.getCtime() * (long) 1e9).setMtimeNs(
                        (long) file.getMtime() * (long) 1e9).setBlksize(blkSize).setTruncateEpoch(
                        file.isDirectory() ? 0 : file.getEpoch()).setAttributes((int) file.getW32Attrs())
                    .setEtag(newEtag));
            
        }
        
        // set the response
        rq.setResponse(stat.build());
        
        finishRequest(rq);
        
    }
    
}
