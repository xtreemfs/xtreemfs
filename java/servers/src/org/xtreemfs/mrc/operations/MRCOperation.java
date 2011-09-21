/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.io.IOException;

import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceConstants;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public abstract class MRCOperation {
    
    protected final MRCRequestDispatcher master;
    
    public MRCOperation(MRCRequestDispatcher master) {
        this.master = master;
    }
    
    /**
     * called after request was parsed and operation assigned.
     * 
     * @param rq - the new request.
     * @param callback - the callback for the request.
     *            
     * @throws Exception if request could not have been processed.
     */
    public abstract void startRequest(MRCRequest rq, RPCRequestCallback callback) throws Exception;
    
    /**
     * Parses the request arguments.
     * 
     * @param rq - the request.
     * @throws Exception if request could not have been parsed.
     */
    public void parseRequestArgs(MRCRequest rq) throws Exception {
                    
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "parsing request arguments");
        
        final Message rqPrototype = MRCServiceConstants.getRequestMessage(rq.getRPCRequest().getHeader()
                .getRequestHeader().getProcId());
        if (rqPrototype == null) {
            rq.setRequestArgs(null);
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "received request with empty message");
        } else {
            if (rq.getRPCRequest().getMessage() != null) {
                rq.setRequestArgs(rqPrototype.newBuilderForType().mergeFrom(
                    new ReusableBufferInputStream(rq.getRPCRequest().getMessage())).build());
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "received request of type %s", rq.getRequestArgs().getClass().getName());
                }
            } else {
                rq.setRequestArgs(rqPrototype.getDefaultInstanceForType());
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "received request of type %s (empty message)", rq.getRequestArgs().getClass()
                                .getName());
                }
            }
        }
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "parsed request: %s", rqPrototype);
        }
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                "successfully parsed request arguments:");
        }
    }
    
    /**
     * Returns the context associated with a request. If the request is not
     * bound to a context, <code>null</code> is returned.
     * 
     * @param rq
     *            the MRC request
     * @return the context, or <code>null</code>, if not available
     */
    public UserCredentials getUserCredentials(MRCRequest rq) throws IOException {
        UserCredentials cred = (UserCredentials) rq.getRPCRequest().getHeader().getRequestHeader()
                .getUserCreds();
        return cred;
    }
        
    protected void validateContext(MRCRequest rq) throws UserException, IOException {
        UserCredentials ctx = getUserCredentials(rq);
        if ((ctx == null) || (ctx.getGroupsCount() == 0) || (ctx.getUsername().length() == 0)) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EACCES,
                "UserCredentials must contain a non-empty userID and at least one groupID!");
        }
    }
}
