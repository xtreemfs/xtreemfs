/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.io.IOException;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

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
    public MRC(MRCConfig config, BabuDBConfig dbConfig) {
        
        if (Logging.isInfo()) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "JAVA_HOME=%s", System
                    .getProperty("java.home"));
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "UUID: %s", config.getUUID()
                    .toString());
        }
        
        try {
            rc = new MRCRequestDispatcher(config, dbConfig);
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
        
        String configFileName = "etc/xos/xtreemfs/mrcconfig.test";
        
        configFileName = (args.length == 1) ? args[0] : configFileName;
        
        MRCConfig config = new MRCConfig(configFileName);
        
        config.setDefaults();
        
        config.checkConfig();
        
        BabuDBConfig dbsConfig = null;
        try {
            dbsConfig = new BabuDBConfig(configFileName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        
        new MRC(config, dbsConfig);
    }
}
