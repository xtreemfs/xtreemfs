/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.common.libxtreemfs;

import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.junit.After;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.utils.MRCHelper.SysAttrs;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import com.sun.xml.internal.ws.encoding.soap.SOAP12Constants;

/**
 * 
 * <br>
 * Sep 9, 2011
 */
public class VolumeTest extends TestCase {

    private DIRRequestDispatcher dir;

    private TestEnvironment      testEnv;

    private DIRConfig            dirConfig;

    private UserCredentials      userCredentials;

    private Auth                 auth        = RPCAuthentication.authNone;

    private DIRClient            dirClient;

    private String               mrcAddress;

    private String               dirAddress;

    private VivaldiCoordinates   defaultCoordinates;

    final private String         VOLUME_NAME = "foobar";

    private StripingPolicy       defaultStripingPolicy;

    private OSD[]                osds;
    private OSDConfig[]          configs;
    private OSDServiceClient     client;

    /**
     * 
     */
    public VolumeTest() throws Exception {

        dirConfig = SetupUtils.createDIRConfig();
        Logging.start(Logging.LEVEL_DEBUG);
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        System.out.println("TEST: " + getClass().getSimpleName());

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirClient = new DIRClient(new DIRServiceClient(testEnv.getRpcClient(), null),
                new InetSocketAddress[] { testEnv.getDIRAddress() }, 3, 1000);

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        defaultCoordinates = VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0)
                .setLocalError(0).build();
        defaultStripingPolicy = StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                .setStripeSize(128).setWidth(1).build();

        osds = new OSD[3];
        configs = SetupUtils.createMultipleOSDConfigs(3);

