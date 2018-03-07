/*
 * Copyright (c) 2009-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
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

public class StripingTest {
    @Rule
    public final TestRule        testLog = TestHelper.testLog;

    private static final boolean COW     = false;

    private TestEnvironment      testEnv;

    static class MRCDummy implements RPCResponseAvailableListener<OSDWriteResponse> {

        private long         issuedEpoch;

        private long         epoch;

        private long         fileSize;

        private final String capSecret;

        public MRCDummy(String capSecret) {
            this.capSecret = capSecret;
        }

        Capability open(char mode) {
            if (mode == 't')
                issuedEpoch++;

            return new Capability(FILE_ID, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                    | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber(), 60, System.currentTimeMillis(), "",
                    (int) issuedEpoch, false, COW ? SnapConfig.SNAP_CONFIG_ACCESS_CURRENT
                            : SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, capSecret);
        }

        synchronized long getFileSize() {
            return fileSize;
        }

        @Override
        public void responseAvailable(RPCResponse<OSDWriteResponse> r) {
            try {

                OSDWriteResponse resp = r.get();
                // System.out.println("fs-update: " + resp);

                if (resp.hasSizeInBytes()) {

                    final long newFileSize = resp.getSizeInBytes();
                    final long epochNo = resp.getTruncateEpoch();

                    if (epochNo < epoch)
                        return;

                    if (epochNo > epoch || newFileSize > fileSize) {
                        epoch = epochNo;
                        fileSize = newFileSize;
                    }
                }

            } catch (Exception exc) {
                exc.printStackTrace();
                System.exit(1);
            }
        }

    }

    private static final String       FILE_ID    = "1:1";

    private static final int          KB         = 1;

    private static final int          SIZE       = KB * 1024;

    private static final byte[]       ZEROS_HALF = new byte[SIZE / 2];

    private static final byte[]       ZEROS      = new byte[SIZE];

    private static DIRConfig          dirConfig;

    private static OSDConfig          osdCfg1;

    private static OSDConfig          osdCfg2;

    private static OSDConfig          osdCfg3;

    private static String             capSecret;

    private List<OSD>                 osdServer;

    private List<ServiceUUID>         osdIDs;

    private OSDServiceClient          client;

    private static StripingPolicyImpl sp;

    private XLocSet                   xloc;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        osdCfg1 = SetupUtils.createOSD1Config();
        osdCfg2 = SetupUtils.createOSD2Config();
        osdCfg3 = SetupUtils.createOSD3Config();

        capSecret = osdCfg1.getCapabilitySecret();

        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(3, KB)).setReplicationFlags(0)
                .build();
        sp = StripingPolicyImpl.getPolicy(r, 0);
        dirConfig = SetupUtils.createDIRConfig();

    }

    @Before
    public void setUp() throws Exception {

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT });
        testEnv.start();

        osdIDs = new ArrayList<ServiceUUID>(3);
        osdIDs.add(SetupUtils.getOSD1UUID());
        osdIDs.add(SetupUtils.getOSD2UUID());
        osdIDs.add(SetupUtils.getOSD3UUID());

        osdServer = new ArrayList<OSD>(3);
        osdServer.add(new OSD(osdCfg1));
        osdServer.add(new OSD(osdCfg2));
        osdServer.add(new OSD(osdCfg3));

        client = testEnv.getOSDClient();

        List<String> osdset = new ArrayList(3);
        osdset.add(SetupUtils.getOSD1UUID().toString());
        osdset.add(SetupUtils.getOSD2UUID().toString());
        osdset.add(SetupUtils.getOSD3UUID().toString());
        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(3, KB)).setReplicationFlags(0)
                .addAllOsdUuids(osdset).build();
        xloc = XLocSet.newBuilder().setReadOnlyFileSize(0).setVersion(1).addReplicas(r).setReplicaUpdatePolicy("")
                .build();

    }

    private Capability getCap(String fname) {
        return new Capability(fname, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber(), 60, System.currentTimeMillis(), "", 0, false,
                COW ? SnapConfig.SNAP_CONFIG_ACCESS_CURRENT : SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, capSecret);
    }

    @After
    public void tearDown() throws Exception {

        osdServer.get(0).shutdown();
        osdServer.get(1).shutdown();
        osdServer.get(2).shutdown();

        testEnv.shutdown();
    }

    /* TODO: test delete/truncate epochs! */
    @Test
    public void testPUTandGET() throws Exception {

        final int numObjs = 5;
        final int[] testSizes = { 1, 2, SIZE - 1, SIZE };

        for (int ts : testSizes) {

            ReusableBuffer data = SetupUtils.generateData(ts);
            String file = "1:1" + ts;
            final FileCredentials fcred = FileCredentials.newBuilder().setXcap(getCap(file).getXCap()).setXlocs(xloc)
                    .build();

            for (int i = 0, osdIndex = 0; i < numObjs; i++, osdIndex = i % osdIDs.size()) {

                // write an object with the given test size

                ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0)
                        .setInvalidChecksumOnOsd(false).build();
                RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(osdIndex).getAddress(),
                        RPCAuthentication.authNone, RPCAuthentication.userService, fcred, file, i, 0, 0, 0, objdata,
                        data.createViewBuffer());
                OSDWriteResponse resp = r.get();
                r.freeBuffers();
                assertTrue(resp.hasSizeInBytes());
                assertEquals(i * SIZE + ts, resp.getSizeInBytes());

                // read and check the previously written object

                RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, fcred, file, i, 0, 0, data.capacity());
                ObjectData result = r2.get();
                checkResponse(data.array(), result, r2.getData());
                r2.freeBuffers();
            }
        }
    }

    @Test
    public void testIntermediateHoles() throws Exception {

        final FileCredentials fcred = FileCredentials.newBuilder().setXcap(getCap(FILE_ID).getXCap()).setXlocs(xloc)
                .build();

        final ReusableBuffer data = SetupUtils.generateData(3);

        // write the nineth object, check the file size
        int obj = 8;
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(obj % osdIDs.size()).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fcred, FILE_ID, obj, 0, 0, 0, objdata,
                data.createViewBuffer());
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(obj * SIZE + data.limit(), resp.getSizeInBytes());

        // write the fifth object, check the file size
        obj = 5;

        r = client.write(osdIDs.get(obj % osdIDs.size()).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, FILE_ID, obj, 0, 0, 0, objdata, data.createViewBuffer());
        resp = r.get();
        r.freeBuffers();
        assertTrue(!resp.hasSizeInBytes()
                || (resp.hasSizeInBytes() && (obj * SIZE + data.limit() == resp.getSizeInBytes())));

        // check whether the first object consists of zeros
        obj = 0;
        RPCResponse<ObjectData> r2 = client.read(osdIDs.get(obj % osdIDs.size()).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fcred, FILE_ID, obj, 0, 0, data.capacity());
        ObjectData result = r2.get();
        // either padding data or all zeros
        if (result.getZeroPadding() == 0)
            checkResponse(ZEROS, result, r2.getData());
        else
            assertEquals(data.capacity(), result.getZeroPadding());

        r2.freeBuffers();

        // write the first object, check the file size header (must be null)
        r = client.write(osdIDs.get(obj % osdIDs.size()).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, FILE_ID, obj, 0, 0, 0, objdata, data.createViewBuffer());
        resp = r.get();
        r.freeBuffers();
        assertFalse(resp.hasSizeInBytes());
    }

    @Test
    public void testWriteExtend() throws Exception {

        final FileCredentials fcred = FileCredentials.newBuilder().setXcap(getCap(FILE_ID).getXCap()).setXlocs(xloc)
                .build();

        final ReusableBuffer data = SetupUtils.generateData(3);
        final byte[] paddedData = new byte[SIZE];
        System.arraycopy(data.array(), 0, paddedData, 0, data.limit());

        // write first object
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(0).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, FILE_ID, 0, 0, 0, 0, objdata, data.createViewBuffer());
        OSDWriteResponse resp = r.get();
        r.freeBuffers();

        // write second object
        r = client.write(osdIDs.get(1).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                FILE_ID, 1, 0, 0, 0, objdata, data.createViewBuffer());
        resp = r.get();
        r.freeBuffers();

        // read first object

        RPCResponse<ObjectData> r2 = client.read(osdIDs.get(0).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, FILE_ID, 0, 0, 0, SIZE);
        ObjectData result = r2.get();
        ReusableBuffer dataOut = r2.getData();
        // System.out.println(result);
        // either padding data or all zeros
        assertNotNull(dataOut);
        assertEquals(3, dataOut.capacity());
        assertEquals(SIZE - 3, result.getZeroPadding());
        r2.freeBuffers();

    }

    /**
     * tests the truncation of striped files
     */
    @Test
    public void testTruncate() throws Exception {

        ReusableBuffer data = SetupUtils.generateData(SIZE);

        FileCredentials fcred = FileCredentials.newBuilder().setXcap(getCap(FILE_ID).getXCap()).setXlocs(xloc).build();

        // -------------------------------
        // create a file with five objects
        // -------------------------------
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {
            ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0)
                    .setInvalidChecksumOnOsd(false).build();
            RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(osdIndex).getAddress(),
                    RPCAuthentication.authNone, RPCAuthentication.userService, fcred, FILE_ID, i, 0, 0, 0, objdata,
                    data.createViewBuffer());
            OSDWriteResponse resp = r.get();
            r.freeBuffers();
        }

        // ----------------------------------------------
        // shrink the file to a length of one full object
        // ----------------------------------------------

        XCap newCap = fcred.getXcap().toBuilder().setTruncateEpoch(1).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        RPCResponse<OSDWriteResponse> rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, FILE_ID, SIZE);
        OSDWriteResponse resp = rt.get();
        rt.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(SIZE, resp.getSizeInBytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, FILE_ID, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            ReusableBuffer dataOut = r2.getData();

            // the first object must exist, all other ones must have been
            // deleted
            if (i == 0)
                checkResponse(data.array(), result, dataOut);
            else
                checkResponse(null, result, dataOut);

            r2.freeBuffers();
        }

        // -------------------------------------------------
        // extend the file to a length of eight full objects
        // -------------------------------------------------
        newCap = fcred.getXcap().toBuilder().setTruncateEpoch(2).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                fcred, FILE_ID, SIZE * 8);
        resp = rt.get();
        rt.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(SIZE * 8, resp.getSizeInBytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 8; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, FILE_ID, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            ReusableBuffer dataOut = r2.getData();

            // the first object must contain data, all other ones must contain
            // zeros
            if (i == 0)
                checkResponse(data.array(), result, dataOut);
            else {
                if (dataOut == null) {
                    assertEquals(SIZE, result.getZeroPadding());
                } else {
                    checkResponse(ZEROS, result, dataOut);
                }

            }

            r2.freeBuffers();
        }

        // ------------------------------------------
        // shrink the file to a length of 3.5 objects
        // ------------------------------------------
        newCap = fcred.getXcap().toBuilder().setTruncateEpoch(3).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        final long size3p5 = (long) (SIZE * 3.5f);
        rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                fcred, FILE_ID, size3p5);
        resp = rt.get();
        rt.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(size3p5, resp.getSizeInBytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, FILE_ID, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            ReusableBuffer dataOut = r2.getData();

            // the first object must contain data, all other ones must contain
            // zeros, where the last one must only be half an object size
            if (i == 0)
                checkResponse(data.array(), result, dataOut);
            else if (i == 3)
                checkResponse(ZEROS_HALF, result, dataOut);
            else if (i >= 4) {
                assertEquals(0, result.getZeroPadding());
                assertNull(dataOut);
            } else {
                if (dataOut == null) {
                    assertEquals(SIZE, result.getZeroPadding());
                } else {
                    checkResponse(ZEROS, result, dataOut);
                }
            }

            r2.freeBuffers();
        }

        // --------------------------------------------------
        // truncate the file to the same length it had before
        // --------------------------------------------------
        newCap = fcred.getXcap().toBuilder().setTruncateEpoch(4).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                fcred, FILE_ID, size3p5);
        resp = rt.get();
        rt.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(size3p5, resp.getSizeInBytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, FILE_ID, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            ReusableBuffer dataOut = r2.getData();

            // the first object must contain data, all other ones must contain
            // zeros, where the last one must only be half an object size
            if (i == 0)
                checkResponse(data.array(), result, dataOut);
            else if (i == 3)
                checkResponse(ZEROS_HALF, result, dataOut);
            else if (i >= 4) {
                assertEquals(0, result.getZeroPadding());
                assertNull(dataOut);
            } else {
                if (dataOut == null) {
                    assertEquals(SIZE, result.getZeroPadding());
                } else {
                    checkResponse(ZEROS, result, dataOut);
                }
            }

            r2.freeBuffers();
        }

        // --------------------------------
        // truncate the file to zero length
        // --------------------------------
        newCap = fcred.getXcap().toBuilder().setTruncateEpoch(5).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                fcred, FILE_ID, 0);
        resp = rt.get();
        rt.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(0, resp.getSizeInBytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, FILE_ID, i, 0, 0, SIZE);
            ObjectData result = r2.get();

            assertEquals(0, result.getZeroPadding());
            assertNull(r2.getData());

            r2.freeBuffers();
        }

        data = SetupUtils.generateData(5);

        // ----------------------------------
        // write new data to the first object
        // ----------------------------------

        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        rt = client.write(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                FILE_ID, 0, 0, 0, 0, objdata, data.createViewBuffer());
        resp = rt.get();
        rt.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(5, resp.getSizeInBytes());

        // ----------------------------------------------
        // extend the file to a length of one full object
        // ----------------------------------------------
        newCap = fcred.getXcap().toBuilder().setTruncateEpoch(6).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                fcred, FILE_ID, SIZE);
        resp = rt.get();
        rt.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(SIZE, resp.getSizeInBytes());

        // try to read the object
        RPCResponse<ObjectData> r2 = client.read(osdIDs.get(0).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, FILE_ID, 0, 0, 0, SIZE);
        ObjectData result = r2.get();

        // the object must contain data plus padding zeros

        final byte[] dataWithZeros = new byte[SIZE];
        System.arraycopy(data.array(), 0, dataWithZeros, 0, data.limit());

        checkResponse(dataWithZeros, result, r2.getData());
        r2.freeBuffers();

        // ---------------------------------------------
        // shrink the file to a length of half an object
        // ---------------------------------------------
        newCap = fcred.getXcap().toBuilder().setTruncateEpoch(7).build();
        fcred = fcred.toBuilder().setXcap(newCap).build();

        rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                fcred, FILE_ID, SIZE / 2);
        resp = rt.get();
        rt.freeBuffers();
        assertTrue(resp.hasSizeInBytes());
        assertEquals(SIZE / 2, resp.getSizeInBytes());

        // try to read the object
        r2 = client.read(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fcred,
                FILE_ID, 0, 0, 0, SIZE);
        result = r2.get();

        // the object must contain data plus padding zeros

        final byte[] dataWithHalfZeros = new byte[SIZE / 2];
        System.arraycopy(data.array(), 0, dataWithHalfZeros, 0, data.limit());

        checkResponse(dataWithHalfZeros, result, r2.getData());
        r2.freeBuffers();
    }

    @Test
    public void testInterleavedWriteAndTruncate() throws Exception {

        final int numIterations = 20;
        final int maxObject = 20;
        final int maxSize = maxObject * SIZE;
        final int numWrittenObjs = 5;

        final MRCDummy mrcDummy = new MRCDummy(capSecret);

        FileCredentials fcred = FileCredentials.newBuilder().setXcap(getCap(FILE_ID).getXCap()).setXlocs(xloc).build();

        final List<RPCResponse> responses = new LinkedList<RPCResponse>();

        for (int l = 0; l < numIterations; l++) {

            Capability cap = mrcDummy.open('w');

            // randomly write 'numWrittenObjs' objects
            for (int i = 0; i < numWrittenObjs; i++) {

                final int objId = (int) (Math.random() * maxObject);
                final int osdIndex = objId % osdIDs.size();

                // write an object with a random amount of bytes
                final int size = (int) ((SIZE - 1) * Math.random()) + 1;
                ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0)
                        .setInvalidChecksumOnOsd(false).build();
                RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(osdIndex).getAddress(),
                        RPCAuthentication.authNone, RPCAuthentication.userService, fcred, FILE_ID, objId, 0, 0, 0,
                        objdata, SetupUtils.generateData(size));
                responses.add(r);

                // update the file size when the response is received
                r.registerListener(mrcDummy);
            }

            // wait until all write requests have been completed, i.e. all file
            // size updates have been performed
            for (RPCResponse r : responses) {
                r.waitForResult();
                r.freeBuffers();
            }
            responses.clear();

            fcred = fcred.toBuilder().setXcap(mrcDummy.open('t').getXCap()).build();

            // truncate the file
            long newSize = (long) (Math.random() * maxSize);
            RPCResponse<OSDWriteResponse> rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone,
                    RPCAuthentication.userService, fcred, FILE_ID, newSize);
            rt.registerListener(mrcDummy);
            rt.waitForResult();
            rt.freeBuffers();

            long fileSize = mrcDummy.getFileSize();

            // read the previously truncated objects, check size
            for (int i = 0; i < maxObject; i++) {
                RPCResponse<ObjectData> r2 = client.read(osdIDs.get(i % osdIDs.size()).getAddress(),
                        RPCAuthentication.authNone, RPCAuthentication.userService, fcred, FILE_ID, i, 0, 0, SIZE);
                ObjectData result = r2.get();
                ReusableBuffer dataOut = r2.getData();
                int dataOutLen = (dataOut == null) ? 0 : dataOut.capacity();

                // check inner objects - should be full
                if (i < fileSize / SIZE)
                    assertEquals(SIZE, result.getZeroPadding() + dataOutLen);

                // check last object - should either be an EOF (null) or partial
                // object
                else if (i == fileSize / SIZE) {
                    if (fileSize % SIZE == 0)
                        assertEquals(0, result.getZeroPadding() + dataOutLen);
                    else
                        assertEquals(fileSize % SIZE, result.getZeroPadding() + dataOutLen);
                }

                // check outer objects - should be EOF (null)
                else
                    assertEquals(0, result.getZeroPadding() + dataOutLen);

                r2.freeBuffers();
            }

        }

    }

    /**
     * tests the deletion of striped files
     */
    // public void testDELETE() throws Exception {
    //
    // final int numObjs = 5;
    //
    // final FileCredentials fcred = new FileCredentials(xloc,
    // getCap(FILE_ID).getXCap());
    //
    // ReusableBuffer data = SetupUtils.generateData(SIZE);
    //
    // // create all objects
    // for (int i = 0, osdIndex = 0; i < numObjs; i++, osdIndex = i %
    // osdIDs.size()) {
    //
    // ObjectData objdata = new ObjectData(data.createViewBuffer(), 0, 0,
    // false);
    // RPCResponse<OSDWriteResponse> r =
    // client.write(osdIDs.get(osdIndex).getAddress(),
    // FILE_ID, fcred, i, 0, 0, 0, objdata);
    // r.get();
    // }
    //
    // // delete the file
    // RPCResponse dr = client.unlink(osdIDs.get(0).getAddress(), FILE_ID,
    // fcred);
    // dr.get();
    // dr.freeBuffers();
    // }

    /**
     * Checks whether the data array received with the response is equal to the given one.
     * 
     * @param data
     *            the data array
     * @param response
     *            the response
     * @throws Exception
     */
    public void checkResponse(byte[] data, ObjectData response, ReusableBuffer objData) throws Exception {

        if (data == null) {
            if (objData != null)
                /*
                 * System.out.println("body (" + response.getBody().capacity() + "): " + new
                 * String(response.getBody().array()));
                 */
                assertEquals(0, objData.remaining());
        }

        else {
            byte[] responseData = objData.array();
            assertEquals(data.length, responseData.length);
            for (int i = 0; i < data.length; i++)
                assertEquals(data[i], responseData[i]);
        }
    }

}
