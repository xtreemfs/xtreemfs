/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.test.osd.replication;

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.InternalReadLocalResponse;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.ObjectList;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 * 
 * 29.01.2009
 * 
 * @author clorenz
 */
public class ReplicationTest extends TestCase {
    OSD[] osds;
    OSDConfig[] configs;
    OSDClient client;

    private Capability cap;
    private String fileID;
    private XLocations xLoc;

    // needed for dummy classes
    private int stripeSize;
    private ReusableBuffer data;

    private long objectNo;
    private TestEnvironment testEnv;

    public ReplicationTest() {
        super();
        // Auto-generated constructor stub
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        this.stripeSize = 128 * 1024; // byte
        this.data = SetupUtils.generateData(stripeSize);

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

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
        cap = new Capability(fileID, 0, System.currentTimeMillis(), "", 0, false, configs[0].getCapabilitySecret());

        xLoc = createLocations(4, 3);
    }

    private XLocations createLocations(int numberOfReplicas, int numberOfStripedOSDs) throws InvalidXLocationsException {
        assert (numberOfReplicas * numberOfStripedOSDs <= osds.length);

        ReplicaSet replicas = new ReplicaSet();
        for (int replica = 0; replica < numberOfReplicas; replica++) {
            StringSet osdset = new StringSet();
            int startOSD = replica * numberOfStripedOSDs;
            for (int stripe = 0; stripe < numberOfStripedOSDs; stripe++) {
                // add available osds
                osdset.add(configs[startOSD + stripe].getUUID().toString());
            }
            Replica r = new Replica(osdset, 0, new org.xtreemfs.interfaces.StripingPolicy(
                    StripingPolicyType.STRIPING_POLICY_RAID0, stripeSize / 1024, osdset.size()));
            replicas.add(r);
        }
        XLocSet locSet = new XLocSet(0, replicas, Constants.REPL_UPDATE_PC_NONE, 1);
        // set the first replica as current replica
        XLocations locations = new XLocations(new XLocSet(0, replicas, Constants.REPL_UPDATE_PC_NONE, 1),
                new ServiceUUID(locSet.getReplicas().get(0).getOsd_uuids().get(0)));
        return locations;
    }

