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
import org.xtreemfs.utils.xtfs_repl;

/**
 *
 * @author bjko
 */
public class PolicyCommand implements TuneFSCommand {

    private CLOption.StringValue optPKCS12file;

    private CLOption.StringValue optPKCS12passphrase;




    @Override
    public void addMapping(Map<String, TuneFSCommand> map) {
        map.put("policy",this);
        map.put("po",this);
    }

    @Override
    public String getCommandHelp() {
        return "policy (po): show/set a policy (replica and OSD selection) or policy attributes";
    }

    @Override
    public void createOptions(CLOptionParser parser) {
        optPKCS12file = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "pkcs12-file-path", ""));
        optPKCS12passphrase = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "pkcs12-passphrase", ""));
    }

    @Override
    public void printUsage(String executableName) {
        System.out.println("po rsel get <volumepath>: displays the replica selection policy for a volume");
        System.out.println("po rsel set <volumepath> <policy>: changes the replica selection policy for a volume");
        System.out.println("po osel get <volumepath>: displays the OSD selection policy for a volume");
        System.out.println("po osel set <volumepath> <policy>: changes the OSD selection policy for a volume");
        System.out.println("po attr get <volumepath>: displays the value of the policy attribute for a volume");
        System.out.println("po attr set <volumepath> <attrname>=<value>: sets the value of the policy attribute for a volume");
        System.out.println("list of policies (OSD and replicas):");
        System.out.println("\tRANDOM - random OSD/replica selection");
        System.out.println("\tFQDN   - FQDN (hostname) based OSD/replica selection");
        System.out.println("\tDCMAP  - datacenter map based OSD/replica selection");
        System.out.println("\tVIVALDI- vivaldi network-coordinate based OSD/replica selection");
        System.out.println("list of policies (OSD selection only):");
        System.out.println("\tUUID   - fixed OSD selection based on a list of UUIDs");
        System.out.println("list of attributes (see user guide for a complete list):");
        System.out.println("\t"+"uuids <comma separated list of UUIDs> (UUID)");
    }

    @Override
    public void execute(List<String> arguments) throws Exception {
        if (arguments.size() < 3)
            throw new InvalidUsageException("usage: "+TuneFS.EXEC_NAME+" po <subcommand> get|set <volumepath> ...");

        final String subCmd = arguments.get(0).toLowerCase();
        final String getSet = arguments.get(1).toLowerCase();
        final String filename = arguments.get(2);
        final List<String> subArgs = arguments.subList(3, arguments.size());


        xtfs_repl r = ReplUtil.initRepl(filename, optPKCS12file, optPKCS12passphrase);
        r.initialize(true);

        if (subCmd.equals("osel")) {
            processOSEL(r, getSet, subArgs);
        } else if (subCmd.equals("rsel")) {
            processRSEL(r, getSet, subArgs);
        } else if (subCmd.equals("attr")) {
            processAttr(r, getSet, subArgs);
        } else {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " po <subcommand> <path to directory>");
        }
    }

    private void processOSEL(xtfs_repl r, String getSet, List<String> subArgs) throws Exception {
        
        if (getSet.equals("get")) {
            r.getOSDSelectionPolicy();
        } else if (getSet.equals("set")) {
            if (subArgs.size() != 1)
                throw new InvalidUsageException("osel set expects a policy name");
            r.setOSDSelectionPolicy(subArgs.get(0));
        } else {
            throw new InvalidUsageException("unknown subcommand to osel: "+getSet);
        }
    }

    private void processRSEL(xtfs_repl r, String getSet, List<String> subArgs) throws Exception {

        if (getSet.equals("get")) {
            r.getReplicaSelectionPolicy();
        } else if (getSet.equals("set")) {
            if (subArgs.size() != 1)
                throw new InvalidUsageException("rsel set expects a policy name");
            r.setReplicaSelectionPolicy(subArgs.get(0));
        } else {
            throw new InvalidUsageException("unknown subcommand to rsel: "+getSet);
        }
    }

    private void processAttr(xtfs_repl r, String getSet, List<String> subArgs) throws Exception {

        final String attrName = subArgs.get(0);

        if (getSet.equals("get")) {
            r.getPolicyAttrs();
        } else if (getSet.equals("set")) {
            if (subArgs.size() < 1)
                throw new InvalidUsageException("policy set expects <attrname>=<value>");
            r.setPolicyAttr(subArgs.get(1));
        } else {
            throw new InvalidUsageException("unknown subcommand to attr: "+getSet);
        }
    }


    
}
