/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.client;

import com.google.protobuf.Message;
import java.io.IOException;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;

/**
 *
 * @author bjko
 */
public class RPCResponse<V extends Message> implements RPCResponseListener<V> {

    private static final boolean TRACE_DUPLICATE_RESPONSES = false;

    private RPCClientRequest request;

    private RPCResponseAvailableListener<V> listener;

    private String errorMessage;

    private boolean failed;

    private final V responsePrototype;

    private Object attachment;

    private ReusableBuffer message, data;


    public RPCResponse(V responsePrototype) {
        failed = false;
        this.responsePrototype = responsePrototype;
    }

    public void freeBuffers() {
        if (request != null)
            request.freeBuffers();
    }

    public void registerListener(RPCResponseAvailableListener<V> listener) {
        synchronized (this) {
            this.listener = listener;

            if (request != null || failed) {
                //do notification
                listener.responseAvailable(this);
            }
        }
    }

    public V get() throws IOException, InterruptedException {
        waitForResult();
        if (failed) {
            throw new IOException(errorMessage);
        } else {
            if (request.getResponseHeader().getMessageType() == MessageType.RPC_RESPONSE_SUCCESS) {
                if (responsePrototype != null) {
                    if (message != null) {
                        ReusableBufferInputStream rbis = new ReusableBufferInputStream(message);
                        V responseObject = (V) responsePrototype.newBuilderForType().mergeFrom(rbis).build();
                        assert(responseObject != null);
                        BufferPool.free(message);
                        message = null;
                        return responseObject;
                    } else {
                        return (V) responsePrototype.getDefaultInstanceForType();
                    }
                } else {
                    if (message != null)
                        throw new RuntimeException("specify response prototype for null message!");
                    return null;
                }
            } else {
                ErrorResponse err = request.getResponseHeader().getErrorResponse();
                throw new PBRPCException(err);
            }
        }
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public Object getAttachment() {
        return this.attachment;
    }

    public ReusableBuffer getData() throws InterruptedException {
        waitForResult();
        return data;
    }

    public void waitForResult() throws InterruptedException {
        synchronized (this) {
            if (request == null && !failed)
                this.wait();
        }
    }

    @Override
    public void responseAvailable(RPCClientRequest<V> request, ReusableBuffer message, ReusableBuffer data) {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "response received");
        synchronized (this) {
            /*if (TRACE_DUPLICATE_RESPONSES) {
                if (responseTrace != null) {
                    StringBuffer strace = new StringBuffer();
                    for (int i = responseTrace.length-1; i >= 0; i--) {
                        strace.append("\t");
                        strace.append(responseTrace[i].toString());
                    }
                    throw new RuntimeException("response already set:\n"+strace.toString());
                } else {
                    responseTrace = Thread.currentThread().getStackTrace();
                }
            }*/
            this.message = message;
            this.data = data;
            this.request = request;
            if (listener != null)
                listener.responseAvailable(this);
            this.notify();
        }
    }

    @Override
    public void requestFailed(String errorMessage) {
        synchronized (this) {
            this.failed = true;
            this.errorMessage = errorMessage;
            if (listener != null)
                listener.responseAvailable(this);
            this.notify();
        }
    }

    /**
     * duration of request from sending the request until the response
     * was received completeley.
     * @return duration in ns
     */
    public long getDuration() {
        return request.getDuration();
    }
}
