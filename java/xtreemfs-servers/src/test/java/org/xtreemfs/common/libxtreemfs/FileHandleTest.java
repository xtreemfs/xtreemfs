/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestHelper;

/**
 * Tests regarding the general FileHandle interface. Has to be compatible to the Java and the native JNI implementation.
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

        fileHandle = volume.openFile(userCredentials, fileName, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber(),
                0777);
        fileHandle.truncate(userCredentials, 1337);
        assertEquals(1337, fileHandle.getAttr(userCredentials).getSize());

        fileHandle.close();
        volume.close();
        client.shutdown();
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
        Lock lock = fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, waitForLock);
        assertEquals(processId, lock.getClientPid());
        assertEquals(offset, lock.getOffset());
        assertEquals(length, lock.getLength());
        assertEquals(exclusive, lock.getExclusive());

        // the cached lock should be equal, too.
        Lock secondLock = fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, waitForLock);
        assertEquals(lock, secondLock);

        // acquiring locks should also work if we don't wait for the lock.
        processId++;
        FileHandle fileHandle2 = volume.openFile(userCredentials, fileName + 2, flags, 0777);
        Lock anotherLock = fileHandle2.acquireLock(userCredentials, processId, offset, length, exclusive, false);
        assertEquals(processId, anotherLock.getClientPid());
        assertEquals(offset, anotherLock.getOffset());
        assertEquals(length, anotherLock.getLength());
        assertEquals(exclusive, anotherLock.getExclusive());

        // acquiring a second lock with a new pid should throw an exception
        processId++;
        fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, waitForLock);

        fileHandle.close();
        fileHandle2.close();
        volume.close();
        client.shutdown();
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

        fileHandle.close();
        fileHandle2.close();
        volume.close();
        client.shutdown();
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
        Lock anotherLock = fileHandle.acquireLock(userCredentials, processId, offset, length, exclusive, true);
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

        fileHandle.close();
        volume.close();
        client.shutdown();
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
        FileHandle fileHandle = volume.openFile(userCredentials, "/bla.tzt",
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
        volume.close();
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
        FileHandle fileHandle = volume.openFile(userCredentials, "/bla.tzt",
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
        volume.close();
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
        FileHandle fileHandle = volume.openFile(userCredentials, "/bla.tzt",
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
        volume.close();
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
        FileHandle fileHandle = volume.openFile(userCredentials, "/bla.tzt",
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
        volume.close();
        client.shutdown();
    }
}
