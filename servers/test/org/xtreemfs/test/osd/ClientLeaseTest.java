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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.osd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ClientLease;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;

/**
 * Class for testing the NewOSD It uses the old OSDTest tests. It checks if the
 * OSD works without replicas neither striping
 *
 * @author Jesus Malo (jmalo)
 */
public class ClientLeaseTest extends TestCase {

    private final ServiceUUID serverID;

    private final Locations   loc;

    private final String      file;

    private Capability        cap;

    private OSD               osd;

    private final long        stripeSize = 1;

    private DIRClient         dirClient;

    private OSDClient         client;

    private final DIRConfig   dirConfig;

    private final OSDConfig   osdConfig;

    private RequestController dir;

    public ClientLeaseTest(String testName) throws Exception {
        super(testName);

        Logging.start(Logging.LEVEL_TRACE);

        dirConfig = SetupUtils.createDIRConfig();
        osdConfig = SetupUtils.createOSD1Config();

        // It sets the loc attribute
        List<Location> locations = new ArrayList<Location>(1);
        StripingPolicy sp = new RAID0(stripeSize, 1);
        serverID = SetupUtils.getOSD1UUID();
        List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
        osd.add(serverID);
        locations.add(new Location(sp, osd));
        loc = new Locations(locations);

        file = "1:1";
        cap = new Capability(file, "x", 0, osdConfig.getCapabilitySecret());
    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        dir = new RequestController(dirConfig);
        dir.startup();

        SetupUtils.initTimeSync();
        
        dirClient = SetupUtils.createDIRClient(10000);

        osd = new OSD(osdConfig);
        client = SetupUtils.createOSDClient(10000);
    }

    protected void tearDown() throws Exception {
        osd.shutdown();
        dir.shutdown();

        client.shutdown();
        client.waitForShutdown();

        if (dirClient != null) {
            dirClient.shutdown();
            dirClient.waitForShutdown();
        }
    }

    public void testAcquireLease() throws Exception {

        OSDClient c = new OSDClient(dirClient.getSpeedy());
        
        ClientLease lease = new ClientLease(file);
        lease.setClientId("ABCDEF");
        lease.setOperation(ClientLease.EXCLUSIVE_LEASE);
        lease.setFirstObject(0);
        lease.setLastObject(ClientLease.TO_EOF);
        
        RPCResponse<List<Map<String,Object>>> r = c.acquireClientLease(serverID.getAddress(), loc, cap, lease);
        List<Map<String,Object>> tmp = r.get();
        
        ClientLease result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getSequenceNo() > 0);
        assertTrue(result.getExpires() > 0);
    }
    
    public void testConflictingLeases() throws Exception {

        assertNotNull(dirClient);
        OSDClient c = new OSDClient(dirClient.getSpeedy());
        
        ClientLease lease = new ClientLease(file);
        lease.setClientId("ABCDEF");
        lease.setOperation(ClientLease.EXCLUSIVE_LEASE);
        lease.setFirstObject(0);
        lease.setLastObject(ClientLease.TO_EOF);
        
        RPCResponse<List<Map<String,Object>>> r = c.acquireClientLease(serverID.getAddress(), loc, cap, lease);
        List<Map<String,Object>> tmp = r.get();
        
        ClientLease result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getSequenceNo() > 0);
        assertTrue(result.getExpires() > 0);
        
        
        lease.setClientId("YXYXYX");
        r = c.acquireClientLease(serverID.getAddress(), loc, cap, lease);
        tmp = r.get();
        
        result = ClientLease.parseFromMap(tmp.get(0));
        assertNull(result.getClientId());

    }
   
    public void testMultipleLeases() throws Exception {

        assertNotNull(dirClient);
        OSDClient c = new OSDClient(dirClient.getSpeedy());
        
        ClientLease lease = new ClientLease(file);
        lease.setClientId("ABCDEF");
        lease.setOperation(ClientLease.EXCLUSIVE_LEASE);
        lease.setFirstObject(10);
        lease.setLastObject(ClientLease.TO_EOF);
        
        RPCResponse<List<Map<String,Object>>> r = c.acquireClientLease(serverID.getAddress(), loc, cap, lease);
        List<Map<String,Object>> tmp = r.get();
        
        ClientLease result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getSequenceNo() > 0);
        assertTrue(result.getExpires() > 0);
        
        
        ClientLease lease2 = new ClientLease(file);
        lease2.setClientId("ABCDEF");
        lease2.setOperation(ClientLease.EXCLUSIVE_LEASE);
        lease2.setFirstObject(0);
        lease2.setLastObject(9);
        
        RPCResponse<List<Map<String,Object>>> r2 = c.acquireClientLease(serverID.getAddress(), loc, cap, lease2);
        tmp = r2.get();
        
        result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getSequenceNo() > 0);
        assertTrue(result.getExpires() > 0);

    }
    
    public void testReturnLease() throws Exception {

        assertNotNull(dirClient);
        OSDClient c = new OSDClient(dirClient.getSpeedy());
        
        ClientLease lease = new ClientLease(file);
        lease.setClientId("ABCDEF");
        lease.setOperation(ClientLease.EXCLUSIVE_LEASE);
        lease.setFirstObject(10);
        lease.setLastObject(ClientLease.TO_EOF);
        
        RPCResponse<List<Map<String,Object>>> r = c.acquireClientLease(serverID.getAddress(), loc, cap, lease);
        List<Map<String,Object>> tmp = r.get();
        
        ClientLease result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getSequenceNo() > 0);
        assertTrue(result.getExpires() > 0);
        
        //return the lease
        try {
            RPCResponse r2 = c.returnLease(serverID.getAddress(), loc, cap, result);
            r2.waitForResponse();
        } catch (HttpErrorException ex) {
            fail("cannot return lease: "+ex);
        }
        
        //try to acquire lease again
        r = c.acquireClientLease(serverID.getAddress(), loc, cap, lease);
        tmp = r.get();
        
        result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getSequenceNo() > 0);
        assertTrue(result.getExpires() > 0);
        

    }
    
    public void testRenewLease() throws Exception {

        assertNotNull(dirClient);
        OSDClient c = new OSDClient(dirClient.getSpeedy());
        
        ClientLease lease = new ClientLease(file);
        lease.setClientId("ABCDEF");
        lease.setOperation(ClientLease.EXCLUSIVE_LEASE);
        lease.setFirstObject(0);
        lease.setLastObject(ClientLease.TO_EOF);
        
        RPCResponse<List<Map<String,Object>>> r = c.acquireClientLease(serverID.getAddress(), loc, cap, lease);
        List<Map<String,Object>> tmp = r.get();
        
        ClientLease result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getSequenceNo() > 0);
        assertTrue(result.getExpires() > 0);
        
        
        r = c.acquireClientLease(serverID.getAddress(), loc, cap, result);
        tmp = r.get();
        
        result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getExpires() > lease.getExpires());

    }
    
    public void testTimeout() throws Exception {
        
        assertNotNull(dirClient);
        OSDClient c = new OSDClient(dirClient.getSpeedy());
        
        ClientLease lease = new ClientLease(file);
        lease.setClientId("ABCDEF");
        lease.setOperation(ClientLease.EXCLUSIVE_LEASE);
        lease.setFirstObject(0);
        lease.setLastObject(ClientLease.TO_EOF);
        
        RPCResponse<List<Map<String,Object>>> r = c.acquireClientLease(serverID.getAddress(), loc, cap, lease);
        List<Map<String,Object>> tmp = r.get();
        
        ClientLease result = ClientLease.parseFromMap(tmp.get(0));
        assertNotNull(result.getClientId());
        assertTrue(result.getSequenceNo() > 0);
        assertTrue(result.getExpires() > 0);
        
        try {
            RPCResponse r2 = c.put(serverID.getAddress(),loc,cap,file,0,ReusableBuffer.wrap("YaggaYagga".getBytes()),result);
            r2.waitForResponse();
        } catch (HttpErrorException ex) {
            fail(ex.toString());
        }
        
        result.setExpires(1);
        
        try {
            RPCResponse r2 = c.put(serverID.getAddress(),loc,cap,file,0,ReusableBuffer.wrap("YaggaYagga".getBytes()),result);
            r2.waitForResponse();
            fail("lease should be timed out");
        } catch (HttpErrorException ex) {
        }
        
        
    }

    public static void main(String[] args) {
        TestRunner.run(ClientLeaseTest.class);
    }
}
