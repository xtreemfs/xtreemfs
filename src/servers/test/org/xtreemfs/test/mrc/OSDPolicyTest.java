/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.mrc;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.mrc.osdselection.FilterDefaultPolicy;
import org.xtreemfs.mrc.osdselection.FilterFQDNPolicy;
import org.xtreemfs.mrc.osdselection.GroupDCMapPolicy;
import org.xtreemfs.mrc.osdselection.GroupFQDNPolicy;
import org.xtreemfs.mrc.osdselection.Inet4AddressMatcher;
import org.xtreemfs.mrc.osdselection.SortDCMapPolicy;
import org.xtreemfs.mrc.osdselection.SortFQDNPolicy;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;

/**
 * 
 * @author bjko
 */
public class OSDPolicyTest extends TestCase {
    
    private TestEnvironment testEnv;
    
    public OSDPolicyTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        testEnv = new TestEnvironment(Services.TIME_SYNC, Services.UUID_RESOLVER);
        testEnv.start();
    }
    
    @After
    public void tearDown() {
        testEnv.shutdown();
    }
    
    @Test
    public void testIPv4Matcher() throws Exception {
        
        Inet4Address ifa1 = (Inet4Address) InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168,
            (byte) 1, (byte) 125 });
        Inet4Address ifa2 = (Inet4Address) InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168,
            (byte) 1, (byte) 126 });
        Inet4Address ifa3 = (Inet4Address) InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168,
            (byte) 1, (byte) 254 });
        Inet4Address ifa4 = (Inet4Address) InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168,
            (byte) 10, (byte) 125 });
        Inet4Address ifa5 = (Inet4Address) InetAddress.getByAddress(new byte[] { (byte) 10, (byte) 0,
            (byte) 1, (byte) 125 });
        
        Inet4AddressMatcher m = new Inet4AddressMatcher(ifa1);
        assertTrue(m.matches(ifa1));
        assertFalse(m.matches(ifa2));
        assertFalse(m.matches(ifa3));
        assertFalse(m.matches(ifa4));
        assertFalse(m.matches(ifa5));
        
        m = new Inet4AddressMatcher(ifa1, 25);
        assertTrue(m.matches(ifa1));
        assertTrue(m.matches(ifa2));
        assertFalse(m.matches(ifa3));
        assertFalse(m.matches(ifa4));
        assertFalse(m.matches(ifa5));
        
        m = new Inet4AddressMatcher(ifa1, 24);
        assertTrue(m.matches(ifa1));
        assertTrue(m.matches(ifa2));
        assertTrue(m.matches(ifa3));
        assertFalse(m.matches(ifa4));
        assertFalse(m.matches(ifa5));
        
        m = new Inet4AddressMatcher(ifa1, 16);
        assertTrue(m.matches(ifa1));
        assertTrue(m.matches(ifa2));
        assertTrue(m.matches(ifa3));
        assertTrue(m.matches(ifa4));
        assertFalse(m.matches(ifa5));
        
        m = new Inet4AddressMatcher(ifa1, 1);
        assertTrue(m.matches(ifa1));
        assertTrue(m.matches(ifa2));
        assertTrue(m.matches(ifa3));
        assertTrue(m.matches(ifa4));
        assertFalse(m.matches(ifa5));
    }
    
    @Test
    public void testFilterDefaultPolicy() throws Exception {
        
        ServiceDataMap sdm1 = new ServiceDataMap();
        sdm1.put("free", "1000");
        ServiceDataMap sdm2 = new ServiceDataMap();
        sdm2.put("free", "5000");
        ServiceDataMap sdm3 = new ServiceDataMap();
        sdm3.put("free", "5000");
        
        ServiceSet services = new ServiceSet();
        services.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd1", 1, "osd1",
            (System.currentTimeMillis() - 5000) / 1000l, sdm1));
        services.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd2", 1, "osd2",
            (System.currentTimeMillis() - 5000) / 1000l, sdm2));
        services.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd3", 1, "osd3",
            System.currentTimeMillis() / 1000l, sdm3));
        
        FilterDefaultPolicy pol = new FilterDefaultPolicy();
        pol.setAttribute("offline_time_secs", "2");
        pol.setAttribute("free_capacity_bytes", "2000");
        ServiceSet filteredOSDs = pol.getOSDs((ServiceSet) services.clone());
        
        assertEquals(1, filteredOSDs.size());
        assertEquals("osd3", filteredOSDs.get(0).getUuid());
    }
    
    @Test
    public void testFilterFQDNPolicy() throws Exception {
        
        ServiceSet services = new ServiceSet();
        services.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd1", 1, "osd1",
            System.currentTimeMillis() / 1000 - 1000, new ServiceDataMap()));
        services.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd2", 1, "osd2",
            System.currentTimeMillis() / 1000 - 5000, new ServiceDataMap()));
        services.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd3", 1, "osd3",
            System.currentTimeMillis() / 1000 - 1000, new ServiceDataMap()));
        
        UUIDResolver.addTestMapping("osd1", "test.xyz.org", 32640, false);
        UUIDResolver.addTestMapping("osd2", "test2.xyz.org", 32640, false);
        UUIDResolver.addTestMapping("osd3", "bla.com", 32640, false);
        
        FilterFQDNPolicy pol = new FilterFQDNPolicy();
        pol.setAttribute("domains", "*.org");
        ServiceSet filteredOSDs = pol.getOSDs((ServiceSet) services.clone());
        
        assertEquals(2, filteredOSDs.size());
        assertEquals("osd1", filteredOSDs.get(0).getUuid());
        assertEquals("osd2", filteredOSDs.get(1).getUuid());
        
        pol.setAttribute("domains", "*.com");
        filteredOSDs = pol.getOSDs((ServiceSet) services.clone());
        
        assertEquals(1, filteredOSDs.size());
        assertEquals("osd3", filteredOSDs.get(0).getUuid());
        
        pol.setAttribute("domains", "*.com test*");
        filteredOSDs = pol.getOSDs((ServiceSet) services.clone());
        
        assertEquals(3, filteredOSDs.size());
        assertEquals("osd1", filteredOSDs.get(0).getUuid());
        assertEquals("osd2", filteredOSDs.get(1).getUuid());
        assertEquals("osd3", filteredOSDs.get(2).getUuid());
    }
    
    @Test
    public void testSortDCMapPolicy() throws Exception {
        
        Properties p = new Properties();
        p.setProperty("datacenters", "A,B,yagg-blupp");
        
        try {
            new SortDCMapPolicy(p);
            fail();
        } catch (IllegalArgumentException ex) {
            // ok, invalid data center name
        }
        
        p.setProperty("datacenters", "A,B,C");
        p.setProperty("distance.A-B", "10");
        p.setProperty("distance.A-C", "100");
        p.setProperty("distance.B-C", "50");
        p.setProperty("A.addresses", "192.168.1.1,192.168.2.0/24");
        p.setProperty("B.addresses", "192.168.1.2,192.168.3.0/24");
        p.setProperty("C.addresses", "192.168.1.3,192.168.4.0/24,192.168.10.10");
        SortDCMapPolicy policy = new SortDCMapPolicy(p);
        
        ServiceSet osds = new ServiceSet();
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd1", 1, "osd1", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd2", 1, "osd2", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd3", 1, "osd3", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd4", 1, "osd4", 0, new ServiceDataMap()));
        
        UUIDResolver.addTestMapping("osd1", "192.168.2.10", 2222, false);
        UUIDResolver.addTestMapping("osd2", "192.168.3.11", 2222, false);
        UUIDResolver.addTestMapping("osd3", "192.168.4.100", 2222, false);
        UUIDResolver.addTestMapping("osd4", "192.168.1.1", 2222, false);
        
        InetAddress clientAddr = InetAddress.getByName("192.168.2.100");
        ServiceSet sortedList = policy.getOSDs(osds, clientAddr, null, Integer.MAX_VALUE);
        assertEquals("osd1", sortedList.get(0).getUuid());
        assertEquals("osd4", sortedList.get(1).getUuid());
        assertEquals("osd2", sortedList.get(2).getUuid());
        assertEquals("osd3", sortedList.get(3).getUuid());
        
        clientAddr = InetAddress.getByName("192.168.3.100");
        sortedList = policy.getOSDs(osds, clientAddr, null, Integer.MAX_VALUE);
        assertEquals("osd2", sortedList.get(0).getUuid());
        assertEquals("osd1", sortedList.get(1).getUuid());
        assertEquals("osd4", sortedList.get(2).getUuid());
        assertEquals("osd3", sortedList.get(3).getUuid());
        
    }
    
    @Test
    public void testGroupDCMapPolicy() throws Exception {
        
        Properties p = new Properties();
        p.setProperty("datacenters", "A,B,C");
        p.setProperty("distance.A-B", "10");
        p.setProperty("distance.A-C", "100");
        p.setProperty("distance.B-C", "50");
        p.setProperty("A.addresses", "192.168.1.1,192.168.2.0/24");
        p.setProperty("B.addresses", "192.168.1.2,192.168.3.0/24");
        p.setProperty("C.addresses", "192.168.1.3,192.168.4.0/24,192.168.10.10");
        GroupDCMapPolicy policy = new GroupDCMapPolicy(p);
        
        ServiceSet osds = new ServiceSet();
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd1", 1, "osd1", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd2", 1, "osd2", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd3", 1, "osd3", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd4", 1, "osd4", 0, new ServiceDataMap()));
        
        UUIDResolver.addTestMapping("osd1", "192.168.2.10", 2222, false);
        UUIDResolver.addTestMapping("osd2", "192.168.3.11", 2222, false);
        UUIDResolver.addTestMapping("osd3", "192.168.4.100", 2222, false);
        UUIDResolver.addTestMapping("osd4", "192.168.1.1", 2222, false);
        
        // get two OSDs from one data center
        InetAddress clientAddr = InetAddress.getByName("192.168.2.100");
        ServiceSet sortedList = policy.getOSDs((ServiceSet) osds.clone(), clientAddr, null, 2);
        assertEquals(2, sortedList.size());
        assertEquals("osd1", sortedList.get(0).getUuid());
        assertEquals("osd4", sortedList.get(1).getUuid());
        
        // request too many OSDs
        clientAddr = InetAddress.getByName("192.168.2.100");
        sortedList = policy.getOSDs((ServiceSet) osds.clone(), clientAddr, null, 3);
        assertEquals(0, sortedList.size());
        
        clientAddr = InetAddress.getByName("192.168.3.100");
        sortedList = policy.getOSDs((ServiceSet) osds.clone(), clientAddr, null, 1);
        assertEquals(1, sortedList.size());
        assertEquals("osd2", sortedList.get(0).getUuid());
        
    }
    
    @Test
    public void testSortFQDNPolicy() throws Exception {
        
        SortFQDNPolicy policy = new SortFQDNPolicy();
        
        ServiceSet osds = new ServiceSet();
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd1", 1, "osd1", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd2", 1, "osd2", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd3", 1, "osd3", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd4", 1, "osd4", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd5", 1, "osd5", 0, new ServiceDataMap()));
        
        UUIDResolver.addTestMapping("osd1", "xtreemfs1.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd2", "www.heise.de", 2222, false);
        UUIDResolver.addTestMapping("osd3", "xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd4", "csr-pc29.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd5", "download.xtreemfs.com", 2222, false);
        
        InetAddress clientAddr = InetAddress.getByName("xtreemfs.zib.de");
        ServiceSet sortedList = policy
                .getOSDs((ServiceSet) osds.clone(), clientAddr, null, Integer.MAX_VALUE);
        assertEquals("osd3", sortedList.get(0).getUuid());
        assertEquals("osd1", sortedList.get(1).getUuid());
        assertEquals("osd4", sortedList.get(2).getUuid());
        assertEquals("osd2", sortedList.get(3).getUuid());
        assertEquals("osd5", sortedList.get(4).getUuid());
        
        clientAddr = InetAddress.getByName("www.heise.de");
        sortedList = policy.getOSDs((ServiceSet) osds.clone(), clientAddr, null, Integer.MAX_VALUE);
        
        assertEquals("osd2", sortedList.get(0).getUuid());
    }
    
    @Test
    public void testGroupFQDNPolicy() throws Exception {
        
        GroupFQDNPolicy policy = new GroupFQDNPolicy();
        
        ServiceSet osds = new ServiceSet();
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd1", 1, "osd1", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd2", 1, "osd2", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd3", 1, "osd3", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd4", 1, "osd4", 0, new ServiceDataMap()));
        osds.add(new Service(ServiceType.SERVICE_TYPE_OSD, "osd5", 1, "osd5", 0, new ServiceDataMap()));
        
        UUIDResolver.addTestMapping("osd1", "bla.xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd2", "www.heise.de", 2222, false);
        UUIDResolver.addTestMapping("osd3", "blub.xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd4", "csr-pc29.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd5", "download.xtreemfs.com", 2222, false);
        
        InetAddress clientAddr = InetAddress.getByName("xtreemfs.zib.de");
        ServiceSet sortedList = policy.getOSDs((ServiceSet) osds.clone(), clientAddr, null, 1);
        assertEquals(1, sortedList.size());
        assertEquals("osd1", sortedList.get(0).getUuid());
        
        sortedList = policy.getOSDs((ServiceSet) osds.clone(), clientAddr, null, 2);
        assertEquals(2, sortedList.size());
        assertEquals("osd1", sortedList.get(0).getUuid());
        assertEquals("osd3", sortedList.get(1).getUuid());
        
        sortedList = policy.getOSDs((ServiceSet) osds.clone(), clientAddr, null, 4);
        assertEquals(0, sortedList.size());
        
        clientAddr = InetAddress.getByName("www.heise.de");
        sortedList = policy.getOSDs((ServiceSet) osds.clone(), clientAddr, null, 1);
        assertEquals(1, sortedList.size());
        assertEquals("osd2", sortedList.get(0).getUuid());
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(OSDPolicyTest.class);
    }
    
}