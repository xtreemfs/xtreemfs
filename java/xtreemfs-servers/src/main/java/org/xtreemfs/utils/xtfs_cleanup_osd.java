/*
 * Copyright (c) 2009 by Felix Langner, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.foundation.util.CLIParser.CliOption.OPTIONTYPE;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;

/**
 * Console-tool for the cleanUp-functionality of the XtreemFS OSD.
 * 
 * 24.04.2009
 * 
 * @author flangner
 */
public class xtfs_cleanup_osd {

    private static String      password;

    private static AdminClient client;

    /**
     * Main method.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        Logging.start(Logging.LEVEL_WARN);

        // parse the call arguments
        Map<String, CliOption> options = utils.getDefaultAdminToolOptions(true);
        List<String> arguments = new ArrayList<String>(1);
        options.put("r", new CliOption(CliOption.OPTIONTYPE.SWITCH, "restore zombies found on the OSD", ""));
        options.put("e", new CliOption(CliOption.OPTIONTYPE.SWITCH, "erase potential zombies", ""));
        options.put("delete_volumes", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "!dangerous! deletes volumes that might be dead", ""));
        CliOption oDelMeta = new CliOption(CliOption.OPTIONTYPE.NUMBER,
                "delete metadata of zombie files, if the XLocSet has not been updated during the last <timeout> seconds (default: 600)",
                "<timeout>");
        options.put("metadata_timeout", oDelMeta);
        options.put("metadata_keep", new CliOption(OPTIONTYPE.SWITCH,
                "keep metadata (by default metadata is deleted after <timeout> seconds)", ""));
        options.put("i", new CliOption(CliOption.OPTIONTYPE.SWITCH, "interactive mode", ""));
        options.put("stop", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "suspends the currently running cleanup process", ""));
        options.put("wait", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "blocks call until the currently running cleanup process has terminated", ""));
        CliOption oDir = new CliOption(CliOption.OPTIONTYPE.STRING,
                "directory services to use (comma separated, e.g. 'pbrpc://localhost:32638',..)", "<uri>");
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

        boolean removeMetadata = !options.get("metadata_keep").switchValue;
        int metaDataTimeoutS = 600;
        if (options.get("metadata_timeout").numValue != null) {
            metaDataTimeoutS = options.get("metadata_timeout").numValue.intValue();
        }

        String[] dirURLs = (options.get("dir").stringValue != null) ? options.get("dir").stringValue
                .split(",") : null;

        password = (options.get(utils.OPTION_ADMIN_PASS).stringValue != null) ? options
                .get(utils.OPTION_ADMIN_PASS).stringValue : "";
        if (password.equals("-")) {
            password = utils.readPassword("Enter admin password: ");
        }

        SSLOptions sslOptions = null;
        String[] dirAddrs = null;

        // read default settings for the OSD
        String osdUUID = null;
        if (arguments.get(0).startsWith("uuid:")) {
            osdUUID = arguments.get(0).substring("uuid:".length());
        } else {
            error("There was no UUID for the OSD given!", options);
        }

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
                    	serviceCredsPass = utils.readPassword("Enter credentials password: ");
                    }
                    String trustedCAsFile = options.get(utils.OPTION_TRUSTSTORE_FILE).stringValue;
                    String trustedCAsPass = options.get(utils.OPTION_TRUSTSTORE_PASS).stringValue;
                    if(trustedCAsPass != null && trustedCAsPass.equals("-")) {
                    	trustedCAsPass = utils.readPassword("Enter trust store password: ");
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

        if (remove && restore) {
            error("Zombies cannot be deleted and restored at the same time!", options);
        }

        Options userOptions = new Options();
        UserCredentials userCredentials = UserCredentials.newBuilder().setUsername("root").addGroups("root")
                .build();
        client = ClientFactory.createAdminClient(ClientType.NATIVE, dirAddrs, userCredentials, sslOptions, userOptions);
        try {
            client.start();
        } catch (Exception e) {
            System.err.println("unable to cleanup OSD, because:" + e.getMessage());
            System.exit(1);
        }
        try {
            if (versionCleanup) {
                client.startVersionCleanUp(osdUUID, password);

            } else if (stop) {
                if (!client.isRunningCleanUp(osdUUID, password))
                    error("No cleanup running on the given OSD.", options);
                client.stopCleanUp(osdUUID, password);
                for (String result : client.getCleanUpResult(osdUUID, password)) {
                    System.out.println(result);
                }
                System.out.println("Cleanup stopped.");

            } else if (interactive) {
                if (client.isRunningCleanUp(osdUUID, password))
                    client.stopCleanUp(osdUUID, password);
                client.startCleanUp(osdUUID, password, remove, deleteVolumes, restore, removeMetadata, metaDataTimeoutS);

                while (client.isRunningCleanUp(osdUUID, password)) {
                    System.out.print(client.getCleanUpState(osdUUID, password) + "\r");
                    Thread.sleep(3000);
                }
                System.out.println();
                for (String result : client.getCleanUpResult(osdUUID, password)) {
                    System.out.println(result);
                }
                System.out.println("Cleanup done.");
            } else {
                if (client.isRunningCleanUp(osdUUID, password)) {
                    client.stopCleanUp(osdUUID, password);
                }
                client.startCleanUp(osdUUID, password, remove, deleteVolumes, restore, removeMetadata, metaDataTimeoutS);
                System.out.println("Cleanup is running.");
            }

            if (waitForFinish) {
                while (client.isRunningCleanUp(osdUUID, password))
                    Thread.sleep(5000);
                System.out.println("Cleanup done.");
            }

        } catch (Exception e) {
            System.err.println("error while running cleanup:" + e.getMessage());
            client.shutdown();
            System.exit(1);
        }
        client.shutdown();
        System.exit(0);
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
     * Prints out usage information and terminates the application.
     */
    public static void usage(Map<String, CliOption> options) {

        System.out.println("usage: xtfs_cleanup [options] uuid:<osd_uuid>\n");
        System.out.println("  " + "<osd_uuid> the unique identifier of the OSD to clean\n");
        System.out.println("  " + "options:");

        utils.printOptions(options);
    }
}
