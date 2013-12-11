/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import java.io.File;

import static junit.framework.Assert.assertEquals;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class QoSTest {
    private OSDServiceClient osdClient;
    private OSD osdServer;
    private TestEnvironment testEnv;
    private final ServiceUUID serverID;
    private final GlobalTypes.FileCredentials fcred;
    private final Capability cap;
    private final String      fileId;

    private OSDConfig osdConfig;

    public QoSTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        osdConfig = SetupUtils.createQoSOSDConfig();
        serverID = SetupUtils.getOSD1UUID();
        fileId = "ABCDEF:1";
        GlobalTypes.Replica r = GlobalTypes.Replica.newBuilder().setReplicationFlags(0).setStripingPolicy(SetupUtils.getStripingPolicy(1, 2)).addOsdUuids(serverID.toString()).build();
        GlobalTypes.XLocSet xloc = GlobalTypes.XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy("").addReplicas(r).setVersion(1).build();
        cap = new Capability(fileId, GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 60, System.currentTimeMillis(), "", 0, false, GlobalTypes.SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, osdConfig.getCapabilitySecret());
        fcred = GlobalTypes.FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xloc).build();
    }

    @Before
    public void setUp() throws Exception {
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
        osdClient = new OSDServiceClient(testEnv.getRpcClient(),null);
    }

    @After
    public void tearDown() throws Exception {
        osdServer.shutdown();

        testEnv.shutdown();
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
            org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData data = org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false).build();
            RPCResponse<GlobalTypes.OSDWriteResponse> r = osdClient.write(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                    fcred, fileId, objId, 0, 0, 0, data,buf);
            GlobalTypes.OSDWriteResponse resp = r.get();
            r.freeBuffers();

            // read data 1st 512 bytes
            RPCResponse<org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData> r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                    fcred, fileId, objId, 0, 0, 512);
            data = r2.get();
            ReusableBuffer dataOut = r2.getData();

            dataOut.position(0);
            assertEquals(512, dataOut.capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'A', dataOut.get());
            r2.freeBuffers();

            r2 = osdClient.read(serverID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                    fcred, fileId, objId, 0, 1024, 512);
            org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData data2 = r2.get();
            dataOut = r2.getData();


            dataOut.position(0);
            assertEquals(512, dataOut.capacity());
            for (int i = 0; i < 512; i++)
                assertEquals((byte) 'C', dataOut.get());

            r2.freeBuffers();
        }
    }
}
