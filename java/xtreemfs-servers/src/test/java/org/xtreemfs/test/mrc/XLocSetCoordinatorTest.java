/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT;
import static org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY;
import static org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
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
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.InvalidViewException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class XLocSetCoordinatorTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    private static TestEnvironment testEnv;

    private static UserCredentials userCredentials;

    private static Auth            auth;

    private static AdminClient     client;

    private static Options         options;

    private static String          mrcAddress;

    private static String          dirAddress;

    private static StripingPolicy  defaultStripingPolicy;

    private static OSD[]           osds;

    private static OSDConfig[]     configs;

    private final static int       NUM_OSDS = 5;

    private static int             LEASE_TIMEOUT_MS;

    @BeforeClass
    public static void initializeTest() throws Exception {
        // cleanup
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(Logging.LEVEL_WARN);
        // Logging.start(Logging.LEVEL_INFO);
        // Logging.start(Logging.LEVEL_DEBUG);

        testEnv = new TestEnvironment(
                new TestEnvironment.Services[] { TestEnvironment.Services.TIME_SYNC,
                        TestEnvironment.Services.UUID_RESOLVER, TestEnvironment.Services.RPC_CLIENT,
                        TestEnvironment.Services.DIR_SERVICE, TestEnvironment.Services.DIR_CLIENT,
                        TestEnvironment.Services.MRC, TestEnvironment.Services.MRC_CLIENT,
                        TestEnvironment.Services.OSD_CLIENT });

        testEnv.start();

        // setup osds
        osds = new OSD[NUM_OSDS];
        configs = SetupUtils.createMultipleOSDConfigs(NUM_OSDS);
        for (int i = 0; i < NUM_OSDS; i++) {
            osds[i] = new OSD(configs[i]);
        }

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        defaultStripingPolicy = StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                .setStripeSize(128).setWidth(1).build();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
        auth = RPCAuthentication.authNone;

        options = new Options();

        options.setMaxTries(2);

        client = ClientFactory.createAdminClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        LEASE_TIMEOUT_MS = configs[0].getFleaseLeaseToMS();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        for (int i = 0; i < osds.length; i++) {
            if (osds[i] != null) {
                osds[i].shutdown();
            }
        }
        testEnv.shutdown();
        client.shutdown();
    }

    /**
     * Test to ensure, that either a InvalidViewException is thrown if a clients tries to read from a replica, that has
     * been removed when view renewals are disabled or the view is renewed transparently.
     */
    @Test
    public void testViewsOnReplicaRemoval() throws Exception {
        String volumeName = "testViewsOnReplicaRemoval";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        // Setup a full read only replica with sequential access strategy.
        int repl_flags = ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialStrategy(0));
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 3,
                repl_flags);

        volume.close();

        removeReadOutdated(volumeName, "/testfile-with-renewal", true);
        removeReadOutdated(volumeName, "/testfile-wo-renewal", false);
    }

    /**
     * Remove the primary replica and provoke an invalid view error by accessing the file with an outdated xLocSet.
     * 
     * @param volumeName
     * @param fileName
     * @param renewView
     * @throws Exception
     */
    private void removeReadOutdated(String volumeName, String fileName, boolean renewView) throws Exception {
        AdminVolume volume = client.openVolume(volumeName, null, options);

        // Open the testfile and write some bytes not "0".
        AdminFileHandle fileHandle = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_CREAT, SYSTEM_V_FCNTL_H_O_RDWR), 0777);

        int count = 256 * 1024;
        ReusableBuffer data = SetupUtils.generateData(count, (byte) 1);

        fileHandle.write(userCredentials, data.createViewBuffer().getData(), count, 0);
        fileHandle.close();

        // Give XtreemFS some time to replicate the data. This is only needed for the RONLY replication.
        Thread.sleep(5 * 1000);

        // Open a fileHandle again and keep it open while removing the primary replica from another client.
        fileHandle = volume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));

        // Get a replica to remove. This will probably be on a backup. If the replica is on the primary the test will
        // fail, because lease returnal isn't available yet and the second request will time out due to redirection
        // errors.
        List<Replica> replicas = fileHandle.getReplicasList();
        Replica replica = replicas.get(1);
        int prevReplicaCount = fileHandle.getReplicasList().size();

        // Since striping is disabled there should be only one OSD for this certain replica.
        assertEquals(1, replica.getOsdUuidsCount());

        // Use another Volume instance to remove the replica from the OSD.
        AdminVolume controlVolume = client.openVolume(volumeName, null, options);
        controlVolume.removeReplica(userCredentials, fileName, replica.getOsdUuids(0));
        AdminFileHandle controlFile = controlVolume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));

        assertEquals(controlFile.getReplicasList().size(), prevReplicaCount - 1);

        controlFile.close();
        controlVolume.close();

        // Reading from the previous fileHandle with the outdated xLocSet has to result in an error.
        try {
            if (renewView) {
                readWithViewRenewal(fileHandle);
            } else {
                readInvalidView(fileHandle);
            }
        } finally {
            fileHandle.close();
            volume.close();
        }
    }

    /**
     * Test to ensure, that either a InvalidViewException is thrown when view renewals are disabled or the view is
     * renewed transparently, after a new replicas is added and a client tries to read with an outdated view.
     */
    @Test
    public void testViewsOnReplicaAdding() throws Exception {
        String volumeName = "testViewsOnReplicaAdding";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));

        // Setup a full read only replica with sequential access strategy.
        int repl_flags = ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialStrategy(0));
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 2,
                repl_flags);
        volume.close();

        addReadOutdated(volumeName, "/testfile-with-renewal", true);
        addReadOutdated(volumeName, "/testfile-wo-renewal", false);
    }

    /**
     * Add additional replicas and and provoke an invalid view error by accessing the file with an outdated xLocSet.
     * 
     * @param volumeName
     * @param fileName
     * @param renewView
     * @throws Exception
     */
    private void addReadOutdated(String volumeName, String fileName, boolean renewView) throws Exception {
        // Open the testfile and write some data to it.
        AdminVolume outdatedVolume = client.openVolume(volumeName, null, options);
        AdminFileHandle outdatedFile = outdatedVolume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_CREAT, SYSTEM_V_FCNTL_H_O_RDWR), 0777);

        ReusableBuffer data = SetupUtils.generateData(256, (byte) 1);
        outdatedFile.write(userCredentials, data.createViewBuffer().getData(), 256, 0);
        outdatedFile.close();

        // Give XtreemFS some time to replicate the data. This is only needed for the RONLY replication.
        Thread.sleep(5 * 1000);

        // Open a fileHandle again and keep it open while add replicas from another client.
        outdatedFile = outdatedVolume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));

        // Use another Volume instance to add additional replicas.
        AdminVolume volume = client.openVolume(volumeName, null, options);
        addReplicas(volume, fileName, 1);
        volume.close();

        // Reading from the previous fileHandle with the outdated xLocSet has to result in an error.
        try {
            if (renewView) {
                readWithViewRenewal(outdatedFile);
            } else {
                readInvalidView(outdatedFile);
            }
        } finally {
            outdatedFile.close();
            outdatedVolume.close();
        }
    }

    /**
     * This test covers the case, that a number of replicas is added which will form a new majority. It has to be
     * ensured that the correct data is returned, even if only new replicas are accessed.
     */
    @Test
    public void testAddMajority() throws Exception {
        String volumeName = "testAddMajority";
        String fileName = "/testfile";

        SuspendableOSDRequestDispatcher[] suspOSDs = replaceWithSuspendableOSDs(0, 2);

        // Create and open the volume and set a replication factor of 2.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        // Ensure the selected OSDs and replicas a sorted ascending by UUIDs.
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 2, 0);
        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));
        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));

        // Create the testfile by writing some bytes to it.
        AdminFileHandle file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDWR, SYSTEM_V_FCNTL_H_O_CREAT), 0777);
        ReusableBuffer dataIn = SetupUtils.generateData(256 * 1024, (byte) 1);
        file.write(userCredentials, dataIn.getData(), 256 * 1024, 0);
        dataIn.clear();
        file.close();

        // Add another 3 replicas that form a majority by itself.
        addReplicas(volume, fileName, 3);
        
        // Ensure no primary can exist, by waiting until the lease timed out.
        Thread.sleep(LEASE_TIMEOUT_MS + 500);

        // Reverse the replica selection policy to ensure subsequent requests will be directed to the recently added
        // replicas.
        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_REVERSE }));

        // Suspend the original 2 first OSDs to ensure only the recently added replicas can be accessed.
        suspOSDs[0].suspend();
        suspOSDs[1].suspend();

        // Read from the file again and ensure the data is consistent.
        file = volume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDWR));
        byte[] dataOut = new byte[1];
        file.read(userCredentials, dataOut, 1, 0);
        file.close();
        volume.close();

        assertEquals(dataOut[0], (byte) 1);

        resetSuspendableOSDs(suspOSDs, 0);
    }

    /**
     * This test covers the removal of replicas forming a majority. It has to be ensured that the remaining replicas are
     * up to date and the correct (last written) data is returned.
     */
    @Test
    public void testRemoveMajority() throws Exception {
        String volumeName = "testRemoveMajority";
        String fileName = "/testfile";

        SuspendableOSDRequestDispatcher[] suspOSDs = replaceWithSuspendableOSDs(0, 2);

        // Create and open the volume and set a replication factor of 5.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 5, 0);

        // Ensure the selected OSDs are sorted ascending, and the list of replicas is sorted decending by UUIDs.
        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));
        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_REVERSE }));

        // Suspend the first 2 of the 5 replicas, to ensure they won't get updated when writing to the file.
        suspOSDs[0].suspend();
        suspOSDs[1].suspend();

        AdminFileHandle file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDWR, SYSTEM_V_FCNTL_H_O_CREAT), 0777);
        ReusableBuffer dataIn = SetupUtils.generateData(256 * 1024, (byte) 1);
        file.write(userCredentials, dataIn.getData(), 256 * 1024, 0);
        dataIn.clear();

        // Resume the first 2 OSDs again.
        suspOSDs[0].resume();
        suspOSDs[1].resume();

        // Remove the last 3 OSDs, which have been updated by the last write.
        List<Replica> replicas = file.getReplicasList();
        for (int i = 0; i < 3; i++) {
            Replica replica = replicas.get(i);

            // Since striping is disabled there can be only one OSD for this certain replica.
            assertEquals(1, replica.getOsdUuidsCount());
            volume.removeReplica(userCredentials, fileName, replica.getOsdUuids(0));
        }
        file.close();

        // Read from the file again and ensure the data is consistent.
        file = volume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));
        byte[] dataOut = new byte[1];
        file.read(userCredentials, dataOut, 1, 0);
        file.close();
        volume.close();

        assertEquals(dataOut[0], (byte) 1);

        resetSuspendableOSDs(suspOSDs, 0);
    }
    
    
    /**
     * This test covers the functionality in case of a network partition when not more then n+1/2 replicas are
     * available. The majority has to be established both in the old and in the new replica set.
     */
    @Test
    public void testPartition() throws Exception {
        String volumeName = "testPartition";
        String fileName = "/testfile";
        
        SuspendableOSDRequestDispatcher[] suspOSDs = replaceWithSuspendableOSDs(0, 1);

        // Create and open the volume and set a replication factor of 4.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 4, 0);

        // Ensure the selected OSDs are sorted ascending, and the list of replicas is sorted decending by UUIDs.
        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));
        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_REVERSE }));

        // Create the file an write some data to it.
        AdminFileHandle file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDWR, SYSTEM_V_FCNTL_H_O_CREAT), 0777);
        ReusableBuffer dataIn = SetupUtils.generateData(256 * 1024, (byte) 1);
        file.write(userCredentials, dataIn.getData(), 256 * 1024, 0);
        dataIn.clear();

        // Suspend the first OSD. The policy requires a majority of 3 for a replication factor of 4.
        suspOSDs[0].suspend();

        // Add another replica.
        addReplicas(volume, fileName, 1);

        // Get the replica recently added from the replica list and remove it.
        // Since a majority in both, the new and the old set, has to be ensured, only one OSD can be suspended.
        List<Replica> replicas = file.getReplicasList();
        Replica replica = replicas.get(0);
        volume.removeReplica(userCredentials, fileName, replica.getOsdUuids(0));

        file.close();
        volume.close();

        resetSuspendableOSDs(suspOSDs, 0);
    }

    /**
     * This test ensures progress in case of corrupted version state files.
     */
    @Test
    public void testCorruptVersionStateFile() throws Exception {
        String volumeName = "testCorruptVersionStateFile";
        String fileName = "/testfile";

        // Create and open the volume and set a replication factor of 4.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        // Ensure the selected OSDs are sorted ascending.
        volume.setOSDSelectionPolicy(userCredentials,
                Helper.policiesToString(
                        new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                                OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));

        // Create the file an write some data to it.
        AdminFileHandle file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDWR, SYSTEM_V_FCNTL_H_O_CREAT), 0777);
        ReusableBuffer dataIn = SetupUtils.generateData(256 * 1024, (byte) 1);
        file.write(userCredentials, dataIn.getData(), 256 * 1024, 0);
        dataIn.clear();

        String fileId = file.getGlobalFileId();
        file.close();

        // Truncate the version state.
        HashStorageLayout hsl = new HashStorageLayout(configs[0], new MetadataCache());
        String filePath = hsl.generateAbsoluteFilePath(fileId) + "/" + HashStorageLayout.XLOC_VERSION_STATE_FILENAME;
        RandomAccessFile raf = new RandomAccessFile(filePath, "rws");
        raf.setLength(0);
        raf.close();

        // Restart the OSD (to clear the MetadataCache)
        osds[0].shutdown();
        osds[0] = new OSD(configs[0]);

        file = volume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));
        file.read(userCredentials, dataIn.getData(), 1, 0);

        file.close();
        volume.close();
        BufferPool.free(dataIn);
    }

    private void addReplicas(Volume volume, String fileName, int replicaNumber) throws IOException,
            InterruptedException {

        // Get a liste of suitable OSDs and ensure the requestes number of replicas can be added.
        List<String> osdUUIDs = volume.getSuitableOSDs(userCredentials, fileName, replicaNumber);
        assertTrue(osdUUIDs.size() >= replicaNumber);

        // Save the current number of Replicas.
        int currentReplicaNumber = volume.listReplicas(userCredentials, fileName).getReplicasCount();

        // Get the default replication flags.
        ReplicationPolicy rp = volume.getDefaultReplicationPolicy(userCredentials, "/");
        int repl_flags = rp.getFlags();

        // Add the required number of new replicas.
        for (int i = 0; i < replicaNumber; i++) {
            Replica replica = Replica.newBuilder().setStripingPolicy(defaultStripingPolicy)
                    .setReplicationFlags(repl_flags).addOsdUuids(osdUUIDs.get(i)).build();
            volume.addReplica(userCredentials, fileName, replica);
        }

        // Ensure the replicas have been added.
        assertEquals(currentReplicaNumber + replicaNumber, volume.listReplicas(userCredentials, fileName)
                .getReplicasCount());
    }

    private void readInvalidView(AdminFileHandle file) throws AddressToUUIDNotFoundException, IOException {
        // Prevent view renewal.
        int prevMaxViewRenewals = options.getMaxViewRenewals();
        options.setMaxViewRenewals(1);

        try {
            byte[] dataOut = new byte[1];
            file.read(userCredentials, dataOut, 1, 0);
            fail("InvalidViewException was expected.");
        } catch (InvalidViewException e) {
            // Everything is fine. The InvalidViewException is expected, because the client read with an outdated view.
        } finally {
            // Restore the settings.
            options.setMaxViewRenewals(prevMaxViewRenewals);
        }
    }

    private void readWithViewRenewal(AdminFileHandle file) throws AddressToUUIDNotFoundException, IOException {
        int prevMaxViewRenewals = options.getMaxViewRenewals();
        options.setMaxViewRenewals(2);

        try {
            byte[] dataOut = new byte[1];
            file.read(userCredentials, dataOut, 1, 0);
        } finally {
            // Restore the settings.
            options.setMaxViewRenewals(prevMaxViewRenewals);
        }
    }

    private SuspendableOSDRequestDispatcher[] replaceWithSuspendableOSDs(int start, int count) throws Exception {
        SuspendableOSDRequestDispatcher[] suspOSDs = new SuspendableOSDRequestDispatcher[count];

        for (int i = 0; i < count; i++) {
            osds[start + i].shutdown();
            osds[start + i] = null;

            suspOSDs[i] = new SuspendableOSDRequestDispatcher(configs[start + i]);
            suspOSDs[i].start();
        }

        return suspOSDs;
    }

    private void resetSuspendableOSDs(SuspendableOSDRequestDispatcher[] suspOSDs, int start) {
        for (int i = 0; i < suspOSDs.length; i++) {
            suspOSDs[i].shutdown();
            osds[start + i] = new OSD(configs[start + i]);
        }
    }

    private class SuspendableOSDRequestDispatcher extends OSDRequestDispatcher {
        private AtomicBoolean suspended;

        public SuspendableOSDRequestDispatcher(OSDConfig config) throws Exception {
            super(config);
            suspended = new AtomicBoolean(false);
        }

        @Override
        public void receiveRecord(RPCServerRequest rq) {
            if (suspended.get()) {
                // Drop the request.
                rq.freeBuffers();
            } else {
                super.receiveRecord(rq);
            }
        }

        public void suspend() {
            suspended.set(true);
        }

        public void resume() {
            suspended.set(false);
        }
    }
}
