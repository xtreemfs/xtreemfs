/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc;

import java.io.IOException;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.junit.After;
import org.junit.Before;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.pbrpc.utils.RecordMarker;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferOutputStream;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class RPCNIOSocketServerTest {

    private RPCNIOSocketServer server;

    public RPCNIOSocketServerTest() {
        Logging.start(Logging.LEVEL_WARN, Logging.Category.all);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testSerialization() throws Exception {
        final int CALLID = 5464566;

        RPC.Auth auth = RPC.Auth.newBuilder().setAuthType(RPC.AuthType.AUTH_NONE).build();
        RPC.UserCredentials ucred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("user").build();
        RPC.RPCHeader.RequestHeader rqHdr = RPC.RPCHeader.RequestHeader.newBuilder().setAuthData(auth).setUserCreds(ucred).setProcId(2).setInterfaceId(2).build();
        RPC.RPCHeader header = RPC.RPCHeader.newBuilder().setCallId(CALLID).setMessageType(RPC.MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();

        ReusableBufferOutputStream ois = new ReusableBufferOutputStream(ReusableBufferOutputStream.BUFF_SIZE);
        header.writeTo(ois);
        ois.flip();

        byte[] data = new byte[ois.getBuffers()[0].remaining()];
        ois.getBuffers()[0].get(data);

        ReusableBufferInputStream is = new ReusableBufferInputStream(ReusableBuffer.wrap(data));

        RPC.RPCHeader deser = RPC.RPCHeader.parseFrom(is);

    }

    @Test
    public void testSimpleRPC() throws Exception {

        final int CALLID = 5464566;
        final int TEST_PORT = 9991;
        final String USERID = "yaggaYagga";

        server = new RPCNIOSocketServer(TEST_PORT, null, new RPCServerRequestListener() {

            @Override
            public void receiveRecord(RPCServerRequest rq) {
                // System.out.println("received request");
                try {
                    assertEquals(CALLID,rq.getHeader().getCallId());
                    assertEquals(RPC.MessageType.RPC_REQUEST, rq.getHeader().getMessageType());

                    //send a dummy message
                    RPC.UserCredentials msg = RPC.UserCredentials.newBuilder().setUsername(USERID).build();

                    ReusableBuffer data = BufferPool.allocate(2);
                    data.put((byte)15);
                    data.put((byte)20);

                    rq.sendResponse(msg, data);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.GARBAGE_ARGS).setErrorMessage(ex.getMessage()).setDebugInfo(OutputUtils.stackTraceToString(ex)).build());
                    fail(ex.toString());

                }
            }
        }, null);

        server.start();
        server.waitForStartup();

        Socket sock = new Socket("localhost", TEST_PORT);
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();

        RPC.Auth auth = RPC.Auth.newBuilder().setAuthType(RPC.AuthType.AUTH_NONE).build();
        RPC.UserCredentials ucred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("user").build();
        RPC.RPCHeader.RequestHeader rqHdr = RPC.RPCHeader.RequestHeader.newBuilder().setAuthData(auth).setUserCreds(ucred).setProcId(2).setInterfaceId(2).build();
        RPC.RPCHeader header = RPC.RPCHeader.newBuilder().setCallId(CALLID).setMessageType(RPC.MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();

        ReusableBufferOutputStream ois = new ReusableBufferOutputStream(ReusableBufferOutputStream.BUFF_SIZE);
        header.writeTo(ois);
        int hdrLen = ois.length();

        ByteBuffer recordMarker = ByteBuffer.allocate(RecordMarker.HDR_SIZE);
        recordMarker.putInt(hdrLen);
        recordMarker.putInt(0);
        recordMarker.putInt(0);
        recordMarker.flip();

        ois.flip();

        out.write(recordMarker.array());
        byte[] data = new byte[ois.getBuffers()[0].remaining()];
        ois.getBuffers()[0].get(data);
        out.write(data);

        byte[] markerIn = new byte[RecordMarker.HDR_SIZE];
        in.read(markerIn);
        ReusableBuffer marker = ReusableBuffer.wrap(markerIn);

        hdrLen = marker.getInt();
        int msgLen = marker.getInt();
        int dataLen = marker.getInt();

        // System.out.println("header: "+hdrLen+"/"+msgLen+"/"+dataLen);

        byte[] hdrIn = new byte[hdrLen];
        byte[] msgIn = new byte[msgLen];
        byte[] dataIn = new byte[dataLen];

        in.read(hdrIn);
        in.read(msgIn);
        in.read(dataIn);

        // System.out.println("read data");

        RPC.RPCHeader respHdr = RPC.RPCHeader.parseFrom(hdrIn);

        RPC.UserCredentials uc = RPC.UserCredentials.parseFrom(msgIn);

        assertEquals(RPC.MessageType.RPC_RESPONSE_SUCCESS,respHdr.getMessageType());
        assertEquals(header.getCallId(),respHdr.getCallId());

        assertEquals(USERID,uc.getUsername());

        assertEquals(15,dataIn[0]);
        assertEquals(20,dataIn[1]);

        sock.close();
        server.shutdown();
        server.waitForShutdown();
    }

    @Test
    public void testRPCWithoutMessage() throws Exception {

        final int CALLID = 5464566;
        final int TEST_PORT = 9991;
        final String USERID = "yaggaYagga";

        server = new RPCNIOSocketServer(TEST_PORT, null, new RPCServerRequestListener() {

            @Override
            public void receiveRecord(RPCServerRequest rq) {
                // System.out.println("received request");
                try {
                    assertNotNull(rq.getData());
                    assertNull(rq.getMessage());
                    assertEquals(CALLID,rq.getHeader().getCallId());
                    assertEquals(RPC.MessageType.RPC_REQUEST, rq.getHeader().getMessageType());

                    //send a dummy message
                    RPC.UserCredentials msg = RPC.UserCredentials.newBuilder().setUsername(USERID).build();

                    ReusableBuffer data = BufferPool.allocate(2);
                    data.put((byte)15);
                    data.put((byte)20);

                    rq.sendResponse(msg, data);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.GARBAGE_ARGS).setErrorMessage(ex.getMessage()).setDebugInfo(OutputUtils.stackTraceToString(ex)).build());
                    fail(ex.toString());

                }
            }
        }, null);

        server.start();
        server.waitForStartup();

        Socket sock = new Socket("localhost", TEST_PORT);
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();

        RPC.Auth auth = RPC.Auth.newBuilder().setAuthType(RPC.AuthType.AUTH_NONE).build();
        RPC.UserCredentials ucred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("user").build();
        RPC.RPCHeader.RequestHeader rqHdr = RPC.RPCHeader.RequestHeader.newBuilder().setAuthData(auth).setUserCreds(ucred).setProcId(2).setInterfaceId(2).build();
        RPC.RPCHeader header = RPC.RPCHeader.newBuilder().setCallId(CALLID).setMessageType(RPC.MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();

        ReusableBufferOutputStream ois = new ReusableBufferOutputStream(ReusableBufferOutputStream.BUFF_SIZE);
        header.writeTo(ois);
        int hdrLen = ois.length();

        ByteBuffer recordMarker = ByteBuffer.allocate(RecordMarker.HDR_SIZE);
        recordMarker.putInt(hdrLen);
        recordMarker.putInt(0);
        recordMarker.putInt(16);
        recordMarker.flip();

        ois.flip();

        out.write(recordMarker.array());
        byte[] data = new byte[ois.getBuffers()[0].remaining()];
        ois.getBuffers()[0].get(data);
        out.write(data);

        for (int i = 0; i < 16; i++) {
            out.write('a');
        }

        byte[] markerIn = new byte[RecordMarker.HDR_SIZE];
        in.read(markerIn);
        ReusableBuffer marker = ReusableBuffer.wrap(markerIn);

        hdrLen = marker.getInt();
        int msgLen = marker.getInt();
        int dataLen = marker.getInt();

        // System.out.println("header: "+hdrLen+"/"+msgLen+"/"+dataLen);

        byte[] hdrIn = new byte[hdrLen];
        byte[] msgIn = new byte[msgLen];
        byte[] dataIn = new byte[dataLen];

        in.read(hdrIn);
        in.read(msgIn);
        in.read(dataIn);

        // System.out.println("read data");

        RPC.RPCHeader respHdr = RPC.RPCHeader.parseFrom(hdrIn);

        RPC.UserCredentials uc = RPC.UserCredentials.parseFrom(msgIn);

        assertEquals(RPC.MessageType.RPC_RESPONSE_SUCCESS,respHdr.getMessageType());
        assertEquals(header.getCallId(),respHdr.getCallId());

        assertEquals(USERID,uc.getUsername());

        assertEquals(15,dataIn[0]);
        assertEquals(20,dataIn[1]);

        sock.close();
        server.shutdown();
        server.waitForShutdown();
    }

    @Test
    public void testErrorResponse() throws Exception {

        final int CALLID = 5464566;
        final int TEST_PORT = 9991;
        final String USERID = "yaggaYagga";

        server = new RPCNIOSocketServer(TEST_PORT, null, new RPCServerRequestListener() {

            @Override
            public void receiveRecord(RPCServerRequest rq) {
                // System.out.println("received request");
                try {
                    assertEquals(CALLID,rq.getHeader().getCallId());
                    assertEquals(RPC.MessageType.RPC_REQUEST, rq.getHeader().getMessageType());

                    rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.AUTH_FAILED).setErrorMessage("dummy error").setDebugInfo("no info here").build());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.GARBAGE_ARGS).setErrorMessage(ex.getMessage()).setDebugInfo(OutputUtils.stackTraceToString(ex)).build());
                    rq.freeBuffers();
                    fail(ex.toString());
                }
            }
        }, null);

        server.start();
        server.waitForStartup();

        Socket sock = new Socket("localhost", TEST_PORT);
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();

        RPC.Auth auth = RPC.Auth.newBuilder().setAuthType(RPC.AuthType.AUTH_NONE).build();
        RPC.UserCredentials ucred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("user").build();
        RPC.RPCHeader.RequestHeader rqHdr = RPC.RPCHeader.RequestHeader.newBuilder().setAuthData(auth).setUserCreds(ucred).setProcId(2).setInterfaceId(2).build();
        RPC.RPCHeader header = RPC.RPCHeader.newBuilder().setCallId(CALLID).setMessageType(RPC.MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();

        ReusableBufferOutputStream ois = new ReusableBufferOutputStream(ReusableBufferOutputStream.BUFF_SIZE);
        header.writeTo(ois);
        int hdrLen = ois.length();

        ByteBuffer recordMarker = ByteBuffer.allocate(RecordMarker.HDR_SIZE);
        recordMarker.putInt(hdrLen);
        recordMarker.putInt(0);
        recordMarker.putInt(0);
        recordMarker.flip();

        ois.flip();

        out.write(recordMarker.array());
        byte[] data = new byte[ois.getBuffers()[0].remaining()];
        ois.getBuffers()[0].get(data);
        out.write(data);

        byte[] markerIn = new byte[RecordMarker.HDR_SIZE];
        in.read(markerIn);
        ReusableBuffer marker = ReusableBuffer.wrap(markerIn);

        hdrLen = marker.getInt();
        int msgLen = marker.getInt();
        int dataLen = marker.getInt();

        assertEquals(0,msgLen);
        assertEquals(0,dataLen);

        // System.out.println("header: "+hdrLen+"/"+msgLen+"/"+dataLen);

        byte[] hdrIn = new byte[hdrLen];

        in.read(hdrIn);

        RPC.RPCHeader respHdr = RPC.RPCHeader.parseFrom(hdrIn);

        assertEquals(RPC.MessageType.RPC_RESPONSE_ERROR,respHdr.getMessageType());
        assertEquals(header.getCallId(),respHdr.getCallId());
        assertEquals(RPC.ErrorType.AUTH_FAILED,respHdr.getErrorResponse().getErrorType());

        sock.close();
        server.shutdown();
        server.waitForShutdown();
    }

}