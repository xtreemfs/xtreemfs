/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.common.uuid;

import java.io.File;
import java.net.InetSocketAddress;
import junit.framework.TestCase;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.util.NetUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AddressMappingSet;
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

        localhost = new InetSocketAddress("localhost", conf.getPort());

        dirCtrl = new DIRRequestDispatcher(conf);
        dirCtrl.startup();
        
        testEnv = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.DIR_CLIENT,
                    TestEnvironment.Services.TIME_SYNC,TestEnvironment.Services.UUID_RESOLVER});
        testEnv.start();
        UUIDResolver.addLocalMapping("localhost", 32636, false);

        
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        dirCtrl.shutdown();
        testEnv.shutdown();
    }

    public void testSimpleMapping() throws Exception {
        AddressMappingSet mpgs = NetUtils.getReachableEndpoints(32636, "http");
        mpgs.get(0).setUuid("MY_TEST_UUID");
        RPCResponse<Long> r = testEnv.getDirClient().xtreemfs_address_mappings_set(null,mpgs);
        r.get();
        ServiceUUID uuid = new ServiceUUID("MY_TEST_UUID");
        uuid.resolve();
        System.out.println(uuid);
        System.out.println(uuid);

        try {
            ServiceUUID uuid2 = new ServiceUUID("YAGGA YAGGA");
            uuid2.getAddress();
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
