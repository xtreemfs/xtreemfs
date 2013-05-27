/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceClient;
import org.xtreemfs.scheduler.SchedulerClient;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class xtfs_create_reservation {
    static Map<String, CLIParser.CliOption> options;
    private static final int RPC_TIMEOUT = 15000;
    private static final int CONNECTION_TIMEOUT = 5 * 60 * 1000;
    private static final int MAX_RETRIES = 15;
    private static final int RETRY_WAIT = 1000;
    private static final int DEFAULT_PORT = 32642;

    public static void main(String args[]) {
        Logging.start(Logging.LEVEL_WARN);
        options = utils.getDefaultAdminToolOptions(false);
        options.put("volume", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.STRING, "Volume name", ""));
        options.put("capacity", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.NUMBER, "Volume capacity", ""));
        options.put("iops", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.NUMBER, "Random throughput (IOPS)", ""));
        options.put("seq-tp", new CLIParser.CliOption(CLIParser.CliOption.OPTIONTYPE.NUMBER, "Sequential throughput (MB/s)", ""));

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
        SSLOptions sslOptions;
        String serviceCredsFile = options.get(utils.OPTION_USER_CREDS_FILE).stringValue;
        String serviceCredsPass = options.get(utils.OPTION_USER_CREDS_PASS).stringValue;
        String trustedCAsFile = options.get(utils.OPTION_TRUSTSTORE_FILE).stringValue;
        String trustedCAsPass = options.get(utils.OPTION_TRUSTSTORE_PASS).stringValue;

        String volumeName = options.get("volumeName").stringValue;
        long capacity = options.get("capacity").numValue;
        long iops = options.get("iops").numValue;
        long seq_tp = options.get("seq-tp").numValue;

        Scheduler.reservationType type = (iops != 0) ? Scheduler.reservationType.RANDOM_IO_RESERVATION :
                ((seq_tp != 0) ? Scheduler.reservationType.STREAMING_RESERVATION :
                        Scheduler.reservationType.BEST_EFFORT_RESERVATION);

        if (schedulerURL.contains(Schemes.SCHEME_PBRPCG + "://")) {
            gridSSL = true;
        }

        // TODO: support custom SSL trust managers
        try {
            sslOptions = new SSLOptions(new FileInputStream(serviceCredsFile), serviceCredsPass,
                    SSLOptions.PKCS12_CONTAINER, new FileInputStream(trustedCAsFile),
                    trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL, null);
            InetSocketAddress schedulerSocket = getSchedulerConnection(schedulerURL);
            RPCNIOSocketClient client = new RPCNIOSocketClient(sslOptions, RPC_TIMEOUT, CONNECTION_TIMEOUT);
            SchedulerServiceClient schedulerServiceClient = new SchedulerServiceClient(client, schedulerSocket);

            RPC.UserCredentials userCreds = RPC.UserCredentials.newBuilder().setUsername("root").addGroups("root").build();
            RPC.Auth authHeader = RPC.Auth.newBuilder().setAuthType(RPC.AuthType.AUTH_NONE).build();

            Scheduler.reservation.Builder r = Scheduler.reservation.newBuilder()
                    .setVolume(Scheduler.volumeIdentifier.newBuilder().setUuid(volumeName).build())
                    .setCapacity(capacity)
                    .setRandomThroughput(iops)
                    .setStreamingThroughput(seq_tp)
                    .setType(type);

            SchedulerClient schedulerClient = new SchedulerClient(schedulerServiceClient, schedulerSocket, MAX_RETRIES, RETRY_WAIT);
            schedulerClient.scheduleReservation(schedulerSocket, authHeader, userCreds, r.build());
        } catch (Exception e) {
            System.err.println("unable to get SSL options, because:" + e.getMessage());
            System.exit(1);
        }
    }

    private static void error(String message, Map<String, CLIParser.CliOption> options) {
        System.err.println(message);
        System.out.println();
        usage(options);
        System.exit(1);
    }

    private static void usage(Map<String, CLIParser.CliOption> options) {
        System.out.println("usage: xtfs_create_reservation [options] <scheduler service>");
        System.out.println("<scheduler service> the scheduler service to use (e.g. 'pbrpc://localhost:32642')");
        System.out.println();
        System.out.println("Options:");
        utils.printOptions(options);
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
