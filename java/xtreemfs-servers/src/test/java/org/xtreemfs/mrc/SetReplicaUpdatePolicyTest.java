/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_replica_update_policyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestEnvironment.Services;
import org.xtreemfs.TestHelper;

public class SetReplicaUpdatePolicyTest {
    @Rule
    public final TestRule     testLog = TestHelper.testLog;

    private MRCServiceClient  client;
    
    private InetSocketAddress mrcAddress;
    
    private TestEnvironment   testEnv;
    
    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    @Before
    public void setUp() throws Exception {
        // register an OSD at the directory service (needed in order to assign
        // it to a new file on 'open')
        
        testEnv = new TestEnvironment(Services.DIR_CLIENT, Services.TIME_SYNC, Services.UUID_RESOLVER,
                Services.MRC_CLIENT, Services.DIR_SERVICE, Services.MRC, Services.OSD, Services.OSD);
        testEnv.start();
        
        mrcAddress = testEnv.getMRCAddress();
        client = testEnv.getMrcClient();
    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }

    @Test
    public void testSetReplicaUpdatePolicyOperation() throws Exception {
        final String uid = "root";
        final List<String> gids = createGIDs("root");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        Auth passwd = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD).setAuthPasswd(AuthPassword.newBuilder().setPassword("")).build();

        StripingPolicy sp = SetupUtils.getStripingPolicy(1, 1024);
        
        Client c = new Client(new InetSocketAddress[]{testEnv.getDIRAddress()}, 15000, 60000, null);
        c.start();
        c.createVolume(volumeName, passwd, uc, sp, 
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, 0777 );
        
        
        Volume v = c.getVolume(volumeName, uc);
        Path p = new Path("foo.txt");
        
        
        
        File f = v.getFile(p.toString());
        f.createFile();
        f.setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE);

        final String fileId = f.getxattr("xtreemfs.file_id");
        
        List<XAttr> xattrs = null;
        String replicaUpdatePolicy = null;
        xtreemfs_set_replica_update_policyRequest.Builder msg = xtreemfs_set_replica_update_policyRequest.newBuilder()
                .setFileId(fileId);
        
        //REPL_UPDATE_PC_NONE
        msg.setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE);
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, msg.build()).get();
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE));
        
        //REPL_UPDATE_PC_RONLY
        msg.setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY);
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, msg.build()).get();
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY));
        
        //REPL_UPDATE_PC_WARONE
        try {
            msg.setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE);
            client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, msg.build()).get();
            fail();
        } catch (Exception e) {
            // ignore (could check if the error is the correct one)
        }

        // Reset to REPL_UPDATE_PC_NONE
        msg.setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE);
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, msg.build()).get();

        // REPL_UPDATE_PC_WARONE
        msg.setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE);
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, msg.build()).get();
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE));
        
        //REPL_UPDATE_PC_WQRQ
        msg.setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ);
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, msg.build()).get();
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ));
        
    }
    
    @Test
    public void testDeniedRequestforRwWithStriping() throws Exception {      
        final String uid = "root";
        final List<String> gids = createGIDs("root");
        final String volumeName = "testVolume";
        final UserCredentials uc = createUserCredentials(uid, gids);
        Auth passwd = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD)
                .setAuthPasswd(AuthPassword.newBuilder().setPassword("")).build();

        StripingPolicy sp = SetupUtils.getStripingPolicy(2, 128);

        Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 15000, 60000, null);
        c.start();
        c.createVolume(volumeName, passwd, uc, sp, AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, 0777);

        Volume v = c.getVolume(volumeName, uc);
        Path p = new Path("foo.txt");

        File f = v.getFile(p.toString());
        f.createFile();
        f.setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE);

        final String fileId = f.getxattr("xtreemfs.file_id");
        
        try {
            // try to set replica update policy to WQRQ (expect PBRPCException).
            xtreemfs_set_replica_update_policyRequest.Builder msg = xtreemfs_set_replica_update_policyRequest
                    .newBuilder().setFileId(fileId).setUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ);
            client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, msg.build()).get();
            assertTrue(false);
        } catch (PBRPCException e) {
            // replica update policy should not changed
            assertTrue(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE));
        }
    }
    
    

    private static List<String> createGIDs(String gid) {
        List<String> list = new LinkedList<String>();
        list.add(gid);
        return list;
    }
    
    private static UserCredentials createUserCredentials(String uid, List<String> gids) {
        return UserCredentials.newBuilder().setUsername(uid).addAllGroups(gids).build();
    }
    
    private static StripingPolicy getDefaultStripingPolicy() {
        return StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0).setStripeSize(
            1000).setWidth(1).build();
    }
    
    private static VivaldiCoordinates getDefaultCoordinates() {
        return VivaldiCoordinates.newBuilder().setXCoordinate(0).setYCoordinate(0).setLocalError(0).build();
    }
}
