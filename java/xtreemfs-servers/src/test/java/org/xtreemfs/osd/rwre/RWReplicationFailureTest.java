/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd.rwre;

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
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * @author bjko
 */
public class RWReplicationFailureTest {
    @Rule
    public final TestRule       testLog  = TestHelper.testLog;

    private OSD[]               osds;
    private OSDConfig[]         configs;
    private TestEnvironment     testEnv;

    private final static int    NUM_OSDS = 2;
    private static final String fileId   = "ABCDEF:1";

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
                        TestEnvironment.Services.OSD_CLIENT, TestEnvironment.Services.MRC,
                        TestEnvironment.Services.MRC_CLIENT });
        testEnv.start();

        osds = new OSD[NUM_OSDS];
        configs = SetupUtils.createMultipleOSDConfigs(NUM_OSDS);
        configs[1].setCapabilitySecret("somenonsense347534593475");
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
    public void testInvalidCap() throws Exception {
        Capability cap = new Capability(fileId, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 60, System.currentTimeMillis() + 10000, "", 0,
                false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, configs[0].getCapabilitySecret());
        List<Replica> rlist = new LinkedList();
        for (OSDConfig osd : this.configs) {
            Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 128))
                    .setReplicationFlags(0).addOsdUuids(osd.getUUID().toString()).build();
            rlist.add(r);
        }

        XLocSet locSet = XLocSet.newBuilder().setReadOnlyFileSize(0)
                .setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE).setVersion(1)
                .addAllReplicas(rlist).build();
        // set the first replica as current replica
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(locSet).build();

        final OSDServiceClient client = testEnv.getOSDClient();

        final InetSocketAddress osd1 = new InetSocketAddress("localhost", configs[0].getPort());

        final InetSocketAddress osd2 = new InetSocketAddress("localhost", configs[1].getPort());

        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        ReusableBuffer rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        rb.limit(rb.capacity());
        rb.position(0);

        RPCResponse<OSDWriteResponse> r = client.write(osd1, RPCAuthentication.authNone, RPCAuthentication.userService,
                fc, fileId, 0, 0, 0, 0, objdata, rb);
        try {
            r.get();
            fail("write should have failed");
        } catch (Exception ex) {
            // System.out.println("write failed as expected: "+ex);
        }
        r.freeBuffers();

    }

    @Test
    public void testOfflineOSD() throws Exception {
        Capability cap = new Capability(fileId, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 60, System.currentTimeMillis() + 10000, "", 0,
                false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, configs[0].getCapabilitySecret());
        List<Replica> rlist = new LinkedList();
        for (OSDConfig osd : this.configs) {
            Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 128))
                    .setReplicationFlags(0).addOsdUuids(osd.getUUID().toString()).build();
            rlist.add(r);
        }

        Replica replica = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 128))
                .setReplicationFlags(0).addOsdUuids("yaggablurp").build();
        rlist.add(replica);

        XLocSet locSet = XLocSet.newBuilder().setReadOnlyFileSize(0)
                .setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE).setVersion(1)
                .addAllReplicas(rlist).build();
        // set the first replica as current replica
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(locSet).build();

        UUIDResolver.addTestMapping("yaggaBluuurp", "www.xtreemfs.org", 32640, false);


        final OSDServiceClient client = testEnv.getOSDClient();

        final InetSocketAddress osd1 = new InetSocketAddress("localhost", configs[0].getPort());

        final InetSocketAddress osd2 = new InetSocketAddress("localhost", configs[1].getPort());

        ObjectData objdata = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        ReusableBuffer rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        rb.limit(rb.capacity());
        rb.position(0);

        RPCResponse<OSDWriteResponse> r = client.write(osd1, RPCAuthentication.authNone, RPCAuthentication.userService,
                fc, fileId, 0, 0, 0, 0, objdata, rb);
        try {
            r.get();
            fail("write should have failed");
        } catch (Exception ex) {
            // System.out.println("write failed as expected: "+ex);
        }
        r.freeBuffers();

    }

}