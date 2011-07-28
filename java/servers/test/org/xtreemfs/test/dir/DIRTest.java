/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.dir;

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingSetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.configurationSetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.globalTimeSGetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 *
 * @author bjko
 */
public class DIRTest extends TestCase {

    DIRRequestDispatcher dir;

    DIRConfig            config;
    
    BabuDBConfig         dbsConfig;

    TestEnvironment      testEnv;

    public DIRTest() throws IOException {
        config = SetupUtils.createDIRConfig();
        dbsConfig = SetupUtils.createDIRdbsConfig();
        Logging.start(Logging.LEVEL_DEBUG);
    }

    @Before
    public void setUp() throws Exception {

        testEnv = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.DIR_CLIENT,
                    TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT
        });
        testEnv.start();

        dir = new DIRRequestDispatcher(config, dbsConfig);
        dir.startup();
        dir.waitForStartup();
    }

    @After
    public void tearDown() throws Exception {
        dir.shutdown();

        dir.waitForShutdown();

        testEnv.shutdown();
        
    }

    //@Test
    public void testGlobalTime() throws Exception {

        RPCResponse<globalTimeSGetResponse> r = testEnv.getDirClient().xtreemfs_global_time_s_get(null, RPCAuthentication.authNone, RPCAuthentication.userService);
        long response = r.get().getTimeInSeconds();
        r.freeBuffers();
    }


    @Test
    public void testAddressMapping() throws Exception {

        DIRServiceClient client = testEnv.getDirClient();

        AddressMappingSet.Builder setB = AddressMappingSet.newBuilder();
        AddressMapping mapping = AddressMapping.newBuilder().setUuid("uuid1").setProtocol(Schemes.SCHEME_PBRPC).setAddress("localhost").setPort(12345).setMatchNetwork("*")
                .setTtlS(3600).setVersion(0).setUri(Schemes.SCHEME_PBRPC+"://localhost:12345").build();
        setB.addMappings(mapping);
        AddressMappingSet set = setB.build();

        RPCResponse<addressMappingSetResponse> r1 = client.xtreemfs_address_mappings_set(null, RPCAuthentication.authNone, RPCAuthentication.userService, set);
        r1.get();
        r1.freeBuffers();

        r1 = client.xtreemfs_address_mappings_set(null, RPCAuthentication.authNone, RPCAuthentication.userService, set);
        try {
            r1.get();
            fail();
        } catch (PBRPCException ex) {
            assertEquals(ex.getPOSIXErrno(),POSIXErrno.POSIX_ERROR_EAGAIN);
            //expected exception because of version mismatch
        }
        r1.freeBuffers();

        RPCResponse<AddressMappingSet> r2 = client.xtreemfs_address_mappings_get(null, RPCAuthentication.authNone, RPCAuthentication.userService, "uuid1");
        AddressMappingSet response = r2.get();
        assertEquals(response.getMappingsCount(),1);
        assertEquals(response.getMappings(0).getUuid(),"uuid1");
        assertEquals(response.getMappings(0).getProtocol(),Schemes.SCHEME_PBRPC);
        assertEquals(response.getMappings(0).getAddress(),"localhost");
        assertEquals(response.getMappings(0).getVersion(),1);
        r2.freeBuffers();

        RPCResponse r3 = client.xtreemfs_address_mappings_remove(null, RPCAuthentication.authNone, RPCAuthentication.userService, "uuid1");
        r3.get();
        r3.freeBuffers();

    }

    @Test
    public void testRegistry() throws Exception {

        DIRServiceClient client = testEnv.getDirClient();

        ServiceDataMap dmap = ServiceDataMap.newBuilder().addData(KeyValuePair.newBuilder().setKey("bla").setValue("yagga")).build();
        Service sr = Service.newBuilder().setData(dmap).setType(ServiceType.SERVICE_TYPE_MRC).setUuid("uuid1").setName("mrc @ farnsworth").setLastUpdatedS(0).setVersion(0).build();

        RPCResponse<serviceRegisterResponse> r1 = client.xtreemfs_service_register(null, RPCAuthentication.authNone, RPCAuthentication.userService, sr);
        r1.get();
        r1.freeBuffers();

        r1 = client.xtreemfs_service_register(null, RPCAuthentication.authNone, RPCAuthentication.userService, sr);
        try {
            r1.get();
            fail();
        } catch (PBRPCException ex) {
            assertEquals(ex.getPOSIXErrno(),POSIXErrno.POSIX_ERROR_EAGAIN);
            //expected exception because of version mismatch
        }
        r1.freeBuffers();

        RPCResponse<ServiceSet> r2 = client.xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone, RPCAuthentication.userService, "uuid1");
        ServiceSet response = r2.get();
        r2.freeBuffers();
        
        
        RPCResponse r3 = client.xtreemfs_service_deregister(null, RPCAuthentication.authNone, RPCAuthentication.userService, "uuid1");
        r3.get();
        r3.freeBuffers();

    }
    
    @Test
    public void testConfiguration() throws Exception {
    	
    	DIRServiceClient client = testEnv.getDirClient();
    	
    	long version = 0;
    	String uuid = "uuidConfTest";
    	int parameterNumber = 5;
    	
    	Configuration.Builder confBuilder = Configuration.newBuilder();
    	confBuilder.setVersion(version).setUuid(uuid);
    	for (int i = 0; i < parameterNumber; i++) {
			confBuilder.addParameter(KeyValuePair.newBuilder()
					.setKey("key"+i).setValue("value"+i).build());
		}
    	
    	RPCResponse<configurationSetResponse> responseSet = null;
    	responseSet = client.xtreemfs_configuration_set(null, RPCAuthentication.authNone, 
    			RPCAuthentication.userService, confBuilder.build());
    	
    	responseSet.get();
    	responseSet.freeBuffers();
    	
    	confBuilder = Configuration.newBuilder();
    	confBuilder.setVersion(version).setUuid(uuid);
    	for (int i = 0; i < parameterNumber; i++) {
			confBuilder.addParameter(KeyValuePair.newBuilder()
					.setKey("key"+i).setValue("value"+i).build());
		}
    	
    	responseSet = client.xtreemfs_configuration_set(null, RPCAuthentication.authNone, 
    			RPCAuthentication.userService, confBuilder.build());
    	
    	try {
			responseSet.get();
			fail();
		} catch (PBRPCException ex) {
            assertEquals(ex.getPOSIXErrno(),POSIXErrno.POSIX_ERROR_EAGAIN);
            //expected exception because of version mismatch
		} finally {
			responseSet.freeBuffers();
		}
		
		RPCResponse<Configuration> resonseGet = null;
		resonseGet = client.xtreemfs_configuration_get(null, RPCAuthentication.authNone, 
				RPCAuthentication.userService, uuid);
		
		Configuration newConf = (Configuration) resonseGet.get();
		
		assertEquals(version+1, newConf.getVersion());
		assertEquals(uuid, newConf.getUuid());
		
		System.out.println(newConf.getAllFields().toString());
		
		for (int i = 0; i < parameterNumber; i++) {
			assertEquals(new String("key"+i), newConf.getParameter(i).getKey());
			assertEquals("value"+i, newConf.getParameter(i).getValue());
		}
    	
		resonseGet.freeBuffers();
   
    }


    @Test
    public void testManyUpdates() throws Exception {

        DIRServiceClient client = testEnv.getDirClient();

        ServiceDataMap dmap = ServiceDataMap.newBuilder().addData(KeyValuePair.newBuilder().setKey("bla").setValue("yagga")).build();
        Service sr = Service.newBuilder().setData(dmap).setType(ServiceType.SERVICE_TYPE_MRC).setUuid("uuid22").setName("mrc @ farnsworth").setLastUpdatedS(0).setVersion(0).build();

        for (int i = 0; i < 100; i++) {
            RPCResponse<serviceRegisterResponse> r1 = client.xtreemfs_service_register(null, RPCAuthentication.authNone, RPCAuthentication.userService, sr);
            r1.get();
            r1.freeBuffers();

            r1 = client.xtreemfs_service_register(null, RPCAuthentication.authNone, RPCAuthentication.userService, sr);
            try {
                r1.get();
                fail();
            } catch (PBRPCException ex) {
                assertEquals(ex.getPOSIXErrno(),POSIXErrno.POSIX_ERROR_EAGAIN);
                //expected exception because of version mismatch
            }
            r1.freeBuffers();

            RPCResponse<ServiceSet> r2 = client.xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone, RPCAuthentication.userService, "uuid22");
            ServiceSet response = r2.get();
            r2.freeBuffers();


            RPCResponse r3 = client.xtreemfs_service_deregister(null, RPCAuthentication.authNone, RPCAuthentication.userService, "uuid22");
            r3.get();
            r3.freeBuffers();
            //sr = sr.toBuilder().setVersion(i+1).build();
        }
        Thread.sleep(1000*5);

    }

}
