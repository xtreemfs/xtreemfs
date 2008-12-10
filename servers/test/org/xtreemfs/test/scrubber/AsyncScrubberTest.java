package org.xtreemfs.test.scrubber;

import java.io.File;
import java.net.InetSocketAddress;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.clients.scrubber.AsyncScrubber;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.RequestController;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;

public class AsyncScrubberTest extends TestCase {
    
    private RequestController                  mrc1;
    
    private org.xtreemfs.dir.RequestController dirService;
    
    private MRCConfig                          mrcCfg1;
    
    private OSDConfig                          osdConfig1, osdConfig2;
    
    private DIRConfig                          dsCfg;
    
    private OSD                                osd1, osd2;
    
    private InetSocketAddress                  mrc1Address;
    
    private InetSocketAddress                  dirAddress;
    
    private MRCClient                          client;
    
    private String                             authString;
    
    private long                               accessMode;
    
    private String                             volumeName;
    
    private AsyncScrubber                      scrubber;
    
    public AsyncScrubberTest() {
        Logging.start(Logging.LEVEL_WARN);
    }
    
    public void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        authString = NullAuthProvider.createAuthString(System.getProperty("user.name"), MRCClient
                .generateStringList(System.getProperty("user.name")));
        
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
        
        // start the Directory Service
        dirService = new org.xtreemfs.dir.RequestController(dsCfg);
        dirService.startup();
        
        // start the OSD
        osd1 = new OSD(osdConfig1);
        osd2 = new OSD(osdConfig2);
        // start MRC
        mrc1 = new RequestController(mrcCfg1);
        mrc1.startup();
        
        client = SetupUtils.createMRCClient(10000000);
        
        volumeName = "testVolume";
        
        // create a volume (no access control)
        client.createVolume(mrc1Address, volumeName, authString);
        
        // create some files and directories
        client.createDir(mrc1Address, volumeName + "/myDir", authString);
        client.createDir(mrc1Address, volumeName + "/anotherDir", authString);
        client.createDir(mrc1Address, volumeName + "/yetAnotherDir", authString);
        
        for (int i = 0; i < 2; i++)
            client.createFile(mrc1Address, volumeName + "/myDir/test" + i + ".txt", null, null,
                accessMode, authString);
        
        client.createFile(mrc1Address, volumeName + "/test10.txt", null, null, accessMode,
            authString);
        
        client.createFile(mrc1Address, volumeName + "/anotherDir/test11.txt", null, null,
            accessMode, authString);
        RandomAccessFile randomAccessFile1 = new RandomAccessFile("r", mrc1Address, volumeName
            + "/anotherDir/test11.txt", client.getSpeedy(), authString);
        RandomAccessFile randomAccessFile2 = new RandomAccessFile("r", mrc1Address, volumeName
            + "/test10.txt", client.getSpeedy(), authString);
        
        String content = "";
        for (int i = 0; i < 6000; i++)
            content = content.concat("Hello World ");
        byte[] bytesIn = content.getBytes();
        assertEquals(bytesIn.length, 72000);
        
        int length = bytesIn.length;
        
        randomAccessFile1.write(bytesIn, 0, length);
        
        randomAccessFile2.write(bytesIn, 0, 65536);
    }
    
    public void tearDown() throws Exception {
        mrc1.shutdown();
        client.shutdown();
        osd1.shutdown();
        osd2.shutdown();
        scrubber.shutdown();
        dirService.shutdown();
        scrubber.waitForShutdown();
        client.waitForShutdown();
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }
    
    public void testAsyncScrubber() throws Exception {
        
        scrubber = new AsyncScrubber(client.getSpeedy(), dirAddress, mrc1Address, volumeName, true, 2, 2, null);
        scrubber.updateFileSize(volumeName + "/myDir/test0.txt", 10);
        scrubber.start();
        
        // file size corrected from 10 to 0
        assertEquals("0", client.stat(mrc1Address, volumeName + "/myDir/test0.txt", false, true,
            false, authString).get("size").toString());
        assertNotNull(client.getXAttr(mrc1Address, volumeName + "/myDir/test0.txt",
            AsyncScrubber.latestScrubAttr, authString));
        // file size same as before
        assertEquals("0", client.stat(mrc1Address, volumeName + "/myDir/test1.txt", false, true,
            false, authString).get("size").toString());
        assertNotNull(client.getXAttr(mrc1Address, volumeName + "/myDir/test0.txt",
            AsyncScrubber.latestScrubAttr, authString));
        // file size corrected from 0 to 72000 (this file is stored in two
        // objects)
        assertEquals("72000", client.stat(mrc1Address, volumeName + "/anotherDir/test11.txt",
            false, true, false, authString).get("size").toString());
        assertNotNull(client.getXAttr(mrc1Address, volumeName + "/myDir/test0.txt",
            AsyncScrubber.latestScrubAttr, authString));
        // file size corrected from 0 to 65536, which is the stripe size.
        assertEquals("65536", client.stat(mrc1Address, volumeName + "/test10.txt", false, true,
            false, authString).get("size").toString());
        assertNotNull(client.getXAttr(mrc1Address, volumeName + "/myDir/test0.txt",
            AsyncScrubber.latestScrubAttr, authString));
        
        long testVolume = Long.valueOf(client.getXAttr(mrc1Address, volumeName,
            AsyncScrubber.latestScrubAttr, authString).toString());
        long test0 = Long.valueOf(client.getXAttr(mrc1Address, volumeName + "/myDir/test0.txt",
            AsyncScrubber.latestScrubAttr, authString).toString());
        long test1 = Long.valueOf(client.getXAttr(mrc1Address, volumeName + "/myDir/test1.txt",
            AsyncScrubber.latestScrubAttr, authString).toString());
        long myDir = Long.valueOf(client.getXAttr(mrc1Address, volumeName + "/myDir",
            AsyncScrubber.latestScrubAttr, authString).toString());
        long anotherDir = Long.valueOf(client.getXAttr(mrc1Address, volumeName + "/anotherDir",
            AsyncScrubber.latestScrubAttr, authString).toString());
        long test11 = Long.valueOf(client.getXAttr(mrc1Address,
            volumeName + "/anotherDir/test11.txt", AsyncScrubber.latestScrubAttr, authString)
                .toString());
        long test10 = Long.valueOf(client.getXAttr(mrc1Address, volumeName + "/test10.txt",
            AsyncScrubber.latestScrubAttr, authString).toString());
        long yetAnotherDir = Long.valueOf(client.getXAttr(mrc1Address,
            volumeName + "/yetAnotherDir", AsyncScrubber.latestScrubAttr, authString).toString());
        
        assertTrue(testVolume >= myDir);
        assertTrue(testVolume >= anotherDir);
        assertTrue(testVolume >= yetAnotherDir);
        assertTrue(testVolume >= test10);
        assertTrue(anotherDir == test11);
        assertTrue(myDir >= test0);
        assertTrue(myDir >= test1);
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(AsyncScrubberTest.class);
    }
    
}