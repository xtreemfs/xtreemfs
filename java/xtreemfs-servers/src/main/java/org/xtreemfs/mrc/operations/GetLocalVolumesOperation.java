/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.util.Collection;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;

/**
 * 
 * @author stender
 */
public class GetLocalVolumesOperation extends MRCOperation {
    
    public GetLocalVolumesOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        Collection<StorageManager> sMans = master.getVolumeManager().getStorageManagers();
        if (sMans == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "cannot retrieve volume list because volume manager has not yet been initialized");
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword().length() > 0
                && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "Invalid password. If you see this error at mount.xtreemfs when mounting a volume, please update your client to version 1.3.1 or newer.");
        
        Volumes.Builder vSet = Volumes.newBuilder();
        for (StorageManager sMan : sMans) {
            
            Volume.Builder vol = Volume.newBuilder();
            StatVFS vInfo = StatFSOperation.getVolumeInfo(master, sMan);
            vol.setAccessControlPolicy(vInfo.getAccessControlPolicy()).setDefaultStripingPolicy(
                vInfo.getDefaultStripingPolicy()).setId(vInfo.getFsid()).setMode(vInfo.getMode()).setName(
                vInfo.getName()).setOwnerGroupId(vInfo.getOwnerGroupId()).setOwnerUserId(
                vInfo.getOwnerUserId());
            
            // add volume attributes
            final String prefix = "xtreemfs." + MRCHelper.VOL_ATTR_PREFIX + ".";
            DatabaseResultSet<XAttr> attrs = sMan.getXAttrs(1, StorageManager.SYSTEM_UID);
            while (attrs.hasNext()) {
                XAttr attr = attrs.next();
                if (attr.getKey().startsWith(prefix)) {
                    byte[] valBytes = attr.getValue();
                    vol.addAttrs(KeyValuePair.newBuilder().setKey(attr.getKey().substring(prefix.length()))
                            .setValue(valBytes == null ? null : new String(valBytes)));
                }
            }
            attrs.destroy();
            
            vSet.addVolumes(vol);
        }
        
        rq.setResponse(vSet.build());
        finishRequest(rq);
    }
    
}
