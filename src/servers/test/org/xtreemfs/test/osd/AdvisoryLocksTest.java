/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.osd;

import java.io.File;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.Lock;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.AdvisoryLock;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 * Class for testing the NewOSD It uses the old OSDTest tests. It checks if the
 * OSD works without replicas neither striping
 *
 * @author bjko
 */
public class AdvisoryLocksTest extends TestCase {

    private final ServiceUUID serverID;

    private final FileCredentials  fcred;

    private final String      fileId;

    private final Capability  cap;

    private final OSDConfig   osdConfig;


    private OSDClient         osdClient;


    private OSD               osdServer;

    private TestEnvironment   testEnv;

    public AdvisoryLocksTest(String testName) throws Exception {
        super(testName);

        Logging.start(Logging.LEVEL_DEBUG);

        osdConfig = SetupUtils.createOSD1Config();
        serverID = SetupUtils.getOSD1UUID();

        fileId = "ABCDEF:1";
        cap = new Capability(fileId, 0, System.currentTimeMillis(), "", 0, osdConfig.getCapabilitySecret());

        ReplicaSet replicas = new ReplicaSet();
        StringSet osdset = new StringSet();
        osdset.add(serverID.toString());
        Replica r = new Replica(new org.xtreemfs.interfaces.StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 2, 1), 0, osdset);
        replicas.add(r);
        XLocSet xloc = new XLocSet(replicas, 1, "",0);

        fcred = new FileCredentials(xloc, cap.getXCap());
    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[]{
                    TestEnvironment.Services.DIR_SERVICE,TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                    TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT
        });
        testEnv.start();

        osdServer = new OSD(osdConfig);

        synchronized (this) {
            try {
                this.wait(50);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        osdClient = new OSDClient(testEnv.getRpcClient());
    }

    protected void tearDown() throws Exception {
        System.out.println("teardown");
        osdServer.shutdown();

        testEnv.shutdown();
        System.out.println("shutdown complete");
    }

    public void testAcquireCheckReleaseLock() throws Exception {

        RPCResponse<Lock> r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 1, 100, AdvisoryLock.LOCK_TO_EOF, true);
        Lock l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(1, l.getClient_pid());

        r = osdClient.lock_check(serverID.getAddress(), fileId, fcred, "test", 2, 100, 1, true);
        l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(1, l.getClient_pid());

        RPCResponse r2 = osdClient.lock_release(serverID.getAddress(), fileId, fcred, "test",1);
        r2.get();

    }


    public void testAcquireModify() throws Exception {

        RPCResponse<Lock> r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 1, 100, AdvisoryLock.LOCK_TO_EOF, true);
        Lock l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(1, l.getClient_pid());

        r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 1, 0, 100, true);
        l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(1, l.getClient_pid());

        r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 2, 0, 100, true);
        l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(1, l.getClient_pid());

        r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 2, 100, 100, true);
        l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(2, l.getClient_pid());



    }

    public void testAcquireUnlockAcquire() throws Exception {

        RPCResponse<Lock> r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 1, 100, AdvisoryLock.LOCK_TO_EOF, true);
        Lock l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(1, l.getClient_pid());

        r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 1, 0, 100, true);
        l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(1, l.getClient_pid());

        r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 2, 0, 100, true);
        l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(1, l.getClient_pid());

        RPCResponse r2 = osdClient.lock_release(serverID.getAddress(), fileId, fcred, "test",1);
        r2.get();

        r = osdClient.lock_acquire(serverID.getAddress(), fileId, fcred, "test", 2, 0, 100, true);
        l = r.get();

        assertEquals("test",l.getClient_uuid());
        assertEquals(2, l.getClient_pid());



    }

    

    public static void main(String[] args) {
        TestRunner.run(AdvisoryLocksTest.class);
    }
}
