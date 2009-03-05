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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.KeyValuePair;
import org.xtreemfs.interfaces.KeyValuePairSet;
import org.xtreemfs.interfaces.NewFileSize;
import org.xtreemfs.interfaces.NewFileSizeSet;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.OSDtoMRCDataSet;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.interfaces.stat_;
import org.xtreemfs.interfaces.Exceptions.MRCException;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.ac.POSIXFileAccessPolicy;
import org.xtreemfs.mrc.ac.VolumeACLFileAccessPolicy;
import org.xtreemfs.mrc.ac.YesToAnyoneFileAccessPolicy;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.mrc.osdselection.RandomSelectionPolicy;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;

/**
 * XtreemFS integration test case.
 * 
 * @author stender
 */
public class MRCTest extends TestCase {
    
    private MRCClient         client;
    
    private InetSocketAddress mrcAddress;
    
    private TestEnvironment   testEnv;
    
    public MRCTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        mrcAddress = SetupUtils.getMRC1Addr();
        
        // register an OSD at the directory service (needed in order to assign
        // it to a new file on 'open')
        
        testEnv = new TestEnvironment(Services.DIR_CLIENT, Services.TIME_SYNC, Services.UUID_RESOLVER,
            Services.MRC_CLIENT, Services.DIR_SERVICE, Services.MRC);
        testEnv.start();
        
        client = testEnv.getMrcClient();
        
