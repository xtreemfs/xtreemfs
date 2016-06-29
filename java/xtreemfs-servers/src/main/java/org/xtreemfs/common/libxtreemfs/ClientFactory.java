/*
 * Copyright (c) 2012 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.libxtreemfs;

import org.xtreemfs.common.libxtreemfs.jni.NativeAdminClient;
import org.xtreemfs.common.libxtreemfs.jni.NativeClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * Returns different kinds of Client implementations.
 *
 */
public class ClientFactory {

    /**
     * Specifies the type of the client implementation to use. <br>
     * NATIVE clients are using the C++ client via JNI and require access to the shared library.
     */
    public enum ClientType {
        JAVA, NATIVE
    }

    /**
     * Specifies the default ClientType that is used if the parameter is omitted.
     * 
     * TODO: Switch to native client due to new features only implemented in the C++ client. To do this, many test junit
     * testcases have to be changed.
     */
    final static ClientType defaultType = ClientType.NATIVE;

    /**
     * Returns an instance of a default client of the specified type with one DIR Address.
     *
     * @param type
     *            Type of the client instance.
     * @param dirServiceAddress
     *            Address of the DIR service (Format: ip-addr:port, e.g. localhost:32638)
     * @param userCredentials
     *            Name and Groups of the user.
     * @param sslOptions
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     */
    public static Client createClient(ClientType type, String dirServiceAddress, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        String[] dirServiceAddresses = { dirServiceAddress };
        return createClient(type, dirServiceAddresses, userCredentials, sslOptions, options);
    }

    /**
     * Returns an instance of a default client of the specified type with multiple DIR Address.
     *
     * @param type
     *            Type of the client instance.
     * @param dirServiceAddress
     *            Addresses of the DIR service (Format: ip-addr:port, e.g. localhost:32638)
     * @param userCredentials
     *            Name and Groups of the user.
     * @param sslOptions
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     */
    public static Client createClient(ClientType type, String[] dirServiceAddresses, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        switch (type) {
        case NATIVE:
            return NativeClient.createClient(dirServiceAddresses, userCredentials, sslOptions, options);
        case JAVA:
        default:
            return new ClientImplementation(dirServiceAddresses, userCredentials, sslOptions, options);
        }
    }

    /**
     * Returns an instance of an admin client of the specified type with one DIR Address.
     *
     * @param type
     *            Type of the client instance.
     * @param dirServiceAddress
     *            Address of the DIR service (Format: ip-addr:port, e.g. localhost:32638)
     * @param userCredentials
     *            Name and Groups of the user.
     * @param sslOptions
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     */
    public static AdminClient createAdminClient(ClientType type, String dirServiceAddress,
            UserCredentials userCredentials, SSLOptions sslOptions, Options options) {
        String[] dirServiceAddresses = { dirServiceAddress };
        return createAdminClient(type, dirServiceAddresses, userCredentials, sslOptions, options);
    }

    /**
     * Returns an instance of a default client of the specified type with multiple DIR Address.
     *
     * @param type
     *            Type of the client instance.
     * @param dirServiceAddresses
     *            Address of the DIR service (Format: ip-addr:port, e.g. localhost:32638)
     * @param userCredentials
     *            Name and Groups of the user.
     * @param sslOptions
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     */
    public static AdminClient createAdminClient(ClientType type, String[] dirServiceAddresses,
            UserCredentials userCredentials, SSLOptions sslOptions, Options options) {
        switch (type) {
        case NATIVE:
            return NativeAdminClient.createClient(dirServiceAddresses, userCredentials, sslOptions, options);
        case JAVA:
        default:
            return new ClientImplementation(dirServiceAddresses, userCredentials, sslOptions, options);
        }
    }

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
    public static Client createClient(String dirServiceAddress, UserCredentials userCredentials, SSLOptions sslOptions,
            Options options) {
        String[] dirAddresses = new String[1];
        dirAddresses[0] = dirServiceAddress;
        return createClient(defaultType, dirAddresses, userCredentials, sslOptions, options);
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
    public static Client createClient(String[] dirServiceAddresses, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        return createClient(defaultType, dirServiceAddresses, userCredentials, sslOptions, options);
    }

    /**
     * Returns an instance of an admin client with one DIR address.
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
        return createAdminClient(defaultType, dirAddresses, userCredentials, sslOptions, options);
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
    public static AdminClient createAdminClient(String[] dirServiceAddresses, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        return createAdminClient(defaultType, dirServiceAddresses, userCredentials, sslOptions, options);
    }
}
