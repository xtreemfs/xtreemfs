/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.foundation.oncrpc.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;

import junit.framework.TestCase;

import org.junit.Test;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.server.NullAuthFlavorProvider;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.DIRInterface.ProtocolException;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getResponse;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 *
 * @author bjko
 */
public class SimpleRPCServerTest extends TestCase {

    public static final int TEST_PORT = 12345;

    RPCNIOSocketServer server;
    private TestEnvironment testEnv;

    public SimpleRPCServerTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    public void setUp() throws Exception {
        testEnv = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.DIR_CLIENT,
        TestEnvironment.Services.TIME_SYNC,TestEnvironment.Services.UUID_RESOLVER
        });
        testEnv.start();
    }

    public void tearDown() throws Exception {
        testEnv.shutdown();
    }

    //@Test
    public void testPingPong() throws Exception {
        RPCServerRequestListener listener = new RPCServerRequestListener() {

            @Override
            public void receiveRecord(ONCRPCRequest rq) {
                System.out.println("request received");
                ReusableBuffer buf = rq.getRequestFragment();
                assertTrue(buf.get() == 'p');
                assertTrue(buf.get() == 'i');
                assertTrue(buf.get() == 'n');
                assertTrue(buf.get() == 'g');
                assertTrue(buf.get() == '!');

                ReusableBuffer resp = BufferPool.allocate(5);
                resp.put((byte)'p');
                resp.put((byte)'o');
                resp.put((byte)'n');
                resp.put((byte)'g');
                resp.put((byte)'!');
                resp.flip();
                rq.sendResponse(resp);
            }
        };
        server = new RPCNIOSocketServer(TEST_PORT, null, listener, null, new NullAuthFlavorProvider());
        server.start();
        server.waitForStartup();

        Socket sock = new Socket("localhost", TEST_PORT);
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();

        ONCRPCRequestHeader rqHdr = new ONCRPCRequestHeader(1, 2, 3, 4);

        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(5+rqHdr.getXDRSize(), true);
        System.out.println("fragHdr is "+fragHdr);
        System.out.println("fragment size is "+(fragHdr ^ (1 << 31)));
        writer.writeInt32(null,fragHdr);
        rqHdr.marshal(writer);
        final ReusableBuffer dataBuf = BufferPool.allocate(5);
        dataBuf.put((byte)'p');
        dataBuf.put((byte)'i');
        dataBuf.put((byte)'n');
        dataBuf.put((byte)'g');
        dataBuf.put((byte)'!');
        dataBuf.flip();
        writer.put(dataBuf);
        writer.flip();
        System.out.println(writer);
        Iterator<ReusableBuffer> bufs = writer.getBuffers().iterator();
        while (bufs.hasNext()) {
            final ReusableBuffer buf = bufs.next();
            out.write(buf.array());
        }

        ONCRPCResponseHeader rhdr = new ONCRPCResponseHeader();
        final ReusableBuffer buf = BufferPool.allocate(4+rhdr.getXDRSize()+5);
        buf.position(0);
        for (int i = 0; i < 4+rhdr.getXDRSize()+5; i++) {
            final int dataByte = in.read();
            assertTrue(dataByte != -1);
            buf.put((byte)dataByte);
        }
        buf.position(0);
        
        final int respFragHdr = buf.getInt();
        final int fragmentSize = ONCRPCRecordFragmentHeader.getFragmentLength(respFragHdr);
        final boolean isLastFrag = ONCRPCRecordFragmentHeader.isLastFragment(respFragHdr);
        assertEquals(fragmentSize,5+rhdr.getXDRSize());
        assertTrue(isLastFrag);

        rhdr.unmarshal(new XDRUnmarshaller(buf));

        assertEquals(rhdr.getXID(),1);
        assertEquals(rhdr.getReplyStat(),rhdr.REPLY_STAT_MSG_ACCEPTED);
        assertEquals(rhdr.getAcceptStat(),rhdr.ACCEPT_STAT_SUCCESS);

        byte[] expectedResponse = { 'p','o','n','g','!' };
        for (int i = 0; i < 5; i++) {
            assertEquals(buf.get(),expectedResponse[i]);
        }

        System.out.println("everything ok!");

        sock.close();
        server.shutdown();
        server.waitForShutdown();

    }

    @Test
    public void testRPCMessage() throws Exception {
        RPCServerRequestListener listener = new RPCServerRequestListener() {

            @Override
            public void receiveRecord(ONCRPCRequest rq) {
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
        server = new RPCNIOSocketServer(TEST_PORT, null, listener, null, new NullAuthFlavorProvider());
        server.start();
        server.waitForStartup();

        Socket sock = new Socket("localhost", TEST_PORT);
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();

        ONCRPCRequestHeader rqHdr = new ONCRPCRequestHeader(1, 20000000+1, 2, 4);

        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        
        xtreemfs_address_mappings_getRequest rq = new xtreemfs_address_mappings_getRequest("Yagga");

        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(rqHdr.getXDRSize()+rq.getXDRSize(), true);
        System.out.println("fragment size is "+fragHdr+"/"+(rqHdr.getXDRSize()+rq.getXDRSize()));
        System.out.println("fragment size is "+(fragHdr ^ (1 << 31)));
        writer.writeInt32(null,fragHdr);
        rqHdr.marshal(writer);
        
        rq.marshal(writer);
        writer.flip();
        System.out.println(writer);
        Iterator<ReusableBuffer> bufs = writer.getBuffers().iterator();
        while (bufs.hasNext()) {
            final ReusableBuffer buf = bufs.next();
            while (buf.hasRemaining()) {
                out.write(buf.get());
            }
        }
        out.flush();
        System.out.println("request sent");
        System.out.println(writer);

        ONCRPCResponseHeader rhdr = new ONCRPCResponseHeader();
        ReusableBuffer buf = BufferPool.allocate(4);
        buf.position(0);
        for (int i = 0; i < 4; i++) {
            final int dataByte = in.read();
            assertTrue(dataByte != -1);
            buf.put((byte)dataByte);
        }
        buf.position(0);

        final int respFragHdr = buf.getInt();
        final int fragmentSize = ONCRPCRecordFragmentHeader.getFragmentLength(respFragHdr);
        final boolean isLastFrag = ONCRPCRecordFragmentHeader.isLastFragment(respFragHdr);
        assertTrue(isLastFrag);

        System.out.println("fragmentSize "+fragmentSize);

        buf = BufferPool.allocate(fragmentSize);
        buf.position(0);
        for (int i = 0; i < fragmentSize; i++) {
            final int dataByte = in.read();
            assertTrue(dataByte != -1);
            buf.put((byte)dataByte);
        }
        buf.position(0);

        rhdr.unmarshal(new XDRUnmarshaller(buf));

        System.out.println("bytes left: "+buf.remaining());

        assertEquals(rhdr.getXID(),1);
        assertEquals(rhdr.getReplyStat(),ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED);
        assertEquals(rhdr.getAcceptStat(),ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);

        xtreemfs_address_mappings_getResponse resp = new xtreemfs_address_mappings_getResponse();
        resp.unmarshal(new XDRUnmarshaller(buf));
        assertNotNull(resp.getAddress_mappings().get(0));
        assertEquals(resp.getAddress_mappings().get(0).getAddress(),"localhost");

        System.out.println("everything ok!");

        sock.close();
        server.shutdown();
        server.waitForShutdown();

    }

    

}