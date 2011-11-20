/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SERVICES;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import com.google.protobuf.Message;

/**
 * 
 * <br>
 * Sep 7, 2011
 */
public class RPCCaller {

	/**
	 * Interface for syncCall which generates the calls. Will be called for each
	 * retry.
	 */
	protected interface CallGenerator<C, R extends Message> {
		public RPCResponse<R> executeCall(InetSocketAddress server,
				Auth authHeader, UserCredentials userCreds, C input)
				throws IOException;
	}

	protected static <C, R extends Message> R syncCall(SERVICES service,
			UserCredentials userCreds, Auth auth, Options options,
			UUIDResolver uuidResolver, UUIDIterator it,
			boolean uuidIteratorHasAddresses, C callRequest,
			CallGenerator<C, R> callGen) {
		return syncCall(service, userCreds, auth, options, uuidResolver, it,
				uuidIteratorHasAddresses, false, callRequest, null, callGen);
	}

	protected static <C, R extends Message> R syncCall(SERVICES service,
			UserCredentials userCreds, Auth auth, Options options,
			UUIDResolver uuidResolver, UUIDIterator it,
			boolean uuidIteratorHasAddresses, boolean delayNextTry,
			C callRequest, CallGenerator<C, R> callGen) {
		return syncCall(service, userCreds, auth, options, uuidResolver, it,
				uuidIteratorHasAddresses, delayNextTry, callRequest, null,
				callGen);
	}

	protected static <C, R extends Message> R syncCall(SERVICES service,
			UserCredentials userCreds, Auth auth, Options options,
			UUIDResolver uuidResolver, UUIDIterator it,
			boolean uuidIteratorHasAddresses, C callRequest,
			ReusableBuffer buf, CallGenerator<C, R> callGen) {
		return syncCall(service, userCreds, auth, options, uuidResolver, it,
				uuidIteratorHasAddresses, false, callRequest, buf, callGen);
	}

	protected static <C, R extends Message> R syncCall(SERVICES service,
			UserCredentials userCreds, Auth auth, Options options,
			UUIDResolver uuidResolver, UUIDIterator it,
			boolean uuidIteratorHasAddresses, boolean delayNextTry,
			C callRequest, ReusableBuffer buffer, CallGenerator<C, R> callGen) {
		R response = null;
		RPCResponse<R> r = null;
		try {
			// create an InetSocketAddresse depending on the uuidIterator and
			// the kind of service
			InetSocketAddress server;
			if (uuidIteratorHasAddresses) {
				server = getInetSocketAddressFromAddress(
						it.getUUID(), service);
			} else { // UUIDIterator has really UUID, not just address Strings.
						// :P
				String address = uuidResolver.uuidToAddress(it.getUUID());
				server = getInetSocketAddressFromAddress(
						address, service);
			}

			r = callGen.executeCall(server, auth, userCreds, callRequest);
			response = r.get();

			// If the buffer is not null it should be filled with data
			// piggybacked in the RPCResponse.
			// This is used the read request.
			if (buffer != null) {
				buffer.put(r.getData());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (r != null) {
				r.freeBuffers();
			}
		}
		return response;
	}

	/**
	 * Create an InetSocketAddress depending on the address and the type of
	 * service object is. If address does not contain a port a default port
	 * depending on the client object is used.
	 * 
	 * @param address
	 *            The address.
	 * @param service
	 *            The service used to determine which default port should
	 *            used when address does not contain a port.
	 * @return
	 */
	protected static InetSocketAddress getInetSocketAddressFromAddress(
			String address, SERVICES service) {
		if (SERVICES.DIR.equals(service)) {
			return Helper.stringToInetSocketAddress(address,
					GlobalTypes.PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber());
		}
		if (SERVICES.MRC.equals(service)) {
			return Helper.stringToInetSocketAddress(address,
					GlobalTypes.PORTS.MRC_PBRPC_PORT_DEFAULT.getNumber());
		}
		if (SERVICES.OSD.equals(service)) {
			return Helper.stringToInetSocketAddress(address,
					GlobalTypes.PORTS.OSD_PBRPC_PORT_DEFAULT.getNumber());
		}
		return null;
	}
}
