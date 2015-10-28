/*
 * Copyright (c) 2009-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.drain.OSDDrain;
import org.xtreemfs.osd.drain.OSDDrain.FileInformation;
import org.xtreemfs.osd.drain.OSDDrainException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * @author bzcseife
 * 
 * <br>
 *         Mar 31, 2011
 */

public class OSDDrainTest {
    @Rule
    public final TestRule             testLog    = TestHelper.testLog;

    private TestEnvironment           testEnv;

    private static final String       VOLNAME    = "testvolume";
    private static final String       VOLNAME2   = "testvolume2";

    private static int                STRIPESIZE = 1024;

    private static OSDConfig          osdConfig1;
    private static OSDConfig          osdConfig2;
    private static OSDConfig          osdConfig3;

    private List<OSD>                 osdServer;

    private MRCServiceClient          mrcClient;

    private static StripingPolicyImpl sp;

    private final Auth                authHeader = RPCAuthentication.authNone;

    private final UserCredentials     uc         = RPCAuthentication.userService;

    private UUIDResolver              resolver;

    private OSDDrain                  osdDrain;

    private MRCConfig                 mrc2Config;

    private MRCRequestDispatcher      mrc2;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        osdConfig1 = SetupUtils.createOSD1Config();
        osdConfig2 = SetupUtils.createOSD2Config();
        osdConfig3 = SetupUtils.createOSD3Config();

        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, STRIPESIZE))
                .setReplicationFlags(0).build();
        sp = StripingPolicyImpl.getPolicy(r, 0);
    }

    @Before
    public void setUp() throws Exception {
        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT, TestEnvironment.Services.MRC,

        });
        testEnv.start();

        mrc2Config = SetupUtils.createMRC2Config();
        mrc2 = new MRCRequestDispatcher(mrc2Config, SetupUtils.createMRC2dbsConfig());
        mrc2.startup();

        osdServer = new ArrayList<OSD>(2);

        mrcClient = testEnv.getMrcClient();

        DIRClient dir = new DIRClient(testEnv.getDirClient(), new InetSocketAddress[] { testEnv.getDIRAddress() }, 10,
                1000 * 5);
        resolver = UUIDResolver.startNonSingelton(dir, 1000, 10 * 10 * 1000);

        osdDrain = new OSDDrain(dir, testEnv.getOSDClient(), testEnv.getMrcClient(), osdConfig1.getUUID(), authHeader,
                uc, resolver);

    }

    @After
    public void tearDown() throws Exception {
        if (mrc2 != null) {
            mrc2.shutdown();
        }

        testEnv.shutdown();
    }

    /**
     * Test if files which are deleted but still have object files on OSDs will be handled correctly.
     * 
     * @throws Exception
     */
    @Test
    public void testHandleNonExistingFile() throws Exception {

        osdServer.add(new OSD(osdConfig1));

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();

        c.createVolume(VOLNAME, authHeader, uc, sp.getPolicy(), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                0777);

        Volume volume = c.getVolume(VOLNAME, uc);

        File file1 = volume.getFile("foo");
        File file2 = volume.getFile("bar");

        file1.createFile();
        file2.createFile();

        RandomAccessFile raf1 = file1.open("rw", 0777);
        RandomAccessFile raf2 = file2.open("rw", 0777);

        final int SIZE = 1024 * 200;
        byte[] data = new byte[SIZE];

        for (int j = 0; j < SIZE; j++) {
            data[j] = 'f';
        }

        raf1.write(data, 0, data.length);
        raf2.write(data, 0, data.length);

        List<FileInformation> fileInfos = new LinkedList<OSDDrain.FileInformation>();

        fileInfos = osdDrain.getFileListOfOSD();

        assertEquals(2, fileInfos.size());

        osdDrain.updateMRCAddresses(fileInfos);

        raf2.close();
        file2.delete();

        fileInfos = osdDrain.removeNonExistingFileIDs(fileInfos);

        assertEquals(1, fileInfos.size());

        raf1.close();
        file1.delete();

        c.deleteVolume(VOLNAME, authHeader, uc);
        c.stop();

        for (OSD osd : osdServer) {
            osd.shutdown();
        }
        osdServer.clear();

        TimeSync.initializeLocal(50).waitForStartup();

    }

    @Test
    public void testRemoveOSD() throws Exception {

        // start only one OSD to ensure that all file lay on the same OSD (easier to make assertions)
        osdServer.add(new OSD(osdConfig1));

        final int NUMBER_OF_FILES = 5;
        LinkedList<String> fileNames = new LinkedList<String>();
        fileNames.add("foo1");
        fileNames.add("foo2");
        fileNames.add("foo3");
        fileNames.add("foo4");
        fileNames.add("foo5");

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();

        c.createVolume(VOLNAME, authHeader, uc, sp.getPolicy(), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                0777);

        Volume volume = c.getVolume(VOLNAME, uc);

        final int SIZE = 1024 * 200;
        byte[] data = new byte[SIZE];

        File files[] = new File[NUMBER_OF_FILES];

        for (int i = 0; i < NUMBER_OF_FILES; i++) {
            files[i] = volume.getFile(fileNames.get(i));
            files[i].createFile();

            for (int j = 0; j < SIZE; j++) {
                data[j] = 'f';
            }

            RandomAccessFile raf = files[i].open("rw", 0777);
            raf.write(data, 0, data.length);
            raf.flush();
            raf.close();
        }

        List<FileInformation> fileInfos = null;
        try {
            // set OSDServiceStatus to prevent further writing on this OSD
            osdDrain.setServiceStatus(ServiceStatus.SERVICE_STATUS_REMOVED);

            // get all files the OSD has
            fileInfos = osdDrain.getFileListOfOSD();
            assertEquals(NUMBER_OF_FILES, fileInfos.size());

            // get address of MRC which is responsible for every file
            osdDrain.updateMRCAddresses(fileInfos);
            for (FileInformation fileInfo : fileInfos) {
                assertEquals(testEnv.getMRCAddress(), fileInfo.mrcAddress);
            }

            // get the current replica configuration
            fileInfos = osdDrain.getReplicaInfo(fileInfos);

            // As there are no RWR replicated files the fileInfos count wont change.
            fileInfos = osdDrain.drainCoordinatedFiles(fileInfos);
            assertEquals(NUMBER_OF_FILES, fileInfos.size());

            // set ReplicationUpdatePolicy to RONLY
            fileInfos = osdDrain.setReplicationUpdatePolicyRonly(fileInfos);
            for (File file : files) {
                assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, file.getReplicaUpdatePolicy());
            }

            // set Files read-only
            fileInfos = osdDrain.setFilesReadOnlyAttribute(fileInfos);
            for (File file : files) {
                assertTrue(file.isReadOnly());
            }

            // start second OSD
            osdServer.add(new OSD(osdConfig2));

            // wait until the OSD is registered and known to the MRC
            Thread.sleep(10 * 1000);

            // create replications
            fileInfos = osdDrain.createReplicasForFiles(fileInfos);
            for (File file : files) {
                assertEquals(2, file.getNumReplicas());
            }

            // start replication
            fileInfos = osdDrain.startReplication(fileInfos);

            // wait for replication to be finished
            fileInfos = osdDrain.waitForReplicationToComplete(fileInfos);
            for (File file : files) {
                assertTrue(file.isReplicated());
            }

            // remove replicas
            osdDrain.removeOriginalFromReplica(fileInfos);
            for (File file : files) {
                assertEquals(1, file.getNumReplicas());
            }

            // set every file to read/write again which wasn't set to read-only before
            osdDrain.resetFilesReadOnlyAttribute(fileInfos);
            for (File file : files) {
                assertFalse(file.isReadOnly());
            }

            // set ReplicationUpdatePolicy to original value
            osdDrain.resetReplicationUpdatePolicy(fileInfos);
            for (File file : files) {
                assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, file.getReplicaUpdatePolicy());
            }

        } catch (OSDDrainException e) {
            osdDrain.handleException(e, true);
            throw e;
        }

        // test if files are the samel like before
        for (int i = 0; i < NUMBER_OF_FILES; i++) {
            RandomAccessFile raf = files[i].open("r", 0777);

            raf.read(data, 0, data.length);
            raf.close();

            for (int j = 0; j < SIZE; j++) {
                assertEquals('f', data[j]);
            }
        }

        // tidy up
        for (File file : files) {
            file.delete();
        }

        c.deleteVolume(VOLNAME, authHeader, uc);
        c.stop();

        for (OSD osd : osdServer) {
            osd.shutdown();
        }
        osdServer.clear();

        TimeSync.initializeLocal(50).waitForStartup();

    }

    @Test
    public void testRemoveOSDWithRWR() throws Exception {
        osdServer.add(new OSD(osdConfig1));
        osdServer.add(new OSD(osdConfig2));

        final int NUMBER_OF_FILES = 5;
        LinkedList<String> fileNames = new LinkedList<String>();
        fileNames.add("foo1");
        fileNames.add("foo2");
        fileNames.add("foo3");
        fileNames.add("foo4");
        fileNames.add("foo5");

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();

        c.createVolume(VOLNAME, authHeader, uc, sp.getPolicy(), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                0777);

        Volume volume = c.getVolume(VOLNAME, uc);

        File root = volume.getFile("/");
        root.setDefaultReplication(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 2);


        final int SIZE = 1024 * 200;
        byte[] data = new byte[SIZE];

        File files[] = new File[NUMBER_OF_FILES];

        for (int i = 0; i < NUMBER_OF_FILES; i++) {
            files[i] = volume.getFile(fileNames.get(i));
            files[i].createFile();

            for (int j = 0; j < SIZE; j++) {
                data[j] = 'f';
            }

            RandomAccessFile raf = files[i].open("rw", 0777);
            raf.write(data, 0, data.length);
            raf.flush();
            raf.close();
        }

        // start third OSD
        osdServer.add(new OSD(osdConfig3));

        // wait until the OSD is registered and known to the MRC
        Thread.sleep(10 * 1000);

        List<FileInformation> fileInfos = null;
        try {
            // set OSDServiceStatus to prevent further writing on this OSD
            osdDrain.setServiceStatus(ServiceStatus.SERVICE_STATUS_REMOVED);

            // get all files the OSD has
            fileInfos = osdDrain.getFileListOfOSD();
            assertEquals(NUMBER_OF_FILES, fileInfos.size());

            // get address of MRC which is responsible for every file
            osdDrain.updateMRCAddresses(fileInfos);
            for (FileInformation fileInfo : fileInfos) {
                assertEquals(testEnv.getMRCAddress(), fileInfo.mrcAddress);
            }

            // get the current replica configuration
            fileInfos = osdDrain.getReplicaInfo(fileInfos);

            // Handle r/w coordinated files and remove them from the file info list.
            fileInfos = osdDrain.drainCoordinatedFiles(fileInfos);
            assertEquals(0, fileInfos.size());

        } catch (OSDDrainException e) {
            osdDrain.handleException(e, true);
            throw e;
        }

        // test if files are the same like before
        for (int i = 0; i < NUMBER_OF_FILES; i++) {
            RandomAccessFile raf = files[i].open("r", 0777);

            raf.read(data, 0, data.length);
            raf.close();

            for (int j = 0; j < SIZE; j++) {
                assertEquals('f', data[j]);
            }
        }

        // tidy up
        for (File file : files) {
            file.delete();
        }

        c.deleteVolume(VOLNAME, authHeader, uc);
        c.stop();

        for (OSD osd : osdServer) {
            osd.shutdown();
        }
        osdServer.clear();

        TimeSync.initializeLocal(50).waitForStartup();

    }

    @Test
    public void testMultipleMRCs() throws Exception {
        osdServer.add(new OSD(osdConfig1));
        osdServer.add(new OSD(osdConfig2));
        osdServer.add(new OSD(osdConfig3));

        String mrc2UUID = mrc2Config.getUUID().toString();

        final int NUMBER_OF_FILES = 10;
        LinkedList<String> fileNames = new LinkedList<String>();
        for (int i = 0; i < NUMBER_OF_FILES; i++) {
            fileNames.add("foo" + i);
        }

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();

        c.createVolume(VOLNAME, authHeader, uc, sp.getPolicy(), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                0777, SetupUtils.getMRC1UUID().toString());
        c.createVolume(VOLNAME2, authHeader, uc, sp.getPolicy(), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                0777, mrc2UUID);

        Volume volume1 = c.getVolume(VOLNAME, uc);
        Volume volume2 = c.getVolume(VOLNAME2, uc);

        final int SIZE = 1024 * 200;
        byte[] data = new byte[SIZE];

        File files[] = new File[NUMBER_OF_FILES];

        for (int i = 0; i < NUMBER_OF_FILES; i++) {
            if (i % 2 == 0) {
                files[i] = volume1.getFile(fileNames.get(i));
            } else {
                files[i] = volume2.getFile(fileNames.get(i));
            }

            files[i].createFile();

            for (int j = 0; j < SIZE; j++) {
                data[j] = 'f';
            }

            RandomAccessFile raf = files[i].open("rw", 0777);
            raf.write(data, 0, data.length);
            raf.flush();
            raf.close();
        }

        // remove first osd
        List<FileInformation> fileInfos = null;
        try {
            // set OSDServiceStatus to prevent further writing on this OSD
            osdDrain.setServiceStatus(ServiceStatus.SERVICE_STATUS_REMOVED);

            // get all files the OSD has
            fileInfos = osdDrain.getFileListOfOSD();

            // get address of MRC which is responsible for every file
            osdDrain.updateMRCAddresses(fileInfos);

            // get the current replica configuration
            fileInfos = osdDrain.getReplicaInfo(fileInfos);

            // Handle r/w coordinated files and remove them from the file info list.
            fileInfos = osdDrain.drainCoordinatedFiles(fileInfos);

            // set ReplicationUpdatePolicy to RONLY
            fileInfos = osdDrain.setReplicationUpdatePolicyRonly(fileInfos);

            // set Files read-only
            fileInfos = osdDrain.setFilesReadOnlyAttribute(fileInfos);

            // create replications
            fileInfos = osdDrain.createReplicasForFiles(fileInfos);

            // start replication
            fileInfos = osdDrain.startReplication(fileInfos);

            // wait for replication to be finished
            fileInfos = osdDrain.waitForReplicationToComplete(fileInfos);

            // remove replicas
            osdDrain.removeOriginalFromReplica(fileInfos);
            for (File file : files) {
                assertEquals(1, file.getNumReplicas());
            }

            // set every file to read/write again which wasn't set to read-only before
            osdDrain.resetFilesReadOnlyAttribute(fileInfos);
            for (File file : files) {
                assertFalse(file.isReadOnly());
            }

            // set ReplicationUpdatePolicy to original value
            osdDrain.resetReplicationUpdatePolicy(fileInfos);
            for (File file : files) {
                assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, file.getReplicaUpdatePolicy());
            }

        } catch (OSDDrainException e) {
            osdDrain.handleException(e, true);
            throw e;
        }
        // test if files are the same like before
        for (int i = 0; i < NUMBER_OF_FILES; i++) {

            RandomAccessFile raf = files[i].open("r", 0777);

            raf.read(data, 0, data.length);
            raf.close();

            for (int j = 0; j < SIZE; j++) {
                assertEquals('f', data[j]);
            }
        }

        // tidy up
        for (File file : files) {
            file.delete();
        }

        c.deleteVolume(VOLNAME, authHeader, uc);
        c.deleteVolume(VOLNAME2, authHeader, uc);
        c.stop();

        for (OSD osd : osdServer) {
            osd.shutdown();
        }
        osdServer.clear();

        TimeSync.initializeLocal(50).waitForStartup();
    }

}
