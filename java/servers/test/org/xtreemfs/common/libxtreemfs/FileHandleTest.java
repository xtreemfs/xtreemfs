/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 * 
 * <br>
 * Dec 15, 2011
 */
public class FileHandleTest {
    private static DIRRequestDispatcher dir;

    private static TestEnvironment      testEnv;

    private static DIRConfig            dirConfig;

    private static UserCredentials      userCredentials;

    private static Auth                 auth        = RPCAuthentication.authNone;

    private static String               mrcAddress;

    private static String               dirAddress;

    private static VivaldiCoordinates   defaultCoordinates;

    private static String               VOLUME_NAME = "foobar";

    private static StripingPolicy       defaultStripingPolicy;

    private static OSD[]                osds;
    private static OSDConfig[]          configs;

    private static MRCServiceClient     mrcClient;

    private static ClientImplementation client;

    private static Options              options;

    @BeforeClass
    public static void initializeTest() throws Exception {
        System.out.println("TEST: " + VolumeTest.class.getSimpleName());

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(Logging.LEVEL_DEBUG);

        dirConfig = SetupUtils.createDIRConfig();
        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv =
                new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                        TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                        TestEnvironment.Services.MRC });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        defaultCoordinates =
                VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0).setLocalError(0).build();
        defaultStripingPolicy =
                StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                        .setStripeSize(128).setWidth(1).build();

        osds = new OSD[3];
        configs = SetupUtils.createMultipleOSDConfigs(3);

        // start two OSDs
        osds[0] = new OSD(configs[0]);
        osds[1] = new OSD(configs[1]);

        mrcClient = new MRCServiceClient(testEnv.getRpcClient(), testEnv.getMRCAddress());

        options = new Options();
        client = (ClientImplementation) Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        for (int i = 0; i < osds.length; i++) {
            if (osds[i] != null) {
                osds[i].shutdown();
            }

        }

        testEnv.shutdown();
        dir.shutdown();
        dir.waitForShutdown();

        client.shutdown();
    }

    @Test
    public void testTruncate() throws Exception {
        VOLUME_NAME = "testTruncate";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);
        fileHandle.close();

        assertEquals(0, volume.getAttr(userCredentials, fileName).getSize());

        fileHandle =
                volume.openFile(userCredentials, fileName,
                        SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber(), 0777);
        fileHandle.truncate(userCredentials, 1337);
        assertEquals(1337, fileHandle.getAttr(userCredentials).getSize());
    }

    @Test(expected = PosixErrorException.class)
    public void testAcquireLock() throws Exception {
        VOLUME_NAME = "testAcquireLock";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);

        int processId = 10000;
        int offset = 0;
        int length = 100;
        boolean exclusive = true;
        boolean waitForLock = true;
        Lock lock =
                fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, waitForLock);
        assertEquals(processId, lock.getClientPid());
        assertEquals(offset, lock.getOffset());
        assertEquals(length, lock.getLength());
        assertEquals(exclusive, lock.getExclusive());

        // the cached lock should be equal, too.
        Lock secondLock =
                fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, waitForLock);
        assertEquals(lock, secondLock);

        // acquiring locks should also work if we don't wait for the lock.
        processId++;
        FileHandle fileHandle2 = volume.openFile(userCredentials, fileName + 2, flags, 0777);
        Lock anotherLock =
                fileHandle2.acquireLock(userCredentials, processId, offset, length, exclusive, false);
        assertEquals(processId, anotherLock.getClientPid());
        assertEquals(offset, anotherLock.getOffset());
        assertEquals(length, anotherLock.getLength());
        assertEquals(exclusive, anotherLock.getExclusive());

        // acquiring a second lock with a new pid should throw an exception
        processId++;
        fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, waitForLock);
    }

    @Test
    public void testCheckLock() throws Exception {
        VOLUME_NAME = "testCheckLock";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);

        int processId = 10000;
        int offset = 0;
        int length = 100;
        boolean exclusive = true;

        Lock lock = fileHandle.checkLock(userCredentials, processId, offset, length, exclusive);
        assertEquals(processId, lock.getClientPid());
        assertEquals(offset, lock.getOffset());
        assertEquals(length, lock.getLength());
        assertEquals(exclusive, lock.getExclusive());

        // create lock and request same lock with different pid

        FileHandle fileHandle2 = volume.openFile(userCredentials, fileName + 2, flags, 0777);
        processId++;
        fileHandle2.acquireLock(userCredentials, processId, offset, length, exclusive, true);
        processId++;
        Lock anotherLock = fileHandle2.checkLock(userCredentials, processId, offset, length, exclusive);
        assertEquals(processId - 1, anotherLock.getClientPid());
        assertEquals(offset, anotherLock.getOffset());
        assertEquals(length, anotherLock.getLength());
        assertEquals(exclusive, anotherLock.getExclusive());

        // this lock should be already cached
        processId--;
        Lock cachedLock = fileHandle2.checkLock(userCredentials, processId, offset, length, exclusive);
        assertEquals(processId, cachedLock.getClientPid());
        assertEquals(offset, cachedLock.getOffset());
        assertEquals(length, cachedLock.getLength());
        assertEquals(exclusive, cachedLock.getExclusive());
    }

    @Test
    public void testReleaseLock() throws Exception {
        VOLUME_NAME = "testReleaseLock";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);

        int processId = 10000;
        int offset = 0;
        int length = 100;
        boolean exclusive = true;

        fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, true);
        fileHandle.releaseLock(userCredentials, processId, offset, length, exclusive);

        // if releaseLock fails the attempt to require the same lock with a differnt PID will fail, too.
        processId++;
        Lock anotherLock =
                fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, true);
        assertEquals(processId, anotherLock.getClientPid());
        assertEquals(offset, anotherLock.getOffset());
        assertEquals(length, anotherLock.getLength());
        assertEquals(exclusive, anotherLock.getExclusive());

        fileHandle.releaseLockOfProcess(processId);
        processId++;
        anotherLock = fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, true);
        assertEquals(processId, anotherLock.getClientPid());
        assertEquals(offset, anotherLock.getOffset());
        assertEquals(length, anotherLock.getLength());
        assertEquals(exclusive, anotherLock.getExclusive());

        // the lock don't belong to the clientPid. Should do nothing. checkLock should return the old one.
        processId++;
        fileHandle.releaseLock(userCredentials, processId, offset, length, exclusive);
        anotherLock = fileHandle.checkLock(userCredentials, processId, offset, length, exclusive);
        assertEquals(processId - 1, anotherLock.getClientPid());
        assertEquals(offset, anotherLock.getOffset());
        assertEquals(length, anotherLock.getLength());
        assertEquals(exclusive, anotherLock.getExclusive());

    }

    @Test
    public void testAsyncXcapRenewal() throws Exception {
        VOLUME_NAME = "testAsyncXcapRenewal";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandleImplementation fileHandle =
                (FileHandleImplementation) volume.openFile(userCredentials, fileName, flags, 0777);

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
        VOLUME_NAME = "testTruncateWithAsyncWritesFailed";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandleImplementation fileHandle =
                (FileHandleImplementation) volume.openFile(userCredentials, fileName, flags, 0777);
        fileHandle.markAsyncWritesAsFailed();
        fileHandle.truncate(userCredentials, 100);
    }

    @Test(expected = PosixErrorException.class)
    public void testFlushWithAsyncWritesFailed() throws Exception {
        VOLUME_NAME = "testFlushWithAsyncWritesFailed";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandleImplementation fileHandle =
                (FileHandleImplementation) volume.openFile(userCredentials, fileName, flags, 0777);
        fileHandle.markAsyncWritesAsFailed();
        fileHandle.flush();
    }

    @Test(expected = PosixErrorException.class)
    public void testWriteWithAsyncWritesFailed() throws Exception {
        VOLUME_NAME = "testWriteWithAsyncWritesFailed";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandleImplementation fileHandle =
                (FileHandleImplementation) volume.openFile(userCredentials, fileName, flags, 0777);
        fileHandle.markAsyncWritesAsFailed();
        fileHandle.write(userCredentials, "hallo".getBytes(), 5, 0);
    }

    @Test(expected = PosixErrorException.class)
    public void testReadWithAsyncWritesFailed() throws Exception {
        VOLUME_NAME = "testReadWithAsyncWritesFailed";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandleImplementation fileHandle =
                (FileHandleImplementation) volume.openFile(userCredentials, fileName, flags, 0777);
        fileHandle.markAsyncWritesAsFailed();
        fileHandle.read(userCredentials, "a".getBytes(), 0, 1);
    }

    @Test
    public void testWriteBackFileSize() throws Exception {
        VOLUME_NAME = "testWriteBackFileSize";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandleImplementation fileHandle =
                (FileHandleImplementation) volume.openFile(userCredentials, fileName, flags, 0777);

        OSDWriteResponse.Builder responseBuilder = OSDWriteResponse.newBuilder();
        responseBuilder.setSizeInBytes(13337);
        responseBuilder.setTruncateEpoch(20);
        OSDWriteResponse response = responseBuilder.build();
        fileHandle.writeBackFileSize(response, false);

        getattrResponse response2 =
                mrcClient.getattr(null, auth, userCredentials, VOLUME_NAME, fileName, 0l).get();
    
        assertEquals(response.getSizeInBytes(), response2.getStbuf().getSize());
        assertEquals(response.getTruncateEpoch(), response2.getStbuf().getTruncateEpoch());
    }
    
    @Test
    public void testWriteBackFileSizeAsync() throws Exception {
        VOLUME_NAME = "testWriteBackFileSizeAsync";
        String fileName = "testfile";
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandleImplementation fileHandle =
                (FileHandleImplementation) volume.openFile(userCredentials, fileName, flags, 0777);

        OSDWriteResponse.Builder responseBuilder = OSDWriteResponse.newBuilder();
        responseBuilder.setSizeInBytes(13337);
        responseBuilder.setTruncateEpoch(20);
        OSDWriteResponse response = responseBuilder.build();
        fileHandle.setOsdWriteResponseForAsyncWriteBack(response);
        fileHandle.writeBackFileSizeAsync();
        
        Thread.sleep(2000);
        
        getattrResponse response2 =
                mrcClient.getattr(null, auth, userCredentials, VOLUME_NAME, fileName, 0l).get();
    
        assertEquals(response.getSizeInBytes(), response2.getStbuf().getSize());
        assertEquals(response.getTruncateEpoch(), response2.getStbuf().getTruncateEpoch());        
    }
    
    @Test
    public void testWriteWithMoreThanOneBlock() throws Exception {
        final String VOLUME_NAME = "testWriteWithMoreThanOneBlock";
        
        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        options.setMetadataCacheSize(0);
        
        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
        
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        
        // Open a volume named "foobar".
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        
        // Open a file.
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                "/bla.tzt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_SYNC.getNumber());
        
        // Get file attributes
        Stat stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(0, stat.getSize());
        
        // Write to file.\
        final int numBytes = 5500;
        final String xtreemfs = "XTREEMFS";
        String data = "";
        while (data.length() < numBytes) {
            data += xtreemfs;
        }
        
        int writtenBytes = fileHandle.write(userCredentials, data.getBytes(), 4092, 0);
    
        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(4092, stat.getSize());
        
        byte[] secondChunk = new byte[data.length()-writtenBytes];
        for (int i = 0; i < secondChunk.length; i++) {
            secondChunk[i] = data.getBytes()[writtenBytes+i];
        }
        fileHandle.write(userCredentials, secondChunk, secondChunk.length, writtenBytes);
        fileHandle.flush();
        
//        Thread.sleep(options.getPeriodicFileSizeUpdatesIntervalS()*1000+1000);
        
        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(data.length(), stat.getSize());
        
        // Read from file.
        byte[] readData = new byte[data.length()];
        int readCount = fileHandle.read(userCredentials, readData, data.length(), 0);
        
        assertEquals(data.length(), readCount);
        for (int i = 0; i < data.length(); i++) {
            assertEquals(readData[i], data.getBytes()[i]);
        }
        
        fileHandle.close();
        client.shutdown();
    }
    
    @Test
    public void testWriteGreaterThanStripesize() throws Exception {
        final String VOLUME_NAME = "testWriteGreaterThanStripesize";
        
        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        options.setMetadataCacheSize(0);
        
        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
        
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();
        
        // Open a volume named "foobar".
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        
        // Open a file.
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                "/bla.tzt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_SYNC.getNumber());
        
        // Get file attributes
        Stat stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(0, stat.getSize());
        
        // Write to file.\
        final int numExtraBytes = 5500/8;
        final String xtreemfs = "XTREEMFS";
        String data = "";
        while (data.length() < defaultStripingPolicy.getStripeSize()*1024) {
            data += xtreemfs;
        }
        for (int i = 0; i < numExtraBytes; i++) {
            data += xtreemfs;
        }
        
        int writtenBytes = fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);
    
        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(data.length(), stat.getSize());
        
        // Read from file.
        byte[] readData = new byte[data.length()];
        int readCount = fileHandle.read(userCredentials, readData, data.length(), 0);
        
        assertEquals(data.length(), readCount);
        assertEquals(writtenBytes, readCount);
        for (int i = 0; i < data.length(); i++) {
            assertEquals(readData[i], data.getBytes()[i]);
        }
        
        fileHandle.close();
        client.shutdown();
    }
}
