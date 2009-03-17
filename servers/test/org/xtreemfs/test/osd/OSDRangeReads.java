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

public class OSDRangeReads extends TestCase {
    
    private final ServiceUUID serverID;
    
    private final FileCredentials  fcred;
    
    private final String      fileId;
    
    private final Capability  cap;
    
    private final OSDConfig   osdConfig;
    
    
    private OSDClient         osdClient;
    
    
    private OSD               osdServer;
    private TestEnvironment testEnv;
    
    public OSDRangeReads(String testName) throws Exception {
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
    
    
    public void testRangeReads() throws Exception {
        
        /*
         * layout is
         * obj 0: never written, does not exist on disk
         * obj 1: 1024 written, 1024 implicit padding
         * obj 2: 1024 written
         */

        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = new ObjectData("", 0, false, buf);
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), fileId, fcred, 1, 0, 0, 0, data);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(2048+1024, resp.getNew_file_size().get(0).getSize_in_bytes());

        buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        data = new ObjectData("", 0, false, buf);
        r = osdClient.write(serverID.getAddress(), fileId, fcred, 2, 0, 0, 0, data);
        resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(4096+1024, resp.getNew_file_size().get(0).getSize_in_bytes());

        //get a range on a fully zero padded object
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 0, 0, 128, 2048-128);
        data = r2.get();

        assertEquals(2048-128,data.getZero_padding());
        assertEquals(0,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());


        //get only the buffer, not the padding from object 1
        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 1, 0, 0, 1024);
        data = r2.get();

        assertEquals(0,data.getZero_padding());
        assertEquals(1024,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());

        //get only the padding, not the buffer
        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 1, 0, 1024, 1024);
        data = r2.get();

        assertEquals(1024,data.getZero_padding());
        assertEquals(0,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());

        //get a bit of the buffer and a bit of the padding
        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 1, 0, 512, 1024);
        data = r2.get();

        assertEquals(512,data.getZero_padding());
        assertEquals(512,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());

        //get beyond EOF
        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 2, 0, 512, 1024);
        data = r2.get();

        assertEquals(0,data.getZero_padding());
        assertEquals(512,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());

        //get beyond EOF
        r2 = osdClient.read(serverID.getAddress(), fileId, fcred, 2, 0, 1024, 1024);
        data = r2.get();

        assertEquals(0,data.getZero_padding());
        assertEquals(0,data.getData().capacity());
        r2.freeBuffers();
        BufferPool.free(data.getData());
    }
    
    public static void main(String[] args) {
        TestRunner.run(OSDRangeReads.class);
    }
    
}
