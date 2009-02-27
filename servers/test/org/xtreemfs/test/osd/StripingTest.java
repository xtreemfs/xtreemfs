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
import java.util.StringTokenizer;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.RPCResponseListener;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;

public class StripingTest extends TestCase {
    
    static class MRCDummy implements RPCResponseListener {
        
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
            
            return new Capability(FILE_ID, "DebugCapability", issuedEpoch, capSecret);
        }
        
        synchronized long getFileSize() {
            return fileSize;
        }
        
        public synchronized void responseAvailable(RPCResponse response) {
            
            try {
                
                String newFileSizeString = response.getSpeedyRequest().responseHeaders
                        .getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
                
                if (newFileSizeString != null) {
                    
                    StringTokenizer st = new StringTokenizer(newFileSizeString, "[],");
                    long newFileSize = Long.parseLong(st.nextToken());
                    long epochNo = Long.parseLong(st.nextToken());
                    
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
    
    private final Capability    cap;
    
    private final DIRConfig     dirConfig;
    
    private final OSDConfig     osdCfg1;
    
    private final OSDConfig     osdCfg2;
    
    private final OSDConfig     osdCfg3;
    
    private final String        capSecret;
    
    private List<OSD>           osdServer;
    
    private List<ServiceUUID>   osdIDs;
    
    private OSDClient           client;
    
    private Locations           loc;
    
    private StripingPolicy      sp;
    
    private RequestController   dir;
    
    private DIRClient           dirClient;
    
    /** Creates a new instance of StripingTest */
    public StripingTest(String testName) throws IOException {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);
        
        osdCfg1 = SetupUtils.createOSD1Config();
        osdCfg2 = SetupUtils.createOSD2Config();
        osdCfg3 = SetupUtils.createOSD3Config();
        
        capSecret = osdCfg1.getCapabilitySecret();
        cap = new Capability(FILE_ID, "DebugCapability", 0, capSecret);
        
        sp = new RAID0(KB, 3);
        dirConfig = SetupUtils.createDIRConfig();
    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        FSUtils.delTree(new File(SetupUtils.TEST_DIR));
        
        dir = new RequestController(dirConfig);
        dir.startup();
        
        dirClient = SetupUtils.initTimeSync();
        
        osdIDs = new ArrayList<ServiceUUID>(3);
        osdIDs.add(SetupUtils.getOSD1UUID());
        osdIDs.add(SetupUtils.getOSD2UUID());
        osdIDs.add(SetupUtils.getOSD3UUID());
        
        osdServer = new ArrayList<OSD>(3);
        osdServer.add(new OSD(osdCfg1));
        osdServer.add(new OSD(osdCfg2));
        osdServer.add(new OSD(osdCfg3));
        
        client = SetupUtils.createOSDClient(10000);
        
        List<Location> locations = new ArrayList<Location>(1);
        
        List<ServiceUUID> osd = new ArrayList<ServiceUUID>(3);
        for (ServiceUUID oid : osdIDs)
            osd.add(oid);
        locations.add(new Location(sp, osd));
        loc = new Locations(locations);
        
        SetupUtils.setupLocalResolver();
    }
    
    protected void tearDown() throws Exception {
        
        client.shutdown();
        client.waitForShutdown();
        
        if (dirClient != null) {
            dirClient.shutdown();
            dirClient.waitForShutdown();
        }
        
        osdServer.get(0).shutdown();
        osdServer.get(1).shutdown();
        osdServer.get(2).shutdown();
        dir.shutdown();
    }
    
    /* TODO: test delete/truncate epochs! */

    /**
     * tests reading and writing of striped files
     */
    public void testPUTandGET() throws Exception {
        
        final int numObjs = 5;
        final int[] testSizes = { 1, 2, SIZE - 1, SIZE };
        
        for (int ts : testSizes) {
            
            ReusableBuffer data = SetupUtils.generateData(ts);
            String file = "1:1" + ts;
            
            for (int i = 0, osdIndex = 0; i < numObjs; i++, osdIndex = i % osdIDs.size()) {
                
                // write an object with the given test size
                RPCResponse resp = client.put(osdIDs.get(osdIndex).getAddress(), loc, cap, file, i,
                    data);
                resp.waitForResponse();
                
                String fileSizeHeader = resp.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
                String fileSizeString = fileSizeHeader.substring(1, fileSizeHeader.indexOf(','));
                assertEquals(i * SIZE + ts, Integer.parseInt(fileSizeString));
                resp.freeBuffers();
                
                // read and check the previously written object
                resp = client.get(osdIDs.get(osdIndex).getAddress(), loc, cap, file, i);
                assertEquals(HTTPUtils.SC_OKAY, resp.getStatusCode());
                checkResponse(data.array(), resp);
                resp.freeBuffers();
            }
        }
    }
    
    public void testIntermediateHoles() throws Exception {
        
        final ReusableBuffer data = SetupUtils.generateData(3);
        
        // write the nineth object, check the file size
        int obj = 8;
        RPCResponse response = client.put(osdIDs.get(obj % osdIDs.size()).getAddress(), loc, cap,
            FILE_ID, obj, data);
        assertEquals("[" + (obj * SIZE + data.limit()) + ",0]", response.getHeaders().getHeader(
            HTTPHeaders.HDR_XNEWFILESIZE));
        response.freeBuffers();
        
        // write the fifth object, check the file size
        obj = 5;
        response = client.put(osdIDs.get(obj % osdIDs.size()).getAddress(), loc, cap, FILE_ID, obj, data);
        
        // file size header may be either null or 4 * size + data.length,
        // depending on whether the globalmax message was received already
        String xNewFileSize = response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
        assertTrue(xNewFileSize == null || xNewFileSize.equals((obj * SIZE + data.limit()) + ""));
        response.freeBuffers();
        
        // check whether the first object consists of zeros
        obj = 0;
        response = client.get(osdIDs.get(obj % osdIDs.size()).getAddress(), loc, cap, FILE_ID, obj);
        checkResponse(ZEROS, response);
        response.freeBuffers();
        
        // write the first object, check the file size header (must be null)
        response = client.put(osdIDs.get(obj % osdIDs.size()).getAddress(), loc, cap, FILE_ID, obj, data);
        assertNull(response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
        response.freeBuffers();
    }
    
    public void testWriteExtend() throws Exception {
        
        final ReusableBuffer data = SetupUtils.generateData(3);
        final byte[] paddedData = new byte[SIZE];
        System.arraycopy(data.array(), 0, paddedData, 0, data.limit());
        
        // write first object
        RPCResponse response = client.put(osdIDs.get(0).getAddress(), loc, cap, FILE_ID, 0, data);
        response.waitForResponse();
        response.freeBuffers();
        
        // write second object
        response = client.put(osdIDs.get(1).getAddress(), loc, cap, FILE_ID, 1, data);
        response.waitForResponse();
        response.freeBuffers();
        
        // read first object
        response = client.get(osdIDs.get(0).getAddress(), loc, cap, FILE_ID, 0);
        response.waitForResponse();
        response.freeBuffers();
        
        // check whether the first object consists of zeros, except for the
        // first character
        response = client.get(osdIDs.get(0).getAddress(), loc, cap, FILE_ID, 0);
        checkResponse(paddedData, response);
        response.freeBuffers();
    }
    
    /**
     * tests the truncation of striped files
     */
    public void testTruncate() throws Exception {
        
        ReusableBuffer data = SetupUtils.generateData(SIZE);
        
        // -------------------------------
        // create a file with five objects
        // -------------------------------
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {
            
            RPCResponse tmp = client.put(osdIDs.get(osdIndex).getAddress(), loc, cap, FILE_ID, i, data);
            tmp.waitForResponse();
            tmp.freeBuffers();
        }
        
        // ----------------------------------------------
        // shrink the file to a length of one full object
        // ----------------------------------------------
        
        Capability truncateCap1 = new Capability(FILE_ID, "DebugCapability", 1, capSecret);
        
        RPCResponse resp = client.truncate(osdIDs.get(0).getAddress(), loc, truncateCap1, FILE_ID,
            SIZE);
        resp.waitForResponse();
        resp.freeBuffers();
        
        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {
            
            // try to read the object
            resp = client.get(osdIDs.get(osdIndex).getAddress(), loc, cap, FILE_ID, i);
            
            // the first object must exist, all other ones must have been
            // deleted
            if (i == 0)
                checkResponse(data.array(), resp);
            else
                checkResponse(null, resp);
            
            resp.freeBuffers();
        }
        
        // -------------------------------------------------
        // extend the file to a length of eight full objects
        // -------------------------------------------------
        Capability truncateCap2 = new Capability(FILE_ID, "DebugCapability", 2, capSecret);
        
        resp = client.truncate(osdIDs.get(0).getAddress(), loc, truncateCap2, FILE_ID, SIZE * 8);
        resp.waitForResponse();
        resp.freeBuffers();
        
        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 8; i++, osdIndex = i % osdIDs.size()) {
            
            // try to read the object
            resp = client.get(osdIDs.get(osdIndex).getAddress(), loc, cap, FILE_ID, i);
            
            // the first object must contain data, all other ones must contain
            // zeros
            if (i == 0)
                checkResponse(data.array(), resp);
            else
                checkResponse(ZEROS, resp);
            
            resp.freeBuffers();
        }
        
        // ------------------------------------------
        // shrink the file to a length of 3.5 objects
        // ------------------------------------------
        Capability truncateCap3 = new Capability(FILE_ID, "DebugCapability", 3, capSecret);
        
        resp = client.truncate(osdIDs.get(0).getAddress(), loc, truncateCap3, FILE_ID,
            (long) (SIZE * 3.5f));
        resp.waitForResponse();
        resp.freeBuffers();
        
        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {
            
            // try to read the object
            resp = client.get(osdIDs.get(osdIndex).getAddress(), loc, cap, FILE_ID, i);
            
            // the first object must contain data, all other ones must contain
            // zeros, where the last one must only be half an object size
            if (i == 0)
                checkResponse(data.array(), resp);
            else if (i == 3)
                checkResponse(ZEROS_HALF, resp);
            else if (i >= 4)
                checkResponse(null, resp);
            else
                checkResponse(ZEROS, resp);
            
            resp.freeBuffers();
        }
        
        // --------------------------------------------------
        // truncate the file to the same length it had before
        // --------------------------------------------------
        Capability truncateCap4 = new Capability(FILE_ID, "DebugCapability", 4, capSecret);
        
        resp = client.truncate(osdIDs.get(0).getAddress(), loc, truncateCap4, FILE_ID,
            (long) (SIZE * 3.5f));
        resp.waitForResponse();
        resp.freeBuffers();
        
        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {
            
            // try to read the object
            resp = client.get(osdIDs.get(osdIndex).getAddress(), loc, cap, FILE_ID, i);
            
            // the first object must contain data, all other ones must contain
            // zeros, where the last one must only be half an object size
            if (i == 0)
                checkResponse(data.array(), resp);
            else if (i == 3)
                checkResponse(ZEROS_HALF, resp);
            else if (i >= 4)
                checkResponse(null, resp);
            else
                checkResponse(ZEROS, resp);
            
            resp.freeBuffers();
        }
        
        // --------------------------------
        // truncate the file to zero length
        // --------------------------------
        Capability truncateCap5 = new Capability(FILE_ID, "DebugCapability", 5, capSecret);
        
        resp = client.truncate(osdIDs.get(0).getAddress(), loc, truncateCap5, FILE_ID, 0);
        resp.waitForResponse();
        resp.freeBuffers();
        
        // check whether all objects have the expected content
        for (int i = 0, osdIndex = 0; i < 5; i++, osdIndex = i % osdIDs.size()) {
            
            // try to read the object
            resp = client.get(osdIDs.get(osdIndex).getAddress(), loc, cap, FILE_ID, i);
            
            // none of the objects must contain data
            checkResponse(null, resp);
            resp.freeBuffers();
        }
        
        data = SetupUtils.generateData(5);
        
        // ----------------------------------
        // write new data to the first object
        // ----------------------------------
        resp = client.put(osdIDs.get(0).getAddress(), loc, truncateCap5, FILE_ID, 0, data);
        resp.waitForResponse();
        resp.freeBuffers();
        
        // ----------------------------------------------
        // extend the file to a length of one full object
        // ----------------------------------------------
        Capability truncateCap6 = new Capability(FILE_ID, "DebugCapability", 6, capSecret);
        
        resp = client.truncate(osdIDs.get(0).getAddress(), loc, truncateCap6, FILE_ID, SIZE);
        resp.waitForResponse();
        resp.freeBuffers();
        
        // try to read the object
        resp = client.get(osdIDs.get(0).getAddress(), loc, cap, FILE_ID, 0);
        
        // the object must contain data plus padding zeros
        
        final byte[] dataWithZeros = new byte[SIZE];
        System.arraycopy(data.array(), 0, dataWithZeros, 0, data.limit());
        
        checkResponse(dataWithZeros, resp);
        resp.freeBuffers();
        
        // ---------------------------------------------
        // shrink the file to a length of half an object
        // ---------------------------------------------
        Capability truncateCap7 = new Capability(FILE_ID, "DebugCapability", 7, capSecret);
        
        resp = client.truncate(osdIDs.get(0).getAddress(), loc, truncateCap7, FILE_ID, SIZE / 2);
        resp.waitForResponse();
        resp.freeBuffers();
        
        // try to read the object
        resp = client.get(osdIDs.get(0).getAddress(), loc, cap, FILE_ID, 0);
        
        // the object must contain data plus padding zeros
        
        final byte[] dataWithHalfZeros = new byte[SIZE / 2];
        System.arraycopy(data.array(), 0, dataWithHalfZeros, 0, data.limit());
        
        checkResponse(dataWithHalfZeros, resp);
        resp.freeBuffers();
    }
    
    public void testInterleavedWriteAndTruncate() throws Exception {
        
        final int numIterations = 20;
        final int maxObject = 20;
        final int maxSize = maxObject * SIZE;
        final int numWrittenObjs = 5;
        
        final MRCDummy mrcDummy = new MRCDummy(capSecret);
        final List<RPCResponse> responses = new LinkedList<RPCResponse>();
        
        for (int l = 0; l < numIterations; l++) {
            
            Capability cap = mrcDummy.open('w');
            
            // randomly write 'numWrittenObjs' objects
            for (int i = 0; i < numWrittenObjs; i++) {
                
                int objId = (int) (Math.random() * maxObject);
                int osdIndex = objId % osdIDs.size();
                
                // write an object with a random amount of bytes
                int size = (int) ((SIZE - 1) * Math.random()) + 1;
                RPCResponse resp = client.put(osdIDs.get(osdIndex).getAddress(), loc, cap, FILE_ID,
                    objId, SetupUtils.generateData(size));
                responses.add(resp);
                
                // update the file size when the response is received
                resp.setResponseListener(mrcDummy);
            }
            
            cap = mrcDummy.open('t');
            
            // truncate the file
            long newSize = (long) (Math.random() * maxSize);
            RPCResponse resp = client.truncate(osdIDs.get(0).getAddress(), loc, cap, FILE_ID,
                newSize);
            resp.setResponseListener(mrcDummy);
            resp.waitForResponse();
            resp.freeBuffers();
            
            // wait until all write requests have been completed, i.e. all file
            // size updates have been performed
            for (RPCResponse r : responses) {
                r.waitForResponse();
                r.freeBuffers();
            }
            responses.clear();
            
            long fileSize = mrcDummy.getFileSize();
            
            // read the previously truncated objects, check size
            for (int i = 0; i < maxObject; i++) {
                resp = client.get(osdIDs.get(i % osdIDs.size()).getAddress(), loc, cap, FILE_ID, i);
                resp.waitForResponse();
                
                // check inner objects - should be full
                if (i < fileSize / SIZE)
                    assertEquals(SIZE + "", resp.getHeaders().getHeader(
                        HTTPHeaders.HDR_CONTENT_LENGTH));
                
                // check last object - should either be an EOF (null) or partial
                // object
                else if (i == fileSize / SIZE) {
                    if (fileSize % SIZE == 0)
                        assertEquals(null, resp.getBody());
                    else
                        assertEquals((fileSize % SIZE) + "", resp.getHeaders().getHeader(
                            HTTPHeaders.HDR_CONTENT_LENGTH));
                }

                // check outer objects - should be EOF (null)
                else
                    assertEquals(null, resp.getBody());
                
                resp.freeBuffers();
            }
            
        }
        
    }
    
    /**
     * tests the deletion of striped files
     */
    public void testDELETE() throws Exception {
        
        final int numObjs = 5;
        
        ReusableBuffer data = SetupUtils.generateData(SIZE);
        
        // create all objects
        for (int i = 0, osdIndex = 0; i < numObjs; i++, osdIndex = i % osdIDs.size()) {
            
            RPCResponse tmp = client.put(osdIDs.get(osdIndex).getAddress(), loc, cap, FILE_ID, i,
                data);
            tmp.waitForResponse();
            tmp.freeBuffers();
        }
        
        Capability deleteCap = new Capability(FILE_ID, "DebugCapability", 1, capSecret);
        
        // delete the file
        RPCResponse resp = client.delete(osdIDs.get(0).getAddress(), loc, deleteCap, FILE_ID);
        resp.waitForResponse();
        resp.freeBuffers();
    }
    
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
    private void checkResponse(byte[] data, RPCResponse response) throws Exception {
        
        if (data == null) {
            if (response.getBody() != null)
                System.out.println("body (" + response.getBody().capacity() + "): "
                    + new String(response.getBody().array()));
            assertNull(response.getBody());
        }

        else {
            byte[] responseData = response.getBody().getData();
            assertEquals(data.length, responseData.length);
            for (int i = 0; i < data.length; i++)
                assertEquals(data[i], responseData[i]);
        }
    }
    
}
