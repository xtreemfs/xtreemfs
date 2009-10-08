/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,
Barcelona Supercomputing Center - Centro Nacional de Supercomputacion and
Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Jan Stender (ZIB), Jesús Malo (BSC), Björn Kolbeck (ZIB),
 *          Eugenio Cesario (CNR)
 */
package org.xtreemfs.test.common.striping;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.junit.Test;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.test.SetupUtils;

/**
 * This class implements the tests for Locations
 * 
 * @author jmalo
 */
public class LocationsTest extends TestCase {

    List<ServiceUUID> osds = new ArrayList<ServiceUUID>();

    /**
     * Creates a new instance of LocationsTest
     */
    public LocationsTest(String testName) {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);

        osds.add(new ServiceUUID("http://127.0.0.1:65535"));
        osds.add(new ServiceUUID("http://192.168.0.1:65535"));
        osds.add(new ServiceUUID("http://172.16.0.1:65535"));
        osds.add(new ServiceUUID("http://10.0.0.1:65535"));
    }

    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
    }

    protected void tearDown() throws Exception {
    }

    public void testLocalReplica() throws Exception {


        StringSet osdList = new StringSet();
        for (ServiceUUID osd : osds) {
            osdList.add(osd.toString());
        }

        StringSet rep2List = new StringSet();
        rep2List.add(osds.get(3).toString());

        ReplicaSet rset = new ReplicaSet();
        rset.add(new Replica(new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 4), 0, osdList));
        rset.add(new Replica(new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 4), 0, rep2List));
        XLocSet xlocset = new XLocSet(rset, 1, "", 0);
        XLocations loc = new XLocations(xlocset, osds.get(1));

        System.out.println(loc.getLocalReplica().toString());
        System.out.println(loc.getReplica(0).toString());
        assertEquals(loc.getLocalReplica(), loc.getReplica(0));
        assertNotNull(loc.getLocalReplica().getStripingPolicy());

    }
    
    @Test
    public void testCorrectSetOfReplicationFlags() {
        StringSet osdList = new StringSet();
        for (ServiceUUID osd : osds) {
            osdList.add(osd.toString());
        }
        StripingPolicy stripingPolicy = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 4);
        org.xtreemfs.common.xloc.Replica r;
        int flags = 0;
        
        // set none
        r = new org.xtreemfs.common.xloc.Replica(new Replica(stripingPolicy, flags, osdList));
        assertFalse(r.isComplete());
        assertTrue(r.isPartialReplica());
        assertFalse(ReplicationFlags.isRandomStrategy(r.getTransferStrategyFlags()));
        assertFalse(ReplicationFlags.isSequentialStrategy(r.getTransferStrategyFlags()));

        // set complete
        flags = ReplicationFlags.setReplicaIsComplete(0);
        r = new org.xtreemfs.common.xloc.Replica(new Replica(stripingPolicy, flags, osdList));
        assertTrue(r.isComplete());
        assertTrue(r.isPartialReplica());
        assertFalse(ReplicationFlags.isRandomStrategy(r.getTransferStrategyFlags()));
        assertFalse(ReplicationFlags.isSequentialStrategy(r.getTransferStrategyFlags()));

        // set partial replica and RandomStrategy
        flags = ReplicationFlags.setPartialReplica(ReplicationFlags.setSequentialPrefetchingStrategy(0));
        r = new org.xtreemfs.common.xloc.Replica(new Replica(stripingPolicy, flags, osdList));
        assertFalse(r.isComplete());
        assertTrue(r.isPartialReplica());
        assertFalse(ReplicationFlags.isRandomStrategy(r.getTransferStrategyFlags()));
        assertTrue(ReplicationFlags.isSequentialPrefetchingStrategy(r.getTransferStrategyFlags()));

        // set full replica and RandomStrategy
        flags = ReplicationFlags.setRandomStrategy(ReplicationFlags.setFullReplica(0));
        r = new org.xtreemfs.common.xloc.Replica(new Replica(stripingPolicy, flags, osdList));
        assertFalse(r.isComplete());
        assertFalse(r.isPartialReplica());
        assertTrue(ReplicationFlags.isRandomStrategy(r.getTransferStrategyFlags()));
        assertFalse(ReplicationFlags.isSequentialStrategy(r.getTransferStrategyFlags()));

        // set full replica and RandomStrategy
        flags = ReplicationFlags.setFullReplica(ReplicationFlags.setRandomStrategy(0));
        assertTrue(ReplicationFlags.isFullReplica(flags));
        assertTrue(ReplicationFlags.isRandomStrategy(flags));
        flags = ReplicationFlags.setSequentialStrategy(flags);
        assertTrue(ReplicationFlags.isSequentialStrategy(flags));
        assertTrue(ReplicationFlags.isFullReplica(flags));
        
        // test correct set of strategies
        // random
        flags = ReplicationFlags.setRandomStrategy(0);
        assertTrue(ReplicationFlags.isRandomStrategy(flags));
        assertFalse(ReplicationFlags.isSequentialStrategy(flags));
        assertFalse(ReplicationFlags.isSequentialPrefetchingStrategy(flags));
        assertFalse(ReplicationFlags.isRarestFirstStrategy(flags));
        // sequential
        flags = ReplicationFlags.setSequentialStrategy(0);
        assertFalse(ReplicationFlags.isRandomStrategy(flags));
        assertTrue(ReplicationFlags.isSequentialStrategy(flags));
        assertFalse(ReplicationFlags.isSequentialPrefetchingStrategy(flags));
        assertFalse(ReplicationFlags.isRarestFirstStrategy(flags));
        // sequential prefetching
        flags = ReplicationFlags.setSequentialPrefetchingStrategy(0);
        assertFalse(ReplicationFlags.isRandomStrategy(flags));
        assertFalse(ReplicationFlags.isSequentialStrategy(flags));
        assertTrue(ReplicationFlags.isSequentialPrefetchingStrategy(flags));
        assertFalse(ReplicationFlags.isRarestFirstStrategy(flags));
        // rarest first
        flags = ReplicationFlags.setRarestFirstStrategy(0);
        assertFalse(ReplicationFlags.isRandomStrategy(flags));
        assertFalse(ReplicationFlags.isSequentialStrategy(flags));
        assertFalse(ReplicationFlags.isSequentialPrefetchingStrategy(flags));
        assertTrue(ReplicationFlags.isRarestFirstStrategy(flags));
    }

    public static void main(String[] args) {
        TestRunner.run(LocationsTest.class);
    }
}
