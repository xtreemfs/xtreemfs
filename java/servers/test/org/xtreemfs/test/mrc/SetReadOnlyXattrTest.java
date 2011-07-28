/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.mrc;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;

import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;

import org.xtreemfs.mrc.utils.Path;

import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_read_only_xattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_read_only_xattrResponse;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;

import junit.framework.TestCase;

public class SetReadOnlyXattrTest extends TestCase {

    private MRCServiceClient  client;

    private InetSocketAddress mrcAddress;

    private TestEnvironment   testEnv;

    public SetReadOnlyXattrTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    public void setUp() throws Exception {

        java.io.File testDir = new java.io.File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        mrcAddress = SetupUtils.getMRC1Addr();

        // register an OSD at the directory service (needed in order to assign
        // it to a new file on 'open')

        testEnv = new TestEnvironment(Services.DIR_CLIENT, Services.TIME_SYNC, Services.UUID_RESOLVER,
                Services.MRC_CLIENT, Services.DIR_SERVICE, Services.MRC, Services.OSD);
        testEnv.start();

        client = testEnv.getMrcClient();
    }

    protected void tearDown() throws Exception {
        testEnv.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }

    public void testSetReadOnlyXattrOperation() throws Exception {
        final String uid = "root";
        final List<String> gids = createGIDs("root");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        Auth passwd = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD).setAuthPasswd(
                AuthPassword.newBuilder().setPassword("")).build();

        StripingPolicy sp = SetupUtils.getStripingPolicy(1, 1024);

        Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 60000, null);
        c.start();
        c.createVolume(volumeName, passwd, uc, sp, AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, 0777);

        Volume v = c.getVolume(volumeName, uc);
        Path p = new Path("foo.txt");

        File f = v.getFile(p.toString());
        f.createFile();
        f.setReadOnly(false);

        final String fileId = f.getxattr("xtreemfs.file_id");


        xtreemfs_set_read_only_xattrResponse response = null;
        
        // set read_only to true an check if it's really set to true
        RPCResponse<xtreemfs_set_read_only_xattrResponse> r1 = null;
        try {
            r1 = client.xtreemfs_set_read_only_xattr(mrcAddress, passwd, uc, fileId, true);
            response = r1.get();
        } finally {
            r1.freeBuffers();
        }
        
        assertTrue(response.getWasSet());
        assertTrue(f.isReadOnly());
        
        // do it the other way around
        f.setReadOnly(true);
        assertTrue(f.isReadOnly());
        RPCResponse<xtreemfs_set_read_only_xattrResponse> r2 = null;

        try {
            r2 = client.xtreemfs_set_read_only_xattr(mrcAddress, passwd, uc, fileId, false);
            response = r2.get();
        } finally {
            r2.freeBuffers();
        }
        
        assertTrue(response.getWasSet());
        assertFalse(f.isReadOnly());
    }

    private static List<String> createGIDs(String gid) {
        List<String> list = new LinkedList<String>();
        list.add(gid);
        return list;
    }

    private static UserCredentials createUserCredentials(String uid, List<String> gids) {
        return UserCredentials.newBuilder().setUsername(uid).addAllGroups(gids).build();
    }

}
