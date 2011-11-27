/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.common.libxtreemfs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 * 
 * <br>
 * Sep 3, 2011
 */
public class ClientTest extends TestCase {

    private DIRRequestDispatcher dir;

    private TestEnvironment      testEnv;

    private DIRConfig            dirConfig;

    private UserCredentials      userCredentials;

    private Auth                 auth = RPCAuthentication.authNone;

    private DIRClient            dirClient;

    /**
     * 
     */
    public ClientTest() throws IOException {
        dirConfig = SetupUtils.createDIRConfig();
        Logging.start(Logging.LEVEL_DEBUG);
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName());

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC, TestEnvironment.Services.MRC2, TestEnvironment.Services.OSD });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirClient = new DIRClient(new DIRServiceClient(testEnv.getRpcClient(), null),
                new InetSocketAddress[] { testEnv.getDIRAddress() }, 3, 1000);

    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();

        dir.shutdown();

        dir.waitForShutdown();
    }
    
    public void testCreateOpenRemoveListVolume() throws Exception {

        final String VOLUME_NAME_1 = "foobar";

        // TODO: Create pseudo commandline and parse it.
        Options options = new Options(5000, 10000, 4, 2);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();

        Client client = Client.createClient(dirAddress, userCredentials, null, options);
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
    
    public void testCreateOpenRemoveListVolumeMultipleMRCs() throws Exception {

        final String VOLUME_NAME_1 = "foobar";

        // TODO: Create pseudo commandline and parse it.
        Options options = new Options(5000, 10000, 4, 2);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();

        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();

        // Create MRC Address List
        String invalidMrcAddress = "ThereIsNoMRC.org:36592";
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
        String mrc2Address = testEnv.getMRC2Address().getHostName() + ":" + testEnv.getMRC2Address().getPort();
        List<String> mrcAddressList = new ArrayList<String>();
        mrcAddressList.add(invalidMrcAddress);
        mrcAddressList.add(mrcAddress);
        mrcAddressList.add(mrc2Address);

        // Create volume
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
        final String VOLUME_NAME_1 = "foobar";

        Options options = new Options(5000, 10000, 4, 2);
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        
        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = Client.createClient(dirAddress, userCredentials, null, options);
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
        ReusableBuffer buf = ReusableBuffer.wrap(data.getBytes());
        fileHandle.write(userCredentials, buf, buf.capacity(), 0);
         
        stat = volume.getAttr(userCredentials, "/bla.tzt");
        assertEquals(data.length(), stat.getSize());

        // Read from file.
        byte[] readData = new byte[data.length()];
        ReusableBuffer readBuf = ReusableBuffer.wrap(readData);
        int readCount = fileHandle.read(userCredentials, readBuf, data.length(), 0);

        assertEquals(data.length(), readCount);
        for (int i = 0; i < data.length(); i++) {
            assertEquals(readData[i], data.getBytes()[i]);
        }

        fileHandle.close();
        client.shutdown();
    }
}
