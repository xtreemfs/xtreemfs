package org.xtreemfs.scheduler.operations;

import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceConstants;
import org.xtreemfs.scheduler.SchedulerRequest;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.Reservation;
import org.xtreemfs.scheduler.exceptions.SchedulerException;

import com.google.protobuf.Message;

public class ScheduleReservationOperation extends SchedulerOperation {

	public ScheduleReservationOperation(SchedulerRequestDispatcher master)
			throws BabuDBException {
		super(master);
	}

	@Override
	public int getProcedureId() {
		return SchedulerServiceConstants.PROC_ID_SCHEDULERESERVATION;
	}

	@Override
	public void startRequest(SchedulerRequest rq) {
        try {
            master.reloadOSDs();
        } catch(Exception ex) {
            rq.sendError(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "Cannot request OSDs from DIR");
        }
		Scheduler.reservation request = (Scheduler.reservation) rq
				.getRequestMessage();

		Reservation r = new Reservation(request.getVolume().getUuid(),
				convertType(request.getType()), request.getRandomThroughput(),
				request.getStreamingThroughput(), request.getCapacity());

		if(!isValidReservation(r)) {
			rq.sendError(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "Invalid reservation");
		}
		
		try {
			List<OSDDescription> osds = master.getReservationScheduler()
					.scheduleReservation(r);
			List<String> schedule = new ArrayList<String>();
			Scheduler.osdSet.Builder osdSetBuilder = Scheduler.osdSet.newBuilder();
			
			for(OSDDescription osd: osds) {
				schedule.add(osd.getIdentifier());
				
				Scheduler.osdIdentifier.Builder osdBuilder = Scheduler.osdIdentifier.newBuilder();
				osdBuilder.setUuid(osd.getIdentifier());
				osdSetBuilder.addOsd(osdBuilder.build());
			}
			
			r.setSchedule(schedule);
			master.getStore().storeReservation(r);
			rq.sendSuccess(osdSetBuilder.build());
		} catch (SchedulerException ex) {
			rq.sendError(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "Cannot schedule reservation");
		}
	}

	@Override
	public boolean isAuthRequired() {
		return false;
	}

	@Override
	protected Message getRequestMessagePrototype() {
		return Scheduler.reservation.getDefaultInstance();
	}

	@Override
	void requestFinished(Object result, SchedulerRequest rq) {
		rq.sendSuccess((Scheduler.osdSet) result);
	}

	private static Reservation.ReservationType convertType(
			Scheduler.reservationType type) {
		switch (Scheduler.reservationType.valueOf(type.getNumber())) {
		case STREAMING_RESERVATION:
			return Reservation.ReservationType.STREAMING_RESERVATION;
		case RANDOM_IO_RESERVATION:
			return Reservation.ReservationType.RANDOM_IO_RESERVATION;
		case COLD_STORAGE_RESERVATION:
			return Reservation.ReservationType.COLD_STORAGE_RESERVATION;
		case BEST_EFFORT_RESERVATION:
			return Reservation.ReservationType.BEST_EFFORT_RESERVATION;
		default:
			return Reservation.ReservationType.BEST_EFFORT_RESERVATION;
		}
	}
	
	private static boolean isValidReservation(Reservation r) {
		// Cannot create volume of size 0
		if(r.getCapacity() == 0.0)
			return false;
		
		// Either sequential or random throughput guarantees are allowed
		if(r.getRamdomThroughput() != 0.0 && r.getStreamingThroughput() != 0.0)
			return false;
		
		// Check for valid random throughput reservation
		if(r.getType() == Reservation.ReservationType.RANDOM_IO_RESERVATION &&
				r.getRamdomThroughput() == 0.0)
			return false;
		
		// Check for valid sequential throughput reservation
		if(r.getType() == Reservation.ReservationType.STREAMING_RESERVATION &&
				r.getStreamingThroughput() == 0.0)
			return false;
		
		// Check for valid best-effort or cold-storage reservation
		if((r.getType() == Reservation.ReservationType.COLD_STORAGE_RESERVATION || 
				r.getType() == Reservation.ReservationType.BEST_EFFORT_RESERVATION) &&
				(r.getRamdomThroughput() != 0.0 || r.getStreamingThroughput() != 0.0))
			return false;
		
		return true;
	}
}
