/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 * 
 * @author bjko
 */
public final class locate_user_home {
    
    public static final String HOMEDIR_PREFIX = "user-";
    
    public static void main(String[] args) {
        try {
            
            Logging.start(Logging.LEVEL_ERROR);
            
            Map<String, CLIParser.CliOption> options = new HashMap();
            List<String> arguments = new ArrayList(3);
            options.put("d", new CliOption(CliOption.OPTIONTYPE.URL));
            
            CLIParser.parseCLI(args, options, arguments);
            
            if (arguments.size() != 1) {
                System.out.println("Usage: locate_user_home <userID>");
                System.out.println("       This utility retrieves the volume URL for a volume named");
                System.out.println("       '" + HOMEDIR_PREFIX + "<userID>' from the directory service");
                System.out.println("options: ");
                System.out.println("  -d http://<host>:<port> specifies the URL to the directory service");
                System.out
                        .println("     by default, the directory service set in /etc/xos/xtreemfs/default_dir is used");
                System.out.println("     The -d option does not work with SSL, use the config file instead.");
                System.out.println("");
                System.out
                        .println("This utility will return the first matching volume URL and an exit status 0 on success. "
                            + "If no matching volume can be found an exit status of 2 is returned. If an error occurs (e.g. directory "
                            + "service not available) an exit status of 1 is returned.");
                System.out.println("");
                System.exit(1);
            }
            
            URL dirUrl = options.get("d").urlValue;
            
            InetSocketAddress dirAddr = null;
            SSLOptions sslopts = null;
            
            if (dirUrl == null) {
                File defaultDir = new File("/etc/xos/xtreemfs/default_dir");
                if (!defaultDir.exists()) {
                    System.err.println("Cannot read Directory Service URL from "
                        + defaultDir.getAbsolutePath());
                    System.err.println("Please create file or specify with -d <URL>");
                    System.exit(1);
                } else {
                    DefaultDirConfig c = new DefaultDirConfig(defaultDir.getAbsolutePath());
                    c.read();
                    dirAddr = c.getDirectoryService();
                    
                    if (c.isSslEnabled()) {
                        sslopts = new SSLOptions(new FileInputStream(c.getServiceCredsFile()), c
                                .getServiceCredsPassphrase(), c.getServiceCredsContainer(),
                            new FileInputStream(c.getTrustedCertsFile()), c.getTrustedCertsPassphrase(), c
                                    .getTrustedCertsContainer(), false);
                    }
                }
            } else {
                dirAddr = new InetSocketAddress(dirUrl.getHost(), dirUrl.getPort());
            }
            
            DIRClient dir = null;
            if (sslopts == null)
                dir = new DIRClient(null, dirAddr, 5000);
            else
                dir = new DIRClient(dirAddr, sslopts, 5000);
            
            String volURL = dir.locateUserHome(arguments.get(0), NullAuthProvider.createAuthString(
                "locate_user_home", "utils"));
            dir.shutdown();
            
            if (volURL == null) {
                System.err.println("Home volume for user " + arguments.get(0) + " does not exist!");
                System.exit(2);
            } else {
                System.out.println(volURL);
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
    
}
