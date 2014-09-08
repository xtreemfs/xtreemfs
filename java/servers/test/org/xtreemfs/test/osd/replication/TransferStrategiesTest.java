/*
 * Copyright (c) 2008-2011 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd.replication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.transferStrategies.RandomStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialPrefetchingStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialStrategy;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.NextRequest;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * 18.12.2008
 * 
 * @author clorenz
 */
public class TransferStrategiesTest {
    @Rule
    public final TestRule       testLog    = TestHelper.testLog;

    private final static String fileID     = "1:1";
    private static XLocations   xLoc;

    // needed for dummy classes
    private final static int    stripeSize = 128 * 1024;        // byte

    private TransferStrategy    strategy;
    private final static int    osdNumber  = 12;
    private final static long   objectNo   = 2;
    private final static long   filesize   = 1024 * 1024 * 128; // byte

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        xLoc = createLocations(4, 3);
    }

    /*
     * copied from org.xtreemfs.test.osd.replication.ReplicationTest
     */
    private static XLocations createLocations(int numberOfReplicas, int numberOfStripedOSDs)
            throws InvalidXLocationsException {
        assert (numberOfReplicas * numberOfStripedOSDs <= osdNumber);

        List<Replica> rlist = new LinkedList();
        int port = 33640;
        for (int replica = 0; replica < numberOfReplicas; replica++) {
            List<String> osdset = new LinkedList();
            int startOSD = replica * numberOfStripedOSDs;
            for (int stripe = 0; stripe < numberOfStripedOSDs; stripe++) {
                // add available osds
                osdset.add(new ServiceUUID("UUID:localhost:" + (port++)).toString());
            }
            int flags;
            if (replica == 1)
                flags = ReplicationFlags.setReplicaIsComplete(0);
            else
                flags = ReplicationFlags.setPartialReplica(ReplicationFlags.setRandomStrategy(0));

            Replica r = Replica.newBuilder()
                    .setStripingPolicy(SetupUtils.getStripingPolicy(osdset.size(), stripeSize / 1024))
                    .setReplicationFlags(flags).addAllOsdUuids(osdset).build();
            rlist.add(r);

        }

        XLocSet locSet = XLocSet.newBuilder().setReadOnlyFileSize(1024 * 1024 * 100)
                .setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE).setVersion(1).addAllReplicas(rlist)
                .build();
        // set the first replica as current replica

        // set the first replica as current replica
        XLocations locations = new XLocations(locSet, new ServiceUUID(locSet.getReplicas(0).getOsdUuids(0)));

        return locations;
    }

    @Before
    public void setUp() throws Exception {
        this.strategy = new RandomStrategy(fileID, xLoc, new ServiceAvailability());
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#addRequiredObject(long)}
     * and {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#removeRequiredObject(long)} .
     */
    @Test
    public void testAddAndRemoveRequiredObject() {
        this.strategy.addObject(objectNo, false);
        assertTrue(this.strategy.removeObject(objectNo));
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#addPreferredObject(long)}
     * and {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#removePreferredObject(long)} .
     */
    @Test
    public void testAddAndRemovePreferredObject() {
        this.strategy.addObject(objectNo, true);
        assertTrue(this.strategy.removeObject(objectNo));
    }

    /**
     * Test method for
     * {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#getRequiredObjectsCount()} and
     * {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#getPreferredObjectsCount()} .
     */
    @Test
    public void testGetXXXObjectsCount() {
        this.strategy.addObject(1, false);
        this.strategy.addObject(2, false);
        this.strategy.addObject(3, false);
        this.strategy.addObject(4, false);
        this.strategy.addObject(4, false);
        this.strategy.addObject(5, true);
        this.strategy.addObject(3, true);

        assertEquals(5, this.strategy.getObjectsCount());
    }

    /**
     * No assert in this test possible due to information hiding. Check debug output for correctness. Test method for
     * {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#setOSDsObjectSet(set, osd)} .
     */
    @Test
    public void testSetOSDsObjectSet() {
        this.strategy.addObject(0, false);
        this.strategy.addObject(1, true);
        this.strategy.addObject(2, false);
        this.strategy.addObject(3, false);
        this.strategy.addObject(4, false);
        this.strategy.addObject(2, true);

        // replica 1
        ObjectSet set = fillObjectSet(0);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(0).getOSDs().get(0));
        set = fillObjectSet(1);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(0).getOSDs().get(1));
        set = fillObjectSet(2);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(0).getOSDs().get(2));
        // replica 2 (complete replica)
        set = fillObjectSet(0, 3, 6, 9);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(1).getOSDs().get(0));
        set = fillObjectSet(1, 4, 7, 10);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(1).getOSDs().get(1));
        set = fillObjectSet(2, 5, 8, 11);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(1).getOSDs().get(2));
        // replica 3
        set = fillObjectSet(9);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(2).getOSDs().get(0));
        set = fillObjectSet(10);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(2).getOSDs().get(1));
        set = fillObjectSet(11);
        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(2).getOSDs().get(2));

        try {
            while (strategy.getObjectsCount() > 0) {
                this.strategy.selectNext();
                NextRequest next = this.strategy.getNext();
                assert (next != null);
            }
        } catch (TransferStrategyException e) {
            fail();
        }
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.SequentialStrategy#selectNext()}.
     */
    @Test
    public void testSelectNextForSequentialTransfer() {
        this.strategy = new SequentialStrategy(fileID, xLoc, new ServiceAvailability());
        this.strategy.addObject(0, false);
        this.strategy.addObject(1, true);
        this.strategy.addObject(2, false);
        this.strategy.addObject(3, false);
        this.strategy.addObject(4, false);
        this.strategy.addObject(2, true);

        try {
            // stripe 2 (preferred)
            this.strategy.selectNext();
            NextRequest next = this.strategy.getNext();
            assertEquals(1, next.objectNo);
            assertEquals(xLoc.getReplica(1).getOSDForObject(next.objectNo), next.osd);
            assertFalse(next.attachObjectSet);

            // stripe 3 (preferred)
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(2, next.objectNo);
            assertEquals(xLoc.getReplica(1).getOSDForObject(next.objectNo), next.osd);
            assertFalse(next.attachObjectSet);

            // stripe 1
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(0, next.objectNo);
            assertEquals(xLoc.getReplica(1).getOSDForObject(next.objectNo), next.osd);
            assertFalse(next.attachObjectSet);

            // stripe 1
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(3, next.objectNo);
            assertEquals(xLoc.getReplica(1).getOSDForObject(next.objectNo), next.osd);
            assertFalse(next.attachObjectSet);

            // stripe 2
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(4, next.objectNo);
            assertEquals(xLoc.getReplica(1).getOSDForObject(next.objectNo), next.osd);
            assertFalse(next.attachObjectSet);

            // no more requests possible
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertNull(next);
        } catch (TransferStrategyException e) {
            fail(e.getLocalizedMessage());
        }
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.RandomStrategy#selectNext()}.
     */
    @Test
    public void testSelectNextForRandomTransfer() {
        this.strategy = new RandomStrategy(fileID, xLoc, new ServiceAvailability());

        ArrayList<Long> objectsToRequest = new ArrayList<Long>();
        objectsToRequest.add(Long.valueOf(1));
        objectsToRequest.add(Long.valueOf(2));
        objectsToRequest.add(Long.valueOf(3));
        objectsToRequest.add(Long.valueOf(4));

        for (int i = 0; i < objectsToRequest.size(); i++) {
            this.strategy.addObject(objectsToRequest.get(i), false);
        }
        this.strategy.addObject(2, true);

        ArrayList<Long> requestedObjects = new ArrayList<Long>();

        try {
            NextRequest next;
            for (int i = 0; i < objectsToRequest.size(); i++) {
                this.strategy.selectNext();
                next = this.strategy.getNext();
                requestedObjects.add(Long.valueOf(next.objectNo));
                boolean contained = false;
                for (org.xtreemfs.common.xloc.Replica r : xLoc.getReplicas()) {
                    if (r.getOSDs().contains(next.osd))
                        contained = true;
                }
                assertTrue(contained);
            }

            for (int i = 0; i < objectsToRequest.size(); i++)
                assertTrue(requestedObjects.contains(objectsToRequest.get(i)));

            // no more requests possible
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertNull(next);
        } catch (TransferStrategyException e) {
            fail(e.getLocalizedMessage());
        }
    }

    /**
     * Test method for
     * {@link org.xtreemfs.osd.replication.transferStrategies.SequentialPrefetchingStrategy#selectNext()}.
     */
    @Test
    public void testSelectNextForSequentialPrefetchingTransfer() {
        this.strategy = new SequentialPrefetchingStrategy(fileID, xLoc, new ServiceAvailability());
        this.strategy.addObject(0, true);
        this.strategy.addObject(60, true);
        this.strategy.addObject(72, true);

        // objects count
        assertEquals(3, strategy.getObjectsCount());

        try {
            // preferred objects
            this.strategy.selectNext();
            NextRequest next = this.strategy.getNext();
            assertEquals(0, next.objectNo);
            // check, if objects are added by prefetching
            int objectsToPrefetch = SequentialPrefetchingStrategy.DEFAULT_PREFETCHING_COUNT + 3;
            assertEquals(2 + objectsToPrefetch, strategy.getObjectsCount());

            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(60, next.objectNo);
            // check, if objects are added by prefetching
            // default + preferred objects - number 72 is preferred, so don't prefetch
            objectsToPrefetch += SequentialPrefetchingStrategy.DEFAULT_PREFETCHING_COUNT + 2 - 1;
            assertEquals(1 + objectsToPrefetch, strategy.getObjectsCount());

            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(72, next.objectNo);
            // check, if objects are added by prefetching
            objectsToPrefetch += SequentialPrefetchingStrategy.DEFAULT_PREFETCHING_COUNT + 1 - 8;
            assertEquals(0 + objectsToPrefetch, strategy.getObjectsCount());

            // prefetched objects
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(3, next.objectNo);

            // ... and more
        } catch (TransferStrategyException e) {
            fail(e.getLocalizedMessage());
        }
    }

    // /**
    // * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.RarestFirstStrategy#selectNext()}.
    // */
    // @Test
    // public void testSelectNextForRarestFirstTransfer() {
    // this.strategy = new RarestFirstStrategy(fileID, xLoc, new ServiceAvailability());
    //
    // // prepare objects to fetch
    // long[] objectsToRequest = { 0, 1, 2, 3, 4, 5, 6, 7, 8,9,10,11,12,13 };
    // this.strategy.addObject(0, false);
    // this.strategy.addObject(1, true);
    // this.strategy.addObject(2, true);
    // for(int i=3; i< objectsToRequest.length-1; i++)
    // this.strategy.addObject(i, false);
    //
    // // prepare object sets of OSDs
    // ObjectSet set = fillObjectSet(0, 2, 4);
    // this.strategy.setOSDsObjectSet(set, xLoc.getReplica(0).getOSDs().get(0));
    // set = fillObjectSet(1, 3);
    // this.strategy.setOSDsObjectSet(set, xLoc.getReplica(0).getOSDs().get(1));
    // set = fillObjectSet(0, 2, 10);
    // this.strategy.setOSDsObjectSet(set, xLoc.getReplica(2).getOSDs().get(0));
    // set = fillObjectSet(1, 7, 9);
    // this.strategy.setOSDsObjectSet(set, xLoc.getReplica(2).getOSDs().get(1));
    // set = fillObjectSet(0, 4, 6, 10, 12);
    // this.strategy.setOSDsObjectSet(set, xLoc.getReplica(3).getOSDs().get(0));
    // set = fillObjectSet(1, 7, 9, 11);
    // this.strategy.setOSDsObjectSet(set, xLoc.getReplica(3).getOSDs().get(1));
    //
    // // set complete replica set later
    // set = fillObjectSet(0, 2, 4, 6, 8, 10, 12);
    // this.strategy.setOSDsObjectSet(set, xLoc.getReplica(1).getOSDs().get(0)); // complete replica
    // set = fillObjectSet(1, 3, 5, 7, 9, 11, 13);
    // this.strategy.setOSDsObjectSet(set, xLoc.getReplica(1).getOSDs().get(1)); // complete replica
    //
    // ArrayList<Long> requestedObjects = new ArrayList<Long>();
    //
    // try {
    // NextRequest next;
    // for (int i = 0; i < objectsToRequest.length; i++) {
    // this.strategy.selectNext();
    // next = this.strategy.getNext();
    // requestedObjects.add(Long.valueOf(next.objectNo));
    // boolean contained = false;
    // for (org.xtreemfs.common.xloc.Replica r : xLoc.getReplicas()) {
    // if (r.getOSDs().contains(next.osd))
    // contained = true;
    // }
    // assertTrue(contained);
    // }
    //
    // // for (int i = 0; i < objectsToRequest.length; i++)
    // // assertTrue(requestedObjects.contains(objectsToRequest.get(i)));
    //
    // // no more requests possible
    // this.strategy.selectNext();
    // next = this.strategy.getNext();
    // assertNull(next);
    // } catch (TransferStrategyException e) {
    // fail(e.getLocalizedMessage());
    // }
    // }

    private ObjectSet fillObjectSet(long... objects) {
        ObjectSet set = new ObjectSet(objects.length);
        for (long object : objects)
            set.add(object);
        return set;
    }
}
