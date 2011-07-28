/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.util.Collection;
import java.util.Iterator;

import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
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
                if (attr.getKey().startsWith(prefix))
                    vol.addAttrs(KeyValuePair.newBuilder().setKey(attr.getKey().substring(prefix.length()))
                            .setValue(attr.getValue()));
            }
            attrs.destroy();
            
            vSet.addVolumes(vol);
        }
        
        rq.setResponse(vSet.build());
        finishRequest(rq);
    }
    
}
