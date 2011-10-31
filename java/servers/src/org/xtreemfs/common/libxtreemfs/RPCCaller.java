/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * 
 * <br>
 * Sep 7, 2011
 */
public class RPCCaller {

    protected static <C, R, V> V makeCall(C client, Method m, UserCredentials userCredentials, Auth auth,
            R request, UUIDIterator uuidIterator, UUIDResolver uuidResolver, int maxTries, Options options,
            boolean uuidIteratorHasAddresses) throws Exception {
        return RPCCaller.<C, R, V> makeCall(client, m, userCredentials, auth, request, uuidIterator,
                uuidResolver, maxTries, options, uuidIteratorHasAddresses, false);
    }

    // TODO: use MaxTries and Options to repeat calls on failure.
    @SuppressWarnings("unchecked")
    protected static <C, R, V> V makeCall(C client, Method m, UserCredentials userCredentials, Auth auth,
            R request, UUIDIterator uuidIterator, UUIDResolver uuidResolver, int maxTries, Options options,
            boolean uuidIteratorHasAddresses, boolean delayRetryOnError) throws Exception {

        assert (m.getDeclaringClass().isInstance(client));
        assert (uuidIteratorHasAddresses || uuidResolver != null);

        // create an InetSocketAddresse depending on the uuidIterator and the kind of service
        InetSocketAddress server;
        if (uuidIteratorHasAddresses) {
            server = getInetSocketAddressFromAddressAndServiceClient(uuidIterator.getUUID(), client);
        } else { // UUIDIterator has really UUID, not just address Strings. :P
            String address = uuidResolver.uuidToAddress(uuidIterator.getUUID());
            server = getInetSocketAddressFromAddressAndServiceClient(address, client);
        }

        RPCResponse<?> response = null;
        V returnValue = null;
        try {
            Object obj = m.invoke(client, new Object[] { server, auth, userCredentials, request });
            if (obj instanceof RPCResponse<?>) {
                response = (RPCResponse<?>) obj;
            }

            Method get = RPCResponse.class.getMethod("get", new Class<?>[0]);
            obj = get.invoke(response, new Object[0]);

            // TODO: Figure out how to make a safe cast to avoid the unchecked warning and get rid of
            // the @SuppressWarnings("unchecked") annotation.
            returnValue = (V) obj;
        } catch (InvocationTargetException ite) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, client, "%s", ite.getCause()
                        .getMessage());
            }
            throw new Exception(ite.getCause());
        } catch (IllegalAccessException accE) {
            // Should never happen except there is an programming error on invocation of this
            // method
            accE.printStackTrace();
        } catch (IllegalArgumentException argE) {
            // Should never happen except there is an programming error on invocation of this
            // method
            argE.printStackTrace();
        } finally {
            if (response != null) {
                response.freeBuffers();
            }
        }

        return returnValue;
    }

    /**
     * Create an InetSocketAddress depending on the address and the type of service object is. If address does
     * not contain a port a default port depending on the client object is used.
     * 
     * @param address
     *            The address.
     * @param client
     *            The service object used to determine which default port should used when address does not
     *            contain a port.
     * @return
     */
    private static InetSocketAddress getInetSocketAddressFromAddressAndServiceClient(String address,
            Object client) {

        if (client instanceof DIRServiceClient) {
            return Helper.stringToInetSocketAddress(address,
                    GlobalTypes.PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber());
        }
        if (client instanceof MRCServiceClient)
            return Helper.stringToInetSocketAddress(address,
                    GlobalTypes.PORTS.MRC_PBRPC_PORT_DEFAULT.getNumber());
        if (client instanceof OSDServiceClient) {
            return Helper.stringToInetSocketAddress(address,
                    GlobalTypes.PORTS.OSD_PBRPC_PORT_DEFAULT.getNumber());
        }
        return null;

    }

}
