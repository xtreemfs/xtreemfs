//automatically generated from Scheduler.proto at Mon Apr 22 14:58:58 CEST 2013
//(c) 2013. See LICENSE file for details.

package org.xtreemfs.pbrpc.generatedinterfaces;

import java.io.IOException;
import java.util.List;
import java.net.InetSocketAddress;
import com.google.protobuf.Message;
import com.google.protobuf.ByteString;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;

public class SchedulerServiceClient {

    private RPCNIOSocketClient client;
    private InetSocketAddress  defaultServer;

    public SchedulerServiceClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        this.client = client;
        this.defaultServer = defaultServer;
    }

    public RPCResponse<Scheduler.osdSet> scheduleReservation(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Scheduler.reservation input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<Scheduler.osdSet> response = new RPCResponse<Scheduler.osdSet>(Scheduler.osdSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 40001, 101, input, null, response, false);
         return response;
    }

    public RPCResponse<Scheduler.osdSet> scheduleReservation(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Scheduler.volumeIdentifier volume, Scheduler.reservationType type, double capacity, double streamingThroughput, double randomThroughput) throws IOException {
         final Scheduler.reservation msg = Scheduler.reservation.newBuilder().setVolume(volume).setType(type).setCapacity(capacity).setStreamingThroughput(streamingThroughput).setRandomThroughput(randomThroughput).build();
         return scheduleReservation(server, authHeader, userCreds,msg);
    }

    public RPCResponse removeReservation(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Scheduler.volumeIdentifier input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 40001, 102, input, null, response, false);
         return response;
    }

    public RPCResponse removeReservation(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid) throws IOException {
         final Scheduler.volumeIdentifier msg = Scheduler.volumeIdentifier.newBuilder().setUuid(uuid).build();
         return removeReservation(server, authHeader, userCreds,msg);
    }

    public RPCResponse<Scheduler.osdSet> getSchedule(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Scheduler.volumeIdentifier input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<Scheduler.osdSet> response = new RPCResponse<Scheduler.osdSet>(Scheduler.osdSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 40001, 103, input, null, response, false);
         return response;
    }

    public RPCResponse<Scheduler.osdSet> getSchedule(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid) throws IOException {
         final Scheduler.volumeIdentifier msg = Scheduler.volumeIdentifier.newBuilder().setUuid(uuid).build();
         return getSchedule(server, authHeader, userCreds,msg);
    }

    public RPCResponse<Scheduler.volumeSet> getVolumes(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Scheduler.osdIdentifier input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<Scheduler.volumeSet> response = new RPCResponse<Scheduler.volumeSet>(Scheduler.volumeSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 40001, 104, input, null, response, false);
         return response;
    }

    public RPCResponse<Scheduler.volumeSet> getVolumes(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid) throws IOException {
         final Scheduler.osdIdentifier msg = Scheduler.osdIdentifier.newBuilder().setUuid(uuid).build();
         return getVolumes(server, authHeader, userCreds,msg);
    }

    public RPCResponse<Scheduler.reservationSet> getAllVolumes(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<Scheduler.reservationSet> response = new RPCResponse<Scheduler.reservationSet>(Scheduler.reservationSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 40001, 105, input, null, response, false);
         return response;
    }

    public RPCResponse<Scheduler.reservationSet> getAllVolumes(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return getAllVolumes(server, authHeader, userCreds,null);
    }

    public RPCResponse<Scheduler.freeResourcesResponse> getFreeResources(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<Scheduler.freeResourcesResponse> response = new RPCResponse<Scheduler.freeResourcesResponse>(Scheduler.freeResourcesResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 40001, 106, input, null, response, false);
         return response;
    }

    public RPCResponse<Scheduler.freeResourcesResponse> getFreeResources(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return getFreeResources(server, authHeader, userCreds,null);
    }

    public boolean clientIsAlive() {
        return client.isAlive();
    }
}