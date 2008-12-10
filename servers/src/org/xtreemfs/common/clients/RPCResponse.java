/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.common.clients;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;

/**
 * Response for an asynchronous RPC request.
 *
 * @author bjko
 */
public class RPCResponse<V extends Object> implements SpeedyResponseListener {
    
    public static int MAX_AUTH_RETRY = 3;

    protected InetSocketAddress   targetServer;

    protected RPCResponseListener listener;

    /**
     * The httpRequest used for sending this RPC via Speedy.
     */
    protected SpeedyRequest       httpRequest;

    /**
     * Arbitrary attachment for continouations.
     */
    protected Object              attachment;

    protected AtomicBoolean       finished;
    
    protected String              username, password;
    
    protected int                 authRetryCount;
    
    protected MultiSpeedy         speedy;

    /**
     * Creates a new instance of RPCResponse
     *
     * @param request
     *            the request sent via Speedy
     */
    public RPCResponse(SpeedyRequest request, InetSocketAddress server) {
        this.httpRequest = request;
        this.targetServer = server;
        finished = new AtomicBoolean(false);
    }
    
    /**
     * Creates a new instance of RPCResponse with credentials for digest authentication
     *
     * @param request
     *            the request sent via Speedy
     */
    public RPCResponse(SpeedyRequest request, InetSocketAddress server, MultiSpeedy speedy, String username, String password) {
        this(request,server);
        this.username = username;
        this.password = password;
        this.authRetryCount = 0;
        this.speedy = speedy;
    }

    /**
     * Checks the status of the request.
     *
     * @return returns true, if the server response is available or the request
     *         has failed.
     */
    public boolean isDone() {
        return this.finished.get();
    }

    /**
     * Waits for the response if necessary and throws exceptions if the request
     * did not succed.
     *
     * If the server sent a response and a status code 200 (OK) the method
     * returns. If another status code is returned, an HttpErrorException is
     * thrown. If the server is not available or some other communication error
     * occurrs, an IO exception is thrown.
     *
     * @throws java.lang.InterruptedException
     *             if it is interrupted while waiting for the server's response.
     * @throws org.xtreemfs.common.clients.HttpErrorException
     *             if the server returns a status code other than 200 (OK)
     * @throws java.io.IOException
     *             if the server is not available or a communication error
     *             occurs
     */
    public void waitForResponse() throws InterruptedException,
        HttpErrorException, IOException {
        waitForResponse(0);
    }

