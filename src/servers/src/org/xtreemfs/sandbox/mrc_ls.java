/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.ONCRPCServiceURL;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DirectoryEntry;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.utils.CLIParser;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 *
 * @author bjko
 */
public class mrc_ls {


    public static void main(String[] args) {
        try {
            Map<String, CliOption> options = new HashMap<String, CliOption>();
            List<String> arguments = new ArrayList<String>(1);

            CLIParser.parseCLI(args, options, arguments);

            if (arguments.size() != 2) {
                return;
            }

            TimeSync.initialize(null, 60*1000, 50);
            Logging.start(Logging.LEVEL_WARN);

            ONCRPCServiceURL mrcUrl = new ONCRPCServiceURL(arguments.get(0),Constants.ONCRPC_SCHEME,MRCInterface.ONCRPC_PORT_DEFAULT);
            String path = arguments.get(1);

            RPCNIOSocketClient rpcClient = new RPCNIOSocketClient(null, 15*1000, 5*60*1000);
            rpcClient.start();
            rpcClient.waitForStartup();

            MRCClient c = new MRCClient(rpcClient,new InetSocketAddress(mrcUrl.getHost(),mrcUrl.getPort()));

            final List<String> groups = new ArrayList(1);
            groups.add("test");
            final UserCredentials uc = MRCClient.getCredentials("test", groups);

            RPCResponse<DirectoryEntrySet> r = c.readdir(null, uc, path);
            DirectoryEntrySet entries = r.get();
            r.freeBuffers();
            for (DirectoryEntry e : entries) {
                final Stat s = e.getStbuf().get(0);
                System.out.println(e.getName()+"\t"+s.getMode()+"\t"+OutputUtils.formatBytes(s.getSize())+"\t"+
                        s.getAttributes());
            }
            rpcClient.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
