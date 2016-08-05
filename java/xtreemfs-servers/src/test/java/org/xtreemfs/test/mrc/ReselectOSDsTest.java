/*
 * Copyright (c) 2015 by Johannes Dillmann,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.libxtreemfs.Helper;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_reselect_osdsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_reselect_osdsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;
import org.xtreemfs.test.TestHelper;

public class ReselectOSDsTest {
    @Rule
    public final TestRule          testLog = TestHelper.testLog;

    private MRCServiceClient       mrcClient;

    private InetSocketAddress      mrcAddress;

    private String                 mrcServiceAddress;
    private String                 dirServiceAddress;

    private static UserCredentials userCredentials;

    private static Auth            auth    = RPCAuthentication.authNone;

    private TestEnvironment        testEnv;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    @Before
    public void setUp() throws Exception {
        testEnv = new TestEnvironment(Services.DIR_CLIENT, Services.TIME_SYNC, Services.UUID_RESOLVER,
                Services.MRC_CLIENT, Services.DIR_SERVICE, Services.MRC, 
                Services.OSD, Services.OSD, Services.OSD, Services.OSD
                );
        testEnv.start();

        mrcAddress = testEnv.getMRCAddress();
        mrcClient = testEnv.getMrcClient();

        dirServiceAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcServiceAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }

    @Test
    public void testDistinctReselection() throws Exception {
        String volumeName = "testDistinctReselection";
        String fileName = "testfile";

        Options options = new Options();
        AdminClient client = ClientFactory.createAdminClient(ClientType.NATIVE, dirServiceAddress, userCredentials, null, options);
        client.start();

        client.createVolume(mrcServiceAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 2, 0);
        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));

        
        AdminFileHandle file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR),
                0777);

        byte[] dataIn = SetupUtils.generateData(256 * 1024).getData();
        file.write(userCredentials, dataIn, 256 * 1024, 0);
        List<Replica> replicas1 = file.getReplicasList();
        file.close();

        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_REVERSE }));

        xtreemfs_reselect_osdsRequest input = xtreemfs_reselect_osdsRequest.newBuilder()
                .setVolumeName(volumeName).setPath(fileName).build();
        RPCResponse<xtreemfs_reselect_osdsResponse> rpcResponse = mrcClient.xtreemfs_reselect_osds(mrcAddress, auth,
                userCredentials, input);
        xtreemfs_reselect_osdsResponse response = rpcResponse.get();
        rpcResponse.freeBuffers();

        // Wait for the new XLocSet to install (will probably take at least leaseTimeout seconds)
        Thread.sleep(20 * 1000);

        file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR));
        List<Replica> replicas2 = file.getReplicasList();

        printReplicaList(replicas1);
        printReplicaList(replicas2);

        // Assert, that there are no replicas contained in both lists.
        HashSet<Replica> replicaSet = new HashSet<Replica>();
        replicaSet.addAll(replicas1);
        replicaSet.addAll(replicas2);

        assertEquals(replicas1.size() + replicas2.size(), replicaSet.size());

        byte[] dataOut = new byte[dataIn.length];
        file.read(userCredentials, dataOut, dataOut.length, 0);
        file.close();

        assertArrayEquals(dataIn, dataOut);

        volume.close();
        client.shutdown();
    }

    @Test
    public void testOverlappingReselection() throws Exception {
        String volumeName = "testOverlappingReselection";
        String fileName = "testfile";

        Options options = new Options();
        AdminClient client = ClientFactory.createAdminClient(ClientType.NATIVE, dirServiceAddress, userCredentials, null, options);
        client.start();

        client.createVolume(mrcServiceAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 3, 0);
        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));

        AdminFileHandle file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR),
                0777);

        byte[] dataIn = SetupUtils.generateData(256 * 1024).getData();
        file.write(userCredentials, dataIn, 256 * 1024, 0);
        List<Replica> replicas1 = file.getReplicasList();
        file.close();

        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_REVERSE }));

        xtreemfs_reselect_osdsRequest input = xtreemfs_reselect_osdsRequest.newBuilder().setVolumeName(volumeName)
                .setPath(fileName).build();
        RPCResponse<xtreemfs_reselect_osdsResponse> rpcResponse = mrcClient.xtreemfs_reselect_osds(mrcAddress, auth,
                userCredentials, input);
        xtreemfs_reselect_osdsResponse response = rpcResponse.get();
        rpcResponse.freeBuffers();

        // Wait for the new XLocSet to install (will probably take at least leaseTimeout seconds)
        Thread.sleep(20 * 1000);

        file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR));
        List<Replica> replicas2 = file.getReplicasList();

        printReplicaList(replicas1);
        printReplicaList(replicas2);

        HashSet<Replica> replicaSet = new HashSet<Replica>();
        replicaSet.addAll(replicas1);
        replicaSet.addAll(replicas2);

        int overlapping = 0;
        for (Replica replica : replicas1) {
            if (replicas2.contains(replica)) {
                overlapping++;
            }
        }

        assertTrue("Expected replica sets to overlap", overlapping > 0);

        byte[] dataOut = new byte[dataIn.length];
        file.read(userCredentials, dataOut, dataOut.length, 0);
        file.close();

        assertArrayEquals(dataIn, dataOut);

        volume.close();
        client.shutdown();
    }

    private void printReplicaList(List<Replica> replicas) {
        if (Logging.isDebug()) {
            StringBuilder str = new StringBuilder();
            for (int i = 0, m = replicas.size(); i < m; ++i) {
                str.append(replicas.get(i).getOsdUuids(0));
                if ((i + 1) < m)
                    str.append(", ");
            }
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Replicas: %s", str.toString());
        }
    }
}
