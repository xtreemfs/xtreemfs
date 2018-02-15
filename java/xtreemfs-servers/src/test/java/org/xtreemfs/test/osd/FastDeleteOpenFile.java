/*
 * Copyright (c) 2013 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.osd;

import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;


/**
 * Write a file, delete it and check if it was deleted on disk as well.
 * 
 * This test ensures that the file is explicitly closed upon deletion in the OSD. If not, the deletion of the
 * file would be delayed until the file is implicitly deleted (by default after 60 seconds).
 * 
 * @author mberlin
 * 
 */
public class FastDeleteOpenFile {
    @Rule
    public final TestRule               testLog = TestHelper.testLog;

    private static TestEnvironment      testEnv;

    private static UserCredentials      userCredentials;

    private static Auth                 auth        = RPCAuthentication.authNone;

    private static String               mrcAddress;

    private static String               dirAddress;

    private static Options              options;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE, TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD});
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        options = new Options();
    }

    @AfterClass
    public static void shutdownTest() throws Exception {
        testEnv.shutdown();
    }

    @Test
    public void testFileOpenAtOSDAndImmediateDelete() throws Exception {
        final String volumeName = "testFileOpenAtOSDAndImmediateDelete";
        final String path = "/test.txt";
        String globalFileId = null;

        // Create client.
        AdminClient client = ClientFactory.createAdminClient(ClientType.JAVA, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        AdminVolume volume = client.openVolume(volumeName, null, options);
        volume.start();

        // Open FileHandle and remember its global file id.
        AdminFileHandle fileHandle = volume.openFile(
                userCredentials,
                path,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);
        globalFileId = fileHandle.getGlobalFileId();

        // Write some content.
        String content = "";
        for (int i = 0; i < 128 * 1024 / 12; i++) {
            content = content.concat("Hello World ");
        }
        byte[] bytesIn = content.getBytes();
        int length = bytesIn.length;
        fileHandle.write(userCredentials, bytesIn, length, 0);
        fileHandle.close();


        // Delete file.
        volume.unlink(userCredentials, path);

        // Make sure the file is deleted on disk.
        HashStorageLayout hsl = new HashStorageLayout(SetupUtils.createOSD1Config(), new MetadataCache());
        String pathOnDisk = hsl.generateAbsoluteFilePath(globalFileId);

        // The file is deleted asynchronously by the DeletionStage in the OSD. Give it some time.
        Thread.sleep(1 * 1000);
        assertFalse(new File(pathOnDisk).isDirectory());

        // Cleanup.
        volume.close();
        client.shutdown();
    }
}