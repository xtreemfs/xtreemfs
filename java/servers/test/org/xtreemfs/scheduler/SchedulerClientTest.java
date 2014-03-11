package org.xtreemfs.scheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SchedulerClientTest {
	TestEnvironment testEnv;
	SchedulerConfig config;
	BabuDBConfig dbsConfig;
	SchedulerRequestDispatcher scheduler;
	SchedulerServiceClient serviceClient;
	SchedulerClient client;

	public SchedulerClientTest() throws Exception {
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
				TestEnvironment.Services.SCHEDULER_CLIENT });
		testEnv.start();
		serviceClient = testEnv.getSchedulerClient();
		scheduler = new SchedulerRequestDispatcher(config, dbsConfig);
		scheduler.startup();
		scheduler.waitForStartup();
		client = new SchedulerClient(serviceClient,
				config.getSchedulerService(), config.getFailoverMaxRetries(),
				config.getFailoverWait());
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
		resBuilder.setCapacity(100.0);
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
		Scheduler.osdSet osds = client.scheduleReservation(
				null, RPCAuthentication.authNone,
				RPCAuthentication.userService, res);

		int numOSDs = osds.getOsdCount();
		assertTrue(numOSDs > 0);

		// Test getting the correct schedule
		osds = client.getSchedule(null, RPCAuthentication.authNone,
				RPCAuthentication.userService, volume);
		assertTrue(numOSDs == osds.getOsdCount());

		// Test getting an empty schedule after removing the reservation
		// TODO(ckleineweber): Scheduler receives emptyResponse instead of volumeIdentifier
		client.removeReservation(null, RPCAuthentication.authNone,
				RPCAuthentication.userService, volume);
		osds = client.getSchedule(null, RPCAuthentication.authNone,
				RPCAuthentication.userService, volume);
		assertTrue(osds.getOsdCount() == 0);
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
		Scheduler.osdSet osds = client.scheduleReservation(
				null, RPCAuthentication.authNone,
				RPCAuthentication.userService, res);
		int numOSDs = osds.getOsdCount();
		assertTrue(numOSDs > 0);
		
		// Get all volumes
		Scheduler.reservationSet reservations = client.getAllVolumes(null, RPCAuthentication.authNone, RPCAuthentication.userService);
		assertEquals(reservations.getReservationsCount(), 1);
		Scheduler.reservation r = reservations.getReservations(0);
		assertEquals(r.getCapacity(), capacity);
		assertEquals(r.getRandomThroughput(), randomTP);
		assertEquals(r.getStreamingThroughput(), seqTP);
		assertTrue(r.getVolume().getUuid().equals(uuid));
	}
}