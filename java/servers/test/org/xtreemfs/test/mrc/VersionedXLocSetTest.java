/*
 * Copyright (c) 2012 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
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
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
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

    @BeforeClass
    public static void initializeTest() throws Exception {
        System.out.println("TEST: " + VersionedXLocSetTest.class.getSimpleName());

        // cleanup
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(Logging.LEVEL_WARN);

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

        client = ClientFactory.createAdminClient(dirAddress, userCredentials, null, options);
        client.start();

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

        // due to a bug in the OSDSelectionPolicys the numOSDs parameter is not working properly and mostly
        // returns all suitable OSDs
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
        // Thread.sleep(16 * 1000);

        // so we have to fall back to add the replicas individually
        for (int i = 0; i < replicaNumber; i++) {
            Replica replica = Replica.newBuilder().setStripingPolicy(defaultStripingPolicy)
                    .setReplicationFlags(repl_flags).addOsdUuids(osdUUIDs.get(i)).build();
            volume.addReplica(userCredentials, fileName, replica);

            // 15s is the default lease timeout
            // Thread.sleep(16 * 1000);
            System.out.println("Added replica on " + osdUUIDs.get(i));
        }
        
        assertEquals(currentReplicaNumber + replicaNumber, volume.listReplicas(userCredentials, fileName)
                .getReplicasCount());
    }

    private void removeReadOutdated(AdminVolume volume, String fileName) throws Exception {
        // open testfile and write some bytes not "0"
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();

        AdminFileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);

        int stripeSize = fileHandle.getStripingPolicy().getStripeSize();
        int count = stripeSize * 2;
        ReusableBuffer data = SetupUtils.generateData(count, (byte) 1);

        System.out.println("writing");

        fileHandle.write(userCredentials, data.createViewBuffer().getData(), count, 0);
        fileHandle.close();

        // wait until the file is written and probably replicated
        Thread.sleep(20 * 1000);

        System.out.println("openagain");

        // open the file again and wait until the primary replica is removed
        flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber();
        fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);

        // get the OSDs which are suitable for the file
        System.out.println(volume.getSuitableOSDs(userCredentials, fileName, NUM_OSDS));

        // get the primary replica
        List<Replica> replicas = fileHandle.getReplicasList();
        Replica replica = replicas.get(0);

        // since striping is disabled there should be only one OSD for this certain replica
        assertEquals(1, replica.getOsdUuidsCount());

        // remove the replica from the OSD
        volume.removeReplica(userCredentials, fileName, replica.getOsdUuids(0));

        // TODO(jdillmann): remove when the changeReplicaSet coordination is working
        // open the file again with another

        // wait until the file is actually deleted on the OSD
        Thread.sleep(70 * 1000);

        // reading a deleted file seems like reading a sparse file and should return "0" bytes
        // TODO(jdillmann): update assertions when versioned XLocSets are implemented
        byte[] data2 = new byte[1];
        fileHandle.read(userCredentials, data2, 1, 0);
        fileHandle.close();

        assert (data2[0] != (byte) 1);

        // get the OSDs which are suitable for the file
        System.out.println(volume.getSuitableOSDs(userCredentials, fileName, NUM_OSDS));
    }

    private void removeReadOutdated(String volumeName, String fileName) throws Exception {
        AdminVolume volume = client.openVolume(volumeName, null, options);
        removeReadOutdated(volume, fileName);
    }

    @Ignore
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

        removeReadOutdated(volume, fileName);
    }
    
    @Ignore
    @Test
    public void testWqRqRemoveReadOutdated() throws Exception {
        String volumeName = "testWqRqRemoveReadOutdated";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        int repl_flags = ReplicationFlags.setSequentialStrategy(0);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 3,
                repl_flags);

        removeReadOutdated(volume, fileName);
    }


    @Ignore
    @Test
    public void testRonlyInvalidViewOnAdd() throws Exception {
        String volumeName = "testRonlyInvalidViewOnAdd";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.close();

        // setup a full read only replica with sequential access strategy
        int repl_flags = ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialStrategy(0));
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 2,
                repl_flags);
        
        testInvalidViewOnAdd(volumeName, fileName);
    }

    @Test
    public void testWqRqInvalidViewOnAdd() throws Exception {
        String volumeName = "testWqRqInvalidViewOnAdd";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 2, 0);
        volume.close();

        testInvalidViewOnAdd(volumeName, fileName);
    }

    private void testInvalidViewOnAdd(String volumeName, String fileName) throws Exception {
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();

        // open outdated file handle and write some data
        AdminVolume outdatedVolume = client.openVolume(volumeName, null, options);
        AdminFileHandle outdatedFile = outdatedVolume.openFile(userCredentials, fileName, flags, 0777);

        System.out.println("writing");
        ReusableBuffer data = SetupUtils.generateData(256, (byte) 1);
        outdatedFile.write(userCredentials, data.createViewBuffer().getData(), 256, 0);
        outdatedFile.close();

        System.out.println("reopen file");
        flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber();
        outdatedFile = outdatedVolume.openFile(userCredentials, fileName, flags, 0777);

        // open the volume again and add some more replicas
        AdminVolume volume = client.openVolume(volumeName, null, options);

        System.out.println("adding replicas");
        addReplicas(volume, fileName, 1);

        volume.close();

        // read data with outdated xLocSet
        System.out.println("read with old version");
        PosixErrorException catched = null;
        try {
            byte[] dataOut = new byte[1];
            outdatedFile.read(userCredentials, dataOut, 1, 0);
        } catch (PosixErrorException e) {
            catched = e;
        } finally {
            outdatedFile.close();
            outdatedVolume.close();
        }

        // TODO(jdillmann): make this more dynamic
        assertTrue(catched != null);
        assertEquals(catched.getPosixError(), RPC.POSIXErrno.POSIX_ERROR_EAGAIN);
        assertTrue(catched.getMessage().contains("view is not valid"));
    }

}
