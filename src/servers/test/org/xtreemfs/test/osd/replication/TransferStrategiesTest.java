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
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.replication.RandomStrategy;
import org.xtreemfs.osd.replication.SequentialStrategy;
import org.xtreemfs.osd.replication.TransferStrategy;
import org.xtreemfs.osd.replication.TransferStrategy.NextRequest;
import org.xtreemfs.osd.replication.TransferStrategy.TransferStrategyException;
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
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        String file = "1:1";
        cap = new Capability(fileID, 0, System.currentTimeMillis(), "", 0, "secretPassphrase");

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
            Replica r = new Replica(new org.xtreemfs.interfaces.StripingPolicy(
                    StripingPolicyType.STRIPING_POLICY_RAID0, stripeSize / 1024, osdset.size()), 0, osdset);
            replicas.add(r);
        }
        XLocSet locSet = new XLocSet(replicas, 1, Constants.REPL_UPDATE_PC_NONE, 0);
        // set the first replica as current replica
        XLocations locations = new XLocations(new XLocSet(replicas, 1, Constants.REPL_UPDATE_PC_NONE, 0),
                new ServiceUUID(locSet.getReplicas().get(0).getOsd_uuids().get(0)));
        localReplica = locations.getLocalReplica();
        return locations;
    }

    @Before
    public void setUp() throws Exception {
        this.strategy = new SequentialStrategy(fileID, xLoc, new ServiceAvailability());
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.TransferStrategy#addRequiredObject(long)} and
     * {@link org.xtreemfs.osd.replication.TransferStrategy#removeRequiredObject(long)} .
     */
    @Test
    public void testAddAndRemoveRequiredObject() {
        this.strategy.addObject(objectNo, false);
        assertTrue(this.strategy.removeObject(objectNo));
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.TransferStrategy#addPreferredObject(long)} and
     * {@link org.xtreemfs.osd.replication.TransferStrategy#removePreferredObject(long)} .
     */
    @Test
    public void testAddAndRemovePreferredObject() {
        this.strategy.addObject(objectNo, true);
        assertTrue(this.strategy.removeObject(objectNo));
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.TransferStrategy#getRequiredObjectsCount()} and
     * {@link org.xtreemfs.osd.replication.TransferStrategy#getPreferredObjectsCount()} .
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
     * Test method for {@link org.xtreemfs.osd.replication.SequentialStrategy#selectNext()}.
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

        int replicaForStripe1 = 0;
        int replicaForStripe2 = 0;
        int replicaForStripe3 = 0;

        try {
            // stripe 2 (preferred)
            this.strategy.selectNext();
            NextRequest next = this.strategy.getNext();
            assertEquals(1, next.objectNo);
            List<ServiceUUID> osds = xLoc.getOSDsForObject(next.objectNo, localReplica);
            assertEquals(osds.get(replicaForStripe2++ % osds.size()), next.osd);
            assertFalse(next.requestObjectList);

            // stripe 3 (preferred)
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(2, next.objectNo);
            osds = xLoc.getOSDsForObject(next.objectNo, localReplica);
            assertEquals(osds.get(replicaForStripe3++ % osds.size()), next.osd);
            assertFalse(next.requestObjectList);

            // stripe 1
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(0, next.objectNo);
            osds = xLoc.getOSDsForObject(next.objectNo, localReplica);
            assertEquals(osds.get(replicaForStripe1++ % osds.size()), next.osd);
            assertFalse(next.requestObjectList);

            // stripe 1
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(3, next.objectNo);
            osds = xLoc.getOSDsForObject(next.objectNo, localReplica);
            assertEquals(osds.get(replicaForStripe1++ % osds.size()), next.osd);
            assertFalse(next.requestObjectList);

            // stripe 2
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertEquals(4, next.objectNo);
            osds = xLoc.getOSDsForObject(next.objectNo, localReplica);
            assertEquals(osds.get(replicaForStripe2++ % osds.size()), next.osd);
            assertFalse(next.requestObjectList);

            // no more requests possible
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertNull(next);
        } catch (TransferStrategyException e) {
            fail();
        }
    }

    /**
     * Test method for {@link org.xtreemfs.osd.replication.RandomStrategy#selectNext()}.
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
                assertFalse(next.requestObjectList);
            }

            for (int i = 0; i < objectsToRequest.size(); i++)
                assertTrue(requestedObjects.contains(objectsToRequest.get(i)));

            // no more requests possible
            this.strategy.selectNext();
            next = this.strategy.getNext();
            assertNull(next);
        } catch (TransferStrategyException e) {
            fail();
        }
    }
}
