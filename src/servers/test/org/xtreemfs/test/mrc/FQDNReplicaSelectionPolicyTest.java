/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.mrc;

import java.net.InetAddress;
import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.mrc.osdselection.DNSSelectionPolicy;
import org.xtreemfs.mrc.replication.FQDNReplicaSelectionPolicy;
import org.xtreemfs.test.TestEnvironment;

/**

 *
 * @author bjko
 */
public class FQDNReplicaSelectionPolicyTest extends TestCase {

    FQDNReplicaSelectionPolicy p;
    TestEnvironment env;

    public FQDNReplicaSelectionPolicyTest() {
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
        p = new FQDNReplicaSelectionPolicy();
        env = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.TIME_SYNC});
        env.start();
        UUIDResolver.start(null, 10000, 100000);
    }

    @After
    public void tearDown() {
        env.shutdown();
        UUIDResolver.shutdown();
    }

    public void testMatchingOSDs() throws Exception {

        ReplicaSet replicas = new ReplicaSet();

        StringSet osds;
        Replica r;
        
        osds = new StringSet();
        osds.add("osd1");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd1", "xtreemfs1.zib.de", 2222, false);

        osds = new StringSet();
        osds.add("osd2");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd2", "www.heise.de", 2222, false);

        osds = new StringSet();
        osds.add("osd3");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd3", "xtreemfs.zib.de", 2222, false);

        osds = new StringSet();
        osds.add("osd4");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd4", "csr-pc29.zib.de", 2222, false);

        osds = new StringSet();
        osds.add("osd5");
        r = new Replica(new StripingPolicy(), 0, osds);
        replicas.add(r);
        UUIDResolver.addTestMapping("osd5", "download.xtreemfs.com", 2222, false);

        InetAddress clientAddr = InetAddress.getByName("xtreemfs.zib.de");




        ReplicaSet sorted = p.getSortedReplicaList(replicas, clientAddr);

        assertEquals("osd3",sorted.get(0).getOsd_uuids().get(0));
        assertEquals("osd4",sorted.get(1).getOsd_uuids().get(0));
        assertEquals("osd1",sorted.get(2).getOsd_uuids().get(0));


        clientAddr = InetAddress.getByName("www.heise.de");

        sorted = p.getSortedReplicaList(replicas, clientAddr);

        assertEquals("osd2",sorted.get(0).getOsd_uuids().get(0));

    }
    
    public void testSortingOSDs() throws Exception {

        StringSet osds = new StringSet();
        osds.add("osd1");
        osds.add("osd2");
        osds.add("osd3");
        osds.add("osd4");
        osds.add("osd5");
        
        UUIDResolver.addTestMapping("osd1", "xtreemfs1.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd2", "www.heise.de", 2222, false);
        UUIDResolver.addTestMapping("osd3", "xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd4", "csr-pc29.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd5", "download.xtreemfs.com", 2222, false);

        InetAddress clientAddr = InetAddress.getByName("xtreemfs.zib.de");
        StringSet sorted = p.getSortedOSDList(osds, clientAddr);

        assertEquals("osd3",sorted.get(0));
        assertEquals("osd1",sorted.get(1));
        assertEquals("osd4",sorted.get(2));

        clientAddr = InetAddress.getByName("www.heise.de");
        sorted = p.getSortedOSDList(osds, clientAddr);

        assertEquals("osd2",sorted.get(0));

    }
    
    public static void main(String[] args) {
        TestRunner.run(FQDNReplicaSelectionPolicyTest.class);
    }

}