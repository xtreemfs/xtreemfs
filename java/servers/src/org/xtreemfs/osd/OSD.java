/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

public class OSD {
    
    private OSDRequestDispatcher dispatcher;
    
    /**
     * Creates a new instance of Main
     */
    public OSD(OSDConfig config) {
        
        if (Logging.isInfo()) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "JAVA_HOME=%s",
                    System.getProperty("java.home"));
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "UUID: %s", config.getUUID());
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
                            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "received shutdown signal");
                        
                        ctrl.shutdown();
                        
                        if (Logging.isInfo())
                            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "OSD shutdown complete");
                        
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });
            
        } catch (Exception ex) {
            
            Logging.logMessage(Logging.LEVEL_ERROR, null, "OSD could not start up due to an exception. Aborted.");
            Logging.logError(Logging.LEVEL_ERROR, null, ex);
            
            if (dispatcher != null)
                try {
                    dispatcher.shutdown();
                } catch (Exception e) {
                    Logging.logMessage(Logging.LEVEL_ERROR, config.getUUID(), "could not shutdown OSD: ");
                    Logging.logError(Logging.LEVEL_ERROR, config.getUUID(), e);
                }
            System.exit(1);
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
        
        String cfgFile = (args.length > 0) ? args[0] : "./etc/xos/xtreemfs/osdconfig.test";
        OSDConfig config = new OSDConfig(cfgFile);
        
        config.setDefaults();
        
        config.checkConfig();
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        
        new OSD(config);
    }
}
