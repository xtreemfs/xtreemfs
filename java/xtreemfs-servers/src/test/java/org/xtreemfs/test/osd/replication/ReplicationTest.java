/*
 * Copyright (c) 2008-2011 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd.replication;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalReadLocalResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * 29.01.2009
 * 
 * @author clorenz
 */
public class ReplicationTest {
    @Rule
    public final TestRule   testLog = TestHelper.testLog;

    OSD[]                   osds;
    OSDConfig[]             configs;
    OSDServiceClient        client;

    private Capability      cap;
    private String          fileID;
    private XLocations      xLoc;

    // needed for dummy classes
    private int             stripeSize;
    private ReusableBuffer  data;

    private long            objectNo;
    private TestEnvironment testEnv;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.stripeSize = 128 * 1024; // byte
        this.data = SetupUtils.generateData(stripeSize);

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.OSD_CLIENT });
        testEnv.start();

        osds = new OSD[12];
        configs = SetupUtils.createMultipleOSDConfigs(12);
        for (int i = 0; i < osds.length; i++) {
            osds[i] = new OSD(configs[i]);
        }

        client = testEnv.getOSDClient();

        fileID = "1:1";
        objectNo = 0;
        cap = new Capability(fileID, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 60,
                System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0,
                configs[0].getCapabilitySecret());

        xLoc = createLocations(4, 3);
    }

    private XLocations createLocations(int numberOfReplicas, int numberOfStripedOSDs) throws InvalidXLocationsException {
        assert (numberOfReplicas * numberOfStripedOSDs <= osds.length);

        List<Replica> rlist = new LinkedList();
        for (int replica = 0; replica < numberOfReplicas; replica++) {
            List<String> osdset = new LinkedList();
            int startOSD = replica * numberOfStripedOSDs;
            for (int stripe = 0; stripe < numberOfStripedOSDs; stripe++) {
                // add available osds
                osdset.add(configs[startOSD + stripe].getUUID().toString());
            }

            Replica r = Replica.newBuilder()
                    .setStripingPolicy(SetupUtils.getStripingPolicy(osdset.size(), stripeSize / 1024))
                    .setReplicationFlags(0).addAllOsdUuids(osdset).build();
            rlist.add(r);
        }
        XLocSet locSet = XLocSet.newBuilder().setReadOnlyFileSize(0)
                .setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE).setVersion(1).addAllReplicas(rlist)
                .build();
        // set the first replica as current replica

        // set the first replica as current replica
        XLocations locations = new XLocations(locSet, new ServiceUUID(locSet.getReplicas(0).getOsdUuids(0)));
        return locations;
    }

    private void setReplicated(long filesize, int indexOfFullReplica) throws Exception {
        // set replication flags

        List<Replica> rlist = new LinkedList();

        for (int i = 0; i < xLoc.getXLocSet().getReplicasCount(); i++) {
            Replica r = xLoc.getXLocSet().getReplicas(i);
            if (i == indexOfFullReplica)
                rlist.add(r
                        .toBuilder()
                        .setReplicationFlags(
                                ReplicationFlags.setReplicaIsComplete(ReplicationFlags
                                        .setPartialReplica(ReplicationFlags.setRandomStrategy(0)))).build());
            else
                rlist.add(r.toBuilder()
                        .setReplicationFlags(ReplicationFlags.setPartialReplica(ReplicationFlags.setRandomStrategy(0)))
                        .build());
        }

        XLocSet locSet = xLoc.getXLocSet().toBuilder().clearReplicas().addAllReplicas(rlist)
                .setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY).setReadOnlyFileSize(filesize)
                .build();

        xLoc = new XLocations(locSet, new ServiceUUID(locSet.getReplicas(0).getOsdUuids(0)));

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        for (OSD osd : this.osds)
            osd.shutdown();

        testEnv.shutdown();

        // free buffers
        BufferPool.free(data);
    }

    /*
     * private ObjectData getObjectData(ReusableBuffer data) { return new ObjectData(0, false, 0,
     * data.createViewBuffer()); }
     */

    @Test
    public void testStriped() throws Exception {
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();

        // write object to replica 3
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> w = client.write(xLoc.getOSDsForObject(objectNo).get(2).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, 0, objdata,
                data.createViewBuffer());
        OSDWriteResponse wResp = w.get();
        w.freeBuffers();

        // change XLoc
        setReplicated(data.limit(), 2);
        fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();

        // read object from replica 3 (object exists on this OSD) => normal read
        RPCResponse<ObjectData> r = client.read(xLoc.getOSDsForObject(objectNo).get(2).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, stripeSize);
        ObjectData rResp = r.get();
        assertTrue(Arrays.equals(data.array(), r.getData().array()));
        r.freeBuffers();

        // read object from replica 2 (object not exists on this OSD) => replication
        r = client.read(xLoc.getOSDsForObject(objectNo).get(1).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, stripeSize);
        rResp = r.get();
        if (data.capacity() > 0) {
            assertNotNull(r.getData());
            assertTrue(Arrays.equals(data.array(), r.getData().array()));
        } else
            assertNull(r.getData());
        r.freeBuffers();

        // read object from replica 4 (object not exists on this OSD) => replication
        r = client.read(xLoc.getOSDsForObject(objectNo).get(3).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, stripeSize);
        rResp = r.get();
        if (data.capacity() > 0)
            assertTrue(Arrays.equals(data.array(), r.getData().array()));
        else
            assertNull(r.getData());
        r.freeBuffers();

        // read part of object from replica 1 (object not exists on this OSD) => replication
        r = client.read(xLoc.getOSDsForObject(objectNo).get(0).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo, 0, stripeSize / 4, stripeSize / 4);
        rResp = r.get();
        int j = stripeSize / 4;
        byte[] responseData = r.getData().array();
        byte[] dataBytes = data.array();
        for (int i = 0; i < responseData.length; i++) {
            assertEquals(dataBytes[j++], responseData[i]);
        }
        r.freeBuffers();

    }

    @Test
    public void testHoleAndEOF() throws Exception {
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();

        // write object 1 to replica 1 => full object
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> w = client.write(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, 0, objdata,
                data.createViewBuffer());
        OSDWriteResponse wResp = w.get();
        w.freeBuffers();

        // object 2 is a hole

        ReusableBuffer data2 = SetupUtils.generateData(stripeSize / 2);
        // write half object 3 to replica 1 with offset => half object, HOLE
        w = client.write(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo + 2, 0, stripeSize / 4, 0, objdata,
                data2.createViewBuffer());
        wResp = w.get();
        w.freeBuffers();

        // write half object 4 to replica 1 => half object, EOF
        w = client.write(xLoc.getOSDsForObject(objectNo + 3).get(0).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo + 3, 0, 0, 0, objdata, data2.createViewBuffer());
        wResp = w.get();
        w.freeBuffers();

        // change XLoc (filesize)
        setReplicated(stripeSize * 3 + data2.limit(), 0);
        fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();

        // read object from replica 2
        RPCResponse<ObjectData> r = client.read(xLoc.getOSDsForObject(objectNo).get(1).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, stripeSize);
        ObjectData rResp = r.get();
        if (data.capacity() > 0)
            assertTrue(Arrays.equals(data.array(), r.getData().array()));
        else
            assertNull(r.getData());
        r.freeBuffers();

        // read hole from replica 2
        r = client.read(xLoc.getOSDsForObject(objectNo + 1).get(1).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo + 1, 0, 0, stripeSize);
        rResp = r.get();
        // filled with zeros
        if (rResp.getZeroPadding() == 0) {
            for (byte b : r.getData().array()) {
                assertEquals(0, b);
            }
        }
        r.freeBuffers();

        // check whether a padding object for object 2 has been created on
        // replica 2
        RPCResponse<InternalReadLocalResponse> intRLRsp = client.xtreemfs_internal_read_local(
                xLoc.getOSDsForObject(objectNo + 1).get(1).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo + 1, 0, 0, stripeSize, false, new ArrayList());
        InternalReadLocalResponse intRL = intRLRsp.get();
        assertEquals(stripeSize, intRL.getData().getZeroPadding());

        // read EOF from replica 2
        r = client.read(xLoc.getOSDsForObject(objectNo + 4).get(1).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo + 4, 0, 0, stripeSize);
        rResp = r.get();
        assertNull(r.getData());
        r.freeBuffers();

        // read hole within an object from replica 2
        r = client.read(xLoc.getOSDsForObject(objectNo + 2).get(1).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo + 2, 0, 0, stripeSize);
        rResp = r.get();
        byte[] responseData = r.getData().array();
        // correct length
        assertEquals(stripeSize / 4 + data2.limit(), responseData.length);
        // first quarter filled with zeros
        for (int i = 0; i < stripeSize / 4; i++) {
            assertEquals((byte) 0, responseData[i]);
        }
        int j = 0;
        // then there is the data
        byte[] data2bytes = data2.array();
        for (int i = stripeSize / 4; i < (stripeSize / 4) * 3; i++) {
            assertEquals(data2bytes[j++], responseData[i]);
        }
        // last quarter filled with zeros again
        assertEquals(stripeSize / 4, rResp.getZeroPadding());
        r.freeBuffers();

        // read EOF within data from replica 2
        r = client.read(xLoc.getOSDsForObject(objectNo + 3).get(1).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo + 3, 0, 0, stripeSize);
        rResp = r.get();
        assertTrue(Arrays.equals(data2.array(), r.getData().array()));
        r.freeBuffers();

        // read hole within an object from replica 2 with offset and length
        r = client.read(xLoc.getOSDsForObject(objectNo + 2).get(2).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo + 2, 0, stripeSize / 4, data2.limit());
        rResp = r.get();
        // correct length and data
        assertEquals(data2.limit(), r.getData().array().length);
        assertTrue(Arrays.equals(data2.array(), r.getData().array()));
        r.freeBuffers();

        // free buffers
        BufferPool.free(data2);
    }

    /*
     * following tests are testing readLocal-RPC
     */
    /**
     * striped case
     */
    @Test
    public void testObjectLocalAvailable() throws Exception {
        // Default case.
        helperObjectLocalAvailable();
    }

    public void helperObjectLocalAvailable() throws Exception {
        ServiceUUID serverID = xLoc.getOSDsForObject(objectNo).get(0);
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();

        // write data
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = client.write(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, 0, objdata, this.data.createViewBuffer());
        OSDWriteResponse resp = r.get();
        r.freeBuffers();

        // change XLoc
        setReplicated(this.data.limit(), 0);
        fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();

        // read data
        RPCResponse<InternalReadLocalResponse> r2 = client.xtreemfs_internal_read_local(serverID.getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, stripeSize,
                false, new ArrayList());
        InternalReadLocalResponse resp2 = r2.get();

        assertTrue(Arrays.equals(data.array(), r2.getData().array()));
        assertEquals(0, resp2.getObjectSetCount());

        r2.freeBuffers();

        // read only part of data
        r2 = client.xtreemfs_internal_read_local(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo, 0, stripeSize / 4, stripeSize / 2, true,
                new ArrayList());
        resp2 = r2.get();

        int j = stripeSize / 4;
        byte[] responseData = r2.getData().array();
        byte[] dataBytes = data.array();
        assertEquals(stripeSize / 2, responseData.length);
        for (int i = 0; i < responseData.length; i++) {
            assertEquals(dataBytes[j++], responseData[i]);
        }
        assertEquals(1, resp2.getObjectSetCount());

        // check object list
        ObjectList objectList = resp2.getObjectSet(0);
        ObjectSet list = new ObjectSet(objectList.getStripeWidth(), objectList.getFirst(), objectList.getSet()
                .toByteArray());
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue(list.contains(objectNo));

        r2.freeBuffers();
    }

    /**
     * striped case
     */
    @Test
    public void testObjectLocalNOTAvailable() throws Exception {
        helperObjectLocalNOTAvailable();
    }

    public void helperObjectLocalNOTAvailable() throws Exception {
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();

        // read object, before one has been written
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<InternalReadLocalResponse> r = client.xtreemfs_internal_read_local(xLoc.getOSDsForObject(objectNo)
                .get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo,
                0, 0, stripeSize, true, new ArrayList());
        InternalReadLocalResponse resp = r.get();
        assertNull(r.getData());
        assertEquals(1, resp.getObjectSetCount());
        ObjectSet list = new ObjectSet(resp.getObjectSet(0).getStripeWidth(), resp.getObjectSet(0).getFirst(), resp
                .getObjectSet(0).getSet().toByteArray());
        assertEquals(0, list.size());
        r.freeBuffers();

        // write data
        RPCResponse<OSDWriteResponse> r2 = client.write(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, 0, objdata,
                this.data.createViewBuffer());
        OSDWriteResponse resp2 = r2.get();
        r2.freeBuffers();
        r2 = client
                .write(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, fc, fileID, objectNo + 2, 0, 0, 0, objdata,
                        this.data.createViewBuffer());
        resp2 = r2.get();
        r2.freeBuffers();

        // change XLoc
        setReplicated(data.limit() * 2, 0);
        fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();

        // read data
        r = client.xtreemfs_internal_read_local(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, stripeSize,
                false, new ArrayList());
        resp = r.get();
        assertTrue(Arrays.equals(data.array(), r.getData().array()));
        r.freeBuffers();
        r = client.xtreemfs_internal_read_local(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo + 2, 0, 0, stripeSize,
                false, new ArrayList());
        resp = r.get();
        assertTrue(Arrays.equals(data.array(), r.getData().array()));
        r.freeBuffers();

        // read higher object than has been written (EOF)
        r = client.xtreemfs_internal_read_local(xLoc.getOSDsForObject(objectNo + 3).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo + 3, 0, 0, stripeSize,
                false, new ArrayList());
        resp = r.get();
        assertNull(r.getData());
        r.freeBuffers();

        // read object that has not been written (hole)
        r = client.xtreemfs_internal_read_local(xLoc.getOSDsForObject(objectNo + 1).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo + 1, 0, 0, stripeSize,
                false, new ArrayList());
        resp = r.get();
        assertNull(r.getData());
        r.freeBuffers();
    }

    @Test
    public void testObjectLocalAvailableNONStriped() throws Exception {
        this.xLoc = createLocations(2, 1);
        // reuse test
        helperObjectLocalAvailable();
    }

    @Test
    public void testObjectLocalNOTAvailableNONStriped() throws Exception {
        this.xLoc = createLocations(2, 1);
        // reuse test
        helperObjectLocalNOTAvailable();
    }

    @Test
    public void testGetObjectList() throws Exception {
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();

        // read data
        RPCResponse<ObjectList> r = client.xtreemfs_internal_get_object_set(xLoc.getOSDsForObject(objectNo).get(0)
                .getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID);
        ObjectList objectList = r.get();
        r.freeBuffers();
        ObjectSet list = new ObjectSet(objectList.getStripeWidth(), objectList.getFirst(), objectList.getSet()
                .toByteArray());
        assertEquals(0, list.size());

        // write object to replica 1 : OSD 1
        RPCResponse<OSDWriteResponse> w = client.write(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, 0, objdata,
                this.data.createViewBuffer());
        OSDWriteResponse wResp = w.get();
        w.freeBuffers();

        // read data
        r = client.xtreemfs_internal_get_object_set(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID);
        objectList = r.get();
        r.freeBuffers();
        list = new ObjectSet(objectList.getStripeWidth(), objectList.getFirst(), objectList.getSet().toByteArray());
        assertEquals(1, list.size());
        assertTrue(list.contains(objectNo));

        // write object to replica 1 : OSD 2
        w = client
                .write(xLoc.getOSDsForObject(objectNo + 1).get(0).getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, fc, fileID, objectNo + 1, 0, 0, 0, objdata,
                        this.data.createViewBuffer());
        wResp = w.get();
        w.freeBuffers();

        // write object to replica 1 : OSD 3
        w = client
                .write(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, fc, fileID, objectNo + 2, 0, 0, 0, objdata,
                        this.data.createViewBuffer());
        wResp = w.get();
        w.freeBuffers();

        // write object to replica 1 : OSD 1
        w = client
                .write(xLoc.getOSDsForObject(objectNo + 3).get(0).getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, fc, fileID, objectNo + 3, 0, 0, 0, objdata,
                        this.data.createViewBuffer());
        wResp = w.get();
        w.freeBuffers();

        // read object list from OSD 1 : OSD 1
        r = client.xtreemfs_internal_get_object_set(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID);
        objectList = r.get();
        r.freeBuffers();
        list = new ObjectSet(objectList.getStripeWidth(), objectList.getFirst(), objectList.getSet().toByteArray());
        assertEquals(2, list.size());
        assertTrue(list.contains(objectNo));
        assertTrue(list.contains(objectNo + 3));

        // read object list from OSD 1 : OSD 2
        r = client.xtreemfs_internal_get_object_set(xLoc.getOSDsForObject(objectNo + 1).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID);
        objectList = r.get();
        r.freeBuffers();
        list = new ObjectSet(objectList.getStripeWidth(), objectList.getFirst(), objectList.getSet().toByteArray());
        assertEquals(1, list.size());
        assertTrue(list.contains(objectNo + 1));

        // read object list from OSD 1 : OSD 3
        r = client.xtreemfs_internal_get_object_set(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID);
        objectList = r.get();
        r.freeBuffers();
        list = new ObjectSet(objectList.getStripeWidth(), objectList.getFirst(), objectList.getSet().toByteArray());
        assertEquals(1, list.size());
        assertTrue(list.contains(objectNo + 2));
    }

    @Test
    public void testOutdatedView() throws Exception {
        xLoc = createLocations(2, 1);

        // Write with view version 2
        XLocSet xLocSet = xLoc.getXLocSet().toBuilder().setVersion(2).build();
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLocSet).build();

        // write data
        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        RPCResponse<OSDWriteResponse> r = client.write(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, 0, objdata,
                this.data.createViewBuffer());
        OSDWriteResponse resp = r.get();
        r.freeBuffers();

        // change XLoc to replicated and set the view to the outdated version 1
        setReplicated(this.data.limit(), 0);
        xLocSet = xLoc.getXLocSet().toBuilder().setVersion(1).build();
        fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLocSet).build();

        // read data from first replica -> would have to replicate but will fail due to the VIEW error
        RPCResponse<ObjectData> r2 = client.read(xLoc.getOSDsForObject(objectNo).get(1).getAddress(),
                RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, stripeSize);
        try {
            ObjectData resp2 = r2.get();
            fail();
        } catch (PBRPCException e) {
            assertEquals(ErrorType.INVALID_VIEW, e.getErrorType());
        } catch (IOException e) {
            fail();
        } finally {
            r2.freeBuffers();
        }

        // update to the view version 2
        xLocSet = xLoc.getXLocSet().toBuilder().setVersion(2).build();
        fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLocSet).build();

        // read data from first replica -> has to replicate
        r2 = client.read(xLoc.getOSDsForObject(objectNo).get(1).getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fc, fileID, objectNo, 0, 0, stripeSize);
        ObjectData resp2 = r2.get();
        if (data.capacity() > 0) {
            assertNotNull(r2.getData());
            assertTrue(Arrays.equals(data.array(), r2.getData().array()));
        } else
            assertNull(r2.getData());
        r2.freeBuffers();
    }
}
