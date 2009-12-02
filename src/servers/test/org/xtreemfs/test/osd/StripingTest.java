/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.test.osd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class StripingTest extends TestCase {
    private TestEnvironment testEnv;
    
    static class MRCDummy implements RPCResponseAvailableListener<OSDWriteResponse> {
        
        private long   issuedEpoch;
        
        private long   epoch;
        
        private long   fileSize;
        
        private String capSecret;
        
        public MRCDummy(String capSecret) {
            this.capSecret = capSecret;
        }
        
        Capability open(char mode) {
            if (mode == 't')
                issuedEpoch++;

            return new Capability(FILE_ID, 0, 60, System.currentTimeMillis(), "", (int)issuedEpoch, false, capSecret);
        }
        
        synchronized long getFileSize() {
            return fileSize;
        }

        @Override
        public void responseAvailable(RPCResponse<OSDWriteResponse> r) {
            try {

                OSDWriteResponse resp = r.get();
                System.out.println("fs-update: "+resp);

                if (resp.getNew_file_size().size() > 0) {

                    final long newFileSize = resp.getNew_file_size().get(0).getSize_in_bytes();
                    final long epochNo = resp.getNew_file_size().get(0).getTruncate_epoch();

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
    
    private static final String FILE_ID    = "1:1";
    
    private static final int    KB         = 1;
    
    private static final int    SIZE       = KB * 1024;
    
    private static final byte[] ZEROS_HALF = new byte[SIZE / 2];
    
    private static final byte[] ZEROS      = new byte[SIZE];
    
    private final DIRConfig     dirConfig;
    
    private final OSDConfig     osdCfg1;
    
    private final OSDConfig     osdCfg2;
    
    private final OSDConfig     osdCfg3;
    
    private final String        capSecret;
    
    private List<OSD>           osdServer;
    
    private List<ServiceUUID>   osdIDs;
    
    private OSDClient           client;
    
    private StripingPolicyImpl      sp;

    private XLocSet                 xloc;
    
    
    /** Creates a new instance of StripingTest */
    public StripingTest(String testName) throws IOException {
        super(testName);
        Logging.start(Logging.LEVEL_DEBUG);
        
        osdCfg1 = SetupUtils.createOSD1Config();
        osdCfg2 = SetupUtils.createOSD2Config();
        osdCfg3 = SetupUtils.createOSD3Config();
        
        capSecret = osdCfg1.getCapabilitySecret();

        sp = StripingPolicyImpl.getPolicy(new Replica(new StringSet(), 0, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, KB, 3)),0);
        dirConfig = SetupUtils.createDIRConfig();

    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        FSUtils.delTree(new File(SetupUtils.TEST_DIR));
        
        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[]{
                    TestEnvironment.Services.DIR_SERVICE,TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                    TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT
        });
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

        ReplicaSet replicas = new ReplicaSet();
        StringSet osdset = new StringSet();
        osdset.add(SetupUtils.getOSD1UUID().toString());
        osdset.add(SetupUtils.getOSD2UUID().toString());
        osdset.add(SetupUtils.getOSD3UUID().toString());
        Replica r = new Replica(osdset, 0, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, KB, 3));
        replicas.add(r);
        xloc = new XLocSet(0, replicas, "",1);
        
    }

    private Capability getCap(String fname) {
        return new Capability(fname, 0, 60, System.currentTimeMillis(), "", 0, false, capSecret);
    }
    
    protected void tearDown() throws Exception {
        
        testEnv.shutdown();

        osdServer.get(0).shutdown();
        osdServer.get(1).shutdown();
        osdServer.get(2).shutdown();
    }
    
    /* TODO: test delete/truncate epochs! */

    
    public void testPUTandGET() throws Exception {

        final int numObjs = 5;
        final int[] testSizes = { 1, 2, SIZE - 1, SIZE };

        for (int ts : testSizes) {

            ReusableBuffer data = SetupUtils.generateData(ts);
            data.flip();
            String file = "1:1" + ts;
            final FileCredentials fcred = new FileCredentials(getCap(file).getXCap(), xloc);

            for (int i = 0, osdIndex = 0; i < numObjs; i++, osdIndex = i % osdIDs.size()) {

                // write an object with the given test size

                ObjectData objdata = new ObjectData(0, false, 0, data.createViewBuffer());
                RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(osdIndex).getAddress(),
                        file, fcred, i, 0, 0, 0, objdata);
                OSDWriteResponse resp = r.get();
                r.freeBuffers();
                assertEquals(1,resp.getNew_file_size().size());
                assertEquals(i * SIZE + ts, resp.getNew_file_size().get(0).getSize_in_bytes());


                // read and check the previously written object

                RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), file, fcred, i, 0, 0, data.capacity());
                ObjectData result = r2.get();
                checkResponse(data.array(), result);
                r2.freeBuffers();
                BufferPool.free(result.getData());
            }
        }
    }

    public void testIntermediateHoles() throws Exception {

        final FileCredentials fcred = new FileCredentials(getCap(FILE_ID).getXCap(), xloc);

        final ReusableBuffer data = SetupUtils.generateData(3);

        // write the nineth object, check the file size
        int obj = 8;
        ObjectData objdata = new ObjectData(0, false, 0, data.createViewBuffer());
        RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(obj % osdIDs.size()).getAddress(),
                FILE_ID, fcred, obj, 0, 0, 0, objdata);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(obj * SIZE + data.limit(), resp.getNew_file_size().get(0).getSize_in_bytes());

        // write the fifth object, check the file size
        obj = 5;

        objdata = new ObjectData(0, false, 0, data.createViewBuffer());
        r = client.write(osdIDs.get(obj % osdIDs.size()).getAddress(),
                FILE_ID, fcred, obj, 0, 0, 0, objdata);
        resp = r.get();
        r.freeBuffers();
        assertTrue((resp.getNew_file_size().size() == 0)
                || (resp.getNew_file_size().size() == 1)
                && (obj * SIZE + data.limit() == resp.getNew_file_size().get(0).getSize_in_bytes()));


        // check whether the first object consists of zeros
        obj = 0;
        RPCResponse<ObjectData> r2 = client.read(osdIDs.get(obj % osdIDs.size()).getAddress(), FILE_ID, fcred, obj, 0, 0, data.capacity());
        ObjectData result = r2.get();
        //either padding data or all zeros
        if (result.getZero_padding() == 0)
            checkResponse(ZEROS, result);
        else
            assertEquals(data.capacity(),result.getZero_padding());

        r2.freeBuffers();
        BufferPool.free(result.getData());

        // write the first object, check the file size header (must be null)
        objdata = new ObjectData(0, false, 0, data.createViewBuffer());
        r = client.write(osdIDs.get(obj % osdIDs.size()).getAddress(),
                FILE_ID, fcred, obj, 0, 0, 0, objdata);
        resp = r.get();
        r.freeBuffers();
        assertEquals(0,resp.getNew_file_size().size());
    }


    public void testWriteExtend() throws Exception {

        final FileCredentials fcred = new FileCredentials(getCap(FILE_ID).getXCap(), xloc);

        final ReusableBuffer data = SetupUtils.generateData(3);
        final byte[] paddedData = new byte[SIZE];
        System.arraycopy(data.array(), 0, paddedData, 0, data.limit());

        // write first object
        ObjectData objdata = new ObjectData(0, false, 0, data.createViewBuffer());
        RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(0).getAddress(),
                FILE_ID, fcred, 0, 0, 0, 0, objdata);
        OSDWriteResponse resp = r.get();
        r.freeBuffers();

        // write second object
        objdata = new ObjectData(0, false, 0, data.createViewBuffer());
        r = client.write(osdIDs.get(1).getAddress(),
                FILE_ID, fcred, 1, 0, 0, 0, objdata);
        resp = r.get();
        r.freeBuffers();

        // read first object

        RPCResponse<ObjectData> r2 = client.read(osdIDs.get(0).getAddress(), FILE_ID, fcred, 0, 0, 0, SIZE);
        ObjectData result = r2.get();
        System.out.println(result);
        //either padding data or all zeros
        assertNotNull(result.getData());
        assertEquals(3,result.getData().capacity());
        assertEquals(SIZE-3,result.getZero_padding());
        r2.freeBuffers();
        BufferPool.free(result.getData());

    }

    /**
     * tests the truncation of striped files
     */
    public void testTruncate() throws Exception {

        ReusableBuffer data = SetupUtils.generateData(SIZE);

        final FileCredentials fcred = new FileCredentials(getCap(FILE_ID).getXCap(), xloc);

        // -------------------------------
        // create a file with five objects
        // -------------------------------
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {
            ObjectData objdata = new ObjectData(0, false, 0, data.createViewBuffer());
            RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(osdIndex).getAddress(),
                    FILE_ID, fcred, i, 0, 0, 0, objdata);
            OSDWriteResponse resp = r.get();
            r.freeBuffers();
        }

        // ----------------------------------------------
        // shrink the file to a length of one full object
        // ----------------------------------------------

        fcred.getXcap().setTruncate_epoch(1);

        RPCResponse<OSDWriteResponse> rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID, fcred, SIZE);
        OSDWriteResponse resp = rt.get();
        rt.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(SIZE, resp.getNew_file_size().get(0).getSize_in_bytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), FILE_ID, fcred, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            r2.freeBuffers();

            // the first object must exist, all other ones must have been
            // deleted
            if (i == 0)
                checkResponse(data.array(), result);
            else
                checkResponse(null, result);

            BufferPool.free(result.getData());
        }

        // -------------------------------------------------
        // extend the file to a length of eight full objects
        // -------------------------------------------------
        fcred.getXcap().setTruncate_epoch(2);

        rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID, fcred, SIZE*8);
        resp = rt.get();
        rt.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(SIZE*8, resp.getNew_file_size().get(0).getSize_in_bytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 8; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), FILE_ID, fcred, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            r2.freeBuffers();

            // the first object must contain data, all other ones must contain
            // zeros
            if (i == 0)
                checkResponse(data.array(), result);
            else {
                if (result.getData().capacity() == 0) {
                    assertEquals(SIZE,result.getZero_padding());
                } else {
                    checkResponse(ZEROS, result);
                }

            }

            BufferPool.free(result.getData());
        }

        // ------------------------------------------
        // shrink the file to a length of 3.5 objects
        // ------------------------------------------
        fcred.getXcap().setTruncate_epoch(3);
        final long size3p5 = (long) (SIZE * 3.5f);
        rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID, fcred, size3p5);
        resp = rt.get();
        rt.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(size3p5, resp.getNew_file_size().get(0).getSize_in_bytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), FILE_ID, fcred, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            r2.freeBuffers();

            // the first object must contain data, all other ones must contain
            // zeros, where the last one must only be half an object size
            if (i == 0)
                checkResponse(data.array(), result);
            else if (i == 3)
                checkResponse(ZEROS_HALF, result);
            else if (i >= 4) {
                assertEquals(0,result.getZero_padding());
                assertEquals(0,result.getData().capacity());
            } else {
                if (result.getData().capacity() == 0) {
                    assertEquals(SIZE,result.getZero_padding());
                } else {
                    checkResponse(ZEROS, result);
                }
            }

            BufferPool.free(result.getData());
        }

        // --------------------------------------------------
        // truncate the file to the same length it had before
        // --------------------------------------------------
        fcred.getXcap().setTruncate_epoch(4);

        rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID, fcred, size3p5);
        resp = rt.get();
        rt.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(size3p5, resp.getNew_file_size().get(0).getSize_in_bytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), FILE_ID, fcred, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            r2.freeBuffers();

            // the first object must contain data, all other ones must contain
            // zeros, where the last one must only be half an object size
            if (i == 0)
                checkResponse(data.array(), result);
            else if (i == 3)
                checkResponse(ZEROS_HALF, result);
            else if (i >= 4) {
                assertEquals(0,result.getZero_padding());
                assertEquals(0,result.getData().capacity());
            } else {
                if (result.getData().capacity() == 0) {
                    assertEquals(SIZE,result.getZero_padding());
                } else {
                    checkResponse(ZEROS, result);
                }
            }

            BufferPool.free(result.getData());
        }

        // --------------------------------
        // truncate the file to zero length
        // --------------------------------
        fcred.getXcap().setTruncate_epoch(5);

        rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID, fcred, 0);
        resp = rt.get();
        rt.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(0, resp.getNew_file_size().get(0).getSize_in_bytes());

        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {

            // try to read the object
            RPCResponse<ObjectData> r2 = client.read(osdIDs.get(osdIndex).getAddress(), FILE_ID, fcred, i, 0, 0, SIZE);
            ObjectData result = r2.get();
            r2.freeBuffers();

            assertEquals(0,result.getZero_padding());
            assertEquals(0,result.getData().capacity());

            BufferPool.free(result.getData());
        }

        data = SetupUtils.generateData(5);

        // ----------------------------------
        // write new data to the first object
        // ----------------------------------

        ObjectData objdata = new ObjectData(0, false, 0, data.createViewBuffer());
        rt = client.write(osdIDs.get(0).getAddress(),
                FILE_ID, fcred, 0, 0, 0, 0, objdata);
        resp = rt.get();
        rt.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(5, resp.getNew_file_size().get(0).getSize_in_bytes());

        // ----------------------------------------------
        // extend the file to a length of one full object
        // ----------------------------------------------
        fcred.getXcap().setTruncate_epoch(6);

        rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID, fcred, SIZE);
        resp = rt.get();
        rt.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(SIZE, resp.getNew_file_size().get(0).getSize_in_bytes());


        // try to read the object
        RPCResponse<ObjectData> r2 = client.read(osdIDs.get(0).getAddress(), FILE_ID, fcred, 0, 0, 0, SIZE);
        ObjectData result = r2.get();
        r2.freeBuffers();

        // the object must contain data plus padding zeros

        final byte[] dataWithZeros = new byte[SIZE];
        System.arraycopy(data.array(), 0, dataWithZeros, 0, data.limit());

        checkResponse(dataWithZeros, result);
        BufferPool.free(result.getData());

        // ---------------------------------------------
        // shrink the file to a length of half an object
        // ---------------------------------------------
        fcred.getXcap().setTruncate_epoch(7);

        rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID, fcred, SIZE/2);
        resp = rt.get();
        rt.freeBuffers();
        assertEquals(1,resp.getNew_file_size().size());
        assertEquals(SIZE/2, resp.getNew_file_size().get(0).getSize_in_bytes());

        // try to read the object
        r2 = client.read(osdIDs.get(0).getAddress(), FILE_ID, fcred, 0, 0, 0, SIZE);
        result = r2.get();
        r2.freeBuffers();

        // the object must contain data plus padding zeros

        final byte[] dataWithHalfZeros = new byte[SIZE / 2];
        System.arraycopy(data.array(), 0, dataWithHalfZeros, 0, data.limit());

        checkResponse(dataWithHalfZeros, result);
        BufferPool.free(result.getData());
    }
    
    public void testInterleavedWriteAndTruncate() throws Exception {
        
        final int numIterations = 20;
        final int maxObject = 20;
        final int maxSize = maxObject * SIZE;
        final int numWrittenObjs = 5;
        
        final MRCDummy mrcDummy = new MRCDummy(capSecret);

        final FileCredentials fcred = new FileCredentials(getCap(FILE_ID).getXCap(), xloc);

        final List<RPCResponse> responses = new LinkedList<RPCResponse>();
        
        for (int l = 0; l < numIterations; l++) {
            
            Capability cap = mrcDummy.open('w');
            
            // randomly write 'numWrittenObjs' objects
            for (int i = 0; i < numWrittenObjs; i++) {
                
                final int objId = (int) (Math.random() * maxObject);
                final int osdIndex = objId % osdIDs.size();
                
                // write an object with a random amount of bytes
                final int size = (int) ((SIZE - 1) * Math.random()) + 1;
                ObjectData objdata = new ObjectData(0, false, 0, SetupUtils.generateData(size));
                RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(osdIndex).getAddress(),
                        FILE_ID, fcred, objId, 0, 0, 0, objdata);
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

            fcred.setXcap(mrcDummy.open('t').getXCap());

            // truncate the file
            long newSize = (long) (Math.random() * maxSize);
            RPCResponse<OSDWriteResponse> rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID, fcred, newSize);
            rt.registerListener(mrcDummy);
            rt.waitForResult();
            rt.freeBuffers();
            
            long fileSize = mrcDummy.getFileSize();
            
            // read the previously truncated objects, check size
            for (int i = 0; i < maxObject; i++) {
                RPCResponse<ObjectData> r2 = client.read(osdIDs.get(i % osdIDs.size()).getAddress(), FILE_ID, fcred, i, 0, 0, SIZE);
                ObjectData result = r2.get();
                r2.freeBuffers();
                
                // check inner objects - should be full
                if (i < fileSize / SIZE)
                    assertEquals(SIZE,result.getZero_padding()+result.getData().capacity());
                
                // check last object - should either be an EOF (null) or partial
                // object
                else if (i == fileSize / SIZE) {
                    if (fileSize % SIZE == 0)
                        assertEquals(0,result.getZero_padding()+result.getData().capacity());
                    else
                        assertEquals(fileSize % SIZE,result.getZero_padding()+result.getData().capacity());
                }

                // check outer objects - should be EOF (null)
                else
                    assertEquals(0,result.getZero_padding()+result.getData().capacity());
                
                BufferPool.free(result.getData());
            }
            
        }
        
    }
    
    /**
     * tests the deletion of striped files
     */
