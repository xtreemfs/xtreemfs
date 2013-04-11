package org.xtreemfs.scheduler.operations;

import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceConstants;
import org.xtreemfs.scheduler.SchedulerRequest;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;

import com.google.protobuf.Message;

public class RemoveReservationOperation extends SchedulerOperation {

	public RemoveReservationOperation(SchedulerRequestDispatcher master) throws BabuDBException {
		super(master);
	}
	
	@Override
	public int getProcedureId() {
		return SchedulerServiceConstants.PROC_ID_REMOVERESERVATION;
	}

	@Override
	public void startRequest(SchedulerRequest rq) {
		try {
			Scheduler.volumeIdentifier request = (Scheduler.volumeIdentifier) rq
					.getRequestMessage();
			String volume = request.getUuid();
			master.getReservationScheduler().removeReservation(volume);
			master.getStore().removeReservation(master.getStore().getReservation(volume));
		} catch(Exception ex) {}
		rq.sendSuccess(emptyResponse.getDefaultInstance());
	}

	@Override
	public boolean isAuthRequired() {
		return false;
	}

	@Override
	protected Message getRequestMessagePrototype() {
		return emptyResponse.getDefaultInstance();
	}

	@Override
	void requestFinished(Object result, SchedulerRequest rq) {
		rq.sendSuccess((emptyResponse) result);
	}
}
