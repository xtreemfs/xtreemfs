package org.xtreemfs.utils;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceClient;
import org.xtreemfs.scheduler.SchedulerClient;

public class xtfs_show_reservations {
	static Map<String, CliOption> options;
    private static final int RPC_TIMEOUT = 15000;
    private static final int CONNECTION_TIMEOUT = 5 * 60 * 1000;
    private static final int MAX_RETRIES = 15;
    private static final int RETRY_WAIT = 1000;
    private static final int DEFAULT_PORT = 32642;

    private static Map<String, String> schedule;
	
	public static void main(String[] args) {
		Logging.start(Logging.LEVEL_WARN);
		options = utils.getDefaultAdminToolOptions(false);

        options.put("r", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "Get free resources", ""));

        List<String> arguments = new ArrayList<String>(1);
        CLIParser.parseCLI(args, options, arguments);
        
        if (options.get(utils.OPTION_HELP).switchValue || options.get(utils.OPTION_HELP_LONG).switchValue) {
            usage(options);
            return;
        }

        if (arguments.size() > 2 || arguments.size() < 1)
            error("invalid number of arguments", options);
        
        String schedulerURL = arguments.get(0);
        
        boolean gridSSL = false;
        SSLOptions sslOptions = null;
        String serviceCredsFile = options.get(utils.OPTION_USER_CREDS_FILE).stringValue;
        String serviceCredsPass = options.get(utils.OPTION_USER_CREDS_PASS).stringValue;
        String trustedCAsFile = options.get(utils.OPTION_TRUSTSTORE_FILE).stringValue;
        String trustedCAsPass = options.get(utils.OPTION_TRUSTSTORE_PASS).stringValue;
        
        if (schedulerURL.contains(Schemes.SCHEME_PBRPCG + "://")) {
            gridSSL = true;
        }
        
        // TODO: support custom SSL trust managers
        try {
            if(serviceCredsFile != null && serviceCredsPass != null &&
                    trustedCAsFile != null && trustedCAsPass != null) {
                sslOptions = new SSLOptions(new FileInputStream(serviceCredsFile), serviceCredsPass,
                        SSLOptions.PKCS12_CONTAINER, new FileInputStream(trustedCAsFile),
                        trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL, null);
            }

            TimeSync.initializeLocal(0).waitForStartup();

            InetSocketAddress schedulerSocket = getSchedulerConnection(schedulerURL);
            RPCNIOSocketClient client = new RPCNIOSocketClient(sslOptions, RPC_TIMEOUT, CONNECTION_TIMEOUT);
            client.start();
            client.waitForStartup();
            SchedulerServiceClient schedulerServiceClient = new SchedulerServiceClient(client, schedulerSocket);

            UserCredentials userCreds = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();
            Auth authHeader = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
            
            SchedulerClient schedulerClient = new SchedulerClient(schedulerServiceClient, schedulerSocket, MAX_RETRIES, RETRY_WAIT);
            Scheduler.reservationSet reservations = schedulerClient.getAllVolumes(schedulerSocket, authHeader, userCreds);

            schedule = new HashMap<String, String>();

            for(Scheduler.reservation r: reservations.getReservationsList()) {
                Scheduler.osdSet osds = schedulerClient.getSchedule(schedulerSocket, authHeader, userCreds, r.getVolume());
                String tmp = "";
                for(Scheduler.osdIdentifier osd: osds.getOsdList()) {
                    if(tmp.equals(""))
                        tmp += osd.getUuid();
                    else
                        tmp += ", " + osd.getUuid();
                }
                schedule.put(r.getVolume().getUuid(), tmp);
            }

            printReservations(reservations);

            if(options.get("r").switchValue) {
                Scheduler.freeResourcesResponse freeRes =
                        schedulerClient.getFreeResources(schedulerSocket, authHeader, userCreds);
                System.out.println("\nFree Resources:");
                printFreeResources(freeRes);
            }

            client.shutdown();
            client.waitForShutdown();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
	}
	
    private static void error(String message, Map<String, CliOption> options) {
        System.err.println(message);
        System.out.println();
        usage(options);
        System.exit(1);
    }
	
    private static void usage(Map<String, CliOption> options) {
        System.out.println("usage: xtfs_show_reservations [options] <scheduler service>");
        System.out.println("<scheduler service> the scheduler service to use (e.g. 'pbrpc://localhost:32642')");
        System.out.println();
        System.out.println("Options:");
        utils.printOptions(options);
    }
    
    private static void printReservations(Scheduler.reservationSet reservations) {
        if(reservations.getReservationsCount() > 0) {
            for(Scheduler.reservation r: reservations.getReservationsList()) {
                System.out.println("Volume:\t\t\t" + r.getVolume().getUuid());
                System.out.println("Capacity:\t\t" + r.getCapacity() + " MB");
                System.out.println("Sequential-Throughput:\t" + r.getStreamingThroughput() + " MB/s");
                System.out.println("Ramdom-Throughput:\t" + r.getRandomThroughput() + " IOPS");
                System.out.println("Type:\t\t\t" + typeToString(r.getType()) + "");
                System.out.println("Schedule:\t\t" + schedule.get(r.getVolume().getUuid()) + "\n");
            }
        } else {
            System.out.println("No reservations");
        }
    }

    private static void printFreeResources(Scheduler.freeResourcesResponse freeResources) {
        System.out.println("Sequential Capacity:\t\t" + freeResources.getStreamingCapacity());
        System.out.println("Sequential Throughput:\t\t" + freeResources.getStreamingThroughput());
        System.out.println("Random Capacity:\t\t" + freeResources.getRandomCapacity());
        System.out.println("Random Throughput:\t\t" + freeResources.getRandomThroughput());
    }

    private static String typeToString(Scheduler.reservationType type) {
        switch(type) {
            case COLD_STORAGE_RESERVATION:
                return "COLD_STORAGE_RESERVATION";
            case RANDOM_IO_RESERVATION:
                return "RANDOM_IO_RESERVATION";
            case STREAMING_RESERVATION:
                return "STREAMING_RESERVATION";
            case BEST_EFFORT_RESERVATION:
            default:
                return "BEST_EFFORT_RESERVATION";
        }
    }
    
    private static InetSocketAddress getSchedulerConnection(String url) {
    	String host = null;
    	int port = DEFAULT_PORT;
    	
        if(url.contains("://")) {
        	url = url.split("://")[1];
        }
        
        if(url.contains(":")) {
        	String[] tmp = url.split(":");
        	
        	if(tmp.length != 2) {
        		error("invalid scheduler url", options);
        	} else {
        		host = tmp[0];
        		port = Integer.parseInt(tmp[1]);
        	}
        } else {
        	host = url;
        }
        
    	return new InetSocketAddress(host, port);
    }
}