        try {
            KeyValuePairSet kvset = new KeyValuePairSet();
            kvset.add(new KeyValuePair("free", "1000000000"));
            kvset.add(new KeyValuePair("total", "1000000000"));
            kvset.add(new KeyValuePair("load", "0"));
            kvset.add(new KeyValuePair("totalRAM", "1000000000"));
            kvset.add(new KeyValuePair("usedRAM", "0"));
            kvset.add(new KeyValuePair("proto_version", "" + OSDInterface.getVersion()));
            ServiceRegistry reg = new ServiceRegistry("mockUpOSD", 0, Constants.SERVICE_TYPE_OSD,
                "mockUpOSD", kvset);
            RPCResponse<Long> response = testEnv.getDirClient().service_register(null, reg);
            response.get();
            response.freeBuffers();
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
        }
    }
    
    protected void tearDown() throws Exception {
        testEnv.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }
    
    public void testCreateDelete() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        
        // create and delete a volume
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, 1, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID));
        
        // Map<String, String> localVols = client.getLocalVolumes(mrc1Address);
        // assertEquals(1, localVols.size());
        // assertEquals(volumeName, localVols.values().iterator().next());
        invokeSync(client.rmvol(mrcAddress, uid, gids, "", volumeName));
        // localVols = client.getLocalVolumes(mrc1Address, authString);
        // assertEquals(0, localVols.size());
        
        // create a volume (no access control)
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, 1, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID));
        
        // create some files and directories
        invokeSync(client.mkdir(mrcAddress, uid, gids, volumeName + "/myDir", 0));
        invokeSync(client.mkdir(mrcAddress, uid, gids, volumeName + "/anotherDir", 0));
        
        for (int i = 0; i < 10; i++)
            invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/myDir/test" + i + ".txt", 0));
        
        // try to create a file w/o a name
        try {
            invokeSync(client.create(mrcAddress, uid, gids, volumeName, 0));
            fail("missing filename");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/myDir/test0.txt", 0));
            fail("duplicate file creation");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/myDir/test0.txt/bla.txt", 0));
            fail("file in file creation");
        } catch (MRCException exc) {
        }
        
        // test 'readDir' and 'stat'
        
        DirectoryEntrySet entrySet = invokeSync(client.readdir(mrcAddress, uid, gids, volumeName));
        assertEquals(2, entrySet.size());
        
        entrySet = invokeSync(client.readdir(mrcAddress, uid, gids, volumeName + "/myDir"));
        assertEquals(10, entrySet.size());
        
        stat_ stat = invokeSync(client.getattr(mrcAddress, uid, gids, volumeName + "/myDir/test2.txt"));
        assertEquals(uid, stat.getUser_id());
        assertEquals(1, stat.getObject_type());
        assertEquals(0, stat.getSize());
        assertTrue(stat.getAtime() > 0);
        assertTrue(stat.getCtime() > 0);
        assertTrue(stat.getMtime() > 0);
        assertTrue((stat.getMode() & 511) > 0);
        assertEquals(1, stat.getNlink());
        
        // test 'delete'
        
        invokeSync(client.unlink(mrcAddress, uid, gids, volumeName + "/myDir/test3.txt"));
        
        entrySet = invokeSync(client.readdir(mrcAddress, uid, gids, volumeName + "/myDir"));
        assertEquals(9, entrySet.size());
        
        invokeSync(client.rmdir(mrcAddress, uid, gids, volumeName + "/anotherDir"));
    }
    
    public void testUserAttributes() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final long accessMode = 511; // rwxrwxrwx
        
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, 1, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID));
        
        // create a file and add some user attributes
        invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/test.txt", 0));
        invokeSync(client.setxattr(mrcAddress, uid, gids, volumeName + "/test.txt", "key1", "quark", 0));
        invokeSync(client.setxattr(mrcAddress, uid, gids, volumeName + "/test.txt", "key2", "quatsch", 0));
        invokeSync(client.setxattr(mrcAddress, uid, gids, volumeName + "/test.txt", "myAttr", "171", 0));
        invokeSync(client.setxattr(mrcAddress, uid, gids, volumeName + "/test.txt", "key1", "blub", 0));
        
        StringSet keys = invokeSync(client.listxattr(mrcAddress, uid, gids, volumeName + "/test.txt"));
        List<String> attrKeys = new LinkedList<String>();
        for (String key : keys)
            if (!key.startsWith("xtreemfs."))
                attrKeys.add(key);
        assertEquals(3, attrKeys.size());
        String val = invokeSync(client.getxattr(mrcAddress, uid, gids, volumeName + "/test.txt", "key1"));
        assertEquals("blub", val);
        val = invokeSync(client.getxattr(mrcAddress, uid, gids, volumeName + "/test.txt", "key2"));
        assertEquals("quatsch", val);
        val = invokeSync(client.getxattr(mrcAddress, uid, gids, volumeName + "/test.txt", "myAttr"));
        assertEquals("171", val);
        
        // create a new file, add some attrs and delete some attrs
        invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/test2.txt", 0));
        invokeSync(client.setxattr(mrcAddress, uid, gids, volumeName + "/test2.txt", "key1", "quark", 0));
        invokeSync(client.setxattr(mrcAddress, uid, gids, volumeName + "/test2.txt", "key2", "quatsch", 0));
        invokeSync(client.setxattr(mrcAddress, uid, gids, volumeName + "/test2.txt", "key3", "171", 0));
        
        invokeSync(client.removexattr(mrcAddress, uid, gids, volumeName + "/test2.txt", "key1"));
        keys = invokeSync(client.listxattr(mrcAddress, uid, gids, volumeName + "/test2.txt"));
        attrKeys = new LinkedList<String>();
        for (String key : keys)
            if (!key.startsWith("xtreemfs."))
                attrKeys.add(key);
        assertEquals(2, attrKeys.size());
        val = invokeSync(client.getxattr(mrcAddress, uid, gids, volumeName + "/test2.txt", "key1"));
        assertEquals("", val);
        
        invokeSync(client.removexattr(mrcAddress, uid, gids, volumeName + "/test2.txt", "key3"));
        keys = invokeSync(client.listxattr(mrcAddress, uid, gids, volumeName + "/test2.txt"));
        attrKeys = new LinkedList<String>();
        for (String key : keys)
            if (!key.startsWith("xtreemfs."))
                attrKeys.add(key);
        assertEquals(1, attrKeys.size());
        val = invokeSync(client.getxattr(mrcAddress, uid, gids, volumeName + "/test2.txt", "key3"));
        assertEquals("", val);
        
        // retrieve a system attribute
        val = invokeSync(client.getxattr(mrcAddress, uid, gids, volumeName + "/test.txt",
            "xtreemfs.object_type"));
        assertEquals("1", val);
    }
    
    public void testSymlink() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, 1, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID));
        invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/test.txt", 0));
        
        // create and test a symbolic link
        invokeSync(client.symlink(mrcAddress, uid, gids, volumeName + "/test.txt", volumeName
            + "/testAlias.txt"));
        stat_ stat = invokeSync(client.getattr(mrcAddress, uid, gids, volumeName + "/testAlias.txt"));
        assertEquals(volumeName + "/test.txt", stat.getLink_target());
        assertEquals(3, stat.getObject_type());
    }
    
    public void testHardLink() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, 1, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID));
        invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/test1.txt", 0));
        
        // create a new link
        invokeSync(client.link(mrcAddress, uid, gids, volumeName + "/test1.txt", volumeName + "/test2.txt"));
        
        // check whether both links refer to the same file
        stat_ stat1 = invokeSync(client.getattr(mrcAddress, uid, gids, volumeName + "/test1.txt"));
        stat_ stat2 = invokeSync(client.getattr(mrcAddress, uid, gids, volumeName + "/test2.txt"));
        
        assertEquals(stat1.getFile_id(), stat2.getFile_id());
        assertEquals(2, stat1.getNlink());
        
        // delete both files and check link count
        invokeSync(client.unlink(mrcAddress, uid, gids, volumeName + "/test1.txt"));
        stat_ stat = invokeSync(client.getattr(mrcAddress, uid, gids, volumeName + "/test2.txt"));
        assertEquals(1, stat.getNlink());
        invokeSync(client.unlink(mrcAddress, uid, gids, volumeName + "/test2.txt"));
        
        try {
            stat = invokeSync(client.getattr(mrcAddress, uid, gids, volumeName + "/test1.txt"));
            fail("file should not exist anymore");
        } catch (MRCException exc) {
        }
        
        try {
            stat = invokeSync(client.getattr(mrcAddress, uid, gids, volumeName + "/test2.txt"));
            fail("file should not exist anymore");
        } catch (MRCException exc) {
        }
        
        // create two links to a directory
        invokeSync(client.mkdir(mrcAddress, uid, gids, volumeName + "/testDir1", 0));
        try {
            invokeSync(client.link(mrcAddress, uid, gids, volumeName + "/testDir1", volumeName
                + "/testDir1/testDir2"));
            fail("links to directories should not be allowed");
        } catch (Exception exc) {
        }
    }
    
    public void testReplicas() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final long accessMode = 511; // rwxrwxrwx
        
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, RandomSelectionPolicy.POLICY_ID,
            getDefaultStripingPolicy(), POSIXFileAccessPolicy.POLICY_ID));
        
        invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/test.txt", 0774));
        invokeSync(client.setxattr(mrcAddress, uid, gids, volumeName + "/test.txt", "xtreemfs.read_only",
            "true", 0));
        
        // TODO: test addition and removal of replicas
    }
    
    public void testOpen() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, RandomSelectionPolicy.POLICY_ID,
            getDefaultStripingPolicy(), POSIXFileAccessPolicy.POLICY_ID));
        invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/test.txt", 0774));
        
        // open w/ O_RDONLY; should not fail
        invokeSync(client
                .open(mrcAddress, uid, gids, volumeName + "/test.txt", FileAccessManager.O_RDONLY, 0));
        
        // open w/ O_RDWR; should not fail
        invokeSync(client.open(mrcAddress, uid, gids, volumeName + "/test.txt", FileAccessManager.O_RDWR, 0));
        
        // create a new file w/ O_CREAT; should implicitly create a new file
        invokeSync(client.open(mrcAddress, uid, gids, volumeName + "/test2.txt", FileAccessManager.O_CREAT,
            256));
        invokeSync(client.getattr(mrcAddress, uid, gids, volumeName + "/test2.txt"));
        
        // open w/ O_WRONLY; should fail
        try {
            invokeSync(client.open(mrcAddress, uid, gids, volumeName + "/test2.txt",
                FileAccessManager.O_WRONLY, 256));
            fail();
        } catch (MRCException exc) {
            assertEquals(ErrNo.EACCES, exc.getError_code());
        }
        
        // open a directory; should fail
        try {
            invokeSync(client.open(mrcAddress, uid, gids, volumeName + "/dir", FileAccessManager.O_RDONLY, 0));
            fail("opened directory");
        } catch (MRCException exc) {
        }
        
        // create some symlinks
        invokeSync(client.mkdir(mrcAddress, uid, gids, volumeName + "/dir", 0));
        invokeSync(client.symlink(mrcAddress, uid, gids, volumeName + "/test2.txt", volumeName + "/link"));
        invokeSync(client.symlink(mrcAddress, uid, gids, "somewhere", volumeName + "/link2"));
        
        // open a symlink
        FileCredentials creds = invokeSync(client.open(mrcAddress, uid, gids, volumeName + "/link",
            FileAccessManager.O_RDONLY, 0));
        
        // wait one second before renewing the capability
        Thread.sleep(1000);
        
        // test renewing a capability
        XCap newCap = invokeSync(client.xtreemfs_renew_capability(mrcAddress, creds.getXcap()));
        assertTrue(creds.getXcap().getExpires() < newCap.getExpires());
        
        // test redirect
        try {
            invokeSync(client.open(mrcAddress, uid, gids, volumeName + "/link2", FileAccessManager.O_RDONLY,
                0));
            fail("should have been redirected");
        } catch (MRCException exc) {
        }
        
        // TODO: check open w/ ACLs set
        
    }
    
    public void testRename() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, RandomSelectionPolicy.POLICY_ID,
            getDefaultStripingPolicy(), YesToAnyoneFileAccessPolicy.POLICY_ID));
        
        // create some files and directories
        invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/test.txt", 0));
        invokeSync(client.create(mrcAddress, uid, gids, volumeName + "/blub.txt", 0));
        invokeSync(client.mkdir(mrcAddress, uid, gids, volumeName + "/mainDir", 0));
        invokeSync(client.mkdir(mrcAddress, uid, gids, volumeName + "/mainDir/subDir", 0));
        invokeSync(client.mkdir(mrcAddress, uid, gids, volumeName + "/mainDir/subDir/newDir", 0));
        
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/test.txt", volumeName + "/blub.txt",
            volumeName + "/mainDir", volumeName + "/mainDir/subDir", volumeName + "/mainDir/subDir/newDir");
        
        // move some files and directories
        
        // file -> none (create w/ different name)
        invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/test.txt", volumeName
            + "/mainDir/bla.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/mainDir/bla.txt", volumeName
            + "/blub.txt", volumeName + "/mainDir", volumeName + "/mainDir/subDir", volumeName
            + "/mainDir/subDir/newDir");
        
        // file -> file (overwrite)
        invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/mainDir/bla.txt", volumeName
            + "/blub.txt"));
        
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/blub.txt", volumeName + "/mainDir",
            volumeName + "/mainDir/subDir", volumeName + "/mainDir/subDir/newDir");
        
        // file -> none (create w/ same name)
        invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/blub.txt", volumeName
            + "/mainDir/blub.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/mainDir/blub.txt", volumeName
            + "/mainDir", volumeName + "/mainDir/subDir", volumeName + "/mainDir/subDir/newDir");
        
        // file -> dir (invalid operation)
        try {
            invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/mainDir/blub.txt", volumeName
                + "/mainDir/subDir"));
            fail("move file -> directory should not be possible");
        } catch (MRCException exc) {
        }
        
        // file -> file (same path, should have no effect)
        invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/mainDir/blub.txt", volumeName
            + "/mainDir/blub.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/mainDir/blub.txt", volumeName
            + "/mainDir", volumeName + "/mainDir/subDir", volumeName + "/mainDir/subDir/newDir");
        
        // file -> file (same directory)
        invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/mainDir/blub.txt", volumeName
            + "/mainDir/blub2.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/mainDir/blub2.txt", volumeName
            + "/mainDir", volumeName + "/mainDir/subDir", volumeName + "/mainDir/subDir/newDir");
        
        // dir -> none (create w/ same name)
        invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/mainDir/subDir", volumeName
            + "/subDir"));
        
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/mainDir/blub2.txt", volumeName
            + "/mainDir", volumeName + "/subDir", volumeName + "/subDir/newDir");
        
        // dir -> dir (overwrite, should fail because of non-empty subdirectory)
        try {
            invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/subDir/newDir", volumeName
                + "/subDir"));
            fail("moved directory to non-empty directory");
        } catch (MRCException exc) {
        }
        
        // dir -> dir (overwrite)
        invokeSync(client.unlink(mrcAddress, uid, gids, volumeName + "/mainDir/blub2.txt"));
        invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/subDir", volumeName + "/mainDir"));
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/mainDir", volumeName + "/mainDir/newDir");
        
        // dir -> volume (should fail because volume can't be overwritten)
        try {
            invokeSync(client.rename(mrcAddress, uid, gids, volumeName + "/mainDir/newDir", volumeName));
            fail("move overwrote volume root");
        } catch (MRCException exc) {
        }
        
        // dir -> invalid volume (should fail)
        try {
            invokeSync(client.rename(mrcAddress, uid, gids, volumeName, "somewhere"));
            fail("moved to invalid volume");
        } catch (MRCException exc) {
        }
        
        assertTree(mrcAddress, uid, gids, volumeName, volumeName + "/mainDir", volumeName + "/mainDir/newDir");
    }
    
    public void testAccessControl() throws Exception {
        
        final String uid1 = "userXY";
        final List<String> gids1 = createGIDs("groupZ");
        final String uid2 = "userAB";
        final List<String> gids2 = createGIDs("groupA");
        final String uid3 = "userZZ";
        final List<String> gids3 = createGIDs("groupY");
        final String uid4 = "root";
        final List<String> gids4 = createGIDs("root");
        
        final String noACVolumeName = "noACVol";
        final String volACVolumeName = "volACVol";
        final String posixVolName = "posixVol";
        
        // NO ACCESS CONTROL
        
        // create a volume
        invokeSync(client.mkvol(mrcAddress, uid1, gids1, "", noACVolumeName, RandomSelectionPolicy.POLICY_ID,
            getDefaultStripingPolicy(), YesToAnyoneFileAccessPolicy.POLICY_ID));
        
        // test chown
        invokeSync(client.create(mrcAddress, uid1, gids1, noACVolumeName + "/chownTestFile", 0));
        invokeSync(client.chown(mrcAddress, uid4, gids4, noACVolumeName + "/chownTestFile", "newUser",
            "newGroup"));
        
        stat_ stat = invokeSync(client.getattr(mrcAddress, uid3, gids3, noACVolumeName + "/chownTestFile"));
        assertEquals("newUser", stat.getUser_id());
        assertEquals("newGroup", stat.getGroup_id());
        
        invokeSync(client.unlink(mrcAddress, uid3, gids3, noACVolumeName + "/chownTestFile"));
        
        // create a new directory; should succeed
        invokeSync(client.mkdir(mrcAddress, uid1, gids1, noACVolumeName + "/newDir", 0));
        
        // create a new file inside the dir: should succeed (in spite of
        // not having explicitly set any rights on the parent directory)
        invokeSync(client.mkdir(mrcAddress, uid2, gids2, noACVolumeName + "/newDir/newFile", 0));
        
        invokeSync(client.access(mrcAddress, "someone", createGIDs("somegroup"), noACVolumeName
            + "/newDir/newFile", FileAccessManager.O_RDWR | FileAccessManager.NON_POSIX_SEARCH));
        
        // VOLUME ACLs
        
        // create a volume
        invokeSync(client.mkvol(mrcAddress, uid1, gids1, "", volACVolumeName,
            RandomSelectionPolicy.POLICY_ID, getDefaultStripingPolicy(), VolumeACLFileAccessPolicy.POLICY_ID));
        
        // create a new directory: should succeed for user1, fail
        // for user2
        invokeSync(client.mkdir(mrcAddress, uid1, gids1, volACVolumeName + "/newDir", 0));
        
        // by default, anyone is allowed to do anything
        invokeSync(client.mkdir(mrcAddress, uid2, gids2, volACVolumeName + "/newDir2", 0));
        
        // TODO: add more tests
        
        // POSIX policy
        
        // create a volume
        invokeSync(client.mkvol(mrcAddress, uid1, gids1, "", posixVolName, POSIXFileAccessPolicy.POLICY_ID,
            getDefaultStripingPolicy(), POSIXFileAccessPolicy.POLICY_ID));
        
        invokeSync(client.chmod(mrcAddress, uid1, gids1, posixVolName, 0700));
        
        // create a new directory: should succeed for user1, fail for user2
        invokeSync(client.mkdir(mrcAddress, uid1, gids1, posixVolName + "/newDir", 0700));
        
        assertTrue(invokeSync(client.access(mrcAddress, uid1, gids1, posixVolName + "/newDir",
            FileAccessManager.O_RDWR | FileAccessManager.O_RDONLY)));
        
        try {
            invokeSync(client.mkdir(mrcAddress, uid2, gids2, posixVolName + "/newDir2", 0700));
            fail("access should have been denied");
        } catch (MRCException exc) {
        }
        
        // TODO: test getting/setting ACL entries
        
        // change the access mode
        invokeSync(client.chmod(mrcAddress, uid1, gids1, posixVolName + "/newDir", 0));
        
        // readdir on "/newDir"; should fail for any user now
        try {
            invokeSync(client.readdir(mrcAddress, uid1, gids1, posixVolName + "/newDir"));
            fail("access should have been denied");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.readdir(mrcAddress, uid2, gids2, posixVolName + "/newDir"));
            fail("access should have been denied");
        } catch (MRCException exc) {
        }
        
        // set access rights to anyone (except for the owner)
        invokeSync(client.chmod(mrcAddress, uid1, gids1, posixVolName + "/newDir", 0007));
        
        try {
            invokeSync(client.readdir(mrcAddress, uid1, gids1, posixVolName + "/newDir"));
            fail("access should have been denied due to insufficient permissions");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.readdir(mrcAddress, uid3, gids3, posixVolName + "/newDir"));
            fail("access should have been denied due to insufficient search permissions");
        } catch (MRCException exc) {
        }
        
        // set search rights on the root directory to 'others'
        invokeSync(client.chmod(mrcAddress, uid1, gids1, posixVolName, 0001));
        
        // access should be granted to others now
        invokeSync(client.readdir(mrcAddress, uid3, gids3, posixVolName + "/newDir"));
        
        assertTrue(invokeSync(client.access(mrcAddress, uid2, gids2, posixVolName + "/newDir",
            FileAccessManager.NON_POSIX_SEARCH)));
        
        assertFalse(invokeSync(client.access(mrcAddress, uid1, gids3, posixVolName,
            FileAccessManager.NON_POSIX_SEARCH)));
        
        // grant any rights to the volume to anyone
        invokeSync(client.chmod(mrcAddress, uid1, gids1, posixVolName, 0777));
        
        // owner of 'newDir' should still not have access rights
        try {
            invokeSync(client.readdir(mrcAddress, uid1, gids1, posixVolName + "/newDir"));
            fail("access should have been denied due to insufficient permissions");
        } catch (MRCException exc) {
        }
        
        // others should still have no write permissions
        assertFalse(invokeSync(client.access(mrcAddress, uid1, gids1, posixVolName + "/newDir",
            FileAccessManager.O_WRONLY)));
        
        // create a POSIX ACL new volume and test "chmod"
        invokeSync(client.rmvol(mrcAddress, uid1, gids1, "", posixVolName));
        invokeSync(client.mkvol(mrcAddress, uid1, gids1, "", posixVolName, POSIXFileAccessPolicy.POLICY_ID,
            getDefaultStripingPolicy(), POSIXFileAccessPolicy.POLICY_ID));
        
        invokeSync(client.create(mrcAddress, uid1, gids1, posixVolName + "/someFile.txt", 224));
        stat = invokeSync(client.getattr(mrcAddress, uid1, gids1, posixVolName + "/someFile.txt"));
        assertEquals(224, stat.getMode());
        
        invokeSync(client.chmod(mrcAddress, uid1, gids1, posixVolName + "/someFile.txt", 192));
        stat = invokeSync(client.getattr(mrcAddress, uid1, gids1, posixVolName + "/someFile.txt"));
        assertEquals(192, stat.getMode());
        
        // create a new directory w/ search access for anyone w/ access rights
        // to anyone
        invokeSync(client.mkdir(mrcAddress, uid1, gids1, posixVolName + "/stickyDir", 0777));
        
        // create and delete/rename a file w/ different user IDs: this should
        // work
        invokeSync(client.create(mrcAddress, uid2, gids2, posixVolName + "/stickyDir/newfile.txt", 0));
        invokeSync(client.unlink(mrcAddress, uid1, gids1, posixVolName + "/stickyDir/newfile.txt"));
        invokeSync(client.create(mrcAddress, uid3, gids3, posixVolName + "/stickyDir/newfile.txt", 0));
        invokeSync(client.rename(mrcAddress, uid1, gids1, posixVolName + "/stickyDir/newfile.txt",
            posixVolName + "/stickyDir/newfile2.txt"));
        
        // create a file and set sticky bit on the directory; now, only the
        // owner should be allowed to delete/rename the nested file
        invokeSync(client.create(mrcAddress, uid2, gids2, posixVolName + "/stickyDir/newfile.txt", 0));
        invokeSync(client.chmod(mrcAddress, uid1, gids1, posixVolName + "/stickyDir", 01777));
        
        try {
            invokeSync(client.unlink(mrcAddress, uid1, gids1, posixVolName + "/stickyDir/newfile.txt"));
            fail("access should have been denied due to insufficient delete permissions (sticky bit)");
        } catch (MRCException exc) {
        }
        try {
            invokeSync(client.rename(mrcAddress, uid1, gids1, posixVolName + "/stickyDir/newfile.txt",
                posixVolName + "/stickyDir/newfile3.txt"));
            fail("access should have been denied due to insufficient renaming permissions (sticky bit)");
        } catch (MRCException exc) {
        }
        
        invokeSync(client.rename(mrcAddress, uid2, gids2, posixVolName + "/stickyDir/newfile.txt",
            posixVolName + "/stickyDir/newfile3.txt"));
        invokeSync(client.unlink(mrcAddress, uid2, gids2, posixVolName + "/stickyDir/newfile3.txt"));
    }
    
    public void testFileSizeUpdate() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final String fileName = volumeName + "/testFile";
        
        // create a new file in a new volume
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, RandomSelectionPolicy.POLICY_ID,
            getDefaultStripingPolicy(), YesToAnyoneFileAccessPolicy.POLICY_ID));
        invokeSync(client.create(mrcAddress, uid, gids, fileName, 0));
        
        // check and update file sizes repeatedly
        XCap cap = invokeSync(client.open(mrcAddress, uid, gids, fileName, FileAccessManager.O_RDONLY, 0))
                .getXcap();
        stat_ stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(0L, stat.getSize());
        
        NewFileSizeSet newFSSet = new NewFileSizeSet();
        OSDWriteResponse resp = new OSDWriteResponse(newFSSet, new OSDtoMRCDataSet());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(27, 0));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(27L, stat.getSize());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(12, 0));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(27L, stat.getSize());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(34, 0));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(34L, stat.getSize());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(10, 1));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(10L, stat.getSize());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(34, 1));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(34L, stat.getSize());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(10, 1));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(34L, stat.getSize());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(0, 2));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(0L, stat.getSize());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(12, 0));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(0L, stat.getSize());
        
        newFSSet.clear();
        newFSSet.add(new NewFileSize(32, 4));
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, cap, resp));
        stat = invokeSync(client.getattr(mrcAddress, uid, gids, fileName));
        assertEquals(32L, stat.getSize());
    }
    
    public void testDefaultStripingPolicies() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        
        final String volumeName = "testVolume";
        final String dirName = volumeName + "/dir";
        final String fileName1 = dirName + "/testFile";
        final String fileName2 = dirName + "/testFile2";
        
        StripingPolicy sp1 = new StripingPolicy(Constants.STRIPING_POLICY_RAID0, 64, 1);
        StripingPolicy sp2 = new StripingPolicy(Constants.STRIPING_POLICY_RAID0, 256, 1);
        
        // create a new file in a directory in a new volume
        invokeSync(client.mkvol(mrcAddress, uid, gids, "", volumeName, RandomSelectionPolicy.POLICY_ID, sp1,
            YesToAnyoneFileAccessPolicy.POLICY_ID));
        
        invokeSync(client.mkdir(mrcAddress, uid, gids, dirName, 0));
        invokeSync(client.create(mrcAddress, uid, gids, fileName1, 0));
        invokeSync(client.create(mrcAddress, uid, gids, fileName2, 0));
        
        // check if the striping policy assigned to the file matches the default
        // striping policy
        XLocSet xLoc = invokeSync(
            client.open(mrcAddress, uid, gids, fileName1, FileAccessManager.O_RDONLY, 0)).getXlocs();
        assertEquals(sp1.toString(), xLoc.getReplicas().get(0).getStriping_policy().toString());
        
        // set the default striping policy of the parent directory via an
        // extended attribute
        invokeSync(client.setxattr(mrcAddress, uid, gids, dirName, "xtreemfs.default_sp", Converter
                .stripingPolicyToString(sp2), 0));
        xLoc = invokeSync(client.open(mrcAddress, uid, gids, fileName2, FileAccessManager.O_RDONLY, 0))
                .getXlocs();
        assertEquals(sp2.toString(), xLoc.getReplicas().get(0).getStriping_policy().toString());
        
    }
    
    private void assertTree(InetSocketAddress server, String uid, List<String> gids, String... paths)
        throws Exception {
        
        // check whether all paths exist exactly once
        for (String path : paths) {
            
            try {
                stat_ stat = invokeSync(client.getattr(mrcAddress, uid, gids, path));
                
                // continue if the path does not point to a directory
                if (stat.getObject_type() != 2)
                    continue;
                
            } catch (MRCException exc) {
                throw new Exception("path '" + path + "' does not exist");
            }
            
            // if the path points to a directory, check whether the number of
            // subdirectories is correct
            int size = invokeSync(client.readdir(mrcAddress, uid, gids, path)).size();
            
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
    
    private static StripingPolicy getDefaultStripingPolicy() {
        return new StripingPolicy(Constants.STRIPING_POLICY_DEFAULT, 1000, 1);
    }
    
    private static List<String> createGIDs(String gid) {
        List<String> list = new LinkedList<String>();
        list.add(gid);
        return list;
    }
    
    private static <T> T invokeSync(RPCResponse<T> response) throws ONCRPCException, IOException,
        InterruptedException {
        
        try {
            return response.get();
        } finally {
            response.freeBuffers();
        }
    }
}
