/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
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
import org.xtreemfs.foundation.buffer.BufferPool;
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
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_commit_vectorResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_get_vectorsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_get_vectorsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_diffRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_intervalRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_write_intervalResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

import com.backblaze.erasure.ReedSolomon;

public class ECOperationsTest extends ECTestCommon {
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
//        Category[] DEBUG_CATEGORIES = new Category[] { 
//                // Category.all,
//                Category.buffer,
//                // Category.lifecycle,
//                // Category.net,
//                Category.auth,
//                // Category.stage,
//                // Category.proc,
//                Category.misc,
//                Category.storage,
//                Category.replication,
//                Category.tool,
//                Category.test,
//                Category.flease,
//                // Category.babudb,
//                Category.ec
//        };
//        Logging.start(Logging.LEVEL_DEBUG, DEBUG_CATEGORIES);
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
                TestEnvironment.Services.OSD_CLIENT,
                TestEnvironment.Services.MOCKUP_OSD, TestEnvironment.Services.MOCKUP_OSD2 });
        testEnv.start();

        osdClient = testEnv.getOSDClient();
        osdConfig = testEnv.getOSDConfig();
        osdUUID = osdConfig.getUUID();

        fileId = "ABCDEF:1";

        fileCredentials = getCreds(fileId, 1, 0, 128);
        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
    }

    Capability getCap(String fileId) {
        // TODO (jdillmann): Check if this Cap is correct
        return new Capability(fileId,
                Helper.flagsToInt(SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR), 60,
                System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0,
                osdConfig.getCapabilitySecret());
    }

    FileCredentials getCreds(String fileId, int width, int parity, int stripeSize) {
        Replica r = Replica.newBuilder().setReplicationFlags(0).setStripingPolicy(getECStripingPolicy(1, 0, stripeSize))
                .addOsdUuids(osdUUID.toString()).build();
        XLocSet xloc = XLocSet.newBuilder().setReadOnlyFileSize(0)
                .setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_EC).addReplicas(r).setVersion(1).build();
        return FileCredentials.newBuilder().setXcap(getCap(fileId).getXCap()).setXlocs(xloc).build();
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



        xtreemfs_ec_get_vectorsRequest request = xtreemfs_ec_get_vectorsRequest.newBuilder()
                .setFileId(fileId)
                .setFileCredentials(fileCredentials)
                .build();

        RPCResponse<xtreemfs_ec_get_vectorsResponse> rpcResponse;
        xtreemfs_ec_get_vectorsResponse response;

        // Test non-existent vectors
        rpcResponse = osdClient.xtreemfs_ec_get_vectors(osdUUID.getAddress(), RPCAuthentication.authNone,
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

        rpcResponse = osdClient.xtreemfs_ec_get_vectors(osdUUID.getAddress(), RPCAuthentication.authNone,
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
        assertFalse(commitResponse.getNeedsReconstruction());
        assertVectorsEquals(fileCredentials, commitIntervals, Collections.<Interval> emptyList());


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
        assertFalse(commitResponse.getNeedsReconstruction());
        assertVectorsEquals(fileCredentials, commitIntervals, Collections.<Interval> emptyList());

        
        // Ignore (op) incomplete intervals
        // ********************************
        curIntervals.clear();
        nextIntervals.clear();
        commitIntervals.clear();
        interval = new ObjectInterval(0, 12, 2, 2);
        curIntervals.add(interval);
        interval = new ObjectInterval(0, 12, 3, 3, 0, 12);
        commitIntervals.add(interval);
        // Add only half of the new interval to next.
        interval = new ObjectInterval(0, 6, 3, 3, 0, 12);
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
        assertTrue(commitResponse.getNeedsReconstruction());
        // Note: if the vector can not be complete, the request is aborted. Thus there can be intervals in next.
        // assertGetVectorsEquals(fileCredentials, curIntervals, Collections.<Interval> emptyList());
      

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
        assertTrue(commitResponse.getNeedsReconstruction());
        // Note: if the vector can not be complete, the request is aborted. Thus there can be intervals in next.
        // assertGetVectorsEquals(fileCredentials, curIntervals, Collections.<Interval> emptyList());
    }

    xtreemfs_ec_commit_vectorRequest intervalList2CommitRequest(List<Interval> intervals,
            xtreemfs_ec_commit_vectorRequest.Builder builder) {
        builder.clearIntervals();
        for (Interval interval : intervals) {
            builder.addIntervals(ProtoInterval.toProto(interval));
        }
        return builder.build();
    }

    @Test
    public void testCommitVectorMultipleStripes() throws Exception {
        // FIXME (jdillmann): Implement
        // This test should ensure every chunk from the OSD committing the vector is committed

        HashStorageLayout layout;

        List<Interval> curIntervals = new LinkedList<Interval>();
        List<Interval> nextIntervals = new LinkedList<Interval>();
        List<Interval> commitIntervals = new LinkedList<Interval>();

        Interval interval;
        FileCredentials fileCredentials;
        StripingPolicyImpl sp;
        String fileId, fileIdNext;
        FileMetadata fi;
        byte[] byteOut;
        ObjectInformation objInf;
        
        xtreemfs_ec_commit_vectorRequest commitRequest;
        xtreemfs_ec_commit_vectorRequest.Builder commitRequestBuilder;

        RPCResponse<xtreemfs_ec_commit_vectorResponse> rpcCommitResponse;
        xtreemfs_ec_commit_vectorResponse commitResponse;

        // Test committing two chunks from two stripes
        // *****************************************
        fileId = "ABCDEF:1";
        fileIdNext = "ABCDEF:1.next";
        fileCredentials = getCreds(fileId, 2, 0, 1);
        sp = getStripingPolicyImplementation(fileCredentials);
        commitRequestBuilder = xtreemfs_ec_commit_vectorRequest.newBuilder().setFileId(fileId)
                .setFileCredentials(fileCredentials);

        layout = new HashStorageLayout(osdConfig, new MetadataCache());
        fi = layout.getFileMetadataNoCaching(sp, fileId);

        // Write two chunks 0,2 from different stripes
        // Both chunks belong to the same operation spanning both stripes
        // FIXME (jdillmann): How should the opid work? per stripe or per whole range
        ReusableBuffer c0 = SetupUtils.generateData(1024, (byte) 1);
        layout.writeObject(fileIdNext, fi, c0.createViewBuffer(), 0, 0, 1, false, false);
        interval = new ObjectInterval(0, 2048, 1, 1, 0, 4096);
        layout.setECIntervalVector(fileId, Arrays.asList(interval), true, false);

        ReusableBuffer c2 = SetupUtils.generateData(1024, (byte) 2);
        layout.writeObject(fileIdNext, fi, c2.createViewBuffer(), 2, 0, 1, false, true);
        interval = new ObjectInterval(2048, 4096, 1, 1, 0, 4096);
        layout.setECIntervalVector(fileId, Arrays.asList(interval), true, true);


        // commit the whole operation
        commitIntervals.add(new ObjectInterval(0, 4096, 1, 1, 0, 4096));
        commitRequest = intervalList2CommitRequest(commitIntervals, commitRequestBuilder);
        rpcCommitResponse = osdClient.xtreemfs_ec_commit_vector(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, commitRequest);
        try {
            commitResponse = rpcCommitResponse.get();
        } finally {
            rpcCommitResponse.freeBuffers();
        }
        assertFalse(commitResponse.getNeedsReconstruction());

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(commitIntervals, fi.getECCurVector().serialize());
        assertEquals(Collections.<Interval> emptyList(), fi.getECNextVector().serialize());

        byteOut = new byte[1024];
        objInf = layout.readObject(fileId, fi, 0, 0, 1024, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        objInf.getData().position(0);
        objInf.getData().get(byteOut);
        assertArrayEquals(c0.array(), byteOut);

        objInf = layout.readObject(fileId, fi, 2, 0, 1024, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        objInf.getData().position(0);
        objInf.getData().get(byteOut);
        assertArrayEquals(c2.array(), byteOut);


        // Test committing one chunk and from a operation spanning two stripes
        // *******************************************************************
        fileId = "ABCDEF:2";
        fileIdNext = "ABCDEF:2.next";
        fileCredentials = getCreds(fileId, 2, 0, 1);
        sp = getStripingPolicyImplementation(fileCredentials);
        commitRequestBuilder = xtreemfs_ec_commit_vectorRequest.newBuilder().setFileId(fileId)
                .setFileCredentials(fileCredentials);

        layout = new HashStorageLayout(osdConfig, new MetadataCache());
        fi = layout.getFileMetadataNoCaching(sp, fileId);

        // Write chunk 2 to second stripe with an operation starting at the first stripe
        layout.writeObject(fileIdNext, fi, c2.createViewBuffer(), 2, 0, 1, false, true);
        interval = new ObjectInterval(1024, 2048, 1, 1, 1024, 3072);
        layout.setECIntervalVector(fileId, Arrays.asList(interval), true, true);
        interval = new ObjectInterval(2048, 3072, 1, 1, 1024, 3072);
        layout.setECIntervalVector(fileId, Arrays.asList(interval), true, true);

        // commit the operation
        commitIntervals.clear();
        commitIntervals.add(new ObjectInterval(1024, 3072, 1, 1, 1024, 3072));
        commitRequest = intervalList2CommitRequest(commitIntervals, commitRequestBuilder);
        rpcCommitResponse = osdClient.xtreemfs_ec_commit_vector(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, commitRequest);
        try {
            commitResponse = rpcCommitResponse.get();
        } finally {
            rpcCommitResponse.freeBuffers();
        }
        assertFalse(commitResponse.getNeedsReconstruction());

        curIntervals.clear();
        curIntervals.add(ObjectInterval.empty(0, 1024));
        curIntervals.add(new ObjectInterval(1024, 3072, 1, 1, 1024, 3072));

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(curIntervals, fi.getECCurVector().serialize());
        assertEquals(Collections.<Interval> emptyList(), fi.getECNextVector().serialize());

        byteOut = new byte[1024];
        objInf = layout.readObject(fileId, fi, 0, 0, 1024, 1);
        assertEquals(ObjectStatus.DOES_NOT_EXIST, objInf.getStatus());
        // objInf.getData().position(0);
        // objInf.getData().get(byteOut);
        // assertArrayEquals(c0.array(), byteOut);

        objInf = layout.readObject(fileId, fi, 2, 0, 1024, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        objInf.getData().position(0);
        objInf.getData().get(byteOut);
        assertArrayEquals(c2.array(), byteOut);
    }

    @Test
    public void testWriteInterval() throws Exception {
        List<Interval> commitIntervals = new LinkedList<Interval>();
        Interval interval;
        long objNo;
        long opId;
        int offset;
        FileMetadata fi;
        ObjectInformation objInf;
        
        String fileIdNext = fileId + ECStorage.FILEID_NEXT_SUFFIX;
        HashStorageLayout layout;
        StripingPolicyImpl sp = getStripingPolicyImplementation(fileCredentials);
        int chunkSize = sp.getPolicy().getStripeSize() * 1024;
        byte[] byteOut = new byte[chunkSize];

        xtreemfs_ec_write_intervalRequest request;
        
        xtreemfs_ec_write_intervalRequest.Builder requestB = xtreemfs_ec_write_intervalRequest.newBuilder()
                .setFileId(fileId)
                .setFileCredentials(fileCredentials);

        RPCResponse<xtreemfs_ec_write_intervalResponse> rpcResponse;
        xtreemfs_ec_write_intervalResponse response;


        // Test writing to non-existent file
        // ***********************************
        final ReusableBuffer dataInF1 = SetupUtils.generateData(chunkSize, (byte) 1);
        objNo = 0;
        opId = 1;
        offset = 0;
        interval = new ObjectInterval(offset, chunkSize, 1, opId);
        requestB.setObjectNumber(objNo)
                .setOpId(opId)
                .setOffset(offset)
                .setStripeInterval(ProtoInterval.toProto(interval));
        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_interval(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, dataInF1);
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }
        assertFalse(response.getNeedsReconstruction());
        
        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(Collections.EMPTY_LIST, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(interval), fi.getECNextVector().serialize());

        objInf = layout.readObject(fileIdNext, fi, objNo, offset, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        objInf.getData().position(0);
        objInf.getData().get(byteOut);
        assertArrayEquals(dataInF1.array(), byteOut);


        // Test writing second half of the chunk: should commit the whole chunk
        // ********************************************************************
        ReusableBuffer dataInH2 = SetupUtils.generateData(chunkSize / 2, (byte) 2);
        objNo = 0;
        opId = 2;
        offset = chunkSize / 2;

        commitIntervals.clear();
        interval = new ObjectInterval(0, chunkSize, 1, 1);
        commitIntervals.add(interval);
        intervalList2WriteIntervalRequest(commitIntervals, requestB);

        interval = new ObjectInterval(offset, chunkSize, 2, opId);
        requestB.setObjectNumber(objNo).setOpId(opId).setOffset(offset)
                .setStripeInterval(ProtoInterval.toProto(interval));

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_interval(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, dataInH2);
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }
        assertFalse(response.getNeedsReconstruction());

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(commitIntervals, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(interval), fi.getECNextVector().getSlice(offset, chunkSize));

        objInf = layout.readObject(fileId, fi, objNo, 0, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        objInf.getData().position(0);
        objInf.getData().get(byteOut);
        assertArrayEquals(dataInF1.array(), byteOut);

        objInf = layout.readObject(fileIdNext, fi, objNo, offset, chunkSize / 2, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        byteOut = new byte[chunkSize / 2];
        objInf.getData().position(0);
        objInf.getData().get(byteOut);
        assertArrayEquals(dataInH2.array(), byteOut);

        
        // Test overwriting and aborting by using the same version but greater opid
        // *************************************************************************
        ReusableBuffer dataInF3 = SetupUtils.generateData(chunkSize, (byte) 3);
        objNo = 0;
        opId = 3;
        offset = 0;

        commitIntervals.clear();
        interval = new ObjectInterval(0, chunkSize, 1, 1);
        commitIntervals.add(interval);
        intervalList2WriteIntervalRequest(commitIntervals, requestB);

        interval = new ObjectInterval(offset, chunkSize, 2, opId);
        requestB.setObjectNumber(objNo).setOpId(opId).setOffset(offset)
                .setStripeInterval(ProtoInterval.toProto(interval));

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_interval(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, dataInF3);
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }
        assertFalse(response.getNeedsReconstruction());

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(commitIntervals, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(interval), fi.getECNextVector().getSlice(offset, chunkSize));

        byteOut = new byte[chunkSize];
        objInf = layout.readObject(fileId, fi, objNo, 0, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        objInf.getData().position(0);
        objInf.getData().get(byteOut);
        assertArrayEquals(dataInF1.array(), byteOut);

        objInf = layout.readObject(fileIdNext, fi, objNo, offset, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        objInf.getData().position(0);
        objInf.getData().get(byteOut);
        assertArrayEquals(dataInF3.array(), byteOut);
    }

    void intervalList2WriteIntervalRequest(List<Interval> intervals,
            xtreemfs_ec_write_intervalRequest.Builder builder) {
        builder.clearCommitIntervals();
        for (Interval interval : intervals) {
            builder.addCommitIntervals(ProtoInterval.toProto(interval));
        }
    }


    @Test
    public void testWriteDiff() throws Exception {
        List<Interval> commitIntervals = new LinkedList<Interval>();
        Interval interval;
        Interval diffInterval;
        Interval stripeInterval;
        long objNo;
        long opId;
        int offset;
        long stripeNo;
        FileMetadata fi;
        ObjectInformation objInf;

        String fileIdDelta = fileId + ECStorage.FILEID_DELTA_SUFFIX;
        String fileIdCode = fileId + ECStorage.FILEID_CODE_SUFFIX;
        HashStorageLayout layout;

        Replica r = Replica.newBuilder().setReplicationFlags(0).setStripingPolicy(getECStripingPolicy(2, 1, 1))
                .addOsdUuids("mockUpOSD").addOsdUuids("mockUpOSD2").addOsdUuids(osdUUID.toString()).build();
        XLocSet xloc = XLocSet.newBuilder().setReadOnlyFileSize(0)
                .setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_EC).addReplicas(r).setVersion(1).build();
        FileCredentials fileCredentials = FileCredentials.newBuilder().setXcap(getCap(fileId).getXCap()).setXlocs(xloc)
                .build();

        StripingPolicyImpl sp = getStripingPolicyImplementation(fileCredentials);

        ReedSolomon codec = ReedSolomon.create(2, 1);

        int chunkSize = sp.getPolicy().getStripeSize() * 1024;
        byte[] byteOut;


        byte[][] data = new byte[2][chunkSize];
        ReusableBuffer[] dataBuf = { ReusableBuffer.wrap(data[0]), ReusableBuffer.wrap(data[1]) };

        byte[] code = new byte[chunkSize];
        ReusableBuffer codeBuf = ReusableBuffer.wrap(code);

        byte[] diff = new byte[chunkSize];
        ReusableBuffer diffBuffer = ReusableBuffer.wrap(diff);
        ReusableBuffer diffView;
        ReusableBuffer delta;


        byte[][] dataCode = { data[0], data[1], code };


        xtreemfs_ec_write_diffRequest request;

        xtreemfs_ec_write_diffRequest.Builder requestB = xtreemfs_ec_write_diffRequest.newBuilder().setFileId(fileId)
                .setFileCredentials(fileCredentials);

        RPCResponse<emptyResponse> rpcResponse;
        emptyResponse response;

        // Test writing to non-existent file
        // ***********************************
        final ReusableBuffer dataInF1 = SetupUtils.generateData(chunkSize, (byte) 1);
        objNo = 0;
        stripeNo = 0;
        opId = 1;
        offset = 0;
        interval = new ObjectInterval(offset, chunkSize, 1, opId);
        stripeInterval = interval;
        diffInterval = interval;
        requestB.setObjectNumber(objNo).setOpId(opId).setOffset(offset)
                .setStripeInterval(ProtoInterval.toProto(stripeInterval))
                .setDiffInterval(ProtoInterval.toProto(diffInterval));
        
        diffBuffer = diff(dataInF1.createViewBuffer(), dataBuf[0].createViewBuffer(), 0);
        
        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_diff(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, diffBuffer.createViewBuffer());
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        // Wait until the storage stage completed.
        Thread.sleep(5 * 1000);

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(Collections.EMPTY_LIST, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(stripeInterval), fi.getECNextVector().serialize());

        // Assert new delta is correct
        objInf = layout.readObject(fileIdDelta, fi, objNo, offset, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        delta = encode(codec, diffBuffer, 0);
        assertBufferEquals(delta, objInf.getData());
        BufferPool.free(objInf.getData());

        // Update Data and Code
        dataBuf[0].position(0);
        dataBuf[0].put(dataInF1.createViewBuffer());
        dataBuf[0].position(0);
        codec.encodeParity(dataCode, 0, chunkSize);



        // Test writing second half of the chunk: should commit the whole chunk
        // ********************************************************************
        ReusableBuffer dataInH2 = SetupUtils.generateData(chunkSize / 2, (byte) 2);
        objNo = 0;
        stripeNo = 0;
        opId = 2;
        offset = chunkSize / 2;
        commitIntervals.clear();
        interval = new ObjectInterval(0, chunkSize, 1, 1);
        commitIntervals.add(interval);
        intervalList2WriteDiffRequest(commitIntervals, requestB);

        interval = new ObjectInterval(offset, chunkSize, 2, opId);
        stripeInterval = interval;
        diffInterval = interval;
        requestB.setObjectNumber(objNo).setOpId(opId).setOffset(offset)
                .setStripeInterval(ProtoInterval.toProto(stripeInterval))
                .setDiffInterval(ProtoInterval.toProto(diffInterval));


        diffBuffer = diff(dataInH2.createViewBuffer(), dataBuf[0].createViewBuffer(), offset);

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_diff(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, diffBuffer.createViewBuffer());
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        // Wait until the storage stage completed.
        Thread.sleep(5 * 1000);

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(commitIntervals, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(interval), fi.getECNextVector().getSlice(offset, chunkSize));

        // Assert new delta is correct
        objInf = layout.readObject(fileIdDelta, fi, objNo, offset, chunkSize / 2, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        delta = encode(codec, diffBuffer, 0);
        assertBufferEquals(delta, objInf.getData());
        BufferPool.free(objInf.getData());

        // Test committed codes
        objInf = layout.readObject(fileIdCode, fi, stripeNo, 0, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        assertBufferEquals(codeBuf, objInf.getData());
        BufferPool.free(objInf.getData());

        // Update Data and Code
        dataBuf[0].position(chunkSize / 2);
        dataBuf[0].put(dataInH2.createViewBuffer());
        dataBuf[0].position(0);
        codec.encodeParity(dataCode, 0, chunkSize);



        // Test overwriting the whole chunk and test the code
        // ********************************************************************
        ReusableBuffer dataInF3 = SetupUtils.generateData(chunkSize, (byte) 3);
        objNo = 0;
        stripeNo = 0;
        opId = 3;
        offset = 0;
        commitIntervals.clear();
        commitIntervals.add(new ObjectInterval(0, chunkSize / 2, 1, 1));
        commitIntervals.add(new ObjectInterval(chunkSize / 2, chunkSize, 2, 2));
        intervalList2WriteDiffRequest(commitIntervals, requestB);

        interval = new ObjectInterval(offset, chunkSize, 3, opId);
        stripeInterval = interval;
        diffInterval = interval;
        requestB.setObjectNumber(objNo).setOpId(opId).setOffset(offset)
                .setStripeInterval(ProtoInterval.toProto(stripeInterval))
                .setDiffInterval(ProtoInterval.toProto(diffInterval));


        diffBuffer = diff(dataInF3.createViewBuffer(), dataBuf[0].createViewBuffer(), offset);

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_diff(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, diffBuffer.createViewBuffer());
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        // Wait until the storage stage completed.
        Thread.sleep(5 * 1000);

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(commitIntervals, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(interval), fi.getECNextVector().getSlice(offset, chunkSize));

        // Assert new delta is correct
        objInf = layout.readObject(fileIdDelta, fi, objNo, offset, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        delta = encode(codec, diffBuffer, 0);
        assertBufferEquals(delta, objInf.getData());
        BufferPool.free(objInf.getData());


        // Test committed codes
        objInf = layout.readObject(fileIdCode, fi, stripeNo, 0, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        assertBufferEquals(codeBuf, objInf.getData());
        BufferPool.free(objInf.getData());

        // Update Data and Code
        dataBuf[0].position(0);
        dataBuf[0].put(dataInF3.createViewBuffer());
        dataBuf[0].position(0);
        codec.encodeParity(dataCode, 0, chunkSize);




        // Test overwriting the whole stripe with two separate diffs (should commit)
        // *************************************************************************
        ReusableBuffer dataInF4 = SetupUtils.generateData(chunkSize, (byte) 4);
        objNo = -1;
        stripeNo = 0;
        opId = 4;
        offset = 0;
        commitIntervals.clear();
        commitIntervals.add(new ObjectInterval(0, chunkSize, 3, 3));
        intervalList2WriteDiffRequest(commitIntervals, requestB);

        stripeInterval = new ObjectInterval(offset, 2 * chunkSize, 4, opId);
        diffInterval = new ObjectInterval(0, chunkSize, 4, opId, offset, 2 * chunkSize);
        requestB.setObjectNumber(0).setOpId(opId).setOffset(offset)
                .setStripeInterval(ProtoInterval.toProto(stripeInterval))
                .setDiffInterval(ProtoInterval.toProto(diffInterval));

        diffBuffer = diff(dataInF4.createViewBuffer(), dataBuf[0].createViewBuffer(), 0);

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_diff(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, diffBuffer);
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        diffInterval = new ObjectInterval(chunkSize, 2 * chunkSize, 4, opId, offset, 2 * chunkSize);
        requestB.setObjectNumber(1).setOpId(opId).setOffset(offset)
                .setStripeInterval(ProtoInterval.toProto(stripeInterval))
                .setDiffInterval(ProtoInterval.toProto(diffInterval));

        diffBuffer = diff(dataInF4.createViewBuffer(), dataBuf[1].createViewBuffer(), 0);

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_diff(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, diffBuffer);
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        // Wait until the storage stage completed.
        Thread.sleep(5 * 1000);

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(commitIntervals, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(stripeInterval), fi.getECNextVector().getSlice(offset, 2 * chunkSize));

        // Assert new deltas are correct
        objInf = layout.readObject(fileIdDelta, fi, 0, offset, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        diffBuffer = diff(dataInF4.createViewBuffer(), dataBuf[0].createViewBuffer(), 0);
        delta = encode(codec, diffBuffer, 0);
        assertBufferEquals(delta, objInf.getData());
        BufferPool.free(objInf.getData());

        objInf = layout.readObject(fileIdDelta, fi, 1, offset, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        diffBuffer = diff(dataInF4.createViewBuffer(), dataBuf[1].createViewBuffer(), 0);
        delta = encode(codec, diffBuffer, 1);
        assertBufferEquals(delta, objInf.getData());
        BufferPool.free(objInf.getData());


        // Test committed codes
        objInf = layout.readObject(fileIdCode, fi, stripeNo, 0, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        assertBufferEquals(codeBuf, objInf.getData());
        BufferPool.free(objInf.getData());

        // Update Data and Code
        dataBuf[0].position(0);
        dataBuf[0].put(dataInF4.createViewBuffer());
        dataBuf[0].position(0);
        dataBuf[1].position(0);
        dataBuf[1].put(dataInF4.createViewBuffer());
        dataBuf[1].position(0);
        codec.encodeParity(dataCode, 0, chunkSize);



        // Test overwriting a partial stripe with two separate diffs (should commit)
        // *************************************************************************
        ReusableBuffer dataInH5 = SetupUtils.generateData(512, (byte) 5);
        objNo = -1;
        stripeNo = 0;
        opId = 5;
        offset = -1;
        commitIntervals.clear();
        commitIntervals.add(new ObjectInterval(0, 2 * chunkSize, 4, 4));
        intervalList2WriteDiffRequest(commitIntervals, requestB);

        stripeInterval = new ObjectInterval(512, 1536, 5, opId);
        diffInterval = new ObjectInterval(512, 1024, 5, opId, 512, 1536);
        requestB.setObjectNumber(0).setOpId(opId).setOffset(512)
                .setStripeInterval(ProtoInterval.toProto(stripeInterval))
                .setDiffInterval(ProtoInterval.toProto(diffInterval));


        diffBuffer = diff(dataInH5.createViewBuffer(), dataBuf[0].createViewBuffer(), 512);

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_diff(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, diffBuffer.createViewBuffer());
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        diffInterval = new ObjectInterval(1024, 1536, 5, opId, 512, 1536);
        requestB.setObjectNumber(1).setOpId(opId).setOffset(0).setStripeInterval(ProtoInterval.toProto(stripeInterval))
                .setDiffInterval(ProtoInterval.toProto(diffInterval));

        diffBuffer = diff(dataInH5.createViewBuffer(), dataBuf[1].createViewBuffer(), 0);

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_diff(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, diffBuffer.createViewBuffer());
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        // Wait until the storage stage completed.
        Thread.sleep(5 * 1000);

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(commitIntervals, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(stripeInterval), fi.getECNextVector().getSlice(512, 1536));


        // Assert new deltas are correct
        objInf = layout.readObject(fileIdDelta, fi, 0, 512, 512, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        diffBuffer = diff(dataInH5.createViewBuffer(), dataBuf[0].createViewBuffer(), 512);
        delta = encode(codec, diffBuffer, 0);
        assertBufferEquals(delta, objInf.getData());
        BufferPool.free(objInf.getData());

        objInf = layout.readObject(fileIdDelta, fi, 1, 0, 512, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        diffBuffer = diff(dataInH5.createViewBuffer(), dataBuf[1].createViewBuffer(), 0);
        delta = encode(codec, diffBuffer, 1);
        assertBufferEquals(delta, objInf.getData());
        BufferPool.free(objInf.getData());


        // Test committed codes
        objInf = layout.readObject(fileIdCode, fi, stripeNo, 0, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        assertBufferEquals(codeBuf, objInf.getData());
        BufferPool.free(objInf.getData());

        // Update Data and Code
        dataBuf[0].position(512);
        dataBuf[0].put(dataInH5.createViewBuffer());
        dataBuf[0].position(0);
        dataBuf[1].position(0);
        dataBuf[1].put(dataInH5.createViewBuffer());
        dataBuf[1].position(0);
        codec.encodeParity(dataCode, 0, chunkSize);




        // Test overwriting within the first chunk and test the code
        // ********************************************************************
        ReusableBuffer dataInQ6 = SetupUtils.generateData(512, (byte) 6);
        objNo = 0;
        stripeNo = 0;
        opId = 6;
        offset = -1;

        commitIntervals.clear();
        commitIntervals.add(new ObjectInterval(0, 512, 4, 4));
        commitIntervals.add(new ObjectInterval(512, 1536, 5, 5));
        commitIntervals.add(new ObjectInterval(1536, 2048, 4, 4));
        intervalList2WriteDiffRequest(commitIntervals, requestB);

        interval = new ObjectInterval(256, 768, 6, opId);
        stripeInterval = interval;
        diffInterval = interval;
        requestB.setObjectNumber(objNo).setOpId(opId).setOffset(256)
                .setStripeInterval(ProtoInterval.toProto(stripeInterval))
                .setDiffInterval(ProtoInterval.toProto(diffInterval));

        diffBuffer = diff(dataInQ6.createViewBuffer(), dataBuf[0].createViewBuffer(), 256);

        request = requestB.build();
        rpcResponse = osdClient.xtreemfs_ec_write_diff(osdUUID.getAddress(), RPCAuthentication.authNone,
                userCredentials, request, diffBuffer.createViewBuffer());
        try {
            response = rpcResponse.get();
        } finally {
            rpcResponse.freeBuffers();
        }

        // Wait until the storage stage completed.
        Thread.sleep(5 * 1000);

        layout = new HashStorageLayout(osdConfig, new MetadataCache()); // clears cache
        fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(commitIntervals, fi.getECCurVector().serialize());
        assertEquals(Arrays.asList(interval), fi.getECNextVector().getSlice(256, 768));

        // Assert new delta is correct
        objInf = layout.readObject(fileIdDelta, fi, 0, 256, 512, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        diffBuffer = diff(dataInQ6.createViewBuffer(), dataBuf[0].createViewBuffer(), 256);
        delta = encode(codec, diffBuffer, 0);
        assertBufferEquals(delta, objInf.getData());
        BufferPool.free(objInf.getData());


        // Test committed codes
        objInf = layout.readObject(fileIdCode, fi, stripeNo, 0, chunkSize, 1);
        assertEquals(ObjectStatus.EXISTS, objInf.getStatus());
        assertBufferEquals(codeBuf, objInf.getData());
        BufferPool.free(objInf.getData());

        // Update Data and Code
        dataBuf[0].position(256);
        dataBuf[0].put(dataInQ6.createViewBuffer());
        dataBuf[0].position(0);
        codec.encodeParity(dataCode, 0, chunkSize);

    }

    ReusableBuffer encode(ReedSolomon codec, ReusableBuffer diff, int shardOffset) {
        ReusableBuffer delta = ReusableBuffer.wrap(new byte[diff.capacity()]);
        codec.encodeDiffParity(diff.getBuffer().slice(), shardOffset, delta.getBuffer().slice(), 0, 0, diff.capacity());
        delta.position(0);
        return delta;
    }



    ReusableBuffer diff(ReusableBuffer data, ReusableBuffer curData, int offset) {
        curData.position(offset);
        data.position(0);
        ReusableBuffer diff = ReusableBuffer.wrap(new byte[data.capacity()]); // BufferPool.allocate(data.capacity());
        ECHelper.xor(diff, data, curData);
        diff.position(0);
        return diff;
    }

    
    void intervalList2WriteDiffRequest(List<Interval> intervals,
            xtreemfs_ec_write_diffRequest.Builder builder) {
        builder.clearCommitIntervals();
        for (Interval interval : intervals) {
            builder.addCommitIntervals(ProtoInterval.toProto(interval));
        }
    }

    void assertVectorsEquals(FileCredentials fileCredentials, List<Interval> curExpected, List<Interval> nextExpected)
            throws Exception {

        String fileId = fileCredentials.getXcap().getFileId();
        HashStorageLayout layout = new HashStorageLayout(osdConfig, new MetadataCache()); // no cache
        StripingPolicyImpl sp = getStripingPolicyImplementation(fileCredentials);

        FileMetadata fi = layout.getFileMetadataNoCaching(sp, fileId);
        assertEquals(curExpected, fi.getECCurVector().serialize());
        assertEquals(nextExpected, fi.getECNextVector().serialize());
    }
}
