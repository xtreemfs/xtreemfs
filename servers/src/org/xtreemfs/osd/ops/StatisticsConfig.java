/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.ops;

import java.util.List;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher.Stages;
import org.xtreemfs.osd.stages.Stage.StageResponseCode;
import org.xtreemfs.osd.stages.StageCallbackInterface;
import org.xtreemfs.osd.stages.StatisticsStage;

/**
 *
 * @author bjko
 */
public class StatisticsConfig extends Operation {

    public StatisticsConfig(OSDRequestDispatcher master) {
        super(master);
    }
    

    public void startRequest(OSDRequest rq) {
        master.getStage(Stages.STATS).enqueueOperation(rq, StatisticsStage.STAGEOP_MEASURE_RQT, new StageCallbackInterface() {

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
            if (arguments.size() > 0) {
                final Boolean[] settings = new Boolean[2];
                settings[0] = (Boolean) arguments.get(0);
                settings[1] = (Boolean) arguments.get(1);
                rq.setAttachment(settings);
            }
            return null;
        } catch (Exception ex) {
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_RPC,
                "body is not well-formatted or does not contain valid arguments", ex);
        }
    }
    
    

}
