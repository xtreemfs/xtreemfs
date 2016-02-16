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
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * <br>
 * Dec 15, 2011
 */
public class FileHandleTest {
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
        client = ClientFactory.createAdminClient(dirAddress, userCredentials, null, options);
        client.start();
    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
        client.shutdown();
    }

    @Test
    public void testTruncate() throws Exception {
        String volumeName = "testTruncate";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);
        fileHandle.close();

        assertEquals(0, volume.getAttr(userCredentials, fileName).getSize());

        fileHandle = volume.openFile(userCredentials, fileName,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber(), 0777);
        fileHandle.truncate(userCredentials, 1337);
        assertEquals(1337, fileHandle.getAttr(userCredentials).getSize());
    }

    @Test(expected = PosixErrorException.class)
    public void testAcquireLock() throws Exception {
        String volumeName = "testAcquireLock";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);

        int processId = 10000;
        int offset = 0;
        int length = 100;
        boolean exclusive = true;
        boolean waitForLock = true;
        Lock lock = fileHandle
                .acquireLock(userCredentials, processId, offset, length, exclusive, waitForLock);
        assertEquals(processId, lock.getClientPid());
        assertEquals(offset, lock.getOffset());
        assertEquals(length, lock.getLength());
        assertEquals(exclusive, lock.getExclusive());

        // the cached lock should be equal, too.
        Lock secondLock = fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive,
                waitForLock);
        assertEquals(lock, secondLock);

        // acquiring locks should also work if we don't wait for the lock.
        processId++;
        FileHandle fileHandle2 = volume.openFile(userCredentials, fileName + 2, flags, 0777);
        Lock anotherLock = fileHandle2.acquireLock(userCredentials, processId, offset, length, exclusive,
                false);
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
        String volumeName = "testCheckLock";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
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
        String volumeName = "testReleaseLock";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        FileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);

        int processId = 10000;
        int offset = 0;
        int length = 100;
        boolean exclusive = true;

        fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, true);
        fileHandle.releaseLock(userCredentials, processId, offset, length, exclusive);

        // if releaseLock fails the attempt to require the same lock with a differnt PID will fail, too.
        processId++;
        Lock anotherLock = fileHandle
                .acquireLock(userCredentials, processId, offset, length, exclusive, true);
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
        String volumeName = "testAsyncXcapRenewal";
        String fileName = "testfile";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
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
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
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
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
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
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
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
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
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
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
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
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
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
    public void testWriteWithMoreThanOneBlock() throws Exception {
        final String volumeName = "testWriteWithMoreThanOneBlock";

        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        options.setMetadataCacheSize(0);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();

        // Open a volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);

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

        byte[] secondChunk = new byte[data.length() - writtenBytes];
        for (int i = 0; i < secondChunk.length; i++) {
            secondChunk[i] = data.getBytes()[writtenBytes + i];
        }
        fileHandle.write(userCredentials, secondChunk, secondChunk.length, writtenBytes);
        fileHandle.flush();

        // Thread.sleep(options.getPeriodicFileSizeUpdatesIntervalS()*1000+1000);

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
        final String volumeName = "testWriteGreaterThanStripesize";

        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        options.setMetadataCacheSize(0);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();

        // Open a volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);

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
        final int numExtraBytes = 5500 / 8;
        final String xtreemfs = "XTREEMFS";
        String data = "";
        while (data.length() < defaultStripingPolicy.getStripeSize() * 1024) {
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

    @Test
    public void testReadBytePerByte() throws Exception {
        final String volumeName = "testReadBytePerByte";

        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        options.setMetadataCacheSize(0);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();

        // Open the volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);

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
        final String data = "FFFFFFFFFF";
        int writtenBytes = fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);

        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(data.length(), stat.getSize());

        // Read from file byte per byte. Should return 0 if EOF is reached.
        int readCount = -1;
        int position = 0;
        while (readCount != 0) {
            byte[] readData = new byte[1];
            readCount = fileHandle.read(userCredentials, readData, 1, position);
            if (readCount != 0) {
                assertEquals(readData[0], "F".getBytes()[0]);
                position++;
            }
        }
        assertEquals(writtenBytes, position);

        fileHandle.close();
        client.shutdown();
    }

    @Test
    public void readBytePerByteManyTimes() throws Exception {
        final String volumeName = "testReadBytePerByteManyTimes";

        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        options.setMetadataCacheSize(0);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();

        // Open the volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);

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

        // Write to file. 2^20 times F to file
        String data = "F";
        for (int i = 0; i < 12; i++) {
            data = data + data;
        }
        int writtenBytes = fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);

        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(data.length(), stat.getSize());

        // Read from file byte per byte. Should return 0 if EOF is reached.
        int readCount = -1;
        int position = 0;
        while (readCount != 0) {
            byte[] readData = new byte[1];
            readCount = fileHandle.read(userCredentials, readData, 1, position);
            if (readCount != 0) {
                assertEquals(readData[0], "F".getBytes()[0]);
                position++;
            }
        }
        assertEquals(writtenBytes, position);

        fileHandle.close();
        client.shutdown();
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
