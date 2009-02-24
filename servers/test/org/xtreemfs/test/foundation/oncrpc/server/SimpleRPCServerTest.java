/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.foundation.oncrpc.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.DIRInterface.getAddressMappingsRequest;
import org.xtreemfs.interfaces.DIRInterface.getAddressMappingsResponse;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class SimpleRPCServerTest {

    public static final int TEST_PORT = 12345;

    RPCNIOSocketServer server;

    public SimpleRPCServerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logging.start(Logging.LEVEL_DEBUG);
        TimeSync.initialize(null, 100000, 50, "");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        TimeSync.close();
    }

    @Before
    public void setUp() {
        
    }

    @After
    public void tearDown() {
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
        server = new RPCNIOSocketServer(TEST_PORT, null, listener, null);
        server.start();
        server.waitForStartup();

        Socket sock = new Socket("localhost", TEST_PORT);
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();

        ONCRPCRequestHeader rqHdr = new ONCRPCRequestHeader(1, 2, 3, 4);

        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(5+rqHdr.calculateSize(), true);
        System.out.println("fragHdr is "+fragHdr);
        System.out.println("fragment size is "+(fragHdr ^ (1 << 31)));
        writer.putInt(fragHdr);
        rqHdr.serialize(writer);
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
        final ReusableBuffer buf = BufferPool.allocate(4+rhdr.calculateSize()+5);
        buf.position(0);
        for (int i = 0; i < 4+rhdr.calculateSize()+5; i++) {
            final int dataByte = in.read();
            assertTrue(dataByte != -1);
            buf.put((byte)dataByte);
        }
        buf.position(0);
        
        final int respFragHdr = buf.getInt();
        final int fragmentSize = ONCRPCRecordFragmentHeader.getFragmentLength(respFragHdr);
        final boolean isLastFrag = ONCRPCRecordFragmentHeader.isLastFragment(respFragHdr);
        assertEquals(fragmentSize,5+rhdr.calculateSize());
        assertTrue(isLastFrag);

        rhdr.deserialize(buf);

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

                    getAddressMappingsRequest rpcRequest = new getAddressMappingsRequest();
                    rpcRequest.deserialize(buf);

                    getAddressMappingsResponse rpcResponse = new getAddressMappingsResponse();

                    if (rpcRequest.getUuid().equalsIgnoreCase("Yagga")) {
                        rpcResponse.getAddress_mappings().add(new AddressMapping("Yagga", 1, "rpc", "localhost", 12345, "*", 3600));
                        System.out.println("response size is "+rpcResponse.calculateSize());
                        rq.sendResponse(rpcResponse);
                    } else {
                        rq.sendGarbageArgs(null);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail();
                }


            }
        };
        server = new RPCNIOSocketServer(TEST_PORT, null, listener, null);
        server.start();
        server.waitForStartup();

        Socket sock = new Socket("localhost", TEST_PORT);
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();

        ONCRPCRequestHeader rqHdr = new ONCRPCRequestHeader(1, 20000000+1, 2, 4);

        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        
        getAddressMappingsRequest rq = new getAddressMappingsRequest("Yagga");

        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(rqHdr.calculateSize()+rq.calculateSize(), true);
        System.out.println("fragment size is "+fragHdr+"/"+(rqHdr.calculateSize()+rq.calculateSize()));
        System.out.println("fragment size is "+(fragHdr ^ (1 << 31)));
        writer.putInt(fragHdr);
        rqHdr.serialize(writer);
        
        rq.serialize(writer);
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

        rhdr.deserialize(buf);

        System.out.println("bytes left: "+buf.remaining());

        assertEquals(rhdr.getXID(),1);
        assertEquals(rhdr.getReplyStat(),rhdr.REPLY_STAT_MSG_ACCEPTED);
        assertEquals(rhdr.getAcceptStat(),rhdr.ACCEPT_STAT_SUCCESS);

        getAddressMappingsResponse resp = new getAddressMappingsResponse();
        resp.deserialize(buf);
        assertNotNull(resp.getAddress_mappings().get(0));
        assertEquals(resp.getAddress_mappings().get(0).getAddress(),"localhost");

        System.out.println("everything ok!");

        sock.close();
        server.shutdown();
        server.waitForShutdown();

    }

    

}