package org.xtreemfs.scheduler.operations;

import java.io.IOException;

import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.scheduler.SchedulerRequest;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;

import com.google.protobuf.Message;

public abstract class SchedulerOperation {

    protected final SchedulerRequestDispatcher master;

    public SchedulerOperation(SchedulerRequestDispatcher master) throws BabuDBException {
        this.master = master;
    }

    public abstract int getProcedureId();

    /**
     * called after request was parsed and operation assigned.
     * 
     * @param rq
     *            the new request
     */
    public abstract void startRequest(SchedulerRequest rq);

    /**
     * Method to check if operation needs user authentication.
     * 
     * @return true, if the user needs to be authenticated
     */
    public abstract boolean isAuthRequired();

    protected abstract Message getRequestMessagePrototype();

    /**
     * parses the RPC request message. Can throw any exception which
     * will result in an error message telling the client that the
     * request message data is garbage.
     * @param rq
     * @throws java.lang.Exception
     */
    public void parseRPCMessage(SchedulerRequest rq) throws IOException {
        rq.deserializeMessage(getRequestMessagePrototype());
    }

    void requestFailed(BabuDBException error, SchedulerRequest rq) {
        assert (error != null);
        rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, error.toString());
    }

    /**
     * Method-interface for sending a response 
     * 
     * @param result - can be null, if not necessary.
     * @param rq - original {@link SchedulerRequest}.
     */
    abstract void requestFinished(Object result, SchedulerRequest rq);
}