        // start one OSD
        osds[0] = new OSD(configs[0]);

    }

    @After
    public void tearDown() throws Exception {

        for (int i = 0; i < osds.length; i++) {
            if (osds[i] != null) {
                osds[i].shutdown();    
            }
            
        }

        testEnv.shutdown();

        dir.shutdown();

        dir.waitForShutdown();

    }

    // public void testStatVFS() throws Exception, VolumeNotFoundException {
    //
    // final String VOLUME_NAME_1 = "foobar";
    //
    // // TODO: Create pseudo commandline and parse it.
    // Options options = new Options(5000, 10000, 4, 2);
    //
    // String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
    //
    // Client client = Client.createClient(dirAddress, userCredentials, null, options);
    // client.start();
    //
    // client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
    //
    //
    // Volume volume = client.openVolume(VOLUME_NAME_1, null, options);
    //
    // StatVFS volumeVFS = volume.statFS(userCredentials);
    //
    // MRCServiceClient mrcClient = new MRCServiceClient(testEnv.getRpcClient(), null);
    //
    // StatVFS mrcClientVFS = null;
    // RPCResponse<StatVFS> resp = null;
    // try {
    // statvfsRequest input = statvfsRequest.newBuilder().setVolumeName(VOLUME_NAME_1).setKnownEtag(0)
    // .build();
    // resp = mrcClient.statvfs(testEnv.getMRCAddress(), auth, userCredentials, input);
    // mrcClientVFS = resp.get();
    // } catch (Exception e) {
    // e.printStackTrace();
    // } finally {
    // if (resp != null) {
    // resp.freeBuffers();
    // }
    // }
    // assertNotNull(volumeVFS);
    // assertEquals(mrcClientVFS, volumeVFS);
    //
    // client.shutdown();
    // }

    // public void testCreateDelete() throws Exception {
    //
    // final String DIR1 = "testdir1";
    // final String DIR2 = "testdir2";
    //
    // final String TESTFILE = "testfile";
    //
    // Options options = new Options(5000, 10000, 4, 2);
    // Client client = Client.createClient(dirAddress, userCredentials, null, options);
    // client.start();
    //
    // // create volume
    // client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME, 0, userCredentials.getUsername(),
    // userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
    // StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
    // defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());
    //
    // MRCServiceClient mrcClient = new MRCServiceClient(testEnv.getRpcClient(), null);
    //
    // Volume volume = client.openVolume(VOLUME_NAME, null, options);
    //
    // // create some files and directories
    // try {
    // volume.createDirectory(userCredentials, DIR1, 0755);
    // volume.createDirectory(userCredentials, DIR2, 0755);
    //
    // } catch (IOException ioe) {
    // fail("failed to create testdirs");
    // }
    //
    // FileHandle fileHandles[] = new FileHandle[10];
    //
    // for (int i = 0; i < fileHandles.length; i++) {
    // fileHandles[i] = volume.openFile(userCredentials, DIR1 + "/" + TESTFILE + i,
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
    // }
    //
    // // // try to create a file w/o a name
    // try {
    // volume.openFile(userCredentials, "", SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
    // fail("missing filename");
    // } catch (Exception e) {
    // }
    //
    // // try to create an already existing file
    // try {
    // volume.openFile(userCredentials, DIR1 + "/" + TESTFILE + "1",
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_EXCL.getNumber()
    // | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
    // fail("file already exists");
    // } catch (Exception e) {
    // }
    //
    // // file in file creation should fail
    // try {
    // volume.openFile(userCredentials, DIR1 + "/" + TESTFILE + "1/foo.txt",
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
    // fail("file in file creation");
    // } catch (Exception e) {
    // }
    //
    // // should fail
    // try {
    // volume.createDirectory(userCredentials, "", 0);
    // fail("directory already exists");
    // } catch (IOException exc) {
    //
    // }
    //
    // // test 'readDir' and 'stat'
    //
    // DirectoryEntries entrySet = volume.readDir(userCredentials, "", 0, 1000, false);
    // assertEquals(4, entrySet.getEntriesCount());
    // assertEquals("..", entrySet.getEntries(0).getName());
    // assertEquals(".", entrySet.getEntries(1).getName());
    // assertEquals(DIR1, entrySet.getEntries(2).getName());
    // assertEquals(DIR2, entrySet.getEntries(3).getName());
    //
    // entrySet = volume.readDir(userCredentials, DIR1, 0, 1000, false);
    // assertEquals(12, entrySet.getEntriesCount());
    //
    //
    // // test 'delete'
    // volume.unlink(userCredentials, DIR1 + "/" + TESTFILE + "4");
    // entrySet = volume.readDir(userCredentials, DIR1, 0, 1000, false);
    // assertEquals(11, entrySet.getEntriesCount());
    //
    // try {
    // volume.removeDirectory(userCredentials, DIR2);
    // } catch (Exception e) {
    // e.printStackTrace();
    // fail("Failed to remove directory");
    // }
    // entrySet = volume.readDir(userCredentials, "", 0, 1000, false);
    // assertEquals(3, entrySet.getEntriesCount());
    //
    // // tidy up
    // client.shutdown();
    // }

    // public void testHardLink() throws Exception {
    //
    // final String ORIG_FILE = "test.txt";
    // final String LINKED_FILE = "test-link.txt";
    // final String LINKED_FILE2 = "test-link2.txt";
    //
    // Options options = new Options(5000, 10000, 4, 2);
    // Client client = Client.createClient(dirAddress, userCredentials, null, options);
    // client.start();
    //
    // // create volume
    // client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME, 0, userCredentials.getUsername(),
    // userCredentials.getGroups(0), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL,
    // StripingPolicyType.STRIPING_POLICY_RAID0, defaultStripingPolicy.getStripeSize(),
    // defaultStripingPolicy.getWidth(), new ArrayList<KeyValuePair>());
    //
    // MRCServiceClient mrcClient = new MRCServiceClient(testEnv.getRpcClient(), null);
    //
    // // create file
    // openResponse open = null;
    // RPCResponse<openResponse> resp = null;
    // try {
    // resp = mrcClient.open(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, ORIG_FILE,
    // FileAccessManager.O_CREAT, 0, 0, defaultCoordinates);
    // open = resp.get();
    // } catch (Exception e) {
    // e.printStackTrace();
    // } finally {
    // if (resp != null) {
    // resp.freeBuffers();
    // }
    // }
    // assertNotNull(open);
    //
    // // create link
    // Volume volume = client.openVolume(VOLUME_NAME, null, options);
    // volume.link(userCredentials, ORIG_FILE, LINKED_FILE);
    //
    // open = null;
    // resp = null;
    // try {
    // resp = mrcClient.open(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
    // "test-hardlink.txt", FileAccessManager.O_CREAT, 0, 0, defaultCoordinates);
    // open = resp.get();
    // } catch (Exception e) {
    // e.printStackTrace();
    // } finally {
    // if (resp != null) {
    // resp.freeBuffers();
    // }
    // }
    // assertNotNull(open);
    //
    // // check whether both filenames refer to the same file
    // Stat stat1 = null;
    // Stat stat2 = null;
    // RPCResponse<getattrResponse> resp1 = null;
    // RPCResponse<getattrResponse> resp2 = null;
    // try {
    // resp1 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, ORIG_FILE,
    // 0);
    // stat1 = resp1.get().getStbuf();
    //
    // resp2 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
    // LINKED_FILE, 0);
    // stat2 = resp2.get().getStbuf();
    //
    // } catch (Exception e) {
    // e.printStackTrace();
    // } finally {
    // if (resp1 != null) {
    // resp1.freeBuffers();
    // }
    // if (resp2 != null) {
    // resp.freeBuffers();
    // }
    // }
    // assertNotNull(stat1);
    // assertNotNull(stat2);
    //
    // assertEquals(stat1.getIno(), stat1.getIno());
    // assertEquals(2, stat1.getNlink());
    //
    // // create another link to the second file
    // volume.link(userCredentials, LINKED_FILE, LINKED_FILE2);
    //
    // // check whether both links refer to the same file
    // stat1 = null;
    // stat2 = null;
    // resp1 = null;
    // resp2 = null;
    // try {
    // resp1 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
    // LINKED_FILE, 0);
    // stat1 = resp1.get().getStbuf();
    //
    // resp2 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
    // LINKED_FILE2, 0);
    // stat2 = resp2.get().getStbuf();
    // } catch (Exception e) {
    // e.printStackTrace();
    // } finally {
    // if (resp1 != null) {
    // resp1.freeBuffers();
    // }
    // if (resp2 != null) {
    // resp2.freeBuffers();
    // }
    // }
    // assertEquals(stat1.getIno(), stat2.getIno());
    // assertEquals(3, stat1.getNlink());
    //
    // // delete one of the links
    // try {
    // volume.unlink(userCredentials, LINKED_FILE);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // // check whether remaining links refer to the same file
    // stat1 = null;
    // stat2 = null;
    // resp1 = null;
    // resp2 = null;
    // try {
    // resp1 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, ORIG_FILE,
    // 0);
    // stat1 = resp1.get().getStbuf();
    //
    // resp2 = mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME,
    // LINKED_FILE2, 0);
    // stat2 = resp2.get().getStbuf();
    // } catch (Exception e) {
    // e.printStackTrace();
    // } finally {
    // if (resp1 != null) {
    // resp1.freeBuffers();
    // }
    // if (resp2 != null) {
    // resp2.freeBuffers();
    // }
    // }
    // assertEquals(stat1.getIno(), stat2.getIno());
    // assertEquals(2, stat1.getNlink());
    //
    // // delete the other two links
    // try {
    // volume.unlink(userCredentials, ORIG_FILE);
    // volume.unlink(userCredentials, LINKED_FILE2);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // try {
    // mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, LINKED_FILE2, 0)
    // .get();
    // fail("file should not exist anymore");
    // } catch (Exception exc) {
    // }
    //
    // try {
    // mrcClient.getattr(testEnv.getMRCAddress(), auth, userCredentials, VOLUME_NAME, ORIG_FILE, 0)
    // .get();
    // fail("file should not exist anymore");
    // } catch (Exception exc) {
    // }
    //
    // //
    // // // create two links to a directory
    // // invokeSync(client.mkdir(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "testDir1", 0));
    // // try {
    // // invokeSync(client.link(mrcAddress, RPCAuthentication.authNone, uc, volumeName, "testDir1",
    // // "testDir1/testDir2"));
    // // fail("links to directories should not be allowed");
    // // } catch (Exception exc) {
    // // }
    // // }
    // //
    //
    // // tidy up
    // client.shutdown();
    //
    // }

    // public void testGetSetAttr() throws Exception {
    //
    // final String TESTFILE = "testfile";
    // final String TESTDIR = "testdir";
    //
    // final int TESTMODE = 0731;
    //
    // Options options = new Options(5000, 10000, 4, 2);
    // Client client = Client.createClient(dirAddress, userCredentials, null, options);
    // client.start();
    //
    // client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
    // Volume volume = client.openVolume(VOLUME_NAME, null, options);
    //
    // volume.createDirectory(userCredentials, TESTDIR, TESTMODE);
    // volume.openFile(userCredentials, TESTDIR + "/" + TESTFILE,
    // SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber(), TESTMODE);
    //
    // Stat stat = volume.getAttr(userCredentials, TESTDIR);
    // assertEquals(userCredentials.getUsername(), stat.getUserId());
    // assertEquals(userCredentials.getGroups(0), stat.getGroupId());
    // assertEquals(0, stat.getSize());
    // assertTrue(stat.getAtimeNs() > 0);
    // assertTrue(stat.getCtimeNs() > 0);
    // assertTrue(stat.getMtimeNs() > 0);
    // assertTrue((stat.getMode() & TESTMODE) > 0);
    // assertEquals(1, stat.getNlink());
    //
    // stat = volume.getAttr(userCredentials, TESTDIR + "/" + TESTFILE);
    // assertEquals(userCredentials.getUsername(), stat.getUserId());
    // assertEquals(userCredentials.getGroups(0), stat.getGroupId());
    // assertEquals(0, stat.getSize());
    // assertTrue(stat.getAtimeNs() > 0);
    // assertTrue(stat.getCtimeNs() > 0);
    // assertTrue(stat.getMtimeNs() > 0);
    // assertTrue((stat.getMode() & TESTMODE) == TESTMODE);
    // assertEquals(1, stat.getNlink());
    //
    // stat = stat.toBuilder().setGroupId("foobar").setUserId("FredFoobar").build();
    // try {
    // volume.setAttr(userCredentials, TESTDIR, stat, Setattrs.SETATTR_UID.getNumber()
    // | Setattrs.SETATTR_GID.getNumber());
    // fail("changing username and groups should be restricted to superuser");
    // } catch (IOException e) {
    // }
    //
    // UserCredentials rootCreds = userCredentials.toBuilder().setUsername("root").setGroups(0, "root")
    // .build();
    //
    // volume.setAttr(rootCreds, TESTDIR, stat,
    // Setattrs.SETATTR_UID.getNumber() | Setattrs.SETATTR_GID.getNumber());
    //
    // stat = volume.getAttr(userCredentials, TESTDIR);
    // assertEquals("FredFoobar", stat.getUserId());
    // assertEquals("foobar", stat.getGroupId());
    // assertTrue((stat.getMode() & TESTMODE)> 0);
    //
    // stat = stat.toBuilder().setMode(0777).build();
    // volume.setAttr(userCredentials, TESTDIR+"/"+TESTFILE, stat, Setattrs.SETATTR_MODE.getNumber());
    // stat = volume.getAttr(userCredentials, TESTDIR+"/"+TESTFILE);
    // assertTrue((stat.getMode() & 0777) == 0777);
    //
    //
    // // tidy up
    // client.shutdown();
    //
    // }

    public void testReplicaAddListRemove() throws Exception {
        final String TESTFILE = "testfile";

        Options options = new Options(5000, 10000, 4, 2);
        Client client = Client.createClient(dirAddress, userCredentials, null, options);
        client.start();

        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        FileHandle fileHandle = volume.openFile(userCredentials, TESTFILE, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber(), 0777);
        fileHandle.close();
        
        // set replication update policy of the file
        volume.setXAttr(userCredentials, TESTFILE, "xtreemfs." + SysAttrs.set_repl_update_policy.toString(),
                ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, XATTR_FLAGS.XATTR_FLAGS_CREATE.getNumber());

        // start a second OSD. This ensures the file is not assigned to this OSD.
        osds[1] = new OSD(configs[1]);
        
        int flags = ReplicationFlags.setSequentialStrategy(0);
        flags = ReplicationFlags.setFullReplica(flags);

        Replica replica = Replica.newBuilder().addOsdUuids(configs[1].getUUID().toString())
                .setStripingPolicy(defaultStripingPolicy).setReplicationFlags(flags).build();
        volume.addReplica(userCredentials, TESTFILE, replica);

        // tidy up
        client.shutdown();

    }

