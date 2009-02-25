/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
