/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class OSDTruncateTest {
    @Rule
    public final TestRule         testLog = TestHelper.testLog;

    private static ServiceUUID    serverID;

    private static FileCredentials fcred;

    private static String         fileId;

    private static Capability     cap;

    private static OSDConfig      osdConfig;

    private OSDServiceClient      osdClient;

    private OSD                   osdServer;
    private TestEnvironment       testEnv;

    @BeforeClass
    public static void initializeTest() throws Exception {

        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        osdConfig = SetupUtils.createOSD1Config();
        serverID = SetupUtils.getOSD1UUID();
    }

    @Before
    public void setUp() throws Exception {
        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT });
        testEnv.start();

        osdServer = new OSD(osdConfig);

        synchronized (this) {
            try {
                this.wait(50);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        osdClient = new OSDServiceClient(testEnv.getRpcClient(), null);

        fileId = "ABCDEF:1";
        cap = new Capability(fileId, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber() | FileAccessManager.NON_POSIX_DELETE, 60,
                System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0,
                osdConfig.getCapabilitySecret(), 0);

        Replica r = Replica.newBuilder().setReplicationFlags(0).setStripingPolicy(SetupUtils.getStripingPolicy(1, 2))
                .addOsdUuids(serverID.toString()).build();
        XLocSet xloc = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy("").addReplicas(r)
                .setVersion(1).build();

        fcred = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xloc).build();
    }

    @After
    public void tearDown() throws Exception {
        osdServer.shutdown();

        testEnv.shutdown();
    }

    @Test
    public void testDeleteFile() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(1024, resp.getSizeInBytes());

        RPCResponse dr = osdClient.unlink(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId);
        dr.get();
        dr.freeBuffers();
    }

    @Test
    public void testTruncateShrink() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(1024, resp.getSizeInBytes());

        buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'C');
        buf.flip();
        r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 3, 0, 0, 0, data, buf);
        resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(3 * 2048 + 1024, resp.getSizeInBytes());

        XCap newCap = fcred.getXcap().toBuilder().setTruncateEpoch(1).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        // truncate shrink to 3 object, 3rd object half
        r = osdClient.truncate(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 2048 * 2 + 1024);
        resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(2 * 2048 + 1024, resp.getSizeInBytes());

        // get object 2 should be 1024 bytes long, no padding
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 2, 0, 0, 2048);
        data = r2.get();

        assertEquals(0, data.getZeroPadding());
        assertEquals(1024, r2.getData().capacity());
        r2.freeBuffers();

        // get object 0 should be 2048 bytes long, either half data + half zeros or padding
        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 0, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        assertTrue((data.getZeroPadding() == 0) && (dataOut.capacity() == 2048) || (data.getZeroPadding() == 1024)
                && (dataOut.capacity() == 1024));
        r2.freeBuffers();
    }

    @Test
    public void testTruncateShrink2() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 6, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(6 * 2048 + 1024, resp.getSizeInBytes());

        XCap newCap = fcred.getXcap().toBuilder().setTruncateEpoch(1).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        // truncate shrink to 3 object, 3rd object half
        r = osdClient.truncate(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 2048 * 2 + 1024);
        resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(2 * 2048 + 1024, resp.getSizeInBytes());

        // get object 2 should be 1024 bytes long, no padding
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 6, 0, 0, 2048);
        data = r2.get();

        assertEquals(0, data.getZeroPadding());
        assertNull(r2.getData()); // null equals length 0
        r2.freeBuffers();
    }

    @Test
    public void testTruncateShrinkInObject() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(1024, resp.getSizeInBytes());

        XCap newCap = fcred.getXcap().toBuilder().setTruncateEpoch(1).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        // truncate shrink to 512
        r = osdClient.truncate(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 512);
        resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(512, resp.getSizeInBytes());

        // get a range on a fully zero padded object
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        assertEquals(0, data.getZeroPadding());
        assertEquals(512, dataOut.capacity());

        for (int i = 0; i < 512; i++)
            assertEquals((byte) 'A', dataOut.get());

        r2.freeBuffers();
    }

    @Test
    public void testTruncateShrinkInObject2() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(1024, resp.getSizeInBytes());

        XCap newCap = fcred.getXcap().toBuilder().setTruncateEpoch(1).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        // wirte first 512 bytes to object 0
        ReusableBuffer buf2 = BufferPool.allocate(512);
        for (int i = 0; i < 512; i++)
            buf2.put((byte) 'B');
        buf2.flip();
        data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false).build();
        RPCResponse<OSDWriteResponse> r2 = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf2);
        OSDWriteResponse resp2 = r2.get();
        r2.freeBuffers();
        assertFalse(resp2.hasSizeInBytes());

        newCap = fcred.getXcap().toBuilder().setTruncateEpoch(1).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        // truncate shrink to 512
        r = osdClient.truncate(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 512);
        resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(512, resp.getSizeInBytes());

        // get a range on a fully zero padded object
        RPCResponse<ObjectData> r3 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 2048);
        data = r3.get();
        ReusableBuffer dataOut = r3.getData();

        assertEquals(0, data.getZeroPadding());
        assertEquals(512, dataOut.capacity());

        for (int i = 0; i < 512; i++)
            assertEquals((byte) 'B', dataOut.get());

        r3.freeBuffers();
    }
    
    @Test
    public void testTruncateExtendInObject() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(1024, resp.getSizeInBytes());

        XCap newCap = fcred.getXcap().toBuilder().setTruncateEpoch(1).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        // truncate extend to 2047
        r = osdClient.truncate(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 2047);
        resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(2047, resp.getSizeInBytes());

        // get a range on a fully zero padded object
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        assertEquals(0, data.getZeroPadding());
        assertEquals(2047, dataOut.capacity());

        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'A', dataOut.get());
        for (int i = 0; i < 1023; i++)
            assertEquals((byte) 0, dataOut.get());

        r2.freeBuffers();
    }

    @Test
    public void testTruncateExtend() throws Exception {
        // wirte first 1024 bytes to object 0
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(1024, resp.getSizeInBytes());

        XCap newCap = fcred.getXcap().toBuilder().setTruncateEpoch(1).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        // truncate extend to 4 objects

        r = osdClient.truncate(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 2048 * 4);
        resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(2048 * 4, resp.getSizeInBytes());

        // get a range on a fully zero padded object
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 2, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        assertEquals(2048, data.getZeroPadding());
        assertNull(dataOut); // null equals length 0
        r2.freeBuffers();

        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 3, 0, 0, 2048);
        data = r2.get();
        dataOut = r2.getData();

        assertEquals(0, data.getZeroPadding());
        assertEquals(2048, dataOut.capacity());
        r2.freeBuffers();
    }


}
