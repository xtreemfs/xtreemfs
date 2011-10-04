/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_is_runningResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class CleanupIsRunningOperation extends OSDOperation {


    public CleanupIsRunningOperation(OSDRequestDispatcher master) {
        
        super(master);
    }

    @Override
    public int getProcedureId() {
        
        return OSDServiceConstants.PROC_ID_XTREEMFS_CLEANUP_IS_RUNNING;
    }

    @Override
    public ErrorResponse startRequest(OSDRequest rq, RPCRequestCallback callback) {

        Auth authData = rq.getRPCRequest().getHeader().getRequestHeader().getAuthData();
        if (!authData.hasAuthPasswd() || authData.getAuthPasswd().equals(master.getConfig().getAdminPassword())) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, 
                    "this operation requires an admin password");
        }
        
        try{ 
         
            callback.success(xtreemfs_cleanup_is_runningResponse.newBuilder().setIsRunning(
                    master.getCleanupThread().isRunning()).build());
            
            return null;
        } catch (ErrorResponseException e) {
            
            return e.getRPCError();
        }
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