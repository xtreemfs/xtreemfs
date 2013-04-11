package org.xtreemfs.scheduler.utils;

import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.scheduler.data.Reservation;

public class SchedulerUtils {
	public static Reservation.ReservationType convertReservationTypeToPB(Scheduler.reservationType type) {
		switch(type) {
		case BEST_EFFORT_RESERVATION:
			return Reservation.ReservationType.BEST_EFFORT_RESERVATION;
		case COLD_STORAGE_RESERVATION:
			return Reservation.ReservationType.COLD_STORAGE_RESERVATION;
		case RANDOM_IO_RESERVATION:
			return Reservation.ReservationType.RANDOM_IO_RESERVATION;
		case STREAMING_RESERVATION:
			return Reservation.ReservationType.STREAMING_RESERVATION;
		default:
			return Reservation.ReservationType.BEST_EFFORT_RESERVATION;
		}
	}
	
	public static Scheduler.reservationType convertReservationTypeFromPB(Reservation.ReservationType type) {
		switch(type) {
		case BEST_EFFORT_RESERVATION:
			return Scheduler.reservationType.BEST_EFFORT_RESERVATION;
		case COLD_STORAGE_RESERVATION:
			return Scheduler.reservationType.COLD_STORAGE_RESERVATION;
		case RANDOM_IO_RESERVATION:
			return Scheduler.reservationType.RANDOM_IO_RESERVATION;
		case STREAMING_RESERVATION:
			return Scheduler.reservationType.STREAMING_RESERVATION;
		default:
			return Scheduler.reservationType.BEST_EFFORT_RESERVATION;
		}
	}
}
