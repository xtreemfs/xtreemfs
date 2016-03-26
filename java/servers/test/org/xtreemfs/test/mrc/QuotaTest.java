/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.Volume.StripeLocation;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class QuotaTest {

    public final static String     USERNAME   = "username";
    public final static String     GROUPNAME  = "groupname";
    public final static String     VOLUMENAME = "testVolumeName";
    public final static String     FILEPATH   = "/foo.bar";

    @Rule
    public final TestRule          testLog    = TestHelper.testLog;

    private static TestEnvironment testEnv;

    private static UserCredentials userCredentials;

    private static Auth            auth       = RPCAuthentication.authNone;

    private static String          dirAddress;
    private static String          mrcAddress;

    @BeforeClass
    public static void initializeTest() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        // keep in mind: SetupUtils.Debug_Level and Logging.Level_Debug are not the same
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.MRC, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD,
                TestEnvironment.Services.OSD });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername(USERNAME).addGroups(GROUPNAME).build();

        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
    }

    @AfterClass
    public static void shutdownTest() throws Exception {
        testEnv.shutdown();
    }

    @Test
    public void testGetSetDefaultQuota() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testGetSetDefaultQuota";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // check default values: unlimited quota
        {
            assertEquals(Long.toString(QuotaConstants.UNLIMITED_QUOTA),
                    volume.getXAttr(userCredentials, "/", "xtreemfs.quota"));
            assertEquals(Long.toString(QuotaConstants.UNLIMITED_QUOTA),
                    volume.getXAttr(userCredentials, "/", "xtreemfs.defaultuserquota"));
            assertEquals(Long.toString(QuotaConstants.UNLIMITED_QUOTA),
                    volume.getXAttr(userCredentials, "/", "xtreemfs.defaultgroupquota"));
        }

        // check user/group quota need user/group name
        {
            boolean error;

            // String[] xattrNamesNoValue = new String[] { "xtreemfs.userquota", "xtreemfs.groupquota" };
            String[] xattrNamesEmptyValue = new String[] { "xtreemfs.userquota.", "xtreemfs.groupquota." };

            String[][] xattrNames = new String[][] { xattrNamesEmptyValue };

            for (String[] xattrNameArray : xattrNames) {
                for (String xattrName : xattrNameArray) {
                    error = false;
                    try {
                        volume.getXAttr(userCredentials, "/", xattrName);
                    } catch (PosixErrorException e) {
                        error = true;
                    } finally {
                        assertTrue(error);
                    }
                }
            }
        }

        // check user/group quota
        {
            assertEquals(Long.toString(QuotaConstants.NO_QUOTA),
                    volume.getXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME));
            assertEquals(Long.toString(QuotaConstants.NO_QUOTA),
                    volume.getXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME));
        }

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testGetSetQuota() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testGetSetQuota";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // check quota
        {

            String volumeQuota = "100";
            String userQuota = "101";
            String groupQuota = "102";

            volume.setXAttr(userCredentials, "/", "xtreemfs.quota", volumeQuota, XATTR_FLAGS.XATTR_FLAGS_CREATE);
            volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, userQuota,
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);
            volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, groupQuota,
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);

            assertEquals(volumeQuota, volume.getXAttr(userCredentials, "/", "xtreemfs.quota"));
            assertEquals(userQuota, volume.getXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME));
            assertEquals(groupQuota, volume.getXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME));
        }

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testGetSetSpaceUsage() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testGetSetSpaceUsage";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // check user/group space usage values on startup
        {
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));
        }

        // check get for user/group space usage: no / empty user/group
        {
            boolean error;

            // String[] xattrNamesNoValue = new String[] { "xtreemfs.userusedspace", "xtreemfs.userblockedspace",
            // "xtreemfs.groupusedspace", "xtreemfs.groupblockedspace" };
            String[] xattrNamesEmptyValue = new String[] { "xtreemfs.userusedspace.", "xtreemfs.userblockedspace.",
                    "xtreemfs.groupusedspace.", "xtreemfs.groupblockedspace." };

            String[][] xattrNames = new String[][] { xattrNamesEmptyValue };

            for (String[] xattrNameArray : xattrNames) {
                for (String xattrName : xattrNameArray) {
                    error = false;
                    try {
                        volume.getXAttr(userCredentials, "/", xattrName);
                    } catch (PosixErrorException e) {
                        error = true;
                    } finally {
                        assertTrue(error);
                    }
                }
            }
        }

        // check set for user/group space usage: no/empty/given user/group
        {
            boolean error;
            String value = "12345";

            // String[] xattrNamesNoValue = new String[] { "xtreemfs.userusedspace", "xtreemfs.userblockedspace",
            // "xtreemfs.groupusedspace", "xtreemfs.groupblockedspace" };
            String[] xattrNamesEmptyValue = new String[] { "xtreemfs.userusedspace.", "xtreemfs.userblockedspace.",
                    "xtreemfs.groupusedspace.", "xtreemfs.groupblockedspace." };
            String[] xattrNamesValue = new String[] { "xtreemfs.userusedspace." + USERNAME,
                    "xtreemfs.userblockedspace." + USERNAME, "xtreemfs.groupusedspace." + GROUPNAME,
                    "xtreemfs.groupblockedspace." + GROUPNAME };
            String[] xattrNamesVolume = new String[] { "xtreemfs.blockedspace", "xtreemfs.usedspace" };

            String[][] xattrNames = new String[][] { xattrNamesEmptyValue, xattrNamesValue, xattrNamesVolume };

            for (String[] xattrNameArray : xattrNames) {
                for (String xattrName : xattrNameArray) {
                    error = false;
                    try {
                        volume.setXAttr(userCredentials, "/", xattrName, value, XATTR_FLAGS.XATTR_FLAGS_CREATE);
                    } catch (PosixErrorException e) {
                        error = true;
                    } finally {
                        assertTrue(error);
                    }
                }
            }
        }

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testUnlimitedWrite() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testUnlimitedWrite";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // Open a file.
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                FILEPATH,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        // check blocked space: no quota -> no blocked space
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        // Get file attributes
        Stat stat = volume.getAttr(userCredentials, FILEPATH);
        assertEquals(0, stat.getSize());

        // Write to file.
        String data = "Need a testfile? Why not (\\|)(+,,,+)(|/)?";
        fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);

        stat = volume.getAttr(userCredentials, FILEPATH);
        assertEquals(data.length(), stat.getSize());

        // Read from file.
        byte[] readData = new byte[data.length()];
        int readCount = fileHandle.read(userCredentials, readData, data.length(), 0);

        assertEquals(data.length(), readCount);
        for (int i = 0; i < data.length(); i++) {
            assertEquals(readData[i], data.getBytes()[i]);
        }

        fileHandle.close();

        // check user/group space usage values on startup
        assertEquals(Long.toString(data.length()), volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals(Long.toString(data.length()),
                volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals(Long.toString(data.length()),
                volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testQuota() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testQuota";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set vouchersize > quota, so that the voucher will block all space
        String voucherSize = "10";
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", voucherSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        boolean error, firstPass;
        String quota = "5";

        // test volume quota
        {
            volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);

            // Open a file.
            FileHandle fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

            // check blocked space for volume
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));

            // check blocked space for user/group: no quota -> no blocked space
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            error = false;
            firstPass = false;
            try {

                // Write to file.
                fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota), 0);
                firstPass = true;

                // extend quota write
                fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota),
                        Integer.parseInt(quota));
            } catch (PosixErrorException e) {
                if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                    if (e.getMessage().contains("volume")) {
                        error = firstPass;
                    }
                }
            } finally {
                assertTrue(error);
            }

            fileHandle.close();

            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

            volume.unlink(userCredentials, FILEPATH);

            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

            // reset to unlimited
            volume.setXAttr(userCredentials, "/", "xtreemfs.quota", "0", XATTR_FLAGS.XATTR_FLAGS_CREATE);
        }

        // test user quota
        {
            quota = "6";
            volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quota,
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);

            // Open a file.
            FileHandle fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

            // check blocked space
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            error = false;
            firstPass = false;
            try {
                // Write to file.
                fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota), 0);
                firstPass = true;

                // extend quota write
                fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota),
                        Integer.parseInt(quota));
            } catch (PosixErrorException e) {
                if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                    if (e.getMessage().contains("owner")) {
                        error = firstPass;
                    }
                }
            } finally {
                assertTrue(error);
            }

            fileHandle.close();

            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            volume.unlink(userCredentials, FILEPATH);

            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

            // reset to unlimted
            volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, "0", XATTR_FLAGS.XATTR_FLAGS_CREATE);
        }

        // test group quota
        {
            quota = "7";
            volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quota,
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);

            // Open a file.
            FileHandle fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

            // check blocked space
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            error = false;
            firstPass = false;
            try {
                // Write to file.
                fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota), 0);
                firstPass = true;

                // extend quota write
                fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota),
                        Integer.parseInt(quota));
            } catch (PosixErrorException e) {
                if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                    if (e.getMessage().contains("owner group")) {
                        error = firstPass;
                    }
                }
            } finally {
                assertTrue(error);
            }

            fileHandle.close();

            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            volume.unlink(userCredentials, FILEPATH);

            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

            volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, "0",
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);
        }

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testRenewVoucher() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testRenewVoucher";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        String voucherSize = "1";

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set vouchersize > quota, so that the voucher will block all space
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", voucherSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        boolean error, firstPass;
        String quota = "5";

        // test renew due to low voucher size
        {
            volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);
            volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quota,
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);
            volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quota,
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);

            // Open a file.
            FileHandle fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

            // check blocked space
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            error = false;
            firstPass = false;
            try {

                // Write to file.
                fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota), 0);

                firstPass = true;

                // extend quota write
                fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota),
                        Integer.parseInt(quota));
            } catch (PosixErrorException e) {
                if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                    error = firstPass;
                }
            } finally {
                assertTrue(error);
            }

            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            fileHandle.close();

            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            volume.unlink(userCredentials, FILEPATH);

            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        }

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testReplicaOpenFile() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testAddReplica";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        String voucherSize = "5";
        String voucherSize2X = "10";

        String fileSize = "5";
        String fileSize2X = "10";

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set vouchersize > quota, so that the voucher will block all space
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", voucherSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        boolean error, firstPass;
        String quotaWithVoucher = "20";

        volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quotaWithVoucher, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quotaWithVoucher,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quotaWithVoucher,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);

        FileHandle fileHandle;

        // create file and put content into it
        {
            // Open a file.
            fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber(), 0777);
            fileHandle.write(userCredentials, getContent(fileSize).getBytes(), Integer.parseInt(fileSize), 0);
            fileHandle.close();
        }

        // Open file again to have open filehandle
        fileHandle = volume.openFile(userCredentials, FILEPATH, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        // check space usage
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        // test add/remove replica to open file
        {
            // add replica of open file
            error = false;
            firstPass = false;
            try {
                addReplicas(volume, FILEPATH, 1);
                assertEquals(fileSize2X, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
                assertEquals(fileSize2X, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
                assertEquals(fileSize2X, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
                assertEquals(voucherSize2X, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
                assertEquals(voucherSize2X,
                        volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
                assertEquals(voucherSize2X,
                        volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

                firstPass = true;

                addReplicas(volume, FILEPATH, 1);
            } catch (PosixErrorException e) {
                if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                    error = firstPass;
                }
            } finally {
                assertTrue(error);
            }

            // remove replica of open file
            Replicas listReplicas = volume.listReplicas(userCredentials, FILEPATH);
            assertEquals(2, listReplicas.getReplicasCount());

            String osdUuid1 = listReplicas.getReplicas(0).getOsdUuids(0);

            volume.removeReplica(userCredentials, FILEPATH, osdUuid1);

            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));
        }

        fileHandle.close();

        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        volume.unlink(userCredentials, FILEPATH);

        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testReplicaClosedFile() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testReplicaClosedFile";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        String voucherSize = "5";

        String fileSize = "5";
        String fileSize2X = "10";

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set vouchersize > quota, so that the voucher will block all space
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", voucherSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        boolean error, firstPass;
        String quotaRegular = "10";

        volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quotaRegular, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quotaRegular,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quotaRegular,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);

        // create file and put content into it
        {
            FileHandle fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber(), 0777);

            // Open a file.
            fileHandle.write(userCredentials, getContent(fileSize).getBytes(), Integer.parseInt(fileSize), 0);
            fileHandle.close();
        }

        // test add/remove replica of closed file
        {

            volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quotaRegular, XATTR_FLAGS.XATTR_FLAGS_CREATE);
            volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quotaRegular,
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);
            volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quotaRegular,
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);

            // add replica
            error = false;
            firstPass = false;
            try {
                addReplicas(volume, FILEPATH, 1);

                assertEquals(fileSize2X, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
                assertEquals(fileSize2X, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
                assertEquals(fileSize2X, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
                assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
                assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
                assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

                firstPass = true;

                addReplicas(volume, FILEPATH, 1);
            } catch (PosixErrorException e) {
                if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                    error = firstPass;
                }
            } finally {
                assertTrue(error);
            }

            // remove replica
            Replicas listReplicas = volume.listReplicas(userCredentials, FILEPATH);
            assertEquals(2, listReplicas.getReplicasCount());

            String osdUuid1 = listReplicas.getReplicas(0).getOsdUuids(0);

            volume.removeReplica(userCredentials, FILEPATH, osdUuid1);

            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));
        }

        volume.unlink(userCredentials, FILEPATH);

        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testDeleteOpenFile() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testDeleteOpenFile";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        String voucherSize = "5";
        String fileSize = "5";

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set vouchersize > quota, so that the voucher will block all space
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", voucherSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        String quotaRegular = "10";

        volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quotaRegular, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quotaRegular,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quotaRegular,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);

        FileHandle fileHandle;

        // create file and put content into it
        {
            fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber(), 0777);

            // Open a file.
            fileHandle.write(userCredentials, getContent(fileSize).getBytes(), Integer.parseInt(fileSize), 0);
            fileHandle.close();
        }

        fileHandle = volume.openFile(userCredentials, FILEPATH, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        volume.unlink(userCredentials, FILEPATH);
        fileHandle.close();

        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testChown() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testChown";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        String voucherSize = "5";
        String fileSize = "5";

        // set credentials on root, because only superuser can perform chown
        UserCredentials userCredentials2 = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set vouchersize > quota, so that the voucher will block all space
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", voucherSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        String quotaInsufficient = "5";
        String quotaSufficient = "10";

        volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quotaSufficient, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quotaSufficient,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quotaSufficient,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME + 2, quotaSufficient,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME + 2, quotaSufficient,
                XATTR_FLAGS.XATTR_FLAGS_CREATE);

        FileHandle fileHandle;

        // create file and put content into it
        {
            fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber(), 0777);

            // Open a file.
            fileHandle.write(userCredentials, getContent(fileSize).getBytes(), Integer.parseInt(fileSize), 0);
            fileHandle.close();
        }

        // chown for closed file
        Stat stat = volume.getAttr(userCredentials, FILEPATH);
        Stat newStat = Stat.newBuilder(stat).setGroupId(GROUPNAME + 2).setUserId(USERNAME + 2).build();
        volume.setAttr(userCredentials2, FILEPATH, newStat, 6); // 6 = user and group

        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME + 2));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME + 2));

        // chown for open file

        fileHandle = volume.openFile(userCredentials, FILEPATH, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME + 2));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME + 2));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME + 2));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME + 2));

        volume.setAttr(userCredentials2, FILEPATH, stat, 6); // 6 = user and group

        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME + 2));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME + 2));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME + 2));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME + 2));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        // chown back with not enough quota

        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME + 2, quotaInsufficient,
                XATTR_FLAGS.XATTR_FLAGS_REPLACE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME + 2, quotaInsufficient,
                XATTR_FLAGS.XATTR_FLAGS_REPLACE);

        boolean error = false;
        boolean firstPass = false;
        try {
            firstPass = true;
            volume.setAttr(userCredentials2, FILEPATH, newStat, 6); // 6 = user and group

            if (QuotaConstants.CHECK_QUOTA_ON_CHOWN) {
                firstPass = false;
            }
        } catch (PosixErrorException e) {
            if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                error = !firstPass;
            }
        } finally {
            assertTrue(!error);
        }

        fileHandle.close();

        volume.unlink(userCredentials, FILEPATH);

        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));

        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME + 2));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME + 2));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME + 2));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME + 2));

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testMultipleOsds() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testMultipleOsds";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        int stripeWidth = 3;
        int stripeSize = 1; // in kb
        int fileSize = 5; // in kb

        String voucherSize = Integer.toString(fileSize * 1024); // in kb
        String fileSizeString = Integer.toString(fileSize * 1024); // in kb

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set default striping policy
        String stripingPolicy = "{\"pattern\":\"STRIPING_POLICY_RAID0\",\"size\":" + stripeSize + ",\"width\":"
                + stripeWidth + "}";
        volume.setXAttr(userCredentials, "/", "xtreemfs.default_sp", stripingPolicy, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        // set vouchersize > quota, so that the voucher will block all space
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", voucherSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        String quota = fileSizeString;
        volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        // create file and write content
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                FILEPATH,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber(), 0777);

        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        fileHandle.write(userCredentials, getContent(fileSizeString).getBytes(), Integer.parseInt(fileSizeString), 0);
        fileHandle.close();

        assertEquals(fileSizeString, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals(fileSizeString, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals(fileSizeString, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

        // check stripe locations
        List<StripeLocation> stripeLocations = volume.getStripeLocations(userCredentials, FILEPATH, 0, fileSize * 1024);
        assertEquals(fileSize / stripeSize, stripeLocations.size());

        int i = 0;
        String[] osdUUIDs = new String[stripeWidth];
        for (StripeLocation stripeLocation : stripeLocations) {
            if (osdUUIDs[i % stripeWidth] == null) {
                osdUUIDs[i % stripeWidth] = Arrays.toString(stripeLocation.getUuids());
            } else {
                assertEquals(osdUUIDs[i % stripeWidth], Arrays.toString(stripeLocation.getUuids()));
            }

            assertEquals(stripeSize * 1024L, stripeLocation.getLength());
            i++;
        }

        // try to open again and write a single byte, although a quota has been reached
        boolean error = false;
        boolean firstPass = false;
        try {
            fileHandle = volume.openFile(userCredentials, FILEPATH, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());
            firstPass = true;

            fileHandle.write(userCredentials, getContent(1).getBytes(), 1, fileSize * 1024);
        } catch (PosixErrorException e) {
            if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                error = firstPass;
            }
        } finally {
            assertTrue(error);
        }

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    @Test
    public void testMultipleClientSingleHost() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testMultipleClientSingleHost";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // 2nd client and 2nd credentials
        UserCredentials userCredentials2 = UserCredentials.newBuilder().setUsername(USERNAME + 2)
                .addGroups(GROUPNAME + 2).build();

        Client client2 = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials2, null, options);
        client2.start();
        Volume volume2 = client.openVolume(VOLUME_NAME, null, options);

        // 3rd client and 3rd credentials
        UserCredentials userCredentials3 = UserCredentials.newBuilder().setUsername(USERNAME + 3)
                .addGroups(GROUPNAME + 3).build();

        Client client3 = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials3, null, options);
        client3.start();
        Volume volume3 = client.openVolume(VOLUME_NAME, null, options);

        String quota = "15";
        String voucherSize = "5";
        String voucherSizeDouble = "10";
        String fileSize = "5";
        String fileSizeDouble = "10";
        String fileSizeTriple = "15";

        // set vouchersize > quota, so that the voucher will block all space
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", voucherSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        // check volume on 2nd client to ensure same view
        assertEquals(quota, volume2.getXAttr(userCredentials, "/", "xtreemfs.quota"));
        assertEquals(quota, volume2.getXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME));
        assertEquals(quota, volume2.getXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME));

        // check volume on 3rd client to ensure same view
        assertEquals(quota, volume3.getXAttr(userCredentials, "/", "xtreemfs.quota"));
        assertEquals(quota, volume3.getXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME));
        assertEquals(quota, volume3.getXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME));

        // create file and write content
        {
            FileHandle fileHandle = volume.openFile(
                    userCredentials,
                    FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber(), 0777);

            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals(voucherSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            fileHandle.write(userCredentials, getContent(fileSize).getBytes(), Integer.parseInt(fileSize), 0);
            fileHandle.close();

            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals(fileSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        }

        // open file now with 3 clients, 1 will fail due to insufficent voucher
        {
            FileHandle fileHandle = volume.openFile(userCredentials, FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());
            FileHandle fileHandle2 = volume2.openFile(userCredentials, FILEPATH,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

            // try to open again, although full quota is used, it should get an xcap
            boolean noError = false;
            try {
                FileHandle fileHandle3 = volume3.openFile(userCredentials, FILEPATH, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());
                noError = true;
                fileHandle3.close();
            } catch (PosixErrorException e) {
                if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                    noError = false;
                }
            } finally {
                assertTrue(noError);
            }

            // check blocked space & write content
            assertEquals(voucherSizeDouble, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals(voucherSizeDouble,
                    volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals(voucherSizeDouble,
                    volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            // close 1st handle: clear voucher should not free blocked space due to 2nd client:
            // clients share same identity, but have different expire time
            fileHandle.write(userCredentials, getContent(fileSize).getBytes(), Integer.parseInt(fileSize),
                    Integer.parseInt(fileSize));
            fileHandle.close();

            assertEquals(voucherSizeDouble, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals(voucherSizeDouble,
                    volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals(voucherSizeDouble,
                    volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            fileHandle2.write(userCredentials, getContent(fileSize).getBytes(), Integer.parseInt(fileSize),
                    Integer.parseInt(fileSizeDouble));
            fileHandle2.close();
            
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
            assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

            assertEquals(fileSizeTriple, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
            assertEquals(fileSizeTriple, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
            assertEquals(fileSizeTriple, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));
        }

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    // TODO(baerhold): Multiple Clients which multiple Hosts (client identities) - how??

    @Test
    public void testMultipleClientMultipleOsds() throws Exception {
        final String VOLUME_NAME = VOLUMENAME + "testMultipleClientMultipleOsds";

        // Start native Client with default options.
        Options options = new Options();
        Client client = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials, null, options);
        client.start();

        // Create and open volume.
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // 2nd client and 2nd credentials
        UserCredentials userCredentials2 = UserCredentials.newBuilder().setUsername(USERNAME + 2)
                .addGroups(GROUPNAME + 2).build();

        Client client2 = ClientFactory.createClient(ClientType.NATIVE, dirAddress, userCredentials2, null, options);
        client2.start();
        Volume volume2 = client.openVolume(VOLUME_NAME, null, options);

        int stripeWidth = 2;
        int stripeSize = 1; // in kb

        String chunkSize = Integer.toString(stripeSize * 1024); // in kb
        String quota = Integer.toString(stripeWidth * stripeSize * 1024); // in kb

        // set default striping policy
        String stripingPolicy = "{\"pattern\":\"STRIPING_POLICY_RAID0\",\"size\":" + stripeSize + ",\"width\":"
                + stripeWidth + "}";
        volume.setXAttr(userCredentials, "/", "xtreemfs.default_sp", stripingPolicy, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        // set vouchersize > quota, so that the voucher will block all space
        volume.setXAttr(userCredentials, "/", "xtreemfs.vouchersize", chunkSize, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        volume.setXAttr(userCredentials, "/", "xtreemfs.quota", quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.userquota." + USERNAME, quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);
        volume.setXAttr(userCredentials, "/", "xtreemfs.groupquota." + GROUPNAME, quota, XATTR_FLAGS.XATTR_FLAGS_CREATE);

        // create file, get xcaps for 2 clients and write content in specific order
        FileHandle fileHandle = volume.openFile(
                userCredentials,
                FILEPATH,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber(), 0777);

        assertEquals(chunkSize, volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals(chunkSize, volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals(chunkSize, volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        // 2nd client will get remaining space as voucher
        FileHandle fileHandle2 = volume2.openFile(userCredentials, FILEPATH,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());

        assertEquals(quota, volume2.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals(quota, volume2.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals(quota, volume2.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        // write data
        fileHandle2.write(userCredentials, getContent(chunkSize).getBytes(), Integer.parseInt(chunkSize), 0);

        boolean noError = false;
        try {
            fileHandle.write(userCredentials, getContent(quota).getBytes(), Integer.parseInt(quota), 0);

            noError = true;
        } catch (PosixErrorException e) {
            if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                noError = false;
            }
        } finally {
            assertTrue(noError);
        }

        boolean error = false;
        try {
            fileHandle2.write(userCredentials, getContent(1).getBytes(), 1, Integer.parseInt(quota));
        } catch (PosixErrorException e) {
            if (e.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOSPC)) {
                error = true;
            }
        } finally {
            assertTrue(error);
        }

        fileHandle2.close();
        fileHandle.close();

        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.blockedspace"));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.userblockedspace." + USERNAME));
        assertEquals("0", volume.getXAttr(userCredentials, "/", "xtreemfs.groupblockedspace." + GROUPNAME));

        assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.usedspace"));
        assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.userusedspace." + USERNAME));
        assertEquals(quota, volume.getXAttr(userCredentials, "/", "xtreemfs.groupusedspace." + GROUPNAME));

        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME);
        client.shutdown();
    }

    // Helper
    private void addReplicas(Volume volume, String fileName, int replicaNumber) throws Exception {

        int repl_flagsX = ReplicationFlags.setFullReplica(ReplicationFlags.setSequentialStrategy(0));
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ, 2,
                repl_flagsX);

        volume.setReplicaUpdatePolicy(userCredentials, fileName, ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ);

        // Get a liste of suitable OSDs and ensure the requestes number of replicas can be added.
        List<String> osdUUIDs = volume.getSuitableOSDs(userCredentials, fileName, replicaNumber);
        assertTrue(osdUUIDs.size() >= replicaNumber);

        // Save the current number of Replicas.
        int currentReplicaNumber = volume.listReplicas(userCredentials, fileName).getReplicasCount();

        // Get the default replication flags.
        ReplicationPolicy rp = volume.getDefaultReplicationPolicy(userCredentials, "/");
        int repl_flags = rp.getFlags();

        StripingPolicy defaultStripingPolicy = StripingPolicy.newBuilder()
                .setType(StripingPolicyType.STRIPING_POLICY_RAID0).setStripeSize(128).setWidth(1).build();

        // Add the required number of new replicas.
        for (int i = 0; i < replicaNumber; i++) {
            Replica replica = Replica.newBuilder().setStripingPolicy(defaultStripingPolicy)
                    .setReplicationFlags(repl_flags).addOsdUuids(osdUUIDs.get(i)).build();
            volume.addReplica(userCredentials, fileName, replica);
        }

        // Ensure the replicas have been added.
        assertEquals(currentReplicaNumber + replicaNumber, volume.listReplicas(userCredentials, fileName)
                .getReplicasCount());
    }

    private String getContent(String lengthAsString) {
        return getContent(Integer.parseInt(lengthAsString));
    }

    private String getContent(int length) {

        String data = "1234567890";
        int dataLength = data.length();

        String content = "";
        int remainingLength = length;
        while (remainingLength > 0) {
            if (remainingLength < dataLength) {
                content += data.substring(0, remainingLength);
            } else {
                content += data;
            }
            remainingLength = length - content.length();
        }

        return content;
    }
}
