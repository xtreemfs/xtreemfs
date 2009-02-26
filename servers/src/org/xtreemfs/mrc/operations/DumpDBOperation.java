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
import java.util.List;

import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class DumpDBOperation extends MRCOperation {
    
    static class Args {
        public String dumpFile;
    }
    
    public static final String RPC_NAME = ".dumpdb";
    
    public DumpDBOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public boolean hasArguments() {
        return true;
    }
    
    @Override
    public boolean isAuthRequired() {
        return true;
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            Args rqArgs = (Args) rq.getRequestArgs();
            final VolumeManager vMan = master.getVolumeManager();
            
            BufferedWriter xmlWriter = new BufferedWriter(new FileWriter(rqArgs.dumpFile));
            xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xmlWriter.write("<filesystem dbversion=\"" + VersionManagement.getMrcDataVersion()
                + "\">\n");
            
            for (VolumeInfo volume : vMan.getVolumes()) {
                xmlWriter.write("<volume id=\""
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
            
            finishRequest(rq);
            
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
                "an error has occurred", exc));
        }
    }
    
    @Override
    public ErrorRecord parseRPCBody(MRCRequest rq, List<Object> arguments) {
        
        Args args = new Args();
        
        try {
            
            args.dumpFile = (String) arguments.get(0);
            
            if (arguments.size() == 1)
                return null;
            
            throw new Exception();
            
        } catch (Exception exc) {
            try {
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "': " + JSONParser.writeJSON(arguments));
            } catch (JSONException je) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "'");
            }
        } finally {
            rq.setRequestArgs(args);
        }
    }
    
}
