/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.osd;

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
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.test.TestEnvironment;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class NewCleanupTest extends TestCase {

    private TestEnvironment env;

    public NewCleanupTest() {
        super();
        Logging.start(Logging.LEVEL_INFO);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        env = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.DIR_SERVICE,
        TestEnvironment.Services.MRC, TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.MRC_CLIENT,
                    TestEnvironment.Services.OSD, TestEnvironment.Services.OSD_CLIENT,
        });
        env.start();
    }

    @After
    public void tearDown() {
        env.shutdown();
    }

    public void testCleanup() throws Exception {

        StringSet group = new StringSet();
        group.add("test");
        UserCredentials uc = new UserCredentials("user", group, "");

        StripingPolicy sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 4, 1);

        RPCResponse r = env.getMrcClient().mkvol(env.getMRCAddress(), uc, "test", OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE.intValue(),
                sp, AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX.intValue(), 511);
        r.get();
        r.freeBuffers();

        RandomAccessFile raf = new RandomAccessFile("rw", env.getMRCAddress(), "test/test1", env.getRpcClient(), uc);
        raf.write(new byte[1024*10], 0, 1024*10);
        raf.close();

        raf = new RandomAccessFile("rw", env.getMRCAddress(), "test/test2", env.getRpcClient(), uc);
        raf.write(new byte[1024*10], 0, 1024*10);
        raf.close();

        r = env.getMrcClient().unlink(env.getMRCAddress(), uc, "test/test1");
        r.get();
        r.freeBuffers();

        r = env.getOSDClient().internal_cleanup_start(env.getOSDAddress(), false, false, false, "");
        r.get();
        r.freeBuffers();

        boolean isRunning = true;
        do {
            r = env.getOSDClient().internal_cleanup_status(env.getOSDAddress(), "");
            String status = (String)r.get();
            r.freeBuffers();
            System.out.println(status);

            r = env.getOSDClient().internal_cleanup_is_running(env.getOSDAddress(), "");
            isRunning = (Boolean)r.get();
            r.freeBuffers();
            if (isRunning)
                Thread.sleep(250);
            
        } while (isRunning);

        r = env.getOSDClient().internal_cleanup_get_result(env.getOSDAddress(), "");
        StringSet results = (StringSet)r.get();
        r.freeBuffers();
        for (String line : results)
            System.out.println(line);


    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

}