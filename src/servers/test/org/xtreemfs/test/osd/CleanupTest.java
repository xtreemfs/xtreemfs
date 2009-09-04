/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.osd;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.storage.CleanupThread;
import org.xtreemfs.test.TestEnvironment;

/**
 * 
 * @author bjko
 */
public class CleanupTest extends TestCase {
    
    private TestEnvironment env;
    
    public CleanupTest() {
        super();
        Logging.start(Logging.LEVEL_ERROR);
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws Exception {
        env = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
            TestEnvironment.Services.MRC, TestEnvironment.Services.DIR_CLIENT,
            TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD,
            TestEnvironment.Services.OSD_CLIENT, });
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
        StringSet results = makeCleanup(false, false, false);
        
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
        StringSet results = makeCleanup(false, false, false);
        
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
        StringSet group = new StringSet();
        group.add("test");
        UserCredentials uc = new UserCredentials("user", group, "");
        
        RPCResponse<?> r = null;
        
        setupTestVolume();
        
        // test/test1 --> zombie
        r = env.getMrcClient().unlink(env.getMRCAddress(), uc, "test/test1");
        r.get();
        r.freeBuffers();
        
        StringSet results = makeCleanup(false, false, false);
        
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
        StringSet group = new StringSet();
        group.add("test");
        UserCredentials uc = new UserCredentials("user", group, "");
        
        RPCResponse<?> r = null;
        
        setupTestVolume();
        
        // test/test1 --> zombie
        r = env.getMrcClient().unlink(env.getMRCAddress(), uc, "test/test1");
        r.get();
        r.freeBuffers();
        
        StringSet results = makeCleanup(false, false, true);
        
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
        StringSet group = new StringSet();
        group.add("test");
        UserCredentials uc = new UserCredentials("user", group, "");
        
        RPCResponse<?> r = null;
        
        setupTestVolume();
        
        // test/test1 --> zombie
        r = env.getMrcClient().unlink(env.getMRCAddress(), uc, "test/test1");
        r.get();
        r.freeBuffers();
        
        StringSet results = makeCleanup(true, false, false);
        
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
    private StringSet makeCleanup(boolean restore, boolean deleteVolumes, boolean killZombies)
        throws InterruptedException, ONCRPCException, IOException {
        String statF = CleanupThread.getRegex(CleanupThread.STATUS_FORMAT);
        String stopF = CleanupThread.getRegex(CleanupThread.STOPPED_FORMAT);
        assertNotNull(statF);
        assertNotNull(stopF);
        
        RPCResponse<?> r = null;
        
        // start the cleanUp Operation
        r = env.getOSDClient().internal_cleanup_start(env.getOSDAddress(), killZombies, deleteVolumes,
            restore, "");
        r.get();
        r.freeBuffers();
        
        boolean isRunning = true;
        do {
            r = env.getOSDClient().internal_cleanup_status(env.getOSDAddress(), "");
            String stat = (String) r.get();
            r.freeBuffers();
            
            assertNotNull(stat);
            try {
                if (stat.matches(statF))
                    assertTrue(true);
            } catch (NullPointerException ne) {
                assertTrue(stat.matches(stopF));
            }
            
            r = env.getOSDClient().internal_cleanup_is_running(env.getOSDAddress(), "");
            isRunning = (Boolean) r.get();
            r.freeBuffers();
        } while (isRunning);
        
        r = env.getOSDClient().internal_cleanup_get_result(env.getOSDAddress(), "");
        StringSet results = (StringSet) r.get();
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
        StringSet group = new StringSet();
        group.add("test");
        UserCredentials uc = new UserCredentials("user", group, "");
        
        StripingPolicy sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 4, 1);
        
        RPCResponse<?> r = env.getMrcClient().mkvol(env.getMRCAddress(), uc, "test", sp,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX.intValue(), 511);
        r.get();
        r.freeBuffers();
        
        RandomAccessFile raf = new RandomAccessFile("rw", env.getMRCAddress(), "test/test1", env
                .getRpcClient(), uc);
        raf.write(new byte[1024 * 10], 0, 1024 * 10);
        raf.close();
        
        raf = new RandomAccessFile("rw", env.getMRCAddress(), "test/test2", env.getRpcClient(), uc);
        raf.write(new byte[1024 * 10], 0, 1024 * 10);
        raf.close();
        
        raf = new RandomAccessFile("rw", env.getMRCAddress(), "test/test3", env.getRpcClient(), uc);
        raf.write(new byte[1024 * 10], 0, 1024 * 10);
        raf.close();
    }
}