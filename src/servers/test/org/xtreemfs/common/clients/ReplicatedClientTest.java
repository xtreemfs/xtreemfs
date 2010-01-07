/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.clients;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class ReplicatedClientTest {

    private TestEnvironment       testEnv;

    private static final String  VOLUME_NAME = "testvol";

    private UserCredentials uc;

    public ReplicatedClientTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName());

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
            TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.TIME_SYNC,
            TestEnvironment.Services.UUID_RESOLVER, TestEnvironment.Services.DIR_SERVICE,
            TestEnvironment.Services.MRC, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD,
            TestEnvironment.Services.OSD});
        testEnv.start();

        List<String> groupIDs = new ArrayList(1);
        groupIDs.add("test");
        uc = MRCClient.getCredentials("test", groupIDs);

        RPCResponse r = testEnv.getMrcClient().mkvol(testEnv.getMRCAddress(), uc, VOLUME_NAME,
            new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1),
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL.intValue(), 0777);
        r.get();
        r.freeBuffers();
    }

    @After
    public void tearDown() {
        testEnv.shutdown();
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testAddRemoveReplica() throws Exception {

        final Client c = new Client(new InetSocketAddress[]{testEnv.getDirClient().getDefaultServerAddress()}, 15000, 300000, null);
        c.start();

        Volume v = c.getVolume(VOLUME_NAME,uc);

        File f = v.getFile("test.file");
        RandomAccessFile raf = f.open("rw", 0666);
        byte[] data = new byte[4096];
        raf.write(data, 0, data.length);
        raf.seek(3*64*1024);
        raf.write(data, 0, data.length);
        raf.close();

        f.setReadOnly(true);

        String[] osds = f.getSuitableOSDs(1);
        assertTrue(osds.length >= 1);
        System.out.println("suitable OSD: "+osds[0]);

        f.addReplica(1, osds, Constants.REPL_FLAG_FULL_REPLICA);

        System.out.println("locations: "+f.getLocations());

        Thread.sleep(1000);

        for (Replica r : f.getReplicas()) {
            r.isCompleteReplica();
        }

        System.out.println("locations: "+f.getLocations());

        for (Replica r : f.getReplicas()) {
            assertTrue(r.isCompleteReplica());
        }

        f.getReplica(0).removeReplica(true);

        c.stop();

        
    }

}