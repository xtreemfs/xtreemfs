/*
 * Copyright (c) 2012 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.libxtreemfs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
            // return NativeClient.createClient(dirServiceAddresses, userCredentials, sslOptions, options);
            try {
                Class<?> clazz = Class.forName("org.xtreemfs.common.libxtreemfs.jni.NativeClient");
                Method factory = clazz.getMethod("createClient", String[].class, UserCredentials.class,
                        SSLOptions.class, Options.class);
                Client client = (Client) factory
                        .invoke(null, dirServiceAddresses, userCredentials, sslOptions, options);

                return client;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            }

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
            // return AdminNativeClient.createClient(dirServiceAddresses, userCredentials, sslOptions, options);
            try {
                Class<?> clazz = Class.forName("org.xtreemfs.common.libxtreemfs.jni.AdminNativeClient");
                Method factory = clazz.getMethod("createClient", String[].class, UserCredentials.class,
                        SSLOptions.class, Options.class);
                AdminClient client = (AdminClient) factory.invoke(null, dirServiceAddresses, userCredentials,
                        sslOptions, options);

                return client;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            }

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
        return createClient(ClientType.JAVA, dirAddresses, userCredentials, sslOptions, options);
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
        return createClient(ClientType.JAVA, dirServiceAddresses, userCredentials, sslOptions, options);
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
        return createAdminClient(ClientType.JAVA, dirAddresses, userCredentials, sslOptions, options);
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
        return createAdminClient(ClientType.JAVA, dirServiceAddresses, userCredentials, sslOptions, options);
    }
}