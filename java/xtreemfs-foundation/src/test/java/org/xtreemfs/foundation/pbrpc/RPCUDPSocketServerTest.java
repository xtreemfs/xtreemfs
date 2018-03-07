/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import java.net.InetSocketAddress;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCUDPSocketServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.Ping.PingRequest;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;

/**
 *
 * @author bjko
 */
public class RPCUDPSocketServerTest {

    RPCUDPSocketServer server1, server2;

    final static int PORT_1 = 33333;
    final static int PORT_2 = 33334;

    public RPCUDPSocketServerTest() {
        Logging.start(Logging.LEVEL_WARN, Logging.Category.all);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testUDP() throws Exception {
        server1 = new RPCUDPSocketServer(PORT_1, new RPCServerRequestListener() {

            @Override
            public void receiveRecord(RPCServerRequest rq) {
                // System.out.println("srv1: "+rq);
            }
        });

        server2 = new RPCUDPSocketServer(PORT_2, new RPCServerRequestListener() {

            @Override
            public void receiveRecord(RPCServerRequest rq) {
                // System.out.println("srv2: "+rq);
                rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, "yagga");
            }
        });

        server1.start();
        server2.start();

        server1.waitForStartup();
        server2.waitForStartup();

        RPCHeader.RequestHeader rqHdr = RPCHeader.RequestHeader.newBuilder().setAuthData(RPCAuthentication.authNone).setUserCreds(RPCAuthentication.userService).setInterfaceId(1).setProcId(5).build();
        RPCHeader hdr = RPCHeader.newBuilder().setCallId(555).setMessageType(MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();
        PingRequest pRq = PingRequest.newBuilder().setSendError(false).setText("yagga").build();
        server1.sendRequest(hdr, pRq, new InetSocketAddress("localhost",PORT_2));

        Thread.sleep(100);

        server1.shutdown();
        server2.shutdown();

        server1.waitForShutdown();
        server2.waitForShutdown();

    }

}