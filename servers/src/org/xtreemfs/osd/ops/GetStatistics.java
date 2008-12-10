/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.ops;

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
public final class GetStatistics extends Operation { 
    
    public GetStatistics(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(OSDRequest rq) {
        master.getStage(Stages.STATS).enqueueOperation(rq, StatisticsStage.STAGEOP_STATISTICS, new StageCallbackInterface() {

            public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                master.requestFinished(request);
            }
        });
    }
}
