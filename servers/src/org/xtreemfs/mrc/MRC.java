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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;

/**
 * 
 * @author bjko
 */
public class MRC {
    
    private MRCRequestDispatcher rc;
    
    /**
     * @param args
     *            the command line arguments
     */
    public MRC(MRCConfig config, boolean useDirService) {
        
        if (Logging.isInfo()) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "JAVA_HOME=%s", System
                    .getProperty("java.home"));
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "UUID: %s", config.getUUID()
                    .toString());
        }
        
        try {
            rc = new MRCRequestDispatcher(config);
            rc.startup();
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        if (Logging.isInfo())
                            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                                "received shutdown signal!");
                        rc.shutdown();
                        if (Logging.isInfo())
                            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                                "MRC shutdown complete");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            
        } catch (Exception ex) {
            
            Logging.logMessage(Logging.LEVEL_CRIT, null,
                "MRC could not start up due to an exception. Aborted.");
            Logging.logError(Logging.LEVEL_CRIT, null, ex);
            
            if (rc != null)
                try {
                    rc.shutdown();
                } catch (Exception e) {
                    Logging.logMessage(Logging.LEVEL_ERROR, config.getUUID(), "could not shutdown MRC: ");
                    Logging.logError(Logging.LEVEL_ERROR, config.getUUID(), e);
                }
        }
        
    }
    
    /**
     * Main routine
     * 
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        Thread.currentThread().setName("MRC");
        
        String cfgFile = (args.length > 0) ? args[0] : "config/mrcconfig.properties";
        MRCConfig config = new MRCConfig(cfgFile);
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        new MRC(config, true);
    };
    
}
