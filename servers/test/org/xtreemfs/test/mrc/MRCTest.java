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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.osdselection.RandomSelectionPolicy;
import org.xtreemfs.mrc.slices.DefaultPartitioningPolicy;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.ac.POSIXFileAccessPolicy;
import org.xtreemfs.new_mrc.ac.VolumeACLFileAccessPolicy;
import org.xtreemfs.new_mrc.ac.YesToAnyoneFileAccessPolicy;
import org.xtreemfs.test.SetupUtils;

/**
 * XtreemFS integration test case.
 * 
 * @author stender
 */
public class MRCTest extends TestCase {
    
    private MRCRequestDispatcher               mrc1;
    
    private org.xtreemfs.dir.RequestController dirService;
    
    private MRCClient                          client;
    
    private MRCConfig                          mrcCfg1;
    
    private DIRConfig                          dsCfg;
    
    private InetSocketAddress                  mrc1Address;
    
    public MRCTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        dsCfg = SetupUtils.createDIRConfig();
        
        mrcCfg1 = SetupUtils.createMRC1Config();
        mrc1Address = SetupUtils.getMRC1Addr();
        
        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);
        
        FSUtils.delTree(testDir);
        testDir.mkdirs();
        
        // start the Directory Service
        dirService = new org.xtreemfs.dir.RequestController(dsCfg);
        dirService.startup();
        
        // register an OSD at the directory service (needed in order to assign
        // it to a new file on 'open')
        DIRClient dirClient = SetupUtils.createDIRClient(1000);
        String authString = NullAuthProvider.createAuthString("mockUpOSD", "mockUpOSD");
        Map<String, Object> data = RPCClient.generateMap("type", "OSD", "free", "1000000000",
            "total", "1000000000", "load", "0", "prot_versions", VersionManagement
                    .getSupportedProtVersAsString(), "totalRAM", "1000000000", "usedRAM", "0");
        TimeSync.initialize(dirClient, mrcCfg1.getRemoteTimeSync(), mrcCfg1.getLocalClockRenew(),
            NullAuthProvider.createAuthString(mrcCfg1.getUUID().toString(), mrcCfg1.getUUID()
                    .toString()));
        try {
            RPCResponse response = dirClient.registerEntity("mockUpOSD", data, 1, authString);
            response.waitForResponse();
            response.freeBuffers();
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
        } finally {
            dirClient.shutdown();
            dirClient.waitForShutdown();
        }
        
        // start two MRCs
        mrc1 = new MRCRequestDispatcher(mrcCfg1);
        mrc1.startup();
        
        client = SetupUtils.createMRCClient(10000);
    }
    
    protected void tearDown() throws Exception {
        
        // shut down all services
        mrc1.shutdown();
        client.shutdown();
        dirService.shutdown();
        
        client.waitForShutdown();
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
        
    }
    
    public void testCreateDelete() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String rootAuthString = NullAuthProvider.createAuthString("root", MRCClient
                .generateStringList("root"));
        final String volumeName = "testVolume";
        
        // create and delete a volume
        client.createVolume(mrc1Address, volumeName, authString);
        Map<String, String> localVols = client.getLocalVolumes(mrc1Address, authString);
        assertEquals(1, localVols.size());
        assertEquals(volumeName, localVols.values().iterator().next());
        client.deleteVolume(mrc1Address, volumeName, authString);
        localVols = client.getLocalVolumes(mrc1Address, authString);
        assertEquals(0, localVols.size());
        
        // create a volume (no access control)
        client.createVolume(mrc1Address, volumeName, authString);
        
        // create some files and directories
        client.createDir(mrc1Address, volumeName + "/myDir", authString);
        client.createDir(mrc1Address, volumeName + "/anotherDir", authString);
        
        for (int i = 0; i < 10; i++)
            client.createFile(mrc1Address, volumeName + "/myDir/test" + i + ".txt", authString);
        
        try {
            client.createFile(mrc1Address, volumeName, authString);
            fail("missing filename");
        } catch (Exception exc) {
        }
        
        try {
            client.createFile(mrc1Address, volumeName + "/myDir/test0.txt", authString);
            fail("duplicate file creation");
        } catch (Exception exc) {
        }
        
        try {
            client.createFile(mrc1Address, volumeName + "/myDir/test0.txt/bla.txt", authString);
            fail("file in file creation");
        } catch (Exception exc) {
        }
        
        // test 'readDir' and 'stat'
        
        List<String> list = client.readDir(mrc1Address, volumeName, authString);
        assertEquals(2, list.size());
        list = client.readDir(mrc1Address, volumeName + "/myDir", authString);
        assertEquals(10, list.size());
        
        Map<String, Object> statInfo = client.stat(mrc1Address, volumeName + "/myDir/test2.txt",
            true, true, true, authString);
        assertNotNull(statInfo.get("fileId"));
        assertEquals("userXY", statInfo.get("ownerId"));
        assertEquals("1", statInfo.get("objType").toString());
        assertEquals("0", statInfo.get("size").toString());
        assertTrue(((Long) statInfo.get("ctime")) > 0);
        assertEquals("511", statInfo.get("posixAccessMode").toString());
        assertNull(statInfo.get("linkTarget"));
        
        // test 'delete'
        
        client.delete(mrc1Address, volumeName + "/myDir/test3.txt", authString);
        client.delete(mrc1Address, volumeName + "/anotherDir", authString);
    }
    
    public void testUserAttributes() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String volumeName = "testVolume";
        final long accessMode = 511; // rwxrwxrwx
        
        client.createVolume(mrc1Address, volumeName, authString);
        
        // add some user attributes
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("key1", "quark");
        attrs.put("key2", "quatsch");
        client.createFile(mrc1Address, volumeName + "/test.txt", attrs, null, accessMode,
            authString);
        
        Map<String, Object> attrs2 = new HashMap<String, Object>();
        attrs2.put("myAttr", "171");
        attrs2.put("key1", "blub");
        client.setXAttrs(mrc1Address, volumeName + "/test.txt", attrs2, authString);
        
        Map<String, Object> attrs3 = (Map<String, Object>) client.stat(mrc1Address,
            volumeName + "/test.txt", false, true, false, authString).get("xAttrs");
        assertEquals("171", attrs3.get("myAttr"));
        
        String val = client.getXAttr(mrc1Address, volumeName + "/test.txt", "key1", authString);
        assertEquals("blub", val);
        
        client.createFile(mrc1Address, volumeName + "/test2.txt", authString);
        client.setXAttrs(mrc1Address, volumeName + "/test2.txt", attrs, authString);
        
        // delete some user attributes
        Map<String, Object> keys = new HashMap<String, Object>();
        keys.put("key2", null);
        client.setXAttrs(mrc1Address, volumeName + "/test2.txt", keys, authString);
        attrs3 = (Map<String, Object>) client.stat(mrc1Address, volumeName + "/test2.txt", false,
            true, false, authString).get("xAttrs");
        assertEquals("quark", attrs3.get("key1"));
        
        keys.put("key1", null);
        client.setXAttrs(mrc1Address, volumeName + "/test2.txt", keys, authString);
        attrs3 = (Map<String, Object>) client.stat(mrc1Address, volumeName + "/test2.txt", false,
            true, false, authString).get("xAttrs");
        assertNull(attrs3.get("key1"));
        
        // retrieve a system attribute
        String sysAttr = client.getXAttr(mrc1Address, volumeName + "/test.txt",
            "xtreemfs.object_type", authString);
        assertEquals("1", sysAttr);
    }
    
    public void testSymlink() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String volumeName = "testVolume";
        
        client.createVolume(mrc1Address, volumeName, authString);
        client.createFile(mrc1Address, volumeName + "/test.txt", authString);
        
        // create and test a symbolic link
        
        client.createSymbolicLink(mrc1Address, volumeName + "/testAlias.txt", volumeName
            + "/test.txt", authString);
        Map<String, Object> statInfo = client.stat(mrc1Address, volumeName + "/testAlias.txt",
            false, false, false, authString);
        assertEquals(3L, statInfo.get("objType"));
        assertEquals(volumeName + "/test.txt", statInfo.get("linkTarget"));
    }
    
    public void testHardLink() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String volumeName = "testVolume";
        
        client.createVolume(mrc1Address, volumeName, authString);
        
        // create a new file
        client.createFile(mrc1Address, volumeName + "/test1.txt", authString);
        
        // create a new link to the file
        client.createLink(mrc1Address, volumeName + "/test2.txt", volumeName + "/test1.txt",
            authString);
        
        // check whether both links refer to the same file
        Map<String, Object> statInfo1 = client.stat(mrc1Address, volumeName + "/test1.txt", false,
            false, false, authString);
        Map<String, Object> statInfo2 = client.stat(mrc1Address, volumeName + "/test2.txt", false,
            false, false, authString);
        assertEquals(statInfo1.get("fileId"), statInfo2.get("fileId"));
        assertEquals(2l, statInfo1.get("linkCount"));
        
        // delete both files
        client.delete(mrc1Address, volumeName + "/test1.txt", authString);
        assertEquals(1l, client.stat(mrc1Address, volumeName + "/test2.txt", false, false, false,
            authString).get("linkCount"));
        client.delete(mrc1Address, volumeName + "/test2.txt", authString);
        
        try {
            client.stat(mrc1Address, volumeName + "/test1.txt", false, false, false, authString);
            fail("file should not exist anymore");
        } catch (Exception exc) {
        }
        
        try {
            client.stat(mrc1Address, volumeName + "/test2.txt", false, false, false, authString);
            fail("file should not exist anymore");
        } catch (Exception exc) {
        }
        
        // create two links to a directory
        client.createDir(mrc1Address, volumeName + "/testDir1", authString);
        try {
            client.createLink(mrc1Address, volumeName + "/testDir1/testDir2", volumeName
                + "/testDir1", authString);
            fail("links to directories should not be allowed");
        } catch (Exception exc) {
        }
    }
    
    public void testReplicas() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String volumeName = "testVolume";
        final long accessMode = 511; // rwxrwxrwx
        
        client.createVolume(mrc1Address, volumeName, RandomSelectionPolicy.POLICY_ID,
            getDefaultStripingPolicy(), POSIXFileAccessPolicy.POLICY_ID,
            DefaultPartitioningPolicy.POLICY_ID, null, authString);
        client.createFile(mrc1Address, volumeName + "/test.txt", authString);
        Map<String, Object> attrs = RPCClient.generateMap("xtreemfs.read_only", true);
        client.setXAttrs(mrc1Address, volumeName + "/test.txt", attrs, authString);
        
        // test adding and retrieval of replicas
        Map<String, Object> statInfo = client.stat(mrc1Address, volumeName + "/test.txt", true,
            false, false, authString);
        String globalFileId = (String) statInfo.get("fileId");
        assertNotNull(globalFileId);
        assertNull(statInfo.get("replicas"));
        
        List<String> osdList = new ArrayList<String>();
        osdList.add("177.127.77.90:7477");
        client.addReplica(mrc1Address, globalFileId, null, osdList, authString);
        statInfo = client
                .stat(mrc1Address, volumeName + "/test.txt", true, false, true, authString);
        
        assertEquals(2, ((List<Object>) ((List<Object>) ((List<Object>) statInfo.get("replicas"))
                .get(0)).get(0)).size());
        assertEquals("177.127.77.90:7477",
            ((List<Object>) ((List<Object>) ((List<Object>) ((List<Object>) statInfo
                    .get("replicas")).get(0)).get(0)).get(1)).get(0));
    }
    
    public void testOpen() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String volumeName = "testVolume";
        
        client.createVolume(mrc1Address, volumeName, RandomSelectionPolicy.POLICY_ID,
            getDefaultStripingPolicy(), POSIXFileAccessPolicy.POLICY_ID,
            DefaultPartitioningPolicy.POLICY_ID, null, authString);
        client.createFile(mrc1Address, volumeName + "/test.txt", authString);
        
        // test capabilities
        Map<String, String> capability = client.open(mrc1Address, volumeName + "/test.txt", "c",
            authString);
        assertNotNull(capability.get("X-Locations"));
        assertNotNull(capability.get("X-Capability"));
        
        capability = client.open(mrc1Address, volumeName + "/test.txt", "w", authString);
        assertNotNull(capability.get("X-Capability"));
        assertNotNull(capability.get("X-Locations"));
        
        Map<String, Object> acl = new HashMap<String, Object>();
        acl.put("user:", 128); // sr
        client.createFile(mrc1Address, volumeName + "/test2.txt", null, getDefaultStripingPolicy(),
            0, authString);
        client.setACLEntries(mrc1Address, volumeName + "/test2.txt", acl, authString);
        
        Map<String, Object> statInfo = client.stat(mrc1Address, volumeName + "/test2.txt", true,
            false, true, authString);
        acl = (Map<String, Object>) statInfo.get("acl");
        assertTrue(acl.containsKey("user:"));
        assertTrue(acl.containsKey("group:") || acl.containsKey("mask:"));
        assertTrue(acl.containsKey("other:"));
        
        capability = client.open(mrc1Address, volumeName + "/test2.txt", "sr", authString);
        assertNotNull(capability.get("X-Locations"));
        assertNotNull(capability.get("X-Capability"));
        
        try {
            capability = client.open(mrc1Address, volumeName + "/test2.txt", "r", authString);
            fail();
        } catch (HttpErrorException exc) {
            assertEquals(420, exc.getStatusCode());
        }
        
        // symlinks and directories ...
        client.createDir(mrc1Address, volumeName + "/dir", authString);
        client.createSymbolicLink(mrc1Address, volumeName + "/link", volumeName + "/test2.txt",
            authString);
        client.createSymbolicLink(mrc1Address, volumeName + "/link2", "somewhere", authString);
        
        try {
            client.open(mrc1Address, volumeName + "/dir", "sr", authString);
            fail("opened directory");
        } catch (HttpErrorException exc) {
            assertEquals(420, exc.getStatusCode());
        }
        
        capability = client.open(mrc1Address, volumeName + "/link", "sr", authString);
        assertNotNull(capability.get(HTTPHeaders.HDR_XLOCATIONS));
        
        String xCapStr = (String) capability.get(HTTPHeaders.HDR_XCAPABILITY);
        assertNotNull(xCapStr);
        List<Object> xCap = (List<Object>) JSONParser.parseJSON(new JSONString(xCapStr));
        
        // wait one second before renewing the capability
        Thread.sleep(1000);
        
        // test renewing a capability
        Map<String, String> newCapability = client.renew(mrc1Address, capability, authString);
        
        String newXCapStr = (String) newCapability.get(HTTPHeaders.HDR_XCAPABILITY);
        assertNotNull(newXCapStr);
        List<Object> newXCap = (List<Object>) JSONParser.parseJSON(new JSONString(newXCapStr));
        
        assertEquals(xCap.get(0), newXCap.get(0));
        assertEquals(xCap.get(1), newXCap.get(1));
        assertTrue((Long) xCap.get(2) < (Long) newXCap.get(2));
        assertEquals(xCap.get(3), newXCap.get(3));
        assertEquals(xCap.get(4), newXCap.get(4));
        assertFalse(xCap.get(5).equals(newXCap.get(5)));
        
        try {
            capability = client.open(mrc1Address, volumeName + "/link2", "r", authString);
            fail("should have been redirected");
        } catch (Exception exc) {
        }
        
    }
    
    public void testMove() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String volumeName = "testVolume";
        
        client.createVolume(mrc1Address, volumeName, authString);
        
        client.createFile(mrc1Address, volumeName + "/test.txt", authString);
        client.createFile(mrc1Address, volumeName + "/blub.txt", authString);
        client.createDir(mrc1Address, volumeName + "/mainDir", authString);
        client.createDir(mrc1Address, volumeName + "/mainDir/subDir", authString);
        client.createDir(mrc1Address, volumeName + "/mainDir/subDir/newDir", authString);
        
        assertTree(mrc1Address, authString, volumeName, volumeName + "/test.txt", volumeName
            + "/blub.txt", volumeName + "/mainDir", volumeName + "/mainDir/subDir", volumeName
            + "/mainDir/subDir/newDir");
        
        // move some files and directories
        
        // file -> none (create w/ different name)
        client.move(mrc1Address, volumeName + "/test.txt", volumeName + "/mainDir/bla.txt",
            authString);
        assertTree(mrc1Address, authString, volumeName, volumeName + "/mainDir/bla.txt", volumeName
            + "/blub.txt", volumeName + "/mainDir", volumeName + "/mainDir/subDir", volumeName
            + "/mainDir/subDir/newDir");
        
        // file -> file (overwrite)
        client.move(mrc1Address, volumeName + "/mainDir/bla.txt", volumeName + "/blub.txt",
            authString);
        assertTree(mrc1Address, authString, volumeName, volumeName + "/blub.txt", volumeName
            + "/mainDir", volumeName + "/mainDir/subDir", volumeName + "/mainDir/subDir/newDir");
        
        // file -> none (create w/ same name)
        client.move(mrc1Address, volumeName + "/blub.txt", volumeName + "/mainDir/blub.txt",
            authString);
        assertTree(mrc1Address, authString, volumeName, volumeName + "/mainDir/blub.txt",
            volumeName + "/mainDir", volumeName + "/mainDir/subDir", volumeName
                + "/mainDir/subDir/newDir");
        
        // file -> dir (invalid operation)
        try {
            client.move(mrc1Address, volumeName + "/mainDir/blub.txt", volumeName
                + "/mainDir/subDir", authString);
            fail("move file -> directory should not be possible");
        } catch (Exception exc) {
        }
        
        // file -> file (same path, should have no effect)
        client.move(mrc1Address, volumeName + "/mainDir/blub.txt",
            volumeName + "/mainDir/blub.txt", authString);
        assertTree(mrc1Address, authString, volumeName, volumeName + "/mainDir/blub.txt",
            volumeName + "/mainDir", volumeName + "/mainDir/subDir", volumeName
                + "/mainDir/subDir/newDir");
        
        // file -> file (same directory)
        client.move(mrc1Address, volumeName + "/mainDir/blub.txt", volumeName
            + "/mainDir/blub2.txt", authString);
        assertTree(mrc1Address, authString, volumeName, volumeName + "/mainDir/blub2.txt",
            volumeName + "/mainDir", volumeName + "/mainDir/subDir", volumeName
                + "/mainDir/subDir/newDir");
        
        // dir -> none (create w/ same name)
        client
                .move(mrc1Address, volumeName + "/mainDir/subDir", volumeName + "/subDir",
                    authString);
        assertTree(mrc1Address, authString, volumeName, volumeName + "/mainDir/blub2.txt",
            volumeName + "/mainDir", volumeName + "/subDir", volumeName + "/subDir/newDir");
        
        // dir -> dir (overwrite, should fail because of non-empty subdirectory)
        try {
            client.move(mrc1Address, volumeName + "/subDir", volumeName + "/mainDir", authString);
            fail("moved directory to non-empty directory");
        } catch (Exception exc) {
        }
        
        // dir -> dir (overwrite)
        client.delete(mrc1Address, volumeName + "/mainDir/blub2.txt", authString);
        client.move(mrc1Address, volumeName + "/subDir", volumeName + "/mainDir", authString);
        assertTree(mrc1Address, authString, volumeName, volumeName + "/mainDir", volumeName
            + "/mainDir/newDir");
        
        // dir -> volume (should fail because volume can't be overwritten)
        try {
            client.move(mrc1Address, volumeName + "/mainDir/newDir", volumeName, authString);
            fail("move overwrote volume");
        } catch (Exception exc) {
        }
        
        // dir -> invalid volume (should fail)
        try {
            client.move(mrc1Address, volumeName, "somewhere", authString);
            fail("moved to invalid volume");
        } catch (Exception exc) {
        }
        
        assertTree(mrc1Address, authString, volumeName, volumeName + "/mainDir", volumeName
            + "/mainDir/newDir");
    }
    
    public void testAccessControl() throws Exception {
        
        final String authString1 = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String authString2 = NullAuthProvider.createAuthString("userAB", MRCClient
                .generateStringList("groupA"));
        final String authString3 = NullAuthProvider.createAuthString("userZZ", MRCClient
                .generateStringList("groupY"));
        final String noACVolumeName = "noACVol";
        final String volACVolumeName = "volACVol";
        final String posixVolName = "acVol";
        
        Map<String, Object> acl = new HashMap<String, Object>();
        acl.put("default", 0);
        acl.put("userXY", (1 << 0) | (1 << 1)); // read, write
        acl.put("userAB", 1); // read
        
        // NO ACCESS CONTROL
        
        // create a volume
        client.createVolume(mrc1Address, noACVolumeName, RandomSelectionPolicy.POLICY_ID, null,
            YesToAnyoneFileAccessPolicy.POLICY_ID, DefaultPartitioningPolicy.POLICY_ID, null,
            authString1);
        
        // test chown
        client.createFile(mrc1Address, noACVolumeName + "/chownTestFile", authString1);
        client.changeOwner(mrc1Address, noACVolumeName + "/chownTestFile", "newUser", "newGroup",
            authString1);
        Map<String, Object> stat = client.stat(mrc1Address, noACVolumeName + "/chownTestFile",
            false, false, false, authString3);
        assertEquals("newUser", stat.get("ownerId"));
        assertEquals("newGroup", stat.get("groupId"));
        client.delete(mrc1Address, noACVolumeName + "/chownTestFile", authString3);
        
        // create a new directory: should succeed
        client.createDir(mrc1Address, noACVolumeName + "/newDir", null, 0, authString1);
        
        // create a new file inside the dir: should succeed (in spite of
        // having set an empty ACL on the parent directory)
        client.createFile(mrc1Address, noACVolumeName + "/newDir/newFile", authString2);
        
        final String someone = NullAuthProvider.createAuthString("someone", MRCClient
                .generateStringList("somegroup"));
        
        assertTrue(client.checkAccess(mrc1Address, noACVolumeName + "/newDir/newFile", "rwx",
            someone));
        
        // VOLUME ACLs
        
        // create a volume
        client.createVolume(mrc1Address, volACVolumeName, RandomSelectionPolicy.POLICY_ID, null,
            VolumeACLFileAccessPolicy.POLICY_ID, DefaultPartitioningPolicy.POLICY_ID, null,
            authString1);
        client.setACLEntries(mrc1Address, volACVolumeName, acl, authString1);
        
        // create a new directory: should succeed for 'authString1', fail
        // for 'authString2'
        client.createDir(mrc1Address, volACVolumeName + "/newDir", null, 0, authString1);
        
        try {
            client.createDir(mrc1Address, volACVolumeName + "/newDir2", authString2);
            fail("access should have been denied");
        } catch (Exception exc) {
        }
        
        // readdir: should succeed for both 'authString1' and 'authString2'
        // and fail for 'authString3'
        assertEquals(0, client.readDir(mrc1Address, volACVolumeName + "/newDir", authString1)
                .size());
        assertEquals(0, client.readDir(mrc1Address, volACVolumeName + "/newDir", authString2)
                .size());
        
        try {
            client.readDir(mrc1Address, volACVolumeName + "/newDir", authString3);
            fail("access should have been denied");
        } catch (HttpErrorException exc) {
        }
        
        // create a new file inside the dir: should succeed (in spite of
        // having set an empty ACL on the parent directory)
        client.createFile(mrc1Address, volACVolumeName + "/newDir/newFile", authString1);
        
        // POSIX ACLs
        
        // create a volume
        client
                .createVolume(mrc1Address, posixVolName, RandomSelectionPolicy.POLICY_ID, null,
                    POSIXFileAccessPolicy.POLICY_ID, DefaultPartitioningPolicy.POLICY_ID, null,
                    authString1);
        
        client.changeAccessMode(mrc1Address, posixVolName, 0700, authString1);
        
        // create a new directory: should succeed for 'authString1', fail
        // for 'authString2'
        client.createDir(mrc1Address, posixVolName + "/newDir", authString1);
        
        assertTrue(client.checkAccess(mrc1Address, posixVolName + "/newDir", "rwx", authString1));
        
        try {
            client.createDir(mrc1Address, posixVolName + "/newDir2", authString2);
            fail("access should have been denied");
        } catch (HttpErrorException exc) {
        }
        
        // retrieve the ACL
        Map<String, Object> statInfo = client.stat(mrc1Address, posixVolName + "/newDir", false,
            false, true, authString1);
        acl = (Map<String, Object>) statInfo.get("acl");
        assertEquals(0, acl.size());
        
        // try to change an ACL entry: should fail for 'authString2',
        // succeed for 'authString1'
        Map<String, Object> newEntries = new HashMap<String, Object>();
        newEntries.put("group:", 5);
        try {
            client.setACLEntries(mrc1Address, posixVolName + "/newDir", newEntries, authString2);
            fail("attempt to modify ACl as non-owner should have failed");
        } catch (HttpErrorException exc) {
        }
        
        newEntries.clear();
        newEntries.put("group:", 2);
        newEntries.put("mask:", 3);
        client.setACLEntries(mrc1Address, posixVolName + "/newDir", newEntries, authString1);
        
        statInfo = client.stat(mrc1Address, posixVolName + "/newDir", false, false, true,
            authString1);
        acl = (Map<String, Object>) statInfo.get("acl");
        assertEquals(4, acl.size());
        assertEquals(511L, acl.get("user:"));
        assertEquals(2L, acl.get("group:"));
        assertEquals(511L, acl.get("other:"));
        assertEquals(3L, acl.get("mask:"));
        
        // change the access mode
        client.changeAccessMode(mrc1Address, posixVolName + "/newDir", 0, authString1);
        statInfo = client.stat(mrc1Address, posixVolName + "/newDir", false, false, true,
            authString1);
        acl = (Map<String, Object>) statInfo.get("acl");
        assertEquals(4, acl.size());
        assertEquals(0L, acl.get("user:"));
        assertEquals(0L, acl.get("group:"));
        assertEquals(0L, acl.get("other:"));
        assertEquals(0L, acl.get("mask:"));
        
        // readdir on "/newDir": should fail for any user now
        try {
            client.readDir(mrc1Address, posixVolName + "/newDir", authString1);
            fail("access should have been denied");
        } catch (HttpErrorException exc) {
        }
        
        try {
            client.readDir(mrc1Address, posixVolName + "/newDir", authString2);
            fail("access should have been denied");
        } catch (HttpErrorException exc) {
        }
        
        // add an entry (and mask) for 'authString2' to 'newDir'
        newEntries.clear();
        newEntries.put("user:userAB", 511);
        newEntries.put("mask:", 511);
        client.setACLEntries(mrc1Address, posixVolName + "/newDir", newEntries, authString1);
        
        try {
            client.readDir(mrc1Address, posixVolName + "/newDir", authString2);
            fail("access should have been denied due to insufficient search permissions");
        } catch (HttpErrorException exc) {
        }
        
        // add an entry (and mask) for 'authString2' to volume
        client.setACLEntries(mrc1Address, posixVolName, newEntries, authString1);
        
        assertTrue(client.checkAccess(mrc1Address, posixVolName + "/newDir", "w", authString2));
        
        assertEquals(0, client.readDir(mrc1Address, posixVolName + "/newDir", authString2).size());
        
        client.removeACLEntries(mrc1Address, posixVolName + "/newDir", new ArrayList<Object>(
            newEntries.keySet()), authString1);
        
        try {
            client.readDir(mrc1Address, posixVolName + "/newDir", authString2);
            fail("access should have been denied due to insufficient search permissions");
        } catch (HttpErrorException exc) {
        }
        
        client.changeAccessMode(mrc1Address, posixVolName, 0005, authString1); // others
        // = rx
        assertEquals(1, client.readDir(mrc1Address, posixVolName, authString3).size());
        assertFalse(client.checkAccess(mrc1Address, posixVolName, "w", authString3));
        
        // create a POSIX ACL new volume and test "chmod"
        client.deleteVolume(mrc1Address, posixVolName, authString1);
        client
                .createVolume(mrc1Address, posixVolName, RandomSelectionPolicy.POLICY_ID, null,
                    POSIXFileAccessPolicy.POLICY_ID, DefaultPartitioningPolicy.POLICY_ID, null,
                    authString1);
        
        client
                .createFile(mrc1Address, posixVolName + "/someFile.txt", null, null, 224,
                    authString1);
        statInfo = client.stat(mrc1Address, posixVolName + "/someFile.txt", false, false, false,
            authString1);
        long accessMode = (Long) statInfo.get("posixAccessMode");
        assertEquals(224, accessMode);
        client.changeAccessMode(mrc1Address, posixVolName + "/someFile.txt", accessMode & 192,
            authString1);
        statInfo = client.stat(mrc1Address, posixVolName + "/someFile.txt", false, false, false,
            authString1);
        accessMode = (Long) statInfo.get("posixAccessMode");
        assertEquals(192, accessMode);
        
        // make root directory accessible for anyone
        client.changeAccessMode(mrc1Address, posixVolName, 511, authString1);
        
        // create a new directory w/ search access for anyone
        client.createDir(mrc1Address, posixVolName + "/stickyDir", null, 511, authString1);
        
        // create and delete/rename a file w/ different user IDs: this should
        // work
        client.createFile(mrc1Address, posixVolName + "/stickyDir/newfile.txt", authString2);
        client.delete(mrc1Address, posixVolName + "/stickyDir/newfile.txt", authString1);
        client.createFile(mrc1Address, posixVolName + "/stickyDir/newfile.txt", authString2);
        client.move(mrc1Address, posixVolName + "/stickyDir/newfile.txt", posixVolName
            + "/stickyDir/newfile2.txt", authString1);
        
        // set sticky bit; now, only the owner should be allowed to
        // delete/rename the
        // nested file
        client.createFile(mrc1Address, posixVolName + "/stickyDir/newfile.txt", authString2);
        client.changeAccessMode(mrc1Address, posixVolName + "/stickyDir", 512 | 511, authString1);
        try {
            client.delete(mrc1Address, posixVolName + "/stickyDir/newfile.txt", authString1);
            fail("access should have been denied due to insufficient delete permissions (sticky bit)");
        } catch (HttpErrorException exc) {
        }
        try {
            client.move(mrc1Address, posixVolName + "/stickyDir/newfile.txt", posixVolName
                + "/stickyDir/newfile2.txt", authString1);
            fail("access should have been denied due to insufficient renaming permissions (sticky bit)");
        } catch (HttpErrorException exc) {
        }
        
        client.move(mrc1Address, posixVolName + "/stickyDir/newfile.txt", posixVolName
            + "/stickyDir/newfile2.txt", authString2);
        client.delete(mrc1Address, posixVolName + "/stickyDir/newfile2.txt", authString2);
    }
    
    public void testFileSizeUpdate() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String volumeName = "testVolume";
        final String fileName = volumeName + "/testFile";
        
        // create a new file in a new volume
        client.createVolume(mrc1Address, volumeName, authString);
        client.createFile(mrc1Address, fileName, authString);
        
        // check and update file sizes repeatedly
        Map<String, String> headers = client.open(mrc1Address, fileName, "r", authString);
        Map<String, Object> statInfo = client.stat(mrc1Address, fileName, false, false, false,
            authString);
        assertEquals(0l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[27,0]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(27l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[12,0]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(27l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[34,0]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(34l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[10,1]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(10l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[34,1]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(34l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[10,1]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(34l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[0,2]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(0l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[12,0]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(0l, statInfo.get("size"));
        
        client.updateFileSize(mrc1Address, headers.get(HTTPHeaders.HDR_XCAPABILITY), "[32,4]",
            authString);
        statInfo = client.stat(mrc1Address, fileName, false, false, false, authString);
        assertEquals(32l, statInfo.get("size"));
    }
    
    public void testDefaultStripingPolicies() throws Exception {
        
        final String authString = NullAuthProvider.createAuthString("userXY", MRCClient
                .generateStringList("groupZ"));
        final String volumeName = "testVolume";
        final String dirName = volumeName + "/dir";
        final String fileName1 = dirName + "/testFile";
        final String fileName2 = dirName + "/testFile2";
        final String fileName3 = dirName + "/testFile3";
        
        Map<String, Object> sp1 = RPCClient.generateMap("width", 1L, "policy", "RAID0",
            "stripe-size", 64L);
        Map<String, Object> sp2 = RPCClient.generateMap("width", 1L, "policy", "RAID0",
            "stripe-size", 256L);
        Map<String, Object> sp3 = RPCClient.generateMap("width", 1L, "policy", "RAID0",
            "stripe-size", 128L);
        String sp2String = sp2.get("policy") + ", " + sp2.get("stripe-size") + ", "
            + sp2.get("width");
        
        // create a new file in a directory in a new volume
        client.createVolume(mrc1Address, volumeName, 1, sp1, 1, 1, null, authString);
        client.createDir(mrc1Address, dirName, authString);
        client.createFile(mrc1Address, fileName1, authString);
        client.createFile(mrc1Address, fileName2, authString);
        client.createFile(mrc1Address, fileName3, null, sp3, 0, authString);
        
        String xLocHeader = client.open(mrc1Address, fileName1, "c", authString).get(
            HTTPHeaders.HDR_XLOCATIONS);
        List header = (List) JSONParser.parseJSON(new JSONString(xLocHeader));
        Map<String, Object> spol = (Map<String, Object>) ((List) ((List) header.get(0)).get(0))
                .get(0);
        assertEquals(sp1, spol);
        
        // set the default striping policy via an extended attribute
        Map<String, Object> defaultSP = new HashMap<String, Object>();
        defaultSP.put("xtreemfs.default_sp", sp2String);
        client.setXAttrs(mrc1Address, dirName, defaultSP, authString);
        
        xLocHeader = client.open(mrc1Address, fileName2, "c", authString).get(
            HTTPHeaders.HDR_XLOCATIONS);
        header = (List) JSONParser.parseJSON(new JSONString(xLocHeader));
        spol = (Map<String, Object>) ((List) ((List) header.get(0)).get(0)).get(0);
        assertEquals(sp2, spol);
        
        xLocHeader = client.open(mrc1Address, fileName3, "c", authString).get(
            HTTPHeaders.HDR_XLOCATIONS);
        header = (List) JSONParser.parseJSON(new JSONString(xLocHeader));
        spol = (Map<String, Object>) ((List) ((List) header.get(0)).get(0)).get(0);
        assertEquals(sp3, spol);
        
    }
    
    private void assertTree(InetSocketAddress server, String authString, String... paths)
        throws Exception {
        
        // check whether all paths exist exactly once
        for (String path : paths) {
            
            try {
                Map<String, Object> statInfo = client.stat(server, path, false, false, false,
                    authString);
                
                // continue if the path does not point to a directory
                if (!statInfo.get("objType").equals(2L))
                    continue;
                
            } catch (Exception exc) {
                throw new Exception("path '" + path + "' does not exist");
            }
            
            // if the path points to a directory, check whether the number of
            // subdirectories is correct
            int size = client.readDir(server, path, authString).size();
            int count = 0;
            for (String otherPath : paths) {
                if (!otherPath.startsWith(path + "/"))
                    continue;
                
                if (otherPath.substring(path.length() + 1).indexOf('/') == -1)
                    count++;
            }
            
            assertEquals(count, size);
        }
    }
    
    public static void main(String[] args) {
        TestRunner.run(MRCTest.class);
    }
    
    private static Map<String, Object> getDefaultStripingPolicy() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("policy", "RAID0");
        map.put("stripe-size", 1000L);
        map.put("width", 1L);
        return map;
    }
}
