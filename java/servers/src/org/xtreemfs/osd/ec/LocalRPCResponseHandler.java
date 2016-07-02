/*
 * Copyright (c) 2016 by Johannes Dillmann,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;

import com.google.protobuf.Message;


public class LocalRPCResponseHandler<M extends Message, O>
        implements RPCResponseAvailableListener<M>, InternalOperationCallback<M> {
    LocalRPCResponseListener<M, O> listener;
    boolean                        listenersRegistered = false;

    final AtomicInteger            numResponses;
    final AtomicInteger            numErrors;
    final AtomicInteger            numQuickFail;

    final RPCResponse<M>[]         remoteResponses;
    final AtomicInteger            remoteResponsesLength;
    final ResponseResult<M, O>[]   results;
    final AtomicInteger            resultsLength;

    ReusableBuffer                 localBuffer;


    @SuppressWarnings("unchecked")
    public LocalRPCResponseHandler(int numRemotes, LocalRPCResponseListener<M, O> listener) {
        this.listener = listener;

        numResponses = new AtomicInteger(0);
        numErrors = new AtomicInteger(0);
        numQuickFail = new AtomicInteger(0);

        remoteResponsesLength = new AtomicInteger(0);
        remoteResponses = new RPCResponse[numRemotes];
        resultsLength = new AtomicInteger(0);
        results = new ResponseResult[numRemotes + 1];
    }

    public LocalRPCResponseHandler(int numRemotes, final int numAcksRequired,
            final LocalRPCResponseQuorumListener<M, O> quorumListener) {
        this(numRemotes, null);
        this.listener = new LocalRPCResponseListener<M, O>() {
            @Override
            public void responseAvailable(ResponseResult<M, O> result, int numResponses, int numErrors,
                    int numQuickFail) {
                // TODO(jdillmann): Think about waiting for quickFail timeouts if numAcksReq is not
                // fullfilled otherwise.
                if (numResponses + numErrors + numQuickFail == results.length) {
                    if (numResponses >= numAcksRequired) {
                        quorumListener.success(results);
                    } else {
                        quorumListener.failed(results);
                    }
                }

            }
        };
    }

    public void addRemote(RPCResponse<M> response) {
        addRemote(response, null, false);
    }

    public void addRemote(RPCResponse<M> response, O object) {
        addRemote(response, object, false);
    }

    public void addRemote(RPCResponse<M> response, O object, boolean quickFail) {
        int i = remoteResponsesLength.getAndIncrement();
        if (i >= remoteResponses.length) {
            throw new IndexOutOfBoundsException();
        }

        if (quickFail) {
            numQuickFail.incrementAndGet();
        }

        remoteResponses[i] = response;
        results[i] = new ResponseResult<M, O>(object, false);

        if (resultsLength.incrementAndGet() == results.length) {
            registerListeners();
        }
    }

    public void registerListeners() {
        if (listenersRegistered) {
            throw new RuntimeException("Listeners can only be registered once.");
        }
        listenersRegistered = true;

        for (RPCResponse<M> response : remoteResponses) {
            response.registerListener(this);
        }
    }

    @Override
    public void responseAvailable(RPCResponse<M> r) {
        int i = indexOf(remoteResponses, r);
        if (i < 0) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this, "received unknown response");
            r.freeBuffers();
            return;
        }

        ResponseResult<M, O> responseResult = results[i];

        int curNumResponses, curNumErrors, curNumQuickFail;

        // Decrement the number of outstanding requests that may fail quick.
        if (responseResult.mayQuickFail()) {
            curNumQuickFail = numQuickFail.decrementAndGet();
        } else {
            curNumQuickFail = numQuickFail.get();
        }

        try {
            // Try to get and add the result.
            M result = r.get();
            responseResult.setResult(result);
            curNumResponses = numResponses.incrementAndGet();
            curNumErrors = numErrors.get();
        } catch (PBRPCException ex) {
            // FIXME (jdillmann): Which errors should mark OSDs as quickfail?
            responseResult.setFailed(ex.getErrorResponse());
            curNumErrors = numErrors.incrementAndGet();
            curNumResponses = numResponses.get();

        } catch (Exception ex) {
            // Try to
            responseResult.setFailed(ErrorUtils.getInternalServerError(ex));
            curNumErrors = numErrors.incrementAndGet();
            curNumResponses = numResponses.get();

        } finally {
            r.freeBuffers();
        }


        listener.responseAvailable(responseResult, curNumResponses, curNumErrors, curNumQuickFail);
    }

    public void addLocal(O object) {
        addLocal(object, null);
    }

    public void addLocal(O object, ReusableBuffer localBuffer) {
        results[results.length - 1] = new ResponseResult<M, O>(object, false);
        this.localBuffer = localBuffer;

        if (resultsLength.incrementAndGet() == results.length) {
            registerListeners();
        }
    }

    @Override
    public void localResultAvailable(M result) {
        ResponseResult<M, O> responseResult = results[results.length - 1];

        int curNumResponses, curNumErrors, curNumQuickFail;

        responseResult.setResult(result);
        curNumResponses = numResponses.incrementAndGet();
        curNumErrors = numErrors.get();
        curNumQuickFail = numQuickFail.get();

        BufferPool.free(localBuffer);
        listener.responseAvailable(responseResult, curNumResponses, curNumErrors, curNumQuickFail);
    }

    @Override
    public void localRequestFailed(ErrorResponse error) {
        ResponseResult<M, O> responseResult = results[results.length - 1];

        if (error != null) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.all, (Object) null,
                    "Retrieving the local result (%s) failed", responseResult.result.getClass().getName());
        }


        int curNumResponses, curNumErrors, curNumQuickFail;

        responseResult.setFailed(error);
        curNumErrors = numErrors.incrementAndGet();
        curNumResponses = numResponses.get();
        curNumQuickFail = numQuickFail.get();

        BufferPool.free(localBuffer);
        listener.responseAvailable(responseResult, curNumResponses, curNumErrors, curNumQuickFail);
    }

    @SuppressWarnings("rawtypes")
    static int indexOf(RPCResponse[] responses, RPCResponse response) {
        for (int i = 0; i < responses.length; ++i) {
            if (responses[i].equals(response)) {
                return i;
            }
        }
        return -1;
    }

    public static class ResponseResult<M extends Message, O> {
        private final O       object;
        private final boolean quickFail;
        private boolean       failed;
        private M             result;
        private boolean       local;
        private ErrorResponse error;

        ResponseResult(O object, boolean quickFail) {
            this.failed = false;
            this.result = null;
            this.object = object;
            this.quickFail = quickFail;
        }

        synchronized void setFailed(ErrorResponse error) {
            this.failed = true;
            this.result = null;
            this.error = error;
        }

        synchronized public boolean hasFailed() {
            return failed;
        }

        synchronized public ErrorResponse getError() {
            return error;
        }

        synchronized public boolean hasFinished() {
            return (result != null || failed);
        }

        synchronized void setResult(M result) {
            this.failed = false;
            this.result = result;
        }

        synchronized public M getResult() {
            return result;
        }

        public O getMappedObject() {
            return object;
        }

        public boolean mayQuickFail() {
            return quickFail;
        }
    }

    public static interface LocalRPCResponseListener<M extends Message, O> {
        void responseAvailable(ResponseResult<M, O> result, int numResponses, int numErrors, int numQuickFail);
    }

    public static interface LocalRPCResponseQuorumListener<M extends Message, O> {
        void success(ResponseResult<M, O>[] results);

        void failed(ResponseResult<M, O>[] results);
    }
}
