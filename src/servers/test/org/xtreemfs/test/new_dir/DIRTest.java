/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.new_dir;

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.DIRInterface.ConcurrentModificationException;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.utils.NettestClient;

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

        RPCResponse<Long> r = testEnv.getDirClient().xtreemfs_global_time_get(null);
        Long response = r.get();

    }

    @Test
    public void testNettestNop() throws Exception {
        NettestClient client = new NettestClient(testEnv.getRpcClient(),testEnv.getDirClient().getDefaultServerAddress());
        RPCResponse r = client.xtreemfs_nettest_nop(null);
        r.get();
        r.freeBuffers();

        ReusableBuffer data = ReusableBuffer.wrap("yagga yagga".getBytes());

        r = client.xtreemfs_nettest_send_buffer(null,data);
        r.get();
        r.freeBuffers();

        RPCResponse<ReusableBuffer> r2 = client.xtreemfs_nettest_recv_buffer(null,1024);
        ReusableBuffer responseData = r2.get();
        r2.freeBuffers();

        assertEquals(1024,responseData.limit());
    }

    @Test
    public void testAddressMapping() throws Exception {

        DIRClient client = testEnv.getDirClient();

        AddressMappingSet set = new AddressMappingSet();
        AddressMapping mapping = new AddressMapping("uuid1", 0, "oncrpc", "localhost", 12345, "*", 3600,
                "oncrpc://localhost:12345");
        set.add(mapping);

        RPCResponse<Long> r1 = client.xtreemfs_address_mappings_set(null, set);
        r1.get();
        r1.freeBuffers();

        r1 = client.xtreemfs_address_mappings_set(null, set);
        try {
            r1.get();
            fail();
        } catch (ConcurrentModificationException ex) {
            //expected exception because of version mismatch
        }

        RPCResponse<AddressMappingSet> r2 = client.xtreemfs_address_mappings_get(null, "uuid1");
        AddressMappingSet response = r2.get();
        assertEquals(response.size(),1);
        assertEquals(response.get(0).getUuid(),"uuid1");
        assertEquals(response.get(0).getProtocol(),"oncrpc");
        assertEquals(response.get(0).getAddress(),"localhost");
        assertEquals(response.get(0).getVersion(),1);

        RPCResponse r3 = client.xtreemfs_address_mappings_remove(null, "uuid1");
        r3.get();

    }

    @Test
    public void testRegistry() throws Exception {

        DIRClient client = testEnv.getDirClient();

        ServiceDataMap dmap = new ServiceDataMap();
        dmap.put("bla", "yagga");
        Service sr = new Service(ServiceType.SERVICE_TYPE_MRC,"uuid1", 0, "mrc @ farnsworth", 0,dmap);

        RPCResponse<Long> r1 = client.xtreemfs_service_register(null, sr);
        r1.get();
        r1.freeBuffers();

        r1 = client.xtreemfs_service_register(null, sr);
        try {
            r1.get();
            fail();
        } catch (ConcurrentModificationException ex) {
            //expected exception because of version mismatch
        }

        RPCResponse<ServiceSet> r2 = client.xtreemfs_service_get_by_uuid(null, "uuid1");
        ServiceSet response = r2.get();
        

        RPCResponse r3 = client.xtreemfs_service_deregister(null, "uuid1");
        r3.get();

    }

}
