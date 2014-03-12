package org.xtreemfs.scheduler.operations;

import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
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
            String volume = rq.getRequestMessage().getUnknownFields().toString().split("\"")[1];
            // TODO(ckleineweber): Fix request parsing (rq contains emptyResponse)
			//Scheduler.volumeIdentifier request = (Scheduler.volumeIdentifier) rq
			//		.getRequestMessage();
			//String volume = request.getUuid();
			master.getReservationScheduler().removeReservation(volume);
			master.getStore().removeReservation(master.getStore().getReservation(volume));
		} catch(Exception ex) {
            rq.sendError(RPC.ErrorType.INTERNAL_SERVER_ERROR, RPC.POSIXErrno.POSIX_ERROR_NONE,
                    "Cannot remove reservation: " + ex.getMessage());
        }
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
