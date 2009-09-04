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
 * AUTHORS: Nele Andersen (ZIB), Björn Kolbeck (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.test.io;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class RandomAccessFileTest extends TestCase {
    RandomAccessFile              randomAccessFile;
    
    private MRCRequestDispatcher  mrc1;
    
    private MRCConfig             mrcCfg1;
    
    private OSDConfig             osdConfig1, osdConfig2, osdConfig3, osdConfig4;
    
    private DIRConfig             dsCfg;
    
    private OSD                   osd1, osd2, osd3, osd4;
    
    private InetSocketAddress     mrc1Address;
    
    private String                authString;
    
    private String                volumeName;
    
    private TestEnvironment       testEnv;
    
    private final String          userID = "test";
    
    private final List<String>    groupIDs;
    
    private final UserCredentials uc;
    
    public RandomAccessFileTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        groupIDs = new ArrayList(1);
        groupIDs.add("test");
        uc = MRCClient.getCredentials(userID, groupIDs);
    }
    
    public void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
            TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.TIME_SYNC,
            TestEnvironment.Services.UUID_RESOLVER, TestEnvironment.Services.DIR_SERVICE });
        testEnv.start();
        
        dsCfg = SetupUtils.createDIRConfig();
        
        mrcCfg1 = SetupUtils.createMRC1Config();
        mrc1Address = SetupUtils.getMRC1Addr();
        
        osdConfig1 = SetupUtils.createOSD1Config();
        osdConfig2 = SetupUtils.createOSD2Config();
        osdConfig3 = SetupUtils.createOSD3Config();
        osdConfig4 = SetupUtils.createOSD4Config();
        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);
        
        FSUtils.delTree(testDir);
        testDir.mkdirs();
        
        // start the OSDs
        osd1 = new OSD(osdConfig1);
        osd2 = new OSD(osdConfig2);
        osd3 = new OSD(osdConfig3);
        osd4 = new OSD(osdConfig4);
        
        // start MRC
        mrc1 = new MRCRequestDispatcher(mrcCfg1);
        mrc1.startup();
        
        volumeName = "testVolume";
        
        // create a volume (no access control)
        RPCResponse r = testEnv.getMrcClient().mkvol(mrc1Address, uc, volumeName,
            new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1),
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL.intValue(), 0);
        r.get();
        
        // create some files and directories
        // testEnv.getMrcClient().createDir(mrc1Address, volumeName + "/myDir",
        // authString);
        r = testEnv.getMrcClient().mkdir(mrc1Address, uc, volumeName + "/myDir", 0);
        r.get();
        
        for (int i = 0; i < 10; i++) {
            r = testEnv.getMrcClient().create(mrc1Address, uc, volumeName + "/myDir/test" + i + ".txt", 0);
            r.get();
        }
        // testEnv.getMrcClient().createFile(mrc1Address, volumeName +
        // "/myDir/test" + i + ".txt", authString);
        
    }
    
    public void tearDown() throws Exception {
        mrc1.shutdown();
        osd1.shutdown();
        osd2.shutdown();
        osd3.shutdown();
        osd4.shutdown();
        
        testEnv.shutdown();
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }
    
    public void testReadAndWrite() throws Exception {
        // randomAccessFile = new RandomAccessFile("w", mrc1Address, volumeName
        // + "/myDir/test1.txt", testEnv.getSpeedy());
        randomAccessFile = new RandomAccessFile("rw", mrc1Address, volumeName + "/myDir/test1.txt", testEnv
                .getRpcClient(), userID, groupIDs);
        
        byte[] bytesIn = new byte[(int) (3 * randomAccessFile.getStripeSize() + 2)];
        for (int i = 0; i < 3 * randomAccessFile.getStripeSize() + 2; i++) {
            bytesIn[i] = (byte) (i % 25 + 65);
        }
        int length = bytesIn.length;
        int result = randomAccessFile.write(bytesIn, 0, length);
        assertEquals(length, result);
        
        byte[] bytesOut = new byte[length];
        result = randomAccessFile.read(bytesOut, 0, length);
        
        assertEquals(0, result);
        
        bytesOut = new byte[length];
        randomAccessFile.seek(0);
        result = randomAccessFile.read(bytesOut, 0, length);
        
        assertEquals(length, result);
        assertEquals(new String(bytesIn), new String(bytesOut));
        
        bytesOut = new byte[4];
        
        bytesIn = "Hello World".getBytes();
        randomAccessFile.seek(0);
        randomAccessFile.write(bytesIn, 0, bytesIn.length);
        
        randomAccessFile.seek(0);
        result = randomAccessFile.read(bytesOut, 0, 4);
        assertEquals(new String(bytesOut), new String("Hell"));
        
        randomAccessFile.seek(1);
        bytesOut = new byte[4];
        result = randomAccessFile.read(bytesOut, 0, 4);
        assertEquals(new String(bytesOut), new String("ello"));
        
    }
    
    public void testReadAndWriteObject() throws Exception {
        randomAccessFile = new RandomAccessFile("rw", mrc1Address, volumeName + "/myDir/test4.txt", testEnv
                .getRpcClient(), userID, groupIDs);
        
        byte[] bytesIn = new String("Hallo").getBytes();
        int length = bytesIn.length;
        ReusableBuffer data = ReusableBuffer.wrap(bytesIn);
        randomAccessFile.writeObject(0, 0, data);
        
        ReusableBuffer result = randomAccessFile.readObject(0, 0, length);
        assertEquals(new String(bytesIn), new String(result.array()));
        int bytesRead = randomAccessFile.checkObject(0l);
        assertEquals(5, bytesRead);
        
        String content = "";
        for (int i = 0; i < 6000; i++)
            content = content.concat("Hello World ");
        bytesIn = content.getBytes();
        assertEquals(bytesIn.length, 72000);
        
        length = bytesIn.length;
        
        randomAccessFile.write(bytesIn, 0, length);
        
        int res = randomAccessFile.checkObject(0l);
        assertEquals(65536, res);
        res = randomAccessFile.checkObject(1l);
        assertEquals(6464, res);
    }
    
    public void testReplicaCreationAndRemoval() throws Exception {
        randomAccessFile = new RandomAccessFile("rw", mrc1Address, volumeName + "/myDir/test2.txt", testEnv
                .getRpcClient(), userID, groupIDs);
        
        // write something
        byte[] bytesIn = new String("Hallo").getBytes();
        int length = bytesIn.length;
        ReusableBuffer data = ReusableBuffer.wrap(bytesIn);
        randomAccessFile.writeObject(0, 0, data);
        
        // set file read-only
        randomAccessFile.setReadOnly(true);
        
        // check
        assertTrue(randomAccessFile.isReadOnly());
        assertEquals(data.limit(), randomAccessFile.getXLoc().getXLocSet().getRead_only_file_size());
        assertEquals(Constants.REPL_UPDATE_PC_RONLY, randomAccessFile.getCredentials().getXlocs()
                .getRepUpdatePolicy());
        
        // try to write something
        try {
            randomAccessFile.writeObject(0, 1, data.createViewBuffer());
            fail("file is read-only. file is not writable");
        } catch (IOException e1) {
            // correct
        }
        
        // get OSDs for a replica
        List<ServiceUUID> replica1 = randomAccessFile.getSuitableOSDsForAReplica();
        replica1 = replica1.subList(0, randomAccessFile.getStripingPolicy().getWidth());
        
        // add a replica
        randomAccessFile.addReplica(replica1, randomAccessFile.getStripingPolicy(), ReplicationFlags
                .setPartialReplica(ReplicationFlags.setRandomStrategy(0)));
        
        // check
        assertEquals(2, randomAccessFile.getCredentials().getXlocs().getReplicas().size());
        // TODO: check if the correct OSDs are in the list as a replica
        
        // get OSDs for a replica
        List<ServiceUUID> replica2 = randomAccessFile.getSuitableOSDsForAReplica();
        replica2 = replica2.subList(0, randomAccessFile.getStripingPolicy().getWidth());
        
        // add a second replica
        randomAccessFile.addReplica(replica2, randomAccessFile.getStripingPolicy(), ReplicationFlags
                .setFullReplica(ReplicationFlags.setSequentialStrategy(0)));
        // check
        // check
        assertEquals(3, randomAccessFile.getCredentials().getXlocs().getReplicas().size());
        // TODO: check if the correct OSDs are in the list as a replica
        
        // check if replication flags are set correctly
        List<org.xtreemfs.common.xloc.Replica> replicas = randomAccessFile.getXLoc().getReplicas();
        assertTrue(replicas.get(0).isComplete()); // original should be marked
                                                  // as full
        assertFalse(replicas.get(1).isComplete()); // replica 1 is empty
        assertFalse(replicas.get(2).isComplete()); // replica 2 is empty
        assertTrue(replicas.get(1).isPartialReplica()); // replica 1 should be
                                                        // filled ondemand
        assertFalse(replicas.get(2).isPartialReplica()); // replica 2 should be
                                                         // filled until it is
                                                         // full
        assertTrue(ReplicationFlags.isRandomStrategy(replicas.get(1).getTransferStrategyFlags())); // replica
                                                                                                   // 1
                                                                                                   // is
                                                                                                   // using
                                                                                                   // random
                                                                                                   // strategy
        assertTrue(ReplicationFlags.isSequentialStrategy(replicas.get(2).getTransferStrategyFlags())); // replica
                                                                                                       // 2
                                                                                                       // is
                                                                                                       // using
                                                                                                       // sequential
                                                                                                       // strategy
        
        // remove the first replica
        randomAccessFile.removeReplica(replica1.get(0));
        // check
        assertEquals(2, randomAccessFile.getCredentials().getXlocs().getReplicas().size());
        // TODO: check if the correct OSDs are in the list as a replica
        
        // try to remove read-only flag
        try {
            randomAccessFile.setReadOnly(false);
            fail("File must not marked as read-only, because replicas exists.");
        } catch (Exception e) {
            // correct
        }
        
        // remove the last replica
        randomAccessFile.removeReplica(replica2.get(0));
        // check
        assertEquals(1, randomAccessFile.getCredentials().getXlocs().getReplicas().size());
        // TODO: check if the correct OSDs are in the list as a replica
        
        // try to remove read-only flag
        try {
            randomAccessFile.setReadOnly(false);
        } catch (Exception e) {
            fail("File should be able to marked as read-only, because replicas exists.");
        }
        
        // try to write something
        try {
            randomAccessFile.writeObject(0, 1, data.createViewBuffer());
        } catch (IOException e1) {
            fail("file must be writable after deleting read-only flag");
        }
    }
    
    public static void main(String[] args) {
        TestRunner.run(RandomAccessFileTest.class);
    }
}
