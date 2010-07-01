/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.utils.tunefs;

import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.util.CLOption;
import org.xtreemfs.foundation.util.CLOptionParser;
import org.xtreemfs.foundation.util.InvalidUsageException;
import org.xtreemfs.foundation.util.CLOption.Switch;
import org.xtreemfs.utils.xtfs_repl;

/**
 *
 * @author bjko
 */
public class ROCommand implements TuneFSCommand {

    private CLOption.StringValue optPKCS12file;

    private CLOption.StringValue optPKCS12passphrase;

    private CLOption.Switch optFull;

    @Override
    public void addMapping(Map<String, TuneFSCommand> map) {
        map.put("ro",this);
        map.put("readonly",this);
    }

    @Override
    public String getCommandHelp() {
        return "readonly (ro): show/set the read-only replication flag for a file (file)";
    }

    @Override
    public void createOptions(CLOptionParser parser) {
        optPKCS12file = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "pkcs12-file-path", ""));
        optPKCS12passphrase = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "pkcs12-passphrase", ""));
        optFull = (Switch) parser.addOption(new CLOption.Switch(null, "full", "flag to create a full replica (read-only replicas only)"));
    }

    @Override
    public void printUsage(String executableName) {
        System.out.println("ro get <filename>: displays the value of the read-only flag of a file (file)");
        System.out.println("ro set <filename> on|off: enables/disables the file readonly flag (file)");
        System.out.println("ro onclose <filename>: display on-close replication config (volume)");
        System.out.println("ro onclose <filename> <factor>: enables on-close replication (volume)");
        System.out.println("ro onclose <filename> 0: disables on-close replication (volume)");
        System.out.println("options (for ro onclose):");
        System.out.println("\t"+optFull);
    }

    @Override
    public void execute(List<String> arguments) throws Exception {
        if (arguments.size() < 2) {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " ro <subcommand> <path to directory>");
        }

        final String subCmd = arguments.get(0).toLowerCase();
        final String filename = TuneFS.cleanupPath(arguments.get(1));

        xtfs_repl r = ReplUtil.initRepl(filename, optPKCS12file, optPKCS12passphrase);
        r.initialize(true);

        if (subCmd.equals("set")) {
            if (arguments.size() < 3)
                throw new InvalidUsageException("ro set explects on|off as argument");
            r.setReadOnly(arguments.get(2).toLowerCase().equals("on"));
        } else if (subCmd.equals("get")) {
            r.isReadOnly();
        } else if (subCmd.equals("onclose")) {
            if (arguments.size() < 3) {
                r.getOnCloseReplFactor();
                r.getOnCloseFull();
            } else {
                r.setOnCloseReplFactor(arguments.get(3));
                r.setOnCloseFull(optFull.getValue());
                
            }
        } else {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " repl <subcommand> <path to directory>");
        }
    }

}
