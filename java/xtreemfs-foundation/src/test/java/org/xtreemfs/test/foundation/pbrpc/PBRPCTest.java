/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.test.foundation.pbrpc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.Ping;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.Ping.PingResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.PingServiceClient;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.util.OutputUtils;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class PBRPCTest {
    private final int TEST_PORT = 12999;
    
    private static TimeSync ts = null;

    public PBRPCTest() {
        
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logging.start(Logging.LEVEL_WARN, Logging.Category.all);
        ts = TimeSync.initializeLocal(50);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ts.close();
    }


    @Test
    public void testRPCClient() throws Exception {
        RPCNIOSocketClient client = null;
        RPCNIOSocketServer server = null;

        try {

            server = new RPCNIOSocketServer(TEST_PORT, null, new RPCServerRequestListener() {

                @Override
                public void receiveRecord(RPCServerRequest rq) {
                    // System.out.println("received request");
                    try {
                        ReusableBufferInputStream is = new ReusableBufferInputStream(rq.getMessage());
                        Ping.PingRequest pingRq = Ping.PingRequest.parseFrom(is);

                        Ping.PingResponse.PingResult result = Ping.PingResponse.PingResult.newBuilder().setText(pingRq.getText()).build();
                        Ping.PingResponse resp = Ping.PingResponse.newBuilder().setResult(result).build();

                        rq.sendResponse(resp, null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.GARBAGE_ARGS).setErrorMessage(ex.getMessage()).setDebugInfo(OutputUtils.stackTraceToString(ex)).build());
                        fail(ex.toString());

                    }
                }
            }, null);

            server.start();
            server.waitForStartup();

            client = new RPCNIOSocketClient(null, 15000, 5*60*1000, "testRPCClient");
            client.start();
            client.waitForStartup();

            PingServiceClient psClient = new PingServiceClient(client,null);

            RPC.UserCredentials userCred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("tester").build();
            RPCResponse<PingResponse> response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Hello World!", false, null);
            assertEquals(response.get().getResult().getText(),"Hello World!");

            response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Murpel", false, null);
            assertEquals(response.get().getResult().getText(),"Murpel");

        } finally {
            //clean up
            if (client != null) {
                client.shutdown();
                client.waitForShutdown();
            }
            if (server != null) {
                server.shutdown();
                server.waitForShutdown();
            }
        }

    }


    @Test
    public void testRPCWithData() throws Exception {
        RPCNIOSocketClient client = null;
        RPCNIOSocketServer server = null;

        try {

            server = new RPCNIOSocketServer(TEST_PORT, null, new RPCServerRequestListener() {

                @Override
                public void receiveRecord(RPCServerRequest rq) {
                    // System.out.println("received request");
                    try {
                        ReusableBufferInputStream is = new ReusableBufferInputStream(rq.getMessage());
                        Ping.PingRequest pingRq = Ping.PingRequest.parseFrom(is);

                        Ping.PingResponse.PingResult result = Ping.PingResponse.PingResult.newBuilder().setText(pingRq.getText()).build();
                        Ping.PingResponse resp = Ping.PingResponse.newBuilder().setResult(result).build();

                        ReusableBuffer data = null;
                        if (rq.getData() != null) {
                            data = rq.getData().createViewBuffer();
                            data.limit(data.capacity());
                            data.position(data.capacity());
                        }

                        rq.sendResponse(resp, data);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.GARBAGE_ARGS).setErrorMessage(ex.getMessage()).setDebugInfo(OutputUtils.stackTraceToString(ex)).build());
                        fail(ex.toString());

                    }
                }
            }, null);

            server.start();
            server.waitForStartup();

            client = new RPCNIOSocketClient(null, 15000, 5*60*1000, "testRPCWithData");
            client.start();
            client.waitForStartup();

            PingServiceClient psClient = new PingServiceClient(client,null);

            byte[] arr = new byte[2065];
            for (int i= 0; i < arr.length; i++)
                arr[i] = 'x';
            ReusableBuffer sendData = ReusableBuffer.wrap(arr);
            // System.out.println("data: "+sendData);

            RPC.UserCredentials userCred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("tester").build();
            RPCResponse<PingResponse> response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Hello World!", false, sendData);
            assertEquals(response.get().getResult().getText(),"Hello World!");

            ReusableBuffer recdata = response.getData();
            assertTrue(recdata.hasRemaining());
            while (recdata.hasRemaining()) {
                assertEquals(recdata.get(),(byte)'x');
            }

            response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Murpel", false, null);
            assertEquals(response.get().getResult().getText(),"Murpel");

        } finally {
            //clean up
            if (client != null) {
                client.shutdown();
                client.waitForShutdown();
            }
            if (server != null) {
                server.shutdown();
                server.waitForShutdown();
            }
        }

    }


    @Test
    public void testEmptyMessages() throws Exception {
        RPCNIOSocketClient client = null;
        RPCNIOSocketServer server = null;

        try {

            server = new RPCNIOSocketServer(TEST_PORT, null, new RPCServerRequestListener() {

                @Override
                public void receiveRecord(RPCServerRequest rq) {
                    // System.out.println("received request");
                    try {
                        assertNull(rq.getMessage());
                        rq.sendResponse(null, null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.GARBAGE_ARGS).setErrorMessage(ex.getMessage()).setDebugInfo(OutputUtils.stackTraceToString(ex)).build());
                        fail(ex.toString());

                    }
                }
            }, null);

            server.start();
            server.waitForStartup();

            client = new RPCNIOSocketClient(null, 15000, 5*60*1000, "PBRPCTest::testEmptyMessages()");
            client.start();
            client.waitForStartup();

            PingServiceClient psClient = new PingServiceClient(client,null);

            RPC.UserCredentials userCred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("tester").build();
            RPCResponse response = psClient.emptyPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred);
            assertNull(response.get());

            response = psClient.emptyPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred);
            assertNull(response.get());

            response = psClient.emptyPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred);
            assertNull(response.get());

        } finally {
            //clean up
            if (client != null) {
                client.shutdown();
                client.waitForShutdown();
            }
            if (server != null) {
                server.shutdown();
                server.waitForShutdown();
            }
        }

    }

}