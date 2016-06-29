/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class NativeTest {
    @Rule
    public final TestRule          testLog = TestHelper.testLog;

    private static TestEnvironment testEnv;

    private static UserCredentials userCredentials;

    private static Auth            auth    = RPCAuthentication.authNone;

    private static String          dirAddress;
    private static String          mrcAddress;

    @BeforeClass
    public static void initializeTest() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.MRC, TestEnvironment.Services.OSD, });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
    }

    @AfterClass
    public static void shutdownTest() throws Exception {
        testEnv.shutdown();
    }

    @Test
    public void testMinimalExample() throws Exception {
        final String VOLUME_NAME = "testMinimalExample";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // Open a file.
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                "/bla.tzt",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(),
                0777);

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

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    @Ignore("TestEnv is not started with SSL support")
    public void testSSL() throws Exception {
        SSLOptions sslOptions = SetupUtils.createClientSSLOptions();

        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, sslOptions, options);
        client.start();

        client.createVolume(mrcAddress, auth, userCredentials, "testSSLVolume");

        String[] volumes = client.listVolumeNames();
        assertTrue(Arrays.asList(volumes).contains("testSSLVolume"));

        client.shutdown();
    }
}
