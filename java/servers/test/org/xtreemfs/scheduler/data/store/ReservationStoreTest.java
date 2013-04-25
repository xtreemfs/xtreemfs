package org.xtreemfs.scheduler.data.store;

import org.junit.Test;
import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.api.BabuDB;
import org.xtreemfs.babudb.api.DatabaseManager;
import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.scheduler.data.Reservation;
import org.xtreemfs.scheduler.data.Reservation.ReservationType;
import org.xtreemfs.test.SetupUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReservationStoreTest {
	private ReservationStore store;
	BabuDBConfig dbsConfig;
	private BabuDB database;
	final String DB_NAME = "schedtest";
	
	@Test
	public void testStoreReservation() throws Exception {
		// Setting up database and ReservationStore
		this.dbsConfig = SetupUtils.createSchedulerdbsConfig();
		database = BabuDBFactory.createBabuDB(dbsConfig);
		DatabaseManager dbm = database.getDatabaseManager();
		Database db;
		try {
			db = dbm.createDatabase(DB_NAME, 2);
		} catch (BabuDBException ex) {
			db = dbm.getDatabase(DB_NAME);
		}
		store = new ReservationStore(db, 0);
		
		// Test writing and reading reservations
		Reservation r1 = new Reservation("testVolume", ReservationType.STREAMING_RESERVATION, 0.0, 100.0, 100.0);
		store.storeReservation(r1);
		assertEquals(store.getReservations().size(), 1);
		assertTrue(store.getReservations().contains(r1));
		
		// Test restoring reservations from database
		store = null;
		store = new ReservationStore(db, 0);
		
		assertEquals(store.getReservations().size(), 1);
		assertTrue(store.getReservations().contains(r1));
		
		// Test deleting reservations
		store.removeReservation(r1);
		assertEquals(store.getReservations().size(), 0);
		
		// Cleanup
		database.getDatabaseManager().deleteDatabase(DB_NAME);
		database.shutdown();
	}
}
