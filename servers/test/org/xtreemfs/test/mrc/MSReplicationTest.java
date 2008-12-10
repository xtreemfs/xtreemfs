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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.RequestController;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class MSReplicationTest extends TestCase {

    private static final boolean DEBUG = true;

    private static final String TEST_DIR = "/tmp/xtreemfs-test";

    private RequestController mrc1;

    private RequestController mrc2;

    private org.xtreemfs.dir.RequestController dir;

    private MRCClient client;

    private MRCConfig mrcCfg1;

    private MRCConfig mrcCfg2;

    private DIRConfig dsCfg;

    private InetSocketAddress mrc1Address;

    private InetSocketAddress mrc2Address;

    public MSReplicationTest(String testName) {
        super(testName);
        //Logging.start(SetupUtils.DEBUG_LEVEL);
        Logging.start(Logging.LEVEL_DEBUG);
    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        dsCfg = SetupUtils.createDIRConfig();

        mrcCfg1 = SetupUtils.createMRC1Config();
        mrc1Address = SetupUtils.getMRC1Addr();

        mrcCfg2 = SetupUtils.createMRC2Config();
        mrc2Address = SetupUtils.getMRC2Addr();

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        // start services
        dir = new org.xtreemfs.dir.RequestController(dsCfg);
        dir.startup();

        mrc1 = new RequestController(mrcCfg1);
        mrc1.startup();
        mrc2 = new RequestController(mrcCfg2);
        mrc2.startup();

        client = SetupUtils.createMRCClient(10000);
    }

    protected void tearDown() throws Exception {
        // shut down all services
        if (mrc1 != null)
            mrc1.shutdown();
        if (mrc2 != null)
            mrc2.shutdown();
        client.shutdown();
        dir.shutdown();

        client.waitForShutdown();
    }

    public void testReplication() throws Exception {

        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient.generateStringList("groupG"));
        final String volumeName = "testVolume";

        client.createVolume(mrc1Address, volumeName, authString);

        client.createFile(mrc1Address, "testVolume/DUMMY", authString);

        RPCResponse<List<Object>> mrcr = client.sendRPC(mrc1Address, ".Rinfo",
                null, null, null);
        List<Object> info = mrcr.get();
        mrcr.freeBuffers();

        String sliceID = null;

        for (Object tmp : info) {
            Map<String, Object> slice = (Map) tmp;
//            System.out.println("info: " + slice.get("volumeName"));
            if (slice.get("volumeName").equals(volumeName))
                sliceID = (String) slice.get("sliceID");

        }
        assertNotNull(sliceID);

        List<Object> args = new LinkedList();
        args.add(sliceID);
        List<Object> slaves = new LinkedList();
        slaves.add(mrc2Address.getHostName() + ":" + mrc2Address.getPort());
        args.add(slaves);
        args.add(false);
        mrcr = client.sendRPC(mrc1Address, ".RnewMasterSlice", args, null, null);
        mrcr.waitForResponse();
        mrcr.freeBuffers();

        // wait a sec because the master is setting up the slaves in async mode
        Thread.sleep(1000);
//        System.out.println("wait done");
        List<String> entries = client.readDir(mrc2Address, "testVolume",
                authString);
//        for (String tmp : entries)
//            System.out.println("entry: " + tmp);

        assertTrue(entries.contains("DUMMY"));

    }

    public void testSlaveRemoteReplay() throws Exception {

        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient.generateStringList("groupG"));
        final String volumeName = "testVolume";

        client.createVolume(mrc1Address, volumeName, authString);

        client.createFile(mrc1Address, "testVolume/DUMMY", authString);

        RPCResponse<List<Object>> mrcr = client.sendRPC(mrc1Address, ".Rinfo",
                null, null, null);
        List<Object> info = mrcr.get();
        mrcr.freeBuffers();

        String sliceID = null;

        for (Object tmp : info) {
            Map<String, Object> slice = (Map) tmp;
//            System.out.println("info: " + slice.get("volumeName"));
            if (slice.get("volumeName").equals(volumeName))
                sliceID = (String) slice.get("sliceID");

        }
        assertNotNull(sliceID);

        List<Object> args = new LinkedList();
        args.add(sliceID);
        List<Object> slaves = new LinkedList();
        slaves.add(mrc2Address.getHostName() + ":" + mrc2Address.getPort());
        args.add(slaves);
        args.add(false);
        mrcr = client.sendRPC(mrc1Address, ".RnewMasterSlice", args, null, null);
        mrcr.waitForResponse();
        mrcr.freeBuffers();

        // wait a sec because the master is setting up the slaves in async mode
        Thread.sleep(1000);
        List<String> entries = client.readDir(mrc2Address, "testVolume",
                authString);
//        for (String tmp : entries)
//            System.out.println("entry: " + tmp);

        assertTrue(entries.contains("DUMMY"));

        // shut down slave
        mrc2.shutdown();
        mrc2 = null;

        // create a new entry
        client.createFile(mrc1Address, "testVolume/DUMMY_TWO", authString);
        client.createFile(mrc1Address, "testVolume/DUMMY_THREE", authString);
        client.createFile(mrc1Address, "testVolume/DUMMY_FOUR", authString);

        Thread.sleep(200);

        // start up the slave again
        mrc2 = new RequestController(mrcCfg2);
        mrc2.startup();

        // check if the slave has recovered all entries it missied while offline
        entries = client.readDir(mrc2Address, "testVolume", authString);
//        for (String tmp : entries)
//            System.out.println("entry: " + tmp);

        assertTrue(entries.contains("DUMMY_TWO"));
        assertTrue(entries.contains("DUMMY_THREE"));
        assertTrue(entries.contains("DUMMY_FOUR"));

    }

    public void testMasterOffline() throws Exception {

        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient.generateStringList("groupG"));
        final String volumeName = "testVolume";

        client.createVolume(mrc1Address, volumeName, authString);

        client.createFile(mrc1Address, "testVolume/DUMMY", authString);

        RPCResponse<List<Object>> mrcr = client.sendRPC(mrc1Address, ".Rinfo",
                null, null, null);
        List<Object> info = mrcr.get();
        mrcr.freeBuffers();

        String sliceID = null;

        for (Object tmp : info) {
            Map<String, Object> slice = (Map) tmp;
//            System.out.println("info: " + slice.get("volumeName"));
            if (slice.get("volumeName").equals(volumeName))
                sliceID = (String) slice.get("sliceID");

        }
        assertNotNull(sliceID);

        List<Object> args = new LinkedList();
        args.add(sliceID);
        List<Object> slaves = new LinkedList();
        slaves.add(mrc2Address.getHostName() + ":" + mrc2Address.getPort());
        args.add(slaves);
        args.add(false);
        mrcr = client.sendRPC(mrc1Address, ".RnewMasterSlice", args, null, null);
        mrcr.waitForResponse();
        mrcr.freeBuffers();

        // wait a sec because the master is setting up the slaves in async mode
        Thread.sleep(1000);
        List<String> entries = client.readDir(mrc2Address, "testVolume",
                authString);
//        for (String tmp : entries)
//            System.out.println("entry: " + tmp);

        assertTrue(entries.contains("DUMMY"));

        // shut down slave
        mrc2.shutdown();
        mrc2 = null;

        // shut down master
        mrc1.shutdown();
        mrc1 = null;

        Thread.sleep(1000);

        // start up the slave again
        mrc2 = new RequestController(mrcCfg2);
        mrc2.startup();

        // now the status of the slice should be OFFLINE
        RPCResponse<List<Object>> resp = client.sendRPC(mrc2Address, ".Rinfo",
                authString, null, null);
        List<Object> slices = resp.get();
        resp.freeBuffers();
        for (Object tmp : slices) {
            Map<String, Object> sl = (Map) tmp;
            if (sl.get("sliceID").equals(sliceID)) {
//                System.out.println("STATUS is " + sl.get("status"));
                assertTrue(sl.get("status").equals("OFFLINE"));
                break;
            }
        }
        Thread.sleep(1000);
//        System.out.println("yabba bla");

    }

    public void testSlaveOnline() throws Exception {

        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient.generateStringList("groupG"));
        final String volumeName = "testVolume";

        client.createVolume(mrc1Address, volumeName, authString);

        client.createFile(mrc1Address, "testVolume/DUMMY", authString);

        RPCResponse<List<Object>> mrcr = client.sendRPC(mrc1Address, ".Rinfo",
                null, null, null);
        List<Object> info = mrcr.get();
        mrcr.freeBuffers();

        String sliceID = null;

        for (Object tmp : info) {
            Map<String, Object> slice = (Map) tmp;
//            System.out.println("info: " + slice.get("volumeName"));
            if (slice.get("volumeName").equals(volumeName))
                sliceID = (String) slice.get("sliceID");

        }
        assertNotNull(sliceID);

        List<Object> args = new LinkedList();
        args.add(sliceID);
        List<Object> slaves = new LinkedList();
        slaves.add(mrc2Address.getHostName() + ":" + mrc2Address.getPort());
        args.add(slaves);
        args.add(false);
        mrcr = client.sendRPC(mrc1Address, ".RnewMasterSlice", args, null, null);
        mrcr.waitForResponse();
        mrcr.freeBuffers();

        // wait a sec because the master is setting up the slaves in async mode
        Thread.sleep(1000);
        List<String> entries = client.readDir(mrc2Address, "testVolume",
                authString);
//        for (String tmp : entries)
//            System.out.println("entry: " + tmp);

        assertTrue(entries.contains("DUMMY"));

        // create a new entry
        client.createFile(mrc1Address, "testVolume/DUMMY_TWO", authString);
        client.createFile(mrc1Address, "testVolume/DUMMY_THREE", authString);
        client.createFile(mrc1Address, "testVolume/DUMMY_FOUR", authString);

        Thread.sleep(200);

        // check if the slave has recovered all entries it missied while offline
        entries = client.readDir(mrc2Address, "testVolume", authString);
//        for (String tmp : entries)
//            System.out.println("entry: " + tmp);

        assertTrue(entries.contains("DUMMY_TWO"));
        assertTrue(entries.contains("DUMMY_THREE"));
        assertTrue(entries.contains("DUMMY_FOUR"));

    }

    public void testUnreplication() throws Exception {

        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient.generateStringList("groupG"));
        final String volumeName = "testVolume";

        client.createVolume(mrc1Address, volumeName, authString);

        client.createFile(mrc1Address, "testVolume/DUMMY", authString);

        RPCResponse<List<Object>> mrcr = client.sendRPC(mrc1Address, ".Rinfo",
                null, null, null);
        List<Object> info = mrcr.get();
        mrcr.freeBuffers();

        String sliceID = null;

        for (Object tmp : info) {
            Map<String, Object> slice = (Map) tmp;
//            System.out.println("info: " + slice.get("volumeName"));
            if (slice.get("volumeName").equals(volumeName))
                sliceID = (String) slice.get("sliceID");

        }
        assertNotNull(sliceID);

        List<Object> args = new LinkedList();
        args.add(sliceID);
        List<Object> slaves = new LinkedList();
        slaves.add(mrc2Address.getHostName() + ":" + mrc2Address.getPort());
        args.add(slaves);
        args.add(false);
        mrcr = client.sendRPC(mrc1Address, ".RnewMasterSlice", args, null, null);
        mrcr.waitForResponse();
        mrcr.freeBuffers();

        // wait a sec because the master is setting up the slaves in async mode
        Thread.sleep(1000);
//        System.out.println("wait done");
        List<String> entries = client.readDir(mrc2Address, "testVolume",
                authString);
//        for (String tmp : entries)
//            System.out.println("entry: " + tmp);

        assertTrue(entries.contains("DUMMY"));

        args = new LinkedList();
        args.add(sliceID);
        mrcr = client.sendRPC(mrc1Address, ".RnoReplication", args, null, null);
        mrcr.waitForResponse();
        mrcr.freeBuffers();
        mrcr = client.sendRPC(mrc2Address, ".RnoReplication", args, null, null);
        mrcr.waitForResponse();
        mrcr.freeBuffers();

        Thread.sleep(200);
        client.createFile(mrc1Address, "testVolume/DUMMY_TWO", authString);
        Thread.sleep(200);

        entries = client.readDir(mrc2Address, "testVolume", authString);
//        for (String tmp : entries)
//            System.out.println("entry: " + tmp);

        assertFalse(entries.contains("DUMMY_TWO"));

    }

    public static void main(String[] args) {
        TestRunner.run(MSReplicationTest.class);
    }

}
