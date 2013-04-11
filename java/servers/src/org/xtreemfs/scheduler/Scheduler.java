package org.xtreemfs.scheduler;

import java.io.IOException;

import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

public class Scheduler {
	public static void main(String[] args) {
		SchedulerConfig config;
		String configFileName = "etc/xos/xtreemfs/schedulerconfig.test";
		
        if (args.length != 1) 
            System.out.println("using default config file " + configFileName);
        else 
            configFileName = args[0];
        
        try {
        	config = new SchedulerConfig(configFileName);
        }
        catch (IOException ex) {
        	ex.printStackTrace();
        	return;
        }
        config.setDefaults();
        
        config.checkConfig();
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, (Object) null, "JAVA_HOME=%s", System
                    .getProperty("java.home"));
        
        try{
        	BabuDBConfig dbsConfig = new BabuDBConfig(configFileName);
        	final SchedulerRequestDispatcher rq = new SchedulerRequestDispatcher(config, dbsConfig);
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
                                "Scheduler shutdown complete");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
        catch(Exception ex) {
            Logging.logMessage(Logging.LEVEL_CRIT, null,
                    "Scheduler could not start up due to an exception. Aborted.");
                Logging.logError(Logging.LEVEL_CRIT, null, ex);
                System.exit(1);
        }
	}
}
