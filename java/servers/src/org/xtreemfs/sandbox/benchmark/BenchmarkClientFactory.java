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

    /**
     * Create a AdminClient. The starting and shutdown of the client is managed by the BenchmarkClientFactory
     * (no need to call Client.start() and Client.shutdown())
     * 
     * @param params
     *            the params data
     * @return a started AdminClient instance
     * @throws Exception
     */
    static AdminClient getNewClient(Params params) {
        return tryCreateClient(params.dirAddress, params.userCredentials, params.sslOptions,
                params.options);
    }

    static AdminClient getNewClient(String dirAddress, RPC.UserCredentials userCredentials, SSLOptions sslOptions,
            Options options) {
        return tryCreateClient(dirAddress, userCredentials, sslOptions, options);
    }

    static {
        clients = new ConcurrentLinkedQueue<AdminClient>();
    }

    /* error handling for 'createNewClient()" */
    private static AdminClient tryCreateClient(String dirAddress, RPC.UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        AdminClient client = null;
        try {
            client = createNewClient(dirAddress, userCredentials, sslOptions, options);
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, BenchmarkClientFactory.class,
                    "Could not create new AdminClient. Errormessage: %s", e.getMessage());
            Thread.yield(); // allow logger to catch up
            e.printStackTrace();
            System.exit(42);
        }
        return client;
    }

    private static AdminClient createNewClient(String dirAddress, RPC.UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) throws Exception {
        AdminClient client = ClientFactory.createAdminClient(dirAddress, userCredentials, sslOptions, options);
        clients.add(client);
        client.start();
        return client;
    }

    /* shutdown all clients */
    static void shutdownClients() {
        for (AdminClient client : clients) {
            client.shutdown();
        }
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, Runtime.getRuntime(), "Shutting down %s clients",
                clients.size());
    }

}
