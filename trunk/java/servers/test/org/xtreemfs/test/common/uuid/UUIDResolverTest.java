/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.common.uuid;

import com.google.protobuf.Message.Builder;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.TestCase;

import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.util.NetUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingSetResponse;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 *
 * @author bjko
 */
public class UUIDResolverTest extends TestCase {


    private DIRRequestDispatcher dirCtrl = null;

    private InetSocketAddress localhost;

    private RPCNIOSocketClient rpcClient;

    private final int         TIMEOUT = 10000;

    private TestEnvironment   testEnv;


    public UUIDResolverTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Logging.start(Logging.LEVEL_DEBUG);
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);
        FSUtils.delTree(testDir);
        testDir.mkdirs();

        DIRConfig conf = SetupUtils.createDIRConfig();
        BabuDBConfig dbConf = SetupUtils.createDIRdbsConfig();
        
        localhost = new InetSocketAddress("localhost", conf.getPort());

        dirCtrl = new DIRRequestDispatcher(conf,dbConf);
        dirCtrl.startup();
        
        testEnv = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.DIR_CLIENT,
                    TestEnvironment.Services.TIME_SYNC,TestEnvironment.Services.UUID_RESOLVER});
        testEnv.start();
        UUIDResolver.addLocalMapping("localhost", 32636, Schemes.SCHEME_PBRPC);

        
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        dirCtrl.shutdown();
        testEnv.shutdown();
    }

    public void testSimpleMapping() throws Exception {
        List<AddressMapping.Builder> mpgs = NetUtils.getReachableEndpoints(32636, "http");
        mpgs.get(0).setUuid("MY_TEST_UUID");
        AddressMappingSet.Builder ams = AddressMappingSet.newBuilder();
        for (AddressMapping.Builder b : mpgs)
            ams.addMappings(b);
        RPCResponse<addressMappingSetResponse> r = testEnv.getDirClient().xtreemfs_address_mappings_set(null,RPCAuthentication.authNone, RPCAuthentication.userService, ams.build());
        r.get();
        ServiceUUID uuid = new ServiceUUID("MY_TEST_UUID");
        uuid.resolve();
        System.out.println(uuid);
        System.out.println(uuid);

        try {
            ServiceUUID uuid2 = new ServiceUUID("YAGGA YAGGA");
            uuid2.getMappings();
            fail("returned result for unknown address mapping");
        } catch (UnknownUUIDException ex) {
            //supi
        }

        Thread.sleep(200);

        uuid = new ServiceUUID("MY_TEST_UUID");
        uuid.resolve();
        System.out.println(uuid);

        uuid = new ServiceUUID("localhost");
        uuid.resolve();
        System.out.println(uuid);
    }


}
