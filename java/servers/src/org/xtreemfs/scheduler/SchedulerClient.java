package org.xtreemfs.scheduler;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceClient;

import com.google.protobuf.Message;

public class SchedulerClient {
	protected SchedulerServiceClient rpcClient;
	protected InetSocketAddress server;
	protected int maxRetries;
	protected int retryWaitMs;
	protected final Auth auth;
	protected final UserCredentials user;

	public SchedulerClient(SchedulerServiceClient rpcClient,
			InetSocketAddress server, int maxRetries, int retryWaitMs) {
		if (server == null) {
			throw new IllegalArgumentException(
					"Must provide a scheduler service address.");
		}
		this.rpcClient = rpcClient;
		this.server = server;
		this.maxRetries = maxRetries;
		this.retryWaitMs = retryWaitMs;
		auth = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
		user = UserCredentials.newBuilder().setUsername("service")
				.addGroups("xtreemfs").build();
	}

	public Scheduler.osdSet scheduleReservation(InetSocketAddress server,
			Auth authHeader, UserCredentials userCreds,
			final Scheduler.reservation input) throws IOException,
			InterruptedException {
		Object response = syncCall(new CallGenerator<Scheduler.osdSet>() {
			@Override
			public RPCResponse<Scheduler.osdSet> executeCall(
					SchedulerServiceClient client, InetSocketAddress server)
					throws IOException {
				return client.scheduleReservation(server, auth, user, input);
			}
		}, maxRetries);
		return (Scheduler.osdSet) response;
	}

	public Scheduler.osdSet getSchedule(InetSocketAddress server,
			Auth authHeader, UserCredentials userCreds,
			final Scheduler.volumeIdentifier volume) throws IOException,
			InterruptedException {
		Object response = syncCall(new CallGenerator<Scheduler.osdSet>() {
			@Override
			public RPCResponse<Scheduler.osdSet> executeCall(
					SchedulerServiceClient client, InetSocketAddress server)
					throws IOException {
				return client.getSchedule(server, auth, user, volume);
			}
		}, maxRetries);
		return (Scheduler.osdSet) response;
	}

	@SuppressWarnings("rawtypes")
	public void removeReservation(InetSocketAddress server, Auth authHeader,
			UserCredentials userCreds, final Scheduler.volumeIdentifier volume)
			throws IOException, InterruptedException {
		syncCall(new CallGenerator() {
			@Override
			public RPCResponse executeCall(SchedulerServiceClient client,
					InetSocketAddress server) throws IOException {
				return client.removeReservation(server, auth, user, volume);
			}
		}, maxRetries);
	}

	public Scheduler.volumeSet getVolumes(InetSocketAddress server,
			Auth authHeader, UserCredentials userCreds,
			final Scheduler.osdIdentifier osd) throws IOException,
			InterruptedException {
		Object response = syncCall(new CallGenerator<Scheduler.volumeSet>() {
			@Override
			public RPCResponse<Scheduler.volumeSet> executeCall(
					SchedulerServiceClient client, InetSocketAddress server)
					throws IOException {
				return client.getVolumes(server, auth, user, osd);
			}
		}, maxRetries);
		return (Scheduler.volumeSet) response;
	}
	
	public Scheduler.reservationSet getAllVolumes(InetSocketAddress server,
			Auth authHeader, UserCredentials userCreds) throws IOException, InterruptedException {
		Object response = syncCall(new CallGenerator<Scheduler.reservationSet>() {
			@Override
			public RPCResponse<Scheduler.reservationSet> executeCall(
					SchedulerServiceClient client, InetSocketAddress server)
					throws IOException {
				return client.getAllVolumes(server, auth, user);
			}
		}, maxRetries);
		return (Scheduler.reservationSet) response;
	}

	/**
	 * Interface for syncCall which generates the calls. Will be called for each
	 * retry.
	 */
	protected interface CallGenerator<K extends Message> {
		public RPCResponse<K> executeCall(SchedulerServiceClient client,
				InetSocketAddress server) throws IOException;
	}

	protected Object syncCall(CallGenerator<?> call, int maxRetries)
			throws InterruptedException, PBRPCException, IOException {
		assert maxRetries != 0 : "Current SchedulerClient implementation supports no infinite retries.";
		int numTries = 1;
		Exception lastException = null;
		while (numTries <= maxRetries) {
			RPCResponse<?> response = null;
			try {
				response = call.executeCall(rpcClient, server);
				Object result = response.get();
				return result;
			} catch (PBRPCException ex) {
				switch (ex.getErrorType()) {
				case ERRNO:
					throw ex;
				default: {
					lastException = ex;
					if (numTries <= maxRetries) {
						Thread.sleep(retryWaitMs);
					}
					break;
				}
				}
			} catch (IOException ex) {
				Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
						"Request failed due to exception: %s", ex);
				lastException = ex;
				if (numTries <= maxRetries) {
					Thread.sleep(retryWaitMs);
				}
			} finally {
				if (response != null) {
					response.freeBuffers();
				}
			}
			numTries++;
		}

		throw new IOException("Request finally failed after " + (numTries - 1)
				+ " tries.", lastException);
	}
}
