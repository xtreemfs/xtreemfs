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

import org.xtreemfs.interfaces.StatVFS;
import org.xtreemfs.interfaces.MRCInterface.statvfsRequest;
import org.xtreemfs.interfaces.MRCInterface.statvfsResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class StatFSOperation extends MRCOperation {
    
    public static final int OP_ID = 19;
    
    public StatFSOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final statvfsRequest rqArgs = (statvfsRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final VolumeInfo volume = vMan.getVolumeByName(rqArgs.getVolume_name());
        
        long blocks = master.getOSDStatusManager().getFreeSpace(volume.getId()) / 1024L;
        StatVFS statfs = new StatVFS(1024, blocks, volume.getId(), 1024);
        
        // set the response
        rq.setResponse(new statvfsResponse(statfs));
        finishRequest(rq);
        
    }
    
}
