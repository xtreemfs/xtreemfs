/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;

import java.io.IOException;

import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

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
    public static void main(String[] args) throws IOException {

        String configFileName = "etc/xos/xtreemfs/dirconfig.test";
        if (args.length != 1) 
            System.out.println("using default config file " + configFileName);
        else 
            configFileName = args[0];
        
        DIRConfig config = null; 
        try {
            config = new DIRConfig(configFileName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        config.setDefaults();
        
        config.checkConfig();
        
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        
        BabuDBConfig dbsConfig = new BabuDBConfig(configFileName);
        
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "JAVA_HOME=%s", System
                    .getProperty("java.home"));
        
        try {
            final DIRRequestDispatcher rq = new DIRRequestDispatcher(config, dbsConfig);
            rq.startup();
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        if (Logging.isInfo())
                            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                                "received shutdown signal!");
                        rq.shutdown();
                        if (Logging.isInfo())
                            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                                "DIR shutdown complete");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_CRIT, null,
                "DIR could not start up due to an exception. Aborted.");
            Logging.logError(Logging.LEVEL_CRIT, null, ex);
            System.exit(1);
        }
        
    }
    
}
