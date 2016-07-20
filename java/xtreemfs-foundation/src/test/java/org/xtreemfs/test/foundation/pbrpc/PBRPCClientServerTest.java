/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.foundation.pbrpc;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.junit.Test;
import org.xtreemfs.foundation.pbrpc.Schemes;
import java.net.InetSocketAddress;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.Ping.PingResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.PingServiceClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.Ping;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.Ping.PingRequest;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.util.OutputUtils;

import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class PBRPCClientServerTest {
    private final int TEST_PORT = 12999;

    private static final String[] schemes = new String[]{Schemes.SCHEME_PBRPC, Schemes.SCHEME_PBRPCS, Schemes.SCHEME_PBRPCG};
    
    private static TimeSync ts = null;

    public PBRPCClientServerTest() {
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}\
    @Test
    public void testRegularRPC() throws Exception {
        ResponseCreator creator = new ResponseCreator() {
            @Override
            public void answer(RPCServerRequest rq, PingRequest pRq) throws Exception {
                Ping.PingResponse.PingResult result = Ping.PingResponse.PingResult.newBuilder().setText(pRq.getText()).build();
                Ping.PingResponse resp = Ping.PingResponse.newBuilder().setResult(result).build();

                rq.sendResponse(resp, null);
            }
        };

        TestExecutor exec = new TestExecutor() {

            @Override
            public void execTest(RPCNIOSocketClient client) throws Exception {
                PingServiceClient psClient = new PingServiceClient(client,null);

                RPC.UserCredentials userCred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("tester").build();
                RPCResponse<PingResponse> response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Hello World!", false, null);
                assertEquals(response.get().getResult().getText(),"Hello World!");
                response.freeBuffers();

                response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Murpel", false, null);
                assertEquals(response.get().getResult().getText(),"Murpel");
                response.freeBuffers();
            }
        };
        for (String scheme: schemes)
            runTest(scheme, creator, exec);

    }


    @Test
    public void testErrorResponse() throws Exception {
        ResponseCreator creator = new ResponseCreator() {
            @Override
            public void answer(RPCServerRequest rq, PingRequest pRq) throws Exception {
                rq.sendError(RPC.ErrorType.ERRNO, RPC.POSIXErrno.POSIX_ERROR_EIO, "YaggYagga");
            }
        };

        TestExecutor exec = new TestExecutor() {

            @Override
            public void execTest(RPCNIOSocketClient client) throws Exception {
                PingServiceClient psClient = new PingServiceClient(client,null);

                RPC.UserCredentials userCred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("tester").build();
                RPCResponse<PingResponse> response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Hello World!", false, null);

                try {
                    response.get();
                    fail("expected error response");
                } catch (PBRPCException ex) {
                    assertEquals(ex.getErrorType(),RPC.ErrorType.ERRNO);
                    assertEquals(ex.getPOSIXErrno(),RPC.POSIXErrno.POSIX_ERROR_EIO);
                }
                response.freeBuffers();


            }
        };
        for (String scheme: schemes)
            runTest(scheme, creator, exec);

    }

    @Test
    public void testInternalServerError() throws Exception {
        ResponseCreator creator = new ResponseCreator() {
            @Override
            public void answer(RPCServerRequest rq, PingRequest pRq) throws Exception {
                rq.sendRedirect("redirtome");
            }
        };

        TestExecutor exec = new TestExecutor() {

            @Override
            public void execTest(RPCNIOSocketClient client) throws Exception {
                PingServiceClient psClient = new PingServiceClient(client,null);

                RPC.UserCredentials userCred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("tester").build();
                RPCResponse<PingResponse> response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Hello World!", false, null);

                try {
                    response.get();
                    fail("expected error response");
                } catch (PBRPCException ex) {
                    assertEquals(ex.getErrorType(),RPC.ErrorType.REDIRECT);
                    assertEquals(ex.getRedirectToServerUUID(),"redirtome");
                }
                response.freeBuffers();


            }
        };
        for (String scheme: schemes)
            runTest(scheme, creator, exec);

    }

    @Test
    public void testDataPing() throws Exception {
        ResponseCreator creator = new ResponseCreator() {
            @Override
            public void answer(RPCServerRequest rq, PingRequest pRq) throws Exception {
                Ping.PingResponse.PingResult result = Ping.PingResponse.PingResult.newBuilder().setText(pRq.getText()).build();
                Ping.PingResponse resp = Ping.PingResponse.newBuilder().setResult(result).build();

                ReusableBuffer data = null;
                if (rq.getData() != null) {
                    data = rq.getData().createViewBuffer();
                    data.limit(data.capacity());
                    data.position(data.capacity());
                }

                rq.sendResponse(resp, data);
            }
        };

        TestExecutor exec = new TestExecutor() {

            @Override
            public void execTest(RPCNIOSocketClient client) throws Exception {
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
                response.freeBuffers();

            }
        };
        for (String scheme: schemes)
            runTest(scheme, creator, exec);

    }

    @Test
    public void testTimeout() throws Exception {
        ResponseCreator creator = new ResponseCreator() {
            @Override
            public void answer(RPCServerRequest rq, PingRequest pRq) throws Exception {
                //don't do anything
            }
        };

        TestExecutor exec = new TestExecutor() {

            @Override
            public void execTest(RPCNIOSocketClient client) throws Exception {
                PingServiceClient psClient = new PingServiceClient(client,null);

                RPC.UserCredentials userCred = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("tester").build();
                RPCResponse<PingResponse> response = psClient.doPing(new InetSocketAddress("localhost", TEST_PORT), RPCAuthentication.authNone, userCred, "Hello World!", false, null);

                try {
                    response.get();
                    fail("expected error response");
                } catch (IOException ex) {
                }
                response.freeBuffers();


            }
        };
        for (String scheme: schemes)
            runTest(scheme, creator, exec);

    }

    public void runTest(String pbrpcScheme, ResponseCreator creator, TestExecutor exec) throws Exception {
        RPCNIOSocketClient client = null;
        RPCNIOSocketServer server = null;

        // System.out.println("loading ssl context");

        SSLOptions srvSSL = null;
        SSLOptions clientSSL = null;
        if (pbrpcScheme.equals(Schemes.SCHEME_PBRPCS) || pbrpcScheme.equals(Schemes.SCHEME_PBRPCG)) {
            srvSSL = createSSLOptions("DIR.p12", "passphrase", SSLOptions.PKCS12_CONTAINER,
                    "trusted.jks", "passphrase", SSLOptions.JKS_CONTAINER, pbrpcScheme.equals(Schemes.SCHEME_PBRPCG), null);

            clientSSL= createSSLOptions("Client.p12", "passphrase",
                SSLOptions.PKCS12_CONTAINER, "trusted.jks", "passphrase", SSLOptions.JKS_CONTAINER, pbrpcScheme.equals(Schemes.SCHEME_PBRPCG), null);
        }

        // System.out.println("setup done");

        try {

            server = getServer(creator,srvSSL);

            server.start();
            server.waitForStartup();

            client = new RPCNIOSocketClient(clientSSL, 5000, 5*60*1000, "runTest");
            client.start();
            client.waitForStartup();

            exec.execTest(client);

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
    
    private RPCNIOSocketServer getServer(final ResponseCreator creator, SSLOptions sslOpt) throws IOException {
        return new RPCNIOSocketServer(TEST_PORT, null, new RPCServerRequestListener() {

                @Override
                public void receiveRecord(RPCServerRequest rq) {
                // System.out.println("received request");
                    try {
                        ReusableBufferInputStream is = new ReusableBufferInputStream(rq.getMessage());
                        Ping.PingRequest pingRq = Ping.PingRequest.parseFrom(is);

                        creator.answer(rq, pingRq);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.GARBAGE_ARGS).setErrorMessage(ex.getMessage()).setDebugInfo(OutputUtils.stackTraceToString(ex)).build());
                        fail(ex.toString());

                    }
                }
            }, sslOpt);
    }

    private SSLOptions createSSLOptions(String keyStoreName, String ksPassphrase,
        String ksContainerType, String trustStoreName, String tsPassphrase, String tsContainerType, boolean gridSSL, String sslProtocolString)
        throws IOException {

        ClassLoader cl = this.getClass().getClassLoader();

        InputStream ks = cl.getResourceAsStream(keyStoreName);
        if (ks == null) {
            // Assume the working directory is "java/xtreemfs-servers".
            String testCert = "../../tests/certs/" + keyStoreName;
            if (new File(testCert).isFile()) {
                ks = new FileInputStream(testCert);
            } else {
                // Assume the working directory is the root of the project.
                ks = new FileInputStream("tests/certs/" + keyStoreName);
            }
        }

        InputStream ts = cl.getResourceAsStream(trustStoreName);
        if (ts == null) {
            // Assume the working directory is "java/xtreemfs-servers".
            String testCert = "../../tests/certs/" + trustStoreName;
            if (new File(testCert).isFile()) {
                ts = new FileInputStream(testCert);
            } else {
                // Assume the working directory is the root of the project.
                ts = new FileInputStream("tests/certs/" + trustStoreName);
            }
        }

        return new SSLOptions(ks, ksPassphrase, ksContainerType, ts, tsPassphrase, tsContainerType, false, gridSSL, sslProtocolString, null);
    }

    private static interface ResponseCreator {
        public void answer(RPCServerRequest rq, PingRequest pRq) throws Exception;
    }

    private static interface TestExecutor {
        public void execTest(RPCNIOSocketClient client) throws Exception;
    }

}