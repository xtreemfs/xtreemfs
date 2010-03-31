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
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingResponse;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
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
        env = new TestEnvironment(new TestEnvironment.Services[]{Services.DIR_SERVICE,Services.OSD, Services.OSD_CLIENT});
        env.start();
    }

    @After
    public void tearDown() {
        env.shutdown();
    }

    @Test
    public void testVivaldiPing() throws Exception {
        xtreemfs_pingRequest payload = new xtreemfs_pingRequest(new VivaldiCoordinates(1.1, 1.2, 0.5));
         
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

    @Test
    public void testVivaldiPingTCP() throws Exception {
        RPCResponse<VivaldiCoordinates> vc = env.getOSDClient().internal_vivaldi_ping(env.getOSDAddress(), new VivaldiCoordinates(1.1, 1.2, 0.5));
        VivaldiCoordinates rv = vc.get();
        vc.freeBuffers();
        

    }

    @Test
    public void testVivaldiCoordinates() throws Exception {
        final VivaldiCoordinates c1 = new VivaldiCoordinates(1.1, 2.2, 0.1);
        final String c1s = VivaldiNode.coordinatesToString(c1);
        final VivaldiCoordinates c2 = VivaldiNode.stringToCoordinates(c1s);

        assertEquals(c1.getX_coordinate(),c2.getX_coordinate());
        assertEquals(c1.getY_coordinate(),c2.getY_coordinate());
        assertEquals(c1.getLocal_error(),c2.getLocal_error());
    }
    // public void hello() {}

}