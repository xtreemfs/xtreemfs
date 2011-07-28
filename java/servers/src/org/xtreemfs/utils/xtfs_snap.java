/*
 * Copyright (c) 2009-2011 by Jan Stender,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;

/**
 * A tool to manage your Replicas. File can be marked as read-only, replicas can
 * be added, ... <br>
 * 06.04.2009
 */
public class xtfs_snap {
    
    public final static String OPTION_HELP       = utils.OPTION_HELP;
    
    public final static String OPTION_HELP_LONG  = utils.OPTION_HELP_LONG;
    
    public final static String OPTION_RECURSIVE  = "r";
    
    public final static String OPTION_CREATE     = "c";
    
    public final static String OPTION_DELETE     = "x";
    
    public final static String OPTION_LIST       = "l";
    
    public final static String OPTION_PATH       = "d";
    
    public final static String OPTION_ENABLE     = "-enable";
    
    public final static String OPTION_DISABLE    = "-disable";
    
    public final static String OPTION_IS_ENABLED = "-is_enabled";
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(4);
        options.put(OPTION_HELP, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_HELP_LONG, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_RECURSIVE, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_CREATE, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_DELETE, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_LIST, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_PATH, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_ENABLE, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_DISABLE, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_IS_ENABLED, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        
        try {
            CLIParser.parseCLI(args, options, arguments);
        } catch (Exception exc) {
            System.out.println(exc);
            usage();
            return;
        }
        
        CliOption h = options.get(OPTION_HELP);
        if (h.switchValue) {
            usage();
            return;
        }
        
        h = options.get(OPTION_HELP_LONG);
        if (h.switchValue) {
            usage();
            return;
        }
        
        if (arguments.size() > 2) {
            usage();
            return;
        }
        
        CliOption r = options.get(OPTION_RECURSIVE);
        
        CliOption c = options.get(OPTION_CREATE);
        CliOption x = options.get(OPTION_DELETE);
        CliOption l = options.get(OPTION_LIST);
        CliOption enable = options.get(OPTION_ENABLE);
        CliOption disable = options.get(OPTION_DISABLE);
        CliOption isEnabled = options.get(OPTION_IS_ENABLED);
        
        if (enable.switchValue && disable.switchValue) {
            usage();
            return;
        }
        
        CliOption p = options.get(OPTION_PATH);
        final String path = p.stringValue == null ? new File("").getAbsolutePath() : p.stringValue;
        if (!utils.isXtreemFSDir(path)) {
            System.err.println("'" + path + "' is not an XtreemFS directory.");
            return;
        }
        
        if (!c.switchValue && !x.switchValue && !l.switchValue && !enable.switchValue && !disable.switchValue
            && !isEnabled.switchValue) {
            usage();
            return;
        }
        
        try {
            if (c.switchValue) {
                
                final String snapName = arguments.size() == 0 ? "" : arguments.get(0);
                utils.setxattr(path, "xtreemfs.snapshots", "c" + (r.switchValue ? "r" : "") + " " + snapName);
                
            } else if (x.switchValue) {
                
                if (arguments.size() == 0) {
                    System.err.println("Please specify a snapshot name to delete!\n");
                    usage();
                }
                
                utils.setxattr(path, "xtreemfs.snapshots", "d " + arguments.get(0));
            }
            
            if (l.switchValue) {
                String snaps = utils.getxattr(utils.findXtreemFSRootDir(path), "xtreemfs.snapshots");
                if (snaps != null)
                    System.out.println(snaps);
            }
            
            if (enable.switchValue) {
                utils.setxattr(path, "xtreemfs.snapshots_enabled", "true");
            } else if (disable.switchValue) {
                utils.setxattr(path, "xtreemfs.snapshots_enabled", "false");
            }
            
            if (isEnabled.switchValue) {
                boolean snapsEnabled = "true".equals(utils.getxattr(path, "xtreemfs.snapshots_enabled"));
                System.out.println(snapsEnabled);
            }
            
        } catch (IOException exc) {
            System.out.println("an error occurred: " + exc.getMessage());
            System.exit(1);
        }
        
    }
    
    public static void usage() {
        
        String cmd = xtfs_snap.class.getSimpleName();
        
        StringBuffer out = new StringBuffer();
        out.append("Usage:\n");
        out.append("\t" + cmd + " -" + OPTION_CREATE + " [-" + OPTION_RECURSIVE + "] [-" + OPTION_PATH
            + " <path>] [<name>]\n");
        out.append("\t" + cmd + " -" + OPTION_DELETE + " [-" + OPTION_PATH + " <path>] <name>\n");
        out.append("\t" + cmd + " -" + OPTION_LIST + " [-" + OPTION_PATH + " <path>]\n");
        out.append("\t" + cmd + " -" + OPTION_ENABLE + " [-" + OPTION_PATH + " <root-dir>]\n");
        out.append("\t" + cmd + " -" + OPTION_DISABLE + " [-" + OPTION_PATH + " <root-dir>]\n");
        out.append("\t" + cmd + " -" + OPTION_IS_ENABLED + " [-" + OPTION_PATH + " <root-dir>]\n");
        out.append("\t" + cmd + " -" + OPTION_HELP + "|-" + OPTION_HELP_LONG + "\n");
        out.append("\n\t<name> a name for the snapshot. If no name is provided, the current server time will be used as name.\n");
        out.append("\noptions:\n");
        out.append("\t-" + OPTION_CREATE + ": create a snapshot\n");
        out
                .append("\t-"
                    + OPTION_PATH
                    + ": path to the mounted XtreemFS directory; if not specified, current working directory will be used\n");
        out.append("\t-" + OPTION_HELP + "/-" + OPTION_HELP_LONG + ": show usage info\n");
        out.append("\t-" + OPTION_LIST + ": list all snapshots in the system\n");
        out.append("\t-" + OPTION_RECURSIVE + ": include subdirectories into snapshot\n");
        out.append("\t-" + OPTION_DELETE + ": delete a snapshot\n");
        out.append("\t-" + OPTION_ENABLE + ": enable snapshots on the volume\n");
        out.append("\t-" + OPTION_DISABLE + ": disable snapshots on the volume\n");
        out.append("\t-" + OPTION_IS_ENABLED + ": check if snapshots are enabled\n");
        
        System.out.println(out.toString());
    }
}