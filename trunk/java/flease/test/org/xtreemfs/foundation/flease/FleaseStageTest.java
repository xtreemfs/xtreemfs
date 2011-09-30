/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class FleaseStageTest extends TestCase {

    private final FleaseConfig cfg;
    
    public FleaseStageTest(String testName) {
        super(testName);

        Logging.start(Logging.LEVEL_DEBUG, Category.all);
        TimeSync.initializeLocal(10000, 50);

        cfg = new FleaseConfig(10000, 500, 500, new InetSocketAddress(12345), "localhost:12345",5);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of createTimer method, of class FleaseStage.
     */
    public void testOpenAndGetLease() throws Exception {
        final ASCIIString CELL_ID = new ASCIIString("testcell");

        final AtomicReference<Flease> result = new AtomicReference();

        FleaseStage fs = new FleaseStage(cfg, "/tmp/xtreemfs-test/", new FleaseMessageSenderInterface() {

            public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
                //ignore me
            }
        }, true, new FleaseViewChangeListenerInterface() {

            public void viewIdChangeEvent(ASCIIString cellId, int viewId) {
            }
        },new FleaseStatusListener() {

            public void statusChanged(ASCIIString cellId, Flease lease) {
                System.out.println("state change: "+cellId+" owner="+lease.getLeaseHolder());
                synchronized (result) {
                    result.set(new Flease(cellId, lease.getLeaseHolder(), lease.getLeaseTimeout_ms(),lease.getMasterEpochNumber()));
                    result.notify();
                }
            }

            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                FleaseStageTest.fail(error.toString());
            }
        }, null);

        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.EVENT_RESTART);
        msg.setCellId(CELL_ID);

        fs.start();
        fs.waitForStartup();

        fs.openCell(CELL_ID, new ArrayList(),false);
       
        synchronized(result) {
            if (result.get() == null)
                result.wait(1000);
            if (result.get() == null)
                fail("timeout!");
        }

        assertEquals(result.get().getLeaseHolder(),cfg.getIdentity());

        FleaseFuture f = fs.closeCell(CELL_ID, false);
        f.get();

        Thread.sleep(12000);

        result.set(null);

        fs.openCell(CELL_ID, new ArrayList(), false);

        synchronized(result) {
            if (result.get() == null)
                result.wait(1000);
            if (result.get() == null)
                fail("timeout!");
        }

        assertEquals(result.get().getLeaseHolder(),cfg.getIdentity());

        fs.shutdown();
        fs.waitForShutdown();

    }


    /**
     * Test of createTimer method, of class FleaseStage.
     */
    public void testGetState() throws Exception {
        FleaseStage fs = new FleaseStage(cfg, "/tmp/xtreemfs-test/", new FleaseMessageSenderInterface() {

            public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
                //ignore me
            }
        }, true, new FleaseViewChangeListenerInterface() {

            public void viewIdChangeEvent(ASCIIString cellId, int viewId) {
            }
        },new FleaseStatusListener() {

            public void statusChanged(ASCIIString cellId, Flease lease) {
            }

            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                FleaseStageTest.fail(error.toString());
            }
        }, null);

        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.EVENT_RESTART);
        msg.setCellId(new ASCIIString("testcell"));

        fs.start();
        fs.waitForStartup();

        FleaseFuture f = fs.openCell(new ASCIIString("testcell"), new ArrayList(), false);
        final AtomicBoolean done = new AtomicBoolean(false);
        f.get();
        
        Thread.sleep(100);


        Map<ASCIIString,FleaseMessage> m = fs.getLocalState();

        for (ASCIIString cellId : m.keySet()) {
            System.out.println("cell "+cellId+" "+m.get(cellId));
        }

        fs.shutdown();
        fs.waitForShutdown();

    }

    /**
     * Test of createTimer method, of class FleaseStage.
     */
    public void testCreateTimer() throws Exception {
        FleaseStage fs = new FleaseStage(cfg, "/tmp/xtreemfs-test/", new FleaseMessageSenderInterface() {

            public void sendMessage(FleaseMessage messages, InetSocketAddress recipient) {
                //ignore me
            }
        }, true, new FleaseViewChangeListenerInterface() {

            public void viewIdChangeEvent(ASCIIString cellId, int viewId) {
            }
        },new FleaseStatusListener() {

            public void statusChanged(ASCIIString cellId, Flease lease) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, null);

        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.EVENT_RESTART);
        msg.setCellId(new ASCIIString("testcell"));

        fs.start();
        fs.waitForStartup();
        fs.createTimer(msg, TimeSync.getLocalSystemTime()+20);
        Thread.sleep(100);
        fs.shutdown();
        fs.waitForShutdown();

    }

}
