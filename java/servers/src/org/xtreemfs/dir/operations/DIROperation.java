/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;

import org.xtreemfs.babudb.api.database.DatabaseRequestListener;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;

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
     * @param rq
     *            the new request
     */
    public abstract void startRequest(DIRRequest rq);

    /**
     * Method to check if operation needs user authentication.
     * 
     * @return true, if the user needs to be authenticated
     */
    public abstract boolean isAuthRequired();

    protected abstract Message getRequestMessagePrototype();

    /**
     * parses the RPC request message. Can throw any exception which
     * will result in an error message telling the client that the
     * request message data is garbage.
     * 
     * @param rq
     * @throws java.lang.Exception
     */
    public void parseRPCMessage(DIRRequest rq) throws IOException {
        
        RPCServerRequest rpcRequest = rq.getRPCRequest();
        Message message = rq.getRequestArgs();
        final ReusableBuffer payload = rpcRequest.getMessage();
        if (message != null) {
            if (payload != null) {
                message = message.newBuilderForType().mergeFrom(new ReusableBufferInputStream(payload)).build();
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "parsed request: %s", message.toString());
                }
            } else {
                message = message.getDefaultInstanceForType();
            }
        } else {
            message = null;
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "parsed request: empty message (emptyRequest)");
            }
        }
        rq.setRequestArgs(message);
    }

    void requestFailed(BabuDBException error, DIRRequest rq) {
        assert(error != null);
        rq.sendError(ErrorType.ERRNO,POSIXErrno.POSIX_ERROR_EINVAL,error.toString());
    }

    /**
     * Method-interface for sending a response 
     * 
     * @param result - can be null, if not necessary.
     * @param rq - original {@link DIRRequest}.
     */
    abstract void requestFinished(Object result, DIRRequest rq);

    /**
     * Listener implementation for non-blocking BabuDB requests.
     * 
     * @author flangner
     * @since 11/16/2009
     * @param <I> - input type.
     * @param <O> - output type.
     */
    abstract class DBRequestListener<I, O> implements DatabaseRequestListener<I> {

        private final boolean finishRequest;

        DBRequestListener(boolean finishRequest) {
            this.finishRequest = finishRequest;
        }

        abstract O execute(I result, DIRRequest rq) throws Exception;

        /*
         * (non-Javadoc)
         * @see org.xtreemfs.babudb.BabuDBRequestListener#failed(org.xtreemfs.babudb.BabuDBException, java.lang.Object)
         */
        @Override
        public void failed(BabuDBException error, Object request) {
            requestFailed(error, (DIRRequest) request);
        }

        /*
         * (non-Javadoc)
         * @see org.xtreemfs.babudb.BabuDBRequestListener#finished(java.lang.Object, java.lang.Object)
         */
        @Override
        public void finished(I data, Object context) {
            try {
                O result = execute(data, (DIRRequest) context);
                if (finishRequest) {
                    requestFinished(result, (DIRRequest) context);
                }
            } catch (IllegalArgumentException ex) {
                DIRRequest rq = (DIRRequest) context;
                rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, ex.toString());
            } catch (java.util.ConcurrentModificationException ex) {
                DIRRequest rq = (DIRRequest) context;
                rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EAGAIN, ex.toString());
            } catch (Exception e) {
                DIRRequest rq = (DIRRequest) context;
                rq.sendInternalServerError(e);
            }
        }
    }
}
