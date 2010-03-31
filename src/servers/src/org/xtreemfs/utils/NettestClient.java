/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils;

import java.net.InetSocketAddress;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.client.ONCRPCClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.NettestInterface.NettestInterface;
import org.xtreemfs.interfaces.NettestInterface.nopRequest;
import org.xtreemfs.interfaces.NettestInterface.nopResponse;
import org.xtreemfs.interfaces.NettestInterface.recv_bufferRequest;
import org.xtreemfs.interfaces.NettestInterface.recv_bufferResponse;
import org.xtreemfs.interfaces.NettestInterface.send_bufferRequest;
import org.xtreemfs.interfaces.NettestInterface.send_bufferResponse;

/**
 *
 * @author bjko
 */
public class NettestClient extends ONCRPCClient {

    public NettestClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        super(client, defaultServer,0,NettestInterface.getVersion());
    }

    public RPCResponse xtreemfs_nettest_nop(InetSocketAddress server) {
        nopRequest rq = new nopRequest();

        RPCResponse r = sendRequest((server == null) ? this.getDefaultServerAddress() : server, rq.getTag(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                final nopResponse resp = new nopResponse();
                resp.unmarshal(new XDRUnmarshaller(data));
                return null;
            }
        });
        return r;
    }

    public RPCResponse xtreemfs_nettest_send_buffer(InetSocketAddress server, ReusableBuffer data) {
        send_bufferRequest rq = new send_bufferRequest(data);

        RPCResponse r = sendRequest((server == null) ? this.getDefaultServerAddress() : server, rq.getTag(), rq,
                new RPCResponseDecoder() {

            @Override
            public ReusableBuffer getResult(ReusableBuffer data) {
                final send_bufferResponse resp = new send_bufferResponse();
                resp.unmarshal(new XDRUnmarshaller(data));
                return null;
            }
        });
        return r;
    }

     public RPCResponse<ReusableBuffer> xtreemfs_nettest_recv_buffer(InetSocketAddress server, int size) {
        recv_bufferRequest rq = new recv_bufferRequest(size);

        RPCResponse r = sendRequest((server == null) ? this.getDefaultServerAddress() : server, rq.getTag(), rq,
                new RPCResponseDecoder<ReusableBuffer>() {

            @Override
            public ReusableBuffer getResult(ReusableBuffer data) {
                final recv_bufferResponse resp = new recv_bufferResponse();
                resp.unmarshal(new XDRUnmarshaller(data));
                return resp.getData();
            }
        });
        return r;
    }

}
