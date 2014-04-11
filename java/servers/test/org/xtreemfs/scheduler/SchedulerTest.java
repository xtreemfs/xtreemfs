package org.xtreemfs.scheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.Common;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import static org.junit.Assert.*;

public class SchedulerTest {
	TestEnvironment testEnv;
	SchedulerConfig config;
	BabuDBConfig dbsConfig;
	SchedulerRequestDispatcher scheduler;
	SchedulerServiceClient client;

	public SchedulerTest() throws Exception {
		Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

		config = SetupUtils.createSchedulerConfig();
		dbsConfig = SetupUtils.createSchedulerdbsConfig();
	}

	@Before
	public void setUp() throws Exception {
		testEnv = new TestEnvironment(new TestEnvironment.Services[] {
				TestEnvironment.Services.DIR_SERVICE,
				TestEnvironment.Services.RPC_CLIENT,
				TestEnvironment.Services.OSD,
				TestEnvironment.Services.SCHEDULER_CLIENT,});
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
	public void testScheduleRequest() throws Exception {
		Scheduler.reservation.Builder resBuilder = Scheduler.reservation
				.newBuilder();
		resBuilder.setCapacity(10.0);
		resBuilder.setRandomThroughput(0.0);
		resBuilder.setStreamingThroughput(10.0);
		resBuilder.setType(Scheduler.reservationType.STREAMING_RESERVATION);
		Scheduler.volumeIdentifier.Builder volBuilder = Scheduler.volumeIdentifier
				.newBuilder();
		volBuilder.setUuid("asdf");
		Scheduler.volumeIdentifier volume = volBuilder.build();
		resBuilder.setVolume(volume);
		Scheduler.reservation res = resBuilder.build();

		// Test reservation to be scheduled to at least one osd
		RPCResponse<Scheduler.osdSet> response = client.scheduleReservation(
				null, RPCAuthentication.authNone,
				RPCAuthentication.userService, res);
		Scheduler.osdSet osds = response.get();
		int numOSDs = osds.getOsdCount();
		assertTrue(numOSDs > 0);
		response.freeBuffers();

		// Test getting the correct schedule
		response = client.getSchedule(null, RPCAuthentication.authNone,
				RPCAuthentication.userService, volume);
		osds = response.get();
		assertTrue(numOSDs == osds.getOsdCount());
		assertTrue(osds.getOsd(0).getUuid().length() > 0);
		response.freeBuffers();

        // Try to schedule random io reservation to the same OSD
        Scheduler.reservation.Builder resBuilder2 = Scheduler.reservation
                .newBuilder();
        resBuilder2.setCapacity(10.0);
        resBuilder2.setRandomThroughput(10.0);
        resBuilder2.setStreamingThroughput(0.0);
        resBuilder2.setType(Scheduler.reservationType.RANDOM_IO_RESERVATION);
        Scheduler.volumeIdentifier.Builder volBuilder2 = Scheduler.volumeIdentifier
                .newBuilder();
        volBuilder2.setUuid("jkl");
        resBuilder2.setVolume(volBuilder2.build());
        RPCResponse<Scheduler.osdSet> response2 = client.scheduleReservation(
                null, RPCAuthentication.authNone,
                RPCAuthentication.userService, resBuilder2.build());
        try {
            response2.get();
            fail();
        }
        catch(Exception e) {}

		client.removeReservation(null, RPCAuthentication.authNone,
				RPCAuthentication.userService, volume);
		response = client.getSchedule(null, RPCAuthentication.authNone,
				RPCAuthentication.userService, volume);
        try {
		    osds = response.get();
            assertTrue(false);
        } catch(PBRPCException ex) {
            // Exception as reservation does not exist
            assertTrue(true);
        }
		response.freeBuffers();
	}
	
	@Test
	public void testGetAllVolumes() throws Exception {
		// Create reservation
		Scheduler.reservation.Builder resBuilder = Scheduler.reservation
				.newBuilder();
		double capacity = 100.0;
		double randomTP = 0.0;
		double seqTP = 10.0;
		String uuid = "asdf";
		resBuilder.setCapacity(capacity);
		resBuilder.setRandomThroughput(randomTP);
		resBuilder.setStreamingThroughput(seqTP);
		resBuilder.setType(Scheduler.reservationType.STREAMING_RESERVATION);
		Scheduler.volumeIdentifier.Builder volBuilder = Scheduler.volumeIdentifier
				.newBuilder();
		volBuilder.setUuid(uuid);
		Scheduler.volumeIdentifier volume = volBuilder.build();
		resBuilder.setVolume(volume);
		Scheduler.reservation res = resBuilder.build();
		
		// Schedule reservation
		RPCResponse<Scheduler.osdSet> response = client.scheduleReservation(
				null, RPCAuthentication.authNone,
				RPCAuthentication.userService, res);
		Scheduler.osdSet osds = response.get();
		int numOSDs = osds.getOsdCount();
		assertTrue(numOSDs > 0);
		response.freeBuffers();
		
		// Get all volumes
		RPCResponse<Scheduler.reservationSet> reservationsResponse = client.getAllVolumes(null, RPCAuthentication.authNone, RPCAuthentication.userService);
		Scheduler.reservationSet reservations = reservationsResponse.get();
		assertEquals(reservations.getReservationsCount(), 1);
		Scheduler.reservation r = reservations.getReservations(0);
		assertTrue(r.getCapacity() == capacity);
		assertTrue(r.getRandomThroughput() == randomTP);
		assertTrue(r.getStreamingThroughput() == seqTP);
		assertTrue(r.getVolume().getUuid().equals(uuid));
        assertTrue(r.getSchedule().getOsdCount() > 0);
		reservationsResponse.freeBuffers();
	}

    /*@Test
    public void testGetVolumes() throws Exception {
        // Create reservation
        Scheduler.reservation.Builder resBuilder = Scheduler.reservation
                .newBuilder();
        double capacity = 100.0;
        double randomTP = 0.0;
        double seqTP = 10.0;
        String uuid = "asdf";
        resBuilder.setCapacity(capacity);
        resBuilder.setRandomThroughput(randomTP);
        resBuilder.setStreamingThroughput(seqTP);
        resBuilder.setType(Scheduler.reservationType.STREAMING_RESERVATION);
        Scheduler.volumeIdentifier.Builder volBuilder = Scheduler.volumeIdentifier
                .newBuilder();
        volBuilder.setUuid(uuid);
        Scheduler.volumeIdentifier volume = volBuilder.build();
        resBuilder.setVolume(volume);
        Scheduler.reservation res = resBuilder.build();

        // Schedule reservation
        RPCResponse<Scheduler.osdSet> response = client.scheduleReservation(
                null, RPCAuthentication.authNone,
                RPCAuthentication.userService, res);
        Scheduler.osdSet osds = response.get();
        int numOSDs = osds.getOsdCount();
        assertTrue(numOSDs > 0);
        response.freeBuffers();

        DIR.ServiceSet availableOSDs = testEnv.getDirClient().xtreemfs_service_get_by_type( null,
                RPCAuthentication.authNone, RPCAuthentication.userService, DIR.ServiceType.SERVICE_TYPE_OSD).get();

        boolean found = false;

        for(DIR.Service osd: availableOSDs.getServicesList()) {
            Scheduler.volumeSet volumes = client.getVolumes(null, RPCAuthentication.authNone,
                    RPCAuthentication.userService, osd.getUuid()).get();
            for(Scheduler.volumeIdentifier v: volumes.getVolumesList()) {
                if(v.getUuid().equals(uuid)){
                    found = true;
                    break;
                }
            }
        }

        assertTrue(found);
    }*/

    @Test
    public void testGetFreeResources() throws Exception {
        // Wait until OSDMonitor discovered OSDs
        Thread.sleep(1000);

        // Get initially free resources
        RPCResponse<Scheduler.freeResourcesResponse> response = client.getFreeResources(
                null, RPCAuthentication.authNone, RPCAuthentication.userService, Common.emptyRequest.getDefaultInstance());

        Scheduler.freeResourcesResponse freeRes = response.get();

        double initialCapacity = freeRes.getStreamingCapacity();
        double initialRandTp = freeRes.getRandomThroughput();
        double initialSeqTp = freeRes.getStreamingThroughput();
        response.freeBuffers();

        assertTrue(initialCapacity > 0.0);
        assertTrue(initialRandTp > 0.0 || initialSeqTp > 0.0);


        // Schedule reservation
        Scheduler.reservation.Builder resBuilder = Scheduler.reservation
                .newBuilder();
        double capacity = 100.0;
        double randomTP = 0.0;
        double seqTP = 10.0;
        String uuid = "asdf";
        resBuilder.setCapacity(capacity);
        resBuilder.setRandomThroughput(randomTP);
        resBuilder.setStreamingThroughput(seqTP);
        resBuilder.setType(Scheduler.reservationType.STREAMING_RESERVATION);
        Scheduler.volumeIdentifier.Builder volBuilder = Scheduler.volumeIdentifier
                .newBuilder();
        volBuilder.setUuid(uuid);
        Scheduler.volumeIdentifier volume = volBuilder.build();
        resBuilder.setVolume(volume);
        Scheduler.reservation res = resBuilder.build();

        RPCResponse<Scheduler.osdSet> resResponse = client.scheduleReservation(
                null, RPCAuthentication.authNone,
                RPCAuthentication.userService, res);


        // Check free resources after scheduling reservation
        RPCResponse<Scheduler.freeResourcesResponse> newResponse = client.getFreeResources(
                null, RPCAuthentication.authNone, RPCAuthentication.userService, Common.emptyRequest.getDefaultInstance());

        Scheduler.freeResourcesResponse newFreeRes = newResponse.get();

        double newCapacity = newFreeRes.getStreamingCapacity();
        double newRandomTP = newFreeRes.getRandomThroughput();
        double newSeqTP = newFreeRes.getStreamingThroughput();

        assertTrue(initialCapacity > newCapacity);
        // remaining random throughput resources are smaller than initial, caused by OSD labeling
        assertTrue(initialRandTp >= newRandomTP);
        assertTrue(initialSeqTp > newSeqTP);

        // schedule reservation requiring all available resources to verify that reported resources
        // are actually available
        resBuilder = Scheduler.reservation.newBuilder();
        resBuilder.setCapacity(newCapacity);
        resBuilder.setRandomThroughput(randomTP);
        resBuilder.setStreamingThroughput(newSeqTP);
        resBuilder.setType(Scheduler.reservationType.STREAMING_RESERVATION);
        volBuilder = Scheduler.volumeIdentifier
                .newBuilder();
        volBuilder.setUuid(uuid + "-1");
        volume = volBuilder.build();
        resResponse = client.scheduleReservation(
                null, RPCAuthentication.authNone,
                RPCAuthentication.userService, res);
        assertTrue(resResponse.get().getOsdCount() > 0);

        resResponse.freeBuffers();
        newResponse.freeBuffers();
    }
}
