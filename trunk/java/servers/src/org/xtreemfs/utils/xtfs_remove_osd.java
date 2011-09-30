/*
 * Copyright (c) 2009-2011 by Paul Seiferth,
 *                            Zuse Institute Berlin
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
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.foundation.util.ONCRPCServiceURL;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.drain.OSDDrain;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

public class xtfs_remove_osd {

    private static final String DEFAULT_DIR_CONFIG = "/etc/xos/xtreemfs/default_dir";
    private OSDServiceClient osd;
    private DIRClient dir;
    private MRCServiceClient mrc;
    private RPCNIOSocketClient dirClient;
    private RPCNIOSocketClient osdClient;
    private RPCNIOSocketClient mrcClient;
    private InetSocketAddress osdAddr;
    private InetSocketAddress mrcAddr;
    private SSLOptions sslOptions;
    private InetSocketAddress dirAddress;
    private UUIDResolver resolver;
    private RPCNIOSocketClient resolverClient;
    private Auth authHeader;
    private UserCredentials credentials;
    private String osdUUIDString;
    private ServiceUUID osdUUID;

    public static void main(String[] args) {

        Map<String, CliOption> options = null;
        try {
            // parse the call arguments
            options = utils.getDefaultAdminToolOptions(true);
            List<String> arguments = new ArrayList<String>(1);



            CliOption oDir = new CliOption(CliOption.OPTIONTYPE.URL,
                    "directory service to use (e.g. 'pbrpc://localhost:32638')", "<uri>");
            oDir.urlDefaultPort = PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber();
            oDir.urlDefaultProtocol = Schemes.SCHEME_PBRPC;
            options.put("dir", oDir);
            options.put("s", new CliOption(CliOption.OPTIONTYPE.SWITCH, "shutdown OSD", ""));
            options.put("d", new CliOption(CliOption.OPTIONTYPE.SWITCH, "enbable debug output", ""));
            CLIParser.parseCLI(args, options, arguments);
            
            //start logging
            if (options.get("d").switchValue) {
                Logging.start(Logging.LEVEL_DEBUG);
            } else {
                Logging.start(Logging.LEVEL_ERROR);
            }

            if (options.get(utils.OPTION_HELP).switchValue || options.get(utils.OPTION_HELP_LONG).switchValue) {
                usage(options);
                return;
            }

            if (arguments.size() != 1) {
                error("invalid number of arguments", options);
            }


            ONCRPCServiceURL dirURL = options.get("dir").urlValue;
            boolean shutdown = options.get("s").switchValue;
            String password = (options.get(utils.OPTION_ADMIN_PASS).stringValue != null) ? options.get(utils.OPTION_ADMIN_PASS).stringValue : "";
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
                    && (Schemes.SCHEME_PBRPCS.equals(dirURL.getProtocol()) || Schemes.SCHEME_PBRPCG.equals(dirURL.getProtocol()))) {
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
            } else {
                dirAddr = new InetSocketAddress(dirURL.getHost(), dirURL.getPort());
            }

            // TODO: support custom SSL trust managers
            SSLOptions sslOptions = useSSL ? new SSLOptions(new FileInputStream(serviceCredsFile),
                    serviceCredsPass, SSLOptions.PKCS12_CONTAINER, new FileInputStream(trustedCAsFile),
                    trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL, null) : null;


            xtfs_remove_osd removeOsd = new xtfs_remove_osd(dirAddr, osdUUID, sslOptions, password);
            removeOsd.initialize();
            removeOsd.drainOSD(shutdown);
            removeOsd.shutdown();

            System.exit(0);

        } catch (Exception e) {

            error(e.getMessage(), options);
        }
    }

    public xtfs_remove_osd(InetSocketAddress dirAddress, String osdUUIDString, SSLOptions sslOptions,
            String password) throws Exception {
        try {

            this.sslOptions = sslOptions;
            this.dirAddress = dirAddress;
            this.osdUUIDString = osdUUIDString;
            if (password.equals("")) {
                this.authHeader = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
            } else {
                this.authHeader = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD).setAuthPasswd(AuthPassword.newBuilder().setPassword(password).build()).build();
            }

            // TODO: use REAL user credentials (this is a SECURITY HOLE)
            this.credentials = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();


        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    public void initialize() throws Exception {

        TimeSync.initializeLocal(0, 50);

        // connect to DIR
        dirClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
        dirClient.start();
        dirClient.waitForStartup();
        DIRServiceClient tmp = new DIRServiceClient(dirClient, dirAddress);
        dir = new DIRClient(tmp, new InetSocketAddress[]{dirAddress}, 100, 15 * 1000);

        resolverClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
        resolverClient.start();
        resolverClient.waitForStartup();
        this.resolver = UUIDResolver.startNonSingelton(dir,1000, 10 * 10 * 1000);
        
        
        // create OSD client
        osdUUID = new ServiceUUID(osdUUIDString, resolver);
        osdUUID.resolve();
        osdAddr = osdUUID.getAddress();

        osdClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
        osdClient.start();
        osdClient.waitForStartup();
        osd = new OSDServiceClient(osdClient, osdAddr);

        // create MRC client
        ServiceSet sSet = null;
        try {
            sSet = dir.xtreemfs_service_get_by_type(null, authHeader, credentials, ServiceType.SERVICE_TYPE_MRC);
        } catch (IOException ioe) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.proc, new Object(),
                    OutputUtils.stackTraceToString(ioe));
            throw ioe;
        }

        mrcClient = new RPCNIOSocketClient(sslOptions, 100000, 5 * 60 * 10000);
        mrcClient.start();
        mrcClient.waitForStartup();

        if (sSet.getServicesCount() == 0) {
            throw new IOException("No MRC is currently registred at DIR");
        }

        String mrcUUID = sSet.getServices(0).getUuid();
        ServiceUUID UUIDService = new ServiceUUID(mrcUUID, resolver);
        UUIDService.resolve();
        mrcAddr = UUIDService.getAddress();

        mrc = new MRCServiceClient(mrcClient, mrcAddr);

    }

    public void shutdown() {
        try {

            UUIDResolver.shutdown(resolver);
            resolverClient.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes (drain) an OSD.
     * 
     * @throws Exception
     */
    public void drainOSD(boolean shutdown) throws Exception {
        OSDDrain osdDrain = new OSDDrain(dir, osd, mrc, osdUUID, authHeader, credentials, resolver);
        osdDrain.drain(shutdown);
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

    public static void usage(Map<String, CliOption> options) {

        System.out.println("usage: xtfs_remove_osd [options] uuid:<osd_uuid>\n");
        System.out.println("  " + "<osd_uuid> the unique identifier of the OSD to be removed\n");
        System.out.println("  " + "options:");

        utils.printOptions(options);
    }
}
