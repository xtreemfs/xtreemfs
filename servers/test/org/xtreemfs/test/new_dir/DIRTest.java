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
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.KeyValuePair;
import org.xtreemfs.interfaces.KeyValuePairSet;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.ServiceRegistrySet;
import org.xtreemfs.interfaces.DIRInterface.deleteAddressMappingsResponse;
import org.xtreemfs.interfaces.Exceptions.ConcurrentModificationException;
import org.xtreemfs.new_dir.DIRConfig;
import org.xtreemfs.new_dir.DIRRequestDispatcher;
import org.xtreemfs.new_dir.client.DIRClient;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class DIRTest extends TestCase {

    DIRRequestDispatcher dir;

    RPCNIOSocketClient   rpcClient;

    DIRConfig            config;

    public DIRTest() throws IOException {
        config = SetupUtils.createNewDIRConfig();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logging.start(Logging.LEVEL_DEBUG);
        TimeSync.initialize(null, 100000, 50, "");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        TimeSync.close();
    }

    @Before
    public void setUp() throws Exception {
        dir = new DIRRequestDispatcher(config);
        dir.startup();
        dir.waitForStartup();

        rpcClient = new RPCNIOSocketClient(null, 5*1000, 15*1000);
        rpcClient.start();
        rpcClient.waitForStartup();
    }

    @After
    public void tearDown() throws Exception {
        rpcClient.shutdown();
        dir.shutdown();

        dir.waitForShutdown();
        rpcClient.waitForShutdown();
    }

    //@Test
    public void testGlobalTime() throws Exception {

        DIRClient client = new DIRClient(rpcClient, new InetSocketAddress("localhost",config.getPort()));

        RPCResponse<Long> r = client.getGlobalTime(null);
        Long response = r.get();

    }

    @Test
    public void testAddressMapping() throws Exception {

        DIRClient client = new DIRClient(rpcClient, new InetSocketAddress("localhost",config.getPort()));

        AddressMappingSet set = new AddressMappingSet();
        AddressMapping mapping = new AddressMapping("uuid1", 0, "oncrpc", "localhost", 12345, "*", 3600);
        set.add(mapping);

        RPCResponse<Long> r1 = client.setAddressMapping(null, set);
        r1.get();
        r1.freeBuffers();

        r1 = client.setAddressMapping(null, set);
        try {
            r1.get();
            fail();
        } catch (ConcurrentModificationException ex) {
            //expected exception because of version mismatch
        }

        RPCResponse<AddressMappingSet> r2 = client.getAddressMapping(null, "uuid1");
        AddressMappingSet response = r2.get();
        assertEquals(response.size(),1);
        assertEquals(response.get(0).getUuid(),"uuid1");
        assertEquals(response.get(0).getProtocol(),"oncrpc");
        assertEquals(response.get(0).getAddress(),"localhost");
        assertEquals(response.get(0).getVersion(),1);

        RPCResponse<deleteAddressMappingsResponse> r3 = client.deleteAddressMapping(null, "uuid1");
        r3.get();

    }

    @Test
    public void testRegistry() throws Exception {

        DIRClient client = new DIRClient(rpcClient, new InetSocketAddress("localhost",config.getPort()));

        KeyValuePairSet kvset = new KeyValuePairSet();
        kvset.add(new KeyValuePair("bla", "yagga"));
        ServiceRegistry sr = new ServiceRegistry("uuid1", 0, Constants.SERVICE_TYPE_MRC, "mrc @ farnsworth", kvset);

        RPCResponse<Long> r1 = client.registerService(null, sr);
        r1.get();
        r1.freeBuffers();

        r1 = client.registerService(null, sr);
        try {
            r1.get();
            fail();
        } catch (ConcurrentModificationException ex) {
            //expected exception because of version mismatch
        }

        RPCResponse<ServiceRegistrySet> r2 = client.getServiceByUuid(null, "uuid1");
        ServiceRegistrySet response = r2.get();
        

        RPCResponse r3 = client.deregisterService(null, "uuid1");
        r3.get();

    }

}