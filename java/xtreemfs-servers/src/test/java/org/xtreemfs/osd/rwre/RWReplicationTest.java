/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestHelper;

/**
 *
 * @author bjko
 */
public class RWReplicationTest {
    @Rule
    public final TestRule       testLog  = TestHelper.testLog;

    private OSD[]               osds;
    private OSDConfig[]         configs;
    private TestEnvironment     testEnv;

    private final static int    NUM_OSDS = 3;
    private static final String fileId   = "ABCDEF:1";

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @Before
    public void setUp() throws Exception {

        java.io.File testDir = new java.io.File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.OSD_CLIENT, TestEnvironment.Services.MRC,
                TestEnvironment.Services.MRC_CLIENT});
        testEnv.start();

        osds = new OSD[NUM_OSDS];
        configs = SetupUtils.createMultipleOSDConfigs(NUM_OSDS);
        for (int i = 0; i < osds.length; i++) {
            osds[i] = new OSD(configs[i]);
        }

    }


    @After
    public void tearDown() {
        if (osds != null) {
            for (OSD osd : this.osds) {
                if (osd != null)
                    osd.shutdown();
            }
        }

        testEnv.shutdown();
    }


    @Test
    public void testReplicationWithClient() throws Exception {
        UserCredentials uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
        Auth passwd = passwd = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD).setAuthPasswd(AuthPassword.newBuilder().setPassword("")).build();

        Client c = new Client(new InetSocketAddress[]{testEnv.getDIRAddress()}, 15000, 60000, null);
        c.start();
        c.createVolume("testVol", passwd, uc, SetupUtils.getStripingPolicy(1, 128), AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, 0777);

        Volume v = c.getVolume("testVol", uc);
        File f = v.getFile("test.file");
        f.createFile();
        f.setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE);
        String[] suitableOSDs = f.getSuitableOSDs(1);
        assertTrue("suitableOSDs.length >= 1", suitableOSDs.length >= 1);
        f.addReplica(1, suitableOSDs, 0);

        byte[] data = new byte[2048];

        // System.out.println("open file with replicas");
        RandomAccessFile raf = f.open("rw", 0444);
        raf.write(data, 0, data.length);

        raf.seek(1024);
        raf.forceReplica(1);

        raf.read(data, 0, data.length);

        raf.close();

        c.stop();
    }

    @Test
    public void testReplicatedWrite() throws Exception {
        Capability cap = new Capability(fileId, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber() | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 60, System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, configs[0].getCapabilitySecret());
        List<Replica> rlist = new LinkedList();
        for (OSDConfig osd : this.configs) {
            Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 128)).setReplicationFlags(0).addOsdUuids(osd.getUUID().toString()).build();
            rlist.add(r);
        }

        XLocSet locSet = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ).setVersion(1).addAllReplicas(rlist).build();
        // set the first replica as current replica
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(locSet).build();

        final OSDServiceClient client = testEnv.getOSDClient();

        final InetSocketAddress osd1 = new InetSocketAddress("localhost",configs[0].getPort());

        final InetSocketAddress osd2 = new InetSocketAddress("localhost",configs[1].getPort());

        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false).build();
        ReusableBuffer rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        rb.limit(rb.capacity());
        rb.position(0);

        RPCResponse<OSDWriteResponse> r = client.write(osd1, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, 0, 0, 0, 0, objdata, rb);
        r.get();
        // System.out.println("got response");
        r.freeBuffers();

        r = client.write(osd2, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, 0, 0, 0, 0, objdata, BufferPool.allocate(1024*8));
        try {
            r.get();
            fail("expected redirect");
        } catch (PBRPCException ex) {
            if (ex.getErrorType() != ErrorType.REDIRECT)
                fail("expected redirect");
            // System.out.println("got response: "+ex);
        }
        r.freeBuffers();

        rb = BufferPool.allocate(1024);
        rb.put("MoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeep".getBytes());
        rb.limit(rb.capacity());
        rb.position(0);

        r = client.write(osd1, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, 0, 0, 1024, 0, objdata, rb);
        r.get();
        // System.out.println("got response");
        r.freeBuffers();

        //read from slave
        // System.out.println("//// START READ ////");
        RPCResponse<ObjectData> r2 = client.read(osd2, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, 0, -1, 0, 2048);
        try {
            r2.get();
            fail("expected redirect");
        } catch (PBRPCException ex) {
            if (ex.getErrorType() != ErrorType.REDIRECT)
                fail("expected redirect");
            // System.out.println("got response: "+ex);
        }
        r2.freeBuffers();


        r2 = client.read(osd1, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, 0, -1, 0, 2048);
        ObjectData od = r2.get();
        
        assertEquals(r2.getData().get(0),(byte)'Y');
        assertEquals(r2.getData().get(1),(byte)'a');
        r2.freeBuffers();

        XCap newCap = fc.getXcap().toBuilder().setTruncateEpoch(1).build();
        fc = fc.toBuilder().setXcap(newCap).build();

        RPCResponse r3 = client.truncate(osd1, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, 128*1024*2);
        r3.get();
        r3.freeBuffers();

    }


    @Test
    public void testReset() throws Exception {
        Capability cap = new Capability(fileId, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber() | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 60, System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, configs[0].getCapabilitySecret());
        List<Replica> rlist = new LinkedList();
        for (OSDConfig osd : this.configs) {
            Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 128)).setReplicationFlags(0).addOsdUuids(osd.getUUID().toString()).build();
            rlist.add(r);
        }

        XLocSet locSet = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ).setVersion(1).addAllReplicas(rlist).build();
        // set the first replica as current replica
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(locSet).build();


        Replica repl = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 128)).setReplicationFlags(0).addOsdUuids(configs[0].getUUID().toString()).build();

        XLocSet oneLocSet = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE).setVersion(1).addReplicas(repl).build();
                //new XLocSet(0, oneRset, Constants.REPL_UPDATE_PC_NONE, 1);
        // set the first replica as current replica
        FileCredentials oneFC = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(oneLocSet).build();


        final OSDServiceClient client = testEnv.getOSDClient();

        final InetSocketAddress osd1 = new InetSocketAddress("localhost",configs[0].getPort());

        final InetSocketAddress osd2 = new InetSocketAddress("localhost",configs[1].getPort());

        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false).build();
        ReusableBuffer rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        rb.limit(rb.capacity());
        rb.position(0);

        RPCResponse<OSDWriteResponse> r = client.write(osd1, RPCAuthentication.authNone, RPCAuthentication.userService,
                    oneFC, fileId, 0, 0, 0, 0, objdata, rb);
        r.get();
        // System.out.println("got response");
        r.freeBuffers();

        rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        rb.limit(rb.capacity());
        rb.position(0);

        r = client.write(osd1, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, 1, 0, 0, 0, objdata, rb);
        r.get();
        // System.out.println("got response");
        r.freeBuffers();

        // System.out.println("waiting...");
        Thread.sleep(90*1000);
        // System.out.println("continue...\n\n");

        r = client.write(osd2, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, 0, 0, 0, 0, objdata, BufferPool.allocate(1024*8));
        r.get();
        // System.out.println("got response");
        r.freeBuffers();
    }
}