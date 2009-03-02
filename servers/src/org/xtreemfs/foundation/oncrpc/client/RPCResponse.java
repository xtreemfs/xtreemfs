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
import org.xtreemfs.interfaces.utils.ONCRPCException;

/**
 *
 * @author bjko
 */
public class RPCResponse<V extends Object> implements RPCResponseListener {

    private ONCRPCRequest request;

    private IOException   ioError;

    private ONCRPCException remoteEx;

    private RPCResponseAvailableListener<V> listener;

    private final RPCResponseDecoder<V> decoder;

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
            if (ioError != null)
                throw ioError;
            throw remoteEx;
        }
    }
    
    public Object getAttachment() throws InterruptedException {
        if (request != null)
            return request.getAttachment();
        else
            return null;
    }

    public void waitForResult() throws InterruptedException {
        synchronized (this) {
            if (request == null)
                this.wait();
        }
    }

    @Override
    public void responseAvailable(ONCRPCRequest request) {
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"response received");
        synchronized (this) {
            this.request = request;
            if (listener != null)
                listener.responseAvailable(this);
            else
                this.notify();
        }
    }

    @Override
    public void remoteExceptionThrown(ONCRPCRequest request, ONCRPCException exception) {
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"remote exception received");
        synchronized (this) {
            this.request = request;
            this.remoteEx = exception;
            if (listener != null)
                listener.responseAvailable(this);
            else
                this.notify();
        }
    }

    @Override
    public void requestFailed(ONCRPCRequest request, IOException reason) {
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"request failed received");
        synchronized (this) {
            this.request = request;
            this.ioError = reason;
            if (listener != null)
                listener.responseAvailable(this);
            else
                this.notify();
        }
    }

}
