/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients;

import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 *
 * @author bjko
 */
public class ReplicatedClientTest {
    @Rule
    public final TestRule       testLog     = TestHelper.testLog;

    private TestEnvironment       testEnv;

    private static final String  VOLUME_NAME = "testvol";

    private UserCredentials uc;

    public ReplicatedClientTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @Before
    public void setUp() throws Exception {
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
            TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.TIME_SYNC,
            TestEnvironment.Services.UUID_RESOLVER, TestEnvironment.Services.DIR_SERVICE,
            TestEnvironment.Services.MRC, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD,
            TestEnvironment.Services.OSD});
        testEnv.start();

        uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        RPCResponse r = testEnv.getMrcClient().xtreemfs_mkvol(testEnv.getMRCAddress(), RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, SetupUtils.getStripingPolicy(1, 64), "", 0777, VOLUME_NAME, "test", "test", new LinkedList<KeyValuePair>(), 0, 0);
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

        final Client c = new Client(new InetSocketAddress[]{testEnv.getDIRAddress()}, 15000, 300000, null);
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
        // System.out.println("suitable OSD: "+osds[0]);

        f.addReplica(1, osds, ReplicationFlags.setRandomStrategy(ReplicationFlags.setFullReplica(0)));

        // System.out.println("locations: "+f.getLocations(uc));

        Thread.sleep(1000);

        for (Replica r : f.getReplicas(uc)) {
            r.isCompleteReplica();
        }

        raf = f.open("r", 0666);
        raf.read(data, 0, data.length);
        raf.close();

        // System.out.println("locations: "+f.getLocations(uc));

        for (Replica r : f.getReplicas(uc)) {
            assertTrue(r.isCompleteReplica());
        }

        f.getReplica(0).removeReplica(true);

        c.stop();

        
    }

}