/*
 * Copyright (c) 2012-2013 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.libxtreemfs.Helper;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestHelper;
import org.xtreemfs.utils.xtfs_scrub.xtfs_scrub;

public class ScrubberTest {
    @Rule
    public final TestRule                testLog         = TestHelper.testLog;

    private static MRCConfig             mrcCfg1;

    private static BabuDBConfig          mrcDBCfg1;

    private static InetSocketAddress     mrc1Address;

    private static InetSocketAddress     dirAddress;

    private static int                   accessMode;

    private static TestEnvironment       testEnv;

    private static AdminClient           client;

    private static byte[]                content;

    private static final UserCredentials userCredentials = xtfs_scrub.credentials;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(Logging.LEVEL_WARN);

        accessMode = 0777; // rwxrwxrwx

        dirAddress = SetupUtils.getDIRAddr();

        mrcCfg1 = SetupUtils.createMRC1Config();
        mrcDBCfg1 = SetupUtils.createMRC1dbsConfig();
        mrc1Address = SetupUtils.getMRC1Addr();

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();
        SetupUtils.CHECKSUMS_ON = true;
        // startup test environment
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT, TestEnvironment.Services.MRC,
                TestEnvironment.Services.OSD,
                TestEnvironment.Services.OSD, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD });
        testEnv.start();
        SetupUtils.CHECKSUMS_ON = false;
        
        // create client
        client = ClientFactory.createAdminClient(ClientType.NATIVE, dirAddress.getHostName() + ":" + dirAddress.getPort(),
                userCredentials, null, new Options());
        client.start();

        // create content
        String tmp = "";
        for (int i = 0; i < 12000; i++) {
            tmp = tmp.concat("Hello World ");
        }
        content = tmp.getBytes();
    }

    @AfterClass
    public static void shutdownTest() throws Exception {
        client.shutdown();
        testEnv.shutdown();
    }

    @Test
    public void testNonReplicatedFileOnDeadOSD() throws Exception {
        final String VOLUME_NAME = "testNonReplicatedFileOnDeadOSD";
        final String FILE_NAME = "myDir/test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();

        // create dir and file
        volume.createDirectory(userCredentials, "myDir", accessMode);
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, content.length, 0);
        file.close();

        DirectoryEntries de = volume.readDir(userCredentials, "/myDir/", 0, 2, true);
        assertEquals(3, de.getEntriesCount());

        // mark OSD as removed
        client.setOSDServiceStatus(file.getReplica(0).getOsdUuids(0), ServiceStatus.SERVICE_STATUS_REMOVED);

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // file should be removed
        de = volume.readDir(userCredentials, "/myDir/", 0, 2, true);
        assertEquals(2, de.getEntriesCount());

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        // mark OSD as available
        client.setOSDServiceStatus(file.getReplica(0).getOsdUuids(0), ServiceStatus.SERVICE_STATUS_AVAIL);
    }

    @Test
    public void testNonReplicatedFileWithWrongChecksum() throws Exception {
        final String VOLUME_NAME = "testNonReplicatedFileWithWrongChecksum";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        // Disable the metadatacache to deal with mixed JNI/JAVA implementations. 
        Options options = new Options();
        options.setMetadataCacheSize(0);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, options);
        volume.start();

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, file.getStripingPolicy().getStripeSize() * 1024 - 20, 0);

        // modify first Object on OSD

        File objFile = openObjectFile(file, 0, 0);
        FileWriter fw = new FileWriter(objFile, false);

        fw.write("foofoofoofoofoo");
        fw.close();

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
    }

    @Test
    public void testNonReplicatedFileWithWrongFileSizeOnMrc() throws Exception {
        final String VOLUME_NAME = "testNonReplicatedFileWithWrongFileSizeOnMrc";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        // Disable the metadatacache to deal with mixed JNI/JAVA implementations.
        Options options = new Options();
        options.setMetadataCacheSize(0);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, options);
        volume.start();

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, content.length, 0);

        Stat stat = file.getAttr(userCredentials);
        assertEquals(content.length, stat.getSize());

        // truncate file on MRC
        file.truncate(userCredentials, 10, true);

        stat = file.getAttr(userCredentials);
        assertEquals(10, stat.getSize());

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // file size should be correct from 10 to content.length
        stat = file.getAttr(userCredentials);
        assertEquals(content.length, stat.getSize());
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
    }

    @Test
    public void testROnlyReplicatedFileWithLostReplica() throws Exception {
        final String VOLUME_NAME = "testROnlyReplicatedFileWithLostReplica";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();

        // set replica update Policy
        int replicationFlags = ReplicationFlags.setRarestFirstStrategy(0);
        replicationFlags = ReplicationFlags.setFullReplica(replicationFlags);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 3,
                replicationFlags);

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, content.length, 0);

        // re-open file to trigger replication
        file.close();
        file = volume.openFile(userCredentials, FILE_NAME, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());

        // wait for replication
        Thread.sleep(5000);

        // mark OSD of the second replica as removed
        client.setOSDServiceStatus(file.getReplica(1).getOsdUuids(0), ServiceStatus.SERVICE_STATUS_REMOVED);

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // re-open file
        file.close();
        file = volume.openFile(userCredentials, FILE_NAME, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());

        // file has still three replicas
        assertEquals(3, file.getReplicasList().size());
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        client.setOSDServiceStatus(file.getReplica(1).getOsdUuids(0), ServiceStatus.SERVICE_STATUS_AVAIL);
    }

    @Test
    public void testROnlyReplicatedFileWithWrongChecksum() throws Exception {
        final String VOLUME_NAME = "testROnlyReplicatedFileWithWrongChecksum";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();

        // set replica update Policy
        int replicationFlags = ReplicationFlags.setRarestFirstStrategy(0);
        replicationFlags = ReplicationFlags.setFullReplica(replicationFlags);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 3,
                replicationFlags);

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        int contentSize = file.getStripingPolicy().getStripeSize() * 1024;
        file.write(userCredentials, content, contentSize, 0);

        // re-open file to trigger replication
        file.close();

        // wait for replication
        Thread.sleep(500);

        file = volume.openFile(userCredentials, FILE_NAME, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());

        // modify first Object of the second replica on OSD
        File objFile = openObjectFile(file, 1, 0);
        FileWriter fw = new FileWriter(objFile, false);

        fw.write("foofoofoofoofoo");
        fw.close();

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // srubber finishes before actual data has been fully written
        Thread.sleep(5000);
        
        //object file should be as long as the originally written content
        objFile = openObjectFile(file, 1, 0);
        assertEquals(contentSize, objFile.length());
        
        // delete volume
        file.close();
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
    }

    //@Test
    public void testRWReplicatedFileWithLostReplica() throws Exception {
        final String VOLUME_NAME = "testRWReplicatedFileWithLostReplica";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(), RPCAuthentication.authNone,
                userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();
        
        // set replica update Policy
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ,
                3, 0);

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, content.length, 0);

        // mark OSD of the second replica as removed
        client.setOSDServiceStatus(file.getReplica(1).getOsdUuids(0), ServiceStatus.SERVICE_STATUS_REMOVED);

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // re-open file
        file.close();
        file = volume.openFile(userCredentials, FILE_NAME, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        // file has still three replicas
        assertEquals(3, file.getReplicasList().size());
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        client.setOSDServiceStatus(file.getReplica(1).getOsdUuids(0), ServiceStatus.SERVICE_STATUS_AVAIL);
    }

    @Test
    public void testRWReplicatedFileWithWrongChecksum() throws Exception {
        final String VOLUME_NAME = "testRWReplicatedFileWithWrongChecksum";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        
        // Disable the metadatacache to deal with mixed JNI/JAVA implementations. 
        Options options = new Options();
        options.setMetadataCacheSize(0);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, options);
        volume.start();

        // set replica update Policy
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 3, 0);

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, content.length, 0);

        // modify second Object
        File objFile = openObjectFile(file, 0, 1);
        FileWriter fw = new FileWriter(objFile, false);

        fw.write("foofoofoofoofoo");
        fw.close();

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
    }

    @Test
    public void testRWReplicatedFileWithWrongFileSizeOnMrc() throws Exception {
        final String VOLUME_NAME = "testRWReplicatedFileWithWrongFileSizeOnMrc";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        // Disable the metadatacache to deal with mixed JNI/JAVA implementations.
        Options options = new Options();
        options.setMetadataCacheSize(0);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, options);
        volume.start();

        // set replica update Policy
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ,
                3, 0);

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, content.length, 0);

        Stat s = file.getAttr(userCredentials);
        assertEquals(content.length, s.getSize());

        // truncate file on MRC
        file.truncate(userCredentials, 10, true);
        
        s = file.getAttr(userCredentials);
        assertEquals(10, s.getSize());

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // file size should be correct from 10 to content.length
        s = file.getAttr(userCredentials);
        assertEquals(content.length, s.getSize());
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
    }
    
    private File openObjectFile(AdminFileHandle file, int replicaIndex, int objectIndex) throws IOException {
    	
    	String osdUUID = Helper.getOSDUUIDFromObjectNo(file.getReplica(replicaIndex), objectIndex);
        HashStorageLayout hsl = new HashStorageLayout(testEnv.getOSDConfig(osdUUID),
                new MetadataCache());
        String filePath = hsl.generateAbsoluteFilePath(file.getGlobalFileId());

        File fileDir = new File(filePath);
        File[] objFiles = fileDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return !name.matches("\\..*");
            }
        });
        return objFiles[objectIndex / file.getStripingPolicy(replicaIndex).getWidth()];
    }
}
