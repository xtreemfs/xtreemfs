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
import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.utils.xtfs_scrub.xtfs_scrub;

public class ScrubberTest {

    private static MRCRequestDispatcher  mrc1;

    private static MRCConfig             mrcCfg1;

    private static BabuDBConfig          mrcDBCfg1;

    private static OSDConfig             osdConfig1, osdConfig2, osdConfig3, osdConfig4;

    private static DIRConfig             dsCfg;

    private static OSD                   osd1, osd2, osd3, osd4;

    private static InetSocketAddress     mrc1Address;

    private static InetSocketAddress     dirAddress;

    private static int                   accessMode;

    private static TestEnvironment       testEnv;

    private static AdminClient           client;

    private static byte[]                content;

    private static final UserCredentials userCredentials = xtfs_scrub.credentials;

    public ScrubberTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @BeforeClass
    public static void setUp() throws Exception {

        System.out.println("TEST: ScrubberTest");

        Logging.start(Logging.LEVEL_WARN);

        accessMode = 0777; // rwxrwxrwx

        dsCfg = SetupUtils.createDIRConfig();
        dirAddress = SetupUtils.getDIRAddr();

        mrcCfg1 = SetupUtils.createMRC1Config();
        mrcDBCfg1 = SetupUtils.createMRC1dbsConfig();
        mrc1Address = SetupUtils.getMRC1Addr();

        SetupUtils.CHECKSUMS_ON = true;
        osdConfig1 = SetupUtils.createOSD1Config();
        osdConfig2 = SetupUtils.createOSD2Config();
        osdConfig3 = SetupUtils.createOSD3Config();
        osdConfig4 = SetupUtils.createOSD4Config();
        SetupUtils.CHECKSUMS_ON = false;

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT });
        testEnv.start();

        // start the OSD
        osd1 = new OSD(osdConfig1);

        // start MRC
        mrc1 = new MRCRequestDispatcher(mrcCfg1, mrcDBCfg1);
        mrc1.startup();

        // create client

        client = ClientFactory.createAdminClient(dirAddress.getHostName() + ":" + dirAddress.getPort(),
                userCredentials, null, new Options());
        client.start();

        // create content
        String tmp = "";
        for (int i = 0; i < 12000; i++)
            tmp = tmp.concat("Hello World ");
        content = tmp.getBytes();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        client.shutdown();
        osd1.shutdown();
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
        client.setOSDServiceStatus(osdConfig1.getUUID().toString(), ServiceStatus.SERVICE_STATUS_REMOVED);

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
        client.setOSDServiceStatus(osdConfig1.getUUID().toString(), ServiceStatus.SERVICE_STATUS_AVAIL);
    }

    @Test
    public void testNonReplicatedFileWithWrongChecksum() throws Exception {
        final String VOLUME_NAME = "testNonReplicatedFileWithWrongChecksum";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
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
        HashStorageLayout hsl = new HashStorageLayout(osdConfig1, new MetadataCache());
        String filePath = hsl.generateAbsoluteFilePath(file.getGlobalFileId());

        File fileDir = new File(filePath);
        FileWriter fw = new FileWriter(fileDir.listFiles()[0], true);

        fw.write("foofoofoofoofoo");
        fw.close();

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // TODO(lukas): add asserts
        
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
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();

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

    @Test
    public void testROnlyReplicatedFileWithLostReplica() throws Exception {
        final String VOLUME_NAME = "testROnlyReplicatedFileWithLostReplica";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, content.length, 0);

        // start new OSDs and give them some time for start up
        osd2 = new OSD(osdConfig2);
        osd3 = new OSD(osdConfig3);
        osd4 = new OSD(osdConfig4);
        Thread.sleep(10000);

        // set replica update Policy
        RPCResponse<?> r = testEnv.getMrcClient().xtreemfs_set_replica_update_policy(mrc1Address,
                RPCAuthentication.authNone, userCredentials, file.getGlobalFileId(),
                ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY);
        r.get();
        r.freeBuffers();

        // create new replicas
        Replica newRepl = file.getReplica(0).toBuilder().setOsdUuids(0, osdConfig2.getUUID().toString())
                .setReplicationFlags(REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber() & REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST.getNumber()).build();
        volume.addReplica(userCredentials, FILE_NAME, newRepl);

        newRepl = file.getReplica(0).toBuilder().setOsdUuids(0, osdConfig3.getUUID().toString())
                .setReplicationFlags(REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber() & REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST.getNumber()).build();
        volume.addReplica(userCredentials, FILE_NAME, newRepl);

        // mark OSD3 as removed
        client.setOSDServiceStatus(osdConfig3.getUUID().toString(), ServiceStatus.SERVICE_STATUS_REMOVED);

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // file has still three replicas
        assertEquals(3, file.getReplicasList().size());
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        // shut down OSDs
        osd2.shutdown();
        osd3.shutdown();
        osd4.shutdown();
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

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // write file
        file.write(userCredentials, content, file.getStripingPolicy().getStripeSize() * 1024 - 20, 0);

        // start new OSDs
        osd2 = new OSD(osdConfig2);

        // set replica update Policy
        RPCResponse<?> r = testEnv.getMrcClient().xtreemfs_set_replica_update_policy(mrc1Address,
                RPCAuthentication.authNone, userCredentials, file.getGlobalFileId(),
                ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY);
        r.get();
        r.freeBuffers();

        // create new replicas
        Replica newRepl = file.getReplica(0).toBuilder().setOsdUuids(0, osdConfig2.getUUID().toString())
                .setReplicationFlags(REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber() & REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST.getNumber()).build();
        volume.addReplica(userCredentials, FILE_NAME, newRepl);

        // modify first Object on OSD
        HashStorageLayout hsl = new HashStorageLayout(osdConfig1, new MetadataCache());
        String filePath = hsl.generateAbsoluteFilePath(file.getGlobalFileId());

        File fileDir = new File(filePath);
        FileWriter fw = new FileWriter(fileDir.listFiles()[0], true);

        fw.write("foofoofoofoofoo");
        fw.close();

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // TODO(lukas): add asserts
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        // shut down OSDs
        osd2.shutdown();
    }

    @Test
    public void testRWReplicatedFileWithLostReplica() throws Exception {
        final String VOLUME_NAME = "testRWReplicatedFileWithLostReplica";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // start new OSDs and give them some time for start up
        osd2 = new OSD(osdConfig2);
        osd3 = new OSD(osdConfig3);
        osd4 = new OSD(osdConfig4);
        Thread.sleep(10000);

        // set replica update Policy
        RPCResponse<?> r = testEnv.getMrcClient().xtreemfs_set_replica_update_policy(mrc1Address,
                RPCAuthentication.authNone, userCredentials, file.getGlobalFileId(),
                ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE);
        r.get();
        r.freeBuffers();

        // write file
        file.write(userCredentials, content, content.length, 0);

        // create new replicas
        Replica newRepl = file.getReplica(0).toBuilder().setOsdUuids(0, osdConfig2.getUUID().toString())
                .setReplicationFlags(REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber() & REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST.getNumber()).build();
        volume.addReplica(userCredentials, FILE_NAME, newRepl);

        newRepl = file.getReplica(0).toBuilder().setOsdUuids(0, osdConfig3.getUUID().toString())
                .setReplicationFlags(REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber() & REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST.getNumber()).build();
        volume.addReplica(userCredentials, FILE_NAME, newRepl);

        // mark OSD3 as removed
        client.setOSDServiceStatus(osdConfig3.getUUID().toString(), ServiceStatus.SERVICE_STATUS_REMOVED);

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // file has still three replicas
        assertEquals(3, file.getReplicasList().size());
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        // shut down OSDs
        osd2.shutdown();
        osd3.shutdown();
        osd4.shutdown();
    }

    @Test
    public void testRWReplicatedFileWithWrongChecksum() throws Exception {
        final String VOLUME_NAME = "testRWReplicatedFileWithWrongChecksum";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // start new OSDs
        osd2 = new OSD(osdConfig2);

        // set replica update Policy
        RPCResponse<?> r = testEnv.getMrcClient().xtreemfs_set_replica_update_policy(mrc1Address,
                RPCAuthentication.authNone, userCredentials, file.getGlobalFileId(),
                ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE);
        r.get();
        r.freeBuffers();

        // write file
        file.write(userCredentials, content, file.getStripingPolicy().getStripeSize() * 1024 - 20, 0);

        // create new replicas
        Replica newRepl = file.getReplica(0).toBuilder().setOsdUuids(0, osdConfig2.getUUID().toString())
                .setReplicationFlags(REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber() & REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST.getNumber()).build();
        volume.addReplica(userCredentials, FILE_NAME, newRepl);

        // modify first Object on OSD
        HashStorageLayout hsl = new HashStorageLayout(osdConfig1, new MetadataCache());
        String filePath = hsl.generateAbsoluteFilePath(file.getGlobalFileId());

        File fileDir = new File(filePath);
        FileWriter fw = new FileWriter(fileDir.listFiles()[0], true);

        fw.write("foofoofoofoofoo");
        fw.close();

        // scrub volume
        xtfs_scrub scrubber = new xtfs_scrub(client, volume, 3, true, true, true);
        scrubber.scrub();

        // TODO(lukas): add asserts
        
        file.close();

        // delete volume
        client.deleteVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);

        // shut down OSDs
        osd2.shutdown();
    }

    // not supported yet
    public void testRWReplicatedFileWithWrongFileSizeOnMrc() throws Exception {
        final String VOLUME_NAME = "testRWReplicatedFileWithWrongFileSizeOnMrc";
        final String FILE_NAME = "test0.txt";

        // create Volume
        client.createVolume(mrc1Address.getHostName() + ":" + mrc1Address.getPort(),
                RPCAuthentication.authNone, userCredentials, VOLUME_NAME);
        AdminVolume volume = client.openVolume(VOLUME_NAME, null, new Options());
        volume.start();

        // create file
        AdminFileHandle file = volume.openFile(
                userCredentials,
                FILE_NAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), accessMode);

        // start new OSDs
        osd2 = new OSD(osdConfig2);

        // set replica update Policy
        RPCResponse<?> r = testEnv.getMrcClient().xtreemfs_set_replica_update_policy(mrc1Address,
                RPCAuthentication.authNone, userCredentials, file.getGlobalFileId(),
                ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE);
        r.get();
        r.freeBuffers();

        // write file
        file.write(userCredentials, content, content.length, 0);

        // create new replicas
        Replica newRepl = file.getReplica(0).toBuilder().setOsdUuids(0, osdConfig2.getUUID().toString())
                .setReplicationFlags(REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber() & REPL_FLAG.REPL_FLAG_STRATEGY_RAREST_FIRST.getNumber()).build();
        volume.addReplica(userCredentials, FILE_NAME, newRepl);

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

        // shut down OSD
        osd2.shutdown();
    }
}
