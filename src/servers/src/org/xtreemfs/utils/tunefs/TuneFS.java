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

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.util.CLOption;
import org.xtreemfs.foundation.util.CLOptionParser;
import org.xtreemfs.foundation.util.InvalidUsageException;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.foundation.oncrpc.utils.XDRUtils;

/**
 *
 * @author bjko
 */
public class TuneFS {

    public static final String EXEC_NAME = "tunefs.xtreemfs";

    private CLOption.Switch optVerbose;
    private CLOption.Switch optHelp;
    private CLOption.StringValue optDebug;
    private CLOption.URLValue optDIR;
    private CLOption.StringValue optPKCS12file;
    private CLOption.StringValue optPKCS12passphrase;
    private CLOption.StringValue optSPolicy;
    private CLOption.IntegerValue optSWidth;
    private CLOption.IntegerValue optSSize;
    private CLOptionParser parser;

    private final Map<String,TuneFSCommand> commands;
    private final List<TuneFSCommand> commandList;

    public TuneFS(String[] args) {

        commands = new HashMap();
        commandList = new LinkedList();

        parser = new CLOptionParser("tunefs.xtreemfs");

        optHelp = new CLOption.Switch("h", "help", "print usage information");
        parser.addOption(optHelp);

        optVerbose = new CLOption.Switch("v", "verbose", "enbale verbose output");
        parser.addOption(optVerbose);

        optDebug = new CLOption.StringValue("d", "debug", "enable debugging with debug level ERROR,WARN,INFO or DEBUG");
        parser.addOption(optDebug);

        optDIR = new CLOption.URLValue(null, "dir", "specify a directory service to use",
                XDRUtils.ONCRPC_SCHEME, Integer.valueOf(DIRInterface.ONC_RPC_PORT_DEFAULT));
        parser.addOption(optDIR);

        optPKCS12file = new CLOption.StringValue(null, "pkcs12-file-path", "a PKCS#12 file with the credentials (required for oncrpcg/s)");
        parser.addOption(optPKCS12file);

        optPKCS12passphrase = new CLOption.StringValue(null, "pkcs12-passphrase", "a PKCS#12 file with the credentials (required for oncrpcg/s)");
        parser.addOption(optPKCS12passphrase);
        
        

        //install commands
        TuneFSCommand cmd = new StatCommand();
        cmd.createOptions(parser);
        cmd.addMapping(commands);
        commandList.add(cmd);

        cmd = new SPCommand();
        cmd.createOptions(parser);
        cmd.addMapping(commands);
        commandList.add(cmd);

        cmd = new ROCommand();
        cmd.createOptions(parser);
        cmd.addMapping(commands);
        commandList.add(cmd);

        cmd = new RECommand();
        cmd.createOptions(parser);
        cmd.addMapping(commands);
        commandList.add(cmd);

        cmd = new PolicyCommand();
        cmd.createOptions(parser);
        cmd.addMapping(commands);
        commandList.add(cmd);

        cmd = new RPCommand();
        cmd.createOptions(parser);
        cmd.addMapping(commands);
        commandList.add(cmd);
        
        cmd = new ACLCommand();
        cmd.createOptions(parser);
        cmd.addMapping(commands);
        commandList.add(cmd);
        

        try {
            parser.parse(args);
            if (parser.getArguments().size() < 2)
                throw new IllegalArgumentException("must specify a command and a pathname");
        } catch (IllegalArgumentException ex) {
            System.err.println("ERROR - invalid arguments: "+ex.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    public static String cleanupPath(String fileName) {
        fileName = fileName.trim();

        // remove leading and trailing quotes
        if (fileName.charAt(0) == '"' && fileName.charAt(fileName.length() - 1) == '"')
            fileName = fileName.substring(1, fileName.length() - 1);

        // replace all backslashes with spaces
        fileName = fileName.replaceAll("\\\\ ", " ");

        return fileName;
    }

    public void run() {
        final String command = parser.getArguments().get(0).toLowerCase();
        final List<String> cmdArgs = parser.getArguments().subList(1, parser.getArguments().size());

        try {

            if (command.startsWith("h")) {
                printHelp(cmdArgs);
            } else {
                TuneFSCommand cmd = commands.get(command);
                if (cmd == null) {
                    System.err.println("ERROR - unrecognized command '"+command+"'");
                    System.exit(1);
                }
                cmd.execute(cmdArgs);
            }

        } catch (FileNotFoundException ex) {
            System.err.println("ERROR - file or directory not found: "+ex.getMessage());
            System.exit(ErrNo.ENOENT);
        } catch (InvalidUsageException ex) {
            System.err.println("ERROR - "+ex.getMessage());
            System.exit(1);
        } catch (Throwable th) {
            th.printStackTrace();
            System.exit(1);
        }
    }

    public void printHelp(List<String> args) {
        if (args.size() != 1) {
            System.err.println("usage: "+EXEC_NAME+" help <command>");
            System.exit(1);
        }
        final String cmdName = args.get(0).toLowerCase();
        TuneFSCommand cmd = commands.get(cmdName);
        if (cmd == null) {
            System.err.println("ERROR - unrecognized command '"+cmdName+"'");
            System.exit(1);
        }
        cmd.printUsage(this.EXEC_NAME);
    }

    public void printUsage() {
        System.out.println("tunefs.xtreemfs [options] <command> [<subcommand>] <pathname>");
        System.out.println("pathname is the path to a file or directory on a mounted XtreemFS volume");
        System.out.println("command can be one of the following:");
        for (TuneFSCommand cmd : commandList) {
            System.out.println("\t"+cmd.getCommandHelp());
        }
        System.out.println("\thelp <command>: display detailed usage for <command>");
        System.out.println("");
        System.out.println("options accepted by all commands:");
        System.out.println("\t"+optVerbose.getName()+" print detailed messages");
        System.out.println("\t"+optDebug.getName()+"DEBUG|INFO|WARN print detailed debugging information");
        System.out.println("\t"+optDIR.getName()+"oncrpc[g|s]://hostname:port to ovveride the DIR provided by the MRC");
        System.out.println("\t"+optPKCS12file.getName()+" path to a PKCS#12 file with user credentials, required for oncrpcg/s");
        System.out.println("\t"+optPKCS12passphrase.getName()+" passphrase for the user credentials, required for oncrpcg/s");
    }

    public static void main(String args[]) {
        try {
            TuneFS t = new TuneFS(args);
            t.run();
            System.exit(0);
        } catch (Throwable th) {
            th.printStackTrace();
            System.exit(1);
        }
    }

}
