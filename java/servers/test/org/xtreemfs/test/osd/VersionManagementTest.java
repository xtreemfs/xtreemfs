/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.osd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.closeResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class VersionManagementTest extends TestCase {

    private TestEnvironment     testEnv;

    private static final String FILE_ID            = "1:1";

    private static final int    KB                 = 1;

    private static final int    OBJ_SIZE           = KB * 1024;

    private static final int    CLOSE_TIMEOUT_SPAN = 5000;

    private final OSDConfig     osdCfg;

    private final String        capSecret;

    private OSD                 osdServer;

    private ServiceUUID         osdId;

    private OSDServiceClient    client;

    private XLocSet             xloc;

    /** Creates a new instance of StripingTest */
    public VersionManagementTest(String testName) throws IOException {

        super(testName);
        Logging.start(Logging.LEVEL_DEBUG);

        // reduce close timeout for faster test runs
        Properties customProps = new Properties();
        customProps.put("open_state.implicit_close_timeout", (CLOSE_TIMEOUT_SPAN / 2) + "");
        customProps.put("open_state.oft_cleanup_interval", CLOSE_TIMEOUT_SPAN + "");
        osdCfg = SetupUtils.createOSD1Config(customProps);

        capSecret = osdCfg.getCapabilitySecret();

    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        FSUtils.delTree(new File(SetupUtils.TEST_DIR));

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT });
        testEnv.start();

        osdId = SetupUtils.getOSD1UUID();

        osdServer = new OSD(osdCfg);
        client = testEnv.getOSDClient();

        List<String> osdset = new ArrayList<String>(1);
        osdset.add(SetupUtils.getOSD1UUID().toString());
        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, KB)).setReplicationFlags(0)
                .addAllOsdUuids(osdset).build();
        xloc = XLocSet.newBuilder().setReadOnlyFileSize(0).setVersion(1).addReplicas(r).setReplicaUpdatePolicy("")
                .build();

    }

    private FileCredentials getFileCredentials(int truncateEpoch, boolean write) {
        return FileCredentials
                .newBuilder()
                .setXcap(
                        new Capability(
                                FILE_ID,
                                write ? (SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber() | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR
                                        .getNumber()) : SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber(), 60,
                                System.currentTimeMillis(), "", truncateEpoch, false,
                                SnapConfig.SNAP_CONFIG_ACCESS_CURRENT, 0, capSecret).getXCap()).setXlocs(xloc).build();
    }

    private FileCredentials getFileCredentials(int truncateEpoch, long snapTimestamp) {
        return FileCredentials
                .newBuilder()
                .setXcap(
                        new Capability(FILE_ID, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber(), 60, System
                                .currentTimeMillis(), "", truncateEpoch, false, SnapConfig.SNAP_CONFIG_ACCESS_SNAP,
                                snapTimestamp, capSecret).getXCap()).setXlocs(xloc).build();
    }

    protected void tearDown() throws Exception {
        osdServer.shutdown();
        testEnv.shutdown();

    }

    // public void testTruncate() throws Exception {
    //
    // FileCredentials fcred = getFileCredentials(1, true);
    //
    // // write a new file
    // ObjectData objdata =
    // ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
    // .build();
    // RPCResponse<OSDWriteResponse> r = client.write(osdId.getAddress(), RPCAuthentication.authNone,
    // RPCAuthentication.userService, fcred, FILE_ID, 5, 0, 0, 0, objdata,
    // SetupUtils.generateData(OBJ_SIZE, (byte) 'x'));
    // r.get();
    // r.freeBuffers();
    //
    // // truncate-extend the file and read it
    // RPCResponse<OSDWriteResponse> r1 = client.truncate(osdId.getAddress(), RPCAuthentication.authNone,
    // RPCAuthentication.userService, fcred, FILE_ID, OBJ_SIZE * 8);
    // r1.get();
    // r1.freeBuffers();
    //
    // RPCResponse<ObjectData> r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone,
    // RPCAuthentication.userService, fcred, FILE_ID, 5, 0, 0, OBJ_SIZE);
    // ObjectData result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 'x', r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
    // FILE_ID,
    // 7, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
    // FILE_ID,
    // 0, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // // truncate-shrink the file and read it
    // fcred = getFileCredentials(2, true);
    //
    // r1 = client.truncate(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
    // fcred,
    // FILE_ID, OBJ_SIZE * 1);
    // r1.get();
    // r1.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
    // FILE_ID,
    // 0, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
    // FILE_ID,
    // 5, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, 0, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // }
    //
    // public void testImplicitVersionCreation() throws Exception {
    //
    // final long t0 = System.currentTimeMillis();
    //
    // FileCredentials wCred = getFileCredentials(1, true);
    // FileCredentials rCred = getFileCredentials(1, false);
    //
    // // write a new file that consists of two objects
    // ObjectData objdata =
    // ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
    // .build();
    // RPCResponse<OSDWriteResponse> r = client.write(osdId.getAddress(), RPCAuthentication.authNone,
    // RPCAuthentication.userService, wCred, FILE_ID, 0, 0, 0, 0, objdata,
    // SetupUtils.generateData(OBJ_SIZE, (byte) 'x'));
    // r.get();
    // r.freeBuffers();
    //
    // r = client.write(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, wCred,
    // FILE_ID,
    // 1, 0, 0, 0, objdata, SetupUtils.generateData(OBJ_SIZE, (byte) 'y'));
    // r.get();
    // r.freeBuffers();
    //
    // // wait for OSD-internal file close, which will implicitly cause a new
    // // version to be created
    // Thread.sleep(CLOSE_TIMEOUT_SPAN + 2500);
    //
    // // overwrite the first object
    // r = client.write(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, wCred,
    // FILE_ID,
    // 0, 0, 0, 0, objdata, SetupUtils.generateData(OBJ_SIZE, (byte) 'z'));
    // r.get();
    // r.freeBuffers();
    //
    // final long t1 = System.currentTimeMillis();
    //
    // // read and check the old version
    // FileCredentials rCredV = getFileCredentials(0, t1);
    //
    // RPCResponse<ObjectData> r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone,
    // RPCAuthentication.userService, rCredV, FILE_ID, 0, 0, 0, OBJ_SIZE);
    // ObjectData result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 'x', r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 1, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 'y', r2.getData());
    // r2.freeBuffers();
    //
    // // read and check the current version
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred,
    // FILE_ID,
    // 0, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 'z', r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred,
    // FILE_ID,
    // 1, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 'y', r2.getData());
    // r2.freeBuffers();
    //
    // // try to read a non-existing version
    // rCredV = getFileCredentials(0, t0 - 999999);
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 0, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, 0, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 1, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, 0, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // // truncate the file to zero length and wait, so that a new version is
    // // created and append another object
    // FileCredentials tCred = getFileCredentials(1, true);
    // r = client.truncate(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
    // tCred,
    // FILE_ID, 0);
    // r.get();
    // r.freeBuffers();
    //
    // // wait for OSD-internal file close, which will implicitly cause a new
    // // version to be created
    // Thread.sleep(CLOSE_TIMEOUT_SPAN + 2500);
    //
    // r = client.write(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, wCred,
    // FILE_ID,
    // 2, 0, 0, 0, objdata, SetupUtils.generateData(OBJ_SIZE, (byte) 'w'));
    // r.get();
    // r.freeBuffers();
    //
    // final long t2 = System.currentTimeMillis();
    //
    // // read and check the current version
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred,
    // FILE_ID,
    // 0, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred,
    // FILE_ID,
    // 1, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred,
    // FILE_ID,
    // 2, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 'w', r2.getData());
    // r2.freeBuffers();
    //
    // // read and check the version at t1
    // rCredV = getFileCredentials(0, t1);
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 0, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 'x', r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 1, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, OBJ_SIZE, (byte) 'y', r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 2, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, 0, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // // read and check the version at t2
    // rCredV = getFileCredentials(0, t2);
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 0, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, 0, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 1, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, 0, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // r2 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
    // FILE_ID, 2, 0, 0, OBJ_SIZE);
    // result = r2.get();
    // checkData(result, 0, (byte) 0, r2.getData());
    // r2.freeBuffers();
    //
    // }

    public void testExplicitClose() throws Exception {

        FileCredentials wCred = getFileCredentials(1, true);
        FileCredentials rCred = getFileCredentials(1, false);

        // write a new file that consists of two objects
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r1 = client.write(osdId.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, wCred, FILE_ID, 0, 0, 0, 0, objdata, -1,
                SetupUtils.generateData(OBJ_SIZE, (byte) 'x'));

        RPCResponse<OSDWriteResponse> r2 = client.write(osdId.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, wCred, FILE_ID, 1, 0, 0, 0, objdata, -1,
                SetupUtils.generateData(OBJ_SIZE, (byte) 'y'));

        r1.get();
        r1.freeBuffers();
        r2.get();
        r2.freeBuffers();

        // close the file
        RPCResponse<closeResponse> r3 = client.close(osdId.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, wCred, FILE_ID);
        long t0 = r3.get().getServerTimestamp();
        r3.freeBuffers();

        // overwrite the first object
        r1 = client.write(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, wCred,
                FILE_ID, 0, 0, 0, 0, objdata, t0, SetupUtils.generateData(OBJ_SIZE, (byte) 'z'));
        long t1 = r1.get().getServerTimestamp();
        r1.freeBuffers();

        // read and check the old version
        FileCredentials rCredV = getFileCredentials(0, t0);

        RPCResponse<ObjectData> r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, rCredV, FILE_ID, 0, 0, 0, OBJ_SIZE);
        ObjectData result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 'x', r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 1, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 'y', r4.getData());
        r4.freeBuffers();

        // read and check the current version
        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred, FILE_ID,
                0, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 'z', r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred, FILE_ID,
                1, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 'y', r4.getData());
        r4.freeBuffers();

        // try to read a non-existing version
        rCredV = getFileCredentials(0, t0 - 999999);

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 0, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, 0, (byte) 0, r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 1, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, 0, (byte) 0, r4.getData());
        r4.freeBuffers();

        // truncate the file to zero length and close it, so that a new version is
        // created, and append another object
        FileCredentials tCred = getFileCredentials(1, true);
        r1 = client.truncate(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, tCred,
                FILE_ID, 0);
        r1.get();
        r1.freeBuffers();

        // close the file
        r3 = client
                .close(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, wCred, FILE_ID);
        long ts = r3.get().getServerTimestamp();
        r3.freeBuffers();

        r1 = client.write(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, wCred,
                FILE_ID, 2, 0, 0, 0, objdata, ts, SetupUtils.generateData(OBJ_SIZE, (byte) 'w'));
        final long t2 = r1.get().getServerTimestamp();
        r1.freeBuffers();

        // read and check the current version
        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred, FILE_ID,
                0, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 0, r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred, FILE_ID,
                1, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 0, r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCred, FILE_ID,
                2, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 'w', r4.getData());
        r4.freeBuffers();

        // read and check the version at t1
        rCredV = getFileCredentials(0, t1);

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 0, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 'x', r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 1, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, OBJ_SIZE, (byte) 'y', r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 2, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, 0, (byte) 0, r4.getData());
        r4.freeBuffers();

        // read and check the version at t2
        rCredV = getFileCredentials(0, t2);

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 0, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, 0, (byte) 0, r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 1, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, 0, (byte) 0, r4.getData());
        r4.freeBuffers();

        r4 = client.read(osdId.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, rCredV,
                FILE_ID, 2, 0, 0, OBJ_SIZE);
        result = r4.get();
        checkData(result, 0, (byte) 0, r4.getData());
        r4.freeBuffers();

    }

    public static void main(String[] args) {
        TestRunner.run(VersionManagementTest.class);
    }

    private void checkData(ObjectData data, long size, byte content, ReusableBuffer dataOut) throws Exception {

        int dataOutLen = (dataOut == null) ? 0 : dataOut.capacity();
        assertEquals(size, dataOutLen + data.getZeroPadding());
        if (dataOut != null) {
            for (byte b : dataOut.array())
                assertEquals(content, b);
        }

    }

}
