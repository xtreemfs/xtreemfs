/*
 * Copyright (c) 2010-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import static org.junit.Assert.assertEquals;

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
import org.xtreemfs.test.osd.StripingTest.MRCDummy;

public class StripingTestCOW {
    @Rule
    public final TestRule        testLog    = TestHelper.testLog;
    
    private static final boolean COW = true;
    
    private TestEnvironment      testEnv;
    
    /*static class MRCDummy implements RPCResponseAvailableListener<OSDWriteResponse> {
        
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
                
                if (resp.getSizeInBytesCount() > 0) {
                    
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
        
    }*/
    
    private static final String FILE_ID    = "1:1";
    
    private static final int    KB         = 1;
    
    private static final int    SIZE       = KB * 1024;
    
    private static final byte[] ZEROS_HALF = new byte[SIZE / 2];
    
    private static final byte[] ZEROS      = new byte[SIZE];
    
    private static DIRConfig     dirConfig;
    
    private static OSDConfig     osdCfg1;
    
    private static OSDConfig     osdCfg2;
    
    private static OSDConfig     osdCfg3;
    
    private static String        capSecret;
    
    private List<OSD>           osdServer;
    
    private List<ServiceUUID>   osdIDs;
    
    private OSDServiceClient    client;
    
    private StripingPolicyImpl  sp;
    
    private XLocSet              xloc;
    
    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        
        osdCfg1 = SetupUtils.createOSD1Config();
        osdCfg2 = SetupUtils.createOSD2Config();
        osdCfg3 = SetupUtils.createOSD3Config();
        
        capSecret = osdCfg1.getCapabilitySecret();
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
        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(3, KB)).setReplicationFlags(0).addAllOsdUuids(osdset).build();
        xloc = XLocSet.newBuilder().setReadOnlyFileSize(0).setVersion(1).addReplicas(r).setReplicaUpdatePolicy("").build();
        
    }
    
    private Capability getCap(String fname) {
        return new Capability(fname, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber(), 60, System.currentTimeMillis(), "", 0, false,
                COW ? SnapConfig.SNAP_CONFIG_ACCESS_CURRENT : SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, capSecret, 0);
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
    public void testInterleavedWriteAndTruncate() throws Exception {
        
        final int numIterations = 20;
        final int maxObject = 20;
        final int maxSize = maxObject * SIZE;
        final int numWrittenObjs = 5;
        
        final MRCDummy mrcDummy = new MRCDummy(capSecret);
        
        for (int k = 0; k < 3; k++) {
            
            FileCredentials fcred = FileCredentials.newBuilder().setXcap(getCap(FILE_ID).getXCap()).setXlocs(xloc).build();
            
            final List<RPCResponse> responses = new LinkedList<RPCResponse>();
            
            for (int l = 0; l < numIterations; l++) {
                
                // randomly write 'numWrittenObjs' objects
                for (int i = 0; i < numWrittenObjs; i++) {
                    
                    final int objId = (int) (Math.random() * maxObject);
                    final int osdIndex = objId % osdIDs.size();
                    
                    // write an object with a random amount of bytes
                    final int size = (int) ((SIZE - 1) * Math.random()) + 1;
                    ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false).build();
                    RPCResponse<OSDWriteResponse> r = client.write(osdIDs.get(osdIndex).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                        fcred, FILE_ID, objId, 0, 0, 0, objdata, SetupUtils.generateData(size));
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

                fcred = fcred.toBuilder().setXcap(mrcDummy.open('t').getXCap()).build();
                
                // truncate the file
                long newSize = (long) (Math.random() * maxSize);
                RPCResponse<OSDWriteResponse> rt = client.truncate(osdIDs.get(0).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                        fcred, FILE_ID, newSize);
                rt.registerListener(mrcDummy);
                rt.waitForResult();
                rt.freeBuffers();
                
                long fileSize = mrcDummy.getFileSize();
                
                // read the previously truncated objects, check size
                for (int i = 0; i < maxObject; i++) {
                    RPCResponse<ObjectData> r2 = client.read(osdIDs.get(i % osdIDs.size()).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                        fcred, FILE_ID, i, 0, 0, SIZE);
                    ObjectData result = r2.get();
                    ReusableBuffer dataOut = r2.getData();
                    int dataOutLen = (dataOut == null) ? 0 : dataOut.capacity();


                    // check inner objects - should be full
                    if (i < fileSize / SIZE)
                        assertEquals(SIZE,result.getZeroPadding()+dataOutLen);

                    // check last object - should either be an EOF (null) or partial
                    // object
                    else if (i == fileSize / SIZE) {
                        if (fileSize % SIZE == 0)
                            assertEquals(0,result.getZeroPadding()+dataOutLen);
                        else
                            assertEquals(fileSize % SIZE,result.getZeroPadding()+dataOutLen);
                    }

                    // check outer objects - should be EOF (null)
                    else
                        assertEquals(0,result.getZeroPadding()+dataOutLen);


                        r2.freeBuffers();
                    }
                
            }
            
            if (k != 2) {
                System.out.println("\n########## waiting 61s ##########\n");
                Thread.sleep(61000);
            }
            
        }
        
    }
    
}
