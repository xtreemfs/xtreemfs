/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
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

public class OSDRangeReads {
    @Rule
    public final TestRule          testLog = TestHelper.testLog;

    private static ServiceUUID     serverID;

    private static FileCredentials fcred;

    private static String          fileId;

    private static Capability      cap;

    private static OSDConfig       osdConfig;

    private OSDServiceClient       osdClient;

    private OSD                    osdServer;
    private TestEnvironment        testEnv;

    @BeforeClass
    public static void setUpClass() throws Exception {
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
    public void testRandomRanges() throws Exception {
        // write object
        ReusableBuffer buf = BufferPool.allocate(2048);
        for (int i = 0; i < 2048; i++)
            buf.put((byte) ((i % 26) + 65));
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(2048, resp.getSizeInBytes());

        // do random reads
        for (int round = 0; round < 200; round++) {
            final int offset = (int) (Math.random() * 2048.0);
            final int size = (int) (Math.random() * (2048.0 - (offset)));
            System.out.println("offset: " + offset + " size: " + size);
            assert (offset + size <= 2048);
            RPCResponse<ObjectData> rr = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, 0, 0, offset, size);
            data = rr.get();
            ReusableBuffer dataOut = rr.getData();
            if (size == 0)
                assertNull(dataOut);
            else
                assertEquals(size, dataOut.remaining());
            for (int i = 0; i < size; i++) {
                assertEquals((byte) (((i + offset) % 26) + 65), dataOut.get());
            }
            rr.freeBuffers();
        }
    }

    @Test
    public void testRandomRangesWithEOF() throws Exception {
        // write object
        ReusableBuffer buf = BufferPool.allocate(1024);
        for (int i = 0; i < 1024; i++)
            buf.put((byte) ((i % 26) + 65));
        buf.flip();
        ObjectData data = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 0, 0, data, buf);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(1024, resp.getSizeInBytes());

        // do random reads
        for (int round = 0; round < 200; round++) {
            final int offset = (int) (Math.random() * 2048.0);
            final int size = (int) (Math.random() * (2048.0 - (offset)));
            System.out.println("offset: " + offset + " size: " + size);
            assert (offset + size <= 2048);
            RPCResponse<ObjectData> rr = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, fileId, 0, 0, offset, size);
            data = rr.get();
            ReusableBuffer dataOut = rr.getData();
            if (offset + size < 1024) {
                if (size == 0)
                    assertNull(dataOut);
                else
                    assertEquals(size, dataOut.remaining());
            } else {
                if (offset >= 1024) {
                    assertNull(dataOut);
                } else {
                    assertEquals(1024 - offset, dataOut.remaining());
                }
            }
            final int remain = (dataOut == null) ? 0 : dataOut.remaining();
            for (int i = 0; i < remain; i++) {
                assertEquals((byte) (((i + offset) % 26) + 65), dataOut.get());
            }
            rr.freeBuffers();
        }
    }

    @Test
    public void testRangeReads() throws Exception {

        /*
         * layout is obj 0: never written, does not exist on disk obj 1: 1024 written, 1024 implicit padding obj 2: 1024
         * written
         */

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
            buf.put((byte) 'A');
        buf.flip();
        r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 2, 0, 0, 0, data, buf);
        resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(4096 + 1024, resp.getSizeInBytes());

        // get a range on a fully zero padded object
        RPCResponse<ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, fileId, 0, 0, 128, 2048 - 128);
        data = r2.get();
        ReusableBuffer dataOut = r2.getData();

        assertEquals(2048 - 128, data.getZeroPadding());
        assertNull(dataOut); // 0 length is now a NULL buffer
        r2.freeBuffers();

        // get only the buffer, not the padding from object 1
        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 1, 0, 0, 1024);
        data = r2.get();
        dataOut = r2.getData();

        assertEquals(0, data.getZeroPadding());
        assertEquals(1024, dataOut.capacity());
        r2.freeBuffers();

        // get only the padding, not the buffer
        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 1, 0, 1024, 1024);
        data = r2.get();
        dataOut = r2.getData();

        assertEquals(1024, data.getZeroPadding());
        assertNull(dataOut); // 0 length = null buffer
        r2.freeBuffers();

        // get a bit of the buffer and a bit of the padding
        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 1, 0, 512, 1024);
        data = r2.get();
        dataOut = r2.getData();

        assertEquals(512, data.getZeroPadding());
        assertEquals(512, dataOut.capacity());
        r2.freeBuffers();

        // get beyond EOF
        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 2, 0, 512, 1024);
        data = r2.get();
        dataOut = r2.getData();

        assertEquals(0, data.getZeroPadding());
        assertEquals(512, dataOut.capacity());
        r2.freeBuffers();

        // get beyond EOF
        r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                fileId, 2, 0, 1024, 1024);
        data = r2.get();
        dataOut = r2.getData();

        assertEquals(0, data.getZeroPadding());
        assertNull(dataOut); // 0 length = null buffer
        r2.freeBuffers();
    }

}
