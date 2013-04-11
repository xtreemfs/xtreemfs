package org.xtreemfs.scheduler.data;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ReservationTest {
	
	@Test
	public void testGetBytes() throws Exception {
		Reservation r1 = new Reservation("testVolume", Reservation.ReservationType.BEST_EFFORT_RESERVATION, 0.0, 0.0, 100.0);
		Reservation r2 = new Reservation(r1.getBytes());
		assertTrue(r1.equals(r2));
		assertTrue(r1.getBytes().length > 0);
	}
}
