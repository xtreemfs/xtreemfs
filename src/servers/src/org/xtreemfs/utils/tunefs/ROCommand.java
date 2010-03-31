/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
