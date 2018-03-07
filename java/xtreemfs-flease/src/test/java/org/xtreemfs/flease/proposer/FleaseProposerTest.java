/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.flease.proposer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.flease.Flease;
import org.xtreemfs.flease.FleaseConfig;
import org.xtreemfs.flease.FleaseStatusListener;
import org.xtreemfs.flease.acceptor.FleaseAcceptor;
import org.xtreemfs.flease.acceptor.LearnEventListener;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.flease.comm.FleaseCommunicationInterface;
import org.xtreemfs.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.FSUtils;

/**
 *
 * @author bjko
 */
public class FleaseProposerTest {

    private FleaseAcceptor acceptor;
    private FleaseProposer                 proposer;
    private final static ASCIIString       TESTCELL = new ASCIIString("testcell");

    private static FleaseConfig cfg;
    private static File                    testDir;

    private static AtomicReference<Flease> result;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        testDir = new File("/tmp/xtreemfs-test/");
        FSUtils.delTree(testDir);
        testDir.mkdirs();

        result = new AtomicReference();
        Logging.start(Logging.LEVEL_WARN, Category.all);
        TimeSync.initializeLocal(50);

        cfg = new FleaseConfig(10000, 500, 500, new InetSocketAddress(12345), "localhost:12345",5);
    }

    @Before
    public void setUp() throws Exception {
        acceptor = new FleaseAcceptor(new LearnEventListener() {

            @Override
            public void learnedEvent(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long me) {
            }
        }, cfg, "/tmp/xtreemfs-test/", true);
        proposer = new FleaseProposer(cfg, acceptor, new FleaseCommunicationInterface() {

            @Override
            public void sendMessage(FleaseMessage msg, InetSocketAddress receiver) throws IOException {
            }

            @Override
            public void requestTimer(FleaseMessage msg, long timestamp) {
                if (msg.getMsgType() == FleaseMessage.MsgType.EVENT_RESTART) {
                    long wait = timestamp - TimeSync.getLocalSystemTime();
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(FleaseProposerTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        proposer.processMessage(msg);
                    } catch (Exception ex) {
                        fail(ex.toString());
                    }
                }
            }
        }, new FleaseStatusListener() {

            @Override
            public void statusChanged(ASCIIString cellId, Flease lease) {
                // System.out.println("result: "+lease.getLeaseHolder());
                synchronized (result) {
                    result.set(new Flease(cellId, lease.getLeaseHolder(), lease.getLeaseTimeout_ms(),lease.getMasterEpochNumber()));
                    result.notify();
                }
            }

            @Override
            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                // System.out.println("failed: "+error);
                fail(error.toString());
            }
        }, new LearnEventListener() {

            @Override
            public void learnedEvent(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long me) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        },null,null);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetLease() throws Exception {

        proposer.openCell(TESTCELL, new ArrayList(), false, 0);

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
