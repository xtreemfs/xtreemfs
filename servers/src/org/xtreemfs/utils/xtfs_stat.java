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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

/**
 *
 * @author bjko
 */
public class xtfs_stat {

    private static final int FILE    = 1;

    private static final int DIR     = 2;

    private static final int SYMLINK = 3;

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        try {

            if (args.length < 1) {
                System.out.println("usage: " + xtfs_stat.class.getSimpleName() + " <file>\n");
                System.exit(1);
            }

            String fileName = "";
            for (String arg : args)
                fileName += arg + " ";

            // remove leading and trailing spaces
            fileName = fileName.trim();

            // remove leading and trailing quotes
            if (fileName.charAt(0) == '"' && fileName.charAt(fileName.length() - 1) == '"')
                fileName = fileName.substring(1, fileName.length() - 1);

            // replace all backslashes with spaces
            fileName = fileName.replaceAll("\\\\ ", " ");

            final String format = "%-25s  %s\n";

            // fetch all XtreemFS-related extended attributes
            Map<String, String> attrs = utils.getxattrs(fileName);
            if (attrs == null) {
                System.err.println("file not found: " + fileName);
                return;
            }

            String url = attrs.get("xtreemfs.url");
            if (url == null) {
                System.out.println("'" + fileName
                    + "' is probably not part of an XtreemFS volume (no MRC URL found).");
                return;
            }

            // first, render all general XtreemFS attributes
            System.out.format(format, "filename", new File(fileName).getName());
            System.out.format(format, "XtreemFS URI", url);
            System.out.format(format, "XtreemFS fileID", attrs.get("xtreemfs.file_id"));

            int ftype = Integer.parseInt(attrs.get("xtreemfs.object_type"));
            switch (ftype) {
            case FILE:
                System.out.format(format, "object type", "regular file");
                break;
            case DIR:
                System.out.format(format, "object type", "directory"
                    + (attrs.get("xtreemfs.file_id").endsWith(":1") ? " (volume root)" : ""));
                break;
            case SYMLINK:
                System.out.format(format, "object type", "symlink");
                break;
            }
            System.out.format(format, "owner", attrs.get("xtreemfs.owner"));
            System.out.format(format, "group", attrs.get("xtreemfs.group"));

            if (ftype == FILE) {
                String readOnly = attrs.containsKey("xtreemfs.read_only") ? attrs
                        .get("xtreemfs.read_only") : String.valueOf(false);
                System.out.format(format, "read-only", readOnly);
            }

            // if the file refers to a directory, render directory attributes
            if (ftype == DIR) {
                String defSP = attrs.get("xtreemfs.default_sp");
                if (defSP == null)
                    defSP = "none";
                System.out.format(format, "default striping policy", defSP);
            }

            // render other known XtreemFS attributes
            if (attrs.containsKey("xtreemfs.ac_policy_id"))
                System.out.format(format, "access control policy ID", attrs
                        .get("xtreemfs.ac_policy_id"));

            if (attrs.containsKey("xtreemfs.osdsel_policy_id"))
                System.out.format(format, "OSD selection policy ID", attrs
                        .get("xtreemfs.osdsel_policy_id"));

            if (attrs.containsKey("xtreemfs.free_space"))
                System.out.format(format, "free usable disk space", OutputUtils.formatBytes(Long
                        .valueOf(attrs.get("xtreemfs.free_space"))));

            // if the file does not refer to a directory, render the
            // X-Locations list
            if (ftype != DIR) {

                // because of escape characters, the X-Locations list needs to
                // be parsed in two steps: first, parse the string,
                // then, parse a list from the parsed string
                String s = (String) JSONParser.parseJSON(new JSONString("\""
                    + attrs.get("xtreemfs.locations") + "\""));
                List<Object> l = (List) JSONParser.parseJSON(new JSONString(s));

                System.out.println("\nXtreemFS replica list");
                if (l == null) {
                    System.out.println("   This file does not have any replicas yet.");
                } else {
                    System.out.format(format, "   list version ", l.get(1));
                    List<List> replicas = (List<List>) l.get(0);
                    for (int i = 0; i < replicas.size(); i++) {
                        final Map<String, Object> policy = (Map) replicas.get(i).get(0);
                        final String pStr = policy.get("policy") + "," + policy.get("stripe-size")
                            + "kb," + policy.get("width");
                        System.out.format(format, "   replica " + (i + 1) + " policy", pStr);
                        System.out.format(format, "   replica " + (i + 1) + " OSDs", replicas
                                .get(i).get(1));
                    }
                }

                System.out.println();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String translatePermissions(Object perms) {
        int perm = ((Long) perms).intValue();

        String pStr = "";

        for (int i = 0; i < 3; i++) {
            if ((perm & 1) != 0) {
                pStr = "x" + pStr;
            } else {
                pStr = "-" + pStr;
            }
            if ((perm & 2) != 0) {
                pStr = "w" + pStr;
            } else {
                pStr = "-" + pStr;
            }
            if ((perm & 4) != 0) {
                pStr = "r" + pStr;
            } else {
                pStr = "-" + pStr;
            }
            perm = perm >> 3;
        }
        return pStr;
    }

}
