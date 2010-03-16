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
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.interfaces.MRCInterface.MRCException;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.ac.POSIXFileAccessPolicy;
import org.xtreemfs.mrc.ac.VolumeACLFileAccessPolicy;
import org.xtreemfs.mrc.ac.YesToAnyoneFileAccessPolicy;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;

/**
 * XtreemFS MRC test
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
            Services.MRC_CLIENT, Services.DIR_SERVICE, Services.MRC, Services.MOCKUP_OSD,
            Services.MOCKUP_OSD2, Services.MOCKUP_OSD3);
        testEnv.start();
        
        client = testEnv.getMrcClient();
    }
    
    protected void tearDown() throws Exception {
        testEnv.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }
    
    public void testCreateDelete() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        // create and delete a volume
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            POSIXFileAccessPolicy.POLICY_ID, 0775));
        
        // Map<String, String> localVols = client.getLocalVolumes(mrc1Address);
        // assertEquals(1, localVols.size());
        // assertEquals(volumeName, localVols.values().iterator().next());
        invokeSync(client.rmvol(mrcAddress, uc, volumeName));
        // localVols = client.getLocalVolumes(mrc1Address, authString);
        // assertEquals(0, localVols.size());
        
        // create a volume (no access control)
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            POSIXFileAccessPolicy.POLICY_ID, 0775));
        
        // create some files and directories
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, "myDir", 0775));
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, "anotherDir", 0775));
        
        for (int i = 0; i < 10; i++)
            invokeSync(client.open(mrcAddress, uc, volumeName, "myDir/test" + i + ".txt",
                FileAccessManager.O_CREAT, 0775, 0, new VivaldiCoordinates()));
        
        // try to create a file w/o a name
        try {
            invokeSync(client.open(mrcAddress, uc, volumeName, "", FileAccessManager.O_CREAT, 0775, 0,
                new VivaldiCoordinates()));
            fail("missing filename");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.open(mrcAddress, uc, volumeName, "myDir/test0.txt", FileAccessManager.O_CREAT
                | FileAccessManager.O_EXCL, 0775, 0, new VivaldiCoordinates()));
            fail("duplicate file creation");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.open(mrcAddress, uc, volumeName, "myDir/test0.txt/bla.txt",
                FileAccessManager.O_CREAT, 0775, 0, new VivaldiCoordinates()));
            fail("file in file creation");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.mkdir(mrcAddress, uc, volumeName, "", 0));
            fail("directory already exists");
        } catch (MRCException exc) {
            
        }
        
        // test 'readDir' and 'stat'
        
        DirectoryEntrySet entrySet = invokeSync(client.readdir(mrcAddress, uc, volumeName, ""));
        assertEquals(3, entrySet.size());
        
        entrySet = invokeSync(client.readdir(mrcAddress, uc, volumeName, "myDir"));
        assertEquals(12, entrySet.size());
        
        Stat stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, "myDir/test2.txt")).get(0);
        assertEquals(uid, stat.getUser_id());
        assertTrue("test2.txt is a not a file", (stat.getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFREG) != 0);
        assertEquals(0, stat.getSize());
        assertTrue(stat.getAtime_ns() > 0);
        assertTrue(stat.getCtime_ns() > 0);
        assertTrue(stat.getMtime_ns() > 0);
        assertTrue((stat.getMode() & 511) > 0);
        assertEquals(1, stat.getNlink());
        
        // test 'delete'
        
        invokeSync(client.unlink(mrcAddress, uc, volumeName, "myDir/test3.txt"));
        
        entrySet = invokeSync(client.readdir(mrcAddress, uc, volumeName, "myDir"));
        assertEquals(11, entrySet.size());
        
        invokeSync(client.rmdir(mrcAddress, uc, volumeName, "anotherDir"));
    }
    
    public void testXAttrs() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        
        // create a file and add some user attributes
        invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "test.txt", "key1", "quark", 0));
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "test.txt", "key2", "quatsch", 0));
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "test.txt", "myAttr", "171", 0));
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "test.txt", "key1", "blub", 0));
        
        StringSet keys = invokeSync(client.listxattr(mrcAddress, uc, volumeName, "test.txt"));
        List<String> attrKeys = new LinkedList<String>();
        for (String key : keys)
            if (!key.startsWith("xtreemfs."))
                attrKeys.add(key);
        assertEquals(3, attrKeys.size());
        String val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "test.txt", "key1"));
        assertEquals("blub", val);
        val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "test.txt", "key2"));
        assertEquals("quatsch", val);
        val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "test.txt", "myAttr"));
        assertEquals("171", val);
        
        // create a new file, add some attrs and delete some attrs
        invokeSync(client.open(mrcAddress, uc, volumeName, "test2.txt", FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "test2.txt", "key1", "quark", 0));
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "test2.txt", "key2", "quatsch", 0));
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "test2.txt", "key3", "171", 0));
        
        invokeSync(client.removexattr(mrcAddress, uc, volumeName, "test2.txt", "key1"));
        keys = invokeSync(client.listxattr(mrcAddress, uc, volumeName, "test2.txt"));
        attrKeys = new LinkedList<String>();
        for (String key : keys)
            if (!key.startsWith("xtreemfs."))
                attrKeys.add(key);
        assertEquals(2, attrKeys.size());
        try {
            val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "test2.txt", "key1"));
            fail("got value for non-existing key");
        } catch (MRCException exc) {
            assertEquals(ErrNo.ENODATA, exc.getError_code());
        }
        
        invokeSync(client.removexattr(mrcAddress, uc, volumeName, "test2.txt", "key3"));
        keys = invokeSync(client.listxattr(mrcAddress, uc, volumeName, "test2.txt"));
        attrKeys = new LinkedList<String>();
        for (String key : keys)
            if (!key.startsWith("xtreemfs."))
                attrKeys.add(key);
        assertEquals(1, attrKeys.size());
        try {
            val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "test2.txt", "key3"));
            fail("got value for non-existing key");
        } catch (MRCException exc) {
            assertEquals(ErrNo.ENODATA, exc.getError_code());
        }
        
        // retrieve a system attribute
        val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "test.txt", "xtreemfs.object_type"));
        assertEquals("1", val);
        
        // check read-only replication
        FileCredentials creds = invokeSync(client.open(mrcAddress, uc, volumeName, "repl",
            FileAccessManager.O_CREAT, 0, 0, new VivaldiCoordinates()));
        assertEquals(Constants.REPL_UPDATE_PC_NONE, creds.getXlocs().getReplica_update_policy());
        
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "repl", "xtreemfs.read_only", "true", 0));
        val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "repl", "xtreemfs.read_only"));
        assertEquals("true", val);
        creds = invokeSync(client.open(mrcAddress, uc, volumeName, "repl", FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        assertEquals(Constants.REPL_UPDATE_PC_RONLY, creds.getXlocs().getReplica_update_policy());
    }
    
    public void testLargeXAttrs() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        
        // create a file and add some user attributes
        invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        byte[] largeAttr = new byte[9000];
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "test.txt", "key1", new String(largeAttr), 0));
        
        String val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "test.txt", "key1"));
        assertEquals(9000, val.length());
        
    }
    
    public void testSymlink() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        
        // create and test a symbolic link
        invokeSync(client.symlink(mrcAddress, uc, volumeName, "test.txt", "testAlias.txt"));
        String target = invokeSync(client.readlink(mrcAddress, uc, volumeName, "testAlias.txt"));
        assertEquals("test.txt", target);
    }
    
    public void testHardLink() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        invokeSync(client.open(mrcAddress, uc, volumeName, "test1.txt", FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        
        // create a new link
        invokeSync(client.link(mrcAddress, uc, volumeName, "test1.txt", "test2.txt"));
        
        // check whether both links refer to the same file
        Stat stat1 = invokeSync(client.getattr(mrcAddress, uc, volumeName, "test1.txt")).get(0);
        Stat stat2 = invokeSync(client.getattr(mrcAddress, uc, volumeName, "test2.txt")).get(0);
        
        assertEquals(stat1.getIno(), stat2.getIno());
        assertEquals(2, stat1.getNlink());
        
        // delete both files and check link count
        invokeSync(client.unlink(mrcAddress, uc, volumeName, "test1.txt"));
        Stat stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, "test2.txt")).get(0);
        assertEquals(1, stat.getNlink());
        invokeSync(client.unlink(mrcAddress, uc, volumeName, "test2.txt"));
        
        try {
            stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, "test1.txt")).get(0);
            fail("file should not exist anymore");
        } catch (MRCException exc) {
        }
        
        try {
            stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, "test2.txt")).get(0);
            fail("file should not exist anymore");
        } catch (MRCException exc) {
        }
        
        // create two links to a directory
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, "testDir1", 0));
        try {
            invokeSync(client.link(mrcAddress, uc, volumeName, "testDir1", "testDir1/testDir2"));
            fail("links to directories should not be allowed");
        } catch (Exception exc) {
        }
    }
    
    @SuppressWarnings("unchecked")
    public void testOpen() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            POSIXFileAccessPolicy.POLICY_ID, 0775));
        invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_CREAT, 0774, 0,
            new VivaldiCoordinates()));
        
        // open w/ O_RDWR; should not fail
        invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_RDWR, 0, 0,
            new VivaldiCoordinates()));
        
        // open w/ O_RDONLY; should not fail
        invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_RDONLY, 0, 0,
            new VivaldiCoordinates()));
        
        // create a new file w/ O_CREAT; should implicitly create a new file
        invokeSync(client.open(mrcAddress, uc, volumeName, "test2.txt", FileAccessManager.O_CREAT, 256, 0,
            new VivaldiCoordinates()));
        invokeSync(client.getattr(mrcAddress, uc, volumeName, "test2.txt"));
        
        // open w/ O_WRONLY; should fail
        try {
            invokeSync(client.open(mrcAddress, uc, volumeName, "test2.txt", FileAccessManager.O_WRONLY, 256,
                0, new VivaldiCoordinates()));
            fail();
        } catch (MRCException exc) {
            assertEquals(ErrNo.EACCES, exc.getError_code());
        }
        
        // open a directory; should fail
        try {
            invokeSync(client.open(mrcAddress, uc, volumeName, "dir", FileAccessManager.O_RDONLY, 0, 0,
                new VivaldiCoordinates()));
            fail("opened directory");
        } catch (MRCException exc) {
        }
        
        // create some symlinks
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, "dir", 0));
        invokeSync(client.symlink(mrcAddress, uc, volumeName, volumeName + "/test2.txt", "link"));
        invokeSync(client.symlink(mrcAddress, uc, volumeName, "somewhere", "link2"));
        
        // open a symlink
        FileCredentials creds = invokeSync(client.open(mrcAddress, uc, volumeName, "link",
            FileAccessManager.O_RDONLY, 0, 0, new VivaldiCoordinates()));
        
        // wait one second before renewing the capability
        Thread.sleep(1000);
        
        // test renewing a capability
        XCap newCap = invokeSync(client.xtreemfs_renew_capability(mrcAddress, creds.getXcap()));
        assertTrue(creds.getXcap().getExpire_time_s() < newCap.getExpire_time_s());
        
        // test redirect
        try {
            invokeSync(client.open(mrcAddress, uc, volumeName, "link2", FileAccessManager.O_RDONLY, 0, 0,
                new VivaldiCoordinates()));
            fail("should have been redirected");
        } catch (MRCException exc) {
        }
        
        // open w/ truncate flag; check whether the epoch number is incremented
        invokeSync(client.open(mrcAddress, uc, volumeName, "trunc", FileAccessManager.O_CREAT, 0777, 0,
            new VivaldiCoordinates()));
        creds = invokeSync(client.open(mrcAddress, uc, volumeName, "trunc", FileAccessManager.O_TRUNC, 0, 0,
            new VivaldiCoordinates()));
        assertEquals(1, creds.getXcap().getTruncate_epoch());
        creds = invokeSync(client.open(mrcAddress, uc, volumeName, "trunc", FileAccessManager.O_TRUNC, 0, 0,
            new VivaldiCoordinates()));
        assertEquals(2, creds.getXcap().getTruncate_epoch());
        
        // TODO: check open w/ ACLs set
        
        // test truncate
        
        // open w/ write cap and truncate
        creds = invokeSync(client.open(mrcAddress, uc, volumeName, "trunc", FileAccessManager.O_RDWR, 0, 0,
            new VivaldiCoordinates()));
        invokeSync(client.ftruncate(mrcAddress, creds.getXcap()));
        
        creds = invokeSync(client.open(mrcAddress, uc, volumeName, "trunc", FileAccessManager.O_RDONLY, 0, 0,
            new VivaldiCoordinates()));
        try {
            invokeSync(client.ftruncate(mrcAddress, creds.getXcap()));
            fail("truncated file w/o write permissions");
        } catch (MRCException exc) {
            // ignore
        }
    }
    
    public void testOpenCreateNoPerm() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            POSIXFileAccessPolicy.POLICY_ID, 0775));
        
        final String uid2 = "bla";
        final List<String> gids2 = createGIDs("groupY");
        final UserCredentials uc2 = MRCClient.getCredentials(uid2, gids2);
        
        // open O_CREATE as uid2 should fail
        try {
            invokeSync(client.open(mrcAddress, uc2, volumeName, "test2.txt",
                (FileAccessManager.O_WRONLY | FileAccessManager.O_CREAT), 256, 0, new VivaldiCoordinates()));
            fail();
        } catch (MRCException exc) {
            assertEquals(ErrNo.EACCES, exc.getError_code());
        }
        
    }
    
    public void testRename() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        
        // create some files and directories
        invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        invokeSync(client.open(mrcAddress, uc, volumeName, "blub.txt", FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, "mainDir", 0));
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, "mainDir/subDir", 0));
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, "mainDir/subDir/newDir", 0));
        
        assertTree(mrcAddress, uid, gids, volumeName, "", "test.txt", "blub.txt", "mainDir",
            "mainDir/subDir", "mainDir/subDir/newDir");
        
        // move some files and directories
        
        // file -> none (create w/ different name)
        invokeSync(client.rename(mrcAddress, uc, volumeName, "test.txt", "mainDir/bla.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/bla.txt", "blub.txt", "mainDir",
            "mainDir/subDir", "mainDir/subDir/newDir");
        
        // file -> file (overwrite)
        invokeSync(client.rename(mrcAddress, uc, volumeName, "mainDir/bla.txt", "blub.txt"));
        
        assertTree(mrcAddress, uid, gids, volumeName, "", "blub.txt", "mainDir", "mainDir/subDir",
            "mainDir/subDir/newDir");
        
        // file -> none (create w/ same name)
        invokeSync(client.rename(mrcAddress, uc, volumeName, "blub.txt", "mainDir/blub.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/blub.txt", "mainDir", "mainDir/subDir",
            "mainDir/subDir/newDir");
        
        // file -> dir (invalid operation)
        try {
            invokeSync(client.rename(mrcAddress, uc, volumeName, "mainDir/blub.txt", "mainDir/subDir"));
            fail("move file -> directory should not be possible");
        } catch (MRCException exc) {
        }
        
        // file -> file (same path, should have no effect)
        invokeSync(client.rename(mrcAddress, uc, volumeName, "mainDir/blub.txt", "mainDir/blub.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/blub.txt", "mainDir", "mainDir/subDir",
            "mainDir/subDir/newDir");
        
        // file -> file (same directory)
        invokeSync(client.rename(mrcAddress, uc, volumeName, "mainDir/blub.txt", "mainDir/blub2.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/blub2.txt", "mainDir", "mainDir/subDir",
            "mainDir/subDir/newDir");
        
        // dir -> none (create w/ same name)
        invokeSync(client.rename(mrcAddress, uc, volumeName, "mainDir/subDir", "subDir"));
        
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/blub2.txt", "mainDir", "subDir",
            "subDir/newDir");
        
        // dir -> dir (overwrite, should fail because of non-empty subdirectory)
        try {
            invokeSync(client.rename(mrcAddress, uc, volumeName, "subDir/newDir", "subDir"));
            fail("moved directory to non-empty directory");
        } catch (MRCException exc) {
        }
        
        // dir -> dir (overwrite)
        invokeSync(client.unlink(mrcAddress, uc, volumeName, "mainDir/blub2.txt"));
        invokeSync(client.rename(mrcAddress, uc, volumeName, "subDir", "mainDir"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir", "mainDir/newDir");
        
        // dir -> volume (should fail because volume can't be overwritten)
        try {
            invokeSync(client.rename(mrcAddress, uc, volumeName, "mainDir/newDir", ""));
            fail("move overwrote volume root");
        } catch (MRCException exc) {
        }
        
        // dir -> invalid volume (should fail)
        try {
            invokeSync(client.rename(mrcAddress, uc, volumeName, "", "somewhere"));
            fail("moved to invalid volume");
        } catch (MRCException exc) {
        }
        
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir", "mainDir/newDir");
        
        invokeSync(client.symlink(mrcAddress, uc, volumeName, "mainDir", "link"));
        invokeSync(client.rename(mrcAddress, uc, volumeName, "link", "newlink"));
        
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
        
        final UserCredentials uc1 = MRCClient.getCredentials(uid1, gids1);
        final UserCredentials uc2 = MRCClient.getCredentials(uid2, gids2);
        final UserCredentials uc3 = MRCClient.getCredentials(uid3, gids3);
        final UserCredentials uc4 = MRCClient.getCredentials(uid4, gids4);
        
        final String noACVolumeName = "noACVol";
        final String volACVolumeName = "volACVol";
        final String posixVolName = "posixVol";
        
        // NO ACCESS CONTROL
        
        // create a volume
        invokeSync(client.mkvol(mrcAddress, uc1, noACVolumeName, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        
        // test chown
        invokeSync(client.open(mrcAddress, uc1, noACVolumeName, "chownTestFile", FileAccessManager.O_CREAT,
            0, 0, new VivaldiCoordinates()));
        invokeSync(client.setattr(mrcAddress, uc4, noACVolumeName, "chownTestFile", createChownStat(
            "newUser", "newGroup"), MRCInterface.SETATTR_UID | MRCInterface.SETATTR_GID));
        
        Stat stat = invokeSync(client.getattr(mrcAddress, uc3, noACVolumeName, "chownTestFile")).get(0);
        assertEquals("newUser", stat.getUser_id());
        assertEquals("newGroup", stat.getGroup_id());
        
        invokeSync(client.unlink(mrcAddress, uc3, noACVolumeName, "chownTestFile"));
        
        // create a new directory; should succeed
        invokeSync(client.mkdir(mrcAddress, uc1, noACVolumeName, "newDir", 0));
        
        // create a new file inside the dir: should succeed (in spite of
        // not having explicitly set any rights on the parent directory)
        invokeSync(client.mkdir(mrcAddress, uc2, noACVolumeName, "newDir/newFile", 0));
        
        final UserCredentials ucS = MRCClient.getCredentials("someone", createGIDs("somegroup"));
        assertNotNull(invokeSync(client.readdir(mrcAddress, ucS, noACVolumeName, "newDir/newFile")));
        
        // VOLUME ACLs
        
        // create a volume
        invokeSync(client.mkvol(mrcAddress, uc1, volACVolumeName, getDefaultStripingPolicy(),
            VolumeACLFileAccessPolicy.POLICY_ID, 0));
        
        // create a new directory: should succeed for user1, fail
        // for user2
        invokeSync(client.mkdir(mrcAddress, uc1, volACVolumeName, "newDir", 0));
        
        // by default, anyone is allowed to do anything
        invokeSync(client.mkdir(mrcAddress, uc2, volACVolumeName, "newDir2", 0));
        
        // TODO: add more tests
        
        // POSIX policy
        
        // create a volume
        invokeSync(client.mkvol(mrcAddress, uc1, posixVolName, getDefaultStripingPolicy(),
            POSIXFileAccessPolicy.POLICY_ID, 0775));
        
        invokeSync(client.setattr(mrcAddress, uc1, posixVolName, "", createChmodStat(0700),
            MRCInterface.SETATTR_MODE));
        
        // create a new directory: should succeed for user1, fail for user2
        invokeSync(client.mkdir(mrcAddress, uc1, posixVolName, "newDir", 0700));
        
        // check permissions by opening the file
        assertNotNull(invokeSync(client.readdir(mrcAddress, uc1, posixVolName, "newDir")));
        
        try {
            invokeSync(client.mkdir(mrcAddress, uc2, posixVolName, "newDir2", 0700));
            fail("access should have been denied");
        } catch (MRCException exc) {
        }
        
        // TODO: test getting/setting ACL entries
        
        // change the access mode
        invokeSync(client.setattr(mrcAddress, uc1, posixVolName, "newDir", createChmodStat(0),
            MRCInterface.SETATTR_MODE));
        
        // readdir on "/newDir"; should fail for any user now
        try {
            invokeSync(client.readdir(mrcAddress, uc1, posixVolName, "newDir"));
            fail("access should have been denied");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.readdir(mrcAddress, uc2, posixVolName, "newDir"));
            fail("access should have been denied");
        } catch (MRCException exc) {
        }
        
        // set access rights to anyone (except for the owner)
        invokeSync(client.setattr(mrcAddress, uc1, posixVolName, "newDir", createChmodStat(0007),
            MRCInterface.SETATTR_MODE));
        
        try {
            invokeSync(client.readdir(mrcAddress, uc1, posixVolName, "newDir"));
            fail("access should have been denied due to insufficient permissions");
        } catch (MRCException exc) {
        }
        
        try {
            invokeSync(client.readdir(mrcAddress, uc3, posixVolName, "newDir"));
            fail("access should have been denied due to insufficient search permissions");
        } catch (MRCException exc) {
        }
        
        // set search rights on the root directory to 'others'
        invokeSync(client.setattr(mrcAddress, uc1, posixVolName, "", createChmodStat(0001),
            MRCInterface.SETATTR_MODE));
        
        // access should be granted to others now
        invokeSync(client.readdir(mrcAddress, uc3, posixVolName, "newDir"));
        
        // check permissions
        assertNotNull(invokeSync(client.readdir(mrcAddress, uc2, posixVolName, "newDir")));
        
        // check permissions
        assertNotNull(invokeSync(client.getattr(mrcAddress, uc3, posixVolName, "")));
        
        // grant any rights to the volume to anyone
        invokeSync(client.setattr(mrcAddress, uc1, posixVolName, "", createChmodStat(0777),
            MRCInterface.SETATTR_MODE));
        
        // owner of 'newDir' should still not have access rights
        try {
            invokeSync(client.readdir(mrcAddress, uc1, posixVolName, "newDir"));
            fail("access should have been denied due to insufficient permissions");
        } catch (MRCException exc) {
        }
        
        // others should still have no write permissions
        try {
            invokeSync(client.open(mrcAddress, uc1, posixVolName, "newDir/newfile",
                FileAccessManager.O_CREAT, 0, 0, new VivaldiCoordinates()));
            fail();
        } catch (MRCException exc) {
        }
        
        // create a POSIX ACL new volume and test "chmod"
        invokeSync(client.rmvol(mrcAddress, uc1, posixVolName));
        invokeSync(client.mkvol(mrcAddress, uc1, posixVolName, getDefaultStripingPolicy(),
            POSIXFileAccessPolicy.POLICY_ID, 0775));
        
        invokeSync(client.open(mrcAddress, uc1, posixVolName, "someFile.txt", FileAccessManager.O_CREAT, 224,
            0, new VivaldiCoordinates()));
        stat = invokeSync(client.getattr(mrcAddress, uc1, posixVolName, "someFile.txt")).get(0);
        assertEquals(224, stat.getMode() & 0x7FF);
        
        invokeSync(client.setattr(mrcAddress, uc1, posixVolName, "someFile.txt", createChmodStat(192),
            MRCInterface.SETATTR_MODE));
        stat = invokeSync(client.getattr(mrcAddress, uc1, posixVolName, "someFile.txt")).get(0);
        assertEquals(192, stat.getMode() & 0x7FF);
        
        // create a new directory w/ search access for anyone w/ access rights
        // to anyone
        invokeSync(client.mkdir(mrcAddress, uc1, posixVolName, "stickyDir", 0777));
        
        // create and delete/rename a file w/ different user IDs: this should
        // work
        invokeSync(client.open(mrcAddress, uc2, posixVolName, "stickyDir/newfile.txt",
            FileAccessManager.O_CREAT, 0, 0, new VivaldiCoordinates()));
        invokeSync(client.unlink(mrcAddress, uc1, posixVolName, "stickyDir/newfile.txt"));
        invokeSync(client.open(mrcAddress, uc3, posixVolName, "stickyDir/newfile.txt",
            FileAccessManager.O_CREAT, 0, 0, new VivaldiCoordinates()));
        invokeSync(client.rename(mrcAddress, uc1, posixVolName, "stickyDir/newfile.txt",
            "stickyDir/newfile2.txt"));
        
        // create a file and set sticky bit on the directory; now, only the
        // owner should be allowed to delete/rename the nested file
        invokeSync(client.open(mrcAddress, uc2, posixVolName, "stickyDir/newfile.txt",
            FileAccessManager.O_CREAT, 0, 0, new VivaldiCoordinates()));
        invokeSync(client.setattr(mrcAddress, uc1, posixVolName, "stickyDir", createChmodStat(01777),
            MRCInterface.SETATTR_MODE));
        
        try {
            invokeSync(client.unlink(mrcAddress, uc1, posixVolName, "stickyDir/newfile.txt"));
            fail("access should have been denied due to insufficient delete permissions (sticky bit)");
        } catch (MRCException exc) {
        }
        try {
            invokeSync(client.rename(mrcAddress, uc1, posixVolName, "stickyDir/newfile.txt",
                "stickyDir/newfile3.txt"));
            fail("access should have been denied due to insufficient renaming permissions (sticky bit)");
        } catch (MRCException exc) {
        }
        
        invokeSync(client.rename(mrcAddress, uc2, posixVolName, "stickyDir/newfile.txt",
            "stickyDir/newfile3.txt"));
        invokeSync(client.unlink(mrcAddress, uc2, posixVolName, "stickyDir/newfile3.txt"));
    }
    
    public void testFileSizeUpdate() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final String fileName = "testFile";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        // create a new file in a new volume
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        invokeSync(client.open(mrcAddress, uc, volumeName, fileName, FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        
        // check and update file sizes repeatedly
        XCap cap = invokeSync(
            client.open(mrcAddress, uc, volumeName, fileName, FileAccessManager.O_RDONLY, 0, 0,
                new VivaldiCoordinates())).getXcap();
        Stat stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(0L, stat.getSize());
        
        stat = createFSStat(27, 0);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(27L, stat.getSize());
        
        stat = createFSStat(12, 0);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(27L, stat.getSize());
        
        stat = createFSStat(34, 0);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(34L, stat.getSize());
        
        stat = createFSStat(10, 1);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(10L, stat.getSize());
        
        stat = createFSStat(34, 1);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(34L, stat.getSize());
        
        stat = createFSStat(10, 1);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(34L, stat.getSize());
        
        stat = createFSStat(0, 2);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(0L, stat.getSize());
        
        stat = createFSStat(12, 0);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(0L, stat.getSize());
        
        stat = createFSStat(32, 4);
        invokeSync(client.fsetattr(mrcAddress, cap, stat, MRCInterface.SETATTR_SIZE));
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName)).get(0);
        assertEquals(32L, stat.getSize());
    }
    
    public void testDefaultStripingPolicies() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        final String volumeName = "testVolume";
        final String dirName = "dir";
        final String fileName1 = dirName + "/testFile";
        final String fileName2 = dirName + "/testFile2";
        
        StripingPolicy sp1 = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1);
        StripingPolicy sp2 = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 256, 1);
        
        // create a new file in a directory in a new volume
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, sp1, YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, dirName, 0));
        invokeSync(client.open(mrcAddress, uc, volumeName, fileName1, FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        invokeSync(client.open(mrcAddress, uc, volumeName, fileName2, FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        
        // check if the striping policy assigned to the file matches the default
        // striping policy
        XLocSet xLoc = invokeSync(
            client.open(mrcAddress, uc, volumeName, fileName1, FileAccessManager.O_RDONLY, 0, 0,
                new VivaldiCoordinates())).getXlocs();
        
        StripingPolicy sp = xLoc.getReplicas().get(0).getStriping_policy();
        assertEquals(sp1.getType().name(), sp.getType().name());
        assertEquals(sp1.getWidth(), sp.getWidth());
        assertEquals(sp1.getStripe_size(), sp.getStripe_size());
        
        // check block size in Stat
        Stat stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, fileName1)).get(0);
        assertEquals(sp1.getStripe_size() * 1024, stat.getBlksize());
        
        stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, dirName)).get(0);
        assertEquals(0, stat.getBlksize());
        
    }
    
    public void testDefaultReplicationPolicies() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        final String volumeName = "testVolume";
        final String dirName = "dir";
        final String fileName = dirName + "/testFile";
        
        final StripingPolicy sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1);
        
        ReplicationPolicy rp = new ReplicationPolicy() {
            
            @Override
            public String getName() {
                return Constants.REPL_UPDATE_PC_WARA;
            }
            
            @Override
            public int getNumReplicas() {
                return 2;
            }
            
        };
        
        // create a new file in a directory in a new volume
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, sp, YesToAnyoneFileAccessPolicy.POLICY_ID, 0));
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "", "xtreemfs.default_rp", Converter
                .replicationPolicyToJSONString(rp), 0));
        
        String val = invokeSync(client.getxattr(mrcAddress, uc, volumeName, "", "xtreemfs.default_rp"));
        ReplicationPolicy pol = Converter.jsonStringToReplicationPolicy(val);
        
        assertEquals(Constants.REPL_UPDATE_PC_WARA, pol.getName());
        assertEquals(rp.getNumReplicas(), pol.getNumReplicas());
        
        invokeSync(client.mkdir(mrcAddress, uc, volumeName, dirName, 0));
        invokeSync(client.open(mrcAddress, uc, volumeName, fileName, FileAccessManager.O_CREAT, 0, 0,
            new VivaldiCoordinates()));
        
        // check if the striping policy assigned to the file matches the default
        // striping policy
        XLocSet xLoc = invokeSync(
            client.open(mrcAddress, uc, volumeName, fileName, FileAccessManager.O_RDONLY, 0, 0,
                new VivaldiCoordinates())).getXlocs();
        
        assertEquals(rp.getName(), xLoc.getReplica_update_policy());
        assertEquals(rp.getNumReplicas(), xLoc.getReplicas().size());
    }
    
    public void testReplicateOnClose() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        // create a volume
        invokeSync(client.mkvol(mrcAddress, uc, volumeName, getDefaultStripingPolicy(),
            POSIXFileAccessPolicy.POLICY_ID, 0775));
        
        // auto-assign three (two more) replicas to each newly-created file
        invokeSync(client.setxattr(mrcAddress, uc, volumeName, "", "xtreemfs.repl_factor", "3", 0));
        
        // create a new file
        invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_CREAT, 0775, 0,
            new VivaldiCoordinates()));
        
        // open the file
        FileCredentials creds = invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt",
            FileAccessManager.O_RDWR, 0, 0, new VivaldiCoordinates()));
        XCap xCap = creds.getXcap();
        
        // close the file
        invokeSync(client.close(mrcAddress, new VivaldiCoordinates(), xCap));
        
        // open the file again
        creds = invokeSync(client.open(mrcAddress, uc, volumeName, "test.txt", FileAccessManager.O_RDONLY, 0,
            0, new VivaldiCoordinates()));
        XLocSet xLoc = creds.getXlocs();
        
        // check whether there are three replicas now
        assertEquals(3, xLoc.getReplicas().size());
    }
    
    private void assertTree(InetSocketAddress server, String uid, List<String> gids, String volumeName,
        String... paths) throws Exception {
        
        final UserCredentials uc = MRCClient.getCredentials(uid, gids);
        
        // check whether all paths exist exactly once
        for (String path : paths) {
            
            try {
                Stat stat = invokeSync(client.getattr(mrcAddress, uc, volumeName, path)).get(0);
                
                // continue if the path does not point to a directory
                if ((stat.getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFDIR) == 0)
                    continue;
                
            } catch (MRCException exc) {
                throw new Exception("path '" + path + "' does not exist");
            }
            
            // if the path points to a directory, check whether the number of
            // subdirectories is correct
            DirectoryEntrySet dir = invokeSync(client.readdir(mrcAddress, uc, volumeName, path));
            int size = dir.size();
            
            int count = 0;
            for (String otherPath : paths) {
                
                // root dir
                if (path.equals("")) {
                    if (otherPath.indexOf('/') == -1)
                        count++;
                }

                // nested dir
                else {
                    if (!otherPath.startsWith(path + "/"))
                        continue;
                    
                    if (otherPath.substring(path.length() + 1).indexOf('/') == -1)
                        count++;
                    
                }
            }
            
            assertEquals(count + (path.equals("") ? 0 : 2), size);
        }
    }
    
    public static void main(String[] args) {
        TestRunner.run(MRCTest.class);
    }
    
    private static StripingPolicy getDefaultStripingPolicy() {
        return new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 1000, 1);
    }
    
    private static List<String> createGIDs(String gid) {
        List<String> list = new LinkedList<String>();
        list.add(gid);
        return list;
    }
    
    private static Stat createChmodStat(int newMode) {
        return new Stat(0, 0, newMode, 0, "", "", 0, 0, 0, 0, 0, 0, 0, 0);
    }
    
    private static Stat createFSStat(int newFS, int newEpoch) {
        return new Stat(0, 0, 0, 0, "", "", newFS, 0, 0, 0, 0, 0, newEpoch, 0);
    }
    
    private static Stat createChownStat(String newUid, String newGid) {
        return new Stat(0, 0, 0, 0, newUid, newGid, 0, 0, 0, 0, 0, 0, 0, 0);
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
