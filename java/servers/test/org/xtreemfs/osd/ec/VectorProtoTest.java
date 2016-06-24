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
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorResponse;
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

    @Test
    public void testCommitVector() throws Exception {
        HashStorageLayout layout = new HashStorageLayout(osdConfig, new MetadataCache());

        List<Interval> curIntervals = new LinkedList<Interval>();
        List<Interval> nextIntervals = new LinkedList<Interval>();
        List<Interval> commitIntervals = new LinkedList<Interval>();

        Interval interval;

        FileCredentials fileCredentials;
        String fileId;
        xtreemfs_ec_commit_vectorRequest commitRequest;
        xtreemfs_ec_commit_vectorRequest.Builder commitRequestBuilder;

        RPCResponse<xtreemfs_ec_commit_vectorResponse> rpcCommitResponse;
        xtreemfs_ec_commit_vectorResponse commitResponse;


        // Write Interval 0:12 to next and commit it
        // *****************************************
        fileId = "ABCDEF:1";
        fileCredentials = getCreds(fileId, 1, 0, 128);
        commitRequestBuilder = xtreemfs_ec_commit_vectorRequest.newBuilder().setFileId(fileId)
                .setFileCredentials(fileCredentials);

        interval = new ObjectInterval(0, 12, 2, 2);
        curIntervals.clear();
        nextIntervals.clear();
        commitIntervals.clear();
        
        nextIntervals.add(interval);
        commitIntervals.add(interval);
        
        // write to next
        layout.setECIntervalVector(fileId, nextIntervals, true, false);

        // commit
        commitRequest = intervalList2CommitRequest(commitIntervals, commitRequestBuilder);
        rpcCommitResponse = osdClient.xtreemfs_ec_commit_vector(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, commitRequest);
        try {
            commitResponse = rpcCommitResponse.get();
        } finally {
            rpcCommitResponse.freeBuffers();
        }
        assertTrue(commitResponse.getComplete());
        assertGetVectorsEquals(fileCredentials, commitIntervals, Collections.<Interval> emptyList());


        // Try to commit Interval 0:12 from empty next
        // *******************************************

        // commit
        commitRequest = intervalList2CommitRequest(commitIntervals, commitRequestBuilder);
        rpcCommitResponse = osdClient.xtreemfs_ec_commit_vector(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, commitRequest);
        try {
            commitResponse = rpcCommitResponse.get();
        } finally {
            rpcCommitResponse.freeBuffers();
        }
        assertTrue(commitResponse.getComplete());
        assertGetVectorsEquals(fileCredentials, commitIntervals, Collections.<Interval> emptyList());

        
        // Ignore (op) incomplete intervals
        // ********************************
        curIntervals.clear();
        nextIntervals.clear();
        commitIntervals.clear();
        interval = new ObjectInterval(0, 12, 2, 2);
        curIntervals.add(interval);
        interval = new OpObjectInterval(0, 12, 3, 3, 0, 12);
        commitIntervals.add(interval);
        // Add only half of the new interval to next.
        interval = new OpObjectInterval(0, 6, 3, 3, 0, 12);
        nextIntervals.add(interval);

        // write to next
        layout.setECIntervalVector(fileId, nextIntervals, true, false);

        // commit
        commitRequest = intervalList2CommitRequest(commitIntervals, commitRequestBuilder);
        rpcCommitResponse = osdClient.xtreemfs_ec_commit_vector(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, commitRequest);
        try {
            commitResponse = rpcCommitResponse.get();
        } finally {
            rpcCommitResponse.freeBuffers();
        }
        assertFalse(commitResponse.getComplete());
        assertGetVectorsEquals(fileCredentials, curIntervals, Collections.<Interval> emptyList());
      

        // Drop intervals with a older version or id
        // *****************************************
        fileId = "ABCDEF:2";
        fileCredentials = getCreds(fileId, 1, 0, 128);
        commitRequestBuilder = xtreemfs_ec_commit_vectorRequest.newBuilder().setFileId(fileId)
                .setFileCredentials(fileCredentials);

        curIntervals.clear();
        nextIntervals.clear();
        commitIntervals.clear();
        interval = new ObjectInterval(0, 12, 1, 1);
        curIntervals.add(interval);

        interval = new ObjectInterval(0, 6, 2, 3);
        commitIntervals.add(interval);
        interval = new ObjectInterval(6, 9, 1, 1);
        commitIntervals.add(interval);
        interval = new ObjectInterval(9, 12, 3, 5);
        commitIntervals.add(interval);

        // Same version, older id => should be ignored/dropped
        interval = new ObjectInterval(0, 9, 2, 2);
        nextIntervals.add(interval);

        // Older version => should be ignored/dropped
        interval = new ObjectInterval(9, 12, 2, 4);
        nextIntervals.add(interval);

        // write to cur
        layout.setECIntervalVector(fileId, curIntervals, false, false);

        // write to next
        layout.setECIntervalVector(fileId, nextIntervals, true, false);

        // commit
        commitRequest = intervalList2CommitRequest(commitIntervals, commitRequestBuilder);
        rpcCommitResponse = osdClient.xtreemfs_ec_commit_vector(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, commitRequest);
        try {
            commitResponse = rpcCommitResponse.get();
        } finally {
            rpcCommitResponse.freeBuffers();
        }
        assertFalse(commitResponse.getComplete());
        assertGetVectorsEquals(fileCredentials, curIntervals, Collections.<Interval> emptyList());
    }

    @Test
    public void testCommitVectorMultipleStripes() throws Exception {
        // FIXME (jdillmann): Implement
        // This test should ensure every chunk from the OSD committing the vector is committed
        fail();

        HashStorageLayout layout = new HashStorageLayout(osdConfig, new MetadataCache());

        List<Interval> curIntervals = new LinkedList<Interval>();
        List<Interval> nextIntervals = new LinkedList<Interval>();
        List<Interval> commitIntervals = new LinkedList<Interval>();

        Interval interval;

        FileCredentials fileCredentials;
        String fileId;
        xtreemfs_ec_commit_vectorRequest commitRequest;
        xtreemfs_ec_commit_vectorRequest.Builder commitRequestBuilder;

        RPCResponse<xtreemfs_ec_commit_vectorResponse> rpcCommitResponse;
        xtreemfs_ec_commit_vectorResponse commitResponse;

        // Use fake policy with width 2
        // *****************************************
        fileId = "ABCDEF:1";
        fileCredentials = getCreds(fileId, 2, 0, 1);
        commitRequestBuilder = xtreemfs_ec_commit_vectorRequest.newBuilder().setFileId(fileId)
                .setFileCredentials(fileCredentials);

        StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(fileCredentials.getXlocs().getReplicas(0), 0);
        FileMetadata md = layout.getFileMetadataNoCaching(sp, fileId);

        interval = new ObjectInterval(0, 32, 1, 1);
        curIntervals.clear();
        nextIntervals.clear();
        commitIntervals.clear();
        curIntervals.add(interval);

        ReusableBuffer buf1 = SetupUtils.generateData(8, (byte) 1);
        // ReusableBuffer buf2 = SetupUtils.generateData(8, (byte) 1);
        layout.writeObject(fileId, md, buf1, 0, 0, 1, false, false);
        layout.writeObject(fileId, md, buf1, 0, 2, 1, false, false);

        layout.setECIntervalVector(fileId, curIntervals, false, false);

    }

    xtreemfs_ec_commit_vectorRequest intervalList2CommitRequest(List<Interval> intervals,
            xtreemfs_ec_commit_vectorRequest.Builder builder) {
        builder.clearIntervals();
        for (Interval interval : intervals) {
            builder.addIntervals(ProtoInterval.toProto(interval));
        }
        return builder.build();
    }

    void assertGetVectorsEquals(FileCredentials fileCredentials, List<Interval> curExpected,
            List<Interval> nextExpected) throws Exception {

        RPCResponse<xtreemfs_ec_get_interval_vectorsResponse> rpcGetResponse;
        xtreemfs_ec_get_interval_vectorsRequest getRequest = xtreemfs_ec_get_interval_vectorsRequest.newBuilder()
                .setFileId(fileCredentials.getXcap().getFileId()).setFileCredentials(fileCredentials).build();
        xtreemfs_ec_get_interval_vectorsResponse getResponse;

        // get_interval is used to check if the changes are reflected in the cached metadata info
        rpcGetResponse = osdClient.xtreemfs_ec_get_interval_vectors(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, getRequest);
        try {
            getResponse = rpcGetResponse.get();
        } finally {
            rpcGetResponse.freeBuffers();
        }

        List<Interval> curIntervals = new LinkedList<Interval>();
        for (int i = 0; i < getResponse.getCurIntervalsCount(); i++) {
            curIntervals.add(new ProtoInterval(getResponse.getCurIntervals(i)));
        }
        assertEquals(curExpected, curIntervals);

        List<Interval> nextIntervals = new LinkedList<Interval>();
        for (int i = 0; i < getResponse.getNextIntervalsCount(); i++) {
            nextIntervals.add(new ProtoInterval(getResponse.getNextIntervals(i)));
        }
        assertEquals(nextExpected, nextExpected);
    }


    class OpObjectInterval extends ObjectInterval {
        final long opStart;
        final long opEnd;

        public OpObjectInterval(long start, long end, long version, long id, long opStart, long opEnd) {
            super(start, end, version, id);
            this.opStart = opStart;
            this.opEnd = opEnd;
        }

        @Override
        public long getOpStart() {
            return opStart;
        }

        @Override
        public long getOpEnd() {
            return opEnd;
        }
    }
    
}
