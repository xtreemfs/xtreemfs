/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.test.osd.replication;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 * 
 * 12.05.2009
 */
public class ReplicationRAFTest extends TestCase {
    public static final int    STRIPE_SIZE = 1024 * 10;              // 10kb in byte
    public static final String VOLUME_NAME = "test";

    private ReusableBuffer     data;
    /**
     * contains the position where the hole starts (the size of a hole is always the stripe size in this test)
     */
    private SortedSet<Integer> holes;
    private TestEnvironment    testEnv;
    private UserCredentials    userCredentials;

    private static Random      random      = new Random(843);

    public ReplicationRAFTest() {
        // TODO Auto-generated constructor stub
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        // user credentials
        StringSet groupIDs = new StringSet();
        groupIDs.add("root");
        userCredentials = new UserCredentials("root", groupIDs, "");

        // startup: DIR, MRC, 8 OSDs, ...
        testEnv = new TestEnvironment(TestEnvironment.Services.DIR_SERVICE, TestEnvironment.Services.MRC,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT,
                TestEnvironment.Services.OSD, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD,
                TestEnvironment.Services.OSD, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD,
                TestEnvironment.Services.OSD, TestEnvironment.Services.OSD);
        testEnv.start();

        // wait a bit so the MRC could notice the OSDs
        Thread.sleep(1000);

        generateData(1000 * 1000 * 3); // ca. 3 MB
        initializeVolume(2);
    }

    private void generateData(int appriximateSize) {
        data = BufferPool.allocate(appriximateSize);
        holes = new TreeSet<Integer>();
        byte[] zeros = new byte[STRIPE_SIZE];
        Arrays.fill(zeros, (byte) 0);
        
        while (data.position() < appriximateSize - STRIPE_SIZE) {
            if (random.nextInt(100) < 70) { // 90% chance
                // write A piece of data
                ReusableBuffer tmpData = SetupUtils.generateData(STRIPE_SIZE);
                data.put(tmpData);
                BufferPool.free(tmpData);
            } else { // skip writing => hole
                // remember that this is a hole
                holes.add(data.position());
                // ... but write zeros to data for checking data later
                data.put(zeros);
            }
        }
        data.flip();
    }

    public void initializeVolume(int stripeWidth) throws ONCRPCException, IOException, InterruptedException {
        // create a volume (no access control)
        RPCResponse r = testEnv.getMrcClient().mkvol(testEnv.getMRCAddress(), userCredentials, VOLUME_NAME,
                OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE.intValue(),
                new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, STRIPE_SIZE / 1024, stripeWidth),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL.intValue(), 0);
        r.get();
        r.freeBuffers();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();

        // free buffers
        BufferPool.free(data);
    }

    @Test
    public void testAllAvailableOSDsAreAReplica() throws Exception {
        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/testfile",
                testEnv.getRpcClient(), userCredentials);

        // test needs a stripe width of 2 OSDs
        assertEquals(2, raf.getStripingPolicy().getWidth());

        writeData(raf);
        
        // set new deterministic selection of OSDs
        raf.setOSDSelectionPolicy(raf.SEQUENTIAL_OSD_SELECTION_POLICY);

        // set read-only
        raf.setReadOnly(true);

        // add replicas
        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
        assertEquals(6, replicas.size());
        raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy());
        raf.addReplica(replicas.subList(2, 4), raf.getStripingPolicy());
        raf.addReplica(replicas.subList(4, 6), raf.getStripingPolicy());

        // assert 4 replicas
        assertEquals(4, raf.getXLoc().getNumReplicas());

        readAllAvailableOSDsAreAReplica(raf);
    }

    private void readAllAvailableOSDsAreAReplica(RandomAccessFile raf) throws Exception {
        // read data
        for (int reads = 0; reads < 4; reads++) {
            // read and check data => replication
            byte[] resultBuffer = new byte[data.limit()];
            raf.seek(0);
            raf.read(resultBuffer, 0, resultBuffer.length);
            assertTrue(Arrays.equals(data.array(), resultBuffer));
        }
        // read EOF
        for (int reads = 0; reads < 4; reads++) {
            byte[] resultBuffer = new byte[STRIPE_SIZE];
            raf.seek(data.limit());
            raf.read(resultBuffer, 0, resultBuffer.length);
            byte[] expected = new byte[STRIPE_SIZE];
            Arrays.fill(expected, (byte) 0);
            assertTrue(Arrays.equals(expected, resultBuffer));

            // read EOF with data
            resultBuffer = new byte[STRIPE_SIZE * 3];
            raf.seek(data.limit() - STRIPE_SIZE * 2);
            raf.read(resultBuffer, 0, resultBuffer.length);
            // check data
            expected = Arrays.copyOfRange(data.array(), data.limit() - STRIPE_SIZE * 2, data.limit());
            assertTrue(Arrays.equals(expected, Arrays.copyOfRange(resultBuffer, 0, STRIPE_SIZE * 2)));
            // check zeros at the end
            expected = new byte[STRIPE_SIZE];
            Arrays.fill(expected, (byte) 0);
            assertTrue(Arrays.equals(expected, Arrays.copyOfRange(resultBuffer, STRIPE_SIZE * 2,
                    resultBuffer.length)));
        }
    }

    private void writeData(RandomAccessFile raf) throws Exception {
        // write data
        int bytesToWrite;
        int startOffset = 0;
        for (int holeStart : holes) {
            // write data in front of the hole
            bytesToWrite = holeStart - startOffset;
            raf.write(data.array(), startOffset, bytesToWrite);
            // skip hole
            raf.seek(holeStart + STRIPE_SIZE);
            startOffset = holeStart + STRIPE_SIZE;
        }
        // write last data
        raf.write(data.array(), startOffset, data.limit() - startOffset);
    }

    /**
     * this test generates heavy load on your pc
     * @throws Exception
     */
    @Test
    public void testMultipleClients() throws Exception {
        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/testfile",
                testEnv.getRpcClient(), userCredentials);
        writeData(raf);

        // set new deterministic selection of OSDs
        raf.setOSDSelectionPolicy(raf.SEQUENTIAL_OSD_SELECTION_POLICY);

        // set read-only
        raf.setReadOnly(true);

        // add replicas
        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
        assertEquals(6, replicas.size());
        raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy());
        raf.addReplica(replicas.subList(2, 4), raf.getStripingPolicy());
        raf.addReplica(replicas.subList(4, 6), raf.getStripingPolicy());

        // assert 4 replicas
        assertEquals(4, raf.getXLoc().getNumReplicas());

        int clients = 12;
        Future[] results = new Future[clients];
        ExecutorService executor = Executors.newFixedThreadPool(clients);
        // start threads
        for (int i = 0; i < clients; i++) {
            results[i] = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        RandomAccessFile raf = new RandomAccessFile("r", testEnv.getMRCAddress(), VOLUME_NAME
                                + "/testfile", testEnv.getRpcClient(), userCredentials);

                        readAllAvailableOSDsAreAReplica(raf);
                        return true;
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        fail();
                        return false;
                    }
                }
            });
        }
        // check results
        for (int i = 0; i < clients; i++) {
            try {
                assertTrue((Boolean) results[i].get());
            } catch (ExecutionException e) {
                if(e.getCause() instanceof AssertionFailedError)
                    e.getCause().printStackTrace();
                    fail(e.getCause().getMessage());
            }
        }
    }

    @Test
    public void testReplicaRemoval() throws Exception {
        RandomAccessFile raf = new RandomAccessFile("rw", testEnv.getMRCAddress(), VOLUME_NAME + "/testfile",
                testEnv.getRpcClient(), userCredentials);
        // test needs a stripe width of 2 OSDs
        assertEquals(2, raf.getStripingPolicy().getWidth());

        writeData(raf);

        // set read-only
        raf.setReadOnly(true);

        // add replica
        List<ServiceUUID> replicas = raf.getSuitableOSDsForAReplica();
        assertEquals(6, replicas.size());
        raf.addReplica(replicas.subList(0, 2), raf.getStripingPolicy());
        assertEquals(2, raf.getXLoc().getNumReplicas());

        // read only from replica 2
        raf.setOSDSelectionPolicy(new RandomAccessFile.OSDSelectionPolicy() {
            @Override
            public List<ServiceUUID> getOSDorder(List<ServiceUUID> osds) {
                List<ServiceUUID> list = new ArrayList<ServiceUUID>();
                // only the second replica should be used
                list.add(osds.get(1));
                return list;
            }
        });

        // read every object from this replica => replicate objects
        // read and check data => replication
        raf.seek(0);
        byte[] resultBuffer = new byte[data.limit()];
        raf.read(resultBuffer, 0, resultBuffer.length);
        assertTrue(Arrays.equals(data.array(), resultBuffer));

        // save old credentials
        FileCredentials credentials = raf.getCredentials();

        // remove replica
        raf.removeReplica(replicas.get(0));
        assertEquals(1, raf.getXLoc().getNumReplicas());

        OSDClient osdClient = testEnv.getOSDClient();

        // NOTE: as long as xLoc cache is not implemented this test will not work
        // check, if old XLoc will be rejected
//        RPCResponse<ObjectData> r = osdClient.read(replicas.get(0).getAddress(), raf.getFileId(),
//                credentials, 0, 0, 0, STRIPE_SIZE);
//        try {
//            ObjectData oData = r.get();
//            fail();
//        } catch (Exception e) {
//            // correct
//        }
//        r.freeBuffers();

        // NOTE: as long as xLoc cache is not implemented this tests will work
        // wait, so the open-file-table could close the file and delete the data
        Thread.sleep(60000);

        // check, if objects are deleted on OSDs (only spot check)
        RPCResponse<ObjectData> r = osdClient.check_object(replicas.get(0).getAddress(), raf.getFileId(),
                credentials, 0, 0);
        ObjectData oData = r.get();
        r.freeBuffers();
        assertEquals(0, oData.getZero_padding());
        r = osdClient.check_object(replicas.get(1).getAddress(), raf.getFileId(), credentials, 1, 0);
        oData = r.get();
        r.freeBuffers();
        assertEquals(0, oData.getZero_padding());
        r = osdClient.check_object(replicas.get(0).getAddress(), raf.getFileId(), credentials, 10, 0);
        oData = r.get();
        r.freeBuffers();
        assertEquals(0, oData.getZero_padding());
        r = osdClient.check_object(replicas.get(1).getAddress(), raf.getFileId(), credentials, 11, 0);
        oData = r.get();
        r.freeBuffers();
        assertEquals(0, oData.getZero_padding());
        r = osdClient.check_object(replicas.get(0).getAddress(), raf.getFileId(), credentials, 100, 0);
        oData = r.get();
        r.freeBuffers();
        assertEquals(0, oData.getZero_padding());
        r = osdClient.check_object(replicas.get(1).getAddress(), raf.getFileId(), credentials, 101, 0);
        oData = r.get();
        r.freeBuffers();
        assertEquals(0, oData.getZero_padding());

        // read and check data on remaining (original) replica
        raf.setOSDSelectionPolicy(raf.SEQUENTIAL_OSD_SELECTION_POLICY);
        raf.seek(0);
        resultBuffer = new byte[data.limit()];
        raf.read(resultBuffer, 0, resultBuffer.length);
        assertTrue(Arrays.equals(data.array(), resultBuffer));
    }
}
