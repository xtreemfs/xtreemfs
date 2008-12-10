/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.osd.ops;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.RequestDispatcher.Stages;
import org.xtreemfs.osd.stages.AuthenticationStage;
import org.xtreemfs.osd.stages.DeletionStage;
import org.xtreemfs.osd.stages.StageCallbackInterface;
import org.xtreemfs.osd.stages.Stage.StageResponseCode;

public final class DeleteOperation extends Operation {

    public DeleteOperation(RequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(OSDRequest rq) {

        // use anoymous inner classes impl
        master.getStage(Stages.AUTH).enqueueOperation(rq,
            AuthenticationStage.STAGEOP_AUTHENTICATE,
            new StageCallbackInterface() {

                public void methodExecutionCompleted(OSDRequest request,
                    StageResponseCode result) {
                    postAuthenticate(request, result);
                }
            });
    }

    /**
     * called after the authentication stage has processed the request
     *
     * @param rq
     *            the request
     * @param result
     *            authentication stage result
     * @param error
     *            error details or null
     */
    protected void postAuthenticate(OSDRequest rq, StageResponseCode result) {

        if (result == StageResponseCode.OK) {

            master.getStage(Stages.DELETION).enqueueOperation(rq,
                DeletionStage.STAGEOP_DELETE_FILE,
                new StageCallbackInterface() {

                    public void methodExecutionCompleted(OSDRequest request,
                        StageResponseCode result) {
                        postDelete(request, result);
                    }
                });

        } else {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                    "authentication failed for " + rq.getRequestId());
            master.requestFinished(rq);
        }
    }

    private void postDelete(OSDRequest request, StageResponseCode result) {
        master.requestFinished(request);
    }

}