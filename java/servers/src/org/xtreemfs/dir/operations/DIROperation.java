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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;

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
     * @param rq
     * @throws java.lang.Exception
     */
    public void parseRPCMessage(DIRRequest rq) throws IOException {
        rq.deserializeMessage(getRequestMessagePrototype());
    }

    void requestFailed(BabuDBException error, DIRRequest rq) {
        assert(error != null);
        rq.sendError(ErrorType.ERRNO,POSIXErrno.POSIX_ERROR_EINVAL,error.toString());
    }

    /**
     * Operation to give a failure back to the client.
     * Will decide, if a {@link RedirectException} should be returned.
     * 
     * @param error - Exception thrown.
     * @param rq - original {@link DIRRequest}.
     */
    /*
    void requestFailed(Exception error, DIRRequest rq) {
        // handle connection errors caused by being not the replication master
        if (error != null && dbsReplicationManager != null
                && ((error instanceof BabuDBException
                && ((BabuDBException) error).getErrorCode().equals(NO_ACCESS)) || ( // TODO better exception handling
                error instanceof ConcurrentModificationException)) // && !dbsReplicationManager.isMaster() ... removed for testing
                ) {

            InetAddress altMaster = dbsReplicationManager.getMaster();
            if (altMaster != null) {
                // retrieve the correct port for the DIR mirror
                String host = altMaster.getHostAddress();
                Integer port = this.master.getConfig().getMirrors().get(host);
                if (port == null) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "The port for "
                            + "the mirror DIR '%s' could not be retrieved.",
                            host);

                    rq.sendInternalServerError(error);
                } else {
                    rq.sendRedirectException(host, port);
                }
            } else {
                // if there is a handover in progress, redirect to the local
                // server to notify the client about this process
                InetAddress host = this.master.getConfig().getAddress();
                int port = this.master.getConfig().getPort();
                InetSocketAddress address =
                        (host == null)
                        ? new InetSocketAddress(port)
                        : new InetSocketAddress(host, port);

                rq.sendRedirectException(address.getAddress().getHostAddress(),
                        port);
            }
            // handle errors caused by ServerExceptions
        } else if (error != null && error instanceof ONCRPCException) {
            Logging.logError(Logging.LEVEL_ERROR, this, error);
            rq.sendException((ONCRPCException) error);

            // handle user errors
        } else if (error != null
                && error instanceof BabuDBException
                && (((BabuDBException) error).getErrorCode().equals(NO_SUCH_DB)
                || ((BabuDBException) error).getErrorCode().equals(DB_EXISTS)
                || ((BabuDBException) error).getErrorCode().equals(NO_SUCH_INDEX)
                || ((BabuDBException) error).getErrorCode().equals(NO_SUCH_SNAPSHOT)
                || ((BabuDBException) error).getErrorCode().equals(SNAP_EXISTS))) { // blame the client
            Logging.logError(Logging.LEVEL_ERROR, this, error);
            rq.sendException(new InvalidArgumentException(error.getMessage()));
            // handle unknown errors
        } else {
            if (error != null && !(error instanceof BabuDBException)) {
                Logging.logError(Logging.LEVEL_ERROR, this, error);
            }

            if (error != null) {
                Logging.logError(Logging.LEVEL_ERROR, this, error);
            }

            rq.sendInternalServerError(error);
        }
    }*/

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
