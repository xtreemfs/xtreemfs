/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_dump_restore_databaseRequest;

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
        
        final xtreemfs_dump_restore_databaseRequest rqArgs = (xtreemfs_dump_restore_databaseRequest) rq
                .getRequestArgs();
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword().length() > 0
            && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "invalid password");
        
        // check if the creation of a dump is already in progress; if not,
        // create a new dump
        if (!new File(rqArgs.getDumpFile() + ".inprogress").exists()) {
            
            // create snapshots of all volumes
            List<StorageManager> storageManagers = new LinkedList<StorageManager>();
            final VolumeManager vMan = master.getVolumeManager();
            
            Collection<StorageManager> sManColl = vMan.getStorageManagers();
            if (sManColl == null)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                        "cannot dump volumes because volume manager has not yet been initialized");

            for (StorageManager sMan : sManColl) {
                
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
            DumpWriter dw = new DumpWriter(storageManagers, new File(rqArgs.getDumpFile()));
            dw.start();
        }
        
        // set the response
        rq.setResponse(emptyResponse.getDefaultInstance());
        finishRequest(rq);
    }
    
}
