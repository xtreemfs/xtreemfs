/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *               2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestHelper;

/**
 * Read a partial replica "simultaneously" by two threads.
 * 
 * This test was originally created to track a problem in the Read-Only replication which occurred when the
 * on-demand replication of the same object was triggered by two concurrent requests. However, the test failed
 * to reproduce the issue and it was resolved otherwise.
 * 
 * @author mberlin
 * 
 */
public class ReadOnlyReplicationTest {
    @Rule
    public final TestRule          testLog = TestHelper.testLog;

    private static TestEnvironment testEnv;

    private static UserCredentials userCredentials;

    private static Auth            auth    = RPCAuthentication.authNone;

    private static String          mrcAddress;

    private static String          dirAddress;

    private static Client          client;

    private static Options         options;

    @BeforeClass
    public static void initializeTest() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(Logging.LEVEL_WARN);

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE, TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD});
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        new MRCServiceClient(testEnv.getRpcClient(), testEnv.getMRCAddress());

        options = new Options();
        client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();
    }

    @AfterClass
    public static void shutdownTest() throws Exception {
        testEnv.shutdown();

        client.shutdown();
    }

    @Test
    public void testMultipleReadRequests() throws Exception {
        final String volumeName = "testMarkReplicaAsComplete";
        final String path = "/test.txt";

        // Create client.
        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        Volume volume = client.openVolume(volumeName, null, options);
        volume.start();
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 2,
                ReplicationFlags.setPartialReplica(ReplicationFlags.setSequentialPrefetchingStrategy(0)));
        volume.setOSDSelectionPolicy(userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT }));
        volume.setReplicaSelectionPolicy(userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_REVERSE }));

        // open FileHandle.
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                path,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);
        // write some content.
        String content = "";
        for (int i = 0; i < 128 * 1024 / 12; i++) {
            content = content.concat("Hello World ");
        }
        byte[] bytesIn = content.getBytes();
        int length = bytesIn.length;
        fileHandle.write(userCredentials, bytesIn, length, 0);
        fileHandle.close();

        // Read file (probably from the partial replica).
        final FileHandle fileHandleRead = volume.openFile(userCredentials, path,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        final int readLength = 64 * 1024;
        Thread th1 = new Thread() {
            @Override
            public void run() {
                byte[] bytesOut1 = new byte[readLength];
                try {
                    fileHandleRead.read(userCredentials, bytesOut1, readLength, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread th2 = new Thread() {
            @Override
            public void run() {
                byte[] bytesOut2 = new byte[readLength];
                try {
                    fileHandleRead.read(userCredentials, bytesOut2, readLength, readLength);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        th1.start();
        Thread.sleep((long) (Math.random() * 0));
        th2.start();

        th1.join();
        th2.join();
        fileHandleRead.close();

        volume.close();
        client.shutdown();
    }
}