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

package org.xtreemfs.test.osd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class OSDDataIntegrityTest extends TestCase {
    
    private final ServiceUUID serverID;
    
    private final Locations   loc;
    
    private final String      fileId;
    
    private final Capability  cap;
    
    private final OSDConfig   osdConfig;
    
    
    private OSDClient         osdClient;
    
    
    private OSD               osdServer;
    private TestEnvironment testEnv;
    
    public OSDDataIntegrityTest(String testName) throws Exception {
        super(testName);
        
        Logging.start(Logging.LEVEL_DEBUG);
        
        osdConfig = SetupUtils.createOSD1Config();
        serverID = SetupUtils.getOSD1UUID();
        
        fileId = "ABCDEF:1";
        cap = new Capability(fileId, "DebugCapability", 0, osdConfig.getCapabilitySecret());
        
        List<Location> locations = new ArrayList<Location>(1);
        StripingPolicy sp = new RAID0(2, 1);
        List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
        osd.add(serverID);
        locations.add(new Location(sp, osd));
        
        loc = new Locations(locations);
    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);
        
        FSUtils.delTree(testDir);
        testDir.mkdirs();
        
        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[]{
                    TestEnvironment.Services.DIR_SERVICE,TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                    TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT
        });
        testEnv.start();
        
        osdServer = new OSD(osdConfig);
        
        synchronized (this) {
            try {
                this.wait(50);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        
        osdClient = testEnv.getOsdClient();
    }
    
    protected void tearDown() throws Exception {
        System.out.println("teardown");
        osdServer.shutdown();
        
        testEnv.shutdown();
        System.out.println("shutdown complete");
    }
    
    public void testWriteRanges() throws Exception {
        
        // test for obj 1,2,3...
        for (int objId = 0; objId < 5; objId++) {
            // write half object
            ReusableBuffer buf = BufferPool.allocate(1024);
            for (int i = 0; i < 1024; i++)
                buf.put((byte) 'A');
            RPCResponse r = osdClient.putWithForcedIncrement(serverID.getAddress(), loc, cap, fileId,
                objId, 0, buf);
            r.waitForResponse();
            
            String newFileSize = r.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
            long newFS = Long.parseLong(newFileSize);
            assertEquals(1024 + (objId) * 2048, newFS);
            
            r.freeBuffers();
            
            // read data
            r = osdClient.get(serverID.getAddress(), loc, cap, fileId, objId);
            ReusableBuffer data = r.getBody();
            
            data.position(0);
            assertEquals(1024, data.capacity());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'A', data.get());
            r.freeBuffers();
            
            // write second half
            buf = BufferPool.allocate(1024);
            for (int i = 0; i < 1024; i++)
                buf.put((byte) 'a');
            r = osdClient.put(serverID.getAddress(), loc, cap, fileId, objId, 1024, buf);
            r.waitForResponse();
            r.freeBuffers();
            
            // read data
            r = osdClient.get(serverID.getAddress(), loc, cap, fileId, objId);
            data = r.getBody();
            
            data.position(0);
            assertEquals(2048, data.capacity());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'A', data.get());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'a', data.get());
            r.freeBuffers();
            
            // write somewhere in the middle
            buf = BufferPool.allocate(1024);
            for (int i = 0; i < 1024; i++)
                buf.put((byte) 'x');
            r = osdClient.putWithForcedIncrement(serverID.getAddress(), loc, cap, fileId, objId, 512,
                buf);
            r.waitForResponse();
            r.freeBuffers();
            
            // read data
            r = osdClient.get(serverID.getAddress(), loc, cap, fileId, objId);
            data = r.getBody();
            
            data.position(0);
            assertEquals(2048, data.capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'A', data.get());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'x', data.get());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'a', data.get());
            
            r.freeBuffers();
        }
        
    }
    
    public void testReadRanges() throws Exception {
        
        // test for obj 1,2,3...
        for (int objId = 0; objId < 5; objId++) {
            // write half object
            ReusableBuffer buf = BufferPool.allocate(2048);
            for (int i = 0; i < 512; i++)
                buf.put((byte) 'A');
            for (int i = 0; i < 512; i++)
                buf.put((byte) 'B');
            for (int i = 0; i < 512; i++)
                buf.put((byte) 'C');
            for (int i = 0; i < 512; i++)
                buf.put((byte) 'D');
            RPCResponse r = osdClient.putWithForcedIncrement(serverID.getAddress(), loc, cap, fileId,
                objId, 0, buf);
            r.waitForResponse();
            r.freeBuffers();
            
            // read data 1st 512 bytes
            r = osdClient.get(serverID.getAddress(), loc, cap, fileId, objId, 0, 511);
            ReusableBuffer data = r.getBody();
            
            data.position(0);
            assertEquals(512, data.capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'A', data.get());
            
            r.freeBuffers();
            
            r = osdClient.get(serverID.getAddress(), loc, cap, fileId, objId, 1024, 1535);
            data = r.getBody();
            
            data.position(0);
            assertEquals(512, data.capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'C', data.get());
            
            r.freeBuffers();
        }
    }
    
    public void testImplicitTruncateWithinObject() throws Exception {
        
        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        RPCResponse r = osdClient.putWithForcedIncrement(serverID.getAddress(), loc, cap, fileId, 0,
            1024, buf);
        r.waitForResponse();
        String newFileSize = r.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
        newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
        long newFS = Long.parseLong(newFileSize);
        assertEquals(2048, newFS);
        r.freeBuffers();
        
        // read data
        r = osdClient.get(serverID.getAddress(), loc, cap, fileId, 0);
        ReusableBuffer data = r.getBody();
        
        data.position(0);
        assertEquals(2048, data.capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, data.get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'A', data.get());
        
        r.freeBuffers();
    }
    
    public void testImplicitTruncate() throws Exception {
        
        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        RPCResponse r = osdClient.putWithForcedIncrement(serverID.getAddress(), loc, cap, fileId, 1,
            1024, buf);
        r.waitForResponse();
        r.freeBuffers();
        
        // read data
        r = osdClient.get(serverID.getAddress(), loc, cap, fileId, 0);
        ReusableBuffer data = r.getBody();
        
        data.position(0);
        assertEquals(2048, data.capacity());
        for (int i = 0; i < 2048; i++)
            assertEquals((byte) 0, data.get());
        
        r.freeBuffers();
        
        // read data
        r = osdClient.get(serverID.getAddress(), loc, cap, fileId, 1);
        data = r.getBody();
        
        data.position(0);
        assertEquals(2048, data.capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, data.get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'A', data.get());
        
        r.freeBuffers();
    }
    
    public void testEOF() throws Exception {
        
        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1023);
        for (int i = 0; i < 1023; i++)
            buf.put((byte) 'A');
        RPCResponse r = osdClient.putWithForcedIncrement(serverID.getAddress(), loc, cap, fileId, 1,
            1024, buf);
        r.waitForResponse();
        r.freeBuffers();
        
        // read data
        r = osdClient.get(serverID.getAddress(), loc, cap, fileId, 1);
        ReusableBuffer data = r.getBody();
        
        data.position(0);
        assertEquals(2047, data.capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, data.get());
        for (int i = 0; i < 1023; i++)
            assertEquals((byte) 'A', data.get());
        
        r.freeBuffers();
        
        // read non-existing object (EOF)
        r = osdClient.get(serverID.getAddress(), loc, cap, fileId, 2);
        data = r.getBody();
        assertNull(data);
        r.freeBuffers();
    }
    
    public void testOverlappingWrites() throws Exception {
        
        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        RPCResponse r = osdClient.putWithForcedIncrement(serverID.getAddress(), loc, cap, fileId, 1,
            0, buf);
        r.waitForResponse();
        r.freeBuffers();
        
        buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'B');
        r = osdClient.putWithForcedIncrement(serverID.getAddress(), loc, cap, fileId, 1, 512, buf);
        r.waitForResponse();
        r.freeBuffers();
        
        // read data
        r = osdClient.get(serverID.getAddress(), loc, cap, fileId, 1);
        ReusableBuffer data = r.getBody();
        
        data.position(0);
        assertEquals(1536, data.capacity());
        for (int i = 0; i < 512; i++)
            assertEquals((byte) 'A', data.get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'B', data.get());
        
        r.freeBuffers();
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(OSDDataIntegrityTest.class);
    }
    
}