//    public void testSetGetListXattr() throws Exception {
//
//        final String TESTFILE = "testfile";
//
//        Options options = new Options(5000, 10000, 4, 2);
//        Client client = Client.createClient(dirAddress, userCredentials, null, options);
//        client.start();
//
//        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
//        Volume volume = client.openVolume(VOLUME_NAME, null, options);
//
//        volume.openFile(userCredentials, TESTFILE, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());
//
//        int initialNumberOfXattr = volume.listXAttrs(userCredentials, TESTFILE).getXattrsCount();
//
//        // This xattr should not exist
//        String xattr = volume.getXAttr(userCredentials, TESTFILE, "foobarasdf");
//        assertNull(xattr);
//
//        // and therefore should have no size
//        assertEquals(-1, volume.getXAttrSize(userCredentials, TESTFILE, "foobarasdf"));
//
//        // creating a new Xattr should increase the number of xattrs
//        volume.setXAttr(userCredentials, TESTFILE, "foobarasdf", "nyancat",
//                XATTR_FLAGS.XATTR_FLAGS_CREATE.getNumber());
//
//        assertEquals(initialNumberOfXattr + 1, volume.listXAttrs(userCredentials, TESTFILE).getXattrsCount());
//
//        xattr = volume.getXAttr(userCredentials, TESTFILE, "foobarasdf");
//        assertEquals("nyancat", xattr);
//
//        // delete the created Xattr
//        volume.removeXAttr(userCredentials, TESTFILE, "foobarasdf");
//        assertEquals(initialNumberOfXattr, volume.listXAttrs(userCredentials, TESTFILE).getXattrsCount());
//        xattr = volume.getXAttr(userCredentials, TESTFILE, "foobarasdf");
//        assertNull(xattr);
//
//        // same with xtreemfs. attributes.
//        try {
//            xattr = volume.getXAttr(userCredentials, TESTFILE, "xtreemfs.nyancat");
//            fail("nyancat is not a valid system attribute");
//        } catch (Exception e) {
//        }
//
//        xattr = volume.getXAttr(userCredentials, TESTFILE,
//                "xtreemfs." + SysAttrs.set_repl_update_policy.toString());
//        assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, xattr);
//
//        // read existing systemflag
//        xattr = volume.getXAttr(userCredentials, TESTFILE, "xtreemfs." + SysAttrs.object_type.toString());
//        assertEquals("1", xattr);
//        
//
//        // tidy up
//        client.shutdown();
//    }
}
