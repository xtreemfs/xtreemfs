/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.common.uuid;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.util.NetUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingSetResponse;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * 
 * @author bjko
 */
public class UUIDResolverTest {
    @Rule
    public final TestRule   testLog = TestHelper.testLog;

    private TestEnvironment      testEnv;

    @Before
    public void setUp() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.UUID_RESOLVER });
        testEnv.start();
        UUIDResolver.addLocalMapping("localhost", 32636, Schemes.SCHEME_PBRPC);

    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
    }

    @Test
    public void testSimpleMapping() throws Exception {
        List<AddressMapping.Builder> mpgs = NetUtils.getReachableEndpoints(32636, "http");
        // Use the first endoint found for testing purposes.
        AddressMapping testMapping = mpgs.get(0).setUuid("MY_TEST_UUID").build();

        AddressMappingSet.Builder ams = AddressMappingSet.newBuilder();
        ams.addMappings(testMapping);

        RPCResponse<addressMappingSetResponse> r = testEnv.getDirClient().xtreemfs_address_mappings_set(null,
                RPCAuthentication.authNone, RPCAuthentication.userService, ams.build());
        r.get();
        ServiceUUID uuid = new ServiceUUID("MY_TEST_UUID");
        uuid.resolve();
        // System.out.println(uuid);
        // System.out.println(uuid);

        try {
            ServiceUUID uuid2 = new ServiceUUID("YAGGA YAGGA");
            uuid2.getMappings();
            fail("returned result for unknown address mapping");
        } catch (UnknownUUIDException ex) {
            // supi
        }

        Thread.sleep(200);

        uuid = new ServiceUUID("MY_TEST_UUID");
        uuid.resolve();
        // System.out.println(uuid);

        uuid = new ServiceUUID("localhost");
        uuid.resolve();
        // System.out.println(uuid);
    }

}
