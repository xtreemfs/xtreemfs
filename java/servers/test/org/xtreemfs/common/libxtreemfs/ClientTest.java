/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class ClientTest {
    @Rule
    public final TestRule               testLog        = TestHelper.testLog;

    private static DIRRequestDispatcher dir;

    private static TestEnvironment      testEnv;

    private static DIRConfig            dirConfig;

    private static UserCredentials      userCredentials;

    private static Auth                 auth           = RPCAuthentication.authNone;

    private static DIRClient            dirClient;

    private static final int            NUMBER_OF_OSDS = 2;

    private static OSDConfig            osdConfigs[];

    private static OSD                  osds[];

    private static MRCConfig            mrc2Config;
    
    private static MRCRequestDispatcher mrc2;
    
    @BeforeClass
    public static void initializeTest() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        dirConfig = SetupUtils.createDIRConfig();
        osdConfigs = SetupUtils.createMultipleOSDConfigs(NUMBER_OF_OSDS);

        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirClient = new DIRClient(new DIRServiceClient(testEnv.getRpcClient(), null),
                new InetSocketAddress[] { testEnv.getDIRAddress() }, 3, 1000);

        // start second MRC
        mrc2Config = SetupUtils.createMRC2Config();
        mrc2 = new MRCRequestDispatcher(mrc2Config, SetupUtils.createMRC2dbsConfig());
        mrc2.startup();

        osds = new OSD[NUMBER_OF_OSDS];
        for (int i = 0; i < osds.length; i++) {
            osds[i] = new OSD(osdConfigs[i]);
        }
    }

    @AfterClass
    public static void shutdownTest() throws Exception {

        for (int i = 0; i < osds.length; i++) {
            if (osds[i] != null) {
                osds[i].shutdown();
            }
        }

        if (mrc2 != null) {
            mrc2.shutdown();
            mrc2 = null;
        }
        
        testEnv.shutdown();
        dir.shutdown();
        dir.waitForShutdown();
    }

    @Test
    public void testCreateOpenRemoveListVolume() throws Exception {

        final String VOLUME_NAME_1 = "testCreateOpenRemoveListVolume";

        Options options = new Options();

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();

        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();

        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        // Create volume
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);

        ServiceSet sSet = null;
        sSet = dirClient.xtreemfs_service_get_by_name(testEnv.getDIRAddress(), auth, userCredentials,
                VOLUME_NAME_1);

        assertEquals(1, sSet.getServicesCount());
        assertEquals(VOLUME_NAME_1, sSet.getServices(0).getName());

        // List volumes
        Volumes volumes = client.listVolumes(mrcAddress);
        assertEquals(1, volumes.getVolumesCount());
        assertEquals(VOLUME_NAME_1, volumes.getVolumes(0).getName());

        // Delete volume
        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);

        sSet = null;
        sSet = dirClient.xtreemfs_service_get_by_name(testEnv.getDIRAddress(), auth, userCredentials,
                VOLUME_NAME_1);

        assertEquals(0, sSet.getServicesCount());

        // List volumes
        volumes = client.listVolumes(mrcAddress);
        assertEquals(0, volumes.getVolumesCount());

        // shutdown the client
        client.shutdown();
    }

    @Test
    public void testCreateOpenRemoveListVolumeMultipleMRCs() throws Exception {

        final String VOLUME_NAME_1 = "testCreateOpenRemoveListVolumeMultipleMRCs";

        Options options = new Options();

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();

        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();

        // Create MRC Address List
        String invalidMrcAddress = "ThereIsNoMRC.org:36592";
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
        String mrc2Address = mrc2Config.getHostName() + ":" + mrc2Config.getPort();
        List<String> mrcAddressList = new ArrayList<String>();
        mrcAddressList.add(invalidMrcAddress);
        mrcAddressList.add(mrcAddress);
        mrcAddressList.add(mrc2Address);

        // Create volume
        Volumes volumesBeforeCreate = client.listVolumes(mrcAddressList);
        assertEquals(0, volumesBeforeCreate.getVolumesCount());

        client.createVolume(mrcAddressList, auth, userCredentials, VOLUME_NAME_1);

        ServiceSet sSet = null;
        sSet = dirClient.xtreemfs_service_get_by_name(testEnv.getDIRAddress(), auth, userCredentials,
                VOLUME_NAME_1);

        assertEquals(1, sSet.getServicesCount());
        assertEquals(VOLUME_NAME_1, sSet.getServices(0).getName());

        // List volumes
        Volumes volumes = client.listVolumes(mrcAddressList);
        assertEquals(1, volumes.getVolumesCount());
        assertEquals(VOLUME_NAME_1, volumes.getVolumes(0).getName());

        // Delete volume
        client.deleteVolume(mrcAddressList, auth, userCredentials, VOLUME_NAME_1);

        sSet = null;
        sSet = dirClient.xtreemfs_service_get_by_name(testEnv.getDIRAddress(), auth, userCredentials,
                VOLUME_NAME_1);
        assertEquals(0, sSet.getServicesCount());

        // List volumes
        volumes = client.listVolumes(mrcAddressList);
        assertEquals(0, volumes.getVolumesCount());

        // shutdown the client
        client.shutdown();
    }

    @Test
    public void testMinimalExample() throws Exception {
        final String VOLUME_NAME_1 = "testMinimalExample";

        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
        Volume volume = client.openVolume(VOLUME_NAME_1, null, options);

        // Open a file.
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                "/bla.tzt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        // Get file attributes
        Stat stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(0, stat.getSize());

        // Write to file.
        String data = "Need a testfile? Why not (\\|)(+,,,+)(|/)?";
        fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);

        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(data.length(), stat.getSize());

        // Read from file.
        byte[] readData = new byte[data.length()];
        int readCount = fileHandle.read(userCredentials, readData, data.length(), 0);

        assertEquals(data.length(), readCount);
        for (int i = 0; i < data.length(); i++) {
            assertEquals(readData[i], data.getBytes()[i]);
        }

        fileHandle.close();
        volume.close();

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
        client.shutdown();
    }

    @Test
    public void testMinimalExampleWithAsyncWrites() throws Exception {
        final String VOLUME_NAME_1 = "testMinimalExampleWithAsyncWrites";

        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        // maxwriteAhead != 0 enables async writes as long as file isn't opened
        // with O_SYNC
        options.setEnableAsyncWrites(true);
        options.setMaxWriteAhead(1024);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();

        // Open a volume named "foobar".
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
        Volume volume = client.openVolume(VOLUME_NAME_1, null, options);

        // Open a file.
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                "/bla.tzt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        // Get file attributes
        Stat stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(0, stat.getSize());

        // Write to file.
        String data = "Need a testfile? Why not (\\|)(+,,,+)(|/)?";
        fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);

        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(data.length(), stat.getSize());

        // Read from file.
        byte[] readData = new byte[data.length()];
        int readCount = fileHandle.read(userCredentials, readData, data.length(), 0);

        assertEquals(data.length(), readCount);
        for (int i = 0; i < data.length(); i++) {
            assertEquals(readData[i], data.getBytes()[i]);
        }

        fileHandle.close();
        volume.close();

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
        client.shutdown();
    }

    @Test
    public void testMinimalExampleWithAsyncWritesAndStriping() throws Exception {
        final String VOLUME_NAME_1 = "testMinimalExampleWithAsyncWritesAndStriping";

        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        // maxwriteAhead != 0 enables async writes as long as file isn't opened
        // with O_SYNC
        options.setMaxWriteAhead(1024);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = ClientFactory.createClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();

        // Open a volume named "foobar".
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1, 0777,
                userCredentials.getUsername(), userCredentials.getGroupsList().get(0),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, StripingPolicyType.STRIPING_POLICY_RAID0,
                4, 2, new ArrayList<KeyValuePair>());
        Volume volume = client.openVolume(VOLUME_NAME_1, null, options);

        // Open a file.
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                "/bla.tzt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        // Get file attributes
        Stat stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(0, stat.getSize());

        // Write to file.
        String data = "Need a testfile? Why not (\\|)(+,,,+)(|/)?";
        fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);

        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(data.length(), stat.getSize());

        // Read from file.
        byte[] readData = new byte[data.length()];
        int readCount = fileHandle.read(userCredentials, readData, data.length(), 0);

        assertEquals(data.length(), readCount);
        for (int i = 0; i < data.length(); i++) {
            assertEquals(readData[i], data.getBytes()[i]);
        }

        fileHandle.close();
        volume.close();

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
        client.shutdown();
    }
}
