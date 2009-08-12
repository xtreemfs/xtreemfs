/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.mrc;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.mrc.replication.dcmap.DCMapReplicaSelectionPolicy;
import org.xtreemfs.test.TestEnvironment;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class DCMapReplicaSelectionPolicyTest {

    TestEnvironment env;

    public DCMapReplicaSelectionPolicyTest() {
        Logging.start(Logging.LEVEL_DEBUG);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        env = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.TIME_SYNC});
        env.start();
        UUIDResolver.start(null, 10000, 100000);
    }

    @After
    public void tearDown() {
        env.shutdown();
        UUIDResolver.shutdown();
    }

    
    @Test
    public void testDCMapConfig() throws Exception {

        Properties p = new Properties();
        p.setProperty("datacenters", "A,B,yagg-blupp");

        try {
            DCMapReplicaSelectionPolicy policy = new DCMapReplicaSelectionPolicy(p);
            fail();
        } catch (IllegalArgumentException ex) {
            //ok, invalid data center name
        }
        p.setProperty("datacenters", "A,B,C");
        p.setProperty("distance.A-B", "10");
        p.setProperty("distance.A-C", "100");
        p.setProperty("distance.B-C", "50");
        p.setProperty("A.addresses","192.168.1.1,192.168.2.0/24");
        p.setProperty("B.addresses","192.168.1.2,192.168.3.0/24");
        p.setProperty("C.addresses","192.168.1.3,192.168.4.0/24,192.168.10.10");
        DCMapReplicaSelectionPolicy policy = new DCMapReplicaSelectionPolicy(p);

        Inet4Address ip1 = (Inet4Address) InetAddress.getByName("192.168.1.1");
        assertEquals(0,policy.getMatchingDC(ip1));
        assertEquals(0,policy.getMatchingDC(ip1));

        ip1 = (Inet4Address) InetAddress.getByName("192.168.2.168");
        assertEquals(0,policy.getMatchingDC(ip1));

        ip1 = (Inet4Address) InetAddress.getByName("192.168.1.2");
        assertEquals(1,policy.getMatchingDC(ip1));

        ip1 = (Inet4Address) InetAddress.getByName("192.168.3.254");
        assertEquals(1,policy.getMatchingDC(ip1));

        ip1 = (Inet4Address) InetAddress.getByName("192.168.4.2");
        assertEquals(2,policy.getMatchingDC(ip1));
        assertEquals(2,policy.getMatchingDC(ip1));
        assertEquals(2,policy.getMatchingDC(ip1));


        ip1 = (Inet4Address) InetAddress.getByName("192.168.1.1");
        Inet4Address ip2 = (Inet4Address) InetAddress.getByName("192.168.1.2");
        Inet4Address ip3 = (Inet4Address) InetAddress.getByName("192.168.4.25");
        assertEquals(10,policy.getDistance(ip1,ip2));
        assertEquals(100,policy.getDistance(ip1,ip3));
        assertEquals(50,policy.getDistance(ip2,ip3));
        assertEquals(0,policy.getDistance(ip1,ip1));
        assertEquals(0,policy.getDistance(ip2,ip2));
        assertEquals(0,policy.getDistance(ip3,ip3));


    }

    public void testMatchingOSDs() throws Exception {

        Properties p = new Properties();
        p.setProperty("datacenters", "A,B,yagg-blupp");

        try {
            DCMapReplicaSelectionPolicy policy = new DCMapReplicaSelectionPolicy(p);
            fail();
        } catch (IllegalArgumentException ex) {
            //ok, invalid data center name
        }
        p.setProperty("datacenters", "A,B,C");
        p.setProperty("distance.A-B", "10");
        p.setProperty("distance.A-C", "100");
        p.setProperty("distance.B-C", "50");
        p.setProperty("A.addresses","192.168.1.1,192.168.2.0/24");
        p.setProperty("B.addresses","192.168.1.2,192.168.3.0/24");
        p.setProperty("C.addresses","192.168.1.3,192.168.4.0/24,192.168.10.10");
        DCMapReplicaSelectionPolicy policy = new DCMapReplicaSelectionPolicy(p);

        ReplicaSet replicas = new ReplicaSet();

        StringSet osds;
        Replica r;

        osds = new StringSet();
        osds.add("osd1");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd1", "192.168.2.10", 2222, false);

        osds = new StringSet();
        osds.add("osd2");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd2", "192.168.3.11", 2222, false);

        osds = new StringSet();
        osds.add("osd3");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd3", "192.168.4.100", 2222, false);

        osds = new StringSet();
        osds.add("osd4");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd4", "192.168.1.1", 2222, false);

        InetAddress clientAddr = InetAddress.getByName("192.168.2.100");




        ReplicaSet sorted = policy.getSortedReplicaList(replicas, clientAddr);

        assertEquals("osd1",sorted.get(0).getOsd_uuids().get(0));
        assertEquals("osd4",sorted.get(1).getOsd_uuids().get(0));
        assertEquals("osd2",sorted.get(2).getOsd_uuids().get(0));
        assertEquals("osd3",sorted.get(2).getOsd_uuids().get(0));

        clientAddr = InetAddress.getByName("192.168.3.100");
        sorted = policy.getSortedReplicaList(replicas, clientAddr);

        assertEquals("osd2",sorted.get(0).getOsd_uuids().get(0));
        assertEquals("osd1",sorted.get(1).getOsd_uuids().get(0));
        assertEquals("osd4",sorted.get(2).getOsd_uuids().get(0));
        assertEquals("osd3",sorted.get(2).getOsd_uuids().get(0));


    }

}