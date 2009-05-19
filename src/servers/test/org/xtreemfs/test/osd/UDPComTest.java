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
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_broadcast_gmaxRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingResponse;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.osd.striping.UDPCommunicator;
import org.xtreemfs.osd.striping.UDPMessage;
import org.xtreemfs.osd.striping.UDPReceiverInterface;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class UDPComTest extends TestCase {

    TestEnvironment env;
    private static final int COMPORT = 22333;

    public UDPComTest() {
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
        env = new TestEnvironment(new Services[]{TestEnvironment.Services.TIME_SYNC});
        env.start();
    }

    @After
    public void tearDown() {
        env.shutdown();
    }

    @Test
    public void testUDPMessage() throws Exception {

        UDPCommunicator udpcom = new UDPCommunicator(COMPORT
                , new UDPReceiverInterface() {

            @Override
            public void receiveUDP(UDPMessage msg) {
                assert(msg.isRequest());
                assert(msg.getRequestData() != null);
                System.out.println("data: "+msg.getRequestData());
            }
        });

        udpcom.start();

        xtreemfs_broadcast_gmaxRequest payload = new xtreemfs_broadcast_gmaxRequest("1234", 1, 1, 1);
        ONCRPCRequestHeader rq = new ONCRPCRequestHeader(1, 1, 1, payload.getTag());

        ONCRPCBufferWriter wr = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        rq.serialize(wr);
        payload.serialize(wr);
        wr.flip();

        DatagramSocket dsock = new DatagramSocket();
        byte[] data = wr.getBuffers().get(0).array();
        DatagramPacket dpack = new DatagramPacket(data, data.length, new InetSocketAddress("localhost", COMPORT));
        dsock.send(dpack);

        dsock.close();

        udpcom.shutdown();


    }


    @Test
    public void testUDPResponse() throws Exception {

        UDPCommunicator udpcom = new UDPCommunicator(COMPORT
                , new UDPReceiverInterface() {

            @Override
            public void receiveUDP(UDPMessage msg) {
                assert(msg.isResponse());
                assert(msg.getResponseData() != null);
                System.out.println("data: "+msg.getResponseData());
            }
        });

        udpcom.start();

        xtreemfs_pingResponse payload = new xtreemfs_pingResponse(new VivaldiCoordinates());
        ONCRPCResponseHeader rq = new ONCRPCResponseHeader(1, ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);

        ONCRPCBufferWriter wr = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        rq.serialize(wr);
        payload.serialize(wr);
        wr.flip();

        DatagramSocket dsock = new DatagramSocket();
        byte[] data = wr.getBuffers().get(0).array();
        DatagramPacket dpack = new DatagramPacket(data, data.length, new InetSocketAddress("localhost", COMPORT));
        dsock.send(dpack);

        dsock.close();

        udpcom.shutdown();


    }

}