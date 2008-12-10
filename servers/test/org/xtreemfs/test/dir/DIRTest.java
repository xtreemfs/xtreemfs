/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.dir;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.*;
import junit.textui.TestRunner;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class DIRTest extends TestCase {

    private final String      nullAuth;

    private RequestController dirCtrl = null;

    private DIRClient         dc;

    private InetSocketAddress localhost;

    private final int         TIMEOUT = 10000;

    public DIRTest(String testName) throws Exception {
        super(testName);
        nullAuth = NullAuthProvider.createAuthString("bla", "blub");
    }

    protected void setUp() throws Exception {

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

    }

    protected void tearDown() throws Exception {
        dc.shutdown();
        dirCtrl.shutdown();
        dc.waitForShutdown();
    }

    public void testRegisterAndQuery() throws Exception {

        Map<String, Object> attrs = new HashMap();

        attrs.put("type", "OSD");
        try {
            RPCResponse<Long> resp = dc.registerEntity("test1", attrs, 1, nullAuth);
            long v = resp.get();
            resp.freeBuffers();
            assertTrue(v > 0);
        } catch (HttpErrorException ex) {
            ex.printStackTrace();
            if (ex.getStatusCode() == HTTPUtils.SC_USER_EXCEPTION) {
                throw ex;
            }
        }

        attrs.clear();
        attrs.put("type", "MRC");
        try {
            RPCResponse<Long> resp = dc.registerEntity("test2", attrs, 1, nullAuth);
            long v = resp.get();
            resp.freeBuffers();
            assertTrue(v > 1);
        } catch (HttpErrorException ex) {
            ex.printStackTrace();
            if (ex.getStatusCode() == HTTPUtils.SC_USER_EXCEPTION) {
                throw ex;
            }
        }

        Map<String, Object> query = new HashMap();
        query.put("type", "MRC");

        List<String> attrs2 = new ArrayList(2);
        attrs2.add("version");
        attrs2.add("type");

        RPCResponse<Map<String, Map<String, Object>>> resp = dc
                .getEntities(query, attrs2, nullAuth);
        Map<String, Map<String, Object>> rv = resp.get();
        resp.freeBuffers();
        assertNotNull(rv);

        assertEquals(rv.size(), 1);
        assertNotNull(rv.get("test2"));

        resp = dc.deregisterEntity("test2", nullAuth);
        resp.waitForResponse();
        resp.freeBuffers();

        query.clear();
        resp = dc.getEntities(query, attrs2, nullAuth);
        rv = resp.get();
        resp.freeBuffers();
        assertNotNull(rv);

        assertEquals(rv.size(), 1);
        assertNotNull(rv.get("test1"));
        assertNull(rv.get("test2"));
    }

    public void testReRegister() throws Exception {

        Map<String, Object> attrs = new HashMap();
        long v = 0;

        // register an entity
        attrs.put("type", "OSD");
        try {
            RPCResponse<Long> resp = dc.registerEntity("test", attrs, 1, nullAuth);
            v = resp.get();
            resp.freeBuffers();
            assertTrue(v > 0);
        } catch (HttpErrorException ex) {
            ex.printStackTrace();
            if (ex.getStatusCode() == HTTPUtils.SC_USER_EXCEPTION) {
                throw ex;
            }
        }

        // try to register an entity with wrong version number; this should fail
        RPCResponse<Long> r1 = null;
        try {
            r1 = dc.registerEntity("test", attrs, 0, nullAuth);
            r1.waitForResponse();
            fail("registration with wrong version number successful!");
        } catch (HttpErrorException ex) {
            r1.freeBuffers();
        }

        // update a registered entity
        try {
            RPCResponse<Long> r2 = dc.registerEntity("test", attrs, v, nullAuth);
            long v2 = r2.get();
            r2.freeBuffers();
            assertFalse(v == v2);

        } catch (HttpErrorException ex) {
        }

        Map<String, Object> query = new HashMap();
        query.put("type", "OSD");

        RPCResponse<Map<String, Map<String, Object>>> resp = dc.getEntities(query, null, nullAuth);
        Map<String, Map<String, Object>> rv = resp.get();
        resp.freeBuffers();
        assertNotNull(rv);

        assertEquals(1, rv.size());

        Map<String, Object> map = rv.get("test");
        assertNotNull(map);
        assertEquals(5, map.size());
    }

    public static void main(String[] args) {
        TestRunner.run(DIRTest.class);
    }

}
