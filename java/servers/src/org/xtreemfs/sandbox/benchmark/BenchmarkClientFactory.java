/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;

/**
 * @author jensvfischer
 */
public class BenchmarkClientFactory {

    private static ConcurrentLinkedQueue<AdminClient> clients;

    static {
        clients = new ConcurrentLinkedQueue<AdminClient>();
    }

    /**
     * Create a AdminClient. The starting and shutdown of the client is managed by the BenchmarkClientFactory
     * (no need to call Client.start() and Client.shutdown())
     * 
     * @param params
     *            the params data
     * @return a started AdminClient instance
     * @throws Exception
     */
    static AdminClient getNewClient(Params params) throws Exception {
        AdminClient client = ClientFactory.createAdminClient(params.dirAddress, params.userCredentials, params.sslOptions, params.options);
        clients.add(client);
        client.start();
        return client;
    }

    static AdminClient getNewClient(String dirAddress, RPC.UserCredentials userCredentials, SSLOptions sslOptions,
            Options options) throws Exception {
        AdminClient client = ClientFactory.createAdminClient(dirAddress, userCredentials, sslOptions, options);
        clients.add(client);
        client.start();
        return client;
    }

    /* shutdown all clients */
    static void shutdownClients() {
        for (AdminClient client : clients) {
            tryShutdownOfClient(client);
        }
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, BenchmarkClientFactory.class, "Shutting down %s clients",
                clients.size());
    }

    static void tryShutdownOfClient(Client client) {
        try {
            client.shutdown();
        } catch (Throwable e) {
            Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.tool, BenchmarkClientFactory.class,
                    "Error while shutting down clients");
            Logging.logError(Logging.LEVEL_WARN, BenchmarkClientFactory.class, e);
        }
    }

}
