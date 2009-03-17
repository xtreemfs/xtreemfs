/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.new_osd.client.OSDClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class OSDTruncateTest extends TestCase {
    
    private final ServiceUUID serverID;
    
    private final FileCredentials  fcred;
    
    private final String      fileId;
    
    private final Capability  cap;
    
    private final OSDConfig   osdConfig;
    
    
    private OSDClient         osdClient;
    
    
    private OSD               osdServer;
    private TestEnvironment testEnv;
    
    public OSDTruncateTest(String testName) throws Exception {
        super(testName);
        
        Logging.start(Logging.LEVEL_DEBUG);
        
        osdConfig = SetupUtils.createOSD1Config();
        serverID = SetupUtils.getOSD1UUID();
        
        fileId = "ABCDEF:1";
        cap = new Capability(fileId, 0, System.currentTimeMillis(), "", 0, osdConfig.getCapabilitySecret());

        ReplicaSet replicas = new ReplicaSet();
        StringSet osdset = new StringSet();
        osdset.add(serverID.toString());
        Replica r = new Replica(new org.xtreemfs.interfaces.StripingPolicy(Constants.STRIPING_POLICY_RAID0, 2, 1), 0, osdset);
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

    public void testDeleteFile() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData("", 0, false, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 0, 0, 0, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(1024, resp.getNew_file_size().get(0).getSize_in_bytes());

        RPCResponse dr = osdClient.unlink(serverID.getAddress(), fileId, fcred);
        dr.get();
        dr.freeBuffers();
    }

    public void testTruncateShrink() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData("", 0, false, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 0, 0, 0, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(1024, resp.getNew_file_size().get(0).getSize_in_bytes());

        buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'C');
        buf.flip();
        data = new ObjectData("", 0, false, buf);
        r = osdClient.write(serverID.getAddress(), fileId, fcred, 3, 0, 0, 0, data);
        resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(3*2048+1024, resp.getNew_file_size().get(0).getSize_in_bytes());

        fcred.getXcap().setTruncate_epoch(1);

        //truncate shrink to 3 object, 3rd object half
        r = osdClient.truncate(serverID.getAddress(), fileId, fcred, 2048*2+1024);
        resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(2048*2+1024, resp.getNew_file_size().get(0).getSize_in_bytes());


        //get object 2 should be 1024 bytes long, no padding
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 2, 0, 0, 2048);
        data = r2.get();

        assertEquals(0,data.getZero_padding());
        assertEquals(1024,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());

        //get object 0 should be 2048 bytes long, either half data + half zeros or padding
        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 0, 0, 0, 2048);
        data = r2.get();

        assertTrue( (data.getZero_padding() == 0) && (data.getData().capacity() == 2048) ||
                (data.getZero_padding() == 1024) && (data.getData().capacity() == 1024) );
        r2.freeBuffers();
        BufferPool.free(data.getData());
    }

    public void testTruncateShrinkInObject() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData("", 0, false, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 0, 0, 0, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(1024, resp.getNew_file_size().get(0).getSize_in_bytes());

        fcred.getXcap().setTruncate_epoch(1);

        //truncate shrink to 512
        r = osdClient.truncate(serverID.getAddress(), fileId, fcred, 512);
        resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(512, resp.getNew_file_size().get(0).getSize_in_bytes());


        //get a range on a fully zero padded object
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 0, 0, 0, 2048);
        data = r2.get();

        assertEquals(0,data.getZero_padding());
        assertEquals(512,data.getData().capacity());

        for (int i = 0; i < 512; i++)
            assertEquals((byte) 'A', data.getData().get());

        r2.freeBuffers();
        BufferPool.free(data.getData());
    }
    
    
    public void testTruncateExtendInObject() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData("", 0, false, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 0, 0, 0, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(1024, resp.getNew_file_size().get(0).getSize_in_bytes());

        fcred.getXcap().setTruncate_epoch(1);

        //truncate extend to 2047
        r = osdClient.truncate(serverID.getAddress(), fileId, fcred, 2047);
        resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(2047, resp.getNew_file_size().get(0).getSize_in_bytes());


        //get a range on a fully zero padded object
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 0, 0, 0, 2048);
        data = r2.get();

        assertEquals(0,data.getZero_padding());
        assertEquals(2047,data.getData().capacity());

        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'A', data.getData().get());
        for (int i = 0; i < 1023; i++)
            assertEquals((byte) 0, data.getData().get());


        r2.freeBuffers();
        BufferPool.free(data.getData());
    }

    public void testTruncateExtend() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData("", 0, false, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 0, 0, 0, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(1024, resp.getNew_file_size().get(0).getSize_in_bytes());

        fcred.getXcap().setTruncate_epoch(1);

        //truncate extend to 4 objects
        r = osdClient.truncate(serverID.getAddress(), fileId, fcred, 2048*4);
        resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(2048*4, resp.getNew_file_size().get(0).getSize_in_bytes());


        //get a range on a fully zero padded object
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 2, 0, 0, 2048);
        data = r2.get();

        assertEquals(2048,data.getZero_padding());
        assertEquals(0,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());

        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 3, 0, 0, 2048);
        data = r2.get();

        assertEquals(0,data.getZero_padding());
        assertEquals(2048,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());
    }
    
    public static void main(String[] args) {
        TestRunner.run(OSDTruncateTest.class);
    }
    
}
