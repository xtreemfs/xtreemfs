/*
 * Copyright (c) 2009 by Felix Langner, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeServerClient;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.ONCRPCServiceURL;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.globalTimeSGetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_get_resultsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_is_runningResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_statusResponse;

/**
 * Console-tool for the cleanUp-functionality of the XtreemFS OSD.
 * 
 * 24.04.2009
 * 
 * @author flangner
 */
public class xtfs_cleanup_osd {
    
    private static final String       DEFAULT_DIR_CONFIG = "/etc/xos/xtreemfs/default_dir";
    
    private static OSDServiceClient   osd;
    
    private static DIRClient          dir;
    
    private static RPCNIOSocketClient dirClient;
    
    private static RPCNIOSocketClient osdClient;
    
    private static InetSocketAddress  osdAddr;
    
    private static Auth               password;
    
    /**
     * Main method.
     * 
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        try {
            Logging.start(Logging.LEVEL_ERROR);
            
            // parse the call arguments
            Map<String, CliOption> options = utils.getDefaultAdminToolOptions(true);
            List<String> arguments = new ArrayList<String>(1);
            options.put("r", new CliOption(CliOption.OPTIONTYPE.SWITCH, "restore zombies found on the OSD",
                ""));
            options.put("e", new CliOption(CliOption.OPTIONTYPE.SWITCH, "erase potential zombies", ""));
            options.put("delete_volumes", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "!dangerous! deletes volumes that might be dead", ""));
            options.put("i", new CliOption(CliOption.OPTIONTYPE.SWITCH, "interactive mode", ""));
            options.put("stop", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "suspends the currently running cleanup process", ""));
            options.put("wait", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "blocks call until the currently running cleanup process has terminated", ""));
            CliOption oDir = new CliOption(CliOption.OPTIONTYPE.URL,
                "directory service to use (e.g. 'pbrpc://localhost:32638')", "<uri>");
            oDir.urlDefaultPort = PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber();
            oDir.urlDefaultProtocol = Schemes.SCHEME_PBRPC;
            options.put("dir", oDir);
            options.put("v", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "run a version cleanup (only if file content versioning is enabled)", ""));
            
            CLIParser.parseCLI(args, options, arguments);
            
            if (options.get(utils.OPTION_HELP).switchValue || options.get(utils.OPTION_HELP_LONG).switchValue) {
                usage(options);
                return;
            }
            
            if (arguments.size() != 1)
                error("invalid number of arguments", options);
            
            boolean remove = options.get("e").switchValue;
            boolean restore = options.get("r").switchValue;
            boolean deleteVolumes = options.get("delete_volumes").switchValue;
            boolean interactive = options.get("i").switchValue;
            boolean stop = options.get("stop").switchValue;
            boolean waitForFinish = options.get("wait").switchValue;
            boolean versionCleanup = options.get("v").switchValue;
            ONCRPCServiceURL dirURL = options.get("dir").urlValue;
            password = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD).setAuthPasswd(
                AuthPassword.newBuilder()
                        .setPassword(
                            (options.get(utils.OPTION_ADMIN_PASS).stringValue != null) ? options
                                    .get(utils.OPTION_ADMIN_PASS).stringValue : "")).build();
            boolean useSSL = false;
            boolean gridSSL = false;
            String serviceCredsFile = null;
            String serviceCredsPass = null;
            String trustedCAsFile = null;
            String trustedCAsPass = null;
            InetSocketAddress dirAddr = null;
            
            // read default settings for the OSD
            String osdUUID = null;
            if (arguments.get(0).startsWith("uuid:")) {
                osdUUID = arguments.get(0).substring("uuid:".length());
            } else {
                error("There was no UUID for the OSD given!", options);
            }
            
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
                
                if (serviceCredsFile == null) {
                    System.out.println("SSL requires '-" + utils.OPTION_USER_CREDS_FILE
                        + "' parameter to be specified");
                    usage(options);
                    System.exit(1);
                } else if (trustedCAsFile == null) {
                    System.out.println("SSL requires '-" + utils.OPTION_TRUSTSTORE_FILE
                        + "' parameter to be specified");
                    usage(options);
                    System.exit(1);
                }
                
            }
            
            // read default settings
            if (dirURL == null) {
                try {
                    DefaultDirConfig cfg = new DefaultDirConfig(DEFAULT_DIR_CONFIG);
                    cfg.read();
                    
                    dirAddr = cfg.getDirectoryService();
                    useSSL = cfg.isSslEnabled();
                    serviceCredsFile = cfg.getServiceCredsFile();
                    serviceCredsPass = cfg.getServiceCredsPassphrase();
                    trustedCAsFile = cfg.getTrustedCertsFile();
                    trustedCAsPass = cfg.getTrustedCertsPassphrase();
                } catch (IOException e) {
                    error("No DIR service configuration available. Please use the -dir option.", options);
                }
            } else
                dirAddr = new InetSocketAddress(dirURL.getHost(), dirURL.getPort());
            
            // TODO: support custom SSL trust managers
            SSLOptions sslOptions = useSSL ? new SSLOptions(new FileInputStream(serviceCredsFile),
                serviceCredsPass, SSLOptions.PKCS12_CONTAINER, new FileInputStream(trustedCAsFile),
                trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL, null) : null;
            
            if (remove && restore)
                error("Zombies cannot be deleted and restored at the same time!", options);
            
            // connect to the OSD
            osdClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
            osdClient.start();
            osdClient.waitForStartup();
            osd = new OSDServiceClient(osdClient, null);
            
            dirClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
            dirClient.start();
            dirClient.waitForStartup();
            DIRServiceClient dirRpcClient = new DIRServiceClient(dirClient, dirAddr);
            dir = new DIRClient(dirRpcClient, new InetSocketAddress[]{dirAddr}, 100, 15*1000);
            
            try {
                TimeSync.getInstance();
            } catch (RuntimeException re) {
                TimeSync.initialize(dir, 60 * 1000, 30 * 1000);
            }
            
            if (!UUIDResolver.isRunning()) {
                UUIDResolver.start(dir, 1000, 1000);
            }
            ServiceUUID UUIDService = new ServiceUUID(osdUUID);
            UUIDService.resolve();
            osdAddr = UUIDService.getAddress();
            UUIDResolver.shutdown();
            
            try {
                if (versionCleanup) {
                    startVersionCleanup();
                    
                } else if (stop) {
                    if (!isRunningCleanup())
                        error("No cleanup running on the given OSD.", options);
                    stop();
                    for (String result : getResult()) {
                        System.out.println(result);
                    }
                    System.out.println("Cleanup stopped.");
                } else if (interactive) {
                    if (isRunningCleanup())
                        stop();
                    start(remove, deleteVolumes, restore);
                    
                    while (isRunningCleanup()) {
                        System.out.print(getState() + "\r");
                        Thread.sleep(3000);
                    }
                    System.out.println();
                    for (String result : getResult()) {
                        System.out.println(result);
                    }
                    System.out.println("Cleanup done.");
                } else {
                    if (isRunningCleanup())
                        stop();
                    
                    start(remove, deleteVolumes, restore);
                    System.out.println("Cleanup is running.");
                }
                
                if (waitForFinish) {
                    while (isRunningCleanup())
                        Thread.sleep(5000);
                    System.out.println("Cleanup done.");
                }
                
            } catch (Exception e) {
                error("Operation could not be performed because: " + e.getMessage(), options);
            } finally {
                dirClient.shutdown();
                osdClient.shutdown();
                dirClient.waitForShutdown();
                osdClient.waitForShutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
    
    private static boolean isRunningCleanup() throws Exception {
        RPCResponse<xtreemfs_cleanup_is_runningResponse> r = osd.xtreemfs_cleanup_is_running(osdAddr,
            password, RPCAuthentication.userService);
        try {
            return r.get().getIsRunning();
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_WARN, null, e);
            throw new Exception("Status-request for cleanup on the given OSD failed, because: " + e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    private static void start(boolean remove, boolean deleteVolumes, boolean restore) throws Exception {
        RPCResponse r = osd.xtreemfs_cleanup_start(osdAddr, password, RPCAuthentication.userService, remove,
            deleteVolumes, restore);
        try {
            r.get();
        } catch (Exception e) {
            throw new Exception("Cleanup could not be started on the given OSD, because: " + e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    private static void stop() throws Exception {
        RPCResponse r = osd.xtreemfs_cleanup_stop(osdAddr, password, RPCAuthentication.userService);
        try {
            r.get();
        } catch (Exception e) {
            throw new Exception("Cleanup could not be stopped on the given OSD, because: " + e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    private static String getState() throws Exception {
        RPCResponse<xtreemfs_cleanup_statusResponse> r = osd.xtreemfs_cleanup_status(osdAddr, password,
            RPCAuthentication.userService);
        try {
            return r.get().getStatus();
        } catch (Exception e) {
            throw new Exception("Cleanup status could not be retrieved, because: " + e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    private static List<String> getResult() throws Exception {
        RPCResponse<xtreemfs_cleanup_get_resultsResponse> r = osd.xtreemfs_cleanup_get_results(osdAddr,
            password, RPCAuthentication.userService);
        try {
            return r.get().getResultsList();
        } catch (Exception e) {
            throw new Exception("Cleanup results could not be retrieved, because: " + e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    private static void startVersionCleanup() throws Exception {
        RPCResponse<?> r = osd.xtreemfs_cleanup_versions_start(osdAddr, password,
            RPCAuthentication.userService);
        try {
            r.get();
        } catch (Exception e) {
            throw new Exception("Version cleanup could not be started on the given OSD, because: "
                + e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    /**
     * Prints the error <code>message</code> and delegates to usage().
     * 
     * @param message
     */
    private static void error(String message, Map<String, CliOption> options) {
        System.err.println(message);
        System.out.println();
        usage(options);
        System.exit(1);
    }
    
    /**
     * Prints out usage informations and terminates the application.
     */
    public static void usage(Map<String, CliOption> options) {
        
        System.out.println("usage: xtfs_cleanup [options] uuid:<osd_uuid>\n");
        System.out.println("  " + "<osd_uuid> the unique identifier of the OSD to clean\n");
        System.out.println("  " + "options:");

        utils.printOptions(options);
    }
}
