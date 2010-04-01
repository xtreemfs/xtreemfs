/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.oncrpc.client;

import java.io.IOException;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCException;

/**
 *
 * @author bjko
 */
public class RPCResponse<V extends Object> implements RPCResponseListener {

    private static final boolean TRACE_DUPLICATE_RESPONSES = false;

    private StackTraceElement[] responseTrace;

    private ONCRPCRequest request;

    private IOException   ioError;

    private ONCRPCException remoteEx;

    private RPCResponseAvailableListener<V> listener;

    private final RPCResponseDecoder<V> decoder;

    private Object         attachment;

    public RPCResponse(RPCResponseDecoder<V> decoder) {
        this.decoder = decoder;
    }

    public void freeBuffers() {
        if (request != null)
            request.freeBuffers();
    }

    public void registerListener(RPCResponseAvailableListener<V> listener) {
        this.listener = listener;
        synchronized (this) {
            if (request != null) {
                //do notification
                listener.responseAvailable(this);
            }
        }
    }

    public V get() throws ONCRPCException, IOException, InterruptedException {
        waitForResult();
        if ((ioError == null) && (remoteEx == null)) {
            V responseObject = decoder.getResult(request.getResponseFragments().get(0));
            return responseObject;
        } else {
            if (ioError != null) {
                ioError.fillInStackTrace();
                throw ioError;
            }
            remoteEx.fillInStackTrace();
            throw remoteEx;
        }
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public Object getAttachment() {
        return this.attachment;
    }

    public void waitForResult() throws InterruptedException {
        synchronized (this) {
            if (request == null)
                this.wait();
        }
    }

    @Override
    public void responseAvailable(ONCRPCRequest request) {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "response received");
        synchronized (this) {
            if (TRACE_DUPLICATE_RESPONSES) {
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
            }
            this.request = request;
            if (listener != null)
                listener.responseAvailable(this);
            this.notify();
        }
    }

    @Override
    public void remoteExceptionThrown(ONCRPCRequest request, ONCRPCException exception) {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "remote exception received");
        synchronized (this) {
            if (TRACE_DUPLICATE_RESPONSES) {
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
            }
            this.request = request;
            this.remoteEx = exception;
            if (listener != null)
                listener.responseAvailable(this);
            this.notify();
        }
    }

    @Override
    public void requestFailed(ONCRPCRequest request, IOException reason) {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "request failed received");
        synchronized (this) {
            if (TRACE_DUPLICATE_RESPONSES) {
                if (responseTrace != null) {
                    StringBuffer strace = new StringBuffer();
                    for (int i = responseTrace.length-1; i >= 0; i--) {
                        strace.append("\t");
                        strace.append(responseTrace[i].toString());
                        strace.append("\n");
                    }
                    throw new RuntimeException("response already set:\n"+strace.toString());
                } else {
                    responseTrace = Thread.currentThread().getStackTrace();
                }
            }
            this.request = request;
            this.ioError = reason;
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
    
    /**
     * @return the decoder
     */
    public RPCResponseDecoder<V> getDecoder() {
        return decoder;
    }
    
    /**
     * Fills the given information into this request. Respecting the
     * changes for the listener or a synchronously waiting thread.
     * @param rp
     */
    public synchronized void fill (RPCResponse<V> rp) {
        synchronized (rp) {
            this.attachment = rp.attachment;
            this.ioError = rp.ioError;
            this.remoteEx = rp.remoteEx;
            if (rp.request != null) {
                this.request = rp.request;
                if (listener != null) 
                    listener.responseAvailable(this);
                this.notifyAll();
            }   
        }
    }
}
