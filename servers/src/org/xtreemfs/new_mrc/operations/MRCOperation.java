/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_mrc.operations;

import java.util.List;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;

/**
 *
 * @author bjko
 */
public abstract class MRCOperation {

    protected final MRCRequestDispatcher master;


    public MRCOperation(MRCRequestDispatcher master) {
        this.master = master;
    }

    /**
     * called after request was parsed and operation assigned.
     * @param rq the new request
     */
    public abstract void startRequest(MRCRequest rq);

    /**
     * Method to check if operation needs to parse arguments.
     * @return true, if the operation needs arguments
     */
    public boolean hasArguments() {
        return false;
    }
    
    /**
     * Parses and inspects the JSON RPC arguments.
     * @param rq the request
     * @param arguments the JSON RPC arguments
     * @return null if successful, error message otherwise
     */
    public ErrorRecord parseRPCBody(MRCRequest rq, List<Object> arguments) {
        return null;
    }

    protected void sendInternalError(MRCRequest rq, String message) {
        Logging.logMessage(Logging.LEVEL_ERROR,this,message+" / request: "+rq);
        rq.setError(new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, message));
        master.requestFinished(rq);
    }

    protected void finishRequest(MRCRequest rq) {
        master.requestFinished(rq);
    }
    
}
