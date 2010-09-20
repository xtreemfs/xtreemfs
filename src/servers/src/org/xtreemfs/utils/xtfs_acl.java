/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.utils.CLIParser.CliOption;

/**
 * A tool to manage your Replicas. File can be marked as read-only, replicas can
 * be added, ... <br>
 * 06.04.2009
 */
public class xtfs_acl {
    
    public final static String OPTION_HELP        = "h";
    
    public final static String OPTION_HELP_LONG   = "-help";
    
    public final static String OPTION_MODIFY      = "m";
    
    public final static String OPTION_MODIFY_LONG = "-modify";
    
    public final static String OPTION_REMOVE      = "x";
    
    public final static String OPTION_REMOVE_LONG = "-remove";
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(1);
        options.put(OPTION_HELP, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_HELP_LONG, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_MODIFY, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_MODIFY_LONG, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_REMOVE, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_REMOVE_LONG, new CliOption(CliOption.OPTIONTYPE.STRING));
        
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
        
        if (arguments.size() != 1) {
            usage();
            return;
        }
        
        String path = arguments.get(0);
        if (!utils.isXtreemFSDir(path)) {
            System.err.println("'" + path + "' is not an XtreemFS directory.");
            return;
        }
        
        CliOption m = options.get(OPTION_MODIFY);
        CliOption mLong = options.get(OPTION_MODIFY_LONG);
        CliOption x = options.get(OPTION_REMOVE);
        CliOption xLong = options.get(OPTION_REMOVE_LONG);
        
        String modArgs = m.stringValue != null ? m.stringValue
            : mLong.stringValue != null ? mLong.stringValue : null;
        String delArgs = x.stringValue != null ? x.stringValue
            : xLong.stringValue != null ? xLong.stringValue : null;
        
        if (modArgs != null)
            executeModify(modArgs, path);
        else if (delArgs != null)
            executeDelete(delArgs, path);
        else
            executeList(path);
        
    }
    
    public static void executeModify(String args, String path) throws Exception {
        utils.setxattr(path, "xtreemfs.acl", "m " + args);
    }
    
    public static void executeDelete(String args, String path) throws Exception {
        utils.setxattr(path, "xtreemfs.acl", "x " + args);
    }
    
    public static void executeList(String path) throws Exception {
        System.out.println(utils.getxattr(path, "xtreemfs.acl"));
    }
    
    public static void usage() {
        StringBuffer out = new StringBuffer();
        out.append("xtfs_acl <path>: lists the ACL of a file\n");
        out.append("xtfs_acl -m|--modify u|g|m|o:[<name>]:[<rwx>|<octal>] <path>: updates an ACL entry\n");
        out.append("xtfs_acl -x|--remove u|g|m|o:<name> <path>: removes an ACL entry\n");
        
        System.out.println(out.toString());
    }
    
}