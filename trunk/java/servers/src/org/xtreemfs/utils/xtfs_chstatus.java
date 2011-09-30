/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.ONCRPCServiceURL;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;

/**
 * 
 * @author bjko
 */
public class xtfs_chstatus {
    
    private static final String DEFAULT_DIR_CONFIG = "/etc/xos/xtreemfs/default_dir";
    
    public static void main(String[] args) {
        try {
            Logging.start(Logging.LEVEL_WARN);
            
            TimeSync.initializeLocal(60000, 50);
            Map<String, CliOption> options = utils.getDefaultAdminToolOptions(false);
            List<String> arguments = new ArrayList<String>(1);
            CliOption oDir = new CliOption(
                CliOption.OPTIONTYPE.URL,
                "directory service to use (e.g. 'pbrpc://localhost:32638'). If no URI is specified, URI and security settings are taken from '"
                    + DEFAULT_DIR_CONFIG + "'", "<uri>");
            oDir.urlDefaultPort = PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber();
            oDir.urlDefaultProtocol = Schemes.SCHEME_PBRPC;
            options.put("dir", oDir);
            
            CLIParser.parseCLI(args, options, arguments);
            
            if (options.get(utils.OPTION_HELP).switchValue || options.get(utils.OPTION_HELP_LONG).switchValue) {
                usage(options);
                return;
            }
            
            if (arguments.size() > 2 || arguments.size() < 1)
                error("invalid number of arguments", options);
            
            InetSocketAddress dirAddr = null;
            boolean useSSL = false;
            boolean gridSSL = false;
            String serviceCredsFile = null;
            String serviceCredsPass = null;
            String trustedCAsFile = null;
            String trustedCAsPass = null;
            
            ONCRPCServiceURL dirURL = options.get("dir").urlValue;
            
            // parse security info if protocol is 'https'
            if (dirURL != null
                && (Schemes.SCHEME_PBRPCS.equals(dirURL.getProtocol()) || Schemes.SCHEME_PBRPCG.equals(dirURL
                        .getProtocol()))) {
                useSSL = true;
                serviceCredsFile = options.get(utils.OPTION_USER_CREDS_FILE).stringValue;
                serviceCredsPass = options.get(utils.OPTION_USER_CREDS_PASS).stringValue;
                trustedCAsFile = options.get(utils.OPTION_TRUSTSTORE_FILE).stringValue;
                trustedCAsPass = options.get(utils.OPTION_TRUSTSTORE_PASS).stringValue;
                if (Schemes.SCHEME_PBRPCG.equals(dirURL.getProtocol())) {
                    gridSSL = true;
                }
            }
            
            // read default settings
            if (dirURL == null) {
                
                DefaultDirConfig cfg = new DefaultDirConfig(DEFAULT_DIR_CONFIG);
                cfg.read();
                
                dirAddr = cfg.getDirectoryService();
                useSSL = cfg.isSslEnabled();
                serviceCredsFile = cfg.getServiceCredsFile();
                serviceCredsPass = cfg.getServiceCredsPassphrase();
                trustedCAsFile = cfg.getTrustedCertsFile();
                trustedCAsPass = cfg.getTrustedCertsPassphrase();
            } else {
                dirAddr = new InetSocketAddress(dirURL.getHost(), dirURL.getPort());
            }
            
            // TODO: support custom SSL trust managers
            SSLOptions sslOptions = useSSL ? new SSLOptions(new FileInputStream(serviceCredsFile),
                serviceCredsPass, SSLOptions.PKCS12_CONTAINER, new FileInputStream(trustedCAsFile),
                trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL, null) : null;
            
            final String uuid = arguments.get(0);
            
            final String newStatus = (arguments.size() == 2) ? arguments.get(1) : null;
            
            RPCNIOSocketClient rpcClient = new RPCNIOSocketClient(sslOptions, 15 * 100, 5 * 60 * 1000);
            rpcClient.start();
            rpcClient.waitForStartup();
            DIRServiceClient dc = new DIRServiceClient(rpcClient, dirAddr);
            RPCResponse<ServiceSet> r = dc.xtreemfs_service_get_by_uuid(dirAddr, RPCAuthentication.authNone,
                RPCAuthentication.userService, uuid);
            ServiceSet set = r.get();
            r.freeBuffers();
            
            if (set.getServicesCount() == 0) {
                System.out.println("no service with UUID " + uuid + " registered at directory service");
                System.exit(1);
            }
            
            Service s = set.getServices(0);
            String hbAttr = KeyValuePairs.getValue(s.getData().getDataList(), HeartbeatThread.STATUS_ATTR);
            if (hbAttr != null) {
                System.out.print("current status: ");
                switch (ServiceStatus.valueOf(Integer.valueOf(hbAttr))) {
                case SERVICE_STATUS_AVAIL:
                    System.out.println(hbAttr + " (online)");
                    break;
                case SERVICE_STATUS_TO_BE_REMOVED:
                    System.out.println(hbAttr + " (locked for removal)");
                    break;
                case SERVICE_STATUS_REMOVED:
                    System.out.println(hbAttr + " (removed, dead)");
                    break;
                }
            }
            
            if (newStatus != null) {
                // change status
                String newStatusInt = "";
                if (newStatus.equalsIgnoreCase("online")) {
                    newStatusInt = Integer.toString(ServiceStatus.SERVICE_STATUS_AVAIL.getNumber());
                } else if (newStatus.equalsIgnoreCase("locked")) {
                    newStatusInt = Integer.toString(ServiceStatus.SERVICE_STATUS_TO_BE_REMOVED.getNumber());
                } else if (newStatus.equalsIgnoreCase("removed")) {
                    newStatusInt = Integer.toString(ServiceStatus.SERVICE_STATUS_REMOVED.getNumber());
                } else {
                    System.out.println("unknown status name: " + newStatus
                        + ". Must be 'online', ' locked' or 'removed'");
                }
                List<KeyValuePair> data = new LinkedList<KeyValuePair>(s.getData().getDataList());
                KeyValuePairs.putValue(data, HeartbeatThread.STATUS_ATTR, newStatusInt);
                ServiceDataMap dataMap = ServiceDataMap.newBuilder().addAllData(data).build();
                s = s.toBuilder().setData(dataMap).build();
                RPCResponse r2 = dc.xtreemfs_service_register(dirAddr, RPCAuthentication.authNone,
                    RPCAuthentication.userService, s);
                r2.get();
                r2.freeBuffers();
                System.out.println("status changed to: " + newStatus);
            }
            
            rpcClient.shutdown();
            TimeSync.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void error(String message, Map<String, CliOption> options) {
        System.err.println(message);
        System.out.println();
        usage(options);
        System.exit(1);
    }
    
    private static void usage(Map<String, CliOption> options) {
        
        System.out.println("usage: xtfs_chstatus [options] <service UUID> [online|locked|removed]");
        System.out
                .println("<service UUID> the registered UUID for which the status is supposed to be changed");
        System.out.println(" online - marks the service as online");
        System.out
                .println(" locked - marks the service as locked (locked OSDs will not be assigned to new files)");
        System.out.println("removed - marks the service as no longer available\n");
        
        System.out.println("options:");
        utils.printOptions(options);
    }
    
}
