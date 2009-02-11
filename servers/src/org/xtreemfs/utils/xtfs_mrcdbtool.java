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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.utils.CLIParser.CliOption;

public class xtfs_mrcdbtool {
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        Logging.start(Logging.LEVEL_ERROR);
        TimeSync.initialize(null, 60000, 50, null);
        
        String authString = null;
        try {
            authString = NullAuthProvider.createAuthString(System.getProperty("user.name"), System
                    .getProperty("user.name"));
        } catch (JSONException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(3);
        options.put("mrc", new CliOption(CliOption.OPTIONTYPE.URL));
        options.put("c", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("cp", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("t", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("tp", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        
        try {
            CLIParser.parseCLI(args, options, arguments);
        } catch (Exception exc) {
            System.out.println(exc);
            usage();
            return;
        }
        
        CliOption h = options.get("h");
        if (h.switchValue != null) {
            usage();
            return;
        }
        
        CliOption mrc = options.get("mrc");
        if (mrc.urlValue == null) {
            System.out.println("missing MRC URL");
            usage();
            return;
        }
        
        if (arguments.size() != 2) {
            usage();
            return;
        }
        
        String op = arguments.get(0);
        if (!"dump".equals(op) && !"restore".equals(op)) {
            System.out.println("invalid operation: " + op);
            usage();
            return;
        }
        
        String dumpFile = arguments.get(1);
        
        CliOption c = options.get("c");
        CliOption cp = options.get("cp");
        CliOption t = options.get("t");
        CliOption tp = options.get("tp");
        
        String host = mrc.urlValue.getHost();
        int port = mrc.urlValue.getPort();
        String protocol = mrc.urlValue.getProtocol();
        
        RPCClient client = null;
        
        try {
            client = protocol.startsWith("https") ? new RPCClient(0, new SSLOptions(c.stringValue,
                cp.stringValue, t.stringValue, tp.stringValue)) : new RPCClient(0, null);
            client.setTimeout(0);
            
            if (op.equals("dump")) {
                RPCResponse r = null;
                try {
                    r = client.sendRPC(new InetSocketAddress(host, port), ".dumpdb", RPCClient
                            .generateList(dumpFile), authString, null);
                    r.waitForResponse();
                } finally {
                    if (r != null)
                        r.freeBuffers();
                }
            }

            else if (op.equals("restore")) {
                RPCResponse r = null;
                try {
                    r = client.sendRPC(new InetSocketAddress(host, port), ".restoredb", RPCClient
                            .generateList(dumpFile), authString, null);
                    r.waitForResponse();
                } finally {
                    if (r != null)
                        r.freeBuffers();
                }
            }

            else
                usage();
            
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            if (client != null)
                client.shutdown();
        }
    }
    
    private static void usage() {
        System.out
                .println("usage: xtfs_mrcdbtool -mrc <mrc_URL> [-c <creds_file>] [-cp <creds_passphrase>] [-t <trusted_CAs>] [-tp <trusted_passphrase>] dump|restore <dump_file>");
        System.exit(1);
    }
    
}
