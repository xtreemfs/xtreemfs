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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;

/**
 *
 * @author bjko
 */
public class replicas {

    /** Creates a new instance of replicas */
    public replicas() {
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        Logging.start(Logging.LEVEL_ERROR);
        // TODO code application logic here
        if (args.length < 2) {
            System.out
                    .println("Usage: replicas http://hostname:port/path command");
            System.exit(1);
        }

        String url = args[0];

        Pattern schema = Pattern
                .compile("http://([a-zA-Z_0-9\\-\\.]+)(?:\\:(\\d+))??/(.+)");
        Matcher m = schema.matcher(url.trim());
        InetSocketAddress mrcAddr = null;
        String path = "";
        if (m.matches()) {
            mrcAddr = new InetSocketAddress(m.group(1),
                (m.group(2) == null) ? 32636 : Integer.parseInt(m.group(2)));
            path = m.group(3);
        } else {
            System.out.println(url + " is not a valid URL");
            System.exit(1);
        }

        String cmd = args[1];

        try {

            MRCClient client = new MRCClient();

            Map<String, Object> data = client.stat(mrcAddr, path, true, false,
                false, NullAuthProvider.createAuthString("bla", "bla"));

            if (!data.containsKey("replicas")) {
                System.out.println("requested object does not exist!");
                System.exit(1);
            }

            String fileId = (String) data.get("fileId");

            if (cmd.equalsIgnoreCase("list")) {
                List<Object> l = (List) data.get("replicas");
                System.out.println("current replica list for " + fileId
                    + ", version " + l.get(1) + ":");
                List<List> replicas = (List<List>) l.get(0);
                for (int i = 0; i < replicas.size(); i++) {
                    System.out.println("\t" + (i + 1) + ": "
                        + replicas.get(i).get(0) + " on "
                        + replicas.get(i).get(1));
                }
            } else if (cmd.equalsIgnoreCase("add")) {
                if (args.length < 6) {
                    System.out
                            .println("add <policy> <width> <stripe-size> <OSD1URL> ... <OSDnURL>");
                    System.out
                            .println("policy can be RAID0, width is the number of OSDs to use for striping, stripe size is the object size in KB");
                    System.out.println("");
                    System.exit(1);
                }
                String sPolicy = args[2].toUpperCase();
                if (!sPolicy.equals("RAID0")) {
                    System.out.println("unknown striping policy " + sPolicy);
                    System.exit(1);
                }

                int sWidth = Integer.parseInt(args[3]);

                int sSize = Integer.parseInt(args[4]);

                if (args.length - 5 != sWidth) {
                    System.out.println("you must specify exactly width ("
                        + sWidth + ") OSDs!");
                    System.exit(1);
                }
                Map<String, Object> policy = new TreeMap();
                policy.put("policy", sPolicy);
                policy.put("width", sWidth);
                policy.put("stripe-size", sSize);

                List<String> osds = new ArrayList(20);
                for (int i = 5; i < args.length; i++) {
                    osds.add(args[i]);
                }
                client.addReplica(mrcAddr, fileId, policy, osds,
                    NullAuthProvider.createAuthString("bla", "bla"));
                System.out.println("replica added");
            } else if (cmd.equalsIgnoreCase("rem")) {
                if (args.length < 3) {
                    System.out.println("remove <replica no>");
                    System.exit(1);
                }
                List<Object> l = (List) ((List) data.get("replicas")).get(0);
                List<Object> repl = (List) l.get(Integer.parseInt(args[2]) - 1);
                Map<String, Object> sp = (Map<String, Object>) repl.get(0);
                List<String> locs = (List<String>) repl.get(1);

                client.removeReplica(mrcAddr, fileId, sp, locs,
                    NullAuthProvider.createAuthString("bla", "bla"));
                System.out.println("replica removed");
            }

            client.shutdown();

        } catch (Exception ex) {
            System.out.println("cannot complete operation: " + ex);
            ex.printStackTrace();
            System.exit(1);
        }

    }

}
