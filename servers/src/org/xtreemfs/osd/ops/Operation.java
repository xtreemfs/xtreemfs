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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd.ops;

import java.util.List;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;


public abstract class Operation {


    protected RequestDispatcher master;


    public Operation(RequestDispatcher master) {
        this.master = master;
    }

    /**
     * called after request was parsed and operation assigned.
     * @param rq the new request
     */
    public abstract void startRequest(OSDRequest rq);

    /**
     * Parses and inspects the JSON RPC arguments.
     * @param rq the request
     * @param arguments the JSON RPC arguments
     * @return null if successful, error message otherwise
     */
    public ErrorRecord parseRPCBody(OSDRequest rq, List<Object> arguments) {
        return null;
    }

    protected void sendInternalError(OSDRequest rq, String message) {
        Logging.logMessage(Logging.LEVEL_ERROR,this,message+" / request: "+rq);
        rq.setError(new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, message));
        master.requestFinished(rq);
    }

    protected void finishRequest(OSDRequest rq) {
        master.requestFinished(rq);
    }



}
