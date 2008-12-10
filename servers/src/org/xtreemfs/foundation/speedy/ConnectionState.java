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

package org.xtreemfs.foundation.speedy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;

/**
 * Contains a buffer and an active marker for each connection. Includes also the
 * parsing routine and state machine state.
 *
 * @author bjko
 */
public class ConnectionState {

    /**
     * Maximum buffer size
     */
    public static final int BUFFSIZE = 1024 * 256;

    /**
     * Maximum header size
     */
    public static final int MAX_HDR_SIZE = 1024;

    /**
     * Initial Size of buffer for headers
     */
    public static final int INIT_HDR_BUF = 128;

    /**
     * Buffer holding the incomming data
     */
    ReusableBuffer data;

    /**
     * The channel associated w/ this connection
     */
    ChannelIO channel;

    /**
     * will be set to false by a periodical clean up task. Inactive connections
     * (timed out) will be closed and removed.
     */
    AtomicBoolean active;

    /**
     * Buffer for receiving the headers.
     */
    StringBuilder requestHeaders;

    /**
     * The payload received from the client.
     */
    ReusableBuffer requestBody;

    /**
     * The URI the client requested
     */
    int responseStatusCode;

    /**
     * The content-bytrange the client requested or null
     */
    String byteRange;

    /**
     * The request that the client waits for.
     */
    public SpeedyRequest waitFor;

    /**
     * Status of the parser state machine if connection is idle
     */
    public static final int STATUS_IDLE = 0;

    /**
     * Status of the parser state machine if reading the headers
     */
    public static final int STATUS_READ_HEADERS = 1;

    /**
     * Status of the parser state machine after receiving a CRLF
     */
    public static final int STATUS_CRLF = 5;

    /**
     * Status of the parser state machine if request was parsed
     */
    public static final int STATUS_PARSE_REQUEST = 2;

    /**
     * Status of the parser state machine while reading the body
     */
    public static final int STATUS_READ_BODY = 3;

    /**
     * Current status of the request parser state machine
     */
    int status;

    /**
     * Request method, can be GET or PUT
     */
    public String method;

    /**
     * pipeline with requests
     */
    public LinkedBlockingQueue<SpeedyRequest> sendQ;

    /**
     * pipeline with requests
     */
    public LinkedList<SpeedyRequest> receiveQ;

    /**
     * this is for the multiSpeedy
     */
    public InetSocketAddress endpoint;

    /**
     * number of connect retries
     */
    public int conRetries;

    /**
     * can be used to set a timeout after which to
     * reset conRetries
     */
    public long nextReconnectTime;

    /**
     * number of reconnect cycles that failed.
     * used to increase the wait timeout
     */
    public int numReconnectCycles;
    
    /**
     * Timestamp of last request sent through this connection.
     */
    public long lastUsed;

    public static final int RETRY_RESET_IN_MS = 500;

    /** max wait is one hour
     */
    public static final int MAX_RETRY_WAIT = 1000*60*60;

    /** Maximum size of body to accept.
     */
    public static final  int MAX_BODY_SIZE = 1024*1024*64;
    
    /**
     * Creates a new instance of ConnectionStatus
     *
     * @param channel
     *            the channel to which this state object belongs.
     */
    public ConnectionState(ChannelIO channel) {

        active = new AtomicBoolean(true);

        this.channel = channel;

        //data = ByteBuffer.allocateDirect(BUFFSIZE);
        data = BufferPool.allocate(BUFFSIZE);

        this.status = STATUS_IDLE;

        waitFor = null;

        sendQ = new LinkedBlockingQueue(MultiSpeedy.MAX_CLIENT_QUEUE);

        receiveQ = new LinkedList();

        this.conRetries = 0;

        this.numReconnectCycles = 0;
        
        this.lastUsed = TimeSync.getLocalSystemTime();
    }

    public void successfulConnect() {
        this.numReconnectCycles = 0;
        this.nextReconnectTime = 0;
    }

    public void connectFailed() {
        this.numReconnectCycles++;
        long waitt = Math.round(RETRY_RESET_IN_MS*Math.pow(2,this.numReconnectCycles));
        if (waitt > MAX_RETRY_WAIT)
            waitt = MAX_RETRY_WAIT;
        Logging.logMessage(Logging.LEVEL_DEBUG,this,"next reconnect possible after "+(waitt/1000)+" s, "+this.numReconnectCycles);
        this.nextReconnectTime = System.currentTimeMillis()+waitt;
        this.lastUsed = TimeSync.getLocalSystemTime();
    }

