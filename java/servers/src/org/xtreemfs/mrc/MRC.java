/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;

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
        
        config.setDefaults(config.getConnectionParameter());
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        
        TimeSync.initializeLocal(60000, 50).waitForStartup();
        
        if (config.isInitializable()) {
            try {
                MRCConfig remoteConfig = getConfigurationFromDIR(config);
                config.mergeConfig(remoteConfig);
            } catch (Exception e) {
                e.printStackTrace();
                Logging.logMessage(Logging.LEVEL_WARN, config.getUUID(),
                    "couldn't fetch configuration file from DIR");
                Logging.logError(Logging.LEVEL_DEBUG, config.getUUID(), e);
            }
        }
        
        config.setDefaults();
        
        config.checkConfig();
        
        BabuDBConfig dbsConfig = null;
        try {
            dbsConfig = new BabuDBConfig(configFileName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        
        new MRC(config, dbsConfig);
    }
    
    private static MRCConfig getConfigurationFromDIR(MRCConfig config) throws Exception {
        
        HeartbeatThread.waitForDIR(config.getDirectoryService(), config.getWaitForDIR());
        
        SSLOptions sslOptions = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config
                .getServiceCredsFile()), config.getServiceCredsPassphrase(), config
                .getServiceCredsContainer(), new FileInputStream(config.getTrustedCertsFile()), config
                .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false, config
                .isGRIDSSLmode(), new MRCPolicyContainer(config).getTrustManager()) : null;
        
        RPCNIOSocketClient clientStage = new RPCNIOSocketClient(sslOptions, 1000, 60 * 1000);
        DIRServiceClient dirClient = new DIRServiceClient(clientStage, config.getDirectoryService());
        
        clientStage.start();
        clientStage.waitForStartup();
                
        Auth authNone = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
        UserCredentials uc = UserCredentials.newBuilder().setUsername("main-method").addGroups(
            "xtreemfs-services").build();
        
        RPCResponse<Configuration> responseGet = null;
        Configuration conf = null;
        try {
            responseGet = dirClient.xtreemfs_configuration_get(null, authNone, uc, config.getUUID()
                    .toString());
            conf = responseGet.get();
        } finally {
            if (responseGet != null)
                responseGet.freeBuffers();
        }
        
        clientStage.shutdown();
        clientStage.waitForShutdown();
        
        HashMap<String, String> returnMap = new HashMap<String, String>();
        
        for (KeyValuePair kvp : conf.getParameterList()) {
            returnMap.put(kvp.getKey(), kvp.getValue());
        }
                
        return new MRCConfig(returnMap);
    };
    
}
