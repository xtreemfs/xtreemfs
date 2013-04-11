package org.xtreemfs.scheduler.osd;

import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;
import org.xtreemfs.scheduler.data.Reservation;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OSDTest {
	private OSDDescription osd;
	
	@Before
	public void setUp() throws Exception {
		Map<Integer, Double> streamingThroughput = new HashMap<Integer, Double>();
		
		for(int i = 1; i <= 100; i++) {
			streamingThroughput.put(i, 100.0 - (double) i + 1.0);
		}
		
		OSDPerformanceDescription perf = new OSDPerformanceDescription(100.0, streamingThroughput, 1000.0);
		
		osd = new OSDDescription("testOSD", perf, OSDDescription.OSDType.DISK);
	}
	
	@Test
	public void testHasFreeCapacity() throws Exception {
		Reservation r1 = new Reservation("volume1", Reservation.ReservationType.BEST_EFFORT_RESERVATION, 1000.0, 100.0, 100.0);
		assertFalse(osd.hasFreeCapacity(r1));
		
		Reservation r2 = new Reservation("volume2", Reservation.ReservationType.BEST_EFFORT_RESERVATION, 100.0, 100.0, 100.0);
		assertTrue(osd.hasFreeCapacity(r2));
	}
	
	@Test
	public void testSerialization() throws Exception {
		byte[] bytes = osd.getBytes();
		OSDDescription newOSD = new OSDDescription(bytes);
		assertTrue(osd.getIdentifier().equals(newOSD.getIdentifier()));
		assertTrue(osd.getType() == newOSD.getType());
		assertTrue(osd.getUsage() == newOSD.getUsage());
		assertTrue(osd.getCapabilities().getCapacity() == newOSD.getCapabilities().getCapacity());
		assertTrue(osd.getCapabilities().getIops() == newOSD.getCapabilities().getIops());
		assertTrue(osd.getCapabilities().getStreamingPerformance().size() == newOSD.getCapabilities().getStreamingPerformance().size());
		for(int i = 1; i <= osd.getCapabilities().getStreamingPerformance().size(); i++) {
			assertTrue(osd.getCapabilities().getStreamingPerformance().get(i).doubleValue() == newOSD.getCapabilities().getStreamingPerformance().get(i).doubleValue());
		}
	}
}
