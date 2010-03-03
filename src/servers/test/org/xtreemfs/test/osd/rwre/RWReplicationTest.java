/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.osd.rwre;

import java.io.File;
import java.net.InetSocketAddress;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.SnapConfig;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
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

        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.OSD_CLIENT });
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

}