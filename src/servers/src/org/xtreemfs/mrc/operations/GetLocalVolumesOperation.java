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

import java.util.Collection;

import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.Volume;
import org.xtreemfs.interfaces.VolumeSet;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_lsvolResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Converter;

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
        
        VolumeSet vSet = new VolumeSet();
        for (StorageManager sMan : sMans) {
            
            VolumeInfo vol = sMan.getVolumeInfo();
            FileMetadata md = sMan.getMetadata(1);
            vSet.add(new Volume(AccessControlPolicyType.parseInt(vol.getAcPolicyId()), Converter
                    .stripingPolicyToStripingPolicy(sMan.getDefaultStripingPolicy(1)), 
                    vol.getId(), sMan.getMetadata(1).getPerms(), vol.getName(), 
                    md.getOwningGroupId(), md.getOwnerId()));
        }
        
        rq.setResponse(new xtreemfs_lsvolResponse(vSet));
        finishRequest(rq);
    }
    
}
