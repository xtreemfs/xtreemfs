package org.xtreemfs.test.io;

import java.io.File;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;

public class RandomAccessFileTest extends TestCase {
    RandomAccessFile randomAccessFile;

    private MRCRequestDispatcher mrc1;

    private RequestController dirService;

    private MRCConfig mrcCfg1;

    private OSDConfig osdConfig1, osdConfig2;

    private DIRConfig dsCfg;

    private OSD osd1, osd2;

    private InetSocketAddress mrc1Address;

    private MRCClient client;

    private MultiSpeedy speedy;

    private String authString;

    private String volumeName;

    public RandomAccessFileTest() {
        Logging.start(Logging.LEVEL_TRACE);
    }

    public void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        dsCfg = SetupUtils.createDIRConfig();

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

        // start the OSDs
        osd1 = new OSD(osdConfig1);
        osd2 = new OSD(osdConfig2);

        // start MRC
        mrc1 = new MRCRequestDispatcher(mrcCfg1);
        mrc1.startup();

        client = SetupUtils.createMRCClient(10000);

        speedy = new MultiSpeedy();
        speedy.start();

        String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));

        volumeName = "testVolume";

        // create a volume (no access control)
        client.createVolume(mrc1Address, volumeName, authString);

        // create some files and directories
        client.createDir(mrc1Address, volumeName + "/myDir", authString);

        for (int i = 0; i < 10; i++)
            client.createFile(mrc1Address, volumeName + "/myDir/test" + i + ".txt", authString);

    }

    public void tearDown() throws Exception {
        mrc1.shutdown();
        client.shutdown();
        osd1.shutdown();
        osd2.shutdown();
        dirService.shutdown();
        speedy.shutdown();

        client.waitForShutdown();
        speedy.waitForShutdown();

        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }

    public void testReadAndWrite() throws Exception {
        randomAccessFile = new RandomAccessFile("w", mrc1Address, volumeName + "/myDir/test1.txt", speedy);

        byte[] bytesIn = new byte[(int) (3 * randomAccessFile.getStripeSize() + 2)];
        for (int i = 0; i < 3 * randomAccessFile.getStripeSize() + 2; i++) {
            bytesIn[i] = (byte) (i % 25 + 65);
        }
        int length = bytesIn.length;
        int result = randomAccessFile.write(bytesIn, 0, length);
        assertEquals(length, result);

        byte[] bytesOut = new byte[length];
        result = randomAccessFile.read(bytesOut, 0, length);

        assertEquals(0, result);

        bytesOut = new byte[length];
        randomAccessFile.seek(0);
        result = randomAccessFile.read(bytesOut, 0, length);

        assertEquals(length, result);
        assertEquals(new String(bytesIn), new String(bytesOut));

        bytesOut = new byte[4];

        bytesIn = "Hello World".getBytes();
        randomAccessFile.seek(0);
        randomAccessFile.write(bytesIn, 0, bytesIn.length);

        randomAccessFile.seek(0);
        result = randomAccessFile.read(bytesOut, 0, 4);
        assertEquals(new String(bytesOut), new String("Hell"));

        randomAccessFile.seek(1);
        bytesOut = new byte[4];
        result = randomAccessFile.read(bytesOut, 0, 4);
        assertEquals(new String(bytesOut), new String("ello"));

    }

    public void testReadAndWriteObject() throws Exception {
        randomAccessFile = new RandomAccessFile("w", mrc1Address, volumeName + "/myDir/test1.txt", speedy);

        byte[] bytesIn = new String("Hallo").getBytes();
        int length = bytesIn.length;
        ReusableBuffer data = ReusableBuffer.wrap(bytesIn);
        randomAccessFile.writeObject(0, 0, data);

        ReusableBuffer result = randomAccessFile.readObject(0, 0, length);
        assertEquals(new String(bytesIn), new String(result.array()));
        int bytesRead = randomAccessFile.readObject(0);
        assertEquals(5, bytesRead);

        String content = "";
        for (int i = 0; i < 6000; i++)
            content = content.concat("Hello World ");
        bytesIn = content.getBytes();
        assertEquals(bytesIn.length, 72000);

        length = bytesIn.length;

        randomAccessFile.write(bytesIn, 0, length);

        int res = randomAccessFile.readObject(0);
        assertEquals(65536, res);
        res = randomAccessFile.readObject(1);
        assertEquals(6464, res);
    }

}
