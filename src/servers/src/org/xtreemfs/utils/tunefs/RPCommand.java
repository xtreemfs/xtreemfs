/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.utils.tunefs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.util.CLOption;
import org.xtreemfs.foundation.util.CLOptionParser;
import org.xtreemfs.foundation.util.InvalidUsageException;
import org.xtreemfs.foundation.util.CLOption.IntegerValue;
import org.xtreemfs.foundation.util.CLOption.StringValue;
import org.xtreemfs.utils.utils;

/**
 *
 * @author bjko
 */
public class RPCommand implements TuneFSCommand {

    public static final String RP_ATTR = "xtreemfs.default_rp";

    private CLOption.StringValue optPolicy;
    private CLOption.IntegerValue optRFactor;

    @Override
    public void addMapping(Map<String, TuneFSCommand> map) {
        map.put("rp",this);
        map.put("replication",this);
    }

    @Override
    public String getCommandHelp() {
        return "striping (sp): shows/sets the default replication policy and factor (volume,directory)";
    }

    @Override
    public void createOptions(CLOptionParser parser) {
        optPolicy = new CLOption.StringValue(null, "replication-policy", "=NONE|WaR1|WaRa replication policy");
        optPolicy = (StringValue) parser.addOption(optPolicy);

        optRFactor = new CLOption.IntegerValue(null, "replication-factor", " number of replicas to create");
        optRFactor = (IntegerValue) parser.addOption(optRFactor);
    }

    @Override
    public void printUsage(String executableName) {
        System.out.println("usage: " + executableName + " rp get|set <path>");
        System.out.println("options (required for set):");
        System.out.println("\t"+optPolicy.getHelp());
        System.out.println("\t"+optRFactor.getHelp());
    }

    @Override
    public void execute(List<String> arguments) throws Exception {
        if (arguments.size() != 2) {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " rp get|set <path to directory>");
        }

        final String subCmd = arguments.get(0);
        final String dirPath = TuneFS.cleanupPath(arguments.get(1));

        // fetch all XtreemFS-related extended attributes
        final Map<String, String> attrs = utils.getxattrs(dirPath);
        if (attrs == null) {
            throw new FileNotFoundException("directory '" + dirPath + "' does not exist");
        }

        

        if (subCmd.equalsIgnoreCase("get")) {

            final String defSP = attrs.get(RP_ATTR);
            if (defSP == null) {
                System.out.println("This object doesn't have a default replication policy/factor.");
                return;
            }

            Map<String,Object> sp = null;
            try {
                sp = (Map<String,Object>)JSONParser.parseJSON(new JSONString(defSP));
            } catch (Exception ex) {
                throw new IOException("cannot read default striping policy due to system error: "+ex+"\ncontent: "+defSP);
            }

            System.out.println("directory:   "+dirPath);
            System.out.println("policy   :   "+sp.get("name"));
            System.out.println("factor   :   "+sp.get("numReplicas"));
        } else if (subCmd.equalsIgnoreCase("set")) {

            if (!optPolicy.isSet())
                throw new InvalidUsageException("must set a replication policy name ("+optPolicy.getName()+")");
            if (!optRFactor.isSet())
                throw new InvalidUsageException("must set a replication factor ("+optRFactor.getName()+")");
            if (optRFactor.getValue() < 1)
                throw new InvalidUsageException("must set a replication factor >= 1 (1 = no replication) ("+optRFactor.getName()+")");

            Map<String,Object> sp = new HashMap();
            sp.put("name",optPolicy.getValue());
            sp.put("numReplicas",Long.valueOf(optRFactor.getValue()));

            String json = JSONParser.writeJSON(sp);//.replace("\"", "\\\"");

            utils.setxattr(dirPath, RP_ATTR, json);

        } else {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " rp get|set <path to directory>");
        }
    }
}
