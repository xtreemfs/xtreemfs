/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
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
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.utils.PBRPCDatagramPacket;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_pingMesssage;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;
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
        xtreemfs_pingMesssage payload = xtreemfs_pingMesssage.newBuilder().setRequestResponse(true).setCoordinates(VivaldiCoordinates.newBuilder().setXCoordinate(1.1).setYCoordinate(1.2).setLocalError(0.5)).build();

        RPCHeader.RequestHeader rqHdr = RPCHeader.RequestHeader.newBuilder().setAuthData(RPCAuthentication.authNone).
                setUserCreds(RPCAuthentication.userService).setInterfaceId(OSDServiceConstants.INTERFACE_ID).
                setProcId(OSDServiceConstants.PROC_ID_XTREEMFS_PING).build();
        RPCHeader hdr = RPCHeader.newBuilder().setCallId(5).setMessageType(MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();

        PBRPCDatagramPacket dpack = new PBRPCDatagramPacket(hdr, payload);

        DatagramSocket dsock = new DatagramSocket();
        byte[] data = dpack.assembleDatagramPacket().array();
        DatagramPacket dpack2 = new DatagramPacket(data, data.length, env.getOSDAddress());
        dsock.send(dpack2);

        DatagramPacket answer = new DatagramPacket(new byte[2048], 2048);
        dsock.setSoTimeout(250);
        dsock.receive(answer);

        ReusableBuffer rb = ReusableBuffer.wrap(answer.getData(), 0, answer.getLength());
        dpack = new PBRPCDatagramPacket(rb, payload);

        System.out.println("result: "+dpack.getMessage());

        dsock.close();
    }

    /*@Test
    public void testVivaldiPingTCP() throws Exception {
        RPCResponse<VivaldiCoordinates> vc = env.getOSDClient().internal_vivaldi_ping(env.getOSDAddress(), new VivaldiCoordinates(1.1, 1.2, 0.5));
        VivaldiCoordinates rv = vc.get();
        vc.freeBuffers();
        

    }*/

    @Test
    public void testVivaldiCoordinates() throws Exception {
        final VivaldiCoordinates c1 = VivaldiCoordinates.newBuilder().setXCoordinate(1.1).setYCoordinate(1.2).setLocalError(0.5).build();
        final String c1s = VivaldiNode.coordinatesToString(c1);
        final VivaldiCoordinates c2 = VivaldiNode.stringToCoordinates(c1s);

        assertEquals(c1.getXCoordinate(),c2.getXCoordinate());
        assertEquals(c1.getYCoordinate(),c2.getYCoordinate());
        assertEquals(c1.getLocalError(),c2.getLocalError());
    }
    // public void hello() {}

}