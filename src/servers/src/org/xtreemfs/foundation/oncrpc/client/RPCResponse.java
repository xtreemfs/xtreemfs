/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.oncrpc.client;

import java.io.IOException;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.interfaces.utils.ONCRPCException;

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
