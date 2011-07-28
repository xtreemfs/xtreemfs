/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.io.FileInputStream;
import java.util.HashMap;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.config.PolicyContainer;
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
        
        config.setDefaults(config.getConnectionParameter());
        
        Logging.start(config.getDebugLevel(), config.getDebugCategories());
        
        if (config.isInitializable()) {
            try {
                OSDConfig remoteConfig = getConfigurationFromDIR(config);
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
        
        new OSD(config);
    };
    
    private static OSDConfig getConfigurationFromDIR(OSDConfig config) throws Exception {
        
        HeartbeatThread.waitForDIR(config.getDirectoryService(), config.getWaitForDIR());
        
        SSLOptions sslOptions = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config
                .getServiceCredsFile()), config.getServiceCredsPassphrase(), config
                .getServiceCredsContainer(), new FileInputStream(config.getTrustedCertsFile()), config
                .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false, config
                .isGRIDSSLmode(), new PolicyContainer(config).getTrustManager()) : null;
        
        RPCNIOSocketClient clientStage = new RPCNIOSocketClient(sslOptions, 1000, 60 * 1000);
        DIRServiceClient dirClient = new DIRServiceClient(clientStage, config.getDirectoryService());
        
        clientStage.start();
        clientStage.waitForStartup();
        
        TimeSync.initializeLocal(60000, 50);
        
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
        
        TimeSync.close();
        
        return new OSDConfig(returnMap);
    };
    
}
