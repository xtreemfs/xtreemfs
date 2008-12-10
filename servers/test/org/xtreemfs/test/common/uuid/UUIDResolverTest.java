/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.common.uuid;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.util.NetUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class UUIDResolverTest extends TestCase {

    private final String      nullAuth;

    private RequestController dirCtrl = null;

    private DIRClient         dc;

    private InetSocketAddress localhost;

    private final int         TIMEOUT = 10000;


    public UUIDResolverTest(String testName) throws JSONException {
        super(testName);
        nullAuth = NullAuthProvider.createAuthString("bla", "blub");
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
        dc = SetupUtils.createDIRClient(60000);

        dirCtrl = new RequestController(conf);
        dirCtrl.startup();

        TimeSync.initialize(dc, 1000000, 50, nullAuth);

        UUIDResolver.shutdown();
        UUIDResolver.start(dc,100,100);
        UUIDResolver.addLocalMapping("localhost", 32636, false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        dirCtrl.shutdown();
        dc.shutdown();
        UUIDResolver.shutdown();
    }

    public void testSimpleMapping() throws Exception {
        List<Map<String,Object>> mpgs = NetUtils.getReachableEndpoints(32636, "http");
        RPCResponse r = dc.registerAddressMapping("MY_TEST_UUID", mpgs, 1l , nullAuth);
        r.waitForResponse();
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
