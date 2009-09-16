/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.utils;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.ErrorCodes;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.DIRInterface.DIRException;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 * 
 *
 * @since 09/16/2009
 * @author flangner
 */

public class xtfs_dirreplicationtool {
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        Logging.start(Logging.LEVEL_ERROR);
        TimeSync.initialize(null, 60000, 50);
        
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(3);
        options.put("dir", new CliOption(CliOption.OPTIONTYPE.URL));
        options.put("c", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("cpass", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("t", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("tpass", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("p", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put("adminpass", new CliOption(CliOption.OPTIONTYPE.STRING));
        
        try {
            CLIParser.parseCLI(args, options, arguments);
        } catch (Exception exc) {
            System.out.println(exc);
            usage();
            return;
        }
        
        CliOption h = options.get("h");
        if (h.switchValue) {
            usage();
            return;
        }
        
        CliOption dir = options.get("dir");
        if (dir.urlValue == null) {
            System.out.println("missing DIR URL");
            usage();
            return;
        }
        
        if (arguments.size() < 2) {
            usage();
            return;
        }
        
        String op = arguments.get(0);
        if (!"toMaster".equals(op)) {
            System.out.println("invalid operation: " + op);
            usage();
            return;
        }
        
        CliOption c = options.get("c");
        CliOption cp = options.get("cpass");
        CliOption t = options.get("t");
        CliOption tp = options.get("tpass");
        CliOption p = options.get("p");
        
        String host = dir.urlValue.getHost();
        int port = dir.urlValue.getPort();
        String protocol = dir.urlValue.getProtocol();
        
        RPCNIOSocketClient rpcClient = null;
        
        try {
            
            SSLOptions sslOptions = protocol.startsWith("https") ? new SSLOptions(new FileInputStream(
                c.stringValue), cp.stringValue, new FileInputStream(t.stringValue), tp.stringValue) : null;
            rpcClient = new RPCNIOSocketClient(sslOptions, Integer.MAX_VALUE - 1000, Integer.MAX_VALUE);
            rpcClient.start();
            DIRClient client = new DIRClient(rpcClient, new InetSocketAddress(host, port));
            
            StringSet gids = new StringSet();
            gids.add("");
            UserCredentials creds = new UserCredentials("", gids, p == null ? "" : p.stringValue);
            
            if (op.equals("toMaster")) {
                RPCResponse<Object> r = null;
                try {
                    r = client.replication_toMaster(null, creds);
                    r.get();
                } finally {
                    if (r != null)
                        r.freeBuffers();
                }
            } else
                usage();
            
        } catch (DIRException exc) {
            if (exc.getError_code() == ErrorCodes.AUTH_FAILED)
                System.out.println("permission denied: invalid administrator password");
            else
                exc.printStackTrace();
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            if (rpcClient != null)
                rpcClient.shutdown();
        }
    }
    
    private static void usage() {
        System.out
                .println("usage: xtfs_dirreplicationtool -dir <dir_URL> [-p <admin_passphrase>] [-c <creds_file>] [-cp <creds_passphrase>] [-t <trusted_CAs>] [-tp <trusted_passphrase>] toMaster");
        System.exit(1);
    }
    
}
