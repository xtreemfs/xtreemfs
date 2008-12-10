/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.ops;

import java.util.List;
import org.xtreemfs.common.ClientLease;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher.Stages;
import org.xtreemfs.osd.stages.AuthenticationStage;
import org.xtreemfs.osd.stages.Stage.StageResponseCode;
import org.xtreemfs.osd.stages.StageCallbackInterface;

/**
 *
 * @author bjko
 */
public class AcquireLease extends Operation {

    public AcquireLease(OSDRequestDispatcher master) {
        super(master);
    }
    

    public void startRequest(OSDRequest rq) {
        master.getStage(Stages.AUTH).enqueueOperation(rq, AuthenticationStage.STAGEOP_AUTHENTICATE | AuthenticationStage.STAGEOP_OFT_OPEN | AuthenticationStage.STAGEOP_ACQUIRE_LEASE, new StageCallbackInterface() {

            public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                postAuthenticate(request);
            }
        });
    }
    
    public void postAuthenticate(OSDRequest rq) {
        
        master.getStage(Stages.AUTH).enqueueOperation(rq,  AuthenticationStage.STAGEOP_ACQUIRE_LEASE, new StageCallbackInterface() {

            public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                master.requestFinished(request);
            }
        });
        
    }
    
    /**
     * Parses and inspects the JSON RPC arguments.
     *
     * @param rq
     *            the request
     * @param arguments
     *            the JSON RPC arguments
     * @return null if successful, error message otherwise
     */
    @Override
    public ErrorRecord parseRPCBody(OSDRequest rq, List<Object> arguments) {
        try {
            final ClientLease l = ClientLease.parseFromList(arguments);
            rq.getDetails().setLease(l);
            
            return null;
        } catch (Exception ex) {
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_RPC,
                "body is not well-formatted or does not contain valid arguments", ex);
        }
    }
    
    

}
