package org.xtreemfs.scheduler.operations;

import java.util.Iterator;

import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceConstants;
import org.xtreemfs.scheduler.SchedulerRequest;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.scheduler.data.Reservation;
import org.xtreemfs.scheduler.utils.SchedulerUtils;

import com.google.protobuf.Message;

public class GetAllVolumesOperation extends SchedulerOperation {

	public GetAllVolumesOperation(SchedulerRequestDispatcher master)
			throws BabuDBException {
		super(master);
	}
	
	@Override
	public int getProcedureId() {
		return SchedulerServiceConstants.PROC_ID_GETALLVOLUMES;
	}

	@Override
	public void startRequest(SchedulerRequest rq) {
		Iterator<Reservation> it = master.getStore().getReservations().iterator();
		Scheduler.reservationSet.Builder resultBuilder = Scheduler.reservationSet.newBuilder();
		
		while(it.hasNext()) {
			Scheduler.reservation.Builder reservationBuilder = Scheduler.reservation.newBuilder();
			Reservation r = it.next();
			reservationBuilder.setCapacity(r.getCapacity());
			reservationBuilder.setRandomThroughput(r.getRamdomThroughput());
			reservationBuilder.setStreamingThroughput(r.getStreamingThroughput());
			Scheduler.volumeIdentifier.Builder volBuilder = Scheduler.volumeIdentifier.newBuilder();
			volBuilder.setUuid(r.getVolumeIdentifier());
			reservationBuilder.setVolume(volBuilder.build());
			reservationBuilder.setType(SchedulerUtils.convertReservationTypeFromPB(r.getType()));
			
			resultBuilder.addReservations(reservationBuilder.build());
		}
		
		rq.sendSuccess(resultBuilder.build());
	}

	@Override
	public boolean isAuthRequired() {
		return false;
	}

	@Override
	protected Message getRequestMessagePrototype() {
		return Scheduler.reservationSet.getDefaultInstance();
	}

	@Override
	void requestFinished(Object result, SchedulerRequest rq) {
		rq.sendSuccess((Scheduler.reservationSet) result);
	}

}
