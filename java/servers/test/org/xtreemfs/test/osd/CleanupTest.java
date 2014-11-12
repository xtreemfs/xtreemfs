/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.osd.storage.CleanupThread;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_get_resultsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_is_runningResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_statusResponse;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * @author bjko
 */
public class CleanupTest {
    @Rule
    public final TestRule   testLog = TestHelper.testLog;
    
    private TestEnvironment env;

    private static Auth     passwd;
    
    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        passwd = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD)
                .setAuthPasswd(AuthPassword.newBuilder().setPassword("")).build();
    }
    
    @AfterClass
    public static void shutdownTest() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        env = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.DIR_SERVICE, TestEnvironment.Services.MRC,
                TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.MRC_CLIENT,
                TestEnvironment.Services.OSD, TestEnvironment.Services.OSD_CLIENT, });
        env.start();
    }

    @After
    public void tearDown() {
        env.shutdown();
    }
    
    /**
     * Test the Cleanup function without any files on the OSD.
     * 
     * @throws Exception
     */
    @Test
    public void testCleanUpEmpty() throws Exception {
        // start the cleanUp Operation
        List<String> results = makeCleanup(false, false, false);
        
        for (String res : results) {
            Pattern p = Pattern.compile(CleanupThread.getRegex(CleanupThread.VOLUME_RESULT_FORMAT));
            Matcher m = p.matcher(res);
            int zombies = Integer.parseInt(m.group(2).trim());
            assertEquals(0, zombies);
            int files = Integer.parseInt(m.group(3).trim());
            assertEquals(0, files);
        }
    }
    
    /**
     * Test the Cleanup function with files without zombies on the OSD.
     * 
     * @throws Exception
     */
    @Test
    public void testCleanUpFilesWithoutZombies() throws Exception {
        setupTestVolume();
        
        // start the cleanUp Operation
        List<String> results = makeCleanup(false, false, false);
        
        for (String res : results) {
            Pattern p = Pattern.compile(CleanupThread.getRegex(CleanupThread.VOLUME_RESULT_FORMAT));
            Matcher m = p.matcher(res);
            assertTrue(m.matches());
            int zombies = Integer.parseInt(m.group(2).trim());
            assertEquals(0, zombies);
            int files = Integer.parseInt(m.group(3).trim());
            assertEquals(3, files);
        }
    }
    
    /**
     * Test the Cleanup function with files with 1 zombie on the OSD.
     * 
     * @throws Exception
     */
    @Test
    public void testCleanupFilesWithZombies() throws Exception {

        UserCredentials uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
        
        RPCResponse r = null;
        
        setupTestVolume();
        
        // test/test1 --> zombie
        r = env.getMrcClient().unlink(env.getMRCAddress(), RPCAuthentication.authNone, uc, "test", "test1");
        r.get();
        r.freeBuffers();
        
        List<String> results = makeCleanup(false, false, false);
        
        for (String res : results) {
            Pattern p = Pattern.compile(CleanupThread.getRegex(CleanupThread.VOLUME_RESULT_FORMAT));
            Matcher m = p.matcher(res);
            assertTrue(m.matches());
            int zombies = Integer.parseInt(m.group(2).trim());
            assertEquals(1, zombies);
            int files = Integer.parseInt(m.group(3).trim());
            assertEquals(3, files);
        }
    }
    
    /**
     * Test the Cleanup function with files with 1 zombie on the OSD, deleting
     * it.
     * 
     * @throws Exception
     */
    @Test
    public void testCleanupFilesWithZombieDelete() throws Exception {
        UserCredentials uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
        
        RPCResponse<?> r = null;
        
        setupTestVolume();
        
        // test/test1 --> zombie
        r = env.getMrcClient().unlink(env.getMRCAddress(), RPCAuthentication.authNone, uc, "test", "test1");
        r.get();
        r.freeBuffers();
        
        List<String> results = makeCleanup(false, false, true);
        
        for (String res : results) {
            Pattern p = Pattern.compile(CleanupThread.getRegex(CleanupThread.VOLUME_RESULT_FORMAT));
            Pattern pd = Pattern.compile(CleanupThread.getRegex(CleanupThread.ZOMBIES_DELETED_FORMAT));
            Matcher m = p.matcher(res);
            // its a zombie delete message
            if (!m.matches()) {
                m = pd.matcher(res);
                assertTrue(m.matches());
                int zombies = Integer.parseInt(m.group(1).trim());
                assertEquals(1, zombies);
                String volState = m.group(2);
                assertEquals("existing", volState);
            } else {
                int zombies = Integer.parseInt(m.group(2).trim());
                assertEquals(1, zombies);
                int files = Integer.parseInt(m.group(3).trim());
                assertEquals(3, files);
            }
        }
        
        // restart the cleanUp Operation on clean volume
        results = makeCleanup(false, false, false);
        
        for (String res : results) {
            Pattern p = Pattern.compile(CleanupThread.getRegex(CleanupThread.VOLUME_RESULT_FORMAT));
            Matcher m = p.matcher(res);
            assertTrue(m.matches());
            int zombies = Integer.parseInt(m.group(2).trim());
            assertEquals(0, zombies);
            int files = Integer.parseInt(m.group(3).trim());
            assertEquals(2, files);
        }
    }
    
    /**
     * Test the Cleanup function with files with 1 zombie on the OSD, restoring
     * it.
     * 
     * @throws Exception
     */
    @Test
    public void testCleanupFilesWithZombieRestore() throws Exception {
        UserCredentials uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
        
        RPCResponse<?> r = null;
        
        setupTestVolume();
        
        // test/test1 --> zombie
        r = env.getMrcClient().unlink(env.getMRCAddress(), RPCAuthentication.authNone, uc, "test", "test1");
        r.get();
        r.freeBuffers();
        
        List<String> results = makeCleanup(true, false, false);
        
        for (String res : results) {
            Pattern p = Pattern.compile(CleanupThread.getRegex(CleanupThread.VOLUME_RESULT_FORMAT));
            Pattern pr = Pattern.compile(CleanupThread.getRegex(CleanupThread.ZOMBIES_RESTORED_FORMAT));
            Matcher m = p.matcher(res);
            // its a zombie restore message
            if (!m.matches()) {
                m = pr.matcher(res);
                assertTrue(m.matches());
                int zombies = Integer.parseInt(m.group(1).trim());
                assertEquals(1, zombies);
            } else {
                int zombies = Integer.parseInt(m.group(2).trim());
                assertEquals(1, zombies);
                int files = Integer.parseInt(m.group(3).trim());
                assertEquals(3, files);
            }
        }
        
        // restart the cleanUp Operation on clean volume
        results = makeCleanup(false, false, false);
        
        for (String res : results) {
            Pattern p = Pattern.compile(CleanupThread.getRegex(CleanupThread.VOLUME_RESULT_FORMAT));
            Matcher m = p.matcher(res);
            assertTrue(m.matches());
            int zombies = Integer.parseInt(m.group(2).trim());
            assertEquals(0, zombies);
            int files = Integer.parseInt(m.group(3).trim());
            assertEquals(3, files);
        }
    }
    
    /**
     * Performs a cleanUp-Operation.<br>
     * Checks the status for errors.
     * 
     * @param restore
     * @param deleteVolumes
     * @param killZombies
     * @return the result of the cleanup operation.
     * @throws InterruptedException
     * @throws ONCRPCException
     * @throws IOException
     */
    private List<String> makeCleanup(boolean restore, boolean deleteVolumes, boolean killZombies)
        throws InterruptedException, IOException {
        String statF = CleanupThread.getRegex(CleanupThread.STATUS_FORMAT);
        String stopF = CleanupThread.getRegex(CleanupThread.STOPPED_FORMAT);
        assertNotNull(statF);
        assertNotNull(stopF);
        
        RPCResponse<?> r = null;
        
        // start the cleanUp Operation
        r = env.getOSDClient().xtreemfs_cleanup_start(env.getOSDAddress(),
                passwd, RPCAuthentication.userService,
                killZombies, deleteVolumes, restore, true, 0);
        r.get();
        r.freeBuffers();
        
        boolean isRunning = true;
        do {
            r = env.getOSDClient().xtreemfs_cleanup_status(env.getOSDAddress(),
                    passwd, RPCAuthentication.userService);
            xtreemfs_cleanup_statusResponse stat = (xtreemfs_cleanup_statusResponse) r.get();
            r.freeBuffers();
            
            assertNotNull(stat);
            try {
                if (stat.getStatus().matches(statF))
                    assertTrue(true);
            } catch (NullPointerException ne) {
                assertTrue(stat.getStatus().matches(stopF));
            }
            
            r = env.getOSDClient().xtreemfs_cleanup_is_running(env.getOSDAddress(),
                    passwd, RPCAuthentication.userService);
            isRunning = ((xtreemfs_cleanup_is_runningResponse)r.get()).getIsRunning();
            r.freeBuffers();
        } while (isRunning);
        
        r = env.getOSDClient().xtreemfs_cleanup_get_results(env.getOSDAddress(),
                passwd, RPCAuthentication.userService);
        List<String> results = ((xtreemfs_cleanup_get_resultsResponse) r.get()).getResultsList();
        r.freeBuffers();
        
        return results;
    }
    
    /**
     * Sets up a test-volume with 3 files.<br>
     * Volume: test<br>
     * Files:<br>
     * test/test1<br>
     * test/test2<br>
     * test/test3<br>
     * 
     * @throws Exception
     */
    private void setupTestVolume() throws Exception {
        UserCredentials uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
        
        StripingPolicy sp = SetupUtils.getStripingPolicy(1, 4);

        Client c = new Client(new InetSocketAddress[]{env.getDIRAddress()}, 15*1000, 5*60*1000, null);
        c.start();
        c.createVolume("test", RPCAuthentication.authNone, uc, sp, AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, 511);

        Volume v = c.getVolume("test", uc);

        File f = v.getFile("test1");
        RandomAccessFile raf = f.open("rw", 511);
        raf.write(new byte[1024 * 10], 0, 1024 * 10);
        raf.close();
        
        f = v.getFile("test2");
        raf = f.open("rw", 511);
        raf.write(new byte[1024 * 10], 0, 1024 * 10);
        raf.close();
        
        f = v.getFile("test3");
        raf = f.open("rw", 511);
        raf.write(new byte[1024 * 10], 0, 1024 * 10);
        raf.close();
    }
}