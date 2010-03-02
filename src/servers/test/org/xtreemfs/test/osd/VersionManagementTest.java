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

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
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

public class VersionManagementTest extends TestCase {
    
    private TestEnvironment     testEnv;
    
    private static final String FILE_ID  = "1:1";
    
    private static final int    KB       = 1;
    
    private static final int    OBJ_SIZE = KB * 1024;
    
    private final OSDConfig     osdCfg;
    
    private final String        capSecret;
    
    private OSD                 osdServer;
    
    private ServiceUUID         osdId;
    
    private OSDClient           client;
    
    private StripingPolicyImpl  sp;
    
    private XLocSet             xloc;
    
    /** Creates a new instance of StripingTest */
    public VersionManagementTest(String testName) throws IOException {
        
        super(testName);
        Logging.start(Logging.LEVEL_DEBUG);
        
        osdCfg = SetupUtils.createOSD1Config();
        capSecret = osdCfg.getCapabilitySecret();
        
        sp = StripingPolicyImpl.getPolicy(new Replica(new StringSet(), 0, new StripingPolicy(
            StripingPolicyType.STRIPING_POLICY_RAID0, KB, 3)), 0);
        
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
        
        ReplicaSet replicas = new ReplicaSet();
        StringSet osdset = new StringSet();
        osdset.add(SetupUtils.getOSD1UUID().toString());
        Replica r = new Replica(osdset, 0,
            new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, KB, 1));
        replicas.add(r);
        xloc = new XLocSet(0, replicas, "", 1);
        
    }
    
    private FileCredentials getFileCredentials(int truncateEpoch, boolean write) {
        return new FileCredentials(new Capability(FILE_ID, write ? Constants.SYSTEM_V_FCNTL_H_O_WRONLY
            : Constants.SYSTEM_V_FCNTL_H_O_RDONLY, 60, System.currentTimeMillis(), "", truncateEpoch, false,
            SnapConfig.SNAP_CONFIG_ACCESS_CURRENT, 0, capSecret).getXCap(), xloc);
    }
    
    private FileCredentials getFileCredentials(int truncateEpoch, long snapTimestamp) {
        return new FileCredentials(new Capability(FILE_ID, 0, 60, System.currentTimeMillis(), "",
            truncateEpoch, false, SnapConfig.SNAP_CONFIG_ACCESS_SNAP, snapTimestamp, capSecret).getXCap(),
            xloc);
    }
    
    protected void tearDown() throws Exception {
        testEnv.shutdown();
        osdServer.shutdown();
    }
    
    public void testTruncate() throws Exception {
        
        FileCredentials fcred = getFileCredentials(1, true);
        
        // write a new file
        ObjectData objdata = new ObjectData(0, false, 0, SetupUtils.generateData(OBJ_SIZE, (byte) 'x'));
        RPCResponse<OSDWriteResponse> r = client.write(osdId.getAddress(), FILE_ID, fcred, 5, 0, 0, 0,
            objdata);
        r.get();
        r.freeBuffers();
        
        // truncate-extend the file and read it
        RPCResponse<OSDWriteResponse> r1 = client.truncate(osdId.getAddress(), FILE_ID, fcred, OBJ_SIZE * 8);
        r1.get();
        r1.freeBuffers();
        
        RPCResponse<ObjectData> r2 = client.read(osdId.getAddress(), FILE_ID, fcred, 5, 0, 0, OBJ_SIZE);
        ObjectData result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 'x');
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, fcred, 7, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 0);
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, fcred, 0, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 0);
        r2.freeBuffers();
        
        // truncate-shrink the file and read it
        fcred = getFileCredentials(2, true);
        
        r1 = client.truncate(osdId.getAddress(), FILE_ID, fcred, OBJ_SIZE * 1);
        r1.get();
        r1.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, fcred, 0, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 0);
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, fcred, 5, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, 0, (byte) 0);
        r2.freeBuffers();
        
    }
    
    public void testImplicitVersionCreation() throws Exception {
        
        final long timeoutSpan = 61000;
        
        final long t0 = System.currentTimeMillis();
        
        FileCredentials wCred = getFileCredentials(1, true);
        FileCredentials rCred = getFileCredentials(1, false);
        
        // write a new file that consists of two objects
        ObjectData objdata = new ObjectData(0, false, 0, SetupUtils.generateData(OBJ_SIZE, (byte) 'x'));
        RPCResponse<OSDWriteResponse> r = client.write(osdId.getAddress(), FILE_ID, wCred, 0, 0, 0, 0,
            objdata);
        r.get();
        r.freeBuffers();
        
        objdata = new ObjectData(0, false, 0, SetupUtils.generateData(OBJ_SIZE, (byte) 'y'));
        r = client.write(osdId.getAddress(), FILE_ID, wCred, 1, 0, 0, 0, objdata);
        r.get();
        r.freeBuffers();
        
        // wait for OSD-internal file close, which will implicitly cause a new
        // version to be created
        Thread.sleep(timeoutSpan);
        
        // overwrite the first object
        objdata = new ObjectData(0, false, 0, SetupUtils.generateData(OBJ_SIZE, (byte) 'z'));
        r = client.write(osdId.getAddress(), FILE_ID, wCred, 0, 0, 0, 0, objdata);
        r.get();
        r.freeBuffers();
        
        final long t1 = System.currentTimeMillis();
        
        // read and check the old version
        FileCredentials rCredV = getFileCredentials(0, t1);
        
        RPCResponse<ObjectData> r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 0, 0, 0, OBJ_SIZE);
        ObjectData result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 'x');
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 1, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 'y');
        r2.freeBuffers();
        
        // read and check the current version
        r2 = client.read(osdId.getAddress(), FILE_ID, rCred, 0, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 'z');
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCred, 1, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 'y');
        r2.freeBuffers();
        
        // try to read a non-existing version
        rCredV = getFileCredentials(0, t0 - 999999);
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 0, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, 0, (byte) 0);
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 1, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, 0, (byte) 0);
        r2.freeBuffers();
        
        // truncate the file to zero length and wait, so that a new version is
        // created and append another object
        FileCredentials tCred = getFileCredentials(1, true);
        r = client.truncate(osdId.getAddress(), FILE_ID, tCred, 0);
        r.get();
        r.freeBuffers();
        
        Thread.sleep(timeoutSpan);
        
        objdata = new ObjectData(0, false, 0, SetupUtils.generateData(OBJ_SIZE, (byte) 'w'));
        r = client.write(osdId.getAddress(), FILE_ID, wCred, 2, 0, 0, 0, objdata);
        r.get();
        r.freeBuffers();
        
        final long t2 = System.currentTimeMillis();
        
        // read and check the current version
        r2 = client.read(osdId.getAddress(), FILE_ID, rCred, 0, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 0);
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCred, 1, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 0);
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCred, 2, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 'w');
        r2.freeBuffers();
        
        // read and check the version at t1
        rCredV = getFileCredentials(0, t1);
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 0, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 'x');
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 1, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, OBJ_SIZE, (byte) 'y');
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 2, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, 0, (byte) 0);
        r2.freeBuffers();
        
        // read and check the version at t2
        rCredV = getFileCredentials(0, t2);
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 0, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, 0, (byte) 0);
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 1, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, 0, (byte) 0);
        r2.freeBuffers();
        
        r2 = client.read(osdId.getAddress(), FILE_ID, rCredV, 2, 0, 0, OBJ_SIZE);
        result = r2.get();
        checkData(result, 0, (byte) 0);
        r2.freeBuffers();
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(VersionManagementTest.class);
    }
    
    private void checkData(ObjectData data, long size, byte content) throws Exception {
        
        assertEquals(size, data.getData().capacity() + data.getZero_padding());
        for (byte b : data.getData().array())
            assertEquals(content, b);
        
    }
    
}
