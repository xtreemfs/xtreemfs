/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.SnapConfig;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class StripingTestCOW extends TestCase {
    
    private static final boolean COW = true;
    
    private TestEnvironment      testEnv;
    
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
            
            return new Capability(FILE_ID, 0, 60, System.currentTimeMillis(), "", (int) issuedEpoch, false,
                COW ? SnapConfig.SNAP_CONFIG_ACCESS_CURRENT : SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0,
                capSecret);
        }
        
        synchronized long getFileSize() {
            return fileSize;
        }
        
        @Override
        public void responseAvailable(RPCResponse<OSDWriteResponse> r) {
            try {
                
                OSDWriteResponse resp = r.get();
                System.out.println("fs-update: " + resp);
                
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
    
    private StripingPolicyImpl  sp;
    
    private XLocSet             xloc;
    
    /** Creates a new instance of StripingTest */
    public StripingTestCOW(String testName) throws IOException {
        super(testName);
        Logging.start(Logging.LEVEL_DEBUG);
        
        osdCfg1 = SetupUtils.createOSD1Config();
        osdCfg2 = SetupUtils.createOSD2Config();
        osdCfg3 = SetupUtils.createOSD3Config();
        
        capSecret = osdCfg1.getCapabilitySecret();
        
        sp = StripingPolicyImpl.getPolicy(new Replica(new StringSet(), 0, new StripingPolicy(
            StripingPolicyType.STRIPING_POLICY_RAID0, KB, 3)), 0);
        dirConfig = SetupUtils.createDIRConfig();
        
    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        FSUtils.delTree(new File(SetupUtils.TEST_DIR));
        
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
        
        ReplicaSet replicas = new ReplicaSet();
        StringSet osdset = new StringSet();
        osdset.add(SetupUtils.getOSD1UUID().toString());
        osdset.add(SetupUtils.getOSD2UUID().toString());
        osdset.add(SetupUtils.getOSD3UUID().toString());
        Replica r = new Replica(osdset, 0,
            new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, KB, 3));
        replicas.add(r);
        xloc = new XLocSet(0, replicas, "", 1);
        
    }
    
    private Capability getCap(String fname) {
        return new Capability(fname, 0, 60, System.currentTimeMillis(), "", 0, false,
            COW ? SnapConfig.SNAP_CONFIG_ACCESS_CURRENT : SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, capSecret);
    }
    
    protected void tearDown() throws Exception {
        
        testEnv.shutdown();
        
        osdServer.get(0).shutdown();
        osdServer.get(1).shutdown();
        osdServer.get(2).shutdown();
    }
    
    /* TODO: test delete/truncate epochs! */

    public void testInterleavedWriteAndTruncate() throws Exception {
        
        final int numIterations = 20;
        final int maxObject = 20;
        final int maxSize = maxObject * SIZE;
        final int numWrittenObjs = 5;
        
        final MRCDummy mrcDummy = new MRCDummy(capSecret);
        
        for (int k = 0; k < 3; k++) {
            
            final FileCredentials fcred = new FileCredentials(getCap(FILE_ID).getXCap(), xloc);
            
            final List<RPCResponse> responses = new LinkedList<RPCResponse>();
            
            for (int l = 0; l < numIterations; l++) {
                
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
                
                // wait until all write requests have been completed, i.e. all
                // file
                // size updates have been performed
                for (RPCResponse r : responses) {
                    r.waitForResult();
                    r.freeBuffers();
                }
                responses.clear();
                
                fcred.setXcap(mrcDummy.open('t').getXCap());
                
                // truncate the file
                long newSize = (long) (Math.random() * maxSize);
                RPCResponse<OSDWriteResponse> rt = client.truncate(osdIDs.get(0).getAddress(), FILE_ID,
                    fcred, newSize);
                rt.registerListener(mrcDummy);
                rt.waitForResult();
                rt.freeBuffers();
                
                long fileSize = mrcDummy.getFileSize();
                
                // read the previously truncated objects, check size
                for (int i = 0; i < maxObject; i++) {
                    RPCResponse<ObjectData> r2 = client.read(osdIDs.get(i % osdIDs.size()).getAddress(),
                        FILE_ID, fcred, i, 0, 0, SIZE);
                    ObjectData result = r2.get();
                    r2.freeBuffers();
                    
                    // check inner objects - should be full
                    if (i < fileSize / SIZE)
                        assertEquals(SIZE, result.getZero_padding() + result.getData().capacity());
                    
                    // check last object - should either be an EOF (null) or
                    // partial
                    // object
                    else if (i == fileSize / SIZE) {
                        if (fileSize % SIZE == 0)
                            assertEquals(0, result.getZero_padding() + result.getData().capacity());
                        else
                            assertEquals(fileSize % SIZE, result.getZero_padding()
                                + result.getData().capacity());
                    }

                    // check outer objects - should be EOF (null)
                    else
                        assertEquals(0, result.getZero_padding() + result.getData().capacity());
                    
                    BufferPool.free(result.getData());
                }
                
            }
            
            if (k != 2) {
                System.out.println("\n########## waiting 61s ##########\n");
                Thread.sleep(61000);
            }
            
        }
        
    }
    
    /*public static void main(String[] args) {
        TestRunner.run(StripingTestCOW.class);
    }*/
    
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
                /*
                 * System.out.println("body (" + response.getBody().capacity() +
                 * "): " + new String(response.getBody().array()));
                 */
                assertEquals(0, response.getData().remaining());
        }

        else {
            byte[] responseData = response.getData().array();
            assertEquals(data.length, responseData.length);
            for (int i = 0; i < data.length; i++)
                assertEquals(data[i], responseData[i]);
        }
    }
    
}
