//automatically generated from Ping.proto at Mon Oct 26 10:41:59 CET 2015
//(c) 2015. See LICENSE file for details.

package org.xtreemfs.foundation.pbrpc.generatedinterfaces;

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

public class PingServiceClient {

    private RPCNIOSocketClient client;
    private InetSocketAddress  defaultServer;

    public PingServiceClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        this.client = client;
        this.defaultServer = defaultServer;
    }

    public RPCResponse<Ping.PingResponse> doPing(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Ping.PingRequest input, ReusableBuffer data) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<Ping.PingResponse> response = new RPCResponse<Ping.PingResponse>(Ping.PingResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 1, 1, input, data, response, false);
         return response;
    }

    public RPCResponse<Ping.PingResponse> doPing(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String text, boolean sendError, ReusableBuffer data) throws IOException {
         final Ping.PingRequest msg = Ping.PingRequest.newBuilder().setText(text).setSendError(sendError).build();
         return doPing(server, authHeader, userCreds,msg, data);
    }

    public RPCResponse emptyPing(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Ping.Ping_emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 1, 2, input, null, response, false);
         return response;
    }

    public RPCResponse emptyPing(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return emptyPing(server, authHeader, userCreds,null);
    }

    public boolean clientIsAlive() {
        return client.isAlive();
    }
}