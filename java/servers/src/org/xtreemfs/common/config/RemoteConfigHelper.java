/*
 * Copyright (c) 2010-2011 by Paul Seiferth, Zuse Institute Berlin
 *                    2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.config;

import java.util.HashMap;

import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;

public class RemoteConfigHelper {
    public static ServiceConfig getConfigurationFromDIR(ServiceConfig config) throws Exception {
        TimeSync ts = null;
        boolean timeSyncAlreadyRunning = true;

        try {
            final int WAIT_BETWEEN_RETRIES = 1000;
            int retries = config.getWaitForDIR() * 1000 / WAIT_BETWEEN_RETRIES;
            if (retries <= 0) {
                retries = 1;
            }
            Logging.logMessage(Logging.LEVEL_INFO, null, "Loading configuration from DIR (will retry up to %d times)", retries);
    
            SSLOptions sslOptions;
            sslOptions = config.isUsingSSL() ? new SSLOptions(config.getServiceCredsFile(),
                    config.getServiceCredsPassphrase(), config.getServiceCredsContainer(),
                    config.getTrustedCertsFile(), config.getTrustedCertsPassphrase(),
                    config.getTrustedCertsContainer(), false, config.isGRIDSSLmode(), config.getSSLProtocolString(),
                    new PolicyContainer(config).getTrustManager()) : null;
    
            RPCNIOSocketClient clientStage = new RPCNIOSocketClient(sslOptions, 1000, 60 * 1000, "RemoteConfigHelper");
            DIRServiceClient dirRPCClient = new DIRServiceClient(clientStage, config.getDirectoryService());
            DIRClient dirClient = new DIRClient(dirRPCClient, config.getDirectoryServices(), retries,
                    WAIT_BETWEEN_RETRIES);
    
            clientStage.start();
            clientStage.waitForStartup();
    
            timeSyncAlreadyRunning = TimeSync.isInitialized();
            if (!timeSyncAlreadyRunning) {
                ts = TimeSync.initializeLocal(0);
                ts.waitForStartup();
            }
    
            Auth authNone = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
            UserCredentials uc = UserCredentials.newBuilder().setUsername("main-method")
                    .addGroups("xtreemfs-services").build();
    
            Configuration conf = dirClient.xtreemfs_configuration_get(null, authNone, uc, config.getUUID()
                    .toString());
    
            clientStage.shutdown();
            clientStage.waitForShutdown();
    
            HashMap<String, String> returnMap = new HashMap<String, String>();
    
            for (KeyValuePair kvp : conf.getParameterList()) {
                returnMap.put(kvp.getKey(), kvp.getValue());
            }
    
            return new ServiceConfig(returnMap);
        } finally {
            if (!timeSyncAlreadyRunning && ts != null) {
                ts.close();
            }
        }
    };
}
