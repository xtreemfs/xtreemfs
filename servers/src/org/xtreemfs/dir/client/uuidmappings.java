/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.dir.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.utils.CLIParser;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 *
 * @author bjko
 */
public class uuidmappings {

    public static void main(String[] args) {
        try {
            Logging.start(Logging.LEVEL_ERROR);
            TimeSync.initialize(null, 60000, 50, "");

            Map<String,CLIParser.CliOption> options = new HashMap();
            options.put("g",new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("s",new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("uuid",new CliOption(CliOption.OPTIONTYPE.STRING));
            options.put("host",new CliOption(CliOption.OPTIONTYPE.STRING));
            options.put("port",new CliOption(CliOption.OPTIONTYPE.NUMBER));

            options.put("mapping", new CliOption(CliOption.OPTIONTYPE.STRING));

            CLIParser.parseCLI(args, options, new ArrayList(1));

            final String hostname = options.get("host").stringValue;
            final Long   port = options.get("port").numValue;

            if ((hostname == null) || (port == null)) {
                System.out.println("must specify DIR service with -host <hostname> and -port");
                System.exit(1);
            }

            final RPCNIOSocketClient rpcClient = new RPCNIOSocketClient(null, 10000, 60000);
            rpcClient.start();
            rpcClient.waitForStartup();

            final DIRClient client = new DIRClient(rpcClient, new InetSocketAddress(hostname,port.intValue()));
            if ( options.get("g").switchValue &&
                    (options.get("uuid").stringValue != null)) {
                RPCResponse<AddressMappingSet> response = client.address_mappings_get(null, options.get("uuid").stringValue);
                AddressMappingSet ams = response.get();
                printAddressMappings(ams);
            }
            if ( options.get("s").switchValue &&
                    (options.get("uuid").stringValue != null)) {

                Pattern p = Pattern.compile("(.*)\\s*\\-\\>\\s*(\\w+)://(\\S+)\\:(\\d+)");
                Matcher m = p.matcher(options.get("mapping").stringValue);
                if (m.matches()) {

                    RPCResponse<AddressMappingSet> response = client.address_mappings_get(null, options.get("uuid").stringValue);
                    AddressMappingSet ams = response.get();
                    long version = 0;
                    if (ams.size() > 0) {
                        version = ams.get(0).getVersion();
                    }

                    AddressMapping am = new AddressMapping(options.get("uuid").stringValue, version,
                            m.group(2),m.group(3),Integer.valueOf(m.group(4)),m.group(1),3600);
                    ams.clear();
                    ams.add(am);
                    RPCResponse<Long> r =client.address_mappings_set(null, ams);
                    r.get();
                } else
                    System.out.println("not a valid mapping: "+options.get("mapping").stringValue);

            }

            rpcClient.shutdown();
            rpcClient.waitForShutdown();
            

        } catch (IOException ex) {
            System.out.println("cannot contact server");
            ex.printStackTrace();
            System.exit(2);
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(3);
        }

    }

    private static void printAddressMappings(AddressMappingSet ams) {
        if (ams.size() > 0) {
            System.out.println("UUID "+ams.get(0).getUuid()+" (version "+ams.get(0).getVersion()+")");
            for (AddressMapping am : ams) {
                System.out.println("     "+am.getMatch_network()+" -> "+am.getProtocol()+"://"+am.getAddress()+":"+am.getPort()+"  (ttl "+am.getTtl()+")");
            }
            System.out.println("");
        } else {
            System.out.println("no mappings");
        }
    }

}
