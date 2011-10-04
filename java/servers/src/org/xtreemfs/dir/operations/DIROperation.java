/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;

import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public abstract class DIROperation {

    protected final DIRRequestDispatcher master;

    public DIROperation(DIRRequestDispatcher master) {
        this.master = master;
    }

    public abstract int getProcedureId();

    /**
     * called after request was parsed and operation assigned.
     * 
     * @param rq - the new request.
     * @param callback - the callback for the request.
     *            
     * @throws ErrorResponseException if request could not have been processed.
     */
    public abstract void startRequest(DIRRequest rq, RPCRequestCallback callback) throws ErrorResponseException;

    /**
     * Method to check if operation needs user authentication.
     * 
     * @return true, if the user needs to be authenticated
     */
    public boolean isAuthRequired() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * parses the RPC request message. Can throw any exception which
     * will result in an error message telling the client that the
     * request message data is garbage.
     * 
     * @param rq
     * @throws IOException
     */
    public void parseRPCMessage(DIRRequest rq) throws IOException {
        
        final Message rqPrototype = DIRServiceConstants.getRequestMessage(rq.getRPCRequest().getHeader()
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
}
