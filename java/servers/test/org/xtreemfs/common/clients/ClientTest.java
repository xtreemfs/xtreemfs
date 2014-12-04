/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * @author bjko
 */
public class ClientTest {
    @Rule
    public final TestRule       testLog     = TestHelper.testLog;

    private TestEnvironment     testEnv;

    private static final String VOLUME_NAME = "testvol";

    private UserCredentials     uc;

    public ClientTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @Before
    public void setUp() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.UUID_RESOLVER, TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.MRC, TestEnvironment.Services.OSD });
        testEnv.start();

        uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        RPCResponse r = testEnv.getMrcClient().xtreemfs_mkvol(testEnv.getMRCAddress(), RPCAuthentication.authNone, uc,
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, SetupUtils.getStripingPolicy(64, 1), "", 0777,
                VOLUME_NAME, "test", "test", new LinkedList<KeyValuePair>(), 0);
        r.get();
        r.freeBuffers();
    }

    @After
    public void tearDown() {
        testEnv.shutdown();
    }

    @Test
    public void testMDOps() throws Exception {

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();

        Volume v = c.getVolume(VOLUME_NAME, uc);

        long fspace = v.getFreeSpace();
        long uspace = v.getUsedSpace();
        System.out.println("free/used: " + fspace + "/" + uspace);

        File dir = v.getFile("dir");
        dir.mkdir(0777);

        String[] entries = v.list("/");
        assertEquals(3, entries.length);
        assertEquals("dir", entries[2]);

        entries = v.list("/dir/");
        assertEquals(2, entries.length);

        assertTrue(dir.isDirectory());
        assertFalse(dir.isFile());
        assertTrue(dir.exists());

        File file = v.getFile("/dir/file");
        file.createFile();
        assertFalse(file.isDirectory());
        assertTrue(file.isFile());
        assertTrue(file.exists());

        File file2 = v.getFile("/file2");
        file.renameTo(file2);

        assertFalse(file.exists());
        assertTrue(file2.exists());

        entries = v.list("/");
        assertEquals(4, entries.length);

        file2.delete();

        c.stop();

    }
    
    @Test
    public void testMDOps2() throws Exception {

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();

        UserCredentials rootCreds = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();
        Volume v = c.getVolume(VOLUME_NAME, rootCreds);

        File f = v.getFile("/test");
        f.createFile();
        f.chown("someone");
        f.chgrp("somegroup");
        f.chmod(0777);

        Stat stat = f.stat();
        assertEquals("someone", stat.getUserId());
        assertEquals("somegroup", stat.getGroupId());
        assertEquals(0777, stat.getMode() & 0777);

        Map<String, Object> acl = f.getACL();
        assertEquals(0, acl.size());

        acl.put("u:", "rwx");
        acl.put("g:", "rwx");
        f.setACL(acl);
        
        acl = f.getACL();
        assertEquals("rwx", acl.get("u:"));
        assertEquals("rwx", acl.get("g:"));
                
        acl.clear();
        acl.put("u:test", "r");
        acl.put("g:test", "r");
        f.setACL(acl);
        
        acl = f.getACL();
        assertEquals("r--", acl.get("u:test"));
        assertEquals("r--", acl.get("g:test"));
        
        acl.clear();
        f.setACL(acl);
        
        acl = f.getACL();
        assertFalse(acl.containsKey("u:test"));
        assertFalse(acl.containsKey("g:test"));
    }

    @Test
    public void testData() throws Exception {

        final Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 300000, null);
        c.start();

        Volume v = c.getVolume(VOLUME_NAME, uc);

        File f = v.getFile("/test");

        RandomAccessFile ra = f.open("rw", 0555);
        ra.seek(2);

        byte[] data = new byte[2048];
        int wbytes = ra.write(data, 0, data.length);
        assertEquals(2048, wbytes);

        ra.seek(0);
        int rbytes = ra.read(data, 0, data.length);
        assertEquals(2048, rbytes);

        ra.seek(2);
        rbytes = ra.read(data, 0, data.length);
        assertEquals(2048, rbytes);

        ra.seek(4);
        rbytes = ra.read(data, 0, data.length);
        assertEquals(2048 - 2, rbytes);

        ra.close();

        c.stop();

    }

}