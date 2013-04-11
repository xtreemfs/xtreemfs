package org.xtreemfs.scheduler.data.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.DatabaseRequestListener;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.scheduler.data.Reservation;

public class ReservationStore {
	private Database database;
	private Map<String, Reservation> reservations;
	private final int index;
	private boolean restored = false;

	public ReservationStore(Database database, final int index) {
		this.database = database;
		restoreReservations();
		this.index = index;
		this.reservations = new HashMap<String, Reservation>();
	}

	private void restoreReservations() {
		this.database.prefixLookup(index, new byte[0], this).registerListener(
				new LoadReservationsDBListener());
	}

	public Collection<Reservation> getReservations() {
		return reservations.values();
	}

	public void storeReservation(Reservation reservation) {
		this.reservations.put(reservation.getVolumeIdentifier(), reservation);
		this.database.singleInsert(index, reservation.getVolumeIdentifier()
				.getBytes(), reservation.getBytes(), null);
	}

	public void removeReservation(Reservation reservation) {
		this.reservations.remove(reservation.getVolumeIdentifier());
		this.database.singleInsert(index, reservation.getVolumeIdentifier()
				.getBytes(), null, null);
	}

	public Reservation getReservation(String volumeUuid) throws Exception {
		Reservation result = this.reservations.get(volumeUuid);

		if (result == null)
			throw new Exception("Unknown volume");
		else
			return result;
	}
	
	public void waitForRestore(){
		while(!restored) {
			try {
				Thread.sleep(10);
			} catch(InterruptedException ex ) {}
		}
	}

	private class LoadReservationsDBListener implements
			DatabaseRequestListener<ResultSet<byte[], byte[]>> {
		@Override
		public void finished(ResultSet<byte[], byte[]> data, Object context) {
			while (data.hasNext()) {
				try {
					Entry<byte[], byte[]> entry = data.next();
					reservations.put(new String(entry.getKey()),
							new Reservation(entry.getValue()));
				} catch (Exception e) {
					Logging.logMessage(Logging.LEVEL_ERROR, this,
							e.getMessage());
				}
			}
			restored = true;
		}

		@Override
		public void failed(BabuDBException error, Object request) {
			Logging.logMessage(Logging.LEVEL_ERROR, this, error.getMessage());
			restored = true;
		}
	}
}
