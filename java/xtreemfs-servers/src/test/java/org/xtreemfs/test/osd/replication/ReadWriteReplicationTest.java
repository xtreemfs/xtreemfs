/*
 * Copyright (c) 2014 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.osd.replication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * @author Robert Schmidtke
 *
 */
public class ReadWriteReplicationTest {

    @Rule
    public final TestRule   testLog = TestHelper.testLog;

    private TestEnvironment testEnv;

    private UserCredentials userCredentials;

    private String          dirAddress, mrcAddress;

    private Options         options;

    private AdminClient     client;

    private Auth            auth;

    @Before
    public void setup() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(Logging.LEVEL_NOTICE);

        SetupUtils.CHECKSUMS_ON = true;
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.RPC_CLIENT, TestEnvironment.Services.MRC, TestEnvironment.Services.MRC_CLIENT,
                TestEnvironment.Services.OSD, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD });
        testEnv.start();
        SetupUtils.CHECKSUMS_ON = false;

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        auth = RPCAuthentication.authNone;

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        options = new Options();

        client = ClientFactory.createAdminClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();
    }

    @After
    public void cleanup() {
        testEnv.shutdown();
        client.shutdown();
    }

    @Test
    public void testAlignedReplicatedWrite() throws Exception {
        testReplicatedWrite(128, 128);
    }

    @Test
    public void testUnalignedReplicatedWrite() throws Exception {
        testReplicatedWrite(128, 32);
    }

    @Test
    public void testUnalignedReplicatedWrite2() throws Exception {
        testReplicatedWrite(128, 160);
    }

    private void testReplicatedWrite(int stripeSize, int fileSize) throws Exception {
        final String volumeName = "testReplicatedWrite";
        final String path = "/replicatedWriteTest.txt";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName, 0777, "test", "test",
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, StripingPolicyType.STRIPING_POLICY_RAID0,
                stripeSize, 1, new ArrayList<KeyValuePair>());

        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 3,
                ReplicationFlags.setSequentialStrategy(0));

        AdminFileHandle fileHandle = volume.openFile(userCredentials, path,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT_VALUE | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC_VALUE
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR_VALUE, 0777);

        Random random = new Random(0);
        byte[] data = new byte[fileSize * 1024];
        random.nextBytes(data);

        fileHandle.write(userCredentials, data, 0, data.length, 0);
        fileHandle.close();

        fileHandle = volume.openFile(userCredentials, path, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY_VALUE, 0777);

        List<Replica> replicas = fileHandle.getReplicasList();

        // OSDs 0+1 live
        readMajority(replicas, fileHandle, this, data, null, new int[] { 2 });

        // OSDs 1+2 live
        readMajority(replicas, fileHandle, this, data, new int[] { 2 }, new int[] { 0 });

        // OSDs 2+0 live
        readMajority(replicas, fileHandle, this, data, new int[] { 0 }, new int[] { 1 });

        // OSDs 0+1 live, but this time with OSD 2 being the primary
        readMajority(replicas, fileHandle, this, data, new int[] { 1 }, new int[] { 2 });

        fileHandle.close();
        client.deleteVolume(auth, userCredentials, volumeName);
    }

    private void readMajority(List<Replica> replicas, FileHandle fileHandle, Object me, byte[] expected,
            int[] startList, int[] stopList) throws Exception {
        if (stopList != null) {
            for (int i : stopList) {
                Logging.logMessage(Logging.LEVEL_NOTICE, me, "Stopping OSD %d with UUID %s", i, replicas.get(i)
                        .getOsdUuids(0));
                testEnv.stopOSD(replicas.get(i).getOsdUuids(0));
            }
        }

        if (startList != null) {
            for (int i : startList) {
                Logging.logMessage(Logging.LEVEL_NOTICE, me, "Starting OSD %d with UUID %s", i, replicas.get(i)
                        .getOsdUuids(0));
                testEnv.startOSD(replicas.get(i).getOsdUuids(0));
            }
        }

        byte[] readData = new byte[expected.length];
        new Random().nextBytes(readData);
        fileHandle.read(userCredentials, readData, 0, readData.length, 0);
        Assert.assertArrayEquals("Expected read data to be the same as written data on OSD", expected, readData);
    }

}
