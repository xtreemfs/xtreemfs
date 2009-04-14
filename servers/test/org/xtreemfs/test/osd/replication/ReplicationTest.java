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
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.InternalReadLocalResponse;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.client.OSDClient;
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
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        Logging.start(Logging.LEVEL_TRACE);

        this.stripeSize = 128 * 1024; // byte
        this.data = SetupUtils.generateData(stripeSize);

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT });
        testEnv.start();

        osds = new OSD[12];
        configs = SetupUtils.createMultipleOSDConfigs(12);
        for (int i = 0; i < osds.length; i++) {
            osds[i] = new OSD(configs[i]);
        }

        client = testEnv.getOSDClient();

        fileID = "1:1";
        objectNo = 0;
        cap = new Capability(fileID, 0, System.currentTimeMillis(), "", 0, configs[0].getCapabilitySecret());

        xLoc = createLocations(4, 3);
    }

    private XLocations createLocations(int numberOfReplicas, int numberOfStripedOSDs) {
        assert (numberOfReplicas * numberOfStripedOSDs <= osds.length);

        ReplicaSet replicas = new ReplicaSet();
        for (int replica = 0; replica < numberOfReplicas; replica++) {
            StringSet osdset = new StringSet();
            int startOSD = replica * numberOfStripedOSDs;
            for (int stripe = 0; stripe < numberOfStripedOSDs; stripe++) {
                // add available osds
                osdset.add(configs[startOSD + stripe].getUUID().toString());
            }
            Replica r = new Replica(new org.xtreemfs.interfaces.StripingPolicy(
                    StripingPolicyType.STRIPING_POLICY_RAID0, stripeSize / 1024, osdset.size()), 0, osdset);
            replicas.add(r);
        }
        return new XLocations(new XLocSet(replicas, 1, Constants.REPL_UPDATE_PC_NONE, 0));
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
        return new ObjectData(data.createViewBuffer(), 0, 0, false);
    }

    private void setReplicated(long filesize) {
        xLoc.getXLocSet().setRepUpdatePolicy(Constants.REPL_UPDATE_PC_RONLY);
        xLoc.getXLocSet().setRead_only_file_size(filesize);
    }

    @Test
    public void testStriped() throws Exception {
        FileCredentials fcred = new FileCredentials(xLoc.getXLocSet(), cap.getXCap());

        // write object to replica 3
        RPCResponse<OSDWriteResponse> w = client.write(xLoc.getOSDsForObject(objectNo).get(2).getAddress(),
                fileID, fcred, objectNo, 0, 0, 0, getObjectData(this.data));
        OSDWriteResponse wResp = w.get();
        w.freeBuffers();

        // change XLoc
        setReplicated(data.limit());

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
        FileCredentials fcred = new FileCredentials(xLoc.getXLocSet(), cap.getXCap());

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
        setReplicated(stripeSize * 3 + data2.limit());

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
        FileCredentials fcred = new FileCredentials(xLoc.getXLocSet(), cap.getXCap());

        // write data
        RPCResponse<OSDWriteResponse> r = client.write(serverID.getAddress(), fileID, fcred, objectNo, 0, 0,
                0, getObjectData(this.data));
        OSDWriteResponse resp = r.get();
        r.freeBuffers();

        // change XLoc
        setReplicated(getObjectData(this.data).getData().limit());

        // read data
        RPCResponse<InternalReadLocalResponse> r2 = client.internal_read_local(serverID.getAddress(), fileID,
                fcred, objectNo, 0, 0, stripeSize);
        InternalReadLocalResponse resp2 = r2.get();

        assertTrue(Arrays.equals(data.array(), resp2.getData().getData().array()));

        r2.freeBuffers();
        BufferPool.free(resp2.getData().getData());
        
        // read only part of data
        r2 = client.internal_read_local(serverID.getAddress(), fileID,
                fcred, objectNo, 0, stripeSize/4, stripeSize/2);
        resp2 = r2.get();

        int j = stripeSize / 4;
        byte[] responseData = resp2.getData().getData().array();
        byte[] dataBytes = data.array();
        assertEquals(stripeSize/2, responseData.length);
        for (int i = 0; i < responseData.length; i++) {
            assertEquals(dataBytes[j++], responseData[i]);
        }

        r2.freeBuffers();
        BufferPool.free(resp2.getData().getData());
    }

    /**
     * striped case
     */
    @Test
    public void testObjectLocalNOTAvailable() throws Exception {
        FileCredentials fcred = new FileCredentials(xLoc.getXLocSet(), cap.getXCap());

        // read object, before one has been written
        RPCResponse<InternalReadLocalResponse> r = client.internal_read_local(xLoc.getOSDsForObject(objectNo)
                .get(0).getAddress(), fileID, fcred, objectNo, 0, 0, stripeSize);
        InternalReadLocalResponse resp = r.get();
        assertEquals(0, resp.getData().getData().limit());
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
        setReplicated(data.limit() * 2);

        // read data
        r = client.internal_read_local(xLoc.getOSDsForObject(objectNo).get(0).getAddress(), fileID, fcred,
                objectNo, 0, 0, stripeSize);
        resp = r.get();
        assertTrue(Arrays.equals(data.array(), resp.getData().getData().array()));
        r.freeBuffers();
        r = client.internal_read_local(xLoc.getOSDsForObject(objectNo + 2).get(0).getAddress(), fileID,
                fcred, objectNo + 2, 0, 0, stripeSize);
        resp = r.get();
        assertTrue(Arrays.equals(data.array(), resp.getData().getData().array()));
        r.freeBuffers();
        BufferPool.free(resp.getData().getData());

        // read higher object than has been written (EOF)
        r = client.internal_read_local(xLoc.getOSDsForObject(objectNo + 3).get(0).getAddress(), fileID,
                fcred, objectNo + 3, 0, 0, stripeSize);
        resp = r.get();
        assertEquals(0, resp.getData().getData().limit());
        r.freeBuffers();
        BufferPool.free(resp.getData().getData());

        // read object that has not been written (hole)
        r = client.internal_read_local(xLoc.getOSDsForObject(objectNo + 1).get(0).getAddress(), fileID,
                fcred, objectNo + 1, 0, 0, stripeSize);
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

    // public void testCorrectFilesize() throws Exception {
    // // write object to replica 1 : OSD 1
    // ServiceUUID osd = xLoc.getOSDsByObject(0).get(0);
    // RPCResponse response = client.put(osd.getAddress(), xLoc, capability, file, 0, data);
    // response.waitForResponse();
    // response.freeBuffers();
    // // write object to replica 1 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(0);
    // response = client.put(osd.getAddress(), xLoc, capability, file, 1, data);
    // response.waitForResponse();
    // response.freeBuffers();
    //
    // String fileLength = (1 * stripeSize * 1024 + data.limit()) + "";
    //
    // // get correct filesize from replica 1 : OSD 1
    // osd = xLoc.getOSDsByObject(0).get(0);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 0);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // assertEquals(data.getBuffer(), response.getBody().getBuffer());
    // response.freeBuffers();
    //
    // // get unknown filesize (0) from replica 2 : OSD 1
    // osd = xLoc.getOSDsByObject(0).get(1);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 0);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get unknown filesize (replica cannot know the filesize, because
    // // nothing has been replicated so far)
    // assertEquals(0 + "", response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // // get empty map, because object was not available
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    //
    // // get unknown filesize (0) from replica 2 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(1);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 0);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get unknown filesize (replica cannot know the filesize, because
    // // nothing has been replicated so far)
    // assertEquals(0 + "", response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // // get empty map, because object was not available
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    //
    // // read object from replica 2 : OSD 1 (object not exists on this OSD)
    // // => replication
    // response = client.get(xLoc.getOSDsByObject(0).get(1).getAddress(), this.xLoc,
    // this.capability, this.file, 0);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
    // response.freeBuffers();
    //
    // // get correct filesize from replica 2 : OSD 1 (just written)
    // osd = xLoc.getOSDsByObject(0).get(1);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 0);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // assertEquals(data.getBuffer(), response.getBody().getBuffer());
    // response.freeBuffers();
    //
    // // get correct filesize from replica 2 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(1);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 1);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize (at least one OSD of this replica knows the
    // // correct filesize)
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // // get empty map, because object was not available
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    //
    // // get unknown filesize (0) from replica 3 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(2);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 0);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get unknown filesize (replica cannot know the filesize, because
    // // nothing has been replicated so far)
    // assertEquals(0 + "", response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // // get empty map, because object was not available
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    //
    // // read object from replica 3 (object not exists on this OSD)
    // // => replication
    // response = client.get(xLoc.getOSDsByObject(1).get(2).getAddress(), this.xLoc,
    // this.capability, this.file, 1);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
    // response.freeBuffers();
    //
    // // get correct filesize from replica 3 : OSD 1
    // osd = xLoc.getOSDsByObject(0).get(2);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 0);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize (at least one OSD of this replica knows the
    // // correct filesize)
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // // get empty map, because object was not available
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    // }
    //
    // public void testCorrectFilesizeForHole() throws Exception {
    // // write object to replica 4 : OSD 1
    // ServiceUUID osd = xLoc.getOSDsByObject(0).get(3);
    // RPCResponse response = client.put(osd.getAddress(), xLoc, capability, file, 0, data);
    // response.waitForResponse();
    // response.freeBuffers();
    // // write object to replica 4 : OSD 3
    // osd = xLoc.getOSDsByObject(2).get(3);
    // response = client.put(osd.getAddress(), xLoc, capability, file, 2, data);
    // response.waitForResponse();
    // response.freeBuffers();
    //
    // String fileLength = (2 * stripeSize * 1024 + data.limit()) + "";
    //
    // // get correct filesize from replica 4 : OSD 3
    // osd = xLoc.getOSDsByObject(2).get(3);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 2);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // assertEquals(data.getBuffer(), response.getBody().getBuffer());
    // response.freeBuffers();
    //
    // // get correct filesize from replica 4 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(3);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 1);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    //
    // // read object from replica 2 : OSD 2 (object not exists on this OSD)
    // // => replication, but it is a hole
    // response = client.get(xLoc.getOSDsByObject(1).get(1).getAddress(), this.xLoc,
    // this.capability, this.file, 1);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // correct length
    // assertEquals(this.stripeSize * 1024, response.getBody().limit());
    // // filled with zeros
    // for (byte b : response.getBody().array()) {
    // assertEquals(0, b);
    // }
    // response.freeBuffers();
    //
    // // get correct filesize from replica 2 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(1);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 1);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    //
    // // read object from replica 3 : OSD 3 (object not exists on this OSD)
    // // => replication
    // response = client.get(xLoc.getOSDsByObject(2).get(2).getAddress(), this.xLoc,
    // this.capability, this.file, 2);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // assertEquals(data.getBuffer(), response.getBody().getBuffer());
    // response.freeBuffers();
    //
    // // get correct filesize from replica 3 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(2);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 1);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    //
    // // get unknown filesize (0) from replica 1 : OSD 1
    // osd = xLoc.getOSDsByObject(0).get(0);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 0);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get unknown filesize (replica cannot know the filesize, because
    // // nothing has been replicated so far)
    // assertEquals(0 + "", response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // // get empty map, because object was not available
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    // response.freeBuffers();
    // }
    //
    // public void testCorrectFilesizeForEOF() throws Exception {
    // // write object to replica 3 : OSD 1
    // ServiceUUID osd = xLoc.getOSDsByObject(0).get(2);
    // RPCResponse response = client.put(osd.getAddress(), xLoc, capability, file, 0, data);
    // response.waitForResponse();
    // response.freeBuffers();
    //
    // ReusableBuffer data2 = SetupUtils.generateData(1024 * this.stripeSize / 2);
    // // write object to replica 3 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(2);
    // response = client.put(osd.getAddress(), xLoc, capability, file, 1, data2);
    // response.waitForResponse();
    // response.freeBuffers();
    //
    // String fileLength = (1 * stripeSize * 1024 + data2.limit()) + "";
    //
    // // get correct filesize from replica 3 : OSD 3
    // osd = xLoc.getOSDsByObject(2).get(2);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 2);
    // response.waitForResponse();
    //
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // // get empty map, because object was not available
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    //
    // // read object from replica 2 : OSD 3 (object not exists on this OSD)
    // // => replication, but EOF
    // response = client.get(xLoc.getOSDsByObject(2).get(1).getAddress(), this.xLoc,
    // this.capability, this.file, 2);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // correct length
    // assertNull(response.getBody());
    // response.freeBuffers();
    //
    // // get correct filesize from replica 2 : OSD 3
    // osd = xLoc.getOSDsByObject(2).get(2);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 2);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // // get empty map, because object was not available
    // assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders().getHeader(
    // HTTPHeaders.HDR_CONTENT_TYPE));
    //
    // // read object from replica 1 : OSD 2 (object not exists on this OSD)
    // // => replication
    // response = client.get(xLoc.getOSDsByObject(1).get(0).getAddress(), this.xLoc,
    // this.capability, this.file, 1);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // correct length
    // assertEquals(data2.getBuffer(), response.getBody().getBuffer());
    // response.freeBuffers();
    //
    // // get correct filesize from replica 1 : OSD 2
    // osd = xLoc.getOSDsByObject(1).get(0);
    // response = client.readLocalRPC(osd.getAddress(), xLoc, capability, file, 1);
    // response.waitForResponse();
    // assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
    // // get correct filesize
    // assertEquals(fileLength, response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
    // assertEquals(data2.getBuffer(), response.getBody().getBuffer());
    // }
}
