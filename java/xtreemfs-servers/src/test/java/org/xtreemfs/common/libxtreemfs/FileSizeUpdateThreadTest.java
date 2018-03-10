/*
 * Copyright (c) 2012 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestHelper;

/**
 * Tests if FileSizeUpdateThread works correctly.
 */
public class FileSizeUpdateThreadTest {
    @Rule
    public final TestRule               testLog = TestHelper.testLog;

    private static DIRRequestDispatcher dir;

    private static TestEnvironment      testEnv;

    private static DIRConfig            dirConfig;

    private static UserCredentials      userCredentials;

    private static Auth                 auth = RPCAuthentication.authNone;

    private static MRCServiceClient     mrcClient;

    @BeforeClass
    public static void initializeTest() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        dirConfig = SetupUtils.createDIRConfig();

        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC, TestEnvironment.Services.OSD });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        mrcClient = new MRCServiceClient(testEnv.getRpcClient(), null);

    }

    @AfterClass
    public static void shutdownTest() throws Exception {

        testEnv.shutdown();
        dir.shutdown();
        dir.waitForShutdown();
    }

    @Test
    public void testFileSizeRenewal() throws Exception {

        final String VOLUME_NAME_1 = "testFileSizeRenewal";

        Options options = new Options();
        options.setPeriodicFileSizeUpdatesIntervalS(10);
        options.setMetadataCacheSize(0);

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        Client client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
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

        // MRC shouldn't know about filesize yet
        stat = mrcClient
                .getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME_1, "/bla.tzt", 0).get()
                .getStbuf();
        assertEquals(0, stat.getSize());

        Thread.sleep(10000);

        // Now the thread should have updated the filesize at MRC
        stat = mrcClient
                .getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME_1, "/bla.tzt", 0).get()
                .getStbuf();
        assertEquals(data.length(), stat.getSize());

        fileHandle.close();
        client.shutdown();
    }
}
