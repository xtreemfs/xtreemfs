/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;

/**
 * 
 * @author bjko
 */
public class xtfs_chstatus {

    public static void main(String[] args) {

        Logging.start(Logging.LEVEL_WARN);

        Map<String, CliOption> options = utils.getDefaultAdminToolOptions(false);
        List<String> arguments = new ArrayList<String>(1);
        CliOption oDir = new CliOption(
                CliOption.OPTIONTYPE.STRING,
                "directory service to use (e.g. 'pbrpc://localhost:32638'). If no URI is specified, URI and security settings are taken from '"
                        + DefaultDirConfig.DEFAULT_DIR_CONFIG + "'", "<uri>");
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

        String[] dirURLs = (options.get("dir").stringValue != null) ? options.get("dir").stringValue
                .split(",") : null;

        SSLOptions sslOptions = null;
        String[] dirAddrs = null;

        // parse security info if protocol is 'https'
        if (dirURLs != null) {
            int i = 0;
            boolean gridSSL = false;
            dirAddrs = new String[dirURLs.length];
            for (String dirURL : dirURLs) {

                // parse security info if protocol is 'https'
                if (dirURL.contains(Schemes.SCHEME_PBRPCS + "://")
                        || dirURL.contains(Schemes.SCHEME_PBRPCG + "://") && sslOptions == null) {
                    String serviceCredsFile = options.get(utils.OPTION_USER_CREDS_FILE).stringValue;
                    String serviceCredsPass = options.get(utils.OPTION_USER_CREDS_PASS).stringValue;
                    if(serviceCredsPass != null && serviceCredsPass.equals("-")) {
                    	serviceCredsPass = new String(System.console().readPassword("Enter credentials password: "));
                    }
                    String trustedCAsFile = options.get(utils.OPTION_TRUSTSTORE_FILE).stringValue;
                    String trustedCAsPass = options.get(utils.OPTION_TRUSTSTORE_PASS).stringValue;
                    if(trustedCAsPass != null && trustedCAsPass.equals("-")) {
                    	trustedCAsPass = new String(System.console().readPassword("Enter credentials password: "));
                    }
                    String sslProtocolString = options.get(utils.OPTION_SSL_PROTOCOL).stringValue;
                    if (dirURL.contains(Schemes.SCHEME_PBRPCG + "://")) {
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

                    // TODO: support custom SSL trust managers
                    try {
                        sslOptions = new SSLOptions(serviceCredsFile, serviceCredsPass, SSLOptions.PKCS12_CONTAINER,
                                trustedCAsFile, trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL,
                                sslProtocolString, null);
                    } catch (Exception e) {
                        System.err.println("unable to get SSL options, because:" + e.getMessage());
                        System.exit(1);
                    }
                }

                // add URL to dirAddrs
                if (dirURL.contains("://")) {
                    // remove Protocol information
                    String[] tmp = dirURL.split("://");
                    // remove possible slash
                    dirAddrs[i++] = tmp[1].replace("/", "");
                } else {
                    // remove possible slash
                    dirAddrs[i++] = dirURL.replace("/", "");
                }
            }

        }

        // read default settings
        if (dirURLs == null) {

            try {
                DefaultDirConfig cfg = new DefaultDirConfig();
                sslOptions = cfg.getSSLOptions();
                dirAddrs = cfg.getDirectoryServices();
            } catch (Exception e) {
                System.err.println("unable to get SSL options, because: " + e.getMessage());
                System.exit(1);
            }
        }

        final String uuid = arguments.get(0);

        String newStatusName = (arguments.size() == 2) ? arguments.get(1) : null;

        AdminClient client = ClientFactory.createAdminClient(ClientType.NATIVE, dirAddrs, RPCAuthentication.userService,
                sslOptions, new Options());
        try {
            client.start();
        } catch (Exception e) {
            System.err.println("unable to start client, because:");
        }

        ServiceStatus oldStatus = null;
        try {
            oldStatus = client.getOSDServiceStatus(uuid);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        System.out.print("current status: ");
        switch (oldStatus) {
        case SERVICE_STATUS_AVAIL:
            System.out.println("online");
            break;
        case SERVICE_STATUS_TO_BE_REMOVED:
            System.out.println("locked (read-only)");
            break;
        case SERVICE_STATUS_REMOVED:
            System.out.println("removed");
            break;
        }

        if (newStatusName != null) {
            // change status
            ServiceStatus newStatus = null;
            if (newStatusName.equalsIgnoreCase("online")) {
                newStatus = ServiceStatus.SERVICE_STATUS_AVAIL;
            } else if (newStatusName.equalsIgnoreCase("locked")) {
                newStatus = ServiceStatus.SERVICE_STATUS_TO_BE_REMOVED;
                newStatusName = "locked (read-only)";
            } else if (newStatusName.equalsIgnoreCase("removed")) {
                newStatus = ServiceStatus.SERVICE_STATUS_REMOVED;
            } else {
                System.out.println("unknown status name: " + newStatusName
                        + ". Must be 'online', ' locked' or 'removed'");
                System.exit(1);
            }
            try {
                client.setOSDServiceStatus(uuid, newStatus);
            } catch (IOException e) {
                System.err.println("unable to set new status at DIR, because " + e.getMessage());
                System.exit(1);
            }
            System.out.println("status changed to: " + newStatusName);
        }
        client.shutdown();
        System.exit(0);
    }

    private static void error(String message, Map<String, CliOption> options) {
        System.err.println(message);
        System.out.println();
        usage(options);
        System.exit(1);
    }

    private static void usage(Map<String, CliOption> options) {

        System.out.println("usage: xtfs_chstatus [options] <OSD UUID> [online|locked|removed]");
        System.out.println("<OSD UUID> the registered UUID for which the status is supposed to be changed");
        System.out.println(" online - Marks the OSD as online.");
        System.out.println(" locked - Marks the OSD as locked (locked OSDs will not be assigned to new files).");
        System.out.println("removed - Marks the OSD as no longer available. Replicas on this OSD can be replaced by xtfs_scrub.");
        System.out.println();
        System.out.println("Options:");
        utils.printOptions(options);
    }

}
