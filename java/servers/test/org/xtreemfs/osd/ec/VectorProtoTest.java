/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.Helper;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.intervals.ListIntervalVector;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_get_interval_vectorsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_get_interval_vectorsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class VectorProtoTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    ServiceUUID           osdUUID;

    FileCredentials       fileCredentials;

    UserCredentials       userCredentials;

    String                fileId;

    Capability            cap;

    OSDConfig             osdConfig;

    OSDServiceClient      osdClient;

    TestEnvironment       testEnv;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @Before
    public void setUp() throws Exception {
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { 
                TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, 
                TestEnvironment.Services.UUID_RESOLVER,
                //TestEnvironment.Services.MRC,
                // TestEnvironment.Services.MRC_CLIENT,
                TestEnvironment.Services.OSD,
                TestEnvironment.Services.OSD_CLIENT });
        testEnv.start();

        osdClient = testEnv.getOSDClient();
        osdConfig = testEnv.getOSDConfig();
        osdUUID = osdConfig.getUUID();

        fileId = "ABCDEF:1";

        fileCredentials = getCreds(fileId, 1, 0, 128);
        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
    }

    public static StripingPolicy getECStripingPolicy(int width, int parity, int stripeSize) {
        return StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_ERASURECODE).setWidth(width)
                .setParityWidth(parity).setStripeSize(stripeSize).build();
    }

    Capability getCap(String fileId) {
        // TODO (jdillmann): Check if this Cap is correct
        return new Capability(fileId,
                Helper.flagsToInt(SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR), 60,
                System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0,
                osdConfig.getCapabilitySecret());
    }

    FileCredentials getCreds(String fileId, int width, int parity, int stripeSize) {
        Replica r = Replica.newBuilder().setReplicationFlags(0).setStripingPolicy(getECStripingPolicy(1, 0, 128))
                .addOsdUuids(osdUUID.toString()).build();
        XLocSet xloc = XLocSet.newBuilder().setReadOnlyFileSize(0)
                .setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_EC).addReplicas(r).setVersion(1).build();
        return FileCredentials.newBuilder().setXcap(getCap(fileId).getXCap()).setXlocs(xloc).build();
    }

    StripingPolicyImpl getStripingPolicyImplementation(FileCredentials fileCredentials) {
        return StripingPolicyImpl.getPolicy(fileCredentials.getXlocs().getReplicas(0), 0);
    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
    }

    @Test
    public void testGetVectors() throws Exception {
        AVLTreeIntervalVector curVector, nextVector;
        IntervalVector expected;
        List<Interval> intervals = new LinkedList<Interval>();
        HashStorageLayout layout = new HashStorageLayout(osdConfig, new MetadataCache());



        xtreemfs_ec_get_interval_vectorsRequest request = xtreemfs_ec_get_interval_vectorsRequest.newBuilder()
                .setFileId(fileId)
                .setFileCredentials(fileCredentials)
                .build();

        RPCResponse<xtreemfs_ec_get_interval_vectorsResponse> rpcResponse;
        xtreemfs_ec_get_interval_vectorsResponse response;

        // Test non-existent vectors
        rpcResponse = osdClient.xtreemfs_ec_get_interval_vectors(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request);
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }
        assertEquals(0, response.getCurIntervalsCount());
        assertEquals(0, response.getNextIntervalsCount());

        // Test retrieving existing vectors with a gap
        intervals.add(new ObjectInterval(0, 1024, 1, 0));
        intervals.add(new ObjectInterval(2048, 4096, 2, 0));
        layout.setECIntervalVector(fileId, intervals, false, true);
        layout.setECIntervalVector(fileId, intervals, true, true);

        rpcResponse = osdClient.xtreemfs_ec_get_interval_vectors(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request);
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        intervals.add(1, new ObjectInterval(1024, 2048, -1, -1));
        expected = new ListIntervalVector(intervals);

        curVector = new AVLTreeIntervalVector();
        for (int i = 0; i < response.getCurIntervalsCount(); i++) {
            curVector.insert(new ProtoInterval(response.getCurIntervals(i)));
        }
        assertEquals(expected, curVector);

        nextVector = new AVLTreeIntervalVector();
        for (int i = 0; i < response.getNextIntervalsCount(); i++) {
            nextVector.insert(new ProtoInterval(response.getNextIntervals(i)));
        }
        assertEquals(expected, nextVector);
    }

}
