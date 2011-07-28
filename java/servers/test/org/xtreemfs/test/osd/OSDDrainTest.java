/*
 * Copyright (c) 2009-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRC;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.drain.OSDDrain;
import org.xtreemfs.osd.drain.OSDDrain.FileInformation;
import org.xtreemfs.osd.drain.OSDDrainException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 * @author bzcseife
 * 
 * <br>
 *         Mar 31, 2011
 */

public class OSDDrainTest extends TestCase {
    private TestEnvironment       testEnv;

    private static final String   VOLNAME    = "testvolume";
    private static final String   VOLNAME2   = "testvolume2";

    private final int             STRIPESIZE = 1024;

    private final OSDConfig       osdConfig1;
    private final OSDConfig       osdConfig2;
    private final OSDConfig       osdConfig3;

    private List<OSD>             osdServer;

    private MRCServiceClient      mrcClient;

    private StripingPolicyImpl    sp;

    private final Auth            authHeader = RPCAuthentication.authNone;

    private final UserCredentials uc         = RPCAuthentication.userService;

    private UUIDResolver          resolver;

    private OSDDrain              osdDrain;

    public OSDDrainTest(String testName) throws IOException {
        super(testName);
        Logging.start(Logging.LEVEL_DEBUG);

        osdConfig1 = SetupUtils.createOSD1Config();
        osdConfig2 = SetupUtils.createOSD2Config();
        osdConfig3 = SetupUtils.createOSD3Config();

        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, STRIPESIZE))
                .setReplicationFlags(0).build();
        sp = StripingPolicyImpl.getPolicy(r, 0);
    }

    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT,
                TestEnvironment.Services.MRC,

        });
        testEnv.start();

        osdServer = new ArrayList<OSD>(2);

        mrcClient = testEnv.getMrcClient();

        resolver = UUIDResolver.startNonSingelton(testEnv.getDirClient(), 1000, 10 * 10 * 1000);

        osdDrain = new OSDDrain(testEnv.getDirClient(), testEnv.getOSDClient(), testEnv.getMrcClient(),
                osdConfig1.getUUID(), authHeader, uc, resolver);

    }

    protected void tearDown() throws Exception {

        testEnv.shutdown();
    }

    /**
     * Test if files which are deleted but still have object files on OSDs will be handled correctly.
     * 
     * @throws Exception
     */
    public void testHandleNonExistingFile() throws Exception {

        osdServer.add(new OSD(osdConfig1));

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();

        c.createVolume(VOLNAME, authHeader, uc, sp.getPolicy(),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, 0777);

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

        TimeSync.initializeLocal(60 * 1000, 50).waitForStartup();

    }

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

        c.createVolume(VOLNAME, authHeader, uc, sp.getPolicy(),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, 0777);

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

            // set ReplicationUpdatePolicy to RONLY
            fileInfos = osdDrain.setReplicationUpdatePolicyRonly(fileInfos);
            for (File file : files) {
                assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, file.getReplicaUpdatePolicy());
            }

            // set Files read-only
            fileInfos = osdDrain.setFilesReadOnlyAttribute(fileInfos, true);
            for (File file : files) {
                assertTrue(file.isReadOnly());
            }

            // start second OSD
            osdServer.add(new OSD(osdConfig2));

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
            List<FileInformation> toSetROList = new LinkedList<FileInformation>();
            for (FileInformation fileInfo : fileInfos) {
                if (!fileInfo.wasAlreadyReadOnly)
                    toSetROList.add(fileInfo);
            }
            osdDrain.setFilesReadOnlyAttribute(toSetROList, false);
            for (File file : files) {
                assertFalse(file.isReadOnly());
            }

            // set ReplicationUpdatePolicy to original value
            osdDrain.setReplicationPolicyToOriginal(fileInfos);
            for (File file : files) {
                assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, file.getReplicaUpdatePolicy());
            }

        } catch (OSDDrainException e) {
            osdDrain.handleException(e, true);
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

        TimeSync.initializeLocal(60 * 1000, 50).waitForStartup();

    }

    public void testMultipleMRCs() throws Exception {
        osdServer.add(new OSD(osdConfig1));
        osdServer.add(new OSD(osdConfig2));
        osdServer.add(new OSD(osdConfig3));
        
        MRC mrc2 = new MRC(SetupUtils.createMRC2Config(), SetupUtils.createMRC2dbsConfig());
        String mrc2UUID = SetupUtils.createMRC2Config().getUUID().toString();
        
        final int NUMBER_OF_FILES = 10;
        LinkedList<String> fileNames = new LinkedList<String>();
        for (int i = 0; i < NUMBER_OF_FILES; i++) {
            fileNames.add("foo"+i);
        }

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();
        
        c.createVolume(VOLNAME, authHeader, uc, sp.getPolicy(),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, 0777, 
                SetupUtils.getMRC1UUID().toString());
        c.createVolume(VOLNAME2, authHeader, uc, sp.getPolicy(), 
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL , 0777, mrc2UUID);

        Volume volume1 = c.getVolume(VOLNAME, uc);
        Volume volume2 = c.getVolume(VOLNAME2, uc);

        final int SIZE = 1024 * 200;
        byte[] data = new byte[SIZE];

        File files[] = new File[NUMBER_OF_FILES];

        for (int i = 0; i < NUMBER_OF_FILES; i++) {
            if (i%2 == 0) {
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

            // set ReplicationUpdatePolicy to RONLY
            fileInfos = osdDrain.setReplicationUpdatePolicyRonly(fileInfos);

            // set Files read-only
            fileInfos = osdDrain.setFilesReadOnlyAttribute(fileInfos, true);

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
            List<FileInformation> toSetROList = new LinkedList<FileInformation>();
            for (FileInformation fileInfo : fileInfos) {
                if (!fileInfo.wasAlreadyReadOnly)
                    toSetROList.add(fileInfo);
            }
            osdDrain.setFilesReadOnlyAttribute(toSetROList, false);
            for (File file : files) {
                assertFalse(file.isReadOnly());
            }

            // set ReplicationUpdatePolicy to original value
            osdDrain.setReplicationPolicyToOriginal(fileInfos);
            for (File file : files) {
                assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, file.getReplicaUpdatePolicy());
            }

        } catch (OSDDrainException e) {
            osdDrain.handleException(e, true);
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
        
        TimeSync.initializeLocal(60 * 1000, 50).waitForStartup();
        
    }

}
