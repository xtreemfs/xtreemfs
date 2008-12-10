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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd.ops;

import java.util.List;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;
import org.xtreemfs.osd.RequestDispatcher.Stages;
import org.xtreemfs.osd.stages.AuthenticationStage;
import org.xtreemfs.osd.stages.ParserStage;
import org.xtreemfs.osd.stages.StageCallbackInterface;
import org.xtreemfs.osd.stages.StorageThread;
import org.xtreemfs.osd.stages.Stage.StageResponseCode;

public class TruncateLocalRPC extends Operation {

    public TruncateLocalRPC(RequestDispatcher master) {
        super(master);
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
            final String fileId = (String) arguments.get(0);
            final long newFileSize = (Long) arguments.get(1);

            if (!ParserStage.validateFileId(fileId)) {
                return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_FILEID,
                    "fileId in JSON body contains invalid characters");
            }

            rq.getDetails().setFileId(fileId);
            rq.getDetails().setTruncateFileSize(newFileSize);
            return null;
        } catch (Exception ex) {
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_RPC,
                "body is not well-formatted or does not contain valid arguments", ex);
        }
    }

    @Override
    public void startRequest(OSDRequest rq) {

        // use anoymous inner classes impl
        master.getStage(Stages.AUTH).enqueueOperation(rq,
            AuthenticationStage.STAGEOP_AUTHENTICATE | AuthenticationStage.STAGEOP_OFT_OPEN,
            new StageCallbackInterface() {

                public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                    postAuthenticate(request, result);
                }
            });
    }

    protected void postAuthenticate(OSDRequest rq, StageResponseCode result) {

        if (result == StageResponseCode.OK) {

            master.getStage(Stages.STORAGE).enqueueOperation(rq,
                StorageThread.STAGEOP_TRUNCATE_LOCAL, new StageCallbackInterface() {

                    public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                        postTruncate(request, result);
                    }
                });

        } else {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "authentication failed for "
                    + rq.getRequestId());
            master.requestFinished(rq);
        }
    }

    private void postTruncate(OSDRequest request, StageResponseCode result) {
        master.requestFinished(request);
    }

}
