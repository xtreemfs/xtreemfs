/*
 * Copyright (c) 2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.dir;

import java.io.IOException;
import java.net.InetSocketAddress;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.globalTimeSGetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.test.TestEnvironment;

/**
 *
 * @author bjko
 */
public class DIRClientTest extends TestCase {

    TestEnvironment      testEnv;

    RPCNIOSocketServer   dummy1, dummy2;

    DummyDir             dir1, dir2;

    InetSocketAddress[]  servers;

    static final int PORT_DIR1 = 32638;
    static final int PORT_DIR2 = 32639;

    protected class DummyDir implements RPCServerRequestListener{
        public volatile boolean resetAfterCall = false;
        public volatile String sendRedirectTo = null;
        public volatile boolean sendException = false;
        public volatile boolean donotAnswer = false;
        final int id;

        public DummyDir(int id) {
            this.id = id;
        }
        
        @Override
        public void receiveRecord(RPCServerRequest rq) {
            try {
                final RPCHeader hdr = rq.getHeader();
                assertTrue(hdr.hasRequestHeader());
                assertEquals(DIRServiceConstants.PROC_ID_XTREEMFS_GLOBAL_TIME_S_GET,
                             hdr.getRequestHeader().getProcId());

                if (sendRedirectTo != null) {
                    rq.sendRedirect(sendRedirectTo);
                } else if (sendException) {
                    rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, "exception requested for test");
                } else if (donotAnswer) {
                    // don't do anything
                    rq.freeBuffers();
                } else {

                    globalTimeSGetResponse response = globalTimeSGetResponse.newBuilder().setTimeInSeconds(id).build();
                    try {
                        rq.sendResponse(response, null);
                    } catch (Exception ex) {
                        fail(ex.toString());
                    }
                }
                if (resetAfterCall) {
                    sendRedirectTo = null;
                    sendException = false;
                    donotAnswer = false;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.toString());
            }
        }
    }

    public DIRClientTest() {
        Logging.start(Logging.LEVEL_DEBUG, Category.all);
    }

    @Before
    public void setUp() throws Exception {
        testEnv = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.DIR_CLIENT,
                    TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT
        });
        testEnv.start();

        dir1 = new DummyDir(1);
        dir2 = new DummyDir(2);

        dummy1 = new RPCNIOSocketServer(PORT_DIR1, null, dir1, null);
        dummy1.start();
        dummy1.waitForStartup();

        dummy2 = new RPCNIOSocketServer(PORT_DIR2, null, dir2, null);
        dummy2.start();
        dummy2.waitForStartup();

        servers = new InetSocketAddress[]{new InetSocketAddress("localhost", PORT_DIR1),
                                          new InetSocketAddress("localhost", PORT_DIR2)};
    }

    @After
    public void tearDown() throws Exception {
        dummy1.shutdown();
        dummy2.shutdown();
        dummy1.waitForShutdown();
        dummy2.waitForShutdown();
        testEnv.shutdown();

    }

    @Test
    public void testStandardCase() throws Exception {
        DIRClient client = new DIRClient(testEnv.getDirClient(), servers, 10, 2);
        long result = client.xtreemfs_global_time_get();
        assertEquals(1, result);
    }

    @Test
    public void testRedirect() throws Exception {
        dir1.sendRedirectTo = "localhost:"+PORT_DIR2;

        DIRClient client = new DIRClient(testEnv.getDirClient(), servers, 10, 2);
        long result = client.xtreemfs_global_time_get();
        assertEquals(2, result);
    }

    @Test
    public void testRedirect2() throws Exception {
        dir1.sendRedirectTo = "localhost:"+PORT_DIR2;
        dir2.sendRedirectTo = "localhost:"+PORT_DIR1;

        try {
            DIRClient client = new DIRClient(testEnv.getDirClient(), servers, 10, 2);
            long result = client.xtreemfs_global_time_get();
            fail("Expected exception.");
        } catch (IOException ex) {

        }
    }

    @Test
    public void testFailover() throws Exception {
        dir1.donotAnswer = true;

        DIRClient client = new DIRClient(testEnv.getDirClient(), servers, 10, 2);
        long result = client.xtreemfs_global_time_get();
        assertEquals(2, result);
    }

    @Test
    public void testFailover2() throws Exception {
        dir1.donotAnswer = true;
        dir1.resetAfterCall = true;
        dir2.donotAnswer = true;

        DIRClient client = new DIRClient(testEnv.getDirClient(), servers, 10, 2);
        long result = client.xtreemfs_global_time_get();
        assertEquals(1, result);
    }

    @Test
    public void testNoAnswer() throws Exception {
        dir1.donotAnswer = true;
        dir2.donotAnswer = true;

        try {
            DIRClient client = new DIRClient(testEnv.getDirClient(), servers, 5, 2);
            long result = client.xtreemfs_global_time_get();
            fail("Expected exception.");
        } catch (IOException ex) {

        }
    }

    @Test
    public void testException() throws Exception {
        dir1.donotAnswer = true;
        dir2.sendException = true;

        try {
            DIRClient client = new DIRClient(testEnv.getDirClient(), servers, 10, 2);
            long result = client.xtreemfs_global_time_get();
            fail("Expected exception.");
        } catch (PBRPCException ex) {

        }
    }

}