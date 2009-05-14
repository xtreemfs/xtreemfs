/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.mrc;

import java.net.InetAddress;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.mrc.osdselection.DNSSelectionPolicy;
import org.xtreemfs.test.TestEnvironment;

/**

 *
 * @author bjko
 */
public class DNSSelectionPolicyTest extends TestCase {

    DNSSelectionPolicy p;
    TestEnvironment env;

    public DNSSelectionPolicyTest() {
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
        p = new DNSSelectionPolicy();
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

        ServiceDataMap m = new ServiceDataMap();
        m.put("free",Long.toString(1024*1024*1024));

        ServiceSet osds = new ServiceSet();
        Service osd = new Service(ServiceType.SERVICE_TYPE_OSD, "osd1", 0, "osd1", 1, m);
        osds.add(osd);
        UUIDResolver.addTestMapping("osd1", "xtreemfs1.zib.de", 2222, false);

        osd = new Service(ServiceType.SERVICE_TYPE_OSD, "osd2", 0, "osd2", 1, m);
        osds.add(osd);
        UUIDResolver.addTestMapping("osd2", "www.heise.de", 2222, false);

        osd = new Service(ServiceType.SERVICE_TYPE_OSD, "osd3", 0, "osd3", 1, m);
        osds.add(osd);
        UUIDResolver.addTestMapping("osd3", "xtreemfs.zib.de", 2222, false);

        osd = new Service(ServiceType.SERVICE_TYPE_OSD, "osd4", 0, "osd4", 1, m);
        osds.add(osd);
        UUIDResolver.addTestMapping("osd4", "csr-pc29.zib.de", 2222, false);

        osd = new Service(ServiceType.SERVICE_TYPE_OSD, "osd5", 0, "osd5", 1, m);
        osds.add(osd);
        UUIDResolver.addTestMapping("osd5", "download.xtreemfs.com", 2222, false);

        InetAddress clientAddr = InetAddress.getByName("xtreemfs.zib.de");

        String[] selectedOSDs = p.getOSDsForNewFile(osds, clientAddr, 10, null);

        assertEquals("osd3",selectedOSDs[0]);
        assertEquals("osd4",selectedOSDs[1]);
        assertEquals("osd1",selectedOSDs[2]);

        selectedOSDs = p.getOSDsForNewFile(osds, clientAddr, 10, "7");

        assertEquals(3,selectedOSDs.length);
        assertEquals("osd3",selectedOSDs[0]);
        assertEquals("osd4",selectedOSDs[1]);
        assertEquals("osd1",selectedOSDs[2]);


        clientAddr = InetAddress.getByName("www.heise.de");

        selectedOSDs = p.getOSDsForNewFile(osds, clientAddr, 10, null);

        assertEquals("osd2",selectedOSDs[0]);

        selectedOSDs = p.getOSDsForNewFile(osds, clientAddr, 10, "5");

        assertEquals(1,selectedOSDs.length);
        assertEquals("osd2",selectedOSDs[0]);

    }

}