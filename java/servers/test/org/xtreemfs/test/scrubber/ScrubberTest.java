/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Nele Andersen,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.scrubber;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.LinkedList;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getxattrResponse;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.utils.xtfs_scrub;

public class ScrubberTest extends TestCase {
    
    private MRCRequestDispatcher mrc1;
    
    private MRCConfig            mrcCfg1;
    
    private BabuDBConfig         mrcDBCfg1;
    
    private OSDConfig            osdConfig1, osdConfig2;
    
    private DIRConfig            dsCfg;
    
    private OSD                  osd1, osd2;
    
    private InetSocketAddress    mrc1Address;
    
    private InetSocketAddress    dirAddress;
    
    private MRCServiceClient     client;
    
    private long                 accessMode;
    
    private String               volumeName;
    
    private xtfs_scrub             scrubber;
    
    private TestEnvironment      testEnv;
    
    private Client               newClient;
    
    public ScrubberTest() {
        Logging.start(Logging.LEVEL_WARN);
    }
    
    public void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        accessMode = 511; // rwxrwxrwx
        
        dsCfg = SetupUtils.createDIRConfig();
        dirAddress = SetupUtils.getDIRAddr();
        
        mrcCfg1 = SetupUtils.createMRC1Config();
        mrcDBCfg1 = SetupUtils.createMRC1dbsConfig();
        mrc1Address = SetupUtils.getMRC1Addr();
        
        osdConfig1 = SetupUtils.createOSD1Config();
        osdConfig2 = SetupUtils.createOSD2Config();
        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);
        
        FSUtils.delTree(testDir);
        testDir.mkdirs();
        
        // startup: DIR
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
            TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
            TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.OSD_CLIENT });
        testEnv.start();
        
        // start the OSD
        osd1 = new OSD(osdConfig1);
        osd2 = new OSD(osdConfig2);
        // start MRC
        mrc1 = new MRCRequestDispatcher(mrcCfg1, mrcDBCfg1);
        mrc1.startup();
        
        client = testEnv.getMrcClient();
        
        volumeName = "testVolume";
        
        // create a volume (no access control)
        RPCResponse r = testEnv.getMrcClient().xtreemfs_mkvol(
            mrc1Address,
            RPCAuthentication.authNone,
            xtfs_scrub.credentials,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
            StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0).setStripeSize(64)
                    .setWidth(1).build(), "", 0, volumeName, "", "", new LinkedList<KeyValuePair>());
        r.get();
        r.freeBuffers();
        
        // create some files and directories
        // testEnv.getMrcClient().createDir(mrc1Address, volumeName + "/myDir",
        // authString);
        r = testEnv.getMrcClient().mkdir(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials,
            volumeName, "myDir", 0);
        r.get();
        r.freeBuffers();
        
        r = testEnv.getMrcClient().mkdir(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials,
            volumeName, "anotherDir", 0);
        r.get();
        r.freeBuffers();
        
        r = testEnv.getMrcClient().mkdir(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials,
            volumeName, "yetAnotherDir", 0);
        r.get();
        
        for (int i = 0; i < 2; i++) {
            r = testEnv.getMrcClient().open(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials,
                volumeName, "myDir/test" + i + ".txt", FileAccessManager.O_CREAT, 0, 0,
                VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0).setLocalError(0).build());
            r.get();
            r.freeBuffers();
        }
        
        r = testEnv.getMrcClient().open(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials,
            volumeName, "test10.txt", FileAccessManager.O_CREAT, 0, 0,
            VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0).setLocalError(0).build());
        r.get();
        r.freeBuffers();
        
        r = testEnv.getMrcClient().open(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials,
            volumeName, "anotherDir/test11.txt", FileAccessManager.O_CREAT, 0, 0,
            VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0).setLocalError(0).build());
        r.get();
        r.freeBuffers();
        
        newClient = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15 * 1000, 5 * 60 * 1000,
            null);
        newClient.start();
        Volume vol = newClient.getVolume(volumeName, xtfs_scrub.credentials);
        
        RandomAccessFile randomAccessFile1 = vol.getFile("/anotherDir/test11.txt").open("rw", 0777);
        RandomAccessFile randomAccessFile2 = vol.getFile("/test10.txt").open("rw", 0777);
        RandomAccessFile randomAccessFile3 = vol.getFile("/test0.txt").open("rw", 0777);
        
        DirectoryEntry[] entries = vol.listEntries("");
        
        randomAccessFile2.forceFileSize(10);
        randomAccessFile3.forceFileSize(10);
        
        String content = "";
        for (int i = 0; i < 6000; i++)
            content = content.concat("Hello World ");
        byte[] bytesIn = content.getBytes();
        assertEquals(bytesIn.length, 72000);
        
        int length = bytesIn.length;
        
        randomAccessFile1.write(bytesIn, 0, length);
        randomAccessFile1.close();
        assertEquals(length, randomAccessFile1.getFile().length());
        
        randomAccessFile2.write(bytesIn, 0, 65536);
        assertEquals(10, randomAccessFile2.getFile().length());
        
    }
    
    public void tearDown() throws Exception {
        mrc1.shutdown();
        osd1.shutdown();
        osd2.shutdown();
        
        newClient.stop();
        
        testEnv.shutdown();
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }
    
    public void testScrubber() throws Exception {
        
        scrubber = new xtfs_scrub(newClient, volumeName, 3, true, true, true);
        scrubber.scrub();
        
        // file size corrected from 10 to 0
        RPCResponse<getattrResponse> r = client.getattr(mrc1Address, RPCAuthentication.authNone,
            xtfs_scrub.credentials, volumeName, "myDir/test0.txt", -1);
        Stat s = r.get().getStbuf();
        r.freeBuffers();
        
        assertEquals(0, s.getSize());
        
        // file size same as before
        r = client.getattr(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials, volumeName,
            "myDir/test1.txt", -1);
        s = r.get().getStbuf();
        r.freeBuffers();
        
        assertEquals(0, s.getSize());
        
        RPCResponse<getxattrResponse> r2 = client.getxattr(mrc1Address, RPCAuthentication.authNone,
            xtfs_scrub.credentials, volumeName, "", xtfs_scrub.latestScrubAttr);
        String result = r2.get().getValue();
        r2.freeBuffers();
        assertNotNull(result);
        assertTrue(result.length() > 0);
        
        // file size corrected from 0 to 72000 (this file is stored in two
        // objects)
        r = client.getattr(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials, volumeName,
            "anotherDir/test11.txt", -1);
        s = r.get().getStbuf();
        r.freeBuffers();
        
        assertEquals(72000, s.getSize());
        
        // file size corrected from 0 to 65536, which is the stripe size.
        
        r = client.getattr(mrc1Address, RPCAuthentication.authNone, xtfs_scrub.credentials, volumeName,
            "test10.txt", -1);
        s = r.get().getStbuf();
        r.freeBuffers();
        
        assertEquals(65536, s.getSize());
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(ScrubberTest.class);
    }
    
}