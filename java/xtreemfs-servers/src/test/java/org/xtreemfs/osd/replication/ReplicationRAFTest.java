///*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
//    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
//
//    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
//    Grid Operating System, see <http://www.xtreemos.eu> for more details.
//    The XtreemOS project has been developed with the financial support of the
//    European Commission's IST program under contract #FP6-033576.
//
//    XtreemFS is free software: you can redistribute it and/or modify it under
//    the terms of the GNU General Public License as published by the Free
//    Software Foundation, either version 2 of the License, or (at your option)
//    any later version.
//
//    XtreemFS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
// */
///*
// * AUTHORS: Christian Lorenz (ZIB)
// */
//package org.xtreemfs.osd.replication;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Random;
//import java.util.SortedSet;
//import java.util.TreeSet;
//
//import junit.framework.TestCase;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.xtreemfs.common.clients.RandomAccessFile;
//import org.xtreemfs.common.uuids.ServiceUUID;
//import org.xtreemfs.common.xloc.Replica;
//import org.xtreemfs.common.xloc.ReplicationFlags;
//import org.xtreemfs.foundation.buffer.BufferPool;
//import org.xtreemfs.foundation.buffer.ReusableBuffer;
//import org.xtreemfs.foundation.logging.Logging;
//import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
//import org.xtreemfs.foundation.util.FSUtils;
//import org.xtreemfs.osd.replication.ObjectSet;
//import org.xtreemfs.SetupUtils;
//import org.xtreemfs.TestEnvironment;
//
///**
// *
// * 12.05.2009
// */
//public class ReplicationRAFTest extends TestCase {
//    public static final int    STRIPE_SIZE      = 1024 * 10;      // 10kb in byte
//
//    public static final String VOLUME_NAME      = "test";
//
//    // FIXME:
//    public static int          HOLE_PROBABILITY = -1;             // 30% chance
//
//    private ReusableBuffer     data;
//
//    /**
//     * contains the position where the hole starts (the size of a hole is always
//     * the stripe size in this test)
//     */
//    private SortedSet<Integer> holes;
//
//    private TestEnvironment    testEnv;
//
//    private UserCredentials    userCredentials;
//
//    private static Random      random           = new Random(843);
//
//    public ReplicationRAFTest() {
//        // Auto-generated constructor stub
//        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
////        Monitoring.enable();
//    }
//
//    /**
//     * @throws java.lang.Exception
//     */
//    @Before
//    public void setUp() throws Exception {
//        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
//
//        // cleanup
//        File testDir = new File(SetupUtils.TEST_DIR);
//
//        FSUtils.delTree(testDir);
//        testDir.mkdirs();
//
//        // user credentials
//        userCredentials = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();
//
//        // startup: DIR, MRC, 8 OSDs, ...
//        testEnv = new TestEnvironment(TestEnvironment.Services.DIR_SERVICE, TestEnvironment.Services.MRC,
//            TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
//            TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT,
//            TestEnvironment.Services.OSD, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD,
//            TestEnvironment.Services.OSD, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD,
//            TestEnvironment.Services.OSD, TestEnvironment.Services.OSD);
//        testEnv.start();
//
//        // wait a bit so the MRC could notice the OSDs
//        Thread.sleep(1000);
//
////        data = ReusableBuffer.wrap("fadsljfalskjdflaskjdfölkjsalödfkjaslsfijal".getBytes());
////        holes = new TreeSet<Integer>();
//        generateData(1000 * 1000 * 3); // ca. 3 MB
//        initializeVolume(2);
//    }
//
//    private void generateData(int appriximateSize) {
//        data = BufferPool.allocate(appriximateSize);
//        holes = new TreeSet<Integer>();
//        byte[] zeros = new byte[(int) (STRIPE_SIZE * 1.5)];
//        Arrays.fill(zeros, (byte) 0);
//
//        while (data.position() < appriximateSize - STRIPE_SIZE * 1.5) {
//            if (random.nextInt(100) > HOLE_PROBABILITY) {
//                // write A piece of data
//                ReusableBuffer tmpData = SetupUtils.generateData((int) (STRIPE_SIZE * 1.5));
//                data.put(tmpData);
//                BufferPool.free(tmpData);
//            } else { // skip writing => hole
//                // remember that this is a hole
//                holes.add(data.position());
//                // ... but write zeros to data for checking data later
//                data.put(zeros);
//            }
//        }
//        data.flip();
//    }
//
//    public void initializeVolume(int stripeWidth) throws ONCRPCException, IOException, InterruptedException {
//        // create a volume (no access control)
//        RPCResponse r = testEnv.getMrcClient().mkvol(testEnv.getMRCAddress(), userCredentials, VOLUME_NAME,
//            new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, STRIPE_SIZE / 1024, stripeWidth),
//            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL.intValue(), 0);
//        r.get();
//        r.freeBuffers();
//    }
//
//    /**
//     * @throws java.lang.Exception
//     */
//    @After
//    public void tearDown() throws Exception {
//        testEnv.shutdown();
//
//        // free buffers
//        BufferPool.free(data);
//    }
//
//    @Test
//    public void testSimpleWithDifferentStrategies() throws Exception {
//        // partial replicas
//        System.out.println("\n### random strategy on partial replica ###");
//        simpleTest("file0", ReplicationFlags.setPartialReplica(ReplicationFlags.setRandomStrategy(0)));
//        System.out.println("\n### sequential strategy on partial replica ###");
//        simpleTest("file1", ReplicationFlags.setPartialReplica(ReplicationFlags.setSequentialStrategy(0)));
//        System.out.println("\n### rarest first strategy on partial replica ###");
//        simpleTest("file2", ReplicationFlags.setPartialReplica(ReplicationFlags.setRarestFirstStrategy(0)));
//        System.out.println("\n### sequential prefetching strategy on partial replica ###");
//        simpleTest("file3", ReplicationFlags.setPartialReplica(ReplicationFlags.setSequentialPrefetchingStrategy(0)));
//
//        // full replicas
//        System.out.println("\n### random strategy on full replica ###");
//        fullReplicasTest("file4", ReplicationFlags.setFullReplica(ReplicationFlags.setRandomStrategy(0)));
//        System.out.println("\n### sequential strategy on full replica ###");
//        fullReplicasTest("file5", ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialStrategy(0)));
//        System.out.println("\n### rarest first strategy on full replica ###");
//        fullReplicasTest("file6", ReplicationFlags.setRarestFirstStrategy(ReplicationFlags.setFullReplica(0)));
//        System.out.println("\n### sequential prefetching strategy on full replica ###");
//        fullReplicasTest("file7", ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialPrefetchingStrategy(0)));
//    }
//
//    private void simpleTest(String filename, int replicationFlags) throws Exception {
//        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/" + filename,
//                testEnv.getRpcClient(), userCredentials);
//
//        // test needs a stripe width of 2 OSDs
//        assertEquals(2, raf.getStripingPolicy().getWidth());
//
//        writeData(raf);
//
//        // set read-only
//        raf.setReadOnly(true);
//        assertEquals(data.limit(), raf.getXLoc().getXLocSet().getRead_only_file_size());
//
//        // set new deterministic selection of OSDs
//        raf.setReplicaSelectionPolicy(raf.SEQUENTIAL_REPLICA_SELECTION_POLICY);
//
//        // add replicas
//        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
//        assertEquals(6, replicas.size());
//        raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy(), replicationFlags);
//        raf.addReplica(replicas.subList(2, 4), raf.getStripingPolicy(), replicationFlags);
//        raf.addReplica(replicas.subList(4, 6), raf.getStripingPolicy(), replicationFlags);
//
//        // assert 4 replicas
//        assertEquals(4, raf.getXLoc().getNumReplicas());
//
//        raf.changeReplicaOrder();
//        // read data
//        for (int reads = 0; reads < 4; reads++) {
//            // read and check data => replication
//            byte[] resultBuffer = new byte[data.limit()];
//            raf.seek(0);
//            raf.read(resultBuffer, 0, resultBuffer.length);
//
//            // count zeros
//            int zeros = 0;
//            for (int i = 0; i < resultBuffer.length; i++)
//                if (resultBuffer[i] == (byte) 0)
//                    zeros++;
//
//            // FIXME: debug stuff
////            System.out.println("filesize: " + data.array().length + "\t zeros: " + zeros);
//
//            assertTrue(Arrays.equals(data.array(), resultBuffer));
//
//            raf.changeReplicaOrder();
//        }
//        // read EOF
//        for (int reads = 0; reads < 4; reads++) {
//            byte[] resultBuffer = new byte[STRIPE_SIZE];
//            raf.seek(data.limit());
//            raf.read(resultBuffer, 0, resultBuffer.length);
//            byte[] expected = new byte[STRIPE_SIZE];
//            Arrays.fill(expected, (byte) 0);
//            assertTrue(Arrays.equals(expected, resultBuffer));
//
//            // read EOF with data
//            resultBuffer = new byte[STRIPE_SIZE * 3];
//            raf.seek(data.limit() - STRIPE_SIZE * 2);
//            raf.read(resultBuffer, 0, resultBuffer.length);
//            // check data
//            expected = Arrays.copyOfRange(data.array(), data.limit() - STRIPE_SIZE * 2, data.limit());
//            assertTrue(Arrays.equals(expected, Arrays.copyOfRange(resultBuffer, 0, STRIPE_SIZE * 2)));
//            // check zeros at the end
//            expected = new byte[STRIPE_SIZE];
//            Arrays.fill(expected, (byte) 0);
//            assertTrue(Arrays.equals(expected, Arrays.copyOfRange(resultBuffer, STRIPE_SIZE * 2,
//                resultBuffer.length)));
//
//            raf.changeReplicaOrder();
//        }
//
//        raf.close();
//    }
//
//    private void fullReplicasTest(String filename, int replicationFlags) throws Exception {
//        generateData(1000 * 1000); // ca. 1 MB
//
//        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/" + filename,
//                testEnv.getRpcClient(), userCredentials);
//
//        // test needs a stripe width of 2 OSDs
//        assertEquals(2, raf.getStripingPolicy().getWidth());
//
//        writeData(raf);
//
//        // set read-only
//        raf.setReadOnly(true);
//        assertEquals(data.limit(), raf.getXLoc().getXLocSet().getRead_only_file_size());
//
//        // add replicas
//        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
//        assertEquals(6, replicas.size());
//        raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy(), replicationFlags);
//        raf.addReplica(replicas.subList(2, 4), raf.getStripingPolicy(), replicationFlags);
//        raf.addReplica(replicas.subList(4, 6), raf.getStripingPolicy(), replicationFlags);
//
//        raf.setReplicaSelectionPolicy(raf.SEQUENTIAL_REPLICA_SELECTION_POLICY);
//
//        // read a few bytes from every replica
//        for (int i = 0; i < 8; i++) {
//            // read data from replica => initiate background replication
//            byte[] resultBuffer = new byte[STRIPE_SIZE
//                * raf.getXLoc().getReplica(0).getStripingPolicy().getWidth()];
//            raf.seek(0);
//            raf.read(resultBuffer, 0, resultBuffer.length);
//            raf.changeReplicaOrder();
//        }
//
//        OSDClient osdClient = testEnv.getOSDClient();
//
//        // wait, so the file could be fully replicated in background
//        Thread.sleep(10000);
//
//        // check, if objects are replicated
//        List<Iterator<ServiceUUID>> replicaIts = new ArrayList<Iterator<ServiceUUID>>();
//        for (Replica replica : raf.getXLoc().getReplicas()) {
//            replicaIts.add(replica.getOSDs().iterator());
//        }
//        // parallel iteration through all OSDs of the replicas
//        while (replicaIts.get(0).hasNext()) {
//            List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
//            // get the OSDs from all replicas responsible for the same objects
//            for (Iterator<ServiceUUID> it : replicaIts) {
//                osds.add(it.next());
//            }
//            // collect the object sets from these OSDs
//            List<ObjectSet> objectSets = new ArrayList<ObjectSet>();
//            for (ServiceUUID osd : osds) {
//                RPCResponse<ObjectList> r = osdClient.internal_getObjectList(osd.getAddress(), raf
//                        .getFileId(), raf.getCredentials());
//                ObjectList objectList = r.get();
//                r.freeBuffers();
//                objectSets.add(new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(),
//                    objectList.getSet().array()));
//            }
//
//            // check, if all objects lists contains the same objects
//            ObjectSet set = objectSets.get(0);
//            for (ObjectSet objectSet : objectSets) {
//                assertEquals(set, objectSet);
//            }
//        }
//    }
//
//    @Test
//    public void testReplicasWithDifferentStripingPolicies() throws Exception {
//        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/testfile",
//                testEnv.getRpcClient(), userCredentials);
//        raf.setReplicaSelectionPolicy(raf.SEQUENTIAL_REPLICA_SELECTION_POLICY);
//
//        // check if original replica hast a stripe width of 2
//        assertEquals(2, raf.getStripingPolicy().getWidth());
//        writeData(raf);
//
//        // set read-only
//        raf.setReadOnly(true);
//
//        // add replica
//        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
//        assertEquals(6, replicas.size());
//
//        // add replica with stripe width of 1
//        StripingPolicy sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, (int) raf
//                .getStripeSize(), 1);
//        raf.addReplica(replicas.subList(0, 1), sp, ReplicationFlags.setPartialReplica(ReplicationFlags
//                .setSequentialStrategy(0)));
//        assertEquals(2, raf.getXLoc().getNumReplicas());
//        assertEquals(1, raf.getXLoc().getReplica(1).getStripingPolicy().getWidth());
//
//        // add replica with stripe width of 3
//        sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, (int) raf.getStripeSize(), 3);
//        raf.addReplica(replicas.subList(1, 4), sp, ReplicationFlags.setPartialReplica(ReplicationFlags
//                .setRandomStrategy(0)));
//        assertEquals(3, raf.getXLoc().getNumReplicas());
//        assertEquals(3, raf.getXLoc().getReplica(2).getStripingPolicy().getWidth());
//
//        // read 3 times (each replica will be read)
//        for (int i = 0; i < 3; i++) {
//            raf.seek(0);
//            byte[] resultBuffer = new byte[data.limit()];
//            raf.read(resultBuffer, 0, resultBuffer.length);
//
//            // change used replica
//            raf.changeReplicaOrder();
//        }
//    }
//
//    @Test
//    public void testBackgroundReplication() throws Exception {
//        HOLE_PROBABILITY = 0;
//        generateData(1000 * 1000); // ca. 1 MB
//
//        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/testfile",
//            testEnv.getRpcClient(), userCredentials);
//
//        // test needs a stripe width of 2 OSDs
//        assertEquals(2, raf.getStripingPolicy().getWidth());
//
//        writeData(raf);
//
//        // set read-only
//        raf.setReadOnly(true);
//        assertEquals(data.limit(), raf.getXLoc().getXLocSet().getRead_only_file_size());
//
//        // add replicas
//        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
//        assertEquals(6, replicas.size());
//        raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy(), ReplicationFlags
//                .setFullReplica(ReplicationFlags.setRandomStrategy(0)));
//        raf.addReplica(replicas.subList(2, 4), raf.getStripingPolicy(), ReplicationFlags
//                .setFullReplica(ReplicationFlags.setRandomStrategy(0)));
//
//        // read data only from replica 2
//        raf.setReplicaSelectionPolicy(new RandomAccessFile.ReplicaSelectionPolicy() {
//            @Override
//            public List<Replica> getReplicaOrder(List<Replica> replicas) {
//                List<Replica> list = new ArrayList<Replica>();
//                if (replicas.size() > 1)
//                    list.add(replicas.get(1));
//                else
//                    list.add(replicas.get(0));
//                return list;
//            }
//        });
//        // read data from replica 2 => initiate background replication
//        byte[] resultBuffer = new byte[STRIPE_SIZE
//            * raf.getXLoc().getReplica(0).getStripingPolicy().getWidth()];
//        raf.seek(0);
//        raf.read(resultBuffer, 0, resultBuffer.length);
//
//        // read data only from replica 3
//        raf.setReplicaSelectionPolicy(new RandomAccessFile.ReplicaSelectionPolicy() {
//            @Override
//            public List<Replica> getReplicaOrder(List<Replica> replicas) {
//                List<Replica> list = new ArrayList<Replica>();
//                if (replicas.size() > 2)
//                    list.add(replicas.get(2));
//                else
//                    list.add(replicas.get(0));
//                return list;
//            }
//        });
//        // read data from replica 3 => initiate background replication
//        resultBuffer = new byte[STRIPE_SIZE * raf.getXLoc().getReplica(0).getStripingPolicy().getWidth()];
//        raf.seek(0);
//        raf.read(resultBuffer, 0, resultBuffer.length);
//
//        OSDClient osdClient = testEnv.getOSDClient();
//
//        // wait, so the file could be fully replicated in background
//        Thread.sleep(2000);
//
//        // check, if objects are replicated
//        RPCResponse<InternalReadLocalResponse> r = osdClient.internal_read_local(raf.getXLoc().getReplica(1)
//                .getOSDForObject(2).getAddress(), raf.getFileId(), raf.getCredentials(), 8, 0, 0,
//            STRIPE_SIZE, false, null);
//        ObjectData oData = r.get().getData();
//        r.freeBuffers();
//        assertEquals(0, oData.getZero_padding());
//        assertEquals(STRIPE_SIZE, oData.getData().capacity());
//
//        r = osdClient.internal_read_local(raf.getXLoc().getReplica(1).getOSDForObject(8).getAddress(), raf
//                .getFileId(), raf.getCredentials(), 8, 0, 0, STRIPE_SIZE, false, null);
//        oData = r.get().getData();
//        r.freeBuffers();
//        assertEquals(0, oData.getZero_padding());
//        assertEquals(STRIPE_SIZE, oData.getData().capacity());
//
//        r = osdClient.internal_read_local(raf.getXLoc().getReplica(1).getOSDForObject(15).getAddress(), raf
//                .getFileId(), raf.getCredentials(), 15, 0, 0, STRIPE_SIZE, false, null);
//        oData = r.get().getData();
//        r.freeBuffers();
//        assertEquals(0, oData.getZero_padding());
//        assertEquals(STRIPE_SIZE, oData.getData().capacity());
//
//        r = osdClient.internal_read_local(raf.getXLoc().getReplica(2).getOSDForObject(16).getAddress(), raf
//                .getFileId(), raf.getCredentials(), 16, 0, 0, STRIPE_SIZE, false, null);
//        oData = r.get().getData();
//        r.freeBuffers();
//        assertEquals(0, oData.getZero_padding());
//        assertEquals(STRIPE_SIZE, oData.getData().capacity());
//
//        r = osdClient.internal_read_local(raf.getXLoc().getReplica(2).getOSDForObject(37).getAddress(), raf
//                .getFileId(), raf.getCredentials(), 37, 0, 0, STRIPE_SIZE, false, null);
//        oData = r.get().getData();
//        r.freeBuffers();
//        assertEquals(0, oData.getZero_padding());
//        assertEquals(STRIPE_SIZE, oData.getData().capacity());
//    }
//
//    @Test
//    public void testReplicaRemoval() throws Exception {
//        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/testfile",
//            testEnv.getRpcClient(), userCredentials);
//        // test needs a stripe width of 2 OSDs
//        assertEquals(2, raf.getStripingPolicy().getWidth());
//
//        writeData(raf);
//
//        // set read-only
//        raf.setReadOnly(true);
//
//        // add replica
//        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
//        assertEquals(6, replicas.size());
//        raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy(), ReplicationFlags
//                .setPartialReplica(ReplicationFlags.setRandomStrategy(0)));
//        assertEquals(2, raf.getXLoc().getNumReplicas());
//
//        // read only from replica 2
//        raf.setReplicaSelectionPolicy(new RandomAccessFile.ReplicaSelectionPolicy() {
//            @Override
//            public List<Replica> getReplicaOrder(List<Replica> replicas) {
//                List<Replica> list = new ArrayList<Replica>();
//                if (replicas.size() > 1)
//                    list.add(replicas.get(1));
//                else
//                    list.add(replicas.get(0));
//                return list;
//            }
//        });
//
//        // read every object from this replica => replicate objects
//        // read and check data => replication
//        raf.seek(0);
//        byte[] resultBuffer = new byte[data.limit()];
//        raf.read(resultBuffer, 0, resultBuffer.length);
//        assertTrue(Arrays.equals(data.array(), resultBuffer));
//
//        // save old credentials
//        FileCredentials credentials = raf.getCredentials();
//
//        // remove replica
//        raf.removeReplica(replicas.get(0));
//        assertEquals(1, raf.getXLoc().getNumReplicas());
//
//        OSDClient osdClient = testEnv.getOSDClient();
//
//        // NOTE: as long as xLoc cache is not implemented this test will not
//        // work
//        // check, if old XLoc will be rejected
//        // RPCResponse<ObjectData> r =
//        // osdClient.read(replicas.get(0).getAddress(), raf.getFileId(),
//        // credentials, 0, 0, 0, STRIPE_SIZE);
//        // try {
//        // ObjectData oData = r.get();
//        // fail();
//        // } catch (Exception e) {
//        // // correct
//        // }
//        // r.freeBuffers();
//
//        // NOTE: as long as xLoc cache is not implemented this tests will work
//        // wait, so the open-file-table could close the file and delete the data
//        System.out.println("Test is waiting 60s.");
//        Thread.sleep(60000);
//
//        // check, if objects are deleted on OSDs (only spot check)
//        RPCResponse<ObjectList> r = osdClient.internal_getObjectList(replicas.get(0).getAddress(), raf
//                .getFileId(), credentials);
//        ObjectList objectList = r.get();
//        r.freeBuffers();
//        ObjectSet objectSet = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(),
//            objectList.getSet().array());
//        assertEquals(0, objectSet.size());
//        r = osdClient.internal_getObjectList(replicas.get(1).getAddress(), raf.getFileId(), credentials);
//        objectList = r.get();
//        r.freeBuffers();
//        objectSet = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList
//                .getSet().array());
//        assertEquals(0, objectSet.size());
//        r = osdClient.internal_getObjectList(replicas.get(1).getAddress(), raf.getFileId(), credentials);
//        objectList = r.get();
//        r.freeBuffers();
//        objectSet = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList
//                .getSet().array());
//        assertEquals(0, objectSet.size());
//
//        // read and check data on remaining (original) replica
//        raf.setReplicaSelectionPolicy(raf.SEQUENTIAL_REPLICA_SELECTION_POLICY);
//        raf.seek(0);
//        resultBuffer = new byte[data.limit()];
//        raf.read(resultBuffer, 0, resultBuffer.length);
//        assertTrue(Arrays.equals(data.array(), resultBuffer));
//    }
//
//    /**
//     * this test must fail because of delayed deletion of data after removing a
//     * replica
//     *
//     * @throws Exception
//     */
//    @Test
//    public void testRemoveAndAddInShortTime() throws Exception {
//        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/testfile",
//            testEnv.getRpcClient(), userCredentials);
//        OSDClient osdClient = testEnv.getOSDClient();
//
//        // test needs a stripe width of 2 OSDs
//        assertEquals(2, raf.getStripingPolicy().getWidth());
//
//        writeData(raf);
//
//        // set read-only
//        raf.setReadOnly(true);
//
//        // read only from replica 2
//        raf.setReplicaSelectionPolicy(new RandomAccessFile.ReplicaSelectionPolicy() {
//            @Override
//            public List<Replica> getReplicaOrder(List<Replica> replicas) {
//                List<Replica> list = new ArrayList<Replica>();
//                if (replicas.size() > 1)
//                    list.add(replicas.get(1));
//                else
//                    list.add(replicas.get(0));
//                return list;
//            }
//        });
//
//        // add replica
//        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
//        assertEquals(6, replicas.size());
//
//        for (int i = 0; i < 2; i++) {
//            raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy(), ReplicationFlags
//                    .setFullReplica(ReplicationFlags.setRandomStrategy(0)));
//            assertEquals(2, raf.getXLoc().getNumReplicas());
//
//            // read an object from this replica => replicate objects
//            raf.seek(0);
//            byte[] resultBuffer = new byte[STRIPE_SIZE * 2];
//            raf.read(resultBuffer, 0, resultBuffer.length);
//
//            // sleep some time so all objects could be replicated
//            System.out.println("Test is waiting 5s.");
//            Thread.sleep(5000);
//
//            // check if objects would be really replicated
//            RPCResponse<ObjectList> r = osdClient.internal_getObjectList(replicas.get(0).getAddress(), raf
//                    .getFileId(), raf.getCredentials());
//            ObjectList objectList = r.get();
//            r.freeBuffers();
//            ObjectSet objectSet = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(),
//                objectList.getSet().array());
//            System.out.println("run " + i + ": " + objectSet);
//            assertTrue(10 <= objectSet.size());
//            r = osdClient.internal_getObjectList(replicas.get(0).getAddress(), raf.getFileId(), raf
//                    .getCredentials());
//            objectList = r.get();
//            r.freeBuffers();
//            objectSet = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList
//                    .getSet().array());
//            System.out.println("run " + i + ": " + objectSet);
//            assertTrue(10 <= objectSet.size());
//
//            // remove replica
//            raf.removeReplica(replicas.get(0));
//            assertEquals(1, raf.getXLoc().getNumReplicas());
//
//            // add replica again
//            raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy(), ReplicationFlags
//                    .setFullReplica(ReplicationFlags.setRandomStrategy(0)));
//            assertEquals(2, raf.getXLoc().getNumReplicas());
//
//            // read an object from this replica => replicate objects
//            raf.seek(0);
//            resultBuffer = new byte[STRIPE_SIZE * 2];
//            raf.read(resultBuffer, 0, resultBuffer.length);
//
//            // wait, so the open-file-table could close the file and delete all
//            // the data (THAT'S THE PROBLEM)
//            System.out.println("Test is waiting 60s.");
//            Thread.sleep(60000);
//
//            r = osdClient.internal_getObjectList(replicas.get(0).getAddress(), raf.getFileId(), raf
//                    .getCredentials());
//            objectList = r.get();
//            r.freeBuffers();
//            objectSet = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList
//                    .getSet().array());
//            System.out.println("run " + i + ": " + objectSet);
//            assertTrue(0 == objectSet.size());
//            r = osdClient.internal_getObjectList(replicas.get(1).getAddress(), raf.getFileId(), raf
//                    .getCredentials());
//            objectList = r.get();
//            r.freeBuffers();
//            objectSet = new ObjectSet(objectList.getStripe_width(), objectList.getFirst_(), objectList
//                    .getSet().array());
//            System.out.println("run " + i + ": " + objectSet);
//            assertTrue(0 == objectSet.size());
//
//            // remove replica
//            raf.removeReplica(replicas.get(0));
//            assertEquals(1, raf.getXLoc().getNumReplicas());
//        }
//    }
//
//    private void writeData(RandomAccessFile raf) throws Exception {
//        // write data
//        int bytesToWrite;
//        int startOffset = 0;
//        for (int holeStart : holes) {
//            // write data in front of the hole
//            bytesToWrite = holeStart - startOffset;
//            raf.write(data.array(), startOffset, bytesToWrite);
//            // skip hole
//            raf.seek(holeStart + STRIPE_SIZE);
//            startOffset = holeStart + STRIPE_SIZE;
//        }
//        // write last data
//        raf.write(data.array(), startOffset, data.limit() - startOffset);
//    }
//}