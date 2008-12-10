/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.mrc;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.RequestController;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class LogReplayTest extends TestCase {

    private static final boolean               DEBUG    = false;

    private static final String                TEST_DIR = "/tmp/xtreemfs-test";

    private RequestController                  mrc1;

    private org.xtreemfs.dir.RequestController dirService;

    private MRCClient                          client;

    private MRCConfig                          mrcCfg1;

    private DIRConfig                          dsCfg;

    private OSDConfig                          osdConfig;

    private OSD                                osd;

    private InetSocketAddress                  mrc1Address;

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "."
            + getName());

        dsCfg = SetupUtils.createDIRConfig();

        mrcCfg1 = SetupUtils.createMRC1Config();
        mrc1Address = SetupUtils.getMRC1Addr();

        osdConfig = SetupUtils.createOSD1Config();

        // cleanup
        File testDir = new File(TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        // start services
        dirService = new org.xtreemfs.dir.RequestController(dsCfg);
        dirService.startup();

        osd = new OSD(osdConfig);

        mrc1 = new RequestController(mrcCfg1);
        mrc1.startup();

        client = SetupUtils.createMRCClient(10000);
    }

    public LogReplayTest(String testName) {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    protected void tearDown() throws Exception {
        client.shutdown();
        mrc1.shutdown();
        dirService.shutdown();
        osd.shutdown();

        client.waitForShutdown();
    }

    public void testReplay() throws Exception {

        final String authString = NullAuthProvider.createAuthString("someUser", MRCClient.generateStringList("someGroup"));

        // create a volume, directory and file
        client.createVolume(mrc1Address, "testVolumeREPL", authString);
        client.createDir(mrc1Address, "testVolumeREPL/bla", authString);
        client.createFile(mrc1Address, "testVolumeREPL/bla/yabba", authString);

        // open the file and update its file size
        Map<String, String> xcap = client.open(mrc1Address,
            "testVolumeREPL/bla/yabba", "r", authString);
        client.updateFileSize(mrc1Address, xcap.get("X-Capability"),
            "[1024,1]", authString);
        mrc1.dropDead();

        synchronized (this) {
            try {
                this.wait(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        mrc1 = new RequestController(mrcCfg1);
        mrc1.startup();

        List<String> dir = client.readDir(mrc1Address, "testVolumeREPL/bla",
            authString);
        assertTrue(dir.size() == 1);
        assertTrue(dir.get(0).equals("yabba"));
        Map<String, Object> statInfo = client.stat(mrc1Address, "testVolumeREPL/bla/yabba", false, false, false, authString);
        assertEquals(1024L, statInfo.get("size"));
    }

    public static void main(String[] args) {
        TestRunner.run(LogReplayTest.class);
    }

}
