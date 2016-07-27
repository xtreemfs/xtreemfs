/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.osd.AdvisoryLock;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * Class for testing the NewOSD It uses the old OSDTest tests. It checks if the OSD works without replicas neither
 * striping
 * 
 * @author bjko
 */
public class AdvisoryLocksTest {
    @Rule
    public final TestRule          testLog = TestHelper.testLog;

    private static ServiceUUID     serverID;

    private static FileCredentials fcred;

    private static String          fileId;

    private static Capability      cap;

    private static OSDConfig       osdConfig;

    private OSDServiceClient       osdClient;

    private TestEnvironment        testEnv;

    @BeforeClass
    public static void initializeTest() throws Exception {

        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @Before
    public void setUp() throws Exception {

        // startup: DIR
        testEnv = new TestEnvironment(
                new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                        TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                        TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT,
                        TestEnvironment.Services.OSD });
        testEnv.start();


        synchronized (this) {
            try {
                this.wait(50);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        osdClient = testEnv.getOSDClient();
        osdConfig = testEnv.getOSDConfig();
        serverID = osdConfig.getUUID();

        fileId = "ABCDEF:1";
        cap = new Capability(fileId, 0, 60, System.currentTimeMillis(), "", 0, false,
                SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, osdConfig.getCapabilitySecret());

        Replica r = Replica.newBuilder().setReplicationFlags(0).setStripingPolicy(SetupUtils.getStripingPolicy(1, 2))
                .addOsdUuids(serverID.toString()).build();
        XLocSet xloc = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy("").addReplicas(r)
                .setVersion(1).build();

        fcred = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xloc).build();
    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
    }

    @Test
    public void testAcquireCheckReleaseLock() throws Exception {

        Lock request = Lock.newBuilder().setClientPid(1).setClientUuid("test").setExclusive(true).setOffset(100)
                .setLength(AdvisoryLock.LENGTH_LOCK_TO_EOF).build();
        RPCResponse<Lock> r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        Lock l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(1, l.getClientPid());

        Lock checkRq = request.toBuilder().setClientPid(2).setLength(1).build();
        r = osdClient.xtreemfs_lock_check(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, checkRq);
        l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(1, l.getClientPid());

        RPCResponse r2 = osdClient.xtreemfs_lock_release(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        r2.get();

    }

    @Test
    public void testAcquireCheckReleaseLockRange01() throws Exception {

        Lock request = Lock.newBuilder().setClientPid(1).setClientUuid("test").setExclusive(true).setOffset(0)
                .setLength(1).build();
        RPCResponse<Lock> r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        Lock l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(1, l.getClientPid());

        Lock checkRq = request.toBuilder().setClientPid(2).setLength(0).setExclusive(false).build();
        r = osdClient.xtreemfs_lock_check(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, checkRq);
        l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(1, l.getClientPid());
        assertEquals(0, l.getOffset());
        assertEquals(1, l.getLength());
        assertTrue(l.getExclusive());

        RPCResponse r2 = osdClient.xtreemfs_lock_release(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        r2.get();

    }

    @Test
    public void testAcquireModify() throws Exception {

        Lock request = Lock.newBuilder().setClientPid(1).setClientUuid("test").setExclusive(true).setOffset(100)
                .setLength(AdvisoryLock.LENGTH_LOCK_TO_EOF).build();
        RPCResponse<Lock> r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        Lock l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(1, l.getClientPid());

        request = Lock.newBuilder().setClientPid(1).setClientUuid("test").setExclusive(true).setOffset(0)
                .setLength(100).build();
        r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(1, l.getClientPid());

        request = Lock.newBuilder().setClientPid(2).setClientUuid("test").setExclusive(true).setOffset(0)
                .setLength(100).build();
        r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        try {
            l = r.get();
            fail();
        } catch (PBRPCException ex) {
            assertEquals(POSIXErrno.POSIX_ERROR_EAGAIN, ex.getPOSIXErrno());
        }

        request = Lock.newBuilder().setClientPid(2).setClientUuid("test").setExclusive(true).setOffset(100)
                .setLength(100).build();
        r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(2, l.getClientPid());

    }

    @Test
    public void testAcquireUnlockAcquire() throws Exception {

        Lock request = Lock.newBuilder().setClientPid(1).setClientUuid("test").setExclusive(true).setOffset(100)
                .setLength(AdvisoryLock.LENGTH_LOCK_TO_EOF).build();
        RPCResponse<Lock> r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        Lock l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(1, l.getClientPid());

        request = Lock.newBuilder().setClientPid(1).setClientUuid("test").setExclusive(true).setOffset(0)
                .setLength(100).build();
        r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(1, l.getClientPid());

        request = Lock.newBuilder().setClientPid(2).setClientUuid("test").setExclusive(true).setOffset(0)
                .setLength(100).build();
        r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        try {
            l = r.get();
            fail();
        } catch (PBRPCException ex) {
            assertEquals(POSIXErrno.POSIX_ERROR_EAGAIN, ex.getPOSIXErrno());
        }

        request = Lock.newBuilder().setClientPid(1).setClientUuid("test").setExclusive(true).setOffset(0).setLength(0)
                .build();
        r = osdClient.xtreemfs_lock_release(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        r.get();

        request = Lock.newBuilder().setClientPid(2).setClientUuid("test").setExclusive(true).setOffset(0)
                .setLength(100).build();
        r = osdClient.xtreemfs_lock_acquire(serverID.getAddress(), RPCAuthentication.authNone,
                RPCAuthentication.userService, fcred, request);
        l = r.get();

        assertEquals("test", l.getClientUuid());
        assertEquals(2, l.getClientPid());

    }

}
