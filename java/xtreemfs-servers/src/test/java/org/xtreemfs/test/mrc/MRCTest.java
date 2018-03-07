/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.ACCESS_FLAGS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Setattrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_suitable_osdsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_replica_update_policyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_addRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;
import org.xtreemfs.test.TestHelper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

/**
 * XtreemFS MRC test
 * 
 * @author stender
 */
public class MRCTest {
    @Rule
    public final TestRule     testLog = TestHelper.testLog;
    
    private MRCServiceClient  client;
    
    private InetSocketAddress mrcAddress;
    
    private TestEnvironment   testEnv;
    
    public MRCTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    @Before
    public void setUp() throws Exception {
        mrcAddress = SetupUtils.getMRC1Addr();
        
        // register an OSD at the directory service (needed in order to assign
        // it to a new file on 'open')
        
        testEnv = new TestEnvironment(Services.DIR_CLIENT, Services.TIME_SYNC, Services.UUID_RESOLVER,
                                      Services.MRC_CLIENT, Services.DIR_SERVICE, Services.MRC,
                                      Services.OSD, Services.OSD, Services.OSD);
        testEnv.start();
        
        client = testEnv.getMrcClient();
    }
    
    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }
    
    @Test
    public void testReCreateVolumes() throws Exception {
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final UserCredentials uc = createUserCredentials(uid, gids);

        // Using a large number of volumes to generate load while creation
        for (int i = 25; i <= 30; i++) {
            // Create volumes
            for (int j = 0; j < i; j++) {
                String name = "vol-" + j;
                invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
                        AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
                        name, "", "", getKVList("i", String.valueOf(i)), 0));
            }

            // Check number of created volumes
            Volumes vols = invokeSync(client.xtreemfs_lsvol(mrcAddress, RPCAuthentication.authNone, uc));
            assertEquals(vols.getVolumesCount(), i);

            // Try to create existing volumes
            for (int j = 0; j < i; j++) {
                String name = "vol-" + j;
                try {
                    invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
                            AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
                            name, "", "", getKVList("i", String.valueOf(i)), 0));
                    fail();
                } catch (Exception ex) {
                    vols = invokeSync(client.xtreemfs_lsvol(mrcAddress, RPCAuthentication.authNone, uc));
                    assertEquals(vols.getVolumesCount(), i);
                }
            }

            // Delete created volumes
            for (int j = 0; j < i; j++) {
                String volName = "vol-" + j;
                invokeSync(client.xtreemfs_rmvol(mrcAddress, RPCAuthentication.authNone, uc, volName));
            }
            vols = invokeSync(client.xtreemfs_lsvol(mrcAddress, RPCAuthentication.authNone, uc));
            assertEquals(vols.getVolumesCount(), 0);
        }
    }

    @Test
    public void testCreateDeleteListVolumes() throws Exception {
        
        final int numVols = 10;
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        Set<String> volNames = new HashSet<String>();
        
        // create multiple volumes
        for (int i = 0; i < numVols; i++) {
            
            String name = "vol-" + i;
            
            invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
                name, "", "", getKVList("bla", "blub"), 0));
            
            volNames.add(name);
        }
        
        for (int i = numVols - 1; i >= 0; i--) {
            String name = "vol-" + i;
            Stat stat1 = invokeSync(client.getattr(mrcAddress, RPCAuthentication.authNone, uc, name, "", -1))
                    .getStbuf();
            assertNotNull(stat1);
        }
        
        // list all volumes
        Set<String> tmp = new HashSet<String>(volNames);
        Volumes vols = invokeSync(client.xtreemfs_lsvol(mrcAddress, RPCAuthentication.authNone, uc));
        for (int i = 0; i < vols.getVolumesCount(); i++) {
            String volName = vols.getVolumes(i).getName();
            assertTrue(tmp.remove(volName));
        }
        assertEquals(0, tmp.size());
        
        // delete all even-numbered volumes
        for (int i = 0; i < numVols; i += 2) {
            String volName = "vol-" + i;
            invokeSync(client.xtreemfs_rmvol(mrcAddress, RPCAuthentication.authNone, uc, volName));
            volNames.remove(volName);
        }
        
        // list all volumes
        vols = invokeSync(client.xtreemfs_lsvol(mrcAddress, RPCAuthentication.authNone, uc));
        for (int i = 0; i < vols.getVolumesCount(); i++) {
            String volName = vols.getVolumes(i).getName();
            assertTrue(volNames.remove(volName));
        }
        
    }
    
    @Test
    public void testCreateDelete() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        // create and delete a volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
            volumeName, "", "", getKVList("bla", "blub"), 0));
        
        Volumes localVols = invokeSync(client.xtreemfs_lsvol(mrcAddress, RPCAuthentication.authNone, uc));
        assertEquals(1, localVols.getVolumesCount());
        assertEquals(volumeName, localVols.getVolumes(0).getName());
        assertEquals(1, localVols.getVolumes(0).getAttrsList().size());
        assertEquals("bla", localVols.getVolumes(0).getAttrsList().get(0).getKey());
        assertEquals("blub", localVols.getVolumes(0).getAttrsList().get(0).getValue());
        invokeSync(client.xtreemfs_rmvol(mrcAddress, RPCAuthentication.authNone, uc, volumeName));
        localVols = invokeSync(client.xtreemfs_lsvol(mrcAddress, RPCAuthentication.authNone, uc));
        assertEquals(0, localVols.getVolumesCount());
        
        // create a volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0775,
            volumeName, "", "", getKVList(), 0));
        
        // create some files and directories
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "myDir", 0775));
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "anotherDir", 0775));
        
        for (int i = 0; i < 10; i++)
            invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "myDir/test" + i
                + ".txt", FileAccessManager.O_CREAT, 0775, 0, getDefaultCoordinates()));
        
        // try to create a file w/o a name
        try {
            invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "",
                FileAccessManager.O_CREAT, 0775, 0, getDefaultCoordinates()));
            fail("missing filename");
        } catch (PBRPCException exc) {
        }
        
        try {
            invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "myDir/test0.txt",
                FileAccessManager.O_CREAT | FileAccessManager.O_EXCL, 0775, 0, getDefaultCoordinates()));
            fail("duplicate file creation");
        } catch (PBRPCException exc) {
        }
        
        try {
            invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName,
                "myDir/test0.txt/bla.txt", FileAccessManager.O_CREAT, 0775, 0, getDefaultCoordinates()));
            fail("file in file creation");
        } catch (PBRPCException exc) {
        }
        
        try {
            invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "", 0));
            fail("directory already exists");
        } catch (PBRPCException exc) {
            
        }
        
        // test 'readDir' and 'stat'
        
        DirectoryEntries entrySet = invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc,
            volumeName, "", -1, 1000, false, 0));
        assertEquals(4, entrySet.getEntriesCount());
        
        entrySet = invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "myDir",
            -1, 1000, false, 0));
        assertEquals(12, entrySet.getEntriesCount());
        
        Stat stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "myDir/test2.txt", -1))
                .getStbuf();
        assertEquals(uid, stat.getUserId());
        assertTrue("test2.txt is a not a file", (stat.getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFREG
                .getNumber()) != 0);
        assertEquals(0, stat.getSize());
        assertTrue(stat.getAtimeNs() > 0);
        assertTrue(stat.getCtimeNs() > 0);
        assertTrue(stat.getMtimeNs() > 0);
        assertTrue((stat.getMode() & 511) > 0);
        assertEquals(1, stat.getNlink());
        
        // test 'delete'
        
        invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "myDir/test3.txt"));
        
        entrySet = invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "myDir",
            -1, 1000, false, 0));
        assertEquals(11, entrySet.getEntriesCount());
        
        invokeSync(client.rmdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "anotherDir"));
    }
    
    @Test
    public void testReaddir() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0,
            volumeName, "", "", getKVList(), 0));
        invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "/", -1, 1000,
            false, 0));
    }
    
    @Test
    public void testXAttrs() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0,
            volumeName, "", "", getKVList(), 0));
        
        // create a file and add some user attributes
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            "key1", "quark", ByteString.copyFrom("quark".getBytes()), 0));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            "key2", "quatsch", ByteString.copyFrom("quatsch".getBytes()), 0));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            "myAttr", "171", ByteString.copyFrom("171".getBytes()), 0));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            "key1", "blub", ByteString.copyFrom("blub".getBytes()), 0));
        
        List<XAttr> xattrs = invokeSync(
            client.listxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt", false))
                .getXattrsList();
        List<String> attrKeys = new LinkedList<String>();
        for (XAttr attr : xattrs)
            if (!attr.getName().startsWith("xtreemfs."))
                attrKeys.add(attr.getName());
        assertEquals(3, attrKeys.size());
        String val = invokeSync(
            client.getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt", "key1"))
                .getValue();
        assertEquals("blub", val);
        val = invokeSync(
            client.getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt", "key2"))
                .getValue();
        assertEquals("quatsch", val);
        val = invokeSync(
            client.getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt", "myAttr"))
                .getValue();
        assertEquals("171", val);
        
        // check if / works
        val = invokeSync(
            client.getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "/", "xtreemfs.url"))
                .getValue();
        
        // create a new file, add some attrs and delete some attrs
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
            "key1", "quark", ByteString.copyFrom("quark".getBytes()), 0));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
            "key2", "quatsch", ByteString.copyFrom("quatsch".getBytes()), 0));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
            "key3", "171", ByteString.copyFrom("171".getBytes()), 0));
        
        invokeSync(client.removexattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
            "key1"));
        xattrs = invokeSync(
            client.listxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", false))
                .getXattrsList();
        attrKeys = new LinkedList<String>();
        for (XAttr attr : xattrs)
            if (!attr.getName().startsWith("xtreemfs."))
                attrKeys.add(attr.getName());
        assertEquals(2, attrKeys.size());
        try {
            val = invokeSync(
                client.getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", "key1"))
                    .getValue();
            fail("got value for non-existing key");
        } catch (PBRPCException exc) {
            assertEquals(POSIXErrno.POSIX_ERROR_ENODATA, exc.getPOSIXErrno());
        }
        
        invokeSync(client.removexattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
            "key3"));
        xattrs = invokeSync(
            client.listxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", false))
                .getXattrsList();
        attrKeys = new LinkedList<String>();
        for (XAttr attr : xattrs)
            if (!attr.getName().startsWith("xtreemfs."))
                attrKeys.add(attr.getName());
        assertEquals(1, attrKeys.size());
        try {
            val = invokeSync(
                client.getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", "key3"))
                    .getValue();
            fail("got value for non-existing key");
        } catch (PBRPCException exc) {
            assertEquals(POSIXErrno.POSIX_ERROR_ENODATA, exc.getPOSIXErrno());
        }
        
        // retrieve a system attribute
        val = invokeSync(
            client.getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
                "xtreemfs.object_type")).getValue();
        assertEquals("1", val);
        
        // check read-only replication
        XLocSet xLoc = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "repl",
                FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates())).getCreds().getXlocs();
        assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, xLoc.getReplicaUpdatePolicy());
        
        xtreemfs_set_replica_update_policyRequest.Builder msg = xtreemfs_set_replica_update_policyRequest.newBuilder()
                .setVolumeName(volumeName).setPath("repl").setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY);
        invokeSync(client.xtreemfs_set_replica_update_policy(mrcAddress, RPCAuthentication.authNone, uc, msg.build()));
        xLoc = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "repl",
                FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates())).getCreds().getXlocs();
        assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, xLoc.getReplicaUpdatePolicy());
    }
    
    @Test
    public void testLargeXAttrs() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0,
            volumeName, "", "", getKVList(), 0));
        
        // create a file and add some user attributes
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        byte[] largeAttr = new byte[9000];
        for(int i = 0; i < largeAttr.length; i++)
            largeAttr[i] = (byte) ((Math.random() * 256) -128);
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            "key1", "", ByteString.copyFrom(largeAttr), 0));
        
        byte[] val = invokeSync(
                client.getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt", "key1"))
                .getValueBytes().toByteArray();
        assertEquals(largeAttr.length, val.length);
        for (int i = 0; i < largeAttr.length; i++)
            assertEquals(largeAttr[i], val[i]);
    }
    
    @Test
    public void testSymlink() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0,
            volumeName, "", "", getKVList(), 0));
        
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        
        // create and test a symbolic link
        invokeSync(client.symlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            "testAlias.txt"));
        String target = invokeSync(
            client.readlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "testAlias.txt"))
                .getLinkTargetPath(0);
        assertEquals("test.txt", target);


        // delete link
        invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "testAlias.txt"));
    }
    
    @Test
    public void testHardLink() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0,
            volumeName, "", "", getKVList(), 0));
        
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test1.txt",
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        
        // create a new link
        invokeSync(client.link(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test1.txt",
            "test2.txt"));
        
        // check whether both links refer to the same file
        Stat stat1 = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test1.txt", -1))
                .getStbuf();
        Stat stat2 = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", -1))
                .getStbuf();
        
        assertEquals(stat1.getIno(), stat2.getIno());
        assertEquals(2, stat1.getNlink());
        
        // create another link to the second file
        invokeSync(client.link(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
            "test3.txt"));
        
        // check whether both links refer to the same file
        stat1 = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", -1))
                .getStbuf();
        stat2 = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test3.txt", -1))
                .getStbuf();
        
        assertEquals(stat1.getIno(), stat2.getIno());
        assertEquals(3, stat1.getNlink());
        
        // delete one of the links
        invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test1.txt"));
        Stat stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", -1))
                .getStbuf();
        assertEquals(2, stat.getNlink());
        
        // delete the other two links
        invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt"));
        invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test3.txt"));
        
        try {
            stat = invokeSync(
                client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test1.txt", -1))
                    .getStbuf();
            fail("file should not exist anymore");
        } catch (PBRPCException exc) {
        }
        
        try {
            stat = invokeSync(
                client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", -1))
                    .getStbuf();
            fail("file should not exist anymore");
        } catch (PBRPCException exc) {
        }
        
        // create two links to a directory
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "testDir1", 0));
        try {
            invokeSync(client.link(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "testDir1",
                "testDir1/testDir2"));
            fail("links to directories should not be allowed");
        } catch (Exception exc) {
        }
    }
    
    @Test
    public void testOpen() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
            volumeName, "", "", getKVList(), 0));
        
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            FileAccessManager.O_CREAT, 0774, 0, getDefaultCoordinates()));
        
        // open w/ O_RDWR; should not fail
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            FileAccessManager.O_RDWR, 0, 0, getDefaultCoordinates()));
        
        // open w/ O_RDONLY; should not fail
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates()));
        
        // create a new file w/ O_CREAT; should implicitly create a new file
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
            FileAccessManager.O_CREAT, 256, 0, getDefaultCoordinates()));
        invokeSync(client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt", -1));
        
        // open w/ O_WRONLY; should fail
        try {
            invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
                FileAccessManager.O_WRONLY, 256, 0, getDefaultCoordinates()));
            fail();
        } catch (PBRPCException exc) {
            assertEquals(POSIXErrno.POSIX_ERROR_EACCES, exc.getPOSIXErrno());
        }
        
        // open a directory; should fail
        try {
            invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "dir",
                FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates()));
            fail("opened directory");
        } catch (PBRPCException exc) {
        }
        
        // open a file in order to obtain a capability
        XCap xcap = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test2.txt",
                FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates())).getCreds().getXcap();
        
        // wait one second before renewing the capability
        Thread.sleep(1000);
        
        // test renewing a capability
        XCap newCap = invokeSync(client.xtreemfs_renew_capability(mrcAddress, RPCAuthentication.authNone,
                RPCAuthentication.userService, xcap));
        assertTrue(xcap.getExpireTimeS() < newCap.getExpireTimeS());
        
        // open w/ truncate flag; check whether the epoch number is incremented
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "trunc",
            FileAccessManager.O_CREAT, 0777, 0, getDefaultCoordinates()));
        xcap = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "trunc",
                FileAccessManager.O_TRUNC, 0, 0, getDefaultCoordinates())).getCreds().getXcap();
        assertEquals(1, xcap.getTruncateEpoch());
        xcap = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "trunc",
                FileAccessManager.O_TRUNC, 0, 0, getDefaultCoordinates())).getCreds().getXcap();
        assertEquals(2, xcap.getTruncateEpoch());
        
        // TODO: check open w/ ACLs set
        
        // test truncate
        
        // open w/ write cap and truncate
        xcap = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "trunc",
                FileAccessManager.O_RDWR, 0, 0, getDefaultCoordinates())).getCreds().getXcap();
        invokeSync(client.ftruncate(mrcAddress, RPCAuthentication.authNone, RPCAuthentication.userService,
            xcap));
        
        xcap = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "trunc",
                FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates())).getCreds().getXcap();
        try {
            invokeSync(client.ftruncate(mrcAddress, RPCAuthentication.authNone,
                RPCAuthentication.userService, xcap));
            fail("truncated file w/o write permissions");
        } catch (PBRPCException exc) {
            assertEquals(POSIXErrno.POSIX_ERROR_EACCES, exc.getPOSIXErrno());
        }
    }
    
    @Test
    public void testOpenCreateNoPerm() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
            volumeName, "", "", getKVList(), 0));
        
        final String uid2 = "bla";
        final List<String> gids2 = createGIDs("groupY");
        final UserCredentials uc2 = createUserCredentials(uid2, gids2);
        
        // open O_CREATE as uid2 should fail
        try {
            invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc2, volumeName, "test2.txt",
                (FileAccessManager.O_WRONLY | FileAccessManager.O_CREAT), 256, 0, getDefaultCoordinates()));
            fail();
        } catch (PBRPCException exc) {
            assertEquals(POSIXErrno.POSIX_ERROR_EACCES, exc.getPOSIXErrno());
        }
        
    }
    
    @Test
    public void testRename() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0,
            volumeName, "", "", getKVList(), 0));
        
        // create some files and directories
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "blub.txt",
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "mainDir", 0));
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "mainDir/subDir", 0));
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName,
            "mainDir/subDir/newDir", 0));
        
        assertTree(mrcAddress, uid, gids, volumeName, "", "test.txt", "blub.txt", "mainDir",
            "mainDir/subDir", "mainDir/subDir/newDir");
        
        // move some files and directories
        
        // file -> none (create w/ different name)
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            "mainDir/bla.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/bla.txt", "blub.txt", "mainDir",
            "mainDir/subDir", "mainDir/subDir/newDir");
        
        // file -> file (overwrite)
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "mainDir/bla.txt",
            "blub.txt"));
        
        assertTree(mrcAddress, uid, gids, volumeName, "", "blub.txt", "mainDir", "mainDir/subDir",
            "mainDir/subDir/newDir");
        
        // file -> none (create w/ same name)
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "blub.txt",
            "mainDir/blub.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/blub.txt", "mainDir", "mainDir/subDir",
            "mainDir/subDir/newDir");
        
        // file -> dir (invalid operation)
        try {
            invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName,
                "mainDir/blub.txt", "mainDir/subDir"));
            fail("move file -> directory should not be possible");
        } catch (PBRPCException exc) {
        }
        
        // file -> file (same path, should have no effect)
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "mainDir/blub.txt",
            "mainDir/blub.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/blub.txt", "mainDir", "mainDir/subDir",
            "mainDir/subDir/newDir");
        
        // file -> file (same directory)
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "mainDir/blub.txt",
            "mainDir/blub2.txt"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/blub2.txt", "mainDir", "mainDir/subDir",
            "mainDir/subDir/newDir");
        
        // dir -> none (create w/ same name)
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "mainDir/subDir",
            "subDir"));
        
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir/blub2.txt", "mainDir", "subDir",
            "subDir/newDir");
        
        // dir -> dir (overwrite, should fail because of non-empty subdirectory)
        try {
            invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "subDir/newDir",
                "subDir"));
            fail("moved directory to non-empty directory");
        } catch (PBRPCException exc) {
        }
        
        // dir -> dir (overwrite)
        invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "mainDir/blub2.txt"));
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "subDir", "mainDir"));
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir", "mainDir/newDir");
        
        // dir -> volume (should fail because volume can't be overwritten)
        try {
            invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName,
                "mainDir/newDir", ""));
            fail("move overwrote volume root");
        } catch (PBRPCException exc) {
        }
        
        // dir -> invalid volume (should fail)
        try {
            invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "", "somewhere"));
            fail("moved to invalid volume");
        } catch (PBRPCException exc) {
        }
        
        assertTree(mrcAddress, uid, gids, volumeName, "", "mainDir", "mainDir/newDir");
        
        invokeSync(client.symlink(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "mainDir", "link"));
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "link", "newlink"));
        
    }
    
    @Test
    public void testAccessControl() throws Exception {
        
        final String uid1 = "userXY";
        final List<String> gids1 = createGIDs("groupZ");
        final String uid2 = "userAB";
        final List<String> gids2 = createGIDs("groupA");
        final String uid3 = "userZZ";
        final List<String> gids3 = createGIDs("groupY");
        final String uid4 = "root";
        final List<String> gids4 = createGIDs("root");
        
        final UserCredentials uc1 = createUserCredentials(uid1, gids1);
        final UserCredentials uc2 = createUserCredentials(uid2, gids2);
        final UserCredentials uc3 = createUserCredentials(uid3, gids3);
        final UserCredentials uc4 = createUserCredentials(uid4, gids4);
        
        final String noACVolumeName = "noACVol";
        final String volACVolumeName = "volACVol";
        final String posixVolName = "posixVol";
        
        // NO ACCESS CONTROL
        
        // create a volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc1,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0,
            noACVolumeName, "", "", getKVList(), 0));
        
        // test chown
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc1, noACVolumeName, "chownTestFile",
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.setattr(mrcAddress, RPCAuthentication.authNone, uc4, noACVolumeName,
            "chownTestFile", createChownStat("newUser", "newGroup"), Setattrs.SETATTR_UID.getNumber()
                | Setattrs.SETATTR_GID.getNumber()));
        
        Stat stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc3, noACVolumeName, "chownTestFile", -1))
                .getStbuf();
        assertEquals("newUser", stat.getUserId());
        assertEquals("newGroup", stat.getGroupId());
        
        invokeSync(client
                .unlink(mrcAddress, RPCAuthentication.authNone, uc3, noACVolumeName, "chownTestFile"));
        
        // create a new directory; should succeed
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc1, noACVolumeName, "newDir", 0));
        
        // create a new file inside the dir: should succeed (in spite of
        // not having explicitly set any rights on the parent directory)
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc2, noACVolumeName,
            "newDir/newFile", 0));
        
        final UserCredentials ucS = createUserCredentials("someone", createGIDs("somegroup"));
        assertNotNull(invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, ucS, noACVolumeName,
            "newDir/newFile", -1, 1000, false, 0)));
        
        // VOLUME policy
        
        // create a volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc1,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_VOLUME, getDefaultStripingPolicy(), "", 0700,
            volACVolumeName, "", "", getKVList(), 0));
        
        // create a new directory: should succeed for user1, fail
        // for user2
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc1, volACVolumeName, "newDir", 0));
        try {
            invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName, "newDir2", 0));
            fail("access should have been denied");
        } catch (PBRPCException exc) {
        }
        
        // create a subdirectory for 'newDir'; should succeed for user1
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc1, volACVolumeName,
            "newDir/subDir", 0));
        
        // POSIX policy
        
        // create a volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc1,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
            posixVolName, "", "", getKVList(), 0));
        
        invokeSync(client.setattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "",
            createChmodStat(0700), Setattrs.SETATTR_MODE.getNumber()));
        
        // create a new directory: should succeed for user1, fail for user2
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "newDir", 0700));
        
        // check permissions by opening the file
        assertNotNull(invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName,
            "newDir", -1, 1000, false, 0)));
        
        try {
            invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName, "newDir2",
                0700));
            fail("access should have been denied");
        } catch (PBRPCException exc) {
        }
        
        // check 'access' call
        
        try {
            invokeSync(client.access(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "grsasd",
                    ACCESS_FLAGS.ACCESS_FLAGS_F_OK.getNumber()));
            fail("access should have been denied");
        } catch (PBRPCException exc) {
            if (exc.getPOSIXErrno() != POSIXErrno.POSIX_ERROR_EACCES) {
                fail("wrong error returned");
            }
        }

        try {
            invokeSync(client.access(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "grsasd",
                    ACCESS_FLAGS.ACCESS_FLAGS_R_OK.getNumber()));
            fail("access should have been denied");
        } catch (PBRPCException exc) {
            if (exc.getPOSIXErrno() != POSIXErrno.POSIX_ERROR_EACCES) {
                fail("wrong error returned");
            }
        }

        try {
            invokeSync(client.access(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName, "grsasd",
                ACCESS_FLAGS.ACCESS_FLAGS_F_OK.getNumber()));
            fail("access should have been denied");
        } catch (PBRPCException exc) {
        }
        
        invokeSync(client.access(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "newDir",
            ACCESS_FLAGS.ACCESS_FLAGS_F_OK.getNumber()));
        
        invokeSync(client.access(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "newDir",
            ACCESS_FLAGS.ACCESS_FLAGS_R_OK.getNumber() | ACCESS_FLAGS.ACCESS_FLAGS_W_OK.getNumber()));
        
        try {
            invokeSync(client.access(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName, "newDir2",
                ACCESS_FLAGS.ACCESS_FLAGS_R_OK.getNumber() | ACCESS_FLAGS.ACCESS_FLAGS_W_OK.getNumber()));
            fail("access should have been denied");
        } catch (PBRPCException exc) {
        }
        
        // TODO: test getting/setting ACL entries
        
        // change the access mode
        invokeSync(client.setattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "newDir",
            createChmodStat(0), Setattrs.SETATTR_MODE.getNumber()));
        
        // readdir on "/newDir"; should fail for any user now
        try {
            invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "newDir",
                -1, 1000, false, 0));
            fail("access should have been denied");
        } catch (PBRPCException exc) {
        }
        
        try {
            invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName, "newDir",
                -1, 1000, false, 0));
            fail("access should have been denied");
        } catch (PBRPCException exc) {
        }
        
        // set access rights to anyone (except for the owner)
        invokeSync(client.setattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "newDir",
            createChmodStat(0007), Setattrs.SETATTR_MODE.getNumber()));
        
        try {
            invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "newDir",
                -1, 1000, false, 0));
            fail("access should have been denied due to insufficient permissions");
        } catch (PBRPCException exc) {
        }
        
        try {
            invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc3, posixVolName, "newDir",
                -1, 1000, false, 0));
            fail("access should have been denied due to insufficient search permissions");
        } catch (PBRPCException exc) {
        }
        
        // set search rights on the root directory to 'others'
        invokeSync(client.setattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "",
            createChmodStat(0001), Setattrs.SETATTR_MODE.getNumber()));
        
        // access should be granted to others now
        invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc3, posixVolName, "newDir", -1,
            1000, false, 0));
        
        // check permissions
        assertNotNull(invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName,
            "newDir", -1, 1000, false, 0)));
        
        // check permissions
        assertNotNull(invokeSync(client.getattr(mrcAddress, RPCAuthentication.authNone, uc3, posixVolName,
            "", -1)));
        
        // grant any rights to the volume to anyone
        invokeSync(client.setattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "",
            createChmodStat(0777), Setattrs.SETATTR_MODE.getNumber()));
        
        // owner of 'newDir' should still not have access rights
        try {
            invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "newDir",
                -1, 1000, false, 0));
            fail("access should have been denied due to insufficient permissions");
        } catch (PBRPCException exc) {
        }
        
        // others should still have no write permissions
        try {
            invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName,
                "newDir/newfile", FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
            fail();
        } catch (PBRPCException exc) {
        }
        
        // create a POSIX ACL new volume and test "chmod"
        invokeSync(client.xtreemfs_rmvol(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName));
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc1,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
            posixVolName, "", "", getKVList(), 0));
        
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "someFile.txt",
            FileAccessManager.O_CREAT, 224, 0, getDefaultCoordinates()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "someFile.txt", -1))
                .getStbuf();
        assertEquals(224, stat.getMode() & 0x7FF);
        
        invokeSync(client.setattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "someFile.txt",
            createChmodStat(192), Setattrs.SETATTR_MODE.getNumber()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "someFile.txt", -1))
                .getStbuf();
        assertEquals(192, stat.getMode() & 0x7FF);
        
        // create a new directory w/ search access for anyone w/ access rights
        // to anyone
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "stickyDir", 0777));
        
        // create and delete/rename a file w/ different user IDs: this should
        // work
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName,
            "stickyDir/newfile.txt", FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName,
            "stickyDir/newfile.txt"));
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc3, posixVolName,
            "stickyDir/newfile.txt", FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName,
            "stickyDir/newfile.txt", "stickyDir/newfile2.txt"));
        
        // create a file and set sticky bit on the directory; now, only the
        // owner should be allowed to delete/rename the nested file
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName,
            "stickyDir/newfile.txt", FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.setattr(mrcAddress, RPCAuthentication.authNone, uc1, posixVolName, "stickyDir",
            createChmodStat(01777), Setattrs.SETATTR_MODE.getNumber()));
        
        try {
            invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc3, posixVolName,
                "stickyDir/newfile.txt"));
            fail("access should have been denied due to insufficient delete permissions (sticky bit)");
        } catch (PBRPCException exc) {
        }
        try {
            invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc3, posixVolName,
                "stickyDir/newfile.txt", "stickyDir/newfile3.txt"));
            fail("access should have been denied due to insufficient renaming permissions (sticky bit)");
        } catch (PBRPCException exc) {
        }
        
        invokeSync(client.rename(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName,
            "stickyDir/newfile.txt", "stickyDir/newfile3.txt"));
        invokeSync(client.unlink(mrcAddress, RPCAuthentication.authNone, uc2, posixVolName,
            "stickyDir/newfile3.txt"));
    }
    
    @Test
    public void testFileSizeUpdate() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final String fileName = "testFile";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        // create a new file in a new volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, getDefaultStripingPolicy(), "", 0,
            volumeName, "", "", getKVList(), 0));
        
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName,
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        
        // check and update file sizes repeatedly
        XCap cap = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName,
                FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates())).getCreds().getXcap();
        Stat stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(0L, stat.getSize());
        
        OSDWriteResponse owr = createFSResponse(27, 0);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(27L, stat.getSize());
        
        owr = createFSResponse(12, 0);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(27L, stat.getSize());
        
        owr = createFSResponse(34, 0);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(34L, stat.getSize());
        
        owr = createFSResponse(10, 1);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(10L, stat.getSize());
        
        owr = createFSResponse(34, 1);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(34L, stat.getSize());
        
        owr = createFSResponse(10, 1);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(34L, stat.getSize());
        
        owr = createFSResponse(0, 2);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(0L, stat.getSize());
        
        owr = createFSResponse(12, 0);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(0L, stat.getSize());
        
        owr = createFSResponse(32, 4);
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xtreemfs_update_file_sizeRequest.newBuilder().setOsdWriteResponse(
                owr).setXcap(cap).build()));
        stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName, -1)).getStbuf();
        assertEquals(32L, stat.getSize());
    }
    
    @Test
    public void testDefaultStripingPolicies() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        final String volumeName = "testVolume";
        final String dirName = "dir";
        final String fileName1 = dirName + "/testFile";
        final String fileName2 = dirName + "/testFile2";
        
        StripingPolicy sp1 = StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                .setStripeSize(64).setWidth(1).build();
        
        // create a new file in a directory in a new volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, sp1, "", 0, volumeName, "", "", getKVList(), 0));
        
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, dirName, 0));
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName1,
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName2,
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        
        // check if the striping policy assigned to the file matches the default
        // striping policy
        XLocSet xLoc = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName1,
                FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates())).getCreds().getXlocs();
        
        StripingPolicy sp = xLoc.getReplicas(0).getStripingPolicy();
        assertEquals(sp1.getType().name(), sp.getType().name());
        assertEquals(sp1.getWidth(), sp.getWidth());
        assertEquals(sp1.getStripeSize(), sp.getStripeSize());
        
        // check block size in Stat
        Stat stat = invokeSync(
            client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName1, -1)).getStbuf();
        assertEquals(sp1.getStripeSize() * 1024, stat.getBlksize());
        
        stat = invokeSync(client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, dirName, -1))
                .getStbuf();
        assertEquals(0, stat.getBlksize());
        
    }
    
    @Test
    public void testDefaultReplicationPolicies() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        final String volumeName = "testVolume";
        final String dirName = "dir";
        final String fileName = dirName + "/testFile";
        
        final StripingPolicy sp = StripingPolicy.newBuilder().setType(
            StripingPolicyType.STRIPING_POLICY_RAID0).setStripeSize(64).setWidth(1).build();
        
        ReplicationPolicy rp = new ReplicationPolicy() {
            
            @Override
            public String getName() {
                return ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE;
            }
            
            @Override
            public int getFactor() {
                return 2;
            }
            
            @Override
            public int getFlags() {
                return 0;
            }
            
        };
        
        // create a new file in a directory in a new volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, sp, "", 0, volumeName, "", "", getKVList(), 0));
        
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "",
            "xtreemfs.default_rp", "", ByteString.copyFrom(Converter.replicationPolicyToJSONString(rp).getBytes()), 0));
        
        String val = invokeSync(
            client
                    .getxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "",
                        "xtreemfs.default_rp")).getValue();
        ReplicationPolicy pol = Converter.jsonStringToReplicationPolicy(val);
        
        assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE, pol.getName());
        assertEquals(rp.getFactor(), pol.getFactor());
        assertEquals(rp.getFlags(), pol.getFlags());
        
        invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, dirName, 0));
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName,
            FileAccessManager.O_CREAT, 0, 0, getDefaultCoordinates()));
        
        // check if the striping policy assigned to the file matches the default
        // striping policy
        XLocSet xLoc = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, fileName,
                FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates())).getCreds().getXlocs();
        
        assertEquals(rp.getName(), xLoc.getReplicaUpdatePolicy());
        assertEquals(rp.getFactor(), xLoc.getReplicasCount());
    }

    @Test
    public void testMarkReplicaComplete() throws Exception {
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);

        final String fileName = "testFile";

        // create volume and file
        final StripingPolicy sp = StripingPolicy.newBuilder().setType(
                StripingPolicyType.STRIPING_POLICY_RAID0).setStripeSize(64).setWidth(1).build();

        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
                                         AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                                         sp, "", 0, volumeName,
                                         "", "",
                                         getKVList(), 0));

        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone,
                               uc, volumeName, fileName, FileAccessManager.O_CREAT,
                               0775, 0, getDefaultCoordinates()));

        // store the UUID of the OSD automatically assigned to the file
        listxattrResponse listxattrResponse =
                invokeSync(client.listxattr(mrcAddress, RPCAuthentication.authNone,
                                            uc, volumeName, fileName, false));
        String locations = null;
        for (XAttr xAttr : listxattrResponse.getXattrsList()) {
            if (xAttr.getName().equals("xtreemfs.locations")) {
                 locations = xAttr.getValue();
            }
        }
        String oldOSD = extractOSDUUIDs(locations).get(0);

        String fileID = "";
        for (XAttr xAttr: listxattrResponse.getXattrsList()) {
            if (xAttr.getName().equals("xtreemfs.file_id")) {
                fileID = xAttr.getValue();
            }
        }

        // set the replica update policy and create a new full replica on a suitable OSD
        xtreemfs_set_replica_update_policyRequest msg =
                xtreemfs_set_replica_update_policyRequest.newBuilder()
                        .setVolumeName(volumeName)
                        .setPath(fileName)
                        .setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)
                        .build();
        invokeSync(client.xtreemfs_set_replica_update_policy(mrcAddress,
                                                             RPCAuthentication.authNone,
                                                             uc, msg));

        xtreemfs_get_suitable_osdsResponse suitable_osdsResponse =
                invokeSync(client.xtreemfs_get_suitable_osds(mrcAddress,
                                                             RPCAuthentication.authNone,
                                                            uc,
                                                            fileID, "", "",
                                                             0));

        String newOSD = suitable_osdsResponse.getOsdUuidsList().get(0);

        GlobalTypes.Replica replica =
                GlobalTypes.Replica.newBuilder()
                        .addOsdUuids(newOSD)
                        .setReplicationFlags(ReplicationFlags.setRarestFirstStrategy(GlobalTypes.REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber()))
                        .setStripingPolicy(sp)
                        .build();

        xtreemfs_replica_addRequest xtreemfsReplicaAddRequest =
                xtreemfs_replica_addRequest.newBuilder()
                        .setFileId(fileID)
                        .setNewReplica(replica)
                        .build();

        invokeSync(client.xtreemfs_replica_add(mrcAddress,
                                               RPCAuthentication.authNone,
                                               uc,
                                               xtreemfsReplicaAddRequest));

        // changes to the meta data database are performed in the background,
        // (the MRC sends the response before changes are made)
        // so we have to wait a little before starting the next change
        Thread.sleep(200);

        // try to remove the old replica - this should fail,
        // as the new replica is not complete yet
        try {
            invokeSync(client.xtreemfs_replica_remove(mrcAddress,
                                                      RPCAuthentication.authNone,
                                                      uc,
                                                      fileID,
                                                      "", "",
                                                      oldOSD));
            fail("deleting the only complete replica should fail!");
        } catch (PBRPCException e) {
            // this exception is expected
        }

        // simulate that the OSD of the new replica has fetched all data of the file
        invokeSync(client.xtreemfs_replica_mark_complete(mrcAddress,
                                                         RPCAuthentication.authNone,
                                                         uc,
                                                         fileID,
                                                         newOSD));

        Thread.sleep(200);

        // now that the new replica has all data and has been marked as complete,
        // it should be possible to delete the old replica
        try {
            invokeSync(client.xtreemfs_replica_remove(mrcAddress,
                                                      RPCAuthentication.authNone,
                                                      uc,
                                                      fileID,
                                                      "", "",
                                                      oldOSD));
        } catch (PBRPCException e) {
            fail("after marking the new replica complete, " +
                         "deleting the old one should be possible!");
        }

        Thread.sleep(200);

        listxattrResponse =
                invokeSync(client.listxattr(mrcAddress, RPCAuthentication.authNone,
                                            uc, volumeName, fileName, false));

        for (XAttr xAttr : listxattrResponse.getXattrsList()) {
            if (xAttr.getName().equals("xtreemfs.locations")) {
                locations = xAttr.getValue();
            }
        }

        // check whether the new replica is on the new OSD and the only replica
        String currentOSD = extractOSDUUIDs(locations).get(0);
        assertEquals(1, extractOSDUUIDs(locations).size());
        assertEquals(newOSD, currentOSD);
        assertNotEquals(oldOSD, currentOSD);
    }
    
    @Test
    public void testReplicateOnClose() throws Exception {
        
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        // create a volume
        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
            volumeName, "", "", getKVList(), 0));
        
        // auto-assign three (two more) replicas to each newly-created file
        ReplicationPolicy rp = new ReplicationPolicy() {
            
            @Override
            public String getName() {
                return ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY;
            }
            
            @Override
            public int getFactor() {
                return 3;
            }
            
            @Override
            public int getFlags() {
                return 0;
            }
            
        };
        
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "",
            "xtreemfs.default_rp", "", ByteString.copyFrom(Converter.replicationPolicyToJSONString(rp).getBytes()), 0));
        
        // create a new file
        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
            FileAccessManager.O_CREAT, 0775, 0, getDefaultCoordinates()));
        
        // open the file
        FileCredentials creds = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
                FileAccessManager.O_RDWR, 0, 0, getDefaultCoordinates())).getCreds();
        XCap xCap = creds.getXcap();
        
        // close the file
        invokeSync(client.xtreemfs_update_file_size(mrcAddress, RPCAuthentication.authNone,
            RPCAuthentication.userService, xCap, OSDWriteResponse.getDefaultInstance(), true,
            getDefaultCoordinates()));
        
        // open the file again
        creds = invokeSync(
            client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
                FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates())).getCreds();
        XLocSet xLoc = creds.getXlocs();
        
        // check whether there are three replicas now, and replica 0 has the
        // correct replication flags
        assertEquals(3, xLoc.getReplicasCount());
        assertTrue((ReplicationFlags.setReplicaIsComplete(0)
                    & xLoc.getReplicas(0).getReplicationFlags()) != 0);
        
        // check if the file is read-only
        try {
            creds = invokeSync(
                client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
                    FileAccessManager.O_RDWR, 0, 0, getDefaultCoordinates())).getCreds();
            fail("file should have been read-only");
            
        } catch (IOException exc) {
            // ok
        }
        
    }

    @Test
    public void testEnableTracing() throws Exception {
        final String uid = "userXY";
        final List<String> gids = createGIDs("groupZ");
        final String volumeName = "testVolume";
        final String tracingPolicyConfig = "config";
        final String tracingPolicy = "defaultTracingPolicy";
        final UserCredentials uc = createUserCredentials(uid, gids);


        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
                volumeName, "", "", getKVList(), 0L));

        invokeSync(client.xtreemfs_mkvol(mrcAddress, RPCAuthentication.authNone, uc,
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, getDefaultStripingPolicy(), "", 0775,
                tracingPolicyConfig, "", "", getKVList(), 0L));

        invokeSync(client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
                FileAccessManager.O_CREAT, 0774, 0, getDefaultCoordinates()));

        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "/",
                "xtreemfs.tracing_enabled", "1", ByteString.copyFrom("1".getBytes()), 0));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "/",
                "xtreemfs.tracing_policy_config", tracingPolicy, ByteString.copyFrom(tracingPolicyConfig.getBytes()), 0));
        invokeSync(client.setxattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "/",
                "xtreemfs.tracing_policy", tracingPolicy, ByteString.copyFrom(tracingPolicy.getBytes()), 0));
        Thread.sleep(1000);

        // open a file in order to obtain a capability
        XCap xcap = invokeSync(
                client.open(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "test.txt",
                        FileAccessManager.O_RDONLY, 0, 0, getDefaultCoordinates())).getCreds().getXcap();

        assertTrue(xcap.getTraceConfig().getTraceRequests());
        assertTrue(xcap.getTraceConfig().getTracingPolicyConfig().equals(tracingPolicyConfig));
        assertTrue(xcap.getTraceConfig().getTracingPolicy().equals(tracingPolicy));
    }
    
    private void assertTree(InetSocketAddress server, String uid, List<String> gids, String volumeName,
        String... paths) throws Exception {
        
        final UserCredentials uc = createUserCredentials(uid, gids);
        
        // check whether all paths exist exactly once
        for (String path : paths) {
            
            try {
                Stat stat = invokeSync(
                    client.getattr(mrcAddress, RPCAuthentication.authNone, uc, volumeName, path, -1))
                        .getStbuf();
                
                // continue if the path does not point to a directory
                if ((stat.getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) == 0)
                    continue;
                
            } catch (PBRPCException exc) {
                throw new Exception("path '" + path + "' does not exist (" + exc.getPOSIXErrno() + ")");
            }
            
            // if the path points to a directory, check whether the number of
            // subdirectories is correct
            DirectoryEntries dir = invokeSync(client.readdir(mrcAddress, RPCAuthentication.authNone, uc,
                volumeName, path, -1, 1000, false, 0));
            int size = dir.getEntriesCount();
            
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
            
            assertEquals(count + (path.equals("") ? 1 : 2), size);
        }
    }
    
    private static List<String> createGIDs(String gid) {
        List<String> list = new LinkedList<String>();
        list.add(gid);
        return list;
    }
    
    private static Stat createChmodStat(int newMode) {
        return getDefaultStatBuilder().setMode(newMode).build();
    }
    
    private static OSDWriteResponse createFSResponse(int newFS, int newEpoch) {
        return OSDWriteResponse.newBuilder().setSizeInBytes(newFS).setTruncateEpoch(newEpoch).build();
    }
    
    private static Stat createChownStat(String newUid, String newGid) {
        return getDefaultStatBuilder().setUserId(newUid).setGroupId(newGid).build();
    }
    
    private static <T extends Message> T invokeSync(RPCResponse<T> response) throws PBRPCException,
        IOException, InterruptedException {
        
        try {
            return response.get();
        } finally {
            response.freeBuffers();
        }
    }
    
    private static UserCredentials createUserCredentials(String uid, List<String> gids) {
        return UserCredentials.newBuilder().setUsername(uid).addAllGroups(gids).build();
    }
    
    private static StripingPolicy getDefaultStripingPolicy() {
        return StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0).setStripeSize(
            1000).setWidth(1).build();
    }
    
    private static Stat.Builder getDefaultStatBuilder() {
        return Stat.newBuilder().setAtimeNs(0).setAttributes(0).setBlksize(0).setCtimeNs(0).setDev(0)
                .setEtag(0).setGroupId("").setIno(0).setMode(0).setMtimeNs(0).setNlink(0).setSize(0)
                .setTruncateEpoch(0).setUserId("");
    }
    
    private static VivaldiCoordinates getDefaultCoordinates() {
        return VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0).setLocalError(0).build();
    }
    
    private static List<KeyValuePair> getKVList(String... kvPairs) {
        
        List<KeyValuePair> kvList = new LinkedList<KeyValuePair>();
        for (int i = 0; i < kvPairs.length; i += 2)
            kvList.add(KeyValuePair.newBuilder().setKey(kvPairs[i]).setValue(kvPairs[i + 1]).build());
        
        return kvList;
    }

    private static List<String> extractOSDUUIDs(String xAttrLocations) {
        List<String> osdUUIDs = new LinkedList<String>();
        if (xAttrLocations == null) {
            return osdUUIDs;
        }
        String UUIDIdentifier = "\"uuid\":";
        String splits [] = xAttrLocations.split(",");
        for (String split : splits) {
            if (split.startsWith(UUIDIdentifier)) {
                osdUUIDs.add(split.substring(UUIDIdentifier.length() + 1,
                                         split.length() - 1));
            }
        }
        return osdUUIDs;
    }
}
