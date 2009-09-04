/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.osd;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingResponse;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;

/**
 *
 * @author bjko
 */
public class SimpleVivaldiStageTest extends TestCase {
    
    TestEnvironment env;

    public SimpleVivaldiStageTest() {
        Logging.start(Logging.LEVEL_DEBUG);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        env = new TestEnvironment(new TestEnvironment.Services[]{Services.DIR_SERVICE,Services.OSD});
        env.start();
    }

    @After
    public void tearDown() {
        env.shutdown();
    }

    @Test
    public void testVivaldiPing() throws Exception {
        xtreemfs_pingRequest payload = new xtreemfs_pingRequest(new VivaldiCoordinates());
         
        ONCRPCRequestHeader rq = new ONCRPCRequestHeader(1, 1, 1, payload.getTag());
        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(rq.getXDRSize(), true);

        ONCRPCBufferWriter wr = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        wr.writeInt32(null,fragHdr);
        rq.marshal(wr);
        payload.marshal(wr);
        wr.flip();

        DatagramSocket dsock = new DatagramSocket();
        byte[] data = wr.getBuffers().get(0).array();
        DatagramPacket dpack = new DatagramPacket(data, data.length, env.getOSDAddress());
        dsock.send(dpack);

        DatagramPacket answer = new DatagramPacket(new byte[1024], 1024);
        dsock.setSoTimeout(250);
        dsock.receive(answer);

        ReusableBuffer rb = ReusableBuffer.wrap(answer.getData());

        rb.position(Integer.SIZE/8);
        ONCRPCResponseHeader rhdr = new ONCRPCResponseHeader();
        rhdr.unmarshal(new XDRUnmarshaller(rb));

        xtreemfs_pingResponse resp = new xtreemfs_pingResponse();
        resp.unmarshal(new XDRUnmarshaller(rb));

        dsock.close();
    }
    // public void hello() {}

}