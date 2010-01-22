/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.test.osd.replication;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.SnapConfig;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.replication.transferStrategies.RandomStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialPrefetchingStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialStrategy;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.NextRequest;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;
import org.xtreemfs.test.SetupUtils;

/**
 * 
 * 18.12.2008
 * 
 * @author clorenz
 */
public class TransferStrategiesTest extends TestCase {
    private Capability                       cap;
    private String                           fileID;
    private XLocations                       xLoc;
    private org.xtreemfs.common.xloc.Replica localReplica;

    // needed for dummy classes
    private int                              stripeSize;

    private TransferStrategy                 strategy;
    private int                              osdNumber;
    private long                             objectNo;
    private long                             filesize;

    /**
     * @throws InvalidXLocationsException
     * 
     */
    public TransferStrategiesTest() throws InvalidXLocationsException {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        String file = "1:1";
        cap = new Capability(fileID, 0, 60, System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, "secretPassphrase");

        this.stripeSize = 128 * 1024; // byte
        osdNumber = 12;
        objectNo = 2;
        filesize = 1024 * 1024 * 128; // byte

        xLoc = createLocations(4, 3);
    }

    /*
     * copied from org.xtreemfs.test.osd.replication.ReplicationTest
     */
    private XLocations createLocations(int numberOfReplicas, int numberOfStripedOSDs)
            throws InvalidXLocationsException {
        assert (numberOfReplicas * numberOfStripedOSDs <= osdNumber);

        ReplicaSet replicas = new ReplicaSet();
        int port = 33640;
        for (int replica = 0; replica < numberOfReplicas; replica++) {
            StringSet osdset = new StringSet();
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

            Replica r = new Replica(
                    osdset, flags, new org.xtreemfs.interfaces.StripingPolicy(
                    StripingPolicyType.STRIPING_POLICY_RAID0, stripeSize / 1024, osdset.size()));
            replicas.add(r);
        }
        XLocSet locSet = new XLocSet(1024 * 1024 * 100, replicas, Constants.REPL_UPDATE_PC_NONE, 1);
        // set the first replica as current replica
        XLocations locations = new XLocations(locSet, new ServiceUUID(locSet.getReplicas().get(0)
                .getOsd_uuids().get(0)));
        localReplica = locations.getLocalReplica();
        return locations;
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        this.strategy = new RandomStrategy(fileID, xLoc, new ServiceAvailability());
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#addRequiredObject(long)} and
     * {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#removeRequiredObject(long)} .
     */
    @Test
    public void testAddAndRemoveRequiredObject() {
        this.strategy.addObject(objectNo, false);
        assertTrue(this.strategy.removeObject(objectNo));
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#addPreferredObject(long)} and
     * {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#removePreferredObject(long)} .
     */
    @Test
    public void testAddAndRemovePreferredObject() {
        this.strategy.addObject(objectNo, true);
        assertTrue(this.strategy.removeObject(objectNo));
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.TransferStrategy#getRequiredObjectsCount()} and
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
     * No assert in this test possible due to information hiding. Check debug output for correctness. Test
     * method for
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
     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.SequentialPrefetchingStrategy#selectNext()}.
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
    
//    /**
//     * Test method for {@link org.xtreemfs.osd.replication.transferStrategies.RarestFirstStrategy#selectNext()}.
//     */
//    @Test
//    public void testSelectNextForRarestFirstTransfer() {
//        this.strategy = new RarestFirstStrategy(fileID, xLoc, new ServiceAvailability());
//
//        // prepare objects to fetch
//        long[] objectsToRequest = { 0, 1, 2, 3, 4, 5, 6, 7, 8,9,10,11,12,13 };
//        this.strategy.addObject(0, false);
//        this.strategy.addObject(1, true);
//        this.strategy.addObject(2, true);
//        for(int i=3; i< objectsToRequest.length-1; i++)
//            this.strategy.addObject(i, false);
//
//        // prepare object sets of OSDs
//        ObjectSet set = fillObjectSet(0, 2, 4);
//        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(0).getOSDs().get(0));
//        set = fillObjectSet(1, 3);
//        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(0).getOSDs().get(1));
//        set = fillObjectSet(0, 2, 10);
//        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(2).getOSDs().get(0));
//        set = fillObjectSet(1, 7, 9);
//        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(2).getOSDs().get(1));
//        set = fillObjectSet(0, 4, 6, 10, 12);
//        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(3).getOSDs().get(0));
//        set = fillObjectSet(1, 7, 9, 11);
//        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(3).getOSDs().get(1));
//
//        // set complete replica set later
//        set = fillObjectSet(0, 2, 4, 6, 8, 10, 12);
//        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(1).getOSDs().get(0)); // complete replica
//        set = fillObjectSet(1, 3, 5, 7, 9, 11, 13);
//        this.strategy.setOSDsObjectSet(set, xLoc.getReplica(1).getOSDs().get(1)); // complete replica
//
//        ArrayList<Long> requestedObjects = new ArrayList<Long>();
//
//        try {
//            NextRequest next;
//            for (int i = 0; i < objectsToRequest.length; i++) {
//                this.strategy.selectNext();
//                next = this.strategy.getNext();
//                requestedObjects.add(Long.valueOf(next.objectNo));
//                boolean contained = false;
//                for (org.xtreemfs.common.xloc.Replica r : xLoc.getReplicas()) {
//                    if (r.getOSDs().contains(next.osd))
//                        contained = true;
//                }
//                assertTrue(contained);
//            }
//
////            for (int i = 0; i < objectsToRequest.length; i++)
////                assertTrue(requestedObjects.contains(objectsToRequest.get(i)));
//
//            // no more requests possible
//            this.strategy.selectNext();
//            next = this.strategy.getNext();
//            assertNull(next);
//        } catch (TransferStrategyException e) {
//            fail(e.getLocalizedMessage());
//        }
//    }

    private ObjectSet fillObjectSet(long...objects) {
        ObjectSet set = new ObjectSet(objects.length);
        for (long object : objects)
            set.add(object);
        return set;
    }
}
