/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.Volume.StripeLocation;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.utils.MRCHelper.SysAttrs;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Setattrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.openResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.statvfsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class VolumeTest {
    @Rule
    public final TestRule               testLog     = TestHelper.testLog;

    private static DIRRequestDispatcher dir;

    private static TestEnvironment      testEnv;

    private static DIRConfig            dirConfig;

    private static UserCredentials      userCredentials;

    private static Auth                 auth        = RPCAuthentication.authNone;

    private static String               mrcAddress;

    private static String               dirAddress;

    private static VivaldiCoordinates   defaultCoordinates;

    private static String               VOLUME_NAME = "foobar";

    private static StripingPolicy       defaultStripingPolicy;

    private static OSD[]                osds;
    private static OSDConfig[]          configs;

    private static MRCServiceClient     mrcClient;

    private static Client               client;

    private static Options              options;

    @BeforeClass
    public static void initializeTest() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        dirConfig = SetupUtils.createDIRConfig();
        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        defaultCoordinates = VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0)
                .setLocalError(0).build();
        defaultStripingPolicy = StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                .setStripeSize(128).setWidth(1).build();

        osds = new OSD[4];
        configs = SetupUtils.createMultipleOSDConfigs(4);

        // start three OSDs
        osds[0] = new OSD(configs[0]);
        osds[1] = new OSD(configs[1]);
        osds[2] = new OSD(configs[2]);
        osds[3] = new OSD(configs[3]);

        mrcClient = new MRCServiceClient(testEnv.getRpcClient(), null);

        options = new Options();
        client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();
    }

    @AfterClass
    public static void shutdownTest() throws Exception {
        for (int i = 0; i < osds.length; i++) {
            if (osds[i] != null) {
                osds[i].shutdown();
            }

        }

        testEnv.shutdown();
        dir.shutdown();
        dir.waitForShutdown();

        client.shutdown();
    }

    @Test
    public void testStatVFS() throws Exception, VolumeNotFoundException {
        final String VOLUME_NAME_1 = "foobar";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);

        Volume volume = client.openVolume(VOLUME_NAME_1, null, options);

        StatVFS volumeVFS = volume.statFS(userCredentials);

        MRCServiceClient mrcClient = new MRCServiceClient(testEnv.getRpcClient(), null);

        StatVFS mrcClientVFS = null;
        RPCResponse<StatVFS> resp = null;
        try {
            statvfsRequest input = statvfsRequest.newBuilder().setVolumeName(VOLUME_NAME_1).setKnownEtag(0)
                    .build();
            resp = mrcClient.statvfs(testEnv.getMRCAddress(), auth, userCredentials, input);
            mrcClientVFS = resp.get();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resp != null) {
                resp.freeBuffers();
            }
        }
        assertNotNull(volumeVFS);
        assertEquals(mrcClientVFS, volumeVFS);
    }

    @Test
    public void testReadDirMultipleChunks() throws Exception {
        options.setReaddirChunkSize(2);

        VOLUME_NAME = "testReadDirMultipleChunks";
        final String TESTFILE = "test";
        final int fileCount = 10;

        // create volume
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME, 0, userCredentials.getUsername(),
                userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
                defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());

        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // create some files
        for (int i = 0; i < fileCount; i++) {
            FileHandle fh = volume.openFile(userCredentials, "/" + TESTFILE + i,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
            fh.close();
        }

        // test 'readDir' across multiple readDir chunks.
        DirectoryEntries entrySet = null;

        entrySet = volume.readDir(userCredentials, "/", 0, 1000, false);
        assertEquals(2 + fileCount, entrySet.getEntriesCount());
        assertEquals("..", entrySet.getEntries(0).getName());
        assertEquals(".", entrySet.getEntries(1).getName());
        for (int i = 0; i < fileCount; i++) {
            assertEquals(TESTFILE + i, entrySet.getEntries(2 + i).getName());
        }
    }

    @Test
    public void testCreateDelete() throws Exception {
        VOLUME_NAME = "testCreateDelete";
        // Both directories should be created under "/"
        final String DIR1 = "/testdir1";
        final String DIR2 = "testdir2";

        final String TESTFILE = "testfile";
        // create volume
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME, 0, userCredentials.getUsername(),
                userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
                defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());

        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // create some files and directories
        try {
            volume.createDirectory(userCredentials, DIR1, 0755);
            volume.createDirectory(userCredentials, DIR2, 0755);
        } catch (IOException ioe) {
            fail("failed to create testdirs");
        }

        for (int i = 0; i < 10; i++) {
            FileHandle fh = volume.openFile(userCredentials, DIR1 + "/" + TESTFILE + i,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
            fh.close();
        }

        // // try to create a file w/o a name
        try {
            volume.openFile(userCredentials, "", SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
            fail("missing filename");
        } catch (Exception e) {
        }

        // try to create an already existing file
        try {
            volume.openFile(userCredentials, DIR1 + "/" + TESTFILE + "1",
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_EXCL.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
            fail("file already exists");
        } catch (Exception e) {
        }

        // file in file creation should fail
        try {
            volume.openFile(userCredentials, DIR1 + "/" + TESTFILE + "1/foo.txt",
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
            fail("file in file creation");
        } catch (Exception e) {
        }

        // should fail
        try {
            volume.createDirectory(userCredentials, "/", 0);
            fail("directory already exists");
        } catch (PosixErrorException exc) {
        }

        // test 'readDir' and 'stat'
        DirectoryEntries entrySet = null;

        entrySet = volume.readDir(userCredentials, "", 0, 1000, false);
        assertEquals(4, entrySet.getEntriesCount());
        assertEquals("..", entrySet.getEntries(0).getName());
        assertEquals(".", entrySet.getEntries(1).getName());
        assertEquals(DIR1, "/" + entrySet.getEntries(2).getName());
        assertEquals("/" + DIR2, "/" + entrySet.getEntries(3).getName());

        entrySet = volume.readDir(userCredentials, DIR1, 0, 1000, false);
        assertEquals(12, entrySet.getEntriesCount());

        // test 'delete'
        volume.unlink(userCredentials, DIR1 + "/" + TESTFILE + "4");
        entrySet = volume.readDir(userCredentials, DIR1, 0, 1000, false);
        assertEquals(11, entrySet.getEntriesCount());

        volume.removeDirectory(userCredentials, DIR2);
        entrySet = volume.readDir(userCredentials, "", 0, 1000, false);
        assertEquals(3, entrySet.getEntriesCount());
    }

    @Test
    public void testCreateDirWithEmptyPathComponents() throws Exception {
        VOLUME_NAME = "testCreateDirWithEmptyPathComponents";
        // Both directories should be created under "/"
        final String DIR1 = "/test";
        final String DIR2 = "/test//";
        final String DIR3 = "/test//testdir";

        // create volume
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME, 0, userCredentials.getUsername(),
                userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
                defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());

        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // create some files and directories
        try {
            volume.createDirectory(userCredentials, DIR1, 0755);
            volume.createDirectory(userCredentials, DIR3, 0755);
        } catch (IOException ioe) {
            fail("failed to create testdirs");
        }

        try {
            volume.createDirectory(userCredentials, DIR2, 0755);
            fail("existing directory could be created");
        } catch (IOException ioe) {}

        // test 'readDir' and 'stat'
        DirectoryEntries entrySet = null;

        entrySet = volume.readDir(userCredentials, DIR2, 0, 1000, false);
        assertEquals(3, entrySet.getEntriesCount());
        assertEquals("..", entrySet.getEntries(0).getName());
        assertEquals(".", entrySet.getEntries(1).getName());
        assertEquals("/testdir", "/" + entrySet.getEntries(2).getName());

        entrySet = volume.readDir(userCredentials, DIR3, 0, 1000, false);
        assertEquals(2, entrySet.getEntriesCount());

        volume.removeDirectory(userCredentials, DIR3);
        entrySet = volume.readDir(userCredentials, DIR1, 0, 1000, false);
        assertEquals(2, entrySet.getEntriesCount());
    }

    @Test
    public void testHardLink() throws Exception {
        VOLUME_NAME = "testHardLink";
        final String ORIG_FILE = "test.txt";
        final String LINKED_FILE = "test-link.txt";
        final String LINKED_FILE2 = "test-link2.txt";

        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME, 0, userCredentials.getUsername(),
                userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
                defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());

        // create file
        openResponse open = null;
        RPCResponse<openResponse> resp = null;
        try {
            resp = mrcClient.open(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, ORIG_FILE,
                    FileAccessManager.O_CREAT, 0, 0777, defaultCoordinates);
            open = resp.get();
        } finally {
            if (resp != null) {
                resp.freeBuffers();
            }
        }
        assertNotNull(open);

        // create link
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        volume.link(userCredentials, ORIG_FILE, LINKED_FILE);

        open = null;
        resp = null;
        try {
            resp = mrcClient.open(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
                    "test-hardlink.txt", FileAccessManager.O_CREAT, 0, 0, defaultCoordinates);
            open = resp.get();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resp != null) {
                resp.freeBuffers();
            }
        }
        assertNotNull(open);

        // check whether both filenames refer to the same file
        Stat stat1 = null;
        Stat stat2 = null;
        RPCResponse<getattrResponse> resp1 = null;
        RPCResponse<getattrResponse> resp2 = null;
        try {
            resp1 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, ORIG_FILE,
                    0);
            stat1 = resp1.get().getStbuf();

            resp2 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
                    LINKED_FILE, 0);
            stat2 = resp2.get().getStbuf();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resp1 != null) {
                resp1.freeBuffers();
            }
            if (resp2 != null) {
                resp.freeBuffers();
            }
        }
        assertNotNull(stat1);
        assertNotNull(stat2);

        assertEquals(stat1.getIno(), stat1.getIno());
        assertEquals(2, stat1.getNlink());

        // create another link to the second file
        volume.link(userCredentials, LINKED_FILE, LINKED_FILE2);

        // check whether both links refer to the same file
        stat1 = null;
        stat2 = null;
        resp1 = null;
        resp2 = null;
        try {
            resp1 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
                    LINKED_FILE, 0);
            stat1 = resp1.get().getStbuf();

            resp2 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
                    LINKED_FILE2, 0);
            stat2 = resp2.get().getStbuf();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resp1 != null) {
                resp1.freeBuffers();
            }
            if (resp2 != null) {
                resp2.freeBuffers();
            }
        }
        assertEquals(stat1.getIno(), stat2.getIno());
        assertEquals(3, stat1.getNlink());

        // delete one of the links
        volume.unlink(userCredentials, LINKED_FILE);

        // check whether remaining links refer to the same file
        stat1 = null;
        stat2 = null;
        resp1 = null;
        resp2 = null;
        try {
            resp1 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, ORIG_FILE,
                    0);
            stat1 = resp1.get().getStbuf();

            resp2 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
                    LINKED_FILE2, 0);
            stat2 = resp2.get().getStbuf();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resp1 != null) {
                resp1.freeBuffers();
            }
            if (resp2 != null) {
                resp2.freeBuffers();
            }
        }
        assertEquals(stat1.getIno(), stat2.getIno());
        assertEquals(2, stat1.getNlink());

        // delete the other two links
        volume.unlink(userCredentials, ORIG_FILE);
        volume.unlink(userCredentials, LINKED_FILE2);

        try {
            mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, LINKED_FILE2, 0)
                    .get();
            fail("file should not exist anymore");
        } catch (Exception exc) {
        }

        try {
            mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, ORIG_FILE, 0)
                    .get();
            fail("file should not exist anymore");
        } catch (Exception exc) {
        }

        //
        // // create two links to a directory
        // invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc,
        // volumeName, "testDir1", 0));
        // try {
        // invokeSync(client.link(mrcAddress, RPCAuthentication.authNone, uc,
        // volumeName, "testDir1",
        // "testDir1/testDir2"));
        // fail("links to directories should not be allowed");
        // } catch (Exception exc) {
        // }
        // }
        //
    }

    @Test
    public void testGetSetAttr() throws Exception {
        VOLUME_NAME = "testGetSetAttr";

        final String TESTFILE = "testfile";
        final String TESTDIR = "testdir";

        final int TESTMODE = 0731;

        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        volume.createDirectory(userCredentials, TESTDIR, TESTMODE);
        FileHandle fh = volume.openFile(userCredentials, TESTDIR + "/" + TESTFILE,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber(), TESTMODE);
        fh.close();

        Stat stat = volume.getAttr(userCredentials, TESTDIR);
        assertEquals(userCredentials.getUsername(), stat.getUserId());
        assertEquals(userCredentials.getGroups(0), stat.getGroupId());
        assertEquals(0, stat.getSize());
        assertTrue(stat.getAtimeNs() > 0);
        assertTrue(stat.getCtimeNs() > 0);
        assertTrue(stat.getMtimeNs() > 0);
        assertTrue((stat.getMode() & TESTMODE) > 0);
        assertEquals(1, stat.getNlink());

        stat = volume.getAttr(userCredentials, TESTDIR + "/" + TESTFILE);
        assertEquals(userCredentials.getUsername(), stat.getUserId());
        assertEquals(userCredentials.getGroups(0), stat.getGroupId());
        assertEquals(0, stat.getSize());
        assertTrue(stat.getAtimeNs() > 0);
        assertTrue(stat.getCtimeNs() > 0);
        assertTrue(stat.getMtimeNs() > 0);
        assertTrue((stat.getMode() & TESTMODE) == TESTMODE);
        assertEquals(1, stat.getNlink());

        stat = stat.toBuilder().setGroupId("foobar").setUserId("FredFoobar").build();
        try {
            volume.setAttr(userCredentials, TESTDIR, stat, Setattrs.SETATTR_UID.getNumber()
                    | Setattrs.SETATTR_GID.getNumber());
            fail("changing username and groups should be restricted to superuser");
        } catch (PosixErrorException e) {
        }

        UserCredentials rootCreds = userCredentials.toBuilder().setUsername("root").setGroups(0, "root")
                .build();

        volume.setAttr(rootCreds, TESTDIR, stat,
                Setattrs.SETATTR_UID.getNumber() | Setattrs.SETATTR_GID.getNumber());

        stat = volume.getAttr(userCredentials, TESTDIR);
        assertEquals("FredFoobar", stat.getUserId());
        assertEquals("foobar", stat.getGroupId());
        assertTrue((stat.getMode() & TESTMODE) > 0);

        stat = stat.toBuilder().setMode(0777).build();
        volume.setAttr(userCredentials, TESTDIR + "/" + TESTFILE, stat, Setattrs.SETATTR_MODE.getNumber());
        stat = volume.getAttr(userCredentials, TESTDIR + "/" + TESTFILE);
        assertTrue((stat.getMode() & 0777) == 0777);
    }

    @Test
    public void testReplicaAddListRemove() throws Exception {
        VOLUME_NAME = "testReplicaAddListRemove";
        final String fileName = "testfile";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set replication update policy of the file
        int flags = ReplicationFlags.setSequentialStrategy(0);
        flags = ReplicationFlags.setFullReplica(flags);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE,
                2, flags);
        FileHandle fileHandle = volume.openFile(userCredentials, fileName,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber(), 0777);
        fileHandle.close();

        assertEquals(2, volume.listReplicas(userCredentials, fileName).getReplicasCount());

        String osdUUID = volume.getSuitableOSDs(userCredentials, fileName, 1).get(0);
        Replica replica = Replica.newBuilder().addOsdUuids(osdUUID).setStripingPolicy(defaultStripingPolicy)
                .setReplicationFlags(flags).build();
        volume.addReplica(userCredentials, fileName, replica);
        assertEquals(3, volume.listReplicas(userCredentials, fileName).getReplicasCount());

        volume.removeReplica(userCredentials, fileName, replica.getOsdUuids(0));
        assertEquals(2, volume.listReplicas(userCredentials, fileName).getReplicasCount());
    }

    @Test
    public void testSetGetListXattr() throws Exception {
        VOLUME_NAME = "testSetGetListXattr";
        final String TESTFILE = "testfile";

        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        FileHandle fh = volume.openFile(userCredentials, TESTFILE, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
        fh.close();

        int initialNumberOfXattr = volume.listXAttrs(userCredentials, TESTFILE).getXattrsCount();

        // This xattr should not exist
        String xattr = volume.getXAttr(userCredentials, TESTFILE, "foobarasdf");
        assertNull(xattr);

        // and therefore should have no size
        assertEquals(-1, volume.getXAttrSize(userCredentials, TESTFILE, "foobarasdf"));

        // creating a new Xattr should increase the number of xattrs
        volume.setXAttr(userCredentials, TESTFILE, "foobarasdf", "nyancat",
                XATTR_FLAGS.XATTR_FLAGS_CREATE);

        assertEquals(initialNumberOfXattr + 1, volume.listXAttrs(userCredentials, TESTFILE).getXattrsCount());

        xattr = volume.getXAttr(userCredentials, TESTFILE, "foobarasdf");
        assertEquals("nyancat", xattr);

        // delete the created Xattr
        volume.removeXAttr(userCredentials, TESTFILE, "foobarasdf");
        assertEquals(initialNumberOfXattr, volume.listXAttrs(userCredentials, TESTFILE).getXattrsCount());
        xattr = volume.getXAttr(userCredentials, TESTFILE, "foobarasdf");
        assertNull(xattr);

        // same with xtreemfs. attributes.
        try {
            xattr = volume.getXAttr(userCredentials, TESTFILE, "xtreemfs.nyancat");
            fail("nyancat is not a valid system attribute");
        } catch (Exception e) {
        }

        xattr = volume.getXAttr(userCredentials, TESTFILE,
                "xtreemfs." + SysAttrs.set_repl_update_policy.toString());
        assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, xattr);

        // read existing systemflag
        xattr = volume.getXAttr(userCredentials, TESTFILE, "xtreemfs." + SysAttrs.object_type.toString());
        assertEquals("1", xattr);
    }

    @Test(expected = PosixErrorException.class)
    public void testReadLinkWithoutLink() throws Exception {
        VOLUME_NAME = "testReadLinkWithoutLink";
        String fileName = "testfile";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fh = volume.openFile(userCredentials, fileName, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
        fh.close();
        volume.readLink(userCredentials, fileName);
    }

    @Test
    public void testReadLinkWithLink() throws Exception {
        VOLUME_NAME = "testReadLinkWithLink";
        String fileName = "testfile";
        String linkName = "linkToFile";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fh = volume.openFile(userCredentials, fileName, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);
        fh.close();
        volume.symlink(userCredentials, fileName, linkName);
        assertEquals(fileName, volume.readLink(userCredentials, linkName));
    }

    @Test(expected = PosixErrorException.class)
    public void testAccessFail() throws Exception {
        VOLUME_NAME = "testAccessFail";
        String fileName = "testfile";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fh = volume.openFile(userCredentials, fileName, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0000);
        fh.close();
        volume.access(userCredentials, fileName, 0777);
    }

    @Test
    public void testAccessSuccess() throws Exception {
        VOLUME_NAME = "testAccessSuccess";
        String fileName = "testfile";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fh = volume.openFile(userCredentials, fileName, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0753);
        fh.close();
        volume.access(userCredentials, fileName, 0753);
    }

    @Test
    public void testTruncate() throws Exception {
        VOLUME_NAME = "testTruncate";
        String fileName = "testfile";
        String emptyFileName = "emptyFileName";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fileHandle = volume.openFile(userCredentials,
                fileName, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);

        String data = "1234567890";
        fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);
        fileHandle.flush();

        assertEquals(data.length(), volume.getAttr(userCredentials, fileName).getSize());

        volume.truncate(userCredentials, fileName, 5);
        assertEquals(5, volume.getAttr(userCredentials, fileName).getSize());
        fileHandle.close();

        fileHandle = volume.openFile(
                userCredentials,
                emptyFileName,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);
        assertEquals(0, fileHandle.getAttr(userCredentials).getSize());
        volume.truncate(userCredentials, emptyFileName, 1000);
        assertEquals(1000, fileHandle.getAttr(userCredentials).getSize());
        fileHandle.close();
    }

    @Test
    public void testRenameFile() throws Exception {
        VOLUME_NAME = "testRenameFile";
        String fileName = "testfile";
        String renamedFileName = "renamed";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fh = volume.openFile(userCredentials, fileName, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0777);
        fh.close();
        // nothing should happen
        volume.rename(userCredentials, fileName, fileName);
        DirectoryEntries dir = volume.readDir(userCredentials, "/", 0, 100, true);
        assertEquals(fileName, dir.getEntries(2).getName());
        assertEquals(3, dir.getEntriesCount());

        volume.rename(userCredentials, fileName, renamedFileName);
        dir = volume.readDir(userCredentials, "/", 0, 100, true);
        assertEquals(renamedFileName, dir.getEntries(2).getName());
        assertEquals(3, dir.getEntriesCount());
    }

    @Test
    public void testGetXattrSize() throws Exception {
        VOLUME_NAME = "testGetXattrSize";
        String fileName = "testfile";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        FileHandle fh = volume.openFile(
                userCredentials,
                fileName,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_SYNC.getNumber(), 0777);
        fh.close();
        int size = volume.getXAttrSize(userCredentials, fileName, "xtreemfs.set_repl_update_policy");
        assertEquals(0, size);
        size = volume.getXAttrSize(userCredentials, fileName, "xtreemfs.owner");
        assertEquals(userCredentials.getUsername().length(), size);
        size = volume.getXAttrSize(userCredentials, fileName, "doesnt-exist");
        assertEquals(-1, size);
        volume.setXAttr(userCredentials, fileName, "foo", "bar", XATTR_FLAGS.XATTR_FLAGS_CREATE);
        size = volume.getXAttrSize(userCredentials, fileName, "foo");
        assertEquals(3, size);
        size = volume.getXAttrSize(userCredentials, fileName, "doesnt-exist-in-cache");
        assertEquals(-1, size);
    }

    // @Test
    // public void testGetSuitableOSDs() throws Exception {
    // VOLUME_NAME = "testGetSuitableOSDs";
    // final String fileName = "testfile";
    // client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
    // Volume volume = client.openVolume(VOLUME_NAME, null, options);
    //
    // // set replication update policy of the file
    // int flags = ReplicationFlags.setSequentialStrategy(0);
    // flags = ReplicationFlags.setFullReplica(flags);
    // volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE,
    // 1, flags);
    // FileHandle fileHandle =
    // volume.openFile(userCredentials, fileName,
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber(), 0777);
    // fileHandle.close();
    //
    // assertEquals(configs[1].getUUID().toString(), volume.getSuitableOSDs(userCredentials, fileName, 1));
    // }

    @Test
    public void testCreateListDirectory() throws Exception {
        VOLUME_NAME = "testCreateListDirectory";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        volume.createDirectory(userCredentials, "/DIR1", 0);
        volume.createDirectory(userCredentials, "DIR2", 0);
        DirectoryEntries dirEntries = volume.readDir(userCredentials, "/", 0, Integer.MAX_VALUE, true);
        assertEquals(4, dirEntries.getEntriesCount());
        dirEntries = volume.readDir(userCredentials, "/", 0, 0, true);
        assertEquals(4, dirEntries.getEntriesCount());
        assertEquals("..", dirEntries.getEntries(0).getName());
        assertEquals(".", dirEntries.getEntries(1).getName());
        assertEquals("DIR1", dirEntries.getEntries(2).getName());
        assertEquals("DIR2", dirEntries.getEntries(3).getName());
    }

    @Test
    public void testCreateDirectoryRecursive() throws Exception {
        VOLUME_NAME = "testCreateDirectoryRecursive";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        final String directory1 = "/home/foo/bar/user/xtreemfs/test/bar/foo";
        final String directroy2 = "/home/foo/path/with/slash/at/end/";
        volume.createDirectory(userCredentials, directory1, 0777, true);
        volume.createDirectory(userCredentials, directroy2, 0777, true);

        final String[] dirs1 = directory1.split("/");
        final String[] dirs2 = directroy2.split("/");

        String tempdir = "";
        for (int i = 1; i < dirs1.length; i++) {
            tempdir = tempdir + "/" + dirs1[i];
            assertTrue(isDirectory(volume, tempdir));
        }

        tempdir = "";
        for (int i = 1; i < dirs2.length; i++) {
            tempdir = tempdir + "/" + dirs2[i];
            assertTrue(isDirectory(volume, tempdir));
        }
        volume.close();
    }

    private boolean isDirectory(Volume volume, String path) throws IOException {
        try {
            Stat stat = volume.getAttr(userCredentials, path);
            return (stat.getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) > 0;
        } catch (PosixErrorException pee) {
            if (pee.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOENT)) {
                return false;
            } else {
                throw pee;
            }
        }
    }

    @Test
    public void testGetStripeLocations() throws Exception {
        VOLUME_NAME = "testGetStripeLocatations";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set replication update policy of the file
        int flags = ReplicationFlags.setSequentialStrategy(0);
        flags = ReplicationFlags.setFullReplica(flags);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY,
                2, flags);

        final String FILENAME = "/foobar.tzt";
        FileHandle fileHandle = volume.openFile(userCredentials, FILENAME,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber(), 0777);
        byte[] data = new byte[6];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) "FOOBAR".charAt(i);
        }
        fileHandle.write(userCredentials, data, data.length, 0);
        fileHandle.close();
        List<StripeLocation> stripeLocations = volume.getStripeLocations(userCredentials, FILENAME, 0, 100);
        assertEquals(1, stripeLocations.size());
        assertEquals(2, stripeLocations.get(0).getUuids().length);
        assertEquals(2, stripeLocations.get(0).getHostnames().length);
        assertEquals(0, stripeLocations.get(0).getStartSize());
        assertEquals(100, stripeLocations.get(0).getLength());

        stripeLocations = volume.getStripeLocations(userCredentials, FILENAME, 200, 123);
        assertEquals(1, stripeLocations.size());
        assertEquals(2, stripeLocations.get(0).getUuids().length);
        assertEquals(2, stripeLocations.get(0).getHostnames().length);
        assertEquals(200, stripeLocations.get(0).getStartSize());
        assertEquals(123, stripeLocations.get(0).getLength());

        List<String> suitableOsds = volume.getSuitableOSDs(userCredentials, FILENAME, 1);
        Replica replica = Replica.newBuilder().setStripingPolicy(defaultStripingPolicy)
                .setReplicationFlags(flags).addOsdUuids(suitableOsds.get(0)).build();
        volume.addReplica(userCredentials, FILENAME, replica);

        stripeLocations = volume.getStripeLocations(userCredentials, FILENAME, 0, 100);
        assertEquals(1, stripeLocations.size());
        assertEquals(3, stripeLocations.get(0).getUuids().length);
        assertEquals(3, stripeLocations.get(0).getHostnames().length);
        assertEquals(0, stripeLocations.get(0).getStartSize());
        assertEquals(100, stripeLocations.get(0).getLength());

        stripeLocations = volume.getStripeLocations(userCredentials, FILENAME, 200, 123);
        assertEquals(1, stripeLocations.size());
        assertEquals(3, stripeLocations.get(0).getUuids().length);
        assertEquals(3, stripeLocations.get(0).getHostnames().length);
        assertEquals(200, stripeLocations.get(0).getStartSize());
        assertEquals(123, stripeLocations.get(0).getLength());

        volume.close();
    }

    @Test
    public void testGetStripeLocationsWithMultipleStripes() throws Exception {
        VOLUME_NAME = "testGetStripeLocatationsWithMultipleStripes";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME, 0777,
                userCredentials.getUsername(), userCredentials.getGroups(0),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                StripingPolicyType.STRIPING_POLICY_RAID0, 2, 1, new ArrayList<GlobalTypes.KeyValuePair>());
        Volume volume = client.openVolume(VOLUME_NAME, options.generateSSLOptions(), options);

        // set replication update policy of the file
        int replicationFlags = ReplicationFlags.setSequentialStrategy(0);
        replicationFlags = ReplicationFlags.setFullReplica(replicationFlags);
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY,
                2, replicationFlags);

        // create new striping policy with width 2
        StripingPolicy stripingPolicy = StripingPolicy.newBuilder().setStripeSize(2).setWidth(2)
                .setType(StripingPolicyType.STRIPING_POLICY_RAID0).build();

        // create file
        final String FILENAME = "/foobar.tzt";
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber();
        FileHandle fileHandle = volume.openFile(userCredentials, FILENAME, flags, 0777);

        // write more than 2kb to the byte to ensure the second stripe is used
        byte[] data = new byte[3000];
        for (int i = 0; i < data.length; i++) {
            data[i] = 'F';
        }
        fileHandle.write(userCredentials, data, data.length, 0);
        fileHandle.close();

        // create replica and add it
        List<String> suitableOsds = volume.getSuitableOSDs(userCredentials, FILENAME, 2);
        Replica replica = Replica.newBuilder().setReplicationFlags(replicationFlags)
                .setStripingPolicy(stripingPolicy).addAllOsdUuids(suitableOsds).build();
        volume.addReplica(userCredentials, FILENAME, replica);

        List<StripeLocation> stripeLocations = volume.getStripeLocations(userCredentials, FILENAME, 0, 4000);
        assertEquals(2, stripeLocations.size());
        assertEquals(3, stripeLocations.get(0).getUuids().length);
        assertEquals(3, stripeLocations.get(0).getHostnames().length);

        assertEquals(0, stripeLocations.get(0).getStartSize());
        assertEquals(2048, stripeLocations.get(0).getLength());
        assertEquals(2048, stripeLocations.get(1).getStartSize());
        assertEquals(4000 - 2048, stripeLocations.get(1).getLength());

        assertEquals(stripeLocations.get(0).getUuids()[0], stripeLocations.get(1).getUuids()[0]);
        assertEquals(stripeLocations.get(0).getUuids()[1], stripeLocations.get(1).getUuids()[1]);
        assertNotSame(stripeLocations.get(0).getUuids()[2], stripeLocations.get(1).getUuids()[2]);
    }

    @Test
    public void testVolumeQuota() throws Exception {
        final String VOLUME_NAME = "testVolumeQuota";

        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME, 0, userCredentials.getUsername(),
                userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
                StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
                defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());

        Volume volume = client.openVolume(VOLUME_NAME, null, options);
        volume.setXAttr(userCredentials, "/", "xtreemfs.quota", "8", XATTR_FLAGS.XATTR_FLAGS_CREATE);

        assertEquals("8", volume.getXAttr(userCredentials, "/", "xtreemfs.quota"));

        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();

        byte[] content = "foo foo foo".getBytes();

        FileHandle file = volume.openFile(userCredentials, "/test1.txt", flags, 0777);
        file.write(userCredentials, content, content.length, 0);
        file.close();

        try {
            file = volume.openFile(userCredentials, "/test2.txt", flags, 0777);
            assertTrue(false);
        } catch (PosixErrorException exc) {
            // check if right exception was thrown
            if (!exc.getMessage().contains("POSIX_ERROR_ENOSPC")) {
                assertTrue(false);
            }
        }
    }

    /**
     * Test if files inherit the group from its parent if the setgid bit is set, and if subdirectories inherit the group
     * and the setgid bit.
     */
    @Test
    public void testSetgid() throws Exception {
        VOLUME_NAME = "testSetgid";
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        volume.createDirectory(userCredentials, "/DIR1", 0777);

        Stat stat;
        stat = volume.getAttr(userCredentials, "/DIR1");

        int mode = stat.getMode() | 02000;
        stat = stat.toBuilder().setGroupId("foobar").setMode(mode).build();

        UserCredentials rootCreds = userCredentials.toBuilder().setUsername("root").setGroups(0, "root").build();
        volume.setAttr(rootCreds, "/DIR1", stat, Setattrs.SETATTR_MODE.getNumber() | Setattrs.SETATTR_GID.getNumber());

        // Test if the setgid bit and the group is set
        stat = volume.getAttr(userCredentials, "/DIR1");
        assertEquals(02000, stat.getMode() & 02000);
        assertEquals("foobar", stat.getGroupId());

        // Test if new subdirectories inherit the setgid bit and the group
        volume.createDirectory(userCredentials, "/DIR1/DIR2", 0777);
        stat = volume.getAttr(userCredentials, "/DIR1/DIR2");
        assertEquals(02000, stat.getMode() & 02000);
        assertEquals("foobar", stat.getGroupId());

        // Test if new files inherit the group
        FileHandle fh = volume.openFile(userCredentials, "/DIR1/FILE1",
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
        fh.close();
        stat = volume.getAttr(userCredentials, "/DIR1/FILE1");
        assertEquals("foobar", stat.getGroupId());

        volume.close();
    }
}
