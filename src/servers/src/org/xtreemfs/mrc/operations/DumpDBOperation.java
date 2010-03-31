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
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_dump_databaseRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_dump_databaseResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;

/**
 * 
 * @author stender
 */
public class DumpDBOperation extends MRCOperation {
    
    class DumpWriter extends Thread {
        
        private File                 dumpFile;
        
        private List<StorageManager> sManList;
        
        public DumpWriter(List<StorageManager> sManList, File dumpFile) {
            this.dumpFile = dumpFile;
            this.sManList = sManList;
        }
        
        public void run() {
            
            File df = new File(dumpFile + ".inprogress");
            
            try {
                BufferedWriter xmlWriter = new BufferedWriter(new FileWriter(df));
                xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                xmlWriter.write("<filesystem dbversion=\"" + VersionManagement.getMrcDataVersion() + "\">\n");
                
                for (StorageManager sMan : sManList) {
                    VolumeInfo vol = sMan.getVolumeInfo();
                    xmlWriter.write("<volume id=\"" + vol.getId() + "\" name=\"" + vol.getName()
                        + "\" acPolicy=\"" + vol.getAcPolicyId() + "\">\n");
                    sMan.dumpDB(xmlWriter);
                    xmlWriter.write("</volume>\n");
                }
                
                xmlWriter.write("</filesystem>\n");
                xmlWriter.close();
                
                df.renameTo(dumpFile);
                
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                    "an error has occurred while dumping the database: %s", OutputUtils
                            .stackTraceToString(exc));
            }
        }
        
    }
    
    public DumpDBOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_dump_databaseRequest rqArgs = (xtreemfs_dump_databaseRequest) rq.getRequestArgs();
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword() != null
            && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(ErrNo.EPERM, "invalid password");
        
        // check if the creation of a dump is already in progress; if not,
        // create a new dump
        if (!new File(rqArgs.getDump_file() + ".inprogress").exists()) {
            
            // create snapshots of all volumes
            List<StorageManager> storageManagers = new LinkedList<StorageManager>();
            final VolumeManager vMan = master.getVolumeManager();
            for (StorageManager sMan : vMan.getStorageManagers()) {
                
                FileMetadata rootDir = sMan.getMetadata(1);
                try {
                    vMan.createSnapshot(sMan.getVolumeInfo().getId(), ".dump", 0, rootDir, true);
                    
                } catch (UserException exc) {
                    
                    // if the snapshot exists already, delete it and create it
                    // again
                    vMan.deleteSnapshot(sMan.getVolumeInfo().getId(), rootDir, ".dump");
                    vMan.createSnapshot(sMan.getVolumeInfo().getId(), ".dump", 0, rootDir, true);
                }
                
                storageManagers.add(vMan.getStorageManagerByName(sMan.getVolumeInfo().getName()
                    + VolumeManager.SNAPSHOT_SEPARATOR + ".dump"));
            }
            
            // write the dump asynchronously
            DumpWriter dw = new DumpWriter(storageManagers, new File(rqArgs.getDump_file()));
            dw.start();
        }
        
        // set the response
        rq.setResponse(new xtreemfs_dump_databaseResponse());
        finishRequest(rq);
    }
    
}
