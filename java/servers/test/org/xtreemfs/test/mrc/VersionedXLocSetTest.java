/*
 * Copyright (c) 2012 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertEquals;

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
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
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

    private void addReplicas(Volume volume, String fileName, int replicaNumber) throws IOException {

        // due to a bug in the OSDSelectionPolicys the numOSDs parameter is not working properly and mostly
        // returns all suitable OSDs
        List<String> osdUUIDs = volume.getSuitableOSDs(userCredentials, fileName, replicaNumber);
        assert (osdUUIDs.size() >= replicaNumber);

        // get Replication Flags
        // copied from org.xtreemfs.common.libxtreemfs.VolumeImplementation.listACL(UserCredentials, String)
        // TODO (jdillmann): move to VolumeImplementation.getDefaultReplicationPolicy ?
        int repl_flags;

        try {
            String rpAsJSON = volume.getXAttr(userCredentials, "/", "xtreemfs.default_rp");
            Map<String, Object> rp = (Map<String, Object>) JSONParser.parseJSON(new JSONString(rpAsJSON));
            repl_flags = (int) ((long) rp.get("replication-flags"));
        } catch (JSONException e) {
            throw new IOException(e);
        }

        // for some reason addAllOsdUuids wont work and we have to add them individually
        // Replica replica = Replica.newBuilder().addAllOsdUuids(osdUUIDs.subList(0, newReplicaNumber))
        // .setStripingPolicy(defaultStripingPolicy).setReplicationFlags(repl_flags).build();
        // volume.addReplica(userCredentials, fileName, replica);

        // so we have to fall back to add the replicas individually
        for (int i = 0; i < replicaNumber; i++) {
            Replica replica = Replica.newBuilder().setStripingPolicy(defaultStripingPolicy)
                    .setReplicationFlags(repl_flags).addOsdUuids(osdUUIDs.get(i)).build();
            volume.addReplica(userCredentials, fileName, replica);
            System.out.println("Added replica on " + osdUUIDs.get(i));
        }

    }

    private void readOutdated(AdminVolume volume, String fileName) throws Exception {
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
        Thread.sleep(5000);

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

        // TODO (jdillmann): remove when the changeReplicaSet cooridnation is working
        // open the file again with another

        // wait until the file is actually deleted on the OSD
        Thread.sleep(70000);

        // reading a deleted file seems like reading a sparse file and should return "0" bytes
        // TODO (jdillmann): update assertions when versioned XLocSets are implemented
        byte[] data2 = new byte[1];
        fileHandle.read(userCredentials, data2, 1, 0);
        fileHandle.close();

        assert (data2[0] != (byte) 1);

        // get the OSDs which are suitable for the file
        System.out.println(volume.getSuitableOSDs(userCredentials, fileName, NUM_OSDS));
    }

    private void readOutdated(String volumeName, String fileName) throws Exception {
        AdminVolume volume = client.openVolume(volumeName, null, options);
        readOutdated(volume, fileName);
    }

    @Ignore
    @Test
    public void testRonlyReadOutdated() throws Exception {
        String volumeName = "testRonlyReadOutdated";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        // setup a full read only replica with sequential access strategy
        int repl_flags = ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialStrategy(0));
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, 2,
                repl_flags);

        readOutdated(volume, fileName);
    }
    
    @Test
    public void testWqRqReadOutdated() throws Exception {
        String volumeName = "testWqRqReadOutdated";
        String fileName = "/testfile";

        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);

        int repl_flags = ReplicationFlags.setSequentialStrategy(0);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 3,
                repl_flags);

        readOutdated(volume, fileName);
    }

    // @Test
    // public void testWqRqOutdatedAdd() throws Exception {
    // String volumeName = "testWqRqOutdatedAdd";
    // String fileName = "/testfile";
    //
    // // client.createVolume(mrcAddress, auth, userCredentials, volumeName, 0,
    // // userCredentials.getUsername(),
    // // userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
    // // StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
    // // defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());
    // client.createVolume(mrcAddress, auth, userCredentials, volumeName);
    // AdminVolume volume = client.openVolume(volumeName, null, options);
    //
    // int replicaNumber = 3;
    // int repl_flags = ReplicationFlags.setSequentialStrategy(0);
    //
    // // addReplica with WQRQ is not working for some reasons i don't understand
    // volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ,
    // replicaNumber, repl_flags);
    //
    // // volume.setDefaultReplicationPolicy(userCredentials, "/",
    // // ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE,
    // // replicaNumber, repl_flags);
    //
    // // open testfile and write some bytes not "0"
    // int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
    // | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
    // AdminFileHandle fileHandle = volume.openFile(userCredentials, fileName, flags, 0777);
    //
    // int stripeSize = fileHandle.getStripingPolicy().getStripeSize();
    // int count = stripeSize * 2;
    // ReusableBuffer data = SetupUtils.generateData(count, (byte) 1);
    //
    // fileHandle.write(userCredentials, data.createViewBuffer().getData(), count, 0);
    // fileHandle.close();
    //
    // // wait some time to allow the file to replicate
    // // Thread.sleep(15 * 1000);
    //
    //
    // // add some more replicas
    // int newReplicaNumber = 1;
    // addReplicas(volume, fileName, newReplicaNumber);
    //
    // // wait for flease
    // // Thread.sleep(20 * 1000);
    //
    // assertEquals(replicaNumber + newReplicaNumber, volume.listReplicas(userCredentials, fileName)
    // .getReplicasCount());
    //
    // // fileHandle = volume.openFile(userCredentials, fileName, flags);
    // // byte[] data2 = new byte[1];
    // // fileHandle.read(userCredentials, data2, 1, 0);
    // // fileHandle.close();
    // // System.out.println(data2);
    //
    // System.out.println("wait");
    // }
    //
    // @Test
    // public void testWqRqOutdatedRemove() throws Exception {
    // String volumeName = "testWqRqOutdatedRemove";
    // String path = "/testfile";
    //
    // // client.createVolume(mrcAddress, auth, userCredentials, volumeName, 0,
    // // userCredentials.getUsername(),
    // // userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
    // // StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
    // // defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());
    // client.createVolume(mrcAddress, auth, userCredentials, volumeName);
    // AdminVolume volume = client.openVolume(volumeName, null, options);
    //
    // int repl_flags = ReplicationFlags.setSequentialStrategy(0);
    // volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ,
    // NUM_OSDS,
    // repl_flags);
    //
    // AdminFileHandle fileHandle = volume.openFile(userCredentials, path,
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber(), 0777);
    //
    // assert (fileHandle.getReplicasList().size() == NUM_OSDS);
    //
    // // write "1" bytes to the file
    // int stripeSize = fileHandle.getStripingPolicy().getStripeSize();
    // int count = stripeSize * 2;
    // ReusableBuffer data = SetupUtils.generateData(count, (byte) 1);
    // byte[] dataout = new byte[count];
    //
    // fileHandle.write(userCredentials, data.createViewBuffer().getData(), count, 0);
    // fileHandle.close();
    //
    // assert (volume.getSuitableOSDs(userCredentials, path, NUM_OSDS).size() == 0);
    //
    // // open a FileHandle which should be outdated
    // System.out.println("open outdated file handle");
    // AdminClient outdatedClient = ClientFactory.createAdminClient(dirAddress, userCredentials, null,
    // options);
    // outdatedClient.start();
    // AdminVolume outdatedVolume = outdatedClient.openVolume(volumeName, null, options);
    // AdminFileHandle outdatedFileHandle = outdatedVolume.openFile(userCredentials, path,
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
    //
    //
    // // remove replicas
    // System.out.println("removing replicas");
    // fileHandle = volume.openFile(userCredentials, path,
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
    // List<Replica> replicas = fileHandle.getReplicasList();
    // fileHandle.close();
    //
    // for (int i = 0; i<3; i++) {
    // volume.removeReplica(userCredentials, path, replicas.get(i).getOsdUuids(0));
    // }
    //
    // assert (volume.getSuitableOSDs(userCredentials, path, NUM_OSDS).size() == 3);
    //
    // Thread.sleep(70 * 1000);
    //
    // System.out.println("open the file with the new xlocset");
    // // write something new => for example two
    // fileHandle = volume.openFile(userCredentials, path,
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());
    // assert (fileHandle.getReplicasList().size() == 2);
    // System.out.println(fileHandle.getReplicasList());
    //
    // data = SetupUtils.generateData(count, (byte) 2);
    // fileHandle.write(userCredentials, data.createViewBuffer().getData(), count, 0);
    //
    // fileHandle.read(userCredentials, dataout, count, 0);
    // fileHandle.close();
    // assert (dataout[0] == (byte) 2);
    //
    // outdatedFileHandle.read(userCredentials, dataout, count, 0);
    // outdatedFileHandle.close();
    // assert (dataout[0] == (byte) 2);
    // }

}
