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

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_startRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_startResponse;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;

public final class CleanupStartOperation extends OSDOperation {

    final int procId;

    public CleanupStartOperation(OSDRequestDispatcher master) {
        super(master);
        procId = xtreemfs_cleanup_startRequest.TAG;
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {

        UserCredentials uc = rq.getRPCRequest().getUserCredentials();
        if ((uc == null) || (!uc.getPassword().equals(master.getConfig().getAdminPassword()))) {
            rq.sendOSDException(ErrorCodes.AUTH_FAILED, "this operation requires an admin password");
            return;
        }
        xtreemfs_cleanup_startRequest args = (xtreemfs_cleanup_startRequest)rq.getRequestArgs();
        master.getCleanupThread().cleanupStart(args.getRemove_zombies(),args.getRemove_unavail_volume(),args.getLost_and_found(),uc);
        xtreemfs_cleanup_startResponse response = new xtreemfs_cleanup_startResponse();
        rq.sendSuccess(response);
    }

    @Override
    public Serializable parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_cleanup_startRequest rpcrq = new xtreemfs_cleanup_startRequest();
        rpcrq.deserialize(data);

        rq.setFileId("");

        return rpcrq;
    }

    @Override
    public boolean requiresCapability() {
        return false;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    

}