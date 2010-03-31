/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils.tunefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.util.CLOption;
import org.xtreemfs.foundation.util.CLOptionParser;
import org.xtreemfs.foundation.util.InvalidUsageException;
import org.xtreemfs.foundation.util.CLOption.IntegerValue;
import org.xtreemfs.foundation.util.CLOption.Switch;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.utils.xtfs_repl;

/**
 *
 * @author bjko
 */
public class RECommand implements TuneFSCommand {

    private CLOption.StringValue optPKCS12file;

    private CLOption.StringValue optPKCS12passphrase;

    private CLOption.StringValue optOSDList;

    private CLOption.Switch      optFull;

    private CLOption.Switch      optPartial;

    private CLOption.IntegerValue optWidth;


    @Override
    public void addMapping(Map<String, TuneFSCommand> map) {
        map.put("re",this);
        map.put("replica",this);
    }

    @Override
    public String getCommandHelp() {
        return "replica (re): list/add/remove replicas for a file (file)";
    }

    @Override
    public void createOptions(CLOptionParser parser) {
        optPKCS12file = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "pkcs12-file-path", ""));
        optPKCS12passphrase = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "pkcs12-passphrase", ""));
        optFull = (Switch) parser.addOption(new CLOption.Switch(null, "full", "flag to create a full replica (read-only replicas only)"));
        optPartial = (Switch) parser.addOption(new CLOption.Switch(null, "partial", "flag to create a partial replica (read-only replicas only)"));
        optOSDList = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "osds", "a comma-separated list with one or more OSD UUIDs"));
        optWidth = (IntegerValue) parser.addOption(new CLOption.IntegerValue("w", "striping-policy-width", " number of OSDs to stripe over"));
    }

    @Override
    public void printUsage(String executableName) {
        System.out.println("re get <filename>: displays the replica list for a file");
        System.out.println("re add <filename>: adds a new replica");
        System.out.println("re rem <filename>: removes a replica");
        System.out.println("re policy <filename> <policy>: sets the replica update policy for a file");
        System.out.println("options for add:");
        System.out.println("\t"+optOSDList.getHelp());
        System.out.println("\t"+optFull.getHelp());
        System.out.println("\t"+optPartial.getHelp());
        System.out.println("\t"+optWidth.getHelp());
        System.out.println("options for rem:");
        System.out.println("\t"+optOSDList.getHelp());
        System.out.println("replica update policies:");
        System.out.println("\tnone - no replication");
        System.out.println("\tWaR1 - write all, read one (primary)");
        System.out.println("\tWaRa - write all, read all (allows stale reads)");
    }

    @Override
    public void execute(List<String> arguments) throws Exception {
        if (arguments.size() != 2) {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " re get|add|rem <filepath>");
        }

        final String subCmd = arguments.get(0).toLowerCase();
        final String filename = TuneFS.cleanupPath(arguments.get(1));

        xtfs_repl r = ReplUtil.initRepl(filename, optPKCS12file, optPKCS12passphrase);
        r.initialize(true);

        if (subCmd.equals("rem")) {
            remReplica(r);
        } else if (subCmd.equals("add")) {
            addReplica(r);
        } else if (subCmd.equals("get")) {
            r.listReplicas();
        } else if (subCmd.equals("policy")) {
            if (arguments.size() != 3)
                throw new InvalidUsageException("re policy requires a filename and a policy name");

            String policy = arguments.get(2);
            if (policy.equalsIgnoreCase("none")) {
                policy = "";
            } else if (policy.equalsIgnoreCase(Constants.REPL_UPDATE_PC_WARA)) {
                policy = Constants.REPL_UPDATE_PC_WARA;
            } else if (policy.equalsIgnoreCase(Constants.REPL_UPDATE_PC_WARONE)) {
                policy = Constants.REPL_UPDATE_PC_WARONE;
            } else {
                throw new InvalidUsageException("unknown replica update policy: "+policy);
            }

            r.setReplicaUpdatePolicy(arguments.get(2));
        } else {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " repl <subcommand> <path to directory>");
        }
    }


    private void addReplica(xtfs_repl r) throws Exception {

        boolean isFull = optFull.getValue();
        boolean isPartial = optPartial.getValue();
        if (isFull == isPartial) {
            throw new InvalidUsageException(("must specify either "+optFull.getName()+" or "+optPartial.getName()));
        }

        int replicationFlags = 0;
        if (isFull) {
            replicationFlags = ReplicationFlags.setFullReplica(replicationFlags);
        } else {
            replicationFlags = ReplicationFlags.setPartialReplica(replicationFlags);
        }

        int rWidth = 1;
        if (optWidth.isSet())
            rWidth = optWidth.getValue();

        if (optOSDList.isSet()) {
            String[] osds = optOSDList.getValue().trim().split(",");
            if (osds.length != rWidth) {
                throw new InvalidUsageException("osd list must contain "+rWidth+" (stripe width) replicas");
            }
            List<ServiceUUID> uuids = new ArrayList(osds.length);
            for (String osd : osds) {
                ServiceUUID uuid = new ServiceUUID(osd,r.getResolver());
                uuids.add(uuid);
            }
            r.addReplica(uuids, replicationFlags, rWidth);
        } else {
            r.addReplicaAutomatically(replicationFlags, rWidth);
        }
    }

    private void remReplica(xtfs_repl r) throws Exception {
        if (optOSDList.isSet()) {

            ServiceUUID osd = new ServiceUUID(optOSDList.getValue(), r.getResolver());
            r.removeReplica(osd);

        } else {
            throw new InvalidUsageException("must specify first OSD UUID of replica to be deleted with "+optOSDList.getName());
        }
    }
}
