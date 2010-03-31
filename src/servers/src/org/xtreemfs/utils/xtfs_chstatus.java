/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.ONCRPCServiceURL;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.utils.CLIParser.CliOption;

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
            Map<String, CliOption> options = new HashMap<String, CliOption>();
            List<String> arguments = new ArrayList<String>(1);
            options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            CliOption oDir = new CliOption(CliOption.OPTIONTYPE.URL);
            oDir.urlDefaultPort = DIRInterface.ONC_RPC_PORT_DEFAULT;
            oDir.urlDefaultProtocol = Constants.ONCRPC_SCHEME;
            options.put("dir", oDir);
            options.put("chk", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("thrs", new CliOption(CliOption.OPTIONTYPE.NUMBER));
            options.put("c", new CliOption(CliOption.OPTIONTYPE.STRING));
            options.put("cpass", new CliOption(CliOption.OPTIONTYPE.STRING));
            options.put("t", new CliOption(CliOption.OPTIONTYPE.STRING));
            options.put("tpass", new CliOption(CliOption.OPTIONTYPE.STRING));
            options.put("-help", new CliOption(CliOption.OPTIONTYPE.SWITCH));

            CLIParser.parseCLI(args, options, arguments);

            if (arguments.size() > 2 || arguments.size() < 1 || options.get("h").switchValue == true || options.get("-help").switchValue == true) {
                usage();
                return;
            }

            InetSocketAddress dirAddr = null;
            boolean useSSL = false;
            boolean gridSSL = false;
            String serviceCredsFile = null;
            String serviceCredsPass = null;
            String trustedCAsFile = null;
            String trustedCAsPass = null;

            ONCRPCServiceURL dirURL = options.get("dir").urlValue;

            // parse security info if protocol is 'https'
            if (dirURL != null && (Constants.ONCRPCS_SCHEME.equals(dirURL.getProtocol()) || Constants.ONCRPCG_SCHEME.equals(dirURL.getProtocol()))) {
                useSSL = true;
                serviceCredsFile = options.get("c").stringValue;
                serviceCredsPass = options.get("cpass").stringValue;
                trustedCAsFile = options.get("t").stringValue;
                trustedCAsPass = options.get("tpass").stringValue;
                if (Constants.ONCRPCG_SCHEME.equals(dirURL.getProtocol())) {
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

            SSLOptions sslOptions = useSSL ? new SSLOptions(new FileInputStream(serviceCredsFile),
                    serviceCredsPass, SSLOptions.PKCS12_CONTAINER, new FileInputStream(trustedCAsFile),
                    trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL) : null;


            final String uuid = arguments.get(0);

            final String newStatus = (arguments.size() == 2) ? arguments.get(1) : null;

            RPCNIOSocketClient rpcClient = new RPCNIOSocketClient(sslOptions, 15*100, 5*60*1000, Client.getExceptionParsers());
            rpcClient.start();
            rpcClient.waitForStartup();
            DIRClient dc = new DIRClient(rpcClient, dirAddr);
            RPCResponse<ServiceSet> r = dc.xtreemfs_service_get_by_uuid(dirAddr, uuid);
            ServiceSet set = r.get();
            r.freeBuffers();

            if (set.size() == 0) {
                System.out.println("no service with UUID "+uuid+" registered at directory service");
                System.exit(1);
            }

            Service s = set.get(0);
            if (s.getData().get(HeartbeatThread.STATUS_ATTR) != null) {
                System.out.print("current status: ");
                int status = Integer.valueOf(s.getData().get(HeartbeatThread.STATUS_ATTR));
                switch (status) {
                    case Constants.SERVICE_STATUS_AVAIL : System.out.println(status+" (online)"); break;
                    case Constants.SERVICE_STATUS_TO_BE_REMOVED : System.out.println(status+" (locked for removal)"); break;
                    case Constants.SERVICE_STATUS_REMOVED : System.out.println(status+" (removed, dead)"); break;
                }
            }

            if (newStatus != null) {
                //change status
                String newStatusInt = "";
                if (newStatus.equalsIgnoreCase("online")) {
                    newStatusInt = Integer.toString(Constants.SERVICE_STATUS_AVAIL);
                } else if (newStatus.equalsIgnoreCase("locked")) {
                    newStatusInt = Integer.toString(Constants.SERVICE_STATUS_TO_BE_REMOVED);
                } else if (newStatus.equalsIgnoreCase("removed")) {
                    newStatusInt = Integer.toString(Constants.SERVICE_STATUS_REMOVED);
                } else {
                    System.out.println("unknown status name: "+newStatus+". Must be 'online', ' locked' or 'removed'");
                }
                s.getData().put(HeartbeatThread.STATUS_ATTR, newStatusInt);
                RPCResponse r2 = dc.xtreemfs_service_register(dirAddr, s);
                r2.get();
                r2.freeBuffers();
                System.out.println("status changed to: "+newStatus);
            }



            rpcClient.shutdown();
            TimeSync.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void usage() {
        System.out.println("usage: xtfs_chstatus [options] <service UUID> [online|locked|removed]");
        System.out.println("  -dir uri  directory service to use (e.g. 'oncrpc://localhost:32638')");
        System.out.println("            If no URI is specified, URI and security settings are taken from '" + DEFAULT_DIR_CONFIG + "'");
        System.out.println("            In case of a secured URI ('oncrpcs://...'), it is necessary to also specify SSL credentials:");
        System.out.println("              -c  <creds_file>         a PKCS#12 file containing user credentials");
        System.out.println("              -cpass <creds_passphrase>   a pass phrase to decrypt the the user credentials file");
        System.out.println("              -t  <trusted_CAs>        a PKCS#12 file containing a set of certificates from trusted CAs");
        System.out.println("              -tpass <trusted_passphrase> a pass phrase to decrypt the trusted CAs file");
        System.out.println("  -h        show usage info");
    }

}
