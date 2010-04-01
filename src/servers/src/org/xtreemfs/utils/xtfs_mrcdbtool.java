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

import org.xtreemfs.common.clients.Client;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.MRCInterface.MRCException;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.MRCInterface.errnoException;
import org.xtreemfs.foundation.oncrpc.utils.XDRUtils;
import org.xtreemfs.mrc.client.MRCClient;

public class xtfs_mrcdbtool {
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        Logging.start(Logging.LEVEL_ERROR);
        try {
            TimeSync.initialize(null, 60000, 50);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(3);
        CliOption oMrc = new CliOption(CliOption.OPTIONTYPE.URL);
        oMrc.urlDefaultPort = MRCInterface.ONC_RPC_PORT_DEFAULT;
        oMrc.urlDefaultProtocol = XDRUtils.ONCRPC_SCHEME;
        options.put("mrc", oMrc);
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
        
        CliOption mrc = options.get("mrc");
        if (mrc.urlValue == null) {
            System.out.println("missing MRC URL");
            usage();
            return;
        }
        
        if (arguments.size() < 2) {
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
        CliOption cp = options.get("cpass");
        CliOption t = options.get("t");
        CliOption tp = options.get("tpass");
        CliOption p = options.get("p");
        
        String host = mrc.urlValue.getHost();
        int port = mrc.urlValue.getPort();
        String protocol = mrc.urlValue.getProtocol();
        
        RPCNIOSocketClient rpcClient = null;
        
        try {
            
            SSLOptions sslOptions = protocol.startsWith("https") ? new SSLOptions(new FileInputStream(
                c.stringValue), cp.stringValue, new FileInputStream(t.stringValue), tp.stringValue) : null;
            rpcClient = new RPCNIOSocketClient(sslOptions, Integer.MAX_VALUE - 1000, Integer.MAX_VALUE, Client.getExceptionParsers());
            rpcClient.start();
            MRCClient client = new MRCClient(rpcClient, new InetSocketAddress(host, port));
            
            StringSet gids = new StringSet();
            gids.add("");
            UserCredentials creds = new UserCredentials("", gids, p == null ? "" : p.stringValue);
            
            if (op.equals("dump")) {
                RPCResponse<Object> r = null;
                try {
                    r = client.xtreemfs_dump_database(null, creds, dumpFile);
                    r.get();
                } finally {
                    if (r != null)
                        r.freeBuffers();
                }
            }

            else if (op.equals("restore")) {
                RPCResponse<Object> r = null;
                try {
                    r = client.xtreemfs_restore_database(null, creds, dumpFile);
                    r.get();
                } finally {
                    if (r != null)
                        r.freeBuffers();
                }
            }

            else
                usage();
            
        } catch (MRCException exc) {
            if (exc.getError_code() == ErrNo.EPERM)
                System.out.println("permission denied: admin password invalid or volumes exist already");
            else
                exc.printStackTrace();
        } catch (errnoException exc) {
            System.err.println(exc.getError_message() + ", errno=" + exc.getError_code());
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            if (rpcClient != null)
                rpcClient.shutdown();
        }
    }
    
    private static void usage() {
        System.out
                .println("usage: xtfs_mrcdbtool -mrc <mrc_URL> [-p <admin_passphrase>] [-c <creds_file>] [-cp <creds_passphrase>] [-t <trusted_CAs>] [-tp <trusted_passphrase>] dump|restore <dump_file>");
        System.exit(1);
    }
    
}
