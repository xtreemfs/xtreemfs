package org.xtreemfs.scheduler.operations;

import java.util.Collection;

import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceConstants;
import org.xtreemfs.scheduler.SchedulerRequest;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.scheduler.data.Reservation;

import com.google.protobuf.Message;

public class GetVolumesOperation extends SchedulerOperation {

	public GetVolumesOperation(SchedulerRequestDispatcher master)
			throws BabuDBException {
		super(master);
	}

	@Override
	public int getProcedureId() {
		return SchedulerServiceConstants.PROC_ID_GETVOLUMES;
	}

	@Override
	public void startRequest(SchedulerRequest rq) {
		Scheduler.osdIdentifier request = (Scheduler.osdIdentifier) rq
				.getRequestMessage();
		Scheduler.volumeSet.Builder volumeSetBuilder = Scheduler.volumeSet.newBuilder();
		
		Collection<Reservation> reservations = master.getStore().getReservations();
		for(Reservation reservation: reservations) {
			for(String volume: reservation.getSchedule()) {
				if(volume.equals(request.getUuid())) {
					Scheduler.volumeIdentifier.Builder volumeIdentifierBuilder = Scheduler.volumeIdentifier.newBuilder();
					volumeIdentifierBuilder.setUuid(volume);
					volumeSetBuilder.addVolumes(volumeIdentifierBuilder.build());
				}
			}
		}
		rq.sendSuccess(volumeSetBuilder.build());
	}

	@Override
	public boolean isAuthRequired() {
		return false;
	}

	@Override
	protected Message getRequestMessagePrototype() {
		return Scheduler.volumeSet.getDefaultInstance();
	}

	@Override
	void requestFinished(Object result, SchedulerRequest rq) {
		rq.sendSuccess((Scheduler.volumeSet) result);
	}
}
