/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_statusResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class CleanupGetStatusOperation extends OSDOperation {

    public CleanupGetStatusOperation(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_CLEANUP_STATUS;
    }

    @Override
    public void startRequest(final OSDRequest rq) {

        Auth authData = rq.getRPCRequest().getHeader().getRequestHeader().getAuthData();
        if (master.getConfig().getAdminPassword().length() > 0
                && (!authData.hasAuthPasswd() || !authData.getAuthPasswd().getPassword()
                        .equals(master.getConfig().getAdminPassword()))) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES,
                    "Invalid admin password.");
            return;
        }
        xtreemfs_cleanup_statusResponse response = xtreemfs_cleanup_statusResponse.newBuilder()
                .setStatus(master.getCleanupThread().getStatus()).build();
        rq.sendSuccess(response, null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        rq.setFileId("");

        return null;
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