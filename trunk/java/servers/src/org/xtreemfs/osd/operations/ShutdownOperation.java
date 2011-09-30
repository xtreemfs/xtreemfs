/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 *
 * @author bjko
 */
public class ShutdownOperation extends OSDOperation {

    public ShutdownOperation(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_SHUTDOWN;
    }

    @Override
    public void startRequest(OSDRequest rq) {

        // check password to ensure that user is authorized
        Auth authData = rq.getRPCRequest().getHeader().getRequestHeader().getAuthData();
        if (master.getConfig().getAdminPassword().length() > 0
                && !master.getConfig().getAdminPassword().equals(authData.getAuthPasswd())) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, "this operation requires an admin password");
            return;
        }

        try {
            rq.sendSuccess(null,null);
            Thread.sleep(100);
            master.asyncShutdown();
        } catch (Throwable thr) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "exception during shutdown");
            Logging.logError(Logging.LEVEL_ERROR, this, thr);
        }
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
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

}
