package org.xtreemfs.scheduler.data.store;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.DatabaseRequestResult;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.scheduler.data.Reservation;

import java.util.ArrayList;
import java.util.List;

public class ReservationStore {
	private Database database;
	private final int index;

	public ReservationStore(Database database, final int index) {
		this.database = database;
		this.index = index;
	}

	public List<Reservation> getReservations() {
        List<Reservation> result = new ArrayList<Reservation>();

        try {
            DatabaseRequestResult<ResultSet<byte[],byte[]>> dbResult = database.prefixLookup(index, "".getBytes(), null);
            ResultSet<byte[], byte[]> resultSet = dbResult.get();
            while(resultSet.hasNext()) {
                Reservation r = new Reservation(resultSet.next().getValue());
                result.add(r);
            }
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }

        return result;
	}

	public void storeReservation(Reservation reservation) {
        try {
            DatabaseRequestResult<Object> result = this.database.singleInsert(index, reservation.getVolumeIdentifier()
                    .getBytes(), reservation.getBytes(), null);
            result.get();
        } catch(BabuDBException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
	}

	public void removeReservation(Reservation reservation) {
        try {
            DatabaseRequestResult<Object> result = this.database.singleInsert(index, reservation.getVolumeIdentifier()
                    .getBytes(), null, null);
            result.get();
        } catch(BabuDBException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
	}

	public Reservation getReservation(String volumeUuid) throws Exception {
        Reservation result;
        try {
            DatabaseRequestResult<byte[]> dbResult = this.database.lookup(index, volumeUuid.getBytes(), null);
            result = new Reservation(dbResult.get());
        } catch(BabuDBException ex) {
			throw new Exception("Unknown volume");
        }
		return result;
	}
}
