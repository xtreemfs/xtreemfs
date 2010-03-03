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
import org.xtreemfs.common.util.CLOption;
import org.xtreemfs.common.util.CLOption.IntegerValue;
import org.xtreemfs.common.util.CLOption.StringValue;
import org.xtreemfs.common.util.CLOptionParser;
import org.xtreemfs.common.util.InvalidUsageException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.utils.utils;

/**
 *
 * @author bjko
 */
public class SPCommand implements TuneFSCommand {

    public static final String SP_ATTR = "xtreemfs.default_sp";

    private CLOption.StringValue optPolicy;
    private CLOption.IntegerValue optWidth;
    private CLOption.IntegerValue optSize;

    @Override
    public void addMapping(Map<String, TuneFSCommand> map) {
        map.put("sp",this);
        map.put("striping",this);
    }

    @Override
    public String getCommandHelp() {
        return "striping (sp): shows/sets the default striping policy (volume,directory)";
    }

    @Override
    public void createOptions(CLOptionParser parser) {
        optPolicy = new CLOption.StringValue("p", "striping-policy", "=NONE|RAID0 striping policy");
        optPolicy = (StringValue) parser.addOption(optPolicy);

        optWidth = new CLOption.IntegerValue("w", "striping-policy-width", " number of OSDs to stripe over");
        optWidth = (IntegerValue) parser.addOption(optWidth);

        optSize = new CLOption.IntegerValue("s", "striping-policy-stripe-size", " size of each stripe (object) in kilo bytes");
        optSize = (IntegerValue) parser.addOption(optSize);
    }

    @Override
    public void printUsage(String executableName) {
        System.out.println("usage: " + executableName + " sp get|set <path>");
        System.out.println("options (required for set):");
        System.out.println("\t"+optPolicy.getHelp());
        System.out.println("\t"+optSize.getHelp());
        System.out.println("\t"+optWidth.getHelp());
    }

    @Override
    public void execute(List<String> arguments) throws Exception {
        if (arguments.size() != 2) {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " sp get|set <path to directory>");
        }

        final String subCmd = arguments.get(0);
        final String dirPath = TuneFS.cleanupPath(arguments.get(1));

        // fetch all XtreemFS-related extended attributes
        final Map<String, String> attrs = utils.getxattrs(dirPath);
        if (attrs == null) {
            throw new FileNotFoundException("directory '" + dirPath + "' does not exist");
        }

        

        if (subCmd.equalsIgnoreCase("get")) {

            final String defSP = attrs.get(SP_ATTR);
            if (defSP == null) {
                System.out.println("This object doesn't have a default striping policy.");
                return;
            }

            Map<String,Object> sp = null;
            try {
                sp = (Map<String,Object>)JSONParser.parseJSON(new JSONString(defSP));
            } catch (Exception ex) {
                throw new IOException("cannot read default striping policy due to system error: "+ex+"\ncontent: "+defSP);
            }

            System.out.println("directory:   "+dirPath);
            System.out.println("policy   :   "+sp.get("pattern"));
            System.out.println("size     :   "+sp.get("size")+" kB");
            System.out.println("width    :   "+sp.get("width"));
        } else if (subCmd.equalsIgnoreCase("set")) {

            if (!optPolicy.isSet())
                throw new InvalidUsageException("must set a striping policy name ("+optPolicy.getName()+")");
            if (!optPolicy.getValue().equals("RAID0"))
                throw new InvalidUsageException("striping policy name ("+optPolicy.getName()+") must be RAID0");
            if (!optSize.isSet())
                throw new InvalidUsageException("must set a stripe size ("+optSize.getName()+")");
            if (!optWidth.isSet())
                throw new InvalidUsageException("must set a striping width ("+optWidth.getName()+")");

            Map<String,Object> sp = new HashMap();
            sp.put("pattern","STRIPING_POLICY_RAID0");
            sp.put("size",Long.valueOf(optSize.getValue()));
            sp.put("width",Long.valueOf(optWidth.getValue()));

            String json = JSONParser.writeJSON(sp);//.replace("\"", "\\\"");

            utils.setxattr(dirPath, SP_ATTR, json);

        } else {
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME + " sp get|set <path to directory>");
        }
    }
}
