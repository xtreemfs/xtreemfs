/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.test.osd.rwre;

import java.net.InetSocketAddress;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.SnapConfig;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 *
 * @author bjko
 */
public class RWReplicationFailureTest extends TestCase {

    private OSD[] osds;
    private OSDConfig[] configs;
    private TestEnvironment testEnv;

    private final static int NUM_OSDS = 2;
    private static final String fileId = "ABCDEF:1";

    public RWReplicationFailureTest() {
        super();
        Logging.start(Logging.LEVEL_DEBUG, Category.all);
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
        Capability cap = new Capability(fileId, 0, 60, System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, configs[0].getCapabilitySecret());
        ReplicaSet rset = new ReplicaSet();
        for (OSDConfig osd : this.configs) {
            StringSet sset = new StringSet();
            sset.add(osd.getUUID().toString());
            rset.add(new Replica(sset, 0, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1)));
        }

        XLocSet locSet = new XLocSet(0, rset, Constants.REPL_UPDATE_PC_WARONE, 1);
        // set the first replica as current replica
        FileCredentials fc = new FileCredentials(cap.getXCap(), locSet);

        final OSDClient client = testEnv.getOSDClient();

        final InetSocketAddress osd1 = new InetSocketAddress("localhost",configs[0].getPort());

        final InetSocketAddress osd2 = new InetSocketAddress("localhost",configs[1].getPort());

        ObjectData data = new ObjectData();
        ReusableBuffer rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        data.setData(rb);
        rb.limit(rb.capacity());
        rb.position(0);

        RPCResponse<OSDWriteResponse> r = client.write(osd1, fileId, fc, 0, 0, 0, 0, data);
        try {
            r.get();
            fail("write should have failed");
        } catch (Exception ex) {
            System.out.println("write failed as expected: "+ex);
        }
        r.freeBuffers();

    }

    @Test
    public void testOfflineOSD() throws Exception {
        Capability cap = new Capability(fileId, 0, 60, System.currentTimeMillis(), "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, configs[0].getCapabilitySecret());
        ReplicaSet rset = new ReplicaSet();
        
        StringSet sset = new StringSet();
        sset.add(configs[0].getUUID().toString());
        rset.add(new Replica(sset, 0, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1)));

        UUIDResolver.addTestMapping("yaggaBluuurp", "www.xtreemfs.org", 32640, false);

        sset = new StringSet();
        sset.add("yaggaBluuurp");
        rset.add(new Replica(sset, 0, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1)));
        

        XLocSet locSet = new XLocSet(0, rset, Constants.REPL_UPDATE_PC_WARONE, 1);
        // set the first replica as current replica
        FileCredentials fc = new FileCredentials(cap.getXCap(), locSet);

        final OSDClient client = testEnv.getOSDClient();

        final InetSocketAddress osd1 = new InetSocketAddress("localhost",configs[0].getPort());

        final InetSocketAddress osd2 = new InetSocketAddress("localhost",configs[1].getPort());

        ObjectData data = new ObjectData();
        ReusableBuffer rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        data.setData(rb);
        rb.limit(rb.capacity());
        rb.position(0);

        RPCResponse<OSDWriteResponse> r = client.write(osd1, fileId, fc, 0, 0, 0, 0, data);
        try {
            r.get();
            fail("write should have failed");
        } catch (Exception ex) {
            System.out.println("write failed as expected: "+ex);
        }
        r.freeBuffers();

    }



}