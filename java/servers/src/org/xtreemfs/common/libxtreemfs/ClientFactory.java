/*
 * Copyright (c) 2012 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.libxtreemfs;

import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * Returns different kinds of Client implementations.
 * 
 */
public class ClientFactory {

    /**
     * Returns an instance of a default client with one DIR Address.
     * 
     * @param dirServiceAddress
     *            Address of the DIR service (Format: ip-addr:port, e.g. localhost:32638)
     * @param userCredentials
     *            Name and Groups of the user.
     * @param sslOptions
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     */
    public static Client createClient(String dirServiceAddress, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        String[] dirAddresses = new String[1];
        dirAddresses[0] = dirServiceAddress;
        return new ClientImplementation(dirAddresses, userCredentials, sslOptions, options);
    }

    /**
     * Returns an instance of a default client with multiple DIR Addresses.
     * 
     * @param dirServiceAddresses
     *            Addresses of the DIR services (Format: ip-addr:port, e.g. localhost:32638)
     * @param userCredentials
     *            Name and Groups of the user.
     * @param sslOptions
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     */
    public static Client createClient(String[] dirServiceAddresseses, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        return new ClientImplementation(dirServiceAddresseses, userCredentials, sslOptions, options);
    }

    /**
     * Returns an instance of a admin client with one DIR Address.
     * 
     * @param dirServiceAddress
     *            Address of the DIR service (Format: ip-addr:port, e.g. localhost:32638)
     * @param userCredentials
     *            Name and Groups of the user.
     * @param sslOptions
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     */
    public static AdminClient createAdminClient(String dirServiceAddress, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        String[] dirAddresses = new String[1];
        dirAddresses[0] = dirServiceAddress;
        return new ClientImplementation(dirAddresses, userCredentials, sslOptions, options);
    }

    /**
     * Returns an instance of a admin client with multiple DIR Addresses.
     * 
     * @param dirServiceAddresses
     *            Addresses of the DIR services (Format: ip-addr:port, e.g. localhost:32638)
     * @param userCredentials
     *            Name and Groups of the user.
     * @param sslOptions
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     * 
     */
    public static AdminClient createAdminClient(String[] dirServiceAddresses,
            UserCredentials userCredentials, SSLOptions sslOptions, Options options) {
        return new ClientImplementation(dirServiceAddresses, userCredentials, sslOptions, options);
    }
}