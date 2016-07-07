/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.osdselection.FilterDefaultPolicy;
import org.xtreemfs.mrc.osdselection.FilterFQDNPolicy;
import org.xtreemfs.mrc.osdselection.GroupDCMapPolicy;
import org.xtreemfs.mrc.osdselection.GroupFQDNPolicy;
import org.xtreemfs.mrc.osdselection.Inet4AddressMatcher;
import org.xtreemfs.mrc.osdselection.SortDCMapPolicy;
import org.xtreemfs.mrc.osdselection.SortFQDNPolicy;
import org.xtreemfs.mrc.osdselection.SortHostRoundRobinPolicy;
import org.xtreemfs.mrc.osdselection.SortVivaldiPolicy;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * @author bjko
 */
public class OSDPolicyTest {
    @Rule
    public final TestRule   testLog = TestHelper.testLog;
    
    private TestEnvironment testEnv;
    
    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    @AfterClass
    public static void shutdownTest() throws Exception {
    }
    
    @Before
    public void setUp() throws Exception {
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
        
        ServiceDataMap.Builder sdm1 = ServiceDataMap.newBuilder();
        sdm1.addData(KeyValuePair.newBuilder().setKey("free").setValue("1000"));
        sdm1.addData(KeyValuePair.newBuilder().setKey("seconds_since_last_update").setValue("5"));
        
        ServiceDataMap.Builder sdm2 = ServiceDataMap.newBuilder();
        sdm2.addData(KeyValuePair.newBuilder().setKey("free").setValue("5000"));
        sdm2.addData(KeyValuePair.newBuilder().setKey("seconds_since_last_update").setValue("5"));
        
        ServiceDataMap.Builder sdm3 = ServiceDataMap.newBuilder();
        sdm3.addData(KeyValuePair.newBuilder().setKey("free").setValue("5000"));
        sdm3.addData(KeyValuePair.newBuilder().setKey("seconds_since_last_update").setValue("0"));
        sdm3.addData(KeyValuePair.newBuilder().setKey(HeartbeatThread.STATUS_ATTR)
                .setValue(String.valueOf(ServiceStatus.SERVICE_STATUS_AVAIL.getNumber())));
        
        // the following two shouldn't be selected because they are not available
        ServiceDataMap.Builder sdm4 = ServiceDataMap.newBuilder();
        sdm4.addData(KeyValuePair.newBuilder().setKey("free").setValue("5000"));
        sdm4.addData(KeyValuePair.newBuilder().setKey("seconds_since_last_update").setValue("0"));
        sdm4.addData(KeyValuePair.newBuilder().setKey(HeartbeatThread.STATUS_ATTR)
                .setValue(String.valueOf(ServiceStatus.SERVICE_STATUS_TO_BE_REMOVED.getNumber())));
        
        ServiceDataMap.Builder sdm5 = ServiceDataMap.newBuilder();
        sdm5.addData(KeyValuePair.newBuilder().setKey("free").setValue("5000"));
        sdm5.addData(KeyValuePair.newBuilder().setKey("seconds_since_last_update").setValue("0"));
        sdm5.addData(KeyValuePair.newBuilder().setKey(HeartbeatThread.STATUS_ATTR)
                .setValue(String.valueOf(ServiceStatus.SERVICE_STATUS_REMOVED.getNumber())));
        
        ServiceSet.Builder servicesBuilder = ServiceSet.newBuilder();
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(0).setName("osd1").setVersion(1).setUuid("osd1").setData(sdm1));
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(0).setName("osd2").setVersion(1).setUuid("osd2").setData(sdm2));
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(0).setName("osd3").setVersion(1).setUuid("osd3").setData(sdm3));
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(0).setName("osd4").setVersion(1).setUuid("osd4").setData(sdm4));
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(0).setName("osd5").setVersion(1).setUuid("osd5").setData(sdm5));
        ServiceSet services = servicesBuilder.build();
        
        
        FilterDefaultPolicy pol = new FilterDefaultPolicy();
        pol.setAttribute("offline_time_secs", "2");
        pol.setAttribute("free_capacity_bytes", "2000");
        ServiceSet.Builder filteredOSDs = pol.getOSDs(services.toBuilder());
        
        assertEquals(1, filteredOSDs.getServicesCount());
        assertEquals("osd3", filteredOSDs.getServices(0).getUuid());
    }
    
    @Test
    public void testFilterDefaultPolicyOnCustomProperty() throws Exception {
        
        ServiceDataMap.Builder sdm1 = ServiceDataMap.newBuilder();
        sdm1.addData(KeyValuePair.newBuilder().setKey("free").setValue("10000"));
        sdm1.addData(KeyValuePair.newBuilder().setKey("seconds_since_last_update").setValue("0"));
        sdm1.addData(KeyValuePair.newBuilder().setKey(HeartbeatThread.STATUS_ATTR)
                .setValue(String.valueOf(ServiceStatus.SERVICE_STATUS_AVAIL.getNumber())));
        sdm1.addData(KeyValuePair.newBuilder().setKey(ServiceConfig.OSD_CUSTOM_PROPERTY_PREFIX+"country")
                .setValue(String.valueOf("FR")));
        
        ServiceDataMap.Builder sdm2 = ServiceDataMap.newBuilder();
        sdm2.addData(KeyValuePair.newBuilder().setKey("free").setValue("10000"));
        sdm2.addData(KeyValuePair.newBuilder().setKey("seconds_since_last_update").setValue("0"));
        sdm2.addData(KeyValuePair.newBuilder().setKey(HeartbeatThread.STATUS_ATTR)
                .setValue(String.valueOf(ServiceStatus.SERVICE_STATUS_AVAIL.getNumber())));

        sdm2.addData(KeyValuePair.newBuilder().setKey(ServiceConfig.OSD_CUSTOM_PROPERTY_PREFIX+"country")
                .setValue(String.valueOf("DE")));

        
        ServiceDataMap.Builder sdm3 = ServiceDataMap.newBuilder();
        sdm3.addData(KeyValuePair.newBuilder().setKey("free").setValue("10000"));
        sdm3.addData(KeyValuePair.newBuilder().setKey("seconds_since_last_update").setValue("0"));
        sdm3.addData(KeyValuePair.newBuilder().setKey(HeartbeatThread.STATUS_ATTR)
                .setValue(String.valueOf(ServiceStatus.SERVICE_STATUS_AVAIL.getNumber())));
        sdm3.addData(KeyValuePair.newBuilder().setKey(ServiceConfig.OSD_CUSTOM_PROPERTY_PREFIX+"country")
                .setValue(String.valueOf("GB")));
        
        ServiceSet.Builder servicesBuilder = ServiceSet.newBuilder();
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(0).setName("osd1").setVersion(1).setUuid("osd1").setData(sdm1));
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(0).setName("osd2").setVersion(1).setUuid("osd2").setData(sdm2));
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(0).setName("osd3").setVersion(1).setUuid("osd3").setData(sdm3));
        ServiceSet services = servicesBuilder.build();
        
        // all OSDs but from FR
        FilterDefaultPolicy pol = new FilterDefaultPolicy();
        pol.setAttribute("not.country", "FR");
        ServiceSet.Builder filteredOSDs = pol.getOSDs(services.toBuilder());        
        
        assertEquals(2, filteredOSDs.getServicesCount());

        
        // only OSDs from DE and NOT from FR
        pol = new FilterDefaultPolicy();
        pol.setAttribute("country", "DE");
        pol.setAttribute("not.country", "FR");
        filteredOSDs = pol.getOSDs(services.toBuilder());
        
        assertEquals(1, filteredOSDs.getServicesCount());
        String osdParameterValue 
        = KeyValuePairs.getValue(filteredOSDs.getServices(0).getData().getDataList(),
            ServiceConfig.OSD_CUSTOM_PROPERTY_PREFIX + "country");
        assertEquals("DE", osdParameterValue);

        // only OSDs not from FR and not from DE
        pol = new FilterDefaultPolicy();
        pol.setAttribute("not.country", "FR DE");
        filteredOSDs = pol.getOSDs(services.toBuilder());
        
        assertEquals(1, filteredOSDs.getServicesCount());        
        osdParameterValue 
        = KeyValuePairs.getValue(filteredOSDs.getServices(0).getData().getDataList(),
            ServiceConfig.OSD_CUSTOM_PROPERTY_PREFIX + "country");
        assertEquals("GB", osdParameterValue);

    }
    
    @Test
    public void testFilterFQDNPolicy() throws Exception {
        
        ServiceSet.Builder servicesBuilder = ServiceSet.newBuilder();
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(System.currentTimeMillis() / 1000 - 1000).setName("osd1").setVersion(1)
                .setUuid("osd1").setData(getDefaultServiceDataMap()));
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(System.currentTimeMillis() / 1000 - 5000).setName("osd2").setVersion(1)
                .setUuid("osd2").setData(getDefaultServiceDataMap()));
        servicesBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(System.currentTimeMillis() / 1000 - 1000).setName("osd3").setVersion(1)
                .setUuid("osd3").setData(getDefaultServiceDataMap()));
        ServiceSet services = servicesBuilder.build();
        
        UUIDResolver.addTestMapping("osd1", "test.xyz.org", 32640, false);
        UUIDResolver.addTestMapping("osd2", "test2.xyz.org", 32640, false);
        UUIDResolver.addTestMapping("osd3", "bla.com", 32640, false);
        
        FilterFQDNPolicy pol = new FilterFQDNPolicy();
        pol.setAttribute("domains", "*.org");
        ServiceSet.Builder filteredOSDs = pol.getOSDs(services.toBuilder());
        
        assertEquals(2, filteredOSDs.getServicesCount());
        assertEquals("osd1", filteredOSDs.getServices(0).getUuid());
        assertEquals("osd2", filteredOSDs.getServices(1).getUuid());
        
        pol.setAttribute("domains", "*.com");
        filteredOSDs = pol.getOSDs(services.toBuilder());
        
        assertEquals(1, filteredOSDs.getServicesCount());
        assertEquals("osd3", filteredOSDs.getServices(0).getUuid());
        
        pol.setAttribute("domains", "*.com test*");
        filteredOSDs = pol.getOSDs(services.toBuilder());
        
        assertEquals(3, filteredOSDs.getServicesCount());
        assertEquals("osd1", filteredOSDs.getServices(0).getUuid());
        assertEquals("osd2", filteredOSDs.getServices(1).getUuid());
        assertEquals("osd3", filteredOSDs.getServices(2).getUuid());
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
        
        ServiceSet.Builder osdsBuilder = ServiceSet.newBuilder();
        osdsBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd1").setVersion(1).setUuid("osd1").setData(getDefaultServiceDataMap()));
        osdsBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd2").setVersion(1).setUuid("osd2").setData(getDefaultServiceDataMap()));
        osdsBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd3").setVersion(1).setUuid("osd3").setData(getDefaultServiceDataMap()));
        osdsBuilder.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd4").setVersion(1).setUuid("osd4").setData(getDefaultServiceDataMap()));
        ServiceSet osds = osdsBuilder.build();
        
        UUIDResolver.addTestMapping("osd1", "192.168.2.10", 2222, false);
        UUIDResolver.addTestMapping("osd2", "192.168.3.11", 2222, false);
        UUIDResolver.addTestMapping("osd3", "192.168.4.100", 2222, false);
        UUIDResolver.addTestMapping("osd4", "192.168.1.1", 2222, false);
        
        InetAddress clientAddr = InetAddress.getByName("192.168.2.100");
        ServiceSet.Builder sortedList = policy.getOSDs(osds.toBuilder(), clientAddr, null, null,
            Integer.MAX_VALUE);
        assertEquals("osd1", sortedList.getServices(0).getUuid());
        assertEquals("osd4", sortedList.getServices(1).getUuid());
        assertEquals("osd2", sortedList.getServices(2).getUuid());
        assertEquals("osd3", sortedList.getServices(3).getUuid());
        
        clientAddr = InetAddress.getByName("192.168.3.100");
        sortedList = policy.getOSDs(osds.toBuilder(), clientAddr, null, null, Integer.MAX_VALUE);
        assertEquals("osd2", sortedList.getServices(0).getUuid());
        assertEquals("osd1", sortedList.getServices(1).getUuid());
        assertEquals("osd4", sortedList.getServices(2).getUuid());
        assertEquals("osd3", sortedList.getServices(3).getUuid());
        
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
        
        ServiceSet.Builder osds = ServiceSet.newBuilder();
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd1").setVersion(1).setUuid("osd1").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd2").setVersion(1).setUuid("osd2").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd3").setVersion(1).setUuid("osd3").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd4").setVersion(1).setUuid("osd4").setData(getDefaultServiceDataMap()));
        
        UUIDResolver.addTestMapping("osd1", "192.168.2.10", 2222, false);
        UUIDResolver.addTestMapping("osd2", "192.168.3.11", 2222, false);
        UUIDResolver.addTestMapping("osd3", "192.168.4.100", 2222, false);
        UUIDResolver.addTestMapping("osd4", "192.168.1.1", 2222, false);
        
        // get two OSDs from one data center
        InetAddress clientAddr = InetAddress.getByName("192.168.2.100");
        ServiceSet.Builder sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(
            osds.getServicesList()), clientAddr, null, null, 2);
        assertEquals(2, sortedList.getServicesCount());
        assertEquals("osd1", sortedList.getServices(0).getUuid());
        assertEquals("osd4", sortedList.getServices(1).getUuid());
        
        // request too many OSDs
        clientAddr = InetAddress.getByName("192.168.2.100");
        sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(osds.getServicesList()),
            clientAddr, null, null, 3);
        assertEquals(0, sortedList.getServicesCount());
        
        clientAddr = InetAddress.getByName("192.168.3.100");
        sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(osds.getServicesList()),
            clientAddr, null, null, 1);
        assertEquals(1, sortedList.getServicesCount());
        assertEquals("osd2", sortedList.getServices(0).getUuid());
        
    }
    
    @Test
    public void testSortFQDNPolicy() throws Exception {
        
        SortFQDNPolicy policy = new SortFQDNPolicy();
        
        ServiceSet.Builder osds = ServiceSet.newBuilder();
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd1").setVersion(1).setUuid("osd1").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd2").setVersion(1).setUuid("osd2").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd3").setVersion(1).setUuid("osd3").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd4").setVersion(1).setUuid("osd4").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd5").setVersion(1).setUuid("osd5").setData(getDefaultServiceDataMap()));
        
        UUIDResolver.addTestMapping("osd1", "xtreemfs1.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd2", "www.berlin.de", 2222, false);
        UUIDResolver.addTestMapping("osd3", "xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd4", "csr-pc29.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd5", "download.xtreemfs.com", 2222, false);
        
        InetAddress clientAddr = InetAddress.getByName("xtreem.zib.de");
        ServiceSet.Builder sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(
            osds.getServicesList()), clientAddr, null, null, Integer.MAX_VALUE);
        assertEquals("osd1", sortedList.getServices(0).getUuid());
        assertEquals("osd3", sortedList.getServices(1).getUuid());
        assertEquals("osd4", sortedList.getServices(2).getUuid());
        assertEquals("osd2", sortedList.getServices(3).getUuid());
        assertEquals("osd5", sortedList.getServices(4).getUuid());
        
        clientAddr = InetAddress.getByName("www.berlin.de");
        sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(osds.getServicesList()),
            clientAddr, null, null, Integer.MAX_VALUE);
        
        assertEquals("osd2", sortedList.getServices(0).getUuid());
    }
    
    @Test
    public void testGroupFQDNPolicy() throws Exception {
        
        GroupFQDNPolicy policy = new GroupFQDNPolicy();
        
        ServiceSet.Builder osds = ServiceSet.newBuilder();
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd1").setVersion(1).setUuid("osd1").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd2").setVersion(1).setUuid("osd2").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd3").setVersion(1).setUuid("osd3").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd4").setVersion(1).setUuid("osd4").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setName("osd5").setVersion(1).setUuid("osd5").setData(getDefaultServiceDataMap()));
        
        UUIDResolver.addTestMapping("osd1", "bla.xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd2", "www.berlin.de", 2222, false);
        UUIDResolver.addTestMapping("osd3", "blub.xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd4", "csr-pc29.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd5", "download.xtreemfs.com", 2222, false);
        
        InetAddress clientAddr = InetAddress.getByName("xtreem.zib.de");
        ServiceSet.Builder sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(
            osds.getServicesList()), clientAddr, null, null, 1);
        assertEquals(1, sortedList.getServicesCount());
        assertEquals("osd1", sortedList.getServices(0).getUuid());
        
        sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(osds.getServicesList()),
            clientAddr, null, null, 2);
        assertEquals(2, sortedList.getServicesCount());
        assertEquals("osd1", sortedList.getServices(0).getUuid());
        assertEquals("osd3", sortedList.getServices(1).getUuid());
        
        sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(osds.getServicesList()),
            clientAddr, null, null, 4);
        assertEquals(0, sortedList.getServicesCount());
        
        clientAddr = InetAddress.getByName("www.berlin.de");
        sortedList = policy.getOSDs(ServiceSet.newBuilder().addAllServices(osds.getServicesList()),
            clientAddr, null, null, 1);
        assertEquals(1, sortedList.getServicesCount());
        assertEquals("osd2", sortedList.getServices(0).getUuid());
        
    }
    
    @Test
    public void testSortVivaldiPolicy() throws Exception {
        
        SortVivaldiPolicy policy = new SortVivaldiPolicy();
        
        // VivaldiCoordinates coords1 = new VivaldiCoordinates(1.0,1.0,0.1);
        VivaldiCoordinates.Builder coords2 = VivaldiCoordinates.newBuilder().setXCoordinate(5.0).setYCoordinate(5.0)
                .setLocalError(0.1);
        VivaldiCoordinates.Builder coords3 = VivaldiCoordinates.newBuilder().setXCoordinate(2.0).setYCoordinate(2.0)
                .setLocalError(0.1);
        VivaldiCoordinates.Builder coords4 = VivaldiCoordinates.newBuilder().setXCoordinate(20.0)
                .setYCoordinate(20.0).setLocalError(0.1);
        VivaldiCoordinates.Builder coords5 = VivaldiCoordinates.newBuilder().setXCoordinate(10.0)
                .setYCoordinate(10.0).setLocalError(0.1);
        
        ServiceSet.Builder osds = ServiceSet.newBuilder();
        // sdm.put("vivaldi_coordinates",
        // VivaldiNode.coordinatesToString(coords1));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setVersion(1).setUuid("osd1").setName("osd1").setData(ServiceDataMap.newBuilder()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
            .setVersion(1).setUuid("osd1").setName("osd1").setData(ServiceDataMap.newBuilder()));
        
        ServiceDataMap.Builder sdm = ServiceDataMap.newBuilder();
        sdm.addData(KeyValuePair.newBuilder().setKey("vivaldi_coordinates").setValue(
            VivaldiNode.coordinatesToString(coords2.build())));
        Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0).setVersion(1).setUuid(
            "osd2").setName("osd2").setData(sdm.clone());
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
            .setVersion(1).setUuid("osd2").setName("osd2").setData(sdm));
        
        sdm = ServiceDataMap.newBuilder();
        sdm.addData(KeyValuePair.newBuilder().setKey("vivaldi_coordinates").setValue(
            VivaldiNode.coordinatesToString(coords3.build())));
        Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0).setVersion(1).setUuid(
            "osd3").setName("osd3").setData(sdm.clone());
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
            .setVersion(1).setUuid("osd3").setName("osd3").setData(sdm));
        
        sdm = ServiceDataMap.newBuilder();
        sdm.addData(KeyValuePair.newBuilder().setKey("vivaldi_coordinates").setValue(
            VivaldiNode.coordinatesToString(coords4.build())));
        Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0).setVersion(1).setUuid(
            "osd4").setName("osd4").setData(sdm.clone());
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
            .setVersion(1).setUuid("osd4").setName("osd4").setData(sdm));
        
        sdm = ServiceDataMap.newBuilder();
        sdm.addData(KeyValuePair.newBuilder().setKey("vivaldi_coordinates").setValue(
            VivaldiNode.coordinatesToString(coords5.build())));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setLastUpdatedS(0)
                .setVersion(1).setUuid("osd5").setName("osd5").setData(sdm));
        
        UUIDResolver.addTestMapping("osd1", "bla.xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd2", "www.berlin.de", 2222, false);
        UUIDResolver.addTestMapping("osd3", "blub.xtreemfs.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd4", "csr-pc29.zib.de", 2222, false);
        UUIDResolver.addTestMapping("osd5", "download.xtreemfs.com", 2222, false);
        
        InetAddress clientAddr = InetAddress.getByName("xtreem.zib.de");
        VivaldiCoordinates clientCoordinates = VivaldiCoordinates.newBuilder().setXCoordinate(0.0)
                .setYCoordinate(0.0).setLocalError(0.1).build();
        
        ServiceSet.Builder sortedList = policy.getOSDs(osds, clientAddr, clientCoordinates, null, 0);
        
        assertEquals("osd3", sortedList.getServices(0).getUuid());
        assertEquals("osd2", sortedList.getServices(1).getUuid());
        assertEquals("osd5", sortedList.getServices(2).getUuid());
        assertEquals("osd4", sortedList.getServices(3).getUuid());
        assertEquals("osd1", sortedList.getServices(4).getUuid());
    }
    
    @Test
    public void testSortHostRoundRobinPolicy() throws Exception {

        SortHostRoundRobinPolicy policy = new SortHostRoundRobinPolicy();

        ServiceSet.Builder osds = ServiceSet.newBuilder();
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(System.currentTimeMillis() / 1000 - 1000).setName("osd1").setVersion(1)
                .setUuid("osd1").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(System.currentTimeMillis() / 1000 - 5000).setName("osd2").setVersion(1)
                .setUuid("osd2").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(System.currentTimeMillis() / 1000 - 1000).setName("osd3").setVersion(1)
                .setUuid("osd3").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(System.currentTimeMillis() / 1000 - 1000).setName("osd4").setVersion(1)
                .setUuid("osd4").setData(getDefaultServiceDataMap()));
        osds.addServices(Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD)
                .setLastUpdatedS(System.currentTimeMillis() / 1000 - 1000).setName("osd5").setVersion(1)
                .setUuid("osd5").setData(getDefaultServiceDataMap()));

        UUIDResolver.addTestMapping("osd1", "test.xyz.org", 32640, false);
        UUIDResolver.addTestMapping("osd2", "test.xyz.org", 32642, false);
        UUIDResolver.addTestMapping("osd3", "test2.xyz.org", 32640, false);
        UUIDResolver.addTestMapping("osd4", "test2.xyz.org", 32642, false);
        UUIDResolver.addTestMapping("osd5", "test3.xyz.org", 32640, false);
        
        ServiceSet.Builder sortedList = policy.getOSDs(osds);
        
        for (int i = 0; i < sortedList.getServicesCount()-1 ; i++) {
            String host1 = new ServiceUUID(sortedList.getServices(i).getUuid()).getAddress().getHostName();
            String host2 = new ServiceUUID(sortedList.getServices(i + 1).getUuid()).getAddress().getHostName();
            assertNotSame(host1, host2);
        }
    }

    private static ServiceDataMap getDefaultServiceDataMap() {
        return ServiceDataMap.newBuilder().build();
    }
    
    private static ServiceDataMap getServiceDataMap(ServiceStatus status) {
        ServiceDataMap sdm = ServiceDataMap.newBuilder().addData(KeyValuePair.newBuilder()
                .setKey(HeartbeatThread.STATUS_ATTR)
                .setValue(String.valueOf(status.getNumber())).build()).build();
        return sdm;
    }

}