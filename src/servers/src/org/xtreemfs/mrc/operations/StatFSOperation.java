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

import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.StatVFS;
import org.xtreemfs.interfaces.StatVFSSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.MRCInterface.statvfsRequest;
import org.xtreemfs.interfaces.MRCInterface.statvfsResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Converter;

/**
 * 
 * @author stender
 */
public class StatFSOperation extends MRCOperation {
    
    public StatFSOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final statvfsRequest rqArgs = (statvfsRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final StorageManager sMan = vMan.getStorageManagerByName(rqArgs.getVolume_name());
        
        StatVFS volumeInfo = getVolumeInfo(master, sMan);
        long knownEtag = rqArgs.getKnown_etag();
        
        StatVFSSet set = new StatVFSSet();
        if (knownEtag != volumeInfo.getEtag())
            set.add(volumeInfo);
        
        // set the response
        rq.setResponse(new statvfsResponse(set));
        finishRequest(rq);
        
    }
    
    protected static StatVFS getVolumeInfo(MRCRequestDispatcher master, StorageManager sMan) throws DatabaseException {
        
        final VolumeInfo volume = sMan.getVolumeInfo();
        final FileMetadata volumeRoot = sMan.getMetadata(1);
        
        int blockSize = sMan.getDefaultStripingPolicy(1).getStripeSize() * 1024;
        long bavail = master.getOSDStatusManager().getFreeSpace(volume.getId()) / blockSize;
        long used_blocks = volume.getVolumeSize() / blockSize;
        long blocks = bavail + used_blocks;
        String volumeId = volume.getId();
        AccessControlPolicyType acPolId = AccessControlPolicyType.parseInt(volume.getAcPolicyId());
        StripingPolicy defaultStripingPolicy = Converter.stripingPolicyToStripingPolicy(sMan
                .getDefaultStripingPolicy(1));
        String volumeName = volume.getName();
        String owningGroupId = volumeRoot.getOwningGroupId();
        String ownerId = volumeRoot.getOwnerId();
        int perms = volumeRoot.getPerms();
        
        long newEtag = blockSize + bavail + used_blocks + blocks;
        
        return new StatVFS(blockSize, bavail, blocks, volumeId, 1024, acPolId, defaultStripingPolicy,
            newEtag, perms, volumeName, owningGroupId, ownerId);
                
    }
}