    private void setReplicated(long filesize, int indexOfFullReplica) {
        // set replication flags
        for(Replica r : xLoc.getXLocSet().getReplicas())
            r.setReplication_flags(ReplicationFlags.setPartialReplica(ReplicationFlags.setRandomStrategy(0)));
        // set first replica full
        xLoc.getXLocSet().getReplicas().get(indexOfFullReplica).setReplication_flags(
                ReplicationFlags.setReplicaIsComplete(xLoc.getXLocSet().getReplicas().get(indexOfFullReplica)
                        .getReplication_flags()));
        // set read-only and filesize
        xLoc.getXLocSet().setReplica_update_policy(Constants.REPL_UPDATE_PC_RONLY);
        xLoc.getXLocSet().setRead_only_file_size(filesize);
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

    private ObjectData getObjectData(ReusableBuffer data) {
        return new ObjectData(0, false, 0, data.createViewBuffer());
    }

    @Test
    public void testStriped() throws Exception {
        FileCredentials fcred = new FileCredentials(cap.getXCap(), xLoc.getXLocSet());

        // write object to replica 3
        RPCResponse<OSDWriteResponse> w = client.write(xLoc.getOSDsForObject(objectNo).get(2).getAddress(),
                fileID, fcred, objectNo, 0, 0, 0, getObjectData(this.data));
        OSDWriteResponse wResp = w.get();
        w.freeBuffers();

        // change XLoc
        setReplicated(data.limit(), 2);

        // read object from replica 3 (object exists on this OSD) => normal read
        RPCResponse<ObjectData> r = client.read(xLoc.getOSDsForObject(objectNo).get(2).getAddress(), fileID,
                fcred, objectNo, 0, 0, stripeSize);
        ObjectData rResp = r.get();
        assertTrue(Arrays.equals(data.array(), rResp.getData().array()));
        r.freeBuffers();
        BufferPool.free(rResp.getData());

        // read object from replica 2 (object not exists on this OSD) => replication
        r = client.read(xLoc.getOSDsForObject(objectNo).get(1).getAddress(), fileID, fcred, objectNo, 0, 0,
                stripeSize);
        rResp = r.get();
        assertTrue(Arrays.equals(data.array(), rResp.getData().array()));
        r.freeBuffers();
        BufferPool.free(rResp.getData());

        // read object from replica 4 (object not exists on this OSD) => replication
        r = client.read(xLoc.getOSDsForObject(objectNo).get(3).getAddress(), fileID, fcred, objectNo, 0, 0,
                stripeSize);
        rResp = r.get();
        assertTrue(Arrays.equals(data.array(), rResp.getData().array()));
        r.freeBuffers();
        BufferPool.free(rResp.getData());

        // read part of object from replica 1 (object not exists on this OSD) => replication
        r = client.read(xLoc.getOSDsForObject(objectNo).get(0).getAddress(), fileID, fcred, objectNo, 0,
                stripeSize / 4, stripeSize / 4);
        rResp = r.get();
        int j=stripeSize / 4;
        byte[] responseData = rResp.getData().array();
        byte[] dataBytes = data.array();
        for (int i = 0; i < responseData.length; i++) {
            assertEquals(dataBytes[j++], responseData[i]);
        }
        r.freeBuffers();
        BufferPool.free(rResp.getData());
    }

    @Test
    public void testHoleAndEOF() throws Exception {
        FileCredentials fcred = new FileCredentials(cap.getXCap(), xLoc.getXLocSet());

        // write object 1 to replica 1 => full object
        RPCResponse<OSDWriteResponse> w = client.write(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                fileID, fcred, objectNo, 0, 0, 0, getObjectData(this.data));
        OSDWriteResponse wResp = w.get();
        w.freeBuffers();
        
        // object 2 is a hole

        ReusableBuffer data2 = SetupUtils.generateData(stripeSize / 2);
        // write half object 3 to replica 1 with offset => half object, HOLE
        w = client.write(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), fileID, fcred,
                objectNo + 2, 0, stripeSize / 4, 0, getObjectData(data2));
        wResp = w.get();
        w.freeBuffers();

        // write half object 4 to replica 1 => half object, EOF
        w = client.write(xLoc.getOSDsForObject(objectNo + 3).get(0).getAddress(), fileID, fcred,
                objectNo + 3, 0, 0, 0, getObjectData(data2));
        wResp = w.get();
        w.freeBuffers();

        // change XLoc (filesize)
        setReplicated(stripeSize * 3 + data2.limit(), 0);

        // read object from replica 2
        RPCResponse<ObjectData> r = client.read(xLoc.getOSDsForObject(objectNo).get(1).getAddress(), fileID,
                fcred, objectNo, 0, 0, stripeSize);
        ObjectData rResp = r.get();
        assertTrue(Arrays.equals(data.array(), rResp.getData().array()));
        r.freeBuffers();
        BufferPool.free(rResp.getData());

        // read hole from replica 2
        r = client.read(xLoc.getOSDsForObject(objectNo + 1).get(1).getAddress(), fileID, fcred, objectNo + 1,
                0, 0, stripeSize);
        rResp = r.get();
        // filled with zeros
        for (byte b : rResp.getData().array()) {
            assertEquals(0, b);
        }
        r.freeBuffers();
        BufferPool.free(rResp.getData());

        // read EOF from replica 2
        r = client.read(xLoc.getOSDsForObject(objectNo + 4).get(1).getAddress(), fileID, fcred, objectNo + 4,
                0, 0, stripeSize);
        rResp = r.get();
        assertEquals(0, rResp.getData().limit());
        r.freeBuffers();
        BufferPool.free(rResp.getData());

        // read hole within an object from replica 2
        r = client.read(xLoc.getOSDsForObject(objectNo + 2).get(1).getAddress(), fileID, fcred, objectNo + 2,
                0, 0, stripeSize);
        rResp = r.get();
        byte[] responseData = rResp.getData().array();
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
        assertEquals(stripeSize / 4, rResp.getZero_padding());
        r.freeBuffers();
        BufferPool.free(rResp.getData());

        // read EOF within data from replica 2
        r = client.read(xLoc.getOSDsForObject(objectNo + 3).get(1).getAddress(), fileID, fcred, objectNo + 3,
                0, 0, stripeSize);
        rResp = r.get();
        assertTrue(Arrays.equals(data2.array(), rResp.getData().array()));
        r.freeBuffers();
        BufferPool.free(rResp.getData());

        // read hole within an object from replica 2 with offset and length
        r = client.read(xLoc.getOSDsForObject(objectNo + 2).get(2).getAddress(), fileID, fcred, objectNo + 2,
                0, stripeSize / 4, data2.limit());
        rResp = r.get();
        // correct length and data
        assertEquals(data2.limit(), rResp.getData().array().length);
        assertTrue(Arrays.equals(data2.array(), rResp.getData().array()));
        r.freeBuffers();
        BufferPool.free(rResp.getData());
        
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
        ServiceUUID serverID = xLoc.getOSDsForObject(objectNo).get(0);
        FileCredentials fcred = new FileCredentials(cap.getXCap(), xLoc.getXLocSet());

        // write data
        RPCResponse<OSDWriteResponse> r = client.write(serverID.getAddress(), fileID, fcred, objectNo, 0, 0,
                0, getObjectData(this.data));
        OSDWriteResponse resp = r.get();
        r.freeBuffers();

        // change XLoc
        setReplicated(getObjectData(this.data).getData().limit(), 0);

        // read data
        RPCResponse<InternalReadLocalResponse> r2 = client.internal_read_local(serverID.getAddress(), fileID,
                fcred, objectNo, 0, 0, stripeSize, false, null);
        InternalReadLocalResponse resp2 = r2.get();

        assertTrue(Arrays.equals(data.array(), resp2.getData().getData().array()));
        assertEquals(0, resp2.getObject_set().size());

        r2.freeBuffers();
        BufferPool.free(resp2.getData().getData());
        
        // read only part of data
        r2 = client.internal_read_local(serverID.getAddress(), fileID,
                fcred, objectNo, 0, stripeSize/4, stripeSize/2, true, null);
        resp2 = r2.get();

        int j = stripeSize / 4;
        byte[] responseData = resp2.getData().getData().array();
        byte[] dataBytes = data.array();
        assertEquals(stripeSize/2, responseData.length);
        for (int i = 0; i < responseData.length; i++) {
            assertEquals(dataBytes[j++], responseData[i]);
        }
        assertEquals(1, resp2.getObject_set().size());
        
        // check object list
        ObjectList objectList = resp2.getObject_set().get(0);
        ObjectSet list = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList.getSet().array());
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue(list.contains(objectNo));

