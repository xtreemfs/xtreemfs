/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.DIRInterface.getAddressMappingsRequest;
import org.xtreemfs.interfaces.DIRInterface.getAddressMappingsResponse;

/**
 *
 * @author bjko
 */
public class DummyONCRPCServer {


    public static void main(String[] args) {
        try {
            Logging.start(Logging.LEVEL_DEBUG);
             RPCServerRequestListener listener = new RPCServerRequestListener() {

                @Override
                public void receiveRecord(ONCRPCRequest rq) {
                    try {
                        System.out.println("request received");
                        ReusableBuffer buf = rq.getRequestFragment();

                        getAddressMappingsRequest rpcRequest = new getAddressMappingsRequest();
                        rpcRequest.deserialize(buf);

                        getAddressMappingsResponse rpcResponse = new getAddressMappingsResponse();

                        if (rpcRequest.uuid.equalsIgnoreCase("Yagga")) {
                            rpcResponse.address_mappings.add(new AddressMapping("Yagga", 1, "rpc", "localhost", 12345, "*", 3600));
                            System.out.println("response size is "+rpcResponse.getSize());
                            rq.sendResponse(rpcResponse);
                        } else {
                            rq.sendGarbageArgsError();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.exit(1);
                    }


                }
            };
            RPCNIOSocketServer server = new RPCNIOSocketServer(12345, null, listener, null);
            server.start();
            server.waitForStartup();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
