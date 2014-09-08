/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import static org.junit.Assert.assertEquals;
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
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class OSDDataIntegrityTest {
    @Rule
    public final TestRule         testLog = TestHelper.testLog;

    private static ServiceUUID     serverID;

    private static FileCredentials fcred;

    private static String          fileId;

    private static Capability      cap;

    private static OSDConfig       osdConfig;

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
        cap = new Capability(fileId, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 60,
                System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0,
                osdConfig.getCapabilitySecret());

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
    public void testWriteRanges() throws Exception {

        // test for obj 1,2,3...
        for (int objId = 0; objId < 5; objId++) {
            // write half object
            ReusableBuffer buf = BufferPool.allocate(1024);
            for (int i = 0; i < 1024; i++)
                buf.put((byte) 'A');

            buf.flip();
            ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                    .build();
            RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, objId, 0, 0, 0, data, buf.createViewBuffer());
            OSDWriteResponse resp = r.get();

            assertTrue(resp.hasSizeInBytes());
            assertEquals(1024 + (objId) * 2048, resp.getSizeInBytes());

            r.freeBuffers();

            // read data
            RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, objId, 0, 0, buf.capacity());
            data = r2.get();
            ReusableBuffer dataOut = r2.getData();

            dataOut.position(0);
            assertEquals(1024, dataOut.capacity());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'A', dataOut.get());

            r2.freeBuffers();

            // write second half
            BufferPool.free(buf);
            buf = BufferPool.allocate(1024);
            for (int i = 0; i < 1024; i++)
                buf.put((byte) 'a');
            buf.flip();
            RPCResponse<OSDWriteResponse> r3 = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, objId, 0, 1024, 0, data, buf);
            resp = r3.get();
            r3.freeBuffers();

            assertTrue(resp.hasSizeInBytes());
            assertEquals(2048 + (objId) * 2048, resp.getSizeInBytes());

            // read data
            RPCResponse<ObjectData> r4 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, objId, 0, 0, 2048);
            data = r4.get();
            dataOut = r4.getData();

            dataOut.position(0);
            assertEquals(2048, dataOut.capacity());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'A', dataOut.get());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'a', dataOut.get());

            r4.freeBuffers();

            // write somewhere in the middle
            buf = BufferPool.allocate(1024);
            for (int i = 0; i < 1024; i++)
                buf.put((byte) 'x');
            buf.flip();
            RPCResponse<OSDWriteResponse> r5 = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, objId, 0, 512, 0, data, buf);
            resp = r5.get();
            r5.freeBuffers();

            // read data
            RPCResponse<ObjectData> r6 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, objId, 0, 0, 2048);
            data = r6.get();
            dataOut = r6.getData();

            dataOut.position(0);
            assertEquals(2048, dataOut.capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'A', dataOut.get());
            for (int i = 0; i < 1024; i++)
                assertEquals((byte) 'x', dataOut.get());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'a', dataOut.get());

            r6.freeBuffers();
        }

    }

    @Test
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
            ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                    .build();
            RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, objId, 0, 0, 0, data, buf);
            OSDWriteResponse resp = r.get();
            r.freeBuffers();

            // read data 1st 512 bytes
            RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, objId, 0, 0, 512);
            data = r2.get();
            ReusableBuffer dataOut = r2.getData();

            dataOut.position(0);
            assertEquals(512, dataOut.capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'A', dataOut.get());
            r2.freeBuffers();

            r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                    fcred, fileId, objId, 0, 1024, 512);
            ObjectData data2 = r2.get();
            dataOut = r2.getData();

            dataOut.position(0);
            assertEquals(512, dataOut.capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'C', dataOut.get());

            r2.freeBuffers();
        }
    }

    @Test
    public void testImplicitTruncateWithinObject() throws Exception {

        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');

        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 1024, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();

        assertTrue(resp.hasSizeInBytes());
        assertEquals(2048, resp.getSizeInBytes());

        // read data
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        dataOut.position(0);

        assertEquals(2048, dataOut.capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, dataOut.get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'A', dataOut.get());

        r2.freeBuffers();
    }

    @Test
    public void testImplicitTruncate() throws Exception {

        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 1, 0, 1024, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(4096, resp.getSizeInBytes());

        // read data

        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        assertTrue((data.getZeroPadding() == 2048) && (dataOut == null) || (data.getZeroPadding() == 0)
                && (dataOut.capacity() == 2048));
        r2.freeBuffers();

        // read data
        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 1, 0, 0, 2048);
        data = r2.get();
        dataOut = r2.getData();

        dataOut.position(0);
        assertEquals(2048, dataOut.capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, dataOut.get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'A', dataOut.get());

        r2.freeBuffers();
    }

    @Test
    public void testEOF() throws Exception {

        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1023);
        for (int i = 0; i < 1023; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 1, 0, 1024, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(2047 + 2048, resp.getSizeInBytes());

        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 1, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        dataOut.position(0);
        assertEquals(2047, dataOut.capacity());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 0, dataOut.get());
        for (int i = 0; i < 1023; i++)
            assertEquals((byte) 'A', dataOut.get());
        r2.freeBuffers();

        // read non-existing object (EOF)
        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 2, 0, 0, 2048);
        data = r2.get();
        dataOut = r2.getData();

        assertNull(dataOut);
        r2.freeBuffers();
    }

    @Test
    public void testReadBeyonEOF() throws Exception {

        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1023);
        for (int i = 0; i < 1023; i++)
            buf.put((byte) 'A');
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 1, 0, 1024, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(2047 + 2048, resp.getSizeInBytes());

        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 10, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        assertNull(dataOut);
        assertEquals(0, data.getZeroPadding());
        r2.freeBuffers();
    }

    @Test
    public void testOverlappingWrites() throws Exception {

        // first test implicit truncate through write within a single object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'A');

        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 1, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(2048 + 1024, resp.getSizeInBytes());

        buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) 'B');
        buf.flip();
        r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 1, 0, 512, 0, data, buf);
        resp = r.get();
        r.freeBuffers();

        // read data
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 1, 0, 0, 2048);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        dataOut.position(0);
        assertEquals(1536, dataOut.capacity());
        for (int i = 0; i < 512; i++)
            assertEquals((byte) 'A', dataOut.get());
        for (int i = 0; i < 1024; i++)
            assertEquals((byte) 'B', dataOut.get());

        r2.freeBuffers();

    }
}
