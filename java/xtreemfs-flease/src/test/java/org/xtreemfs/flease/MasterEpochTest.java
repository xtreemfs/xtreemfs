/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.flease;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.flease.proposer.FleaseException;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.FSUtils;

/**
 *
 * @author bjko
 */
public class MasterEpochTest {

    private static FleaseConfig cfg;
    private static File         testDir;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        Logging.start(Logging.LEVEL_WARN, Category.all);
        TimeSync.initializeLocal(50);

        cfg = new FleaseConfig(10000, 500, 500, new InetSocketAddress(12345), "localhost:12345",5, true, 0, true);
        testDir = new File("/tmp/xtreemfs-test/");
    }

    @Before
    public void setUp() throws Exception {
        FSUtils.delTree(testDir);
        testDir.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test of createTimer method, of class FleaseStage.
     */
    @Test
    public void testOpenAndGetLease() throws Exception {
        final ASCIIString CELL_ID = new ASCIIString("testcell");

        final AtomicReference<Flease> result = new AtomicReference();

        FleaseStage fs = new FleaseStage(cfg, "/tmp/xtreemfs-test/", new FleaseMessageSenderInterface() {

            @Override
            public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
                //ignore me
            }
        }, true, new FleaseViewChangeListenerInterface() {

            @Override
            public void viewIdChangeEvent(ASCIIString cellId, int viewId, boolean onProposal) {
            }
        },new FleaseStatusListener() {

            @Override
            public void statusChanged(ASCIIString cellId, Flease lease) {
                // System.out.println("state change: "+cellId+" owner="+lease.getLeaseHolder());
                synchronized (result) {
                    result.set(new Flease(cellId, lease.getLeaseHolder(), lease.getLeaseTimeout_ms(),lease.getMasterEpochNumber()));
                    result.notify();
                }
            }

            @Override
            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                fail(error.toString());
            }
        }, new MasterEpochHandlerInterface() {

            long masterEpochNum = 0;

            @Override
            public void sendMasterEpoch(FleaseMessage response, Continuation callback) {
                // System.out.println("sending: "+masterEpochNum);
                response.setMasterEpochNumber(masterEpochNum);
                callback.processingFinished();
            }

            @Override
            public void storeMasterEpoch(FleaseMessage request, Continuation callback) {
                masterEpochNum = request.getMasterEpochNumber();
                // System.out.println("storing: "+masterEpochNum);
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
    @Test
    public void testSimpleMasterEpochHandler() throws Exception {
        final ASCIIString CELL_ID = new ASCIIString("testcell");

        final AtomicReference<Flease> result = new AtomicReference();

        SimpleMasterEpochHandler meHandler = new SimpleMasterEpochHandler("/tmp/xtreemfs-test/");
        meHandler.start();
        meHandler.waitForStartup();

        FleaseStage fs = new FleaseStage(cfg, "/tmp/xtreemfs-test/", new FleaseMessageSenderInterface() {

            @Override
            public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
                //ignore me
            }
        }, true, new FleaseViewChangeListenerInterface() {

            @Override
            public void viewIdChangeEvent(ASCIIString cellId, int viewId, boolean onProposal) {
            }
        },new FleaseStatusListener() {

            @Override
            public void statusChanged(ASCIIString cellId, Flease lease) {
                // System.out.println("state change: "+cellId+" owner="+lease.getLeaseHolder());
                synchronized (result) {
                    result.set(new Flease(cellId, lease.getLeaseHolder(), lease.getLeaseTimeout_ms(),lease.getMasterEpochNumber()));
                    result.notify();
                }
            }

            @Override
            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                fail(error.toString());
            }
        }, meHandler);

        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.EVENT_RESTART);
        msg.setCellId(CELL_ID);

        fs.start();
        fs.waitForStartup();

        fs.openCell(CELL_ID, new ArrayList(),true);

        synchronized(result) {
            if (result.get() == null)
                result.wait(120000);
            if (result.get() == null)
                fail("timeout!");
        }

        assertEquals(result.get().getLeaseHolder(),cfg.getIdentity());
        assertEquals(1, result.get().getMasterEpochNumber());

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

            @Override
            public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
                //ignore me
            }
        }, true, new FleaseViewChangeListenerInterface() {

            @Override
            public void viewIdChangeEvent(ASCIIString cellId, int viewId, boolean onProposal) {
            }
        },new FleaseStatusListener() {

            @Override
            public void statusChanged(ASCIIString cellId, Flease lease) {
                // System.out.println("state change: "+cellId+" owner="+lease.getLeaseHolder());
                synchronized (result) {
                    result.set(new Flease(cellId, lease.getLeaseHolder(), lease.getLeaseTimeout_ms(),lease.getMasterEpochNumber()));
                    result.notify();
                }
            }

            @Override
            public void leaseFailed(ASCIIString cellId, FleaseException error) {
                fail(error.toString());
            }
        }, meHandler);

        fs.start();
        fs.waitForStartup();

        result.set(null);

        fs.openCell(CELL_ID, new ArrayList(), true);

        synchronized(result) {
            if (result.get() == null)
                result.wait(120000);
            if (result.get() == null)
                fail("timeout!");
        }

        assertEquals(result.get().getLeaseHolder(),cfg.getIdentity());
        assertEquals(result.get().getMasterEpochNumber(),2);

        fs.shutdown();
        fs.waitForShutdown();

    }


}
