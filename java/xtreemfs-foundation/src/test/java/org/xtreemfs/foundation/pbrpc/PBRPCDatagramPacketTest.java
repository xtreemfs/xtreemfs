/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.utils.PBRPCDatagramPacket;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.Ping.PingRequest;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;

import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class PBRPCDatagramPacketTest {

    public PBRPCDatagramPacketTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testPacket() throws Exception {

        RPCHeader.RequestHeader rqHdr = RPCHeader.RequestHeader.newBuilder().setAuthData(RPCAuthentication.authNone).setUserCreds(RPCAuthentication.userService).setInterfaceId(1).setProcId(2).build();
        RPCHeader hdr = RPCHeader.newBuilder().setCallId(12345).setMessageType(MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();

        PingRequest pRq = PingRequest.newBuilder().setText("YAGGA!").setSendError(false).build();

        PBRPCDatagramPacket dp = new PBRPCDatagramPacket(hdr, pRq);
        ReusableBuffer data = dp.assembleDatagramPacket();

        dp = new PBRPCDatagramPacket(data, PingRequest.getDefaultInstance());

        PingRequest response = (PingRequest)dp.getMessage();

        assertEquals(dp.getHeader().getCallId(),12345);
        assertEquals(dp.getHeader().getMessageType(),MessageType.RPC_REQUEST);
        assertEquals(response.getText(),pRq.getText());
    }

}