/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.libxtreemfs.exceptions.InvalidChecksumException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * Tests regarding the Java Implementation of the FileHandle interface.
 */
public class FileHandleImplementationTest {
    @Rule
    public final TestRule           testLog = TestHelper.testLog;

    private static TestEnvironment  testEnv;

    private static UserCredentials  userCredentials;

    private static Auth             auth    = RPCAuthentication.authNone;

    private static String           mrcAddress;

    private static String           dirAddress;

    private static StripingPolicy   defaultStripingPolicy;

    private static MRCServiceClient mrcClient;

    private static AdminClient      client;

    private static Options          options;

    @Before
    public void setUp() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(Logging.LEVEL_WARN);

        SetupUtils.CHECKSUMS_ON = true;
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.RPC_CLIENT, TestEnvironment.Services.MRC, TestEnvironment.Services.MRC_CLIENT,
                TestEnvironment.Services.OSD, TestEnvironment.Services.OSD });
        testEnv.start();
        SetupUtils.CHECKSUMS_ON = false;

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        defaultStripingPolicy = StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                .setStripeSize(128).setWidth(1).build();

        mrcClient = testEnv.getMrcClient();

        options = new Options();
        client = ClientFactory.createAdminClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
        client.shutdown();
    }
    
    @Test
    public void testAsyncXcapRenewal() throws Exception {
        String volumeName = "testAsyncXcapRenewal";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandleImplementation fileHandle = (FileHandleImplementation) volume.openFile(userCredentials,
                fileName, flags, 0777);

        XCap oldXCap = fileHandle.getXcap();
        Thread.sleep(2000);
        fileHandle.renewXCapAsync();
        fileHandle.waitForAsyncXcapRenewalFinished();
        XCap renewedXCap = fileHandle.getXcap();
        assertEquals(oldXCap.getExpireTimeoutS(), renewedXCap.getExpireTimeoutS());
        assertTrue(oldXCap.getExpireTimeS() < renewedXCap.getExpireTimeS());
        assertEquals(oldXCap.getAccessMode(), renewedXCap.getAccessMode());
        assertEquals(oldXCap.getClientIdentity(), renewedXCap.getClientIdentity());
        assertEquals(oldXCap.getReplicateOnClose(), renewedXCap.getReplicateOnClose());
        assertEquals(oldXCap.getSerializedSize(), renewedXCap.getSerializedSize());
        assertEquals(oldXCap.getSnapConfig(), renewedXCap.getSnapConfig());
        assertEquals(oldXCap.getTruncateEpoch(), renewedXCap.getTruncateEpoch());
    }
    

    @Test(expected = PosixErrorException.class)
    public void testTruncateWithAsyncWritesFailed() throws Exception {
        String volumeName = "testTruncateWithAsyncWritesFailed";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandleImplementation fileHandle = (FileHandleImplementation) volume.openFile(userCredentials,
                fileName, flags, 0777);
        fileHandle.markAsyncWritesAsFailed();
        fileHandle.truncate(userCredentials, 100);
    }
    
    @Test(expected = PosixErrorException.class)
    public void testFlushWithAsyncWritesFailed() throws Exception {
        String volumeName = "testFlushWithAsyncWritesFailed";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandleImplementation fileHandle = (FileHandleImplementation) volume.openFile(userCredentials,
                fileName, flags, 0777);
        fileHandle.markAsyncWritesAsFailed();
        fileHandle.flush();
    }

    @Test(expected = PosixErrorException.class)
    public void testWriteWithAsyncWritesFailed() throws Exception {
        String volumeName = "testWriteWithAsyncWritesFailed";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandleImplementation fileHandle = (FileHandleImplementation) volume.openFile(userCredentials,
                fileName, flags, 0777);
        fileHandle.markAsyncWritesAsFailed();
        fileHandle.write(userCredentials, "hallo".getBytes(), 5, 0);
    }
    
    @Test(expected = PosixErrorException.class)
    public void testReadWithAsyncWritesFailed() throws Exception {
        String volumeName = "testReadWithAsyncWritesFailed";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandleImplementation fileHandle = (FileHandleImplementation) volume.openFile(userCredentials,
                fileName, flags, 0777);
        fileHandle.markAsyncWritesAsFailed();
        fileHandle.read(userCredentials, "a".getBytes(), 0, 1);
    }
    
    @Test
    public void testWriteBackFileSize() throws Exception {
        String volumeName = "testWriteBackFileSize";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandleImplementation fileHandle = (FileHandleImplementation) volume.openFile(userCredentials,
                fileName, flags, 0777);

        OSDWriteResponse.Builder responseBuilder = OSDWriteResponse.newBuilder();
        responseBuilder.setSizeInBytes(13337);
        responseBuilder.setTruncateEpoch(20);
        OSDWriteResponse response = responseBuilder.build();
        fileHandle.writeBackFileSize(response, false);

        RPCResponse<getattrResponse> r = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, volumeName,
                fileName, 0l);
        getattrResponse response2 = r.get(); 

        assertEquals(response.getSizeInBytes(), response2.getStbuf().getSize());
        assertEquals(response.getTruncateEpoch(), response2.getStbuf().getTruncateEpoch());
        
        r.freeBuffers();
    }  

    @Test
    public void testWriteBackFileSizeAsync() throws Exception {
        String volumeName = "testWriteBackFileSizeAsync";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandleImplementation fileHandle = (FileHandleImplementation) volume.openFile(userCredentials,
                fileName, flags, 0777);

        OSDWriteResponse.Builder responseBuilder = OSDWriteResponse.newBuilder();
        responseBuilder.setSizeInBytes(13337);
        responseBuilder.setTruncateEpoch(20);
        OSDWriteResponse response = responseBuilder.build();
        fileHandle.setOsdWriteResponseForAsyncWriteBack(response);
        fileHandle.writeBackFileSizeAsync();

        Thread.sleep(2000);

        RPCResponse<getattrResponse> r = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, volumeName,
                fileName, 0l);
        getattrResponse response2 = r.get();

        assertEquals(response.getSizeInBytes(), response2.getStbuf().getSize());
        assertEquals(response.getTruncateEpoch(), response2.getStbuf().getTruncateEpoch());
        
        r.freeBuffers();
    }
    
    @Test
    public void testMarkReplicaAsComplete() throws Exception {
        String volumeName = "testMarkReplicaAsComplete";

        // start new OSDs
        testEnv.startAdditionalOSDs(2);

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.start();

        // set replica update policy.
        int replFlags = ReplicationFlags.setFullReplica(0);
        replFlags = ReplicationFlags.setRarestFirstStrategy(replFlags);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 2,
                replFlags);

        // open FileHandle.
        AdminFileHandle fileHandle = volume.openFile(
                userCredentials,
                "/test.txt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);

        // write some content.
        String content = "";
        for (int i = 0; i < 12000; i++)
            content = content.concat("Hello World ");
        byte[] bytesIn = content.getBytes();

        int length = bytesIn.length;

        fileHandle.write(userCredentials, bytesIn, length, 0);
        // Close to trigger the replication
        fileHandle.close();
        // and wait some time to let it finish.
        Thread.sleep(5 * 1000);

        // mark new replica as complete.
        fileHandle = volume.openFile(userCredentials, "/test.txt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        boolean complete = fileHandle.checkAndMarkIfReadOnlyReplicaComplete(1, userCredentials);
        fileHandle.close();
        assertTrue(complete);

        // re-open file to get replica with updated flags.
        fileHandle = volume.openFile(userCredentials, "/test.txt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());

        assertTrue((fileHandle.getReplica(1).getReplicationFlags() & REPL_FLAG.REPL_FLAG_IS_COMPLETE
                .getNumber()) != 0);

        // close volume and file, shut down client
        fileHandle.close();
        volume.close();
        client.deleteVolume(auth, userCredentials, volumeName);
    }
    
    /**
     * 
     * Creates a rw replicated file and gets the file size from the OSDs.
     * To obtain a quorum the file has 3 replicas (the first replica will be outdated). 
     * 
     * @throws Exception
     */
    //TODO(lukas) split test in OSD test (internalGetFileSizeOperation test) and client test (getSizeOnOSD test) 
    @Test
    public void testGetSizeOnOsd() throws Exception {
        String volumeName = "testGetSizeOnOsd";

        // Start new OSDs.
        testEnv.startAdditionalOSDs(3);

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.start();

        // Set replica update Policy.
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 3,
                ReplicationFlags.setSequentialStrategy(0));

        // Open FileHandle.
        AdminFileHandle fileHandle = volume.openFile(
                userCredentials,
                "/test.txt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);

        // Get OSD of first replica.
        String firstReplicaUuid = fileHandle.getReplica(0).getOsdUuids(0);
        
        // Create some content.
        String content = "";
        for (int i = 0; i < 12000; i++) {
            content = content.concat("Hello World ");
        }
        byte[] bytesIn = content.getBytes();
        int length = bytesIn.length;
        
        // Stop OSD of first replica.
        testEnv.stopOSD(firstReplicaUuid);
        
        // Write content and close file.
        fileHandle.write(userCredentials, bytesIn, length, 0);
        fileHandle.close();
        
        // Restart OSD with first replica.
        testEnv.startOSD(firstReplicaUuid);
        
        long size = 0;
        
        // Repeat until first replica is primary.
        do {

        	// Wait until current primary give up lease.
        	while (testEnv.getPrimary(fileHandle.getGlobalFileId()) != null) {
        		Thread.sleep(5000);
        	}

            // Get size on OSD.
            size = fileHandle.getSizeOnOSD();

        } while (testEnv.getPrimary(fileHandle.getGlobalFileId()).equals(firstReplicaUuid));
        
        // check if size of the first replica is correct
        assertEquals(length, size);
        
        client.deleteVolume(auth, userCredentials, volumeName);
    }

    @Test
    public void testCheckObjectAndGetSize() throws Exception {
        String volumeName = "testCheckObjectAndGetSize";

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.start();

        // open FileHandle.
        AdminFileHandle fileHandle = volume.openFile(
                userCredentials,
                "/test.txt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);

        // create some content.
        String content = "";
        for (int i = 0; i < 6000; i++)
            content = content.concat("Hello World ");
        byte[] bytesIn = content.getBytes();
        int length = bytesIn.length;
        
        //write content
        fileHandle.write(userCredentials, bytesIn, length, 0);
        
        fileHandle.close();

        // check Object to get size(File consists of one Object)
        int objSize = fileHandle.checkObjectAndGetSize(0, 0);

        assertEquals(length, objSize);

        HashStorageLayout hsl = new HashStorageLayout(testEnv.getOSDConfig(fileHandle.getReplica(0).getOsdUuids(0)),
                new MetadataCache());
        String filePath = hsl.generateAbsoluteFilePath(fileHandle.getGlobalFileId());

        File fileDir = new File(filePath);
        File[] fileList = fileDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                // Filter .dot files.
                return (!pathname.getName().startsWith("."));
            }
        });
        assertTrue(fileList.length > 0);

        FileWriter writer = new FileWriter(fileList[0], true);
        writer.write("foofoofoofoo");
        writer.close();

        try {
            fileHandle.checkObjectAndGetSize(0, 0);
            assertTrue(false);
        } catch (InvalidChecksumException e) {
            assertTrue(true);
        }

        volume.close();
        client.deleteVolume(auth, userCredentials, volumeName);
    }
}
