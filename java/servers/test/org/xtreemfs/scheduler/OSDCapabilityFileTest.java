/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.pbrpc.generatedinterfaces.*;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import static org.junit.Assert.*;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class OSDCapabilityFileTest {
    SchedulerConfig schedulerConfig;
    SchedulerRequestDispatcher schedulerRequestDispatcher;
    TestEnvironment testEnv;

    public OSDCapabilityFileTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @Before
    public void startUp() throws Exception {
        schedulerConfig = SetupUtils.createSchedulerConfigWithCapabilityFile();
        File capabilityFile = new File(schedulerConfig.getOSDCapabilitiesFile());
        String capabilityString = "osd-ssd-maia01;5000.0;256000.0;200.0,200.0,200.0,200.0,200.0,200.0\n" +
                                  "osd-hdd-maia01;100.0;200000.0;10.0,10.0,10.0,10.0,10.0,10.0";

        if(!capabilityFile.createNewFile())
            fail();

        BufferedWriter capabilityFileWriter = new BufferedWriter(new FileWriter(capabilityFile));
        capabilityFileWriter.write(capabilityString);
        capabilityFileWriter.close();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] {
                TestEnvironment.Services.SCHEDULER_CLIENT});
        testEnv.start();

        schedulerRequestDispatcher = new SchedulerRequestDispatcher(schedulerConfig, SetupUtils.createSchedulerdbsConfig());
        schedulerRequestDispatcher.startup();
    }

    @After
    public void tearDown() throws Exception {
        File capabilityFile = new File(schedulerConfig.getOSDCapabilitiesFile());
        capabilityFile.delete();
        schedulerRequestDispatcher.shutdown();
        testEnv.shutdown();
    }

    @Test
    public void testGetFreeResources() throws Exception {
        SchedulerServiceClient schedulerClient = testEnv.getSchedulerClient();
        Scheduler.freeResourcesResponse resources = schedulerClient.getFreeResources(null,
                RPCAuthentication.authNone, RPCAuthentication.userService).get();
        assertTrue(resources.getRandomCapacity() >= 100.0);
        assertTrue(resources.getRandomThroughput() >= 100.0);
        assertTrue(resources.getStreamingThroughput() >= 100.0);
    }

    @Test
    public void testCreateReservation() throws Exception {
        SchedulerServiceClient schedulerClient = testEnv.getSchedulerClient();
        Scheduler.reservation res = Scheduler.reservation.newBuilder().setCapacity(200000.0)
                .setStreamingThroughput(5.0).setType(Scheduler.reservationType.STREAMING_RESERVATION)
                .setVolume(Scheduler.volumeIdentifier.newBuilder().setUuid("test").build()).build();
        Scheduler.osdSet osds = schedulerClient.scheduleReservation(null, RPCAuthentication.authNone,
                RPCAuthentication.userService, res).get();
        assertTrue(osds.getOsdCount() > 0);
        assertTrue(osds.getOsd(0).getUuid().equals("osd-hdd-maia01"));

        res = Scheduler.reservation.newBuilder().setCapacity(100000.0)
                .setStreamingThroughput(200.0).setType(Scheduler.reservationType.STREAMING_RESERVATION)
                .setVolume(Scheduler.volumeIdentifier.newBuilder().setUuid("test2").build()).build();
        osds = schedulerClient.scheduleReservation(null, RPCAuthentication.authNone,
                RPCAuthentication.userService, res).get();
        assertTrue(osds.getOsdCount() > 0);
        assertTrue(osds.getOsd(0).getUuid().equals("osd-ssd-maia01"));
    }
}
