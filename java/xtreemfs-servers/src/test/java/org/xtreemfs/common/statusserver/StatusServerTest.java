/*
 * Copyright (c) 2014 by Michael Berlin, Zuse Institute Berlin.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.statusserver;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestHelper;

public class StatusServerTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    /*
     * Start two servers concurrently at the same port. The second server should keep retrying to bind the
     * port until the first server has stopped.
     */
    @Test
    public void testStartRetryLogicIfAddressAlreadyInUse() throws IOException, InterruptedException {
        final StatusServer server = new StatusServer(ServiceType.SERVICE_TYPE_OSD, new Object(),
                SetupUtils.PORT_RANGE_OFFSET + PORTS.OSD_HTTP_PORT_DEFAULT.getNumber());
        final StatusServer server2 = new StatusServer(ServiceType.SERVICE_TYPE_OSD, new Object(),
                SetupUtils.PORT_RANGE_OFFSET + PORTS.OSD_HTTP_PORT_DEFAULT.getNumber());

        // Start first server and let it block the port for 5 seconds.
        server.start();
        Thread shutdownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                }
                server.shutdown();
            }
        });
        shutdownThread.start();

        // Start second server on the same port.
        server2.start();
        server2.shutdown();

        shutdownThread.join();
    }

}