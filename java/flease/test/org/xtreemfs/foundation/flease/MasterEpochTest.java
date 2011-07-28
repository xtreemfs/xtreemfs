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
public class MasterEpochTest extends TestCase {

    private final FleaseConfig cfg;
    
    public MasterEpochTest(String testName) {
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
                MasterEpochTest.fail(error.toString());
            }
        }, new MasterEpochHandlerInterface() {

            long masterEpochNum = 0;

            @Override
            public void sendMasterEpoch(FleaseMessage response, Continuation callback) {
                System.out.println("sending: "+masterEpochNum);
                response.setMasterEpochNumber(masterEpochNum);
                callback.processingFinished();
            }

            @Override
            public void storeMasterEpoch(FleaseMessage request, Continuation callback) {
                masterEpochNum = request.getMasterEpochNumber();
                System.out.println("storing: "+masterEpochNum);
                callback.processingFinished();
            }
        });

        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.EVENT_RESTART);
        msg.setCellId(CELL_ID);

        fs.start();
        fs.waitForStartup();

        fs.openCell(CELL_ID, new ArrayList(),true);
       
        synchronized(result) {
            if (result.get() == null)
                result.wait(1000);
            if (result.get() == null)
                fail("timeout!");
        }

        assertEquals(result.get().getLeaseHolder(),cfg.getIdentity());
        assertEquals(result.get().getMasterEpochNumber(),1);

        FleaseFuture f = fs.closeCell(CELL_ID, false);
        f.get();

        Thread.sleep(12000);

        result.set(null);

        fs.openCell(CELL_ID, new ArrayList(), true);

        synchronized(result) {
            if (result.get() == null)
                result.wait(1000);
            if (result.get() == null)
                fail("timeout!");
        }

        assertEquals(result.get().getLeaseHolder(),cfg.getIdentity());
        assertEquals(result.get().getMasterEpochNumber(),2);

        fs.shutdown();
        fs.waitForShutdown();

    }


    /**
     * Test of createTimer method, of class FleaseStage.
     */
    public void testSimpleMasterEpochHandler() throws Exception {
        final ASCIIString CELL_ID = new ASCIIString("testcell");

        final AtomicReference<Flease> result = new AtomicReference();

        SimpleMasterEpochHandler meHandler = new SimpleMasterEpochHandler("/tmp/xtreemfs-test/");
        meHandler.start();
        meHandler.waitForStartup();

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
                MasterEpochTest.fail(error.toString());
            }
        }, meHandler);

        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.EVENT_RESTART);
        msg.setCellId(CELL_ID);

        fs.start();
        fs.waitForStartup();

        fs.openCell(CELL_ID, new ArrayList(),true);

        synchronized(result) {
            if (result.get() == null)
                result.wait(1000);
            if (result.get() == null)
                fail("timeout!");
        }

        assertEquals(result.get().getLeaseHolder(),cfg.getIdentity());
        assertEquals(result.get().getMasterEpochNumber(),1);

        FleaseFuture f = fs.closeCell(CELL_ID, false);
        f.get();

        fs.shutdown();
        fs.waitForShutdown();
        meHandler.shutdown();
        meHandler.waitForShutdown();

        Thread.sleep(12000);

        //restart
        meHandler = new SimpleMasterEpochHandler("/tmp/xtreemfs-test/");
        meHandler.start();
        meHandler.waitForStartup();

        fs = new FleaseStage(cfg, "/tmp/xtreemfs-test/", new FleaseMessageSenderInterface() {

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
                MasterEpochTest.fail(error.toString());
            }
        }, meHandler);

        fs.start();
        fs.waitForStartup();

        result.set(null);

        fs.openCell(CELL_ID, new ArrayList(), true);

        synchronized(result) {
            if (result.get() == null)
                result.wait(1000);
            if (result.get() == null)
                fail("timeout!");
        }

        assertEquals(result.get().getLeaseHolder(),cfg.getIdentity());
        assertEquals(result.get().getMasterEpochNumber(),2);

        fs.shutdown();
        fs.waitForShutdown();

    }


}
