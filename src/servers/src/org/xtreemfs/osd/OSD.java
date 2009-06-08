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

package org.xtreemfs.osd;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;

public class OSD {
    
    private OSDRequestDispatcher dispatcher;
    
    /**
     * Creates a new instance of Main
     */
    public OSD(OSDConfig config) {
        
        if (Logging.isInfo()) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "JAVA_HOME=%s", System
                    .getProperty("java.home"));
            Logging
                    .logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "UUID: %s", config
                            .getUUID());
        }
        
        try {
            // FIXME: pass UUID + useDIR
            dispatcher = new OSDRequestDispatcher(config);
            dispatcher.start();
            
            final OSDRequestDispatcher ctrl = dispatcher;
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        
                        if (Logging.isInfo())
                            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                                "received shutdown signal");
                        
                        ctrl.heartbeatThread.shutdown();
                        // FIXME: provide a solution that does not attempt to
                        // shut down an OSD that is already being shut down due
                        // to an error
                        // ctrl.shutdown();
                        
                        if (Logging.isInfo())
                            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                                "OSD shutdown complete");
                        
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });
            
        } catch (Exception ex) {
            
            Logging.logMessage(Logging.LEVEL_ERROR, null,
                "OSD could not start up due to an exception. Aborted.");
            Logging.logError(Logging.LEVEL_ERROR, null, ex);
            
            if (dispatcher != null)
                try {
                    dispatcher.shutdown();
                } catch (Exception e) {
                    Logging.logMessage(Logging.LEVEL_ERROR, config.getUUID(), "could not shutdown MRC: ");
                    Logging.logError(Logging.LEVEL_ERROR, config.getUUID(), e);
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
        
        String cfgFile = (args.length > 0) ? args[0] : "../../etc/xos/xtreemfs/osdconfig.test";
        OSDConfig config = new OSDConfig(cfgFile);
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        new OSD(config);
    };
    
}
