/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck(ZIB), Nele Andersen (ZIB)
 */
package org.xtreemfs.test.scrubber;

import java.io.File;
import java.net.InetSocketAddress;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.clients.simplescrubber.Scrubber;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.stat_;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class ScrubberTest extends TestCase {

    private MRCRequestDispatcher                  mrc1;


    private MRCConfig                          mrcCfg1;

    private OSDConfig                          osdConfig1, osdConfig2;

    private DIRConfig                          dsCfg;

    private OSD                                osd1, osd2;

    private InetSocketAddress                  mrc1Address;

    private InetSocketAddress                  dirAddress;

    private MRCClient                          client;


    private long                               accessMode;

    private String                             volumeName;

    private Scrubber                           scrubber;
    private TestEnvironment testEnv;

    public ScrubberTest() {
        Logging.start(Logging.LEVEL_WARN);
    }

    public void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        accessMode = 511; // rwxrwxrwx

        dsCfg = SetupUtils.createDIRConfig();
        dirAddress = SetupUtils.getDIRAddr();

        mrcCfg1 = SetupUtils.createMRC1Config();
        mrc1Address = SetupUtils.getMRC1Addr();

        osdConfig1 = SetupUtils.createOSD1Config();
        osdConfig2 = SetupUtils.createOSD2Config();
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

        // start the OSD
        osd1 = new OSD(osdConfig1);
        osd2 = new OSD(osdConfig2);
        // start MRC
        mrc1 = new MRCRequestDispatcher(mrcCfg1);
        mrc1.startup();


        client = testEnv.getMrcClient();

        volumeName = "testVolume";


        // create a volume (no access control)
        RPCResponse r = testEnv.getMrcClient().mkvol(mrc1Address, Scrubber.userID, Scrubber.groupIDs, "",volumeName,
                Constants.OSD_SELECTION_POLICY_SIMPLE,
                new StripingPolicy(Constants.STRIPING_POLICY_RAID0, 64, 1),
                Constants.ACCESS_CONTROL_POLICY_NULL);
        r.get();
        r.freeBuffers();

        // create some files and directories
        //testEnv.getMrcClient().createDir(mrc1Address, volumeName + "/myDir", authString);
        r = testEnv.getMrcClient().mkdir(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/myDir", 0);
        r.get();
        r.freeBuffers();

        r = testEnv.getMrcClient().mkdir(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/anotherDir", 0);
        r.get();
        r.freeBuffers();

        r = testEnv.getMrcClient().mkdir(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/yetAnotherDir", 0);
        r.get();
        
        for (int i = 0; i < 2; i++) {
            r = testEnv.getMrcClient().create(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/myDir/test" + i + ".txt", 0);
            r.get();
            r.freeBuffers();
        }

        r = testEnv.getMrcClient().create(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/test10.txt", 0);
        r.get();
        r.freeBuffers();

        r = testEnv.getMrcClient().create(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/anotherDir/test11.txt", 0);
        r.get();
        r.freeBuffers();

        
        RandomAccessFile randomAccessFile1 = new RandomAccessFile("r", mrc1Address, volumeName
            + "/anotherDir/test11.txt", testEnv.getRpcClient(), Scrubber.userID, Scrubber.groupIDs);
        RandomAccessFile randomAccessFile2 = new RandomAccessFile("r", mrc1Address, volumeName
            + "/test10.txt", testEnv.getRpcClient(), Scrubber.userID, Scrubber.groupIDs);
        randomAccessFile2.forceFileSize(10);

        RandomAccessFile randomAccessFile3 = new RandomAccessFile("r", mrc1Address, volumeName
            + "/test0.txt", testEnv.getRpcClient(), Scrubber.userID, Scrubber.groupIDs);
        randomAccessFile3.forceFileSize(10);



        String content = "";
        for (int i = 0; i < 6000; i++)
            content = content.concat("Hello World ");
        byte[] bytesIn = content.getBytes();
        assertEquals(bytesIn.length, 72000);

        int length = bytesIn.length;

        randomAccessFile1.write(bytesIn, 0, length);
        randomAccessFile1.close();

        randomAccessFile2.write(bytesIn, 0, 65536);
    }

    public void tearDown() throws Exception {
        mrc1.shutdown();
        osd1.shutdown();
        osd2.shutdown();

        testEnv.shutdown();

        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }

    public void testScrubber() throws Exception {

        scrubber = new Scrubber(testEnv.getRpcClient(), testEnv.getDirClient(), new MRCClient(testEnv.getRpcClient(),mrc1Address)
                , volumeName, false, 3);
        scrubber.scrub();


        // file size corrected from 10 to 0
        RPCResponse r = client.getattr(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/myDir/test0.txt");
        stat_ s = (stat_) r.get();
        r.freeBuffers();

        assertEquals(0, s.getSize());


        // file size same as before
        r = client.getattr(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/myDir/test1.txt");
        s = (stat_) r.get();
        r.freeBuffers();

        assertEquals(0, s.getSize());

        r = client.getxattr(mrc1Address, Scrubber.userID, Scrubber.groupIDs,
                volumeName, Scrubber.latestScrubAttr);
        String result = (String)r.get();
        r.freeBuffers();
        assertNotNull(result);
        assertTrue(result.length() > 0);


        // file size corrected from 0 to 72000 (this file is stored in two
        // objects)
        r = client.getattr(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/anotherDir/test11.txt");
        s = (stat_) r.get();
        r.freeBuffers();

        assertEquals(72000, s.getSize());

        // file size corrected from 0 to 65536, which is the stripe size.

        r = client.getattr(mrc1Address, Scrubber.userID, Scrubber.groupIDs, volumeName + "/test10.txt");
        s = (stat_) r.get();
        r.freeBuffers();

        assertEquals(65536, s.getSize());

    }

    public static void main(String[] args) {
        TestRunner.run(ScrubberTest.class);
    }

}