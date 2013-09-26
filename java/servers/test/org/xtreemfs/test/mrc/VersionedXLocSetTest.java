/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT;
import static org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY;
import static org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Helper;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class VersionedXLocSetTest {
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

    // private static Long OFT_CLEAN_INTERVAL_MS;
    private static int             LEASE_TIMEOUT_MS;

    @BeforeClass
    public static void initializeTest() throws Exception {
        System.out.println("TEST: " + VersionedXLocSetTest.class.getSimpleName());

        // cleanup
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(Logging.LEVEL_WARN);
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

        client = ClientFactory.createAdminClient(dirAddress, userCredentials, null, options);
        client.start();

        // get the current timeouts to save time spend waiting
        // OFT_CLEAN_INTERVAL_MS = (Long) PA.getValue(osds[0].getDispatcher().getPreprocStage(), "OFT_CLEAN_INTERVAL");
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

    private void addReplicas(Volume volume, String fileName, int replicaNumber) throws IOException,
            InterruptedException {


        List<String> osdUUIDs = volume.getSuitableOSDs(userCredentials, fileName, replicaNumber);
        assert (osdUUIDs.size() >= replicaNumber);

        // save the current Replica Number
        int currentReplicaNumber = volume.listReplicas(userCredentials, fileName).getReplicasCount();

        // get Replication Flags
        // copied from org.xtreemfs.common.libxtreemfs.VolumeImplementation.listACL(UserCredentials, String)
        // TODO(jdillmann): move to VolumeImplementation.getDefaultReplicationPolicy ?
        int repl_flags;

        try {
            String rpAsJSON = volume.getXAttr(userCredentials, "/", "xtreemfs.default_rp");
            Map<String, Object> rp = (Map<String, Object>) JSONParser.parseJSON(new JSONString(rpAsJSON));
            long temp = ((Long) rp.get("replication-flags"));
            repl_flags = (int) temp;
        } catch (JSONException e) {
            throw new IOException(e);
        }

        // 15s is the default lease timeout
        // we have to wait that long, because addReplica calls ping which requires do become primary
        // Thread.sleep(LEASE_TIMEOUT_MS + 500);

        // so we have to fall back to add the replicas individually
        for (int i = 0; i < replicaNumber; i++) {
            Replica replica = Replica.newBuilder().setStripingPolicy(defaultStripingPolicy)
                    .setReplicationFlags(repl_flags).addOsdUuids(osdUUIDs.get(i)).build();
            volume.addReplica(userCredentials, fileName, replica);

            // 15s is the default lease timeout
            // Thread.sleep(LEASE_TIMEOUT_MS + 500);
            System.out.println("Added replica on " + osdUUIDs.get(i));
        }

        assertEquals(currentReplicaNumber + replicaNumber, volume.listReplicas(userCredentials, fileName)
                .getReplicasCount());
    }

    private static void readInvalidView(AdminFileHandle file) throws AddressToUUIDNotFoundException, IOException {
        PosixErrorException catched = null;
        try {
            byte[] dataOut = new byte[1];
            file.read(userCredentials, dataOut, 1, 0);
        } catch (PosixErrorException e) {
            catched = e;
        }

        // TODO(jdillmann): make this more dynamic
        assertTrue(catched != null);
        assertEquals(catched.getPosixError(), RPC.POSIXErrno.POSIX_ERROR_EAGAIN);
        assertTrue(catched.getMessage().contains("view is not valid"));
    }

    @Test
    public void testRonlyRemoveReadOutdated() throws Exception {
        String volumeName = "testRonlyRemoveReadOutdated";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        // setup a full read only replica with sequential access strategy
        int repl_flags = ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialStrategy(0));
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 2,
                repl_flags);

        volume.close();

        removeReadOutdated(volumeName, fileName);
    }

    @Test
    public void testWqRqRemoveReadOutdated() throws Exception {
        String volumeName = "testWqRqRemoveReadOutdated";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 3, 0);
        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));
        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));

        volume.close();

        removeReadOutdated(volumeName, fileName);
    }

    private void removeReadOutdated(String volumeName, String fileName) throws Exception {
        AdminVolume volume = client.openVolume(volumeName, null, options);

        // open testfile and write some bytes not "0"
        AdminFileHandle fileHandle = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_CREAT, SYSTEM_V_FCNTL_H_O_RDWR), 0777);

        int count = 256 * 1024;
        ReusableBuffer data = SetupUtils.generateData(count, (byte) 1);

        System.out.println("writing");

        fileHandle.write(userCredentials, data.createViewBuffer().getData(), count, 0);
        fileHandle.close();

        // wait until the file is written and probably replicated
        Thread.sleep(20 * 1000);

        System.out.println("openagain");

        // open the file again and wait until the primary replica is removed
        fileHandle = volume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));

        // get the primary replica
        List<Replica> replicas = fileHandle.getReplicasList();
        System.out.println(replicas);
        Replica replica = replicas.get(0);
        int prevReplicaCount = fileHandle.getReplicasList().size();

        System.out.println("Remove replica: " + replica.getOsdUuids(0));

        // since striping is disabled there should be only one OSD for this certain replica
        assertEquals(1, replica.getOsdUuidsCount());

        // use another volume remove the replica from the OSD
        AdminVolume controlVolume = client.openVolume(volumeName, null, options);
        controlVolume.removeReplica(userCredentials, fileName, replica.getOsdUuids(0));
        AdminFileHandle controlFile = controlVolume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));

        assertEquals(controlFile.getReplicasList().size(), prevReplicaCount - 1);

        System.out.println(controlFile.getReplicasList());

        controlFile.close();
        controlVolume.close();

        // wait until the file is actually deleted on the OSD
        System.out.println("wait until the file is closed and deleted on the OSD");
        Thread.sleep(70 * 1000);

        // Reading the the file should result in a invalid view.
        System.out.println("Read with old version");
        readInvalidView(fileHandle);

        fileHandle.close();
        volume.close();
    }

    @Test
    public void testRonlyAddReadOutdated() throws Exception {
        String volumeName = "testRonlyAddReadOutdated";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));

        // setup a full read only replica with sequential access strategy
        int repl_flags = ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialStrategy(0));
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 2,
                repl_flags);
        volume.close();
        
        addReadOutdated(volumeName, fileName);
    }

    @Test
    public void testWqRqAddReadOutdated() throws Exception {
        String volumeName = "testWqRqAddReadOutdated";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 2, 0);
        volume.close();

        addReadOutdated(volumeName, fileName);
    }

    private void addReadOutdated(String volumeName, String fileName) throws Exception {
        // open outdated file handle and write some data
        AdminVolume outdatedVolume = client.openVolume(volumeName, null, options);
        AdminFileHandle outdatedFile = outdatedVolume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_CREAT, SYSTEM_V_FCNTL_H_O_RDWR), 0777);

        System.out.println("writing");
        ReusableBuffer data = SetupUtils.generateData(256, (byte) 1);
        outdatedFile.write(userCredentials, data.createViewBuffer().getData(), 256, 0);
        outdatedFile.close();

        System.out.println("reopen file");
        outdatedFile = outdatedVolume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));

        // open the volume again and add some more replicas
        AdminVolume volume = client.openVolume(volumeName, null, options);

        System.out.println("adding replicas");
        addReplicas(volume, fileName, 1);

        volume.close();

        // read data with outdated xLocSet
        System.out.println("read with old version");
        readInvalidView(outdatedFile);

        outdatedFile.close();
        outdatedVolume.close();
    }

    /**
     * This test covers the case, that a number of replicas is added which will form a new majority. It has to be
     * ensured that the correct data is returned even if only new replicas are accessed.
     * 
     * @throws Exception
     */
    @Test
    public void testAddMajority() throws Exception {
        String volumeName = "testAddMajority";
        String fileName = "/testfile";

        SuspendableOSDRequestDispatcher[] suspOSDs = replaceWithSuspendableOSDs(0, 2);

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);

        System.out.println("open");
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 2, 0);
        volume.setOSDSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));
        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));

        System.out.println("write");
        AdminFileHandle file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDWR, SYSTEM_V_FCNTL_H_O_CREAT), 0777);
        ReusableBuffer dataIn = SetupUtils.generateData(256 * 1024, (byte) 1);
        file.write(userCredentials, dataIn.getData(), 256 * 1024, 0);
        dataIn.clear();
        file.close();

        System.out.println("add replicas");
        addReplicas(volume, fileName, 3);
        
        System.out.println("ensure lease timed out");
        Thread.sleep(LEASE_TIMEOUT_MS + 500);

        // reverse the selection policy
        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] {
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID,
                        OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_REVERSE }));

        // suspend the first two OSDs
        for (int i = 0; i < 2; i++) {
            suspOSDs[i].suspended.set(true);
        }

        System.out.println("read");
        file = volume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDWR));
        byte[] dataOut = new byte[1];
        file.read(userCredentials, dataOut, 1, 0);
        file.close();
        volume.close();

        // resume the first two OSDs
        for (int i = 0; i < 2; i++) {
            suspOSDs[i].suspended.set(false);
        }
        resetSuspendableOSDs(suspOSDs, 0);

        assertEquals(dataOut[0], (byte) 1);
    }

    /**
     * This test covers the removal of replicas forming a majority. It has to be ensured that the remaining replicas are
     * up to date and the correct (last written) data is returned.
     * 
     * @throws Exception
     */
    @Test
    public void testRemoveMajority() throws Exception {
        String volumeName = "testRemoveMajority";
        String fileName = "/testfile";

        SuspendableOSDRequestDispatcher[] suspOSDs = replaceWithSuspendableOSDs(0, 2);

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);

        System.out.println("open");
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 5, 0);
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

        // suspend the first two OSDs
        for (int i = 0; i < 2; i++) {
            suspOSDs[i].suspended.set(true);
        }

        System.out.println("write");
        AdminFileHandle file = volume.openFile(userCredentials, fileName,
                Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDWR, SYSTEM_V_FCNTL_H_O_CREAT), 0777);
        ReusableBuffer dataIn = SetupUtils.generateData(256 * 1024, (byte) 1);
        file.write(userCredentials, dataIn.getData(), 256 * 1024, 0);
        dataIn.clear();

        // start the OSDs again
        for (int i = 0; i < 2; i++) {
            suspOSDs[i].suspended.set(false);
        }

        System.out.println("remove replicas");
        List<Replica> replicas = file.getReplicasList();
        for (int i = 0; i < 3; i++) {
            Replica replica = replicas.get(i);

            // since striping is disabled there should be only one OSD for this certain replica
            assertEquals(1, replica.getOsdUuidsCount());
            volume.removeReplica(userCredentials, fileName, replica.getOsdUuids(0));
        }
        file.close();

        // revert the selection policy
        volume.setReplicaSelectionPolicy(
                userCredentials,
                Helper.policiesToString(new OSDSelectionPolicyType[] { OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID }));
        
        // reopen the file
        System.out.println("read");
        file = volume.openFile(userCredentials, fileName, Helper.flagsToInt(SYSTEM_V_FCNTL_H_O_RDONLY));
        
        byte[] dataOut = new byte[1];
        file.read(userCredentials, dataOut, 1, 0);
        file.close();
        volume.close();

        resetSuspendableOSDs(suspOSDs, 0);

        assertEquals(dataOut[0], (byte) 1);
    }
    
    
    /**
     * This test covers the functionality in case of a network partition when not more then n+1/2 replicas are
     * available. The majority has to be established both in the old and in the new replica set.
     */
    @Test
    public void testPartition() throws Exception {
        String volumeName = "testPartition";
        String fileName = "/testfile";
        
        // Make the first OSD suspendable
        SuspendableOSDRequestDispatcher[] suspOSDs = replaceWithSuspendableOSDs(0, 1);

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);

        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 4, 0);
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
        suspOSDs[0].suspended.set(true);

        // Add another replica.
        addReplicas(volume, fileName, 1);

        // Get the replica recently added from the replica list and remove it.
        // Since a majority in both, the new and the old set, has to be ensured, only one osd can be suspended.
        List<Replica> replicas = file.getReplicasList();
        Replica replica = replicas.get(0);
        volume.removeReplica(userCredentials, fileName, replica.getOsdUuids(0));

        file.close();
        volume.close();

        resetSuspendableOSDs(suspOSDs, 0);
    }

    private SuspendableOSDRequestDispatcher[] replaceWithSuspendableOSDs(int start, int count) throws Exception {
        SuspendableOSDRequestDispatcher[] suspOSDs = new SuspendableOSDRequestDispatcher[count];
        for (int i = start; i < start + count; i++) {
            osds[i].shutdown();
            osds[i] = null;

            suspOSDs[i] = new SuspendableOSDRequestDispatcher(configs[i]);
            suspOSDs[i].start();
        }

        return suspOSDs;
    }

    private void resetSuspendableOSDs(SuspendableOSDRequestDispatcher[] suspOSDs, int start) {
        for (int i = start; i < suspOSDs.length; i++) {
            suspOSDs[i].shutdown();
            suspOSDs[i] = null;

            osds[i] = new OSD(configs[i]);
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
    }

}
