/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_dir.operations;

import org.xtreemfs.interfaces.DIRInterface.getGlobalTimeRequest;
import org.xtreemfs.interfaces.DIRInterface.getGlobalTimeResponse;
import org.xtreemfs.new_dir.DIRRequest;
import org.xtreemfs.new_dir.DIRRequestDispatcher;

/**
 *
 * @author bjko
 */
public class GetGlobalTimeOperation extends DIROperation {

    private final int operationNumber;

    public GetGlobalTimeOperation(DIRRequestDispatcher master) {
        super(master);
        getGlobalTimeRequest tmp = new getGlobalTimeRequest();
        operationNumber = tmp.getOperationNumber();
    }

    @Override
    public int getProcedureId() {
        return operationNumber;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        getGlobalTimeResponse gtr = new getGlobalTimeResponse();
        gtr.setReturnValue(System.currentTimeMillis()/1000);
        rq.sendSuccess(gtr);
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        getGlobalTimeRequest gtr = new getGlobalTimeRequest();
        rq.deserializeMessage(gtr);
    }

}
