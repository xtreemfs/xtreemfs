/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;

/**
 *
 * @author bjko
 */
public class lookupdirurl {

    public static void main(String[] args) {
        try {

            if (args.length != 3) {
                System.out.println("usage: lookupdirurl <dir_address> <dir_port> <volume_name>\n");
                System.exit(1);
            }
            Logging.start(Logging.LEVEL_ERROR);
            // remove leading and trailing quotes
            if (args[2].charAt(0) == '"' && args[2].charAt(args[2].length() - 1) == '"')
                args[2] = args[2].substring(1, args[2].length() - 1);

            // remove all backslashes with spaces
            String volname = args[2].replaceAll("\\\\ ", " ");
            
            String diraddr = args[0];
            
            int    dirport = Integer.valueOf(args[1]);

            DIRClient dir = new DIRClient(null, new InetSocketAddress(diraddr,dirport));
            Map<String,Object> qry = new HashMap();
            qry.put("type","volume");
            qry.put("name",volname);
            RPCResponse<Map<String,Map<String,Object>>> r = dir.getEntities(qry, null, NullAuthProvider.createAuthString("nobody", "lookupdirurl")); 
            Map<String,Map<String,Object>> map = r.get();
            
            if (map.size() == 0)
                System.exit(1);
            
            for (String uuid : map.keySet()) {
                Map<String,Object> data = map.get(uuid);
                System.out.println(data.get("mrc")+"/"+data.get("name"));
                break;
            }
            dir.shutdown();

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
