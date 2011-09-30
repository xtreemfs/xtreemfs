/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.mrc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestEnvironment.Services;

import com.google.protobuf.Message;

import junit.framework.TestCase;

public class SetReplicaUpdatePolicyTest extends TestCase {

    private MRCServiceClient  client;
    
    private InetSocketAddress mrcAddress;
    
    private TestEnvironment   testEnv;
    
    public SetReplicaUpdatePolicyTest() {
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
    
    public void  testSetReplicaUpdatePolicyOperation() throws Exception {
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
        
        //REPL_UPDATE_PC_NONE
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, fileId,
                ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE).get();        
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE));
        
        //REPL_UPDATE_PC_RONLY
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, fileId,
                ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY).get();
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY));
        
        //REPL_UPDATE_PC_WARA
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, fileId,
                ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA).get();       
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA));
        
        //REPL_UPDATE_PC_WARONE
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, fileId,
                ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE).get();
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE));
        
        //REPL_UPDATE_PC_WQRQ
        client.xtreemfs_set_replica_update_policy(mrcAddress, passwd, uc, fileId,
                ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ).get();
        assert(f.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ));
        
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
