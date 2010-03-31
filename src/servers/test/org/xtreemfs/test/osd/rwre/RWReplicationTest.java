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
public class RWReplicationTest extends TestCase {

    private OSD[] osds;
    private OSDConfig[] configs;
    private TestEnvironment testEnv;

    private final static int NUM_OSDS = 3;
    private static final String fileId = "ABCDEF:1";

    public RWReplicationTest() {
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testReplicationWithClient() throws Exception {

        System.out.println("TEST: replication with client");

        StringSet grps = new StringSet();
        grps.add("test");
        UserCredentials uc = new UserCredentials("test", grps, null);

        Client c = new Client(new InetSocketAddress[]{testEnv.getDIRAddress()}, 15000, 60000, null);
        c.start();
        RPCResponse r = testEnv.getMrcClient().mkvol(testEnv.getMRCAddress(), uc, "testVol",
                new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX.intValue(),
                0777);
        r.get();
        r.freeBuffers();
        Volume v = c.getVolume("testVol", uc);
        File f = v.getFile("test.file");
        f.createFile();
        f.setReplicaUpdatePolicy(Constants.REPL_UPDATE_PC_WARONE);
        String[] suitableOSDs = f.getSuitableOSDs(1);
        f.addReplica(1, suitableOSDs, 0);

        byte[] data = new byte[2048];

        System.out.println("open file with replicas");
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
        r.get();
        System.out.println("got response");
        r.freeBuffers();


        data = new ObjectData();
        data.setData(BufferPool.allocate(1024*8));
        r = client.write(osd2, fileId, fc, 0, 0, 0, 0, data);
        try {
            r.get();
            fail("expected redirect");
        } catch (RedirectException ex) {
            System.out.println("got response: "+ex);
        }
        r.freeBuffers();

        data = new ObjectData();
        rb = BufferPool.allocate(1024);
        rb.put("MoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeepMoeep".getBytes());
        data.setData(rb);
        rb.limit(rb.capacity());
        rb.position(0);

        r = client.write(osd1, fileId, fc, 0, 0, 1024, 0, data);
        r.get();
        System.out.println("got response");
        r.freeBuffers();

        //read from slave
        System.out.println("//// START READ ////");
        RPCResponse<ObjectData> r2 = client.read(osd2, fileId, fc, 0, -1, 0, 2048);
        try {
            r2.get();
            fail("expected redirect");
        } catch (RedirectException ex) {
            System.out.println("got response: "+ex);
        }
        r2.freeBuffers();


        r2 = client.read(osd1, fileId, fc, 0, -1, 0, 2048);
        ObjectData od = r2.get();
        r2.freeBuffers();
        assertEquals(od.getData().get(0),(byte)'Y');
        assertEquals(od.getData().get(1),(byte)'a');
        BufferPool.free(od.getData());

        fc.getXcap().setTruncate_epoch(1);

        RPCResponse r3 = client.truncate(osd1, fileId, fc, 128*1024*2);
        r3.get();
        r3.freeBuffers();

    }


    @Test
    public void testReset() throws Exception {
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


        ReplicaSet oneRset = new ReplicaSet();
        StringSet sset = new StringSet();
        sset.add(configs[0].getUUID().toString());
        oneRset.add(new Replica(sset, 0, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1)));

        XLocSet oneLocSet = new XLocSet(0, oneRset, Constants.REPL_UPDATE_PC_NONE, 1);
        // set the first replica as current replica
        FileCredentials oneFC = new FileCredentials(cap.getXCap(), oneLocSet);


        final OSDClient client = testEnv.getOSDClient();

        final InetSocketAddress osd1 = new InetSocketAddress("localhost",configs[0].getPort());

        final InetSocketAddress osd2 = new InetSocketAddress("localhost",configs[1].getPort());

        ObjectData data = new ObjectData();
        ReusableBuffer rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        data.setData(rb);
        rb.limit(rb.capacity());
        rb.position(0);

        RPCResponse<OSDWriteResponse> r = client.write(osd1, fileId, oneFC, 0, 0, 0, 0, data);
        r.get();
        System.out.println("got response");
        r.freeBuffers();

        data = new ObjectData();
        rb = BufferPool.allocate(1024);
        rb.put("YaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYaggaYagga".getBytes());
        data.setData(rb);
        rb.limit(rb.capacity());
        rb.position(0);

        r = client.write(osd1, fileId, oneFC, 1, 0, 0, 0, data);
        r.get();
        System.out.println("got response");
        r.freeBuffers();

        System.out.println("waiting...");
        Thread.sleep(90*1000);
        System.out.println("continue...\n\n");

        data = new ObjectData();
        data.setData(BufferPool.allocate(1024*8));
        r = client.write(osd2, fileId, fc, 0, 0, 0, 0, data);
        r.get();
        System.out.println("got response");
        r.freeBuffers();



    }



}