/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.foundation.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.junit.Test;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseListener;
import org.xtreemfs.foundation.oncrpc.server.NullAuthFlavorProvider;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.DIRInterface.ConcurrentModificationException;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DIRInterface.ProtocolException;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.test.TestEnvironment;

/**
 *
 * @author bjko
 */
public class FakeSSLRPCClientServerTest extends TestCase {

    public static final int TEST_PORT = 12345;

    RPCNIOSocketServer server;
    RPCNIOSocketClient client;
    private TestEnvironment testEnv;

    private SSLOptions srvSSL, clientSSL;

    public FakeSSLRPCClientServerTest() throws Exception {
        Logging.start(Logging.LEVEL_DEBUG);
    }

    public void setUp() throws Exception {
        System.out.println("starting");
        testEnv = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.DIR_CLIENT,
        TestEnvironment.Services.TIME_SYNC,TestEnvironment.Services.UUID_RESOLVER
        });
        testEnv.start();

        System.out.println("loading ssl context");

        srvSSL = createSSLOptions("DIR.p12", "passphrase", SSLOptions.PKCS12_CONTAINER,
            "trusted.jks", "passphrase", SSLOptions.JKS_CONTAINER, true);

        clientSSL = createSSLOptions("Client.p12", "passphrase",
            SSLOptions.PKCS12_CONTAINER, "trusted.jks", "passphrase", SSLOptions.JKS_CONTAINER, true);

        System.out.println("setup done");
    }

    public void tearDown() throws Exception {
        testEnv.shutdown();
    }

    @Test
    public void testRPCMessage() throws Exception {
        RPCServerRequestListener listener = new RPCServerRequestListener() {

            @Override
            public void receiveRecord(org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest rq) {
                try {
                    System.out.println("request received");
                    ReusableBuffer buf = rq.getRequestFragment();

                    xtreemfs_address_mappings_getRequest rpcRequest = new xtreemfs_address_mappings_getRequest();
                    rpcRequest.unmarshal(new XDRUnmarshaller(buf));

                    xtreemfs_address_mappings_getResponse rpcResponse = new xtreemfs_address_mappings_getResponse();

                    if (rpcRequest.getUuid().equalsIgnoreCase("Yagga")) {
                        rpcResponse.getAddress_mappings().add(new AddressMapping("Yagga", 1, "rpc", "localhost", 12345, "*", 3600,""));
                        System.out.println("response size is "+rpcResponse.getXDRSize());
                        rq.sendResponse(rpcResponse);
                    } else {
                        rq.sendGarbageArgs();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail();
                }


            }

        };
        server = new RPCNIOSocketServer(TEST_PORT, null, listener, srvSSL, new NullAuthFlavorProvider());
        server.start();
        server.waitForStartup();

        client = new RPCNIOSocketClient(clientSSL, 10000, 5*60*1000, Client.getExceptionParsers());
        client.start();
        client.waitForStartup();

        final AtomicReference<ONCRPCRequest> result = new AtomicReference<ONCRPCRequest>();
        RPCResponseListener rListener = new RPCResponseListener() {

            @Override
            public void responseAvailable(ONCRPCRequest request) {
                synchronized (result) {
                    result.set(request);
                    result.notifyAll();
                }
            }

            @Override
            public void requestFailed(ONCRPCRequest request, IOException reason) {
                reason.printStackTrace();
                fail();
            }

            @Override
            public void remoteExceptionThrown(ONCRPCRequest rquest, ONCRPCException exception) {
                fail();
            }

        };

        xtreemfs_address_mappings_getRequest amr = new xtreemfs_address_mappings_getRequest("Yagga");

        client.sendRequest(rListener, new InetSocketAddress("localhost", TEST_PORT), 1, DIRInterface.getVersion(),
                amr.getTag(),amr);

        synchronized (result) {
            if (result.get() == null)
                result.wait();
        }

        xtreemfs_address_mappings_getResponse amresp = new xtreemfs_address_mappings_getResponse();
        ONCRPCRequest resp = result.get();
        resp.deserializeResponse(amresp);
        resp.freeBuffers();

        assertEquals(amresp.getAddress_mappings().size(),1);
        assertEquals(amresp.getAddress_mappings().get(0).getUuid(),"Yagga");


        client.shutdown();
        client.waitForShutdown();


        server.shutdown();
        server.waitForShutdown();

    }



    @Test
    public void testRemoteException() throws Exception {
        RPCServerRequestListener listener = new RPCServerRequestListener() {

            @Override
            public void receiveRecord(org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest rq) {
                try {
                    System.out.println("request received");
                    ReusableBuffer buf = rq.getRequestFragment();

                    rq.sendException(new ConcurrentModificationException());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail();
                }


            }
        };
        server = new RPCNIOSocketServer(TEST_PORT, null, listener, srvSSL, new NullAuthFlavorProvider());
        server.start();
        server.waitForStartup();

        client = new RPCNIOSocketClient(clientSSL, 10000, 5*60*1000, Client.getExceptionParsers());
        client.start();
        client.waitForStartup();

        final AtomicReference<ONCRPCException> result = new AtomicReference<ONCRPCException>();
        RPCResponseListener rListener = new RPCResponseListener() {

            @Override
            public void responseAvailable(ONCRPCRequest request) {
                fail();
            }

            @Override
            public void requestFailed(ONCRPCRequest request, IOException reason) {
                reason.printStackTrace();
                fail();
            }

            @Override
            public void remoteExceptionThrown(ONCRPCRequest request, ONCRPCException exception) {
                synchronized (result) {
                    result.set(exception);
                    request.freeBuffers();
                    result.notifyAll();
                }
            }

        };

        xtreemfs_address_mappings_getRequest amr = new xtreemfs_address_mappings_getRequest("Yagga");

        client.sendRequest(rListener, new InetSocketAddress("localhost", TEST_PORT), 1, DIRInterface.getVersion(),
                amr.getTag(),amr);

        synchronized (result) {
            if (result.get() == null)
                result.wait();
        }

        xtreemfs_address_mappings_getResponse amresp = new xtreemfs_address_mappings_getResponse();
        ONCRPCException ex = result.get();

        assertTrue(ex instanceof ConcurrentModificationException);
        System.out.println("got expected exception: "+ex);


        client.shutdown();
        client.waitForShutdown();


        server.shutdown();
        server.waitForShutdown();

    }


    @Test
    public void testTimeout() throws Exception {
        final int MSG_TIMEOUT = 5000;
        RPCServerRequestListener listener = new RPCServerRequestListener() {

            @Override
            public void receiveRecord(org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest rq) {
                try {
                    System.out.println("request received");
                    Thread.sleep(MSG_TIMEOUT+1000);
                } catch (InterruptedException ex) {
                    //ignore
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail();
                }


            }
        };
        server = new RPCNIOSocketServer(TEST_PORT, null, listener, srvSSL, new NullAuthFlavorProvider());
        server.start();
        server.waitForStartup();

        client = new RPCNIOSocketClient(clientSSL, MSG_TIMEOUT, 5*60*1000, Client.getExceptionParsers());
        client.start();
        client.waitForStartup();

        final AtomicReference<Exception> result = new AtomicReference<Exception>();
        RPCResponseListener rListener = new RPCResponseListener() {

            @Override
            public void responseAvailable(ONCRPCRequest request) {
                fail();
            }

            @Override
            public void requestFailed(ONCRPCRequest request, IOException reason) {
                synchronized (result) {
                    result.set(reason);
                    result.notifyAll();
                }
            }

            @Override
            public void remoteExceptionThrown(ONCRPCRequest rquest, ONCRPCException exception) {
                fail();
            }
        };

        xtreemfs_address_mappings_getRequest amr = new xtreemfs_address_mappings_getRequest("Yagga");

        client.sendRequest(rListener, new InetSocketAddress("localhost", TEST_PORT), 1, DIRInterface.getVersion(),
                amr.getTag(),amr);

        synchronized (result) {
            if (result.get() == null)
                result.wait();
        }

        Exception ex = result.get();
        System.out.println("got exception as expected: "+ex);
        assertTrue(ex instanceof IOException);


        client.shutdown();
        client.waitForShutdown();


        server.shutdown();
        server.waitForShutdown();

    }

    @Test
    public void testLargeMessage() throws Exception {
        RPCServerRequestListener listener = new RPCServerRequestListener() {

            @Override
            public void receiveRecord(org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest rq) {
                try {
                    System.out.println("request received");
                    ReusableBuffer buf = rq.getRequestFragment();

                    ObjectData od = new ObjectData();
                    od.unmarshal(new XDRUnmarshaller(buf));
                    BufferPool.free(od.getData());

                    ReusableBuffer buf2 = BufferPool.allocate(1024*128);
                    buf2.limit(buf2.capacity());

                    od.setData(buf2);
                    rq.sendResponse(od);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail();
                }


            }

        };
        server = new RPCNIOSocketServer(TEST_PORT, null, listener, srvSSL, new NullAuthFlavorProvider());
        server.start();
        server.waitForStartup();

        client = new RPCNIOSocketClient(clientSSL, 10000, 5*60*1000, Client.getExceptionParsers());
        client.start();
        client.waitForStartup();

        final AtomicReference<ONCRPCRequest> result = new AtomicReference<ONCRPCRequest>();
        RPCResponseListener rListener = new RPCResponseListener() {

            @Override
            public void responseAvailable(ONCRPCRequest request) {
                synchronized (result) {
                    result.set(request);
                    result.notifyAll();
                }
            }

            @Override
            public void requestFailed(ONCRPCRequest request, IOException reason) {
                reason.printStackTrace();
                fail();
            }

            @Override
            public void remoteExceptionThrown(ONCRPCRequest rquest, ONCRPCException exception) {
                fail();
            }

        };

        ReusableBuffer buf = BufferPool.allocate(1024*64);
        ObjectData od = new ObjectData();
        buf.limit(buf.capacity());
        od.setData(buf);

        client.sendRequest(rListener, new InetSocketAddress("localhost", TEST_PORT), 1, DIRInterface.getVersion(),
                0,od);

        synchronized (result) {
            if (result.get() == null)
                result.wait();
        }




        client.shutdown();
        client.waitForShutdown();


        server.shutdown();
        server.waitForShutdown();

    }

    public static SSLOptions createSSLOptions(String keyStoreName, String ksPassphrase,
        String ksContainerType, String trustStoreName, String tsPassphrase, String tsContainerType, boolean fakeSSLMode)
        throws IOException {

        ClassLoader cl = FakeSSLRPCClientServerTest.class.getClassLoader();

        InputStream ks = cl.getResourceAsStream(keyStoreName);
        if (ks == null)
            ks = new FileInputStream("../../tests/certs/" + keyStoreName);

        InputStream ts = cl.getResourceAsStream(trustStoreName);
        if (ts == null)
            ts = new FileInputStream("../../tests/certs/" + trustStoreName);

        return new SSLOptions(ks, ksPassphrase, ksContainerType, ts, tsPassphrase, tsContainerType, false, fakeSSLMode);
    }

}