    public void waitForResponse(long timeout) throws InterruptedException, HttpErrorException,
        IOException {
        synchronized (this) {
            if (!isDone()) {
                this.wait(timeout);
            }
        }
        assert (httpRequest != null);
        if (httpRequest.status == SpeedyRequest.RequestStatus.FINISHED) {
            if (httpRequest.statusCode == HTTPUtils.SC_OKAY) {
                return;
            } else {
                if ( (httpRequest.statusCode == HTTPUtils.SC_UNAUTHORIZED) && 
                     (username != null) && (this.authRetryCount > MAX_AUTH_RETRY)) {
                    //resend with authentication!
                    httpRequest.addDigestAuthentication(username, password);
                    this.authRetryCount++;
                    assert(httpRequest.listener == this);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"resending request with digest authentication");
                    speedy.sendRequest(httpRequest, targetServer);
                }
                if (httpRequest.responseBody != null)
                    throw new HttpErrorException(httpRequest.statusCode, httpRequest.responseBody
                            .array());
                else
                    throw new HttpErrorException(httpRequest.statusCode);
            }
        } else if (httpRequest.status == SpeedyRequest.RequestStatus.PENDING) {
            throw new IOException("server " + targetServer + " is not available");
        } else if (httpRequest.status == SpeedyRequest.RequestStatus.SENDING) {
            throw new IOException("cannot establish connection to server " + targetServer);
        } else if (httpRequest.status == SpeedyRequest.RequestStatus.WAITING) {
            throw new IOException("server " + targetServer + " did not send a response");
        } else {
            throw new IOException("server " + targetServer + " is not available");
        }
    }

    /**
     * Retrieves the response body sent by the server. Waits for the server if
     * necessary.
     *
     * If the server sent a response and a status code 200 (OK) the method
     * returns. If another status code is returned, an HttpErrorException is
     * thrown. If the server is not available or some other communication error
     * occurrs, an IO exception is thrown.
     *
     * @param timeout
     *            milliseconds to wait for a server response
     * @throws java.lang.InterruptedException
     *             if it is interrupted while waiting for the server's response.
     * @throws org.xtreemfs.common.clients.HttpErrorException
     *             if the server returns a status code other than 200 (OK)
     * @throws java.io.IOException
     *             if the server is not available or a communication error
     *             occurs
     * @return the response body
     */
    public ReusableBuffer getBody() throws InterruptedException,
        HttpErrorException, IOException {
        waitForResponse();
        return httpRequest.responseBody;
    }

    public V get() throws InterruptedException, HttpErrorException,
        IOException, JSONException {

        waitForResponse();
        if (httpRequest.responseBody == null)
            return null;

        String body = new String(httpRequest.responseBody.array(),
            HTTPUtils.ENC_UTF8);
        Object o = JSONParser.parseJSON(new JSONString(body));
        return (V) o;
    }

    /**
     * Retrieves the response status code. Waits for the server's response if
     * necessary.
     *
     * @param timeout
     *            milliseconds to wait for a server response
     * @return the status code
     * @throws java.lang.InterruptedException
     *             if it is interrupted while waiting for the server's response.
     * @throws org.xtreemfs.common.clients.HttpErrorException
     *             if the server returns a status code other than 200 (OK)
     * @throws java.io.IOException
     *             if the server is not available or a communication error
     *             occurs
     */
    public int getStatusCode() throws InterruptedException, HttpErrorException,
        IOException {
        waitForResponse();
        return httpRequest.statusCode;
    }

    /**
     * Retrieves the response headers. Waits for the server's response if
     * necessary.
     *
     * @param timeout
     *            milliseconds to wait for a server response
     * @throws java.lang.InterruptedException
     *             if it is interrupted while waiting for the server's response.
     * @throws org.xtreemfs.common.clients.HttpErrorException
     *             if the server returns a status code other than 200 (OK)
     * @throws java.io.IOException
     *             if the server is not available or a communication error
     *             occurs
     * @return the response headers sent by the server
     */
    public HTTPHeaders getHeaders() throws InterruptedException,
        HttpErrorException, IOException {
        waitForResponse();
        return httpRequest.responseHeaders;
    }

    public void receiveRequest(SpeedyRequest theRequest) {
        // Logging.logMessage(Logging.LEVEL_ERROR,this,"EVENT: "+theRequest);

        if (this.finished.getAndSet(true)) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,
                "RESPONSE ALREADY SET!");
            throw new RuntimeException("response already sent!");
        }
        
        if (listener != null) {
            listener.responseAvailable(this);
        }
        
        synchronized (this) {
            this.notifyAll();
        }
    }
    
    

    public void setResponseListener(RPCResponseListener listener) {
        synchronized (this) {
            this.listener = listener;
            if (this.isDone())
                listener.responseAvailable(this);
        }
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public Object getAttachment() {
        return this.attachment;
    }

    public SpeedyRequest getSpeedyRequest() {
        return httpRequest;
    }

    public void freeBuffers() {
        this.httpRequest.freeBuffer();
        this.httpRequest = null;
    }

    protected void finalize() {
        if (this.httpRequest != null) {
            Logging.logMessage(Logging.LEVEL_DEBUG,this,"auto free for: "+this.httpRequest.responseHeaders);
            freeBuffers();
        }
    }
    
}
