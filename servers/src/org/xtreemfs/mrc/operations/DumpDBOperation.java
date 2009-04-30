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

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_dump_databaseRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_dump_databaseResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class DumpDBOperation extends MRCOperation {
    
    public static final int OP_ID = 52;
    
    public DumpDBOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_dump_databaseRequest rqArgs = (xtreemfs_dump_databaseRequest) rq.getRequestArgs();
        final VolumeManager vMan = master.getVolumeManager();
        
        BufferedWriter xmlWriter = new BufferedWriter(new FileWriter(rqArgs.getDump_file()));
        xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlWriter.write("<filesystem dbversion=\"" + VersionManagement.getMrcDataVersion() + "\">\n");
        
        for (VolumeInfo volume : vMan.getVolumes()) {
            xmlWriter
                    .write("<volume id=\""
                        + volume.getId()
                        + "\" name=\""
                        + volume.getName()
                        + "\" acPolicy=\""
                        + volume.getAcPolicyId()
                        + "\" osdPolicy=\""
                        + volume.getOsdPolicyId()
                        + (volume.getOsdPolicyArgs() != null ? "\" osdPolicyArgs=\""
                            + volume.getOsdPolicyArgs() : "") + "\">\n");
            
            StorageManager sMan = vMan.getStorageManager(volume.getId());
            sMan.dumpDB(xmlWriter);
            
            xmlWriter.write("</volume>\n");
        }
        
        xmlWriter.write("</filesystem>\n");
        xmlWriter.close();
        
        // set the response
        rq.setResponse(new xtreemfs_dump_databaseResponse());
        finishRequest(rq);
    }
}
