/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease.proposer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.acceptor.FleaseAcceptor;
import org.xtreemfs.foundation.flease.acceptor.LearnEventListener;
import org.xtreemfs.foundation.flease.comm.FleaseCommunicationInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class FleaseProposerTest extends TestCase {

    private FleaseAcceptor acceptor;
    private FleaseProposer proposer;
    private final static ASCIIString TESTCELL = new ASCIIString("testcell");

    private final FleaseConfig cfg;

    private final AtomicReference<Flease> result;
    
    public FleaseProposerTest(String testName) {
        super(testName);

        result = new AtomicReference();
        Logging.start(Logging.LEVEL_DEBUG, Category.all);
        TimeSync.initializeLocal(10000, 50);

        cfg = new FleaseConfig(10000, 500, 500, new InetSocketAddress(12345), "localhost:12345",5);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        acceptor = new FleaseAcceptor(new LearnEventListener() {

            public void learnedEvent(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long me) {
            }
        }, cfg, "/tmp/xtreemfs-test/", true);
        proposer = new FleaseProposer(cfg, acceptor, new FleaseCommunicationInterface() {

            public void sendMessage(FleaseMessage msg, InetSocketAddress receiver) throws IOException {
            }

            public void requestTimer(FleaseMessage msg, long timestamp) {
                if (msg.getMsgType() == FleaseMessage.MsgType.EVENT_RESTART) {
                    long wait = timestamp - TimeSync.getLocalSystemTime();
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(FleaseProposerTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    proposer.processMessage(msg);
                }
            }
        }, new FleaseStatusListener() {

            public void statusChanged(ASCIIString cellId, Flease lease) {
                System.out.println("result: "+lease.getLeaseHolder());
                synchronized (result) {
                    result.set(new Flease(cellId, lease.getLeaseHolder(), lease.getLeaseTimeout_ms(),lease.getMasterEpochNumber()));
                    result.notify();
                }
            }

            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                System.out.println("failed: "+error);
                FleaseProposerTest.fail(error.toString());
            }
        }, new LearnEventListener() {

            public void learnedEvent(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long me) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        },null,null);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetLease() throws Exception {

        proposer.openCell(TESTCELL, new ArrayList(),false);

        Thread.sleep(100);

        FleaseProposerCell cell = proposer.cells.get(TESTCELL);

        assertEquals(TESTCELL,cell.getMessageSent().getCellId());
        assertEquals(cfg.getIdentity(),cell.getMessageSent().getLeaseHolder());
        assertTrue(cell.getMessageSent().getLeaseTimeout() > TimeSync.getGlobalTime());


    }

    /*public void testHandoverLease() throws Exception {

        proposer.openCell(TESTCELL, new ArrayList(),false);


        Thread.sleep(100);

        FleaseProposerCell cell = proposer.cells.get(TESTCELL);

        assertEquals(TESTCELL,cell.getMessageSent().getCellId());
        assertEquals(cfg.getIdentity(),cell.getMessageSent().getLeaseHolder());
        assertTrue(cell.getMessageSent().getLeaseTimeout() > TimeSync.getGlobalTime());

        final ASCIIString HANDOVER = new ASCIIString("HANDOVER");

        proposer.handoverLease(TESTCELL, HANDOVER);

        Thread.sleep(100);

        cell = proposer.cells.get(TESTCELL);

        assertEquals(TESTCELL,cell.getMessageSent().getCellId());
        assertEquals(HANDOVER,cell.getMessageSent().getLeaseHolder());
        assertTrue(cell.getMessageSent().getLeaseTimeout() > TimeSync.getGlobalTime());


    }*/


//    public void testPriorProposal() throws Exception {
//
//        final ASCIIString otherId = new ASCIIString("YAGGAYAGGA");
//        final AtomicBoolean finished = new AtomicBoolean(false);
//
//        //initialize the acceptor
//        FleaseMessage tmp = new FleaseMessage(FleaseMessage.MsgType.MSG_ACCEPT);
//        tmp.setCellId(TESTCELL);
//        tmp.setProposalNo(new ProposalNumber(10, 123456));
//        tmp.setLeaseHolder(otherId);
//        tmp.setLeaseTimeout(TimeSync.getGlobalTime()-20000);
//        tmp.setSendTimestamp(TimeSync.getGlobalTime());
//        acceptor.processMessage(tmp);
//
//        tmp.setCellId(TESTCELL);
//        tmp.setProposalNo(new ProposalNumber(10, 123456));
//        tmp.setLeaseTimeout(TimeSync.getGlobalTime()+10000);
//        tmp.setSendTimestamp(TimeSync.getGlobalTime());
//        acceptor.processMessage(tmp);
//
//        proposer.openCell(TESTCELL, new ArrayList<InetSocketAddress>(),false);
//
//        System.out.println("start acquire lease...");
//        proposer.acquireLease(TESTCELL, new FleaseListener() {
//
//            public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms) {
//                System.out.println("got lease!");
//                assertEquals(TESTCELL,cellId);
//                assertEquals(otherId,leaseHolder);
//                finished.set(true);
//            }
//
//            public void proposalFailed(ASCIIString cellId, Throwable cause) {
//                System.out.println("failed!");
//                fail(cause.toString());
//            }
//        });
//
//
//    }
//
//
//    public void testPriorProposal2() throws Exception {
//
//        final ASCIIString otherId = new ASCIIString("YAGGAYAGGA");
//        final AtomicBoolean finished = new AtomicBoolean(false);
//
//        //initialize the acceptor
//        FleaseMessage tmp = new FleaseMessage(FleaseMessage.MsgType.MSG_LEARN);
//        tmp.setCellId(TESTCELL);
//        tmp.setProposalNo(new ProposalNumber(10, 123456));
//        tmp.setLeaseHolder(otherId);
//        tmp.setLeaseTimeout(TimeSync.getGlobalTime()+10000);
//        tmp.setSendTimestamp(TimeSync.getGlobalTime());
//        acceptor.processMessage(tmp);
//
//        tmp.setCellId(TESTCELL);
//        tmp.setProposalNo(new ProposalNumber(10, 123456));
//        tmp.setLeaseTimeout(TimeSync.getGlobalTime()+10000);
//        tmp.setSendTimestamp(TimeSync.getGlobalTime());
//        acceptor.processMessage(tmp);
//
//        proposer.openCell(TESTCELL, new ArrayList<InetSocketAddress>(),false);
//
//        System.out.println("start acquire lease...");
//        proposer.acquireLease(TESTCELL, new FleaseListener() {
//
//            public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms) {
//                System.out.println("got lease!");
//                assertEquals(TESTCELL,cellId);
//                assertEquals(otherId,leaseHolder);
//                finished.set(true);
//            }
//
//            public void proposalFailed(ASCIIString cellId, Throwable cause) {
//                System.out.println("failed!");
//                fail(cause.toString());
//            }
//        });
//
//
//    }
//
//    public void testValidLease() throws Exception {
//
//        final ASCIIString otherId = new ASCIIString("YAGGAYAGGA");
//        final AtomicBoolean finished = new AtomicBoolean(false);
//
//        //initialize the acceptor
//        FleaseMessage tmp = new FleaseMessage(FleaseMessage.MsgType.MSG_LEARN);
//        tmp.setCellId(TESTCELL);
//        tmp.setProposalNo(new ProposalNumber(10, 123456));
//        tmp.setLeaseHolder(otherId);
//        tmp.setLeaseTimeout(TimeSync.getGlobalTime()+10000);
//        tmp.setSendTimestamp(TimeSync.getGlobalTime());
//        acceptor.processMessage(tmp);
//
//        proposer.openCell(TESTCELL, new ArrayList<InetSocketAddress>(),false);
//
//        System.out.println("start acquire lease...");
//        proposer.acquireLease(TESTCELL, new FleaseListener() {
//
//            public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms) {
//                System.out.println("got lease!");
//                assertEquals(TESTCELL,cellId);
//                assertEquals(otherId,leaseHolder);
//                finished.set(true);
//            }
//
//            public void proposalFailed(ASCIIString cellId, Throwable cause) {
//                System.out.println("failed!");
//                fail(cause.toString());
//            }
//        });
//    }
//
//    public void testLeaseInGP() throws Exception {
//
//        System.out.println("TEST testLeaseInGP");
//
//        final ASCIIString otherId = new ASCIIString("YAGGAYAGGA");
//        final AtomicBoolean finished = new AtomicBoolean(false);
//
//        //initialize the acceptor
//        FleaseMessage tmp = new FleaseMessage(FleaseMessage.MsgType.MSG_LEARN);
//        tmp.setCellId(TESTCELL);
//        tmp.setProposalNo(new ProposalNumber(10, 123456));
//        tmp.setLeaseHolder(otherId);
//        tmp.setLeaseTimeout(TimeSync.getGlobalTime()+100);
//        tmp.setSendTimestamp(TimeSync.getGlobalTime());
//        acceptor.processMessage(tmp);
//
//        proposer.openCell(TESTCELL, new ArrayList<InetSocketAddress>(),false);
//
//        System.out.println("start acquire lease...");
//        proposer.acquireLease(TESTCELL, new FleaseListener() {
//
//            public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms) {
//                System.out.println("got lease!");
//                assertEquals(TESTCELL,cellId);
//                assertEquals(cfg.getIdentity(),leaseHolder);
//                finished.set(true);
//            }
//
//            public void proposalFailed(ASCIIString cellId, Throwable cause) {
//                System.out.println("failed!");
//                fail(cause.toString());
//            }
//        });
//
//
//    }

    

    

}