//    public void testDELETE() throws Exception {
//
//        final int numObjs = 5;
//
//        final FileCredentials fcred = new FileCredentials(xloc, getCap(FILE_ID).getXCap());
//
//        ReusableBuffer data = SetupUtils.generateData(SIZE);
//
//        // create all objects
//        for (int i = 0, osdIndex = 0; i < numObjs; i++, osdIndex = i % osdIDs.size()) {
//
//            ObjectData objdata = new ObjectData(data.createViewBuffer(), 0, 0, false);
//            RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(osdIndex).getAddress(),
//                    FILE_ID, fcred, i, 0, 0, 0, objdata);
//            r.get();
//        }
//
//        // delete the file
//        RPCResponse dr = client.unlink(osdIDs.get(0).getAddress(), FILE_ID, fcred);
//        dr.get();
//        dr.freeBuffers();
//    }
    
    public static void main(String[] args) {
        TestRunner.run(StripingTest.class);
    }
    
    /**
     * Checks whether the data array received with the response is equal to the
     * given one.
     * 
     * @param data
     *            the data array
     * @param response
     *            the response
     * @throws Exception
     */
    private void checkResponse(byte[] data, ObjectData response) throws Exception {
        
        if (data == null) {
            if (response.getData() != null)
                /*System.out.println("body (" + response.getBody().capacity() + "): "
                    + new String(response.getBody().array()));*/
            assertEquals(0,response.getData().remaining());
        }

        else {
            byte[] responseData = response.getData().array();
            assertEquals(data.length, responseData.length);
            for (int i = 0; i < data.length; i++)
                assertEquals(data[i], responseData[i]);
        }
    }
    
}