        r2.freeBuffers();
        BufferPool.free(resp2.getData().getData());
    }

    /**
     * striped case
     */
    @Test
    public void testObjectLocalNOTAvailable() throws Exception {
        FileCredentials fcred = new FileCredentials(cap.getXCap(), xLoc.getXLocSet());

        // read object, before one has been written
        RPCResponse<InternalReadLocalResponse> r = client.internal_read_local(xLoc.getOSDsForObject(objectNo)
                .get(0).getAddress(), fileID, fcred, objectNo, 0, 0, stripeSize, true, null);
        InternalReadLocalResponse resp = r.get();
        assertEquals(0, resp.getData().getData().limit());
        assertEquals(1, resp.getObject_set().size());
        ObjectSet list = new ObjectSet(resp.getObject_set().get(0).getStripe_width(), resp.getObject_set()
                .get(0).getFirst_(), resp.getObject_set().get(0).getSet().array());
        assertEquals(0, list.size());
        r.freeBuffers();

        // write data
        RPCResponse<OSDWriteResponse> r2 = client.write(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                fileID, fcred, objectNo, 0, 0, 0, getObjectData(this.data));
        OSDWriteResponse resp2 = r2.get();
        r2.freeBuffers();
        r2 = client.write(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), fileID, fcred,
                objectNo + 2, 0, 0, 0, getObjectData(this.data));
        resp2 = r2.get();
        r2.freeBuffers();

        // change XLoc
        setReplicated(data.limit() * 2, 0);

        // read data
        r = client.internal_read_local(xLoc.getOSDsForObject(objectNo).get(0).getAddress(), fileID, fcred,
                objectNo, 0, 0, stripeSize, false, null);
        resp = r.get();
        assertTrue(Arrays.equals(data.array(), resp.getData().getData().array()));
        r.freeBuffers();
        r = client.internal_read_local(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), fileID,
                fcred, objectNo + 2, 0, 0, stripeSize, false, null);
        resp = r.get();
        assertTrue(Arrays.equals(data.array(), resp.getData().getData().array()));
        r.freeBuffers();
        BufferPool.free(resp.getData().getData());

        // read higher object than has been written (EOF)
        r = client.internal_read_local(xLoc.getOSDsForObject(objectNo + 3).get(0).getAddress(), fileID,
                fcred, objectNo + 3, 0, 0, stripeSize, false, null);
        resp = r.get();
        assertEquals(0, resp.getData().getData().limit());
        r.freeBuffers();
        BufferPool.free(resp.getData().getData());

        // read object that has not been written (hole)
        r = client.internal_read_local(xLoc.getOSDsForObject(objectNo + 1).get(0).getAddress(), fileID,
                fcred, objectNo + 1, 0, 0, stripeSize, false, null);
        resp = r.get();
        assertEquals(0, resp.getData().getData().limit());
        r.freeBuffers();
        BufferPool.free(resp.getData().getData());
    }

    @Test
    public void testObjectLocalAvailableNONStriped() throws Exception {
        this.xLoc = createLocations(2, 1);
        // reuse test
        testObjectLocalAvailable();
    }

    @Test
    public void testObjectLocalNOTAvailableNONStriped() throws Exception {
        this.xLoc = createLocations(2, 1);
        // reuse test
        testObjectLocalNOTAvailable();
    }
    
    @Test
    public void testGetObjectList() throws Exception {
        FileCredentials fcred = new FileCredentials(cap.getXCap(), xLoc.getXLocSet());

        // read data
        RPCResponse<ObjectList> r = client.internal_getObjectList(xLoc.getOSDsForObject(objectNo).get(0)
                .getAddress(), fileID, fcred);
        ObjectList objectList = r.get();
        r.freeBuffers();
        ObjectSet list = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList.getSet().array());
        assertEquals(0, list.size());

        // write object to replica 1 : OSD 1
        RPCResponse<OSDWriteResponse> w = client.write(xLoc.getOSDsForObject(objectNo).get(0).getAddress(),
                fileID, fcred, objectNo, 0, 0, 0, getObjectData(this.data));
        OSDWriteResponse wResp = w.get();
        w.freeBuffers();

        // read data
        r = client.internal_getObjectList(xLoc.getOSDsForObject(objectNo).get(0).getAddress(), fileID, fcred);
        objectList = r.get();
        r.freeBuffers();
        list = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList.getSet().array());
        assertEquals(1, list.size());
        assertTrue(list.contains(objectNo));

        // write object to replica 1 : OSD 2
        w = client.write(xLoc.getOSDsForObject(objectNo + 1).get(0).getAddress(), fileID, fcred, objectNo + 1, 0,
                0, 0, getObjectData(this.data));
        wResp = w.get();
        w.freeBuffers();

        // write object to replica 1 : OSD 3
        w = client.write(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), fileID, fcred, objectNo + 2, 0,
                0, 0, getObjectData(this.data));
        wResp = w.get();
        w.freeBuffers();

        // write object to replica 1 : OSD 1
        w = client.write(xLoc.getOSDsForObject(objectNo + 3).get(0).getAddress(), fileID, fcred, objectNo + 3, 0,
                0, 0, getObjectData(this.data));
        wResp = w.get();
        w.freeBuffers();

        // read object list from OSD 1 : OSD 1
        r = client.internal_getObjectList(xLoc.getOSDsForObject(objectNo).get(0).getAddress(), fileID, fcred);
        objectList = r.get();
        r.freeBuffers();
        list = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList.getSet().array());
        assertEquals(2, list.size());
        assertTrue(list.contains(objectNo));
        assertTrue(list.contains(objectNo + 3));

        // read object list from OSD 1 : OSD 2
        r = client.internal_getObjectList(xLoc.getOSDsForObject(objectNo + 1).get(0).getAddress(), fileID,
                fcred);
        objectList = r.get();
        r.freeBuffers();
        list = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList.getSet().array());
        assertEquals(1, list.size());
        assertTrue(list.contains(objectNo + 1));

        // read object list from OSD 1 : OSD 3
        r = client.internal_getObjectList(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), fileID,
                fcred);
        objectList = r.get();
        r.freeBuffers();
        list = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList.getSet().array());
        assertEquals(1, list.size());
        assertTrue(list.contains(objectNo + 2));
    }
}
