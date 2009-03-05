/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.new_osd;

import org.xtreemfs.common.logging.Logging;

public class OSD {
    
    private OSDRequestDispatcher dispatcher;
    
    /**
     * Creates a new instance of Main
     */
    public OSD(OSDConfig config) {
        
        Logging
                .logMessage(Logging.LEVEL_INFO, null, "JAVA_HOME="
                    + System.getProperty("java.home"));
        Logging.logMessage(Logging.LEVEL_INFO, null, "UUID: " + config.getUUID());
        
        try {
            // FIXME: pass UUID + useDIR
            dispatcher = new OSDRequestDispatcher(config);
            dispatcher.start();
            
            final OSDRequestDispatcher ctrl = dispatcher;
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        
                        Logging.logMessage(Logging.LEVEL_INFO, this, "received shutdown signal!");
                        
                        ctrl.heartbeatThread.shutdown();
                        // FIXME: provide a solution that does not attempt to
                        // shut down an OSD that is already being shut down due
                        // to an error
                        // ctrl.shutdown();
                        
                        Logging.logMessage(Logging.LEVEL_INFO, this, "OSD shutdown complete");
                        
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            
        } catch (Exception ex) {
            
            Logging.logMessage(Logging.LEVEL_DEBUG, null,
                "System could not start up due to an exception. Aborted.");
            Logging.logMessage(Logging.LEVEL_ERROR, null, ex);
            
            if (dispatcher != null)
                try {
                    dispatcher.shutdown();
                } catch (Exception e) {
                    Logging.logMessage(Logging.LEVEL_ERROR, config.getUUID(),
                        "could not shutdown MRC: ");
                    Logging.logMessage(Logging.LEVEL_ERROR, config.getUUID(), e);
                }
        }
    }
    
    public void shutdown() {
        dispatcher.shutdown();
    }
    
    public OSDRequestDispatcher getDispatcher() {
        return dispatcher;
    }
    
    /**
     * Main routine
     * 
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        Thread.currentThread().setName("OSD");
        
        String cfgFile = (args.length > 0) ? args[0] : "config/osdconfig.properties";
        OSDConfig config = new OSDConfig(cfgFile);
        
        Logging.start(config.getDebugLevel());
        new OSD(config);
    };
    
}
