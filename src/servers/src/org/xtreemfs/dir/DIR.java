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
 * AUTHORS: Jan Stender (ZIB)
 */
package org.xtreemfs.dir;

import java.io.IOException;

import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.babudb.config.ReplicationConfig;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;

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

        String configFileName = "../../etc/xos/xtreemfs/dirconfig.test";
        
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
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        
        BabuDBConfig dbsConfig = null;
        try {
            dbsConfig = new ReplicationConfig(configFileName);
        } catch (Throwable e) {
            Logging.logError(Logging.LEVEL_INFO, null, e);
            try {
                dbsConfig = new BabuDBConfig(configFileName);
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
        }
        
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
                                "DIR shotdown complete");
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
