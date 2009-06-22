/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.osd;

import java.io.File;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class OSDDataIntegrityTest extends TestCase {
    
    private final ServiceUUID serverID;
    
    private final FileCredentials  fcred;
    
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
        cap = new Capability(fileId, 0, System.currentTimeMillis(), "", 0, osdConfig.getCapabilitySecret());

        ReplicaSet replicas = new ReplicaSet();
        StringSet osdset = new StringSet();
        osdset.add(serverID.toString());
        Replica r = new Replica(new org.xtreemfs.interfaces.StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 2, 1), 0, osdset);
        replicas.add(r);
        XLocSet xloc = new XLocSet(replicas, 1, "",0);

        fcred = new FileCredentials(xloc, cap.getXCap());
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
        
        osdClient = new OSDClient(testEnv.getRpcClient());
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

            buf.flip();
            ObjectData data = new ObjectData(0, false, 0, buf.createViewBuffer());
            RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, objId, 0, 0, 0, data);
            OSDWriteResponse resp = r.get();

            assertEquals(1,resp.getNew_file_size().size());
            assertEquals(1024 + (objId) * 2048, resp.getNew_file_size().get(0).getSize_in_bytes());

            r.freeBuffers();
            
            // read data
            RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, objId, 0, 0, buf.capacity());
            data = r2.get();
            
            data.getData().position(0);
            assertEquals(1024, data.getData().capacity());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'A', data.getData().get());
            BufferPool.free(data.getData());
            r2.freeBuffers();
            
            // write second half
            BufferPool.free(buf);
            buf = BufferPool.allocate(1024);
            for (int i = 0; i < 1024; i++)
                buf.put((byte) 'a');
            buf.flip();
            data = new ObjectData(0, false, 0, buf);
            RPCResponse<OSDWriteResponse> r3 = osdClient.write(serverID.getAddress(), fileId, fcred, objId, 0, 1024, 0, data);
            resp = r3.get();
            r3.freeBuffers();

            assertEquals(1,resp.getNew_file_size().size());
            assertEquals(2048 + (objId) * 2048, resp.getNew_file_size().get(0).getSize_in_bytes());
            
            // read data
            RPCResponse<ObjectData> r4 = osdClient.read(serverID.getAddress(), fileId, fcred, objId, 0, 0, 2048);
            data = r4.get();
            
            
            data.getData().position(0);
            assertEquals(2048, data.getData().capacity());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'A', data.getData().get());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'a', data.getData().get());
            BufferPool.free(data.getData());
            r4.freeBuffers();
            
            // write somewhere in the middle
            buf = BufferPool.allocate(1024);
            for (int i = 0; i < 1024; i++)
                buf.put((byte) 'x');
            buf.flip();
            data = new ObjectData(0, false, 0, buf);
            RPCResponse<OSDWriteResponse> r5 = osdClient.write(serverID.getAddress(), fileId, fcred, objId, 0, 512, 0, data);
            resp = r5.get();
            r5.freeBuffers();
            
            // read data
            RPCResponse<ObjectData> r6 = osdClient.read(serverID.getAddress(), fileId, fcred, objId, 0, 0, 2048);
            data = r6.get();
            r6.freeBuffers();
            
            data.getData().position(0);
            assertEquals(2048, data.getData().capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'A', data.getData().get());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'x', data.getData().get());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'a', data.getData().get());
            BufferPool.free(data.getData());
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

            buf.flip();
            ObjectData data = new ObjectData(0, false, 0, buf);
            RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, objId, 0, 0, 0, data);
            OSDWriteResponse resp = r.get();
            r.freeBuffers();
            
            // read data 1st 512 bytes
            RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, objId, 0, 0, 512);
            data = r2.get();
            
            data.getData().position(0);
            assertEquals(512, data.getData().capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'A', data.getData().get());
            BufferPool.free(data.getData());
            r2.freeBuffers();

            r2 = osdClient.read(serverID.getAddress(), fileId, fcred, objId, 0, 1024, 512);
            ObjectData data2 = r2.get();


            data2.getData().position(0);
            assertEquals(512, data2.getData().capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'C', data2.getData().get());
            
            BufferPool.free(data2.getData());
            r2.freeBuffers();
        }
    }
    
    
    public void testImplicitTruncateWithinObject() throws Exception {
        
        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');

        buf.flip();
        ObjectData data = new ObjectData(0, false, 0, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 0, 0, 1024, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(2048, resp.getNew_file_size().get(0).getSize_in_bytes());

        
        // read data
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 0, 0, 0, 2048);
        data = r2.get();

        data.getData().position(0);
        
        assertEquals(2048, data.getData().capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, data.getData().get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'A', data.getData().get());
        
        BufferPool.free(data.getData());
    }
    
    
    public void testImplicitTruncate() throws Exception {
        
        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData(0, false, 0, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 1, 0, 1024, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(4096, resp.getNew_file_size().get(0).getSize_in_bytes());
        
        // read data

        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 0, 0, 0, 2048);
        data = r2.get();

        assertEquals(2048,data.getZero_padding());
        assertEquals(0,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());
        
        // read data
        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 1, 0, 0, 2048);
        data = r2.get();
        
        data.getData().position(0);
        assertEquals(2048, data.getData().capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, data.getData().get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'A', data.getData().get());
        
        r2.freeBuffers();
        BufferPool.free(data.getData());
    }
    
    public void testEOF() throws Exception {
        
        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1023);
        for (int i = 0; i < 1023; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData(0, false, 0, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 1, 0, 1024, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(2047+2048, resp.getNew_file_size().get(0).getSize_in_bytes());


        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 1, 0, 0, 2048);
        data = r2.get();
        r2.freeBuffers();

        data.getData().position(0);
        assertEquals(2047, data.getData().capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, data.getData().get());
        for (int i = 0; i < 1023; i++)
            assertEquals((byte) 'A', data.getData().get());
        BufferPool.free(data.getData());

        
        // read non-existing object (EOF)
        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 2, 0, 0, 2048);
        data = r2.get();
        r2.freeBuffers();
        assertEquals(0,data.getData().capacity());
        BufferPool.free(data.getData());
    }


    public void testReadBeyonEOF() throws Exception {

        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1023);
        for (int i = 0; i < 1023; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData(0, false, 0, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 1, 0, 1024, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(2047+2048, resp.getNew_file_size().get(0).getSize_in_bytes());


        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 10, 0, 0, 2048);
        data = r2.get();
        r2.freeBuffers();

        data.getData().position(0);
        assertEquals(0, data.getData().capacity());
        assertEquals(0, data.getZero_padding());
        BufferPool.free(data.getData());
    }
     
    
    public void testOverlappingWrites() throws Exception {
        
        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');

        buf.flip();
        ObjectData data = new ObjectData(0, false, 0, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 1, 0, 0, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(2048+1024, resp.getNew_file_size().get(0).getSize_in_bytes());
        
        buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'B');
        buf.flip();
        data = new ObjectData(0, false, 0, buf);
        r = osdClient.write(serverID.getAddress(), fileId, fcred, 1, 0, 512, 0, data);
        resp = r.get();
        r.freeBuffers();
        
        // read data
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 1, 0, 0, 2048);
        data = r2.get();
        r2.freeBuffers();

        data.getData().position(0);
        assertEquals(1536, data.getData().capacity());
        for (int i = 0; i < 512; i++)
            assertEquals((byte) 'A', data.getData().get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'B', data.getData().get());
        
        BufferPool.free(data.getData());
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(OSDDataIntegrityTest.class);
    }
    
}
