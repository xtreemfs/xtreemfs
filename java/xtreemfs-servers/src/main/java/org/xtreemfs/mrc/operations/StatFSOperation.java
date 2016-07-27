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
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.statvfsRequest;

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
        final StorageManager sMan = vMan.getStorageManagerByName(rqArgs.getVolumeName());
        
        StatVFS volumeInfo = getVolumeInfo(master, sMan);
        // long knownEtag = rqArgs.getKnownEtag();
        
        // StatVFSSet set = new StatVFSSet();
        // if (knownEtag != volumeInfo.getEtag())
        // set.add(volumeInfo);
        
        // set the response
        rq.setResponse(volumeInfo);
        finishRequest(rq);
        
    }
    
    protected static StatVFS getVolumeInfo(MRCRequestDispatcher master, StorageManager sMan)
        throws DatabaseException {
        
        final VolumeInfo volume = sMan.getVolumeInfo();
        final FileMetadata volumeRoot = sMan.getMetadata(1);
        
        int blockSize = sMan.getDefaultStripingPolicy(1).getStripeSize() * 1024;

        long availSpace = master.getOSDStatusManager().getUsableSpace(volume.getId());
        long freeSpace = master.getOSDStatusManager().getFreeSpace(volume.getId());
        long totalSpace = master.getOSDStatusManager().getTotalSpace(volume.getId());
        long quota = sMan.getVolumeQuota();

        // Use minimum of free space relative to the quota and free space on OSDs as free/available space.
        if (quota != 0) {
            long quotaFreeSpace = quota - sMan.getVolumeInfo().getVolumeSize();
            quotaFreeSpace = quotaFreeSpace < 0 ? 0 : quotaFreeSpace;
            freeSpace = freeSpace < quotaFreeSpace ? freeSpace : quotaFreeSpace;
            availSpace = availSpace < quotaFreeSpace ? availSpace : quotaFreeSpace;
        }

        long bavail = availSpace / blockSize;
        long bfree = freeSpace / blockSize;
        long blocks = totalSpace / blockSize;
        String volumeId = volume.getId();
        AccessControlPolicyType acPolId = AccessControlPolicyType.valueOf(volume.getAcPolicyId());
        StripingPolicy.Builder defaultStripingPolicy = Converter.stripingPolicyToStripingPolicy(sMan
                .getDefaultStripingPolicy(1));
        String volumeName = volume.getName();
        String owningGroupId = volumeRoot.getOwningGroupId();
        String ownerId = volumeRoot.getOwnerId();
        int perms = volumeRoot.getPerms();
        
        long newEtag = blockSize + bavail + blocks;
        
        return StatVFS.newBuilder().setBsize(blockSize).setBfree(bfree).setBavail(bavail).setBlocks(blocks).setFsid(volumeId)
                .setNamemax(1024).setOwnerUserId(ownerId).setOwnerGroupId(owningGroupId).setName(volumeName)
                .setEtag(newEtag).setMode(perms).setAccessControlPolicy(acPolId).setDefaultStripingPolicy(
                    defaultStripingPolicy).build();
        
    }
    
}
