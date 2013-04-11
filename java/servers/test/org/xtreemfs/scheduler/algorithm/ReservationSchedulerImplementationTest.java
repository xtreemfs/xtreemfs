package org.xtreemfs.scheduler.algorithm;

import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;
import org.xtreemfs.scheduler.data.Reservation;
import org.xtreemfs.scheduler.exceptions.SchedulerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ReservationSchedulerImplementationTest {
	private OSDDescription osd1;
	private OSDDescription osd2;
	private ReservationSchedulerImplementation scheduler;
	private List<OSDDescription> osds;
	
    @Before
	public void setUp() throws Exception {
		Map<Integer, Double> streamingThroughput = new HashMap<Integer, Double>();
		
		for(int i = 1; i <= 100; i++) {
			streamingThroughput.put(i, 100.0 - (double) i + 1.0);
		}
		
		OSDPerformanceDescription perf = new OSDPerformanceDescription(100, streamingThroughput, 1000);
		
		osd1 = new OSDDescription("osd1", perf, OSDDescription.OSDType.DISK);
		osd2 = new OSDDescription("osd2", perf, OSDDescription.OSDType.DISK);
		
		osds = new ArrayList<OSDDescription>();
		osds.add(osd1);
		osds.add(osd2);
		
		scheduler = new ReservationSchedulerImplementation(osds, 0.0, 1.0, 1.0, 1.0, true);
	}

    @Test
	public void testScheduleStreamingReservation() throws Exception {
		Reservation reservation1 = new Reservation("volume1", Reservation.ReservationType.STREAMING_RESERVATION, 0.0, 80.0, 100.0);
		Reservation reservation2 = new Reservation("volume2", Reservation.ReservationType.STREAMING_RESERVATION, 0.0, 40.0, 100.0);
		Reservation reservation3 = new Reservation("volume3", Reservation.ReservationType.STREAMING_RESERVATION, 0.0, 100.0, 100.0);
		
		scheduler.reset();
		
		try {
			List<OSDDescription> targets;
			targets = scheduler.scheduleReservation(reservation1);
			assertTrue(targets.size() > 0);
			assertTrue(osd1.getReservations().size() > 0 || osd2.getReservations().size() > 0);
			targets = scheduler.scheduleReservation(reservation2);
			assertTrue(targets.size() > 0);
			assertTrue(osd1.getReservations().size() > 0 && osd2.getReservations().size() > 0);
		}
		catch (SchedulerException e) {
			fail();
		}
		
		try {
			scheduler.scheduleReservation(reservation3);
			fail();
		}
		catch (SchedulerException e) {
			assertTrue(true);
		}
	}
	
    @Test
	public void testGetAngle() throws Exception {
		Reservation reservation1 = new Reservation("volume1", Reservation.ReservationType.BEST_EFFORT_RESERVATION, 1.0, 0.0, 0.0);
		Reservation reservation2 = new Reservation("volume2", Reservation.ReservationType.BEST_EFFORT_RESERVATION, 0.0, 1.0, 0.0);
		Reservation reservation3 = new Reservation("volume3", Reservation.ReservationType.BEST_EFFORT_RESERVATION, 1.0, 1.0, 0.0);
		Map<Integer, Double> streamingThroughput = new HashMap<Integer, Double>();
		streamingThroughput.put(0, 0.0);
		streamingThroughput.put(1, 0.0);
		OSDPerformanceDescription perf = new OSDPerformanceDescription(1.0, streamingThroughput, 0.0);
		OSDDescription osd = new OSDDescription("osd1", perf, OSDDescription.OSDType.DISK);
		assertEquals(scheduler.getAngle(reservation1, osd), 0.0);
		assertFalse(scheduler.getAngle(reservation2, osd2) == 0.0);
		assertTrue(scheduler.getAngle(reservation3, osd) < scheduler.getAngle(reservation2, osd));
	}
}
