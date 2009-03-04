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
 * AUTHORS: Nele Andersen (ZIB), Björn Kolbeck (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.test.io;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class RandomAccessFileTest extends TestCase {
    RandomAccessFile randomAccessFile;

    private MRCRequestDispatcher mrc1;

    private DIRRequestDispatcher dirService;

    private MRCConfig mrcCfg1;

    private OSDConfig osdConfig1, osdConfig2;

    private DIRConfig dsCfg;

    private OSD osd1, osd2;

    private InetSocketAddress mrc1Address;

    private String authString;

    private String volumeName;

    private TestEnvironment testEnv;

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
        dirService = new org.xtreemfs.dir.DIRRequestDispatcher(dsCfg);
        dirService.startup();

        // start the OSDs
        osd1 = new OSD(osdConfig1);
        osd2 = new OSD(osdConfig2);

        // start MRC
        mrc1 = new MRCRequestDispatcher(mrcCfg1);
        mrc1.startup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.DIR_CLIENT,
        TestEnvironment.Services.MRC_CLIENT,TestEnvironment.Services.TIME_SYNC,TestEnvironment.Services.UUID_RESOLVER
        });
        testEnv.start();

        String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));

        volumeName = "testVolume";

        // create a volume (no access control)
        testEnv.getMrcClient().createVolume(mrc1Address, volumeName, authString);

        // create some files and directories
        testEnv.getMrcClient().createDir(mrc1Address, volumeName + "/myDir", authString);

        for (int i = 0; i < 10; i++)
            testEnv.getMrcClient().createFile(mrc1Address, volumeName + "/myDir/test" + i + ".txt", authString);

    }

    public void tearDown() throws Exception {
        mrc1.shutdown();
        osd1.shutdown();
        osd2.shutdown();
        dirService.shutdown();


        testEnv.shutdown();

        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }

    public void testReadAndWrite() throws Exception {
        randomAccessFile = new RandomAccessFile("w", mrc1Address, volumeName + "/myDir/test1.txt", testEnv.getSpeedy());

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
        randomAccessFile = new RandomAccessFile("w", mrc1Address, volumeName + "/myDir/test1.txt", testEnv.getSpeedy());

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

    public void testReplicaCreationAndRemoval() throws Exception {
        randomAccessFile = new RandomAccessFile("cw", mrc1Address, volumeName + "/myDir/test2.txt", testEnv.getSpeedy());

        // // set file read-only
        // randomAccessFile.setReadOnly(true);
        //        
        // // add a replica
        // List<ServiceUUID> replica1 = new ArrayList<ServiceUUID>();
        // while (true) {
        // // TODO: get a list of "free" OSDs
        // try {
        // randomAccessFile.addReplica(replica1);
        // break;
        // } catch (Exception e) {
        // wait(1000);
        // }
        // }
        // // check
        // assertEquals(2, randomAccessFile.getLocations().getNumberOfReplicas());
        // if(!findReplicaInLocations(replica1))
        // fail("Added OSD-list not found in location-list.");
        //        
        // // add a second replica
        // List<ServiceUUID> replica2 = new ArrayList<ServiceUUID>();
        // // TODO: get a list of "free" OSDs
        // randomAccessFile.addReplica(replica2);
        // // check
        // assertEquals(3, randomAccessFile.getLocations().getNumberOfReplicas());
        // if(!findReplicaInLocations(replica1))
        // fail("Added 'old' OSD-list not found in location-list.");
        // if(!findReplicaInLocations(replica2))
        // fail("Added OSD-list not found in location-list.");
        //
        // // remove the first replica
        // randomAccessFile.removeReplica(replica1);
        // // check
        // assertEquals(2, randomAccessFile.getLocations().getNumberOfReplicas());
        // if(findReplicaInLocations(replica1))
        // fail("Removed OSD-list found in location-list.");
        // if(!findReplicaInLocations(replica2))
        // fail("Added OSD-list not found in location-list.");
        //        
        // // try to remove read-only flag
        // try {
        // randomAccessFile.setReadOnly(false);
        // fail("File must not marked as read-only, because replicas exists.");
        // } catch (Exception e) {
        // // do nothing
        // }
        //
        // // remove the last replica
        // randomAccessFile.removeReplica(replica2);
        // // check
        // assertEquals(1, randomAccessFile.getLocations().getNumberOfReplicas());
        // if(findReplicaInLocations(replica2))
        // fail("Removed OSD-list found in location-list.");
        //        
        // // try to remove read-only flag
        // try {
        // randomAccessFile.setReadOnly(false);
        // } catch (Exception e) {
        // fail("File should be able to marked as read-only, because replicas exists.");
        // }
    }

    /**
     * @param osds
     */
    private boolean findReplicaInLocations(List<ServiceUUID> osds) {
        boolean found = false;
        for (Location loc : randomAccessFile.getLocations()) {
            if (osds.equals(loc.getOSDs())) {
                assertEquals(osds, loc.getOSDs());
                found = true;
                break;
            }
        }
        return found;
    }
    
    public static void main(String[] args) {
        TestRunner.run(RandomAccessFileTest.class);
    }
}
