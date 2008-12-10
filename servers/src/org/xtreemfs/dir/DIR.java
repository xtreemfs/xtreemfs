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
package org.xtreemfs.dir;

import java.io.IOException;

import org.xtreemfs.common.logging.Logging;

/**
 * This class can be used to start a new instance of the Directory Service.
 * 
 * @author stender
 * 
 */
public class DIR {
    
    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        
        String configFileName = "../config/dirconfig.properties";
        
        if (args.length != 1) {
            System.out.println("using default config file " + configFileName);
        } else {
            configFileName = args[0];
        }
        
        DIRConfig config = null;
        try {
            config = new DIRConfig(configFileName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        
        Logging.start(config.getDebugLevel());
        
        Logging
                .logMessage(Logging.LEVEL_INFO, null, "JAVA_HOME="
                    + System.getProperty("java.home"));
        
        try {
            final RequestController rq = new RequestController(config);
            rq.startup();
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        Logging.logMessage(Logging.LEVEL_INFO, this, "received shutdown signal!");
                        rq.shutdown();
                        Logging.logMessage(Logging.LEVEL_INFO, this, "DIR shotdown complete");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, null, ex);
            Logging.logMessage(Logging.LEVEL_DEBUG, null,
                "System could not start up due to an exception. Aborted.");
            System.exit(1);
        }
        
    }
    
}
