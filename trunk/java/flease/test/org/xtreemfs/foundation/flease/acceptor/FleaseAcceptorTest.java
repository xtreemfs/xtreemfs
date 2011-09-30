/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease.acceptor;

import java.net.InetSocketAddress;
import junit.framework.TestCase;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.comm.FleaseMessage.MsgType;
import org.xtreemfs.foundation.flease.comm.ProposalNumber;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class FleaseAcceptorTest extends TestCase {

    private FleaseAcceptor acceptor;
    private final static ASCIIString TESTCELL = new ASCIIString("testcell");

    private final FleaseConfig cfg;
    
    public FleaseAcceptorTest(String testName) {
        super(testName);
        Logging.start(Logging.LEVEL_DEBUG, Category.all);
        TimeSync.initializeLocal(10000, 50);

        cfg = new FleaseConfig(10000, 500, 500, new InetSocketAddress(12345), "localhost:12345",1);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        acceptor = new FleaseAcceptor(new LearnEventListener() {

            public void learnedEvent(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long me) {
            }
        }, cfg, "/tmp/xtreemfs-test/", true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        acceptor.shutdown();
    }

    public void testPrepAccLearn() {

        FleaseMessage prep = new FleaseMessage(FleaseMessage.MsgType.MSG_PREPARE);
        prep.setCellId(TESTCELL);
        prep.setProposalNo(new ProposalNumber(1, 1));
        prep.setSendTimestamp(TimeSync.getLocalSystemTime());
        prep.setLeaseTimeout(TimeSync.getLocalSystemTime()+10000);
        prep.setLeaseHolder(new ASCIIString("me"));

        FleaseMessage response = acceptor.processMessage(prep);
        assertEquals(MsgType.MSG_PREPARE_ACK,response.getMsgType());

        prep = new FleaseMessage(MsgType.MSG_ACCEPT,prep);
        prep.setSendTimestamp(TimeSync.getLocalSystemTime());

        response = acceptor.processMessage(prep);
        assertEquals(MsgType.MSG_ACCEPT_ACK,response.getMsgType());

        prep = new FleaseMessage(MsgType.MSG_LEARN,prep);
        prep.setSendTimestamp(TimeSync.getLocalSystemTime());
        response = acceptor.processMessage(prep);
        assertNull(response);

    }

    public void testConcurrentProposal() {

        FleaseMessage prep1 = new FleaseMessage(FleaseMessage.MsgType.MSG_PREPARE);
        prep1.setCellId(TESTCELL);
        prep1.setProposalNo(new ProposalNumber(1, 1));
        prep1.setSendTimestamp(TimeSync.getLocalSystemTime());
        prep1.setLeaseTimeout(TimeSync.getLocalSystemTime()+10000);
        prep1.setLeaseHolder(new ASCIIString("me1"));

        FleaseMessage prep2 = new FleaseMessage(FleaseMessage.MsgType.MSG_PREPARE);
        prep2.setCellId(TESTCELL);
        prep2.setProposalNo(new ProposalNumber(1, 2));
        prep2.setSendTimestamp(TimeSync.getLocalSystemTime());
        prep2.setLeaseTimeout(TimeSync.getLocalSystemTime()+10000);
        prep2.setLeaseHolder(new ASCIIString("me2"));

        FleaseMessage response = acceptor.processMessage(prep1);
        assertEquals(MsgType.MSG_PREPARE_ACK,response.getMsgType());

        response = acceptor.processMessage(prep2);
        assertEquals(MsgType.MSG_PREPARE_ACK,response.getMsgType());

        prep1 = new FleaseMessage(MsgType.MSG_ACCEPT,prep1);
        prep1.setSendTimestamp(TimeSync.getLocalSystemTime());

        response = acceptor.processMessage(prep1);
        assertEquals(MsgType.MSG_ACCEPT_NACK,response.getMsgType());

    }


    public void testGetLease() {


        FleaseMessage prep2 = new FleaseMessage(FleaseMessage.MsgType.MSG_PREPARE);
        prep2.setCellId(TESTCELL);
        prep2.setProposalNo(new ProposalNumber(1, 2));
        prep2.setSendTimestamp(TimeSync.getLocalSystemTime());
        prep2.setLeaseTimeout(TimeSync.getLocalSystemTime()+10000);
        prep2.setLeaseHolder(new ASCIIString("me2"));


        FleaseMessage response = acceptor.processMessage(prep2);
        assertEquals(MsgType.MSG_PREPARE_ACK,response.getMsgType());

        prep2 = new FleaseMessage(MsgType.MSG_ACCEPT,prep2);
        prep2.setSendTimestamp(TimeSync.getLocalSystemTime());
        response = acceptor.processMessage(prep2);
        assertEquals(MsgType.MSG_ACCEPT_ACK,response.getMsgType());

        FleaseMessage lease = acceptor.getLocalLeaseInformation(TESTCELL);
        assertNull(lease);

        prep2 = new FleaseMessage(MsgType.MSG_LEARN,prep2);
        prep2.setSendTimestamp(TimeSync.getLocalSystemTime());
        response = acceptor.processMessage(prep2);
        assertNull(response);

        lease = acceptor.getLocalLeaseInformation(TESTCELL);
        assertNotNull(lease);
        assertEquals(new ASCIIString("me2"),lease.getLeaseHolder());



    }

    

}
