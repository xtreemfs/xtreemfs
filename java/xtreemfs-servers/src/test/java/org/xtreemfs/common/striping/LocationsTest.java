/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Eugenio Cesario,
 *               Zuse Institute Berlin, Consiglio Nazionale delle Ricerche
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.striping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestHelper;

/**
 * This class implements the tests for Locations
 * 
 * @author jmalo
 */
public class LocationsTest {
    @Rule
    public final TestRule    testLog = TestHelper.testLog;

    static List<ServiceUUID> osds = new ArrayList<ServiceUUID>();

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);

        osds.add(new ServiceUUID("http://127.0.0.1:65535"));
        osds.add(new ServiceUUID("http://192.168.0.1:65535"));
        osds.add(new ServiceUUID("http://172.16.0.1:65535"));
        osds.add(new ServiceUUID("http://10.0.0.1:65535"));
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLocalReplica() throws Exception {

        List<String> osdList = new ArrayList();
        for (ServiceUUID osd : osds) {
            osdList.add(osd.toString());
        }

        List<String> rep2List = new ArrayList();
        rep2List.add(osds.get(3).toString());

        Replica r1 = Replica.newBuilder().setReplicationFlags(0)
                .setStripingPolicy(SetupUtils.getStripingPolicy(4, 128)).addAllOsdUuids(osdList).build();
        Replica r2 = Replica.newBuilder().setReplicationFlags(0)
                .setStripingPolicy(SetupUtils.getStripingPolicy(4, 128)).addAllOsdUuids(rep2List).build();
        XLocSet xlocset = XLocSet.newBuilder().setReadOnlyFileSize(0).setVersion(1).addReplicas(r1).addReplicas(r2)
                .setReplicaUpdatePolicy("").build();// XLocSet(0, rset, "", 1);
        XLocations loc = new XLocations(xlocset, osds.get(1));

        // System.out.println(loc.getLocalReplica().toString());
        // System.out.println(loc.getReplica(0).toString());
        assertEquals(loc.getLocalReplica(), loc.getReplica(0));
        assertNotNull(loc.getLocalReplica().getStripingPolicy());

    }

    @Test
    public void testCorrectSetOfReplicationFlags() {
        List<String> osdList = new ArrayList();
        for (ServiceUUID osd : osds) {
            osdList.add(osd.toString());
        }
        StripingPolicy stripingPolicy = SetupUtils.getStripingPolicy(4, 128);
        org.xtreemfs.common.xloc.Replica r;
        int flags = 0;

        // set none
        Replica interfR = Replica.newBuilder().setStripingPolicy(stripingPolicy).setReplicationFlags(flags)
                .addAllOsdUuids(osdList).build();
        r = new org.xtreemfs.common.xloc.Replica(interfR, null);
        assertFalse(r.isComplete());
        assertTrue(r.isPartialReplica());
        assertFalse(ReplicationFlags.isRandomStrategy(r.getTransferStrategyFlags()));
        assertFalse(ReplicationFlags.isSequentialStrategy(r.getTransferStrategyFlags()));

        // set complete
        flags = ReplicationFlags.setReplicaIsComplete(0);
        interfR = Replica.newBuilder().setStripingPolicy(stripingPolicy).setReplicationFlags(flags)
                .addAllOsdUuids(osdList).build();
        r = new org.xtreemfs.common.xloc.Replica(interfR, null);
        assertTrue(r.isComplete());
        assertTrue(r.isPartialReplica());
        assertFalse(ReplicationFlags.isRandomStrategy(r.getTransferStrategyFlags()));
        assertFalse(ReplicationFlags.isSequentialStrategy(r.getTransferStrategyFlags()));

        // set partial replica and RandomStrategy
        flags = ReplicationFlags.setPartialReplica(ReplicationFlags.setSequentialPrefetchingStrategy(0));
        interfR = Replica.newBuilder().setStripingPolicy(stripingPolicy).setReplicationFlags(flags)
                .addAllOsdUuids(osdList).build();
        r = new org.xtreemfs.common.xloc.Replica(interfR, null);
        assertFalse(r.isComplete());
        assertTrue(r.isPartialReplica());
        assertFalse(ReplicationFlags.isRandomStrategy(r.getTransferStrategyFlags()));
        assertTrue(ReplicationFlags.isSequentialPrefetchingStrategy(r.getTransferStrategyFlags()));

        // set full replica and RandomStrategy
        flags = ReplicationFlags.setRandomStrategy(ReplicationFlags.setFullReplica(0));
        interfR = Replica.newBuilder().setStripingPolicy(stripingPolicy).setReplicationFlags(flags)
                .addAllOsdUuids(osdList).build();
        r = new org.xtreemfs.common.xloc.Replica(interfR, null);
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

}
