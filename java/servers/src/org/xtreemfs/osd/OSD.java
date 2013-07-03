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

import sun.misc.Signal;
import sun.misc.SignalHandler;

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
        
        // try registering a handler for the USR2 signal
        // the VM should be instructed to use different signals with the -XX:+UseAltSigs flag
        // TODO(jdillmann): Test on different VMs and operating systems
        try {
            Signal.handle(new Signal("USR2"), new SignalHandler() {

                @Override
                public void handle(Signal signal) {
                    if (dispatcher != null) {
                        // instruct dispatcher.heartbeatThread to renew the address mappings and send them to the DIR
                        dispatcher.renewAddressMappings();
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            Logging.logMessage(Logging.LEVEL_WARN, config.getUUID(), "Could not register SignalHandler for USR2");
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