    public boolean canReconnect() {
        return (this.nextReconnectTime < System.currentTimeMillis());
    }

    public boolean serverIsAvailable() {
        if (this.conRetries < MultiSpeedy.MAX_RECONNECT) {
            return true;
        } else {
            return canReconnect();
        }
    }

    /**
     * This is the main parsing method. It parses the available data in the
     * buffer.
     *
     * @throws com.xtreemfs.speedyg.SpeedyException
     *             if an error occurs while parsing the response
     */
    public void processBuffer() throws SpeedyException {

        // loop until data is empty
        while (data.hasRemaining()) {
            
            switch (this.status) {
                case STATUS_IDLE : {
                    // prepare request
                    this.requestHeaders = new StringBuilder(INIT_HDR_BUF);
                    this.requestBody = null;
                    this.status = STATUS_READ_HEADERS;
                    this.responseStatusCode = 0;
                    // TRANSITION
                    this.waitFor = null;
                    // find next waiting request
                    Iterator<SpeedyRequest> iter = this.receiveQ.iterator();
                    while (iter.hasNext()) {
                        SpeedyRequest sr = iter.next();
                        if (sr.status == SpeedyRequest.RequestStatus.WAITING) {
                            this.waitFor = sr;
                            break;
                        }
                    }
                    if (waitFor == null) {
                        Logging.logMessage(Logging.LEVEL_ERROR,this,"WWWWWWWWWWWWWWWWWWWWW waitFor == null! Clearing buffer...");
                        this.status = STATUS_IDLE;
                        data.limit(0);
                        return;
                    }
                }

                case STATUS_READ_HEADERS : {
                    char ch = (char) (data.get() & 0xFF);
                    if (ch == '\n') {
                        // TRANSITION
                        this.status = STATUS_CRLF;
                        this.requestHeaders.append(ch);

                    } else if (ch != '\r') {
                        // ignore \r s
                        this.requestHeaders.append(ch);
                    }

                    // check for overflows...
                    if (this.requestHeaders.length() >= MAX_HDR_SIZE) {
                        // invalidate all requests!
                        for (SpeedyRequest rq : this.sendQ) {
                            rq.status = SpeedyRequest.RequestStatus.FAILED;
                        }
                        throw new SpeedyException(
                                "Response header exceeds max header size ("
                                        + MAX_HDR_SIZE + " bytes): "+this.requestHeaders, true);
                    }
                    break;
                }

                case  STATUS_CRLF : {
                    char ch = (char) (data.get() & 0xFF);
                    if (ch == '\r') {
                        // IGNORE \r
                        continue;
                    }
                    if (ch != '\n') {
                        // TRANSITION
                        this.requestHeaders.append(ch);
                        this.status = STATUS_READ_HEADERS;
                        continue;
                    }
                    // if a second \n comes, headers are done
                    // TRANSITION
                    this.status = STATUS_PARSE_REQUEST;
                }

                case STATUS_PARSE_REQUEST: {
                    
                    // if there is a content length field, try to read the body
                    int nextNL = this.requestHeaders.indexOf("\n");
                    int cPos = 0;
                    String ftLine = null;
                    int length = 0;

                    while (nextNL != -1) {

                        String line = this.requestHeaders.substring(cPos, nextNL);
                        cPos = nextNL + 1;
                        nextNL = this.requestHeaders.indexOf("\n", cPos);

                        if (ftLine == null)
                            ftLine = line;

                        //if (HTTPUtils.isContentLength(line)) {
                        if (HTTPUtils.compareHeaderName("CONTENT-LENGTH",line)) {
                            try {
                                String len = line.substring(15).trim();
                                length = Integer.valueOf(len);
                            } catch (Exception ex) {
                                // no transition because con is closed anyway...
                                // invalidate all requests!
                                for (SpeedyRequest rq : this.sendQ) {
                                    rq.status = SpeedyRequest.RequestStatus.FAILED;
                                }
                                throw new SpeedyException(
                                        "Conten-Length header is not an integer value!",
                                        true);
                            }
                            if (length > MAX_BODY_SIZE) {
                                // make sure its not too long...
                                // no transition because con is closed anyway...
                                // invalidate all requests!
                                for (SpeedyRequest rq : this.sendQ) {
                                    rq.status = SpeedyRequest.RequestStatus.FAILED;
                                }
                                throw new SpeedyException(
                                        "Body length exceeds max body size ("
                                                + BUFFSIZE + " bytes)", true);
                            }
                        /*} else if (HTTPUtils.compareHeaderName("CONNECTION",line)) {
                            System.out.println(line);
                            // client wants connection to be closed after request
                            if (line.substring(11).trim().equalsIgnoreCase("close")) {
                                closeConn = true;
                            }*/
                        //} else if (HTTPUtils.isContentRange(line)) {
                        /*} else if (HTTPUtils.compareHeaderName("CONTENT-RANGE",line)) {
                            this.byteRange = line.substring(14).trim();
                        }*/
                        }

                    }

                    if (ftLine == null) {
                        // this is an empty request...ignore it
                        // TRANSITION
                        this.status = STATUS_IDLE;
                        this.requestHeaders = new StringBuilder();
                        continue;
                    }

                    if ( (ftLine.length() > 4)
                        && (ftLine.charAt(0) == 'H')
                        && (ftLine.charAt(1) == 'T')
                        && (ftLine.charAt(2) == 'T')
                        && (ftLine.charAt(3) == 'P') ) {
                    //if (ftLine.startsWith("HTTP")) {
                        // extract status code
                        try {
                            // HTTP/1.1 SCD Text
                            String tmp = ftLine.substring(9, 12);
                            Integer tmp2 = new Integer(tmp);
                            this.responseStatusCode = tmp2;
                        } catch (Exception ex) {
                            // no transition because con is closed anyway...
                            // invalidate all requests!
                            for (SpeedyRequest rq : this.sendQ) {
                                rq.status = SpeedyRequest.RequestStatus.FAILED;
                            }
                            throw new SpeedyException(
                                    "Invalid response line. Status code is not an integer or malformed."
                                            + ftLine, true);
                        }
                    } else {
                        // flush the entire buffer!
                        // no transition because con is closed anyway...
                        for (SpeedyRequest rq : this.sendQ) {
                            rq.status = SpeedyRequest.RequestStatus.FAILED;
                        }
                        throw new SpeedyException(
                                "Invalid response line. Status code is not an integer or malformed."
                                        + ftLine, true);
                    }
                    if (length > 0) {
                        // TRANISTION
                        this.status = STATUS_READ_BODY;
                        this.requestBody = BufferPool.allocate(length);
                        assert (this.requestBody.remaining() == length) :
                            "invalid buffer: remaining > length! capacity=" + this.requestBody.capacity();
                    } else {
                        // TRANSITION
                        this.status = STATUS_IDLE;
                        this.waitFor.responseHeaders = new HTTPHeaders();
                        this.waitFor.responseHeaders.parse(this.requestHeaders.toString());
                        this.requestHeaders = null;

                        this.waitFor.responseBody = null;
                        this.waitFor.statusCode = this.responseStatusCode;
                        this.waitFor.status = SpeedyRequest.RequestStatus.FINISHED;
                    }
                    break;
                }

                case STATUS_READ_BODY: {
                    // we assume the body to be raw data
                    if (data.remaining() <= this.requestBody.remaining()) {
                        this.requestBody.put(data);
                    } else {
                        int oldLimit = data.limit();
                        data.limit(data.position()+this.requestBody.remaining());
                        assert(oldLimit > data.limit());
                        this.requestBody.put(data);
                        data.limit(oldLimit);
                    }
                    /*while (data.hasRemaining() && this.requestBody.hasRemaining()) {
                        this.requestBody.put(data.get());
                    }*/
                    if (!this.requestBody.hasRemaining()) {
                        // TRANSITION
                        this.status = STATUS_IDLE;
                        if (this.waitFor == null) {
                            Logging.logMessage(Logging.LEVEL_ERROR,this,"this.waitFor is null!");
                            System.exit(1);
                        }
                        this.waitFor.responseHeaders = new HTTPHeaders();
                        this.waitFor.responseHeaders.parse(this.requestHeaders.toString());
                        this.requestHeaders = null;

                        this.waitFor.responseBody = this.requestBody;
                        this.requestBody = null;

                        this.waitFor.statusCode = this.responseStatusCode;
                        this.waitFor.status = SpeedyRequest.RequestStatus.FINISHED;
                    }
                }
            }
        }
    }

    void freeBuffers() {
        BufferPool.free(this.data);
        for (SpeedyRequest rq : this.sendQ) {
            rq.freeBuffer();
        }
    }

}
