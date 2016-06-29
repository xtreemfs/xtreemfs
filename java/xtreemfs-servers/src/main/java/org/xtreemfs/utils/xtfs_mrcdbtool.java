/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Jan Stender,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;

public class xtfs_mrcdbtool {
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        Logging.start(Logging.LEVEL_ERROR);
        try {
            TimeSync.initializeLocal(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        
        Map<String, CliOption> options = utils.getDefaultAdminToolOptions(true);
        List<String> arguments = new ArrayList<String>(3);
        CliOption oMrc = new CliOption(CliOption.OPTIONTYPE.URL,
            "MRC to use (e.g. 'pbrpc://localhost:32636')", "<uri>");
        oMrc.urlDefaultPort = PORTS.MRC_PBRPC_PORT_DEFAULT.getNumber();
        oMrc.urlDefaultProtocol = Schemes.SCHEME_PBRPC;
        options.put("mrc", oMrc);
        
        try {
            CLIParser.parseCLI(args, options, arguments);
        } catch (Exception exc) {
            System.out.println(exc);
            usage(options);
            System.exit(1);
        }
        
        CliOption h = options.get(utils.OPTION_HELP);
        CliOption help = options.get(utils.OPTION_HELP_LONG);
        if (h.switchValue || help.switchValue) {
            usage(options);
            System.exit(0);
        }
        
        CliOption mrc = options.get("mrc");
        if (mrc.urlValue == null) {
            System.out.println("missing MRC URL");
            usage(options);
            System.exit(1);
        }
        
        if (arguments.size() < 2) {
            usage(options);
            System.exit(1);
        }
        
        String op = arguments.get(0);
        if (!"dump".equals(op) && !"restore".equals(op)) {
            System.out.println("invalid operation: " + op);
            usage(options);
            System.exit(1);
        }
        
        String dumpFile = arguments.get(1);
        
        CliOption c = options.get(utils.OPTION_USER_CREDS_FILE);
        String cp = options.get(utils.OPTION_USER_CREDS_PASS).stringValue;
        if(cp != null && cp.equals("-")) {
        	cp = new String(System.console().readPassword("Enter credentials password: "));
        }
        CliOption t = options.get(utils.OPTION_TRUSTSTORE_FILE);
        String tp = options.get(utils.OPTION_TRUSTSTORE_PASS).stringValue;
        if(tp != null && tp.equals("-")) {
        	tp = new String(System.console().readPassword("Enter trust store password: "));
        }
        
        String sslProtocolString = options.get(utils.OPTION_SSL_PROTOCOL).stringValue;
        
        String host = mrc.urlValue.getHost();
        int port = mrc.urlValue.getPort();
        String protocol = mrc.urlValue.getProtocol();
        
        RPCNIOSocketClient rpcClient = null;
        
        try {
            SSLOptions sslOptions = null;
            boolean gridSSL = false;
            if (protocol.startsWith(Schemes.SCHEME_PBRPCS) || protocol.startsWith(Schemes.SCHEME_PBRPCG)) {
                if (c.stringValue == null) {
                    System.out.println("SSL requires '-" + utils.OPTION_USER_CREDS_FILE + "' parameter to be specified");
                    usage(options);
                    System.exit(1);
                } else if (t.stringValue == null) {
                    System.out.println("SSL requires '-" + utils.OPTION_TRUSTSTORE_FILE + "' parameter to be specified");
                    usage(options);
                    System.exit(1);
                }
                
                if (protocol.startsWith(Schemes.SCHEME_PBRPCG)) {
                    gridSSL = true;
                }

                sslOptions = new SSLOptions(c.stringValue, cp, SSLOptions.PKCS12_CONTAINER, t.stringValue, tp,
                        SSLOptions.JKS_CONTAINER, false, gridSSL, sslProtocolString, null);
            }
            rpcClient = new RPCNIOSocketClient(sslOptions, Integer.MAX_VALUE - 1000, Integer.MAX_VALUE, "xtfs_mrcdbtool");
            rpcClient.start();
            MRCServiceClient client = new MRCServiceClient(rpcClient, new InetSocketAddress(host, port));
            
            Auth passwdAuth = RPCAuthentication.authNone;
            if (options.get(utils.OPTION_ADMIN_PASS).stringValue != null)
                passwdAuth = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD).setAuthPasswd(
                    AuthPassword.newBuilder().setPassword(options.get(utils.OPTION_ADMIN_PASS).stringValue)).build();
            
            if (op.equals("dump")) {
                RPCResponse<?> r = null;
                try {
                    r = client.xtreemfs_dump_database(null, passwdAuth, RPCAuthentication.userService,
                        dumpFile);
                    r.get();
                } finally {
                    if (r != null)
                        r.freeBuffers();
                }
            } else if (op.equals("restore")) {
                RPCResponse<?> r = null;
                try {
                    r = client.xtreemfs_restore_database(null, passwdAuth, RPCAuthentication.userService,
                        dumpFile);
                    r.get();
                } finally {
                    if (r != null)
                        r.freeBuffers();
                }
            } else {
                usage(options);
                System.exit(1);
            }
            
        } catch (PBRPCException exc) {
            if (exc.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_EACCES) {
                System.out.println("permission denied: admin password invalid or volumes exist already");
            } else {
                exc.printStackTrace();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            if (rpcClient != null) {
                rpcClient.shutdown();
            }
        }
    }
    
    public static void usage(Map<String, CliOption> options) {
        System.out.println("usage: xtfs_mrcdbtool [options] dump|restore <dump_file>\n");
        System.out.println("  " + "<dump_file> the file for the dump\n");
        System.out.println("  " + "options:");
        
        utils.printOptions(options);
    }
    
}
