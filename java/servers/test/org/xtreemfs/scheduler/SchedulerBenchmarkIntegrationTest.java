/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.pbrpc.generatedinterfaces.*;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import java.lang.reflect.Field;
import java.util.HashSet;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author jensvfischer
 */
public class SchedulerBenchmarkIntegrationTest {
    TestEnvironment testEnv;
    SchedulerConfig config;
    BabuDBConfig dbsConfig;
    SchedulerRequestDispatcher scheduler;
    SchedulerServiceClient client;

    public SchedulerBenchmarkIntegrationTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        config = SetupUtils.createSchedulerConfig();
        dbsConfig = SetupUtils.createSchedulerdbsConfig();
    }

    @Before
    public void setUp() throws Exception {
        testEnv = new TestEnvironment(TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.MRC,
                TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.OSD,
                TestEnvironment.Services.SCHEDULER_CLIENT);
        testEnv.start();
        client = testEnv.getSchedulerClient();
        scheduler = new SchedulerRequestDispatcher(config, dbsConfig);
        scheduler.startup();
        scheduler.waitForStartup();
        }

    @After
    public void tearDown() throws Exception {
        scheduler.shutdown();
        scheduler.waitForShutdown();

        testEnv.shutdown();
    }

    @Test
    public void testSchedulerBenchmarkIntegration() throws Exception {

        /* wait for the benchmark to finish */
        Scheduler.freeResourcesResponse resources = client.getFreeResources(null,
                RPCAuthentication.authNone, RPCAuthentication.userService).get();
        while (resources.getStreamingThroughput() == 0.0){
            Thread.sleep(500);
            resources = client.getFreeResources(null, RPCAuthentication.authNone, RPCAuthentication.userService).get();
        }

        assertTrue(resources.getRandomThroughput() != 0.0);

        /* Get requestedBenchmark field */
        Field requestedBenchmarksField = SchedulerRequestDispatcher.class.getDeclaredField("requestedBenchmarks");
        requestedBenchmarksField.setAccessible(true);
        HashSet<String> requestedBenchmarks = (HashSet<String>) requestedBenchmarksField.get(SchedulerRequestDispatcher.class);

        /* requestedBenchmarks should be empty after the successful benchmark*/
        assertEquals(0, requestedBenchmarks.size());
    }

}
