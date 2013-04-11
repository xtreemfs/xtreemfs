package org.xtreemfs.scheduler.operations;

import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceConstants;
import org.xtreemfs.scheduler.SchedulerRequest;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.scheduler.data.Reservation;

import com.google.protobuf.Message;

public class GetScheduleOperation extends SchedulerOperation {

	public GetScheduleOperation(SchedulerRequestDispatcher master)
			throws BabuDBException {
		super(master);
	}

	@Override
	public int getProcedureId() {
		return SchedulerServiceConstants.PROC_ID_GETSCHEDULE;
	}

	@Override
	public void startRequest(SchedulerRequest rq) {
		Scheduler.volumeIdentifier request = (Scheduler.volumeIdentifier) rq
				.getRequestMessage();
		
		try {
			Reservation r = master.getStore().getReservation(request.getUuid());
			Scheduler.osdSet.Builder osdSetBuilder = Scheduler.osdSet.newBuilder();
			
			for(String osd: r.getSchedule()) {
				Scheduler.osdIdentifier.Builder osdBuilder = Scheduler.osdIdentifier.newBuilder();
				osdBuilder.setUuid(osd);
				osdSetBuilder.addOsd(osdBuilder.build());
			}
			
			rq.sendSuccess(osdSetBuilder.build());
		} catch(Exception ex) {
			rq.sendInternalServerError(ex);
		}
	}

	@Override
	public boolean isAuthRequired() {
		return false;
	}

	@Override
	protected Message getRequestMessagePrototype() {
		return Scheduler.volumeIdentifier.getDefaultInstance();
	}

	@Override
	void requestFinished(Object result, SchedulerRequest rq) {
		rq.sendSuccess((Scheduler.osdSet) result);
	}

}
