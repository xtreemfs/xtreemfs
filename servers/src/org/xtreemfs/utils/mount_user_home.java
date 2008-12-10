/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils;

import java.io.File;
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
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 *
 * @author bjko
 */
public final class mount_user_home {
    
    

    public static void main(String[] args) {
        try {
            
            Logging.start(Logging.LEVEL_ERROR);

            Map<String,CLIParser.CliOption> options = new HashMap();
            List<String> arguments = new ArrayList(3);
            options.put("d",new CliOption(CliOption.OPTIONTYPE.URL));
            
            CLIParser.parseCLI(args, options, arguments);
            
            if (arguments.size() != 1) {
                System.out.println("Usage: locate_volume <userID>");
                System.out.println("options: ");
                System.out.println("  -d http(s)://<host>:<port> specifies the URL to the directory service");
                System.out.println("     by default, the directory service set in /etc/xos/xtreemfs/default_dir is used");
                System.out.println("");
                System.exit(1);
            }
            
            URL dirUrl = options.get("d").urlValue;
            
            InetSocketAddress dirAddr = null;
            
            if (dirUrl == null) {
                File defaultDir = new File("/etc/xos/xtreemfs/default_dir");
                if (!defaultDir.exists()) {
                    System.err.println("Cannot read Directory Service URL from "+defaultDir.getAbsolutePath());
                    System.err.println("Please create file or specify with -d <URL>");
                    System.exit(1);
                } else {
                    DefaultDirConfig c = new DefaultDirConfig(defaultDir.getAbsolutePath());
                    dirAddr = c.getDirectoryService();
                }
            } else {
                dirAddr = new InetSocketAddress(dirUrl.getHost(),dirUrl.getPort());
            }
            
            DIRClient dir = new DIRClient(null, dirAddr);
            Map<String,Object> qry = new HashMap();
            qry.put("type","volume");
            qry.put("name","user-"+arguments.get(0));
            RPCResponse<Map<String,Map<String,Object>>> r = dir.getEntities(qry, null, NullAuthProvider.createAuthString("nobody", "lookupdirurl")); 
            Map<String,Map<String,Object>> map = r.get();
            
            if (map.size() == 0)
                System.exit(1);
            
            String volURL = null;
            for (String uuid : map.keySet()) {
                Map<String,Object> data = map.get(uuid);
                volURL = data.get("mrc")+"/"+data.get("name");
                break;
            }
            dir.shutdown();
            
            if (volURL == null) {
                System.err.println("Home volume for user "+arguments.get(0)+" does not exist!");
                System.exit(1);
            } else {
                File gridhome = new File(System.getenv().get("HOME")+"/gridhome");
                if (!gridhome.exists()) {
                    gridhome.mkdirs();
                }
                System.out.println("executing: "+"xtfs_mount -o volume_url="+volURL+" "+gridhome.getAbsolutePath());
                Process mount = Runtime.getRuntime().exec("xtfs_mount -o volume_url="+volURL+" "+gridhome.getAbsolutePath());
                mount.waitFor();
                if (mount.exitValue() != 0) {
                    System.err.println("cannot mount gridhome... exiting");
                    System.exit(1);
                }
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
