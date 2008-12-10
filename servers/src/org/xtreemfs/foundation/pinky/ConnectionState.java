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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.foundation.pinky;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;

/**
 * ontains a buffer and an active marker for each connection. Includes also the
 * parsing routine and state machine state.
 *
 * @author bjko
 */
public class ConnectionState {

    /**
     * Size of input buffer
     */
    public static final int BUFFSIZE = 1024 * 64;


    /**
     * Maximum body size to accept.
     */
    public static final int MAX_BODY_SIZE = 1024 * 1024 * 10;

    /**
     * Maximum header size
     */
    public static final int MAX_HDR_SIZE = 4096;

    /**
     * Initial Size of buffer for headers
     */
    public static final int INIT_HDR_BUF = 128;

    /**
     * Buffer holding the incomming data
     */
    public final ReusableBuffer data;

    /**
     * The channel associated w/ this connection
     */
    public final ChannelIO channel;

    /**
     * will be set to false by a periodical clean up task. Inactive connections
     * (timed out) will be closed and removed.
     */
    public final AtomicBoolean active;

    /**
     * Buffer for receiving the headers.
     */
    public StringBuilder requestHeaders;

    /**
     * The data of the request that is currently being parsed
     * may be incomplete while parsing
     */
    private PinkyRequest currentRq;

    /**
     * The request that should be sent to the client.
     */
    public PinkyRequest toSend;

    public long         remainingBytes = 0;

    public ByteBuffer sendData[];

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
    private int status;

    /**
     * The request pipeline for that connection.
     */
    public final List<PinkyRequest> pipeline;

    private boolean closed;
    
    private int length;
    
    /**
     * set to a non-null value when the channel has been authenticated with a
     * HTTP Digest Authentication
     */
    public String userName;
    
    /**
     * used to store the nounce used in HTTP digest authentication
     */
    public String httpDigestNounce;
   
    
    

    /**
     * Creates a new instance of ConnectionStatus
     *
     * @param channel
     *            the channel to which this state object belongs.
     */
    public ConnectionState(ChannelIO channel) {

        closed = false;

        active = new AtomicBoolean(true);

        this.channel = channel;

        //data = ByteBuffer.allocateDirect(BUFFSIZE);
        data = BufferPool.allocate(BUFFSIZE);

        this.status = STATUS_IDLE;

        toSend = null;

        pipeline = new LinkedList();

    }

    /**
     * This is the main parsing method. It parses the available data in the
     * buffer.
     *
     * @return a list of requests or null if there is no request available
     */
    public PinkyRequest processBuffer() {

        // loop until data is empty
        while (data.hasRemaining()) {

            switch (this.status) {
                case  STATUS_IDLE: {
                    // prepare request
                    this.requestHeaders = new StringBuilder(INIT_HDR_BUF);
                    this.status = STATUS_READ_HEADERS;
                    this.currentRq = new PinkyRequest();
                    // TRANSITION
                    break;
                }

                case STATUS_READ_HEADERS: {
                    char ch = (char) (data.get() & 0xFF);
                    if (ch == '\n') {
                        // TRANSITION
                        this.requestHeaders.append(ch);
                        this.status = STATUS_CRLF;
                    } else if (ch != '\r') {
                        // ignore \r s
                        this.requestHeaders.append(ch);
                        // check for overflows...
                        if (this.requestHeaders.length() >= MAX_HDR_SIZE) {
                            PinkyRequest rq = new PinkyRequest(HTTPUtils.SC_BAD_REQUEST);
                            rq.setResponse(HTTPUtils.SC_BAD_REQUEST);
                            rq.register(this);
                            rq.setClose(true);
                            rq.responseSet = true;
                            Logging.logMessage(Logging.LEVEL_DEBUG,this,"Bad Request: Close channel for "
                                    + this.channel.socket()
                                    .getRemoteSocketAddress()+", max header size exceeded");
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "header parsed so far: " + requestHeaders);
                            this.data.limit(0);
                            return rq;
                        }
                    }
                    break;
                }

                case STATUS_CRLF: {
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
                    //no break here! fallthroug is required for requests w/
                    //empty body.
                }

                case STATUS_PARSE_REQUEST: {
                    // if there is a content length field, try to read the body
                    int nextNL = this.requestHeaders.indexOf("\n");
                    int cPos = 0;
                    String ftLine = null;
                    //int length = 0;

                    while (nextNL != -1) {

                        String line = this.requestHeaders.substring(cPos, nextNL);
                        cPos = nextNL + 1;
                        nextNL = this.requestHeaders.indexOf("\n", cPos);

                        if (ftLine == null)
                            ftLine = line;
                        else {
                            this.currentRq.requestHeaders.addHeader(line);
                        }
                    }

                    //check some important headers
                    String cLength = this.currentRq.requestHeaders.getHeader(HTTPHeaders.HDR_CONTENT_LENGTH);
                    if (cLength != null) {
                        try {
                            length = Integer.valueOf(cLength);
                            if (length > MAX_BODY_SIZE)
                                throw new RuntimeException("Too Long");
                        } catch (Exception ex) {
                            // no transition because con is closed anyway...
                            PinkyRequest rq = new PinkyRequest(
                                    HTTPUtils.SC_BAD_REQUEST);
                            rq.setResponse(HTTPUtils.SC_BAD_REQUEST);
                            rq.register(this);
                            rq.setClose(true);
                            rq.responseSet = true;
                            Logging.logMessage(Logging.LEVEL_DEBUG,this,"Bad Request: Close channel for "
                                    + this.channel.socket().getRemoteSocketAddress()+", Content-Length no integer: "+cLength+"/"+ex);
                            this.data.limit(0);
                            return rq;
                        }
                    } else {
                        //no body
                        length = 0;
                    }

                    String conClose = this.currentRq.requestHeaders.getHeader("Connection");
                    if (conClose != null) {
                        if (conClose.equalsIgnoreCase("close")) {
                            this.currentRq.closeConnection = true;
                        }
                    }

                    if (ftLine == null) {
                        // this is an empty request...ignore it
                        // TRANSITION
                        this.status = STATUS_IDLE;
                        continue;
                    }

                    //parse request line
                    int firstSpace = ftLine.indexOf(' ');
                    int lastSpace = ftLine.lastIndexOf(' ');
                    if ((firstSpace == -1) || (firstSpace == lastSpace)) {
                        PinkyRequest rq = new PinkyRequest(
                                        HTTPUtils.SC_BAD_REQUEST);
                        rq.setResponse(HTTPUtils.SC_BAD_REQUEST);
                        rq.register(this);
                        rq.setClose(true);
                        rq.responseSet = true;
                        Logging.logMessage(Logging.LEVEL_DEBUG,this,"Bad Request (malformed request line): Close channel for "
                                + this.channel.socket().getRemoteSocketAddress()+", not a valid request line: "+ftLine);
                        this.data.limit(0);
                        return rq;
                    } else {
                        this.currentRq.requestMethod = ftLine.substring(0, firstSpace);
                        this.currentRq.requestURI = ftLine.substring(firstSpace+1, lastSpace);
                    }
                    //IGNORE the HTTP/1.1 for now

                    if ( !this.currentRq.requestMethod.equals("GET") &&
                            !this.currentRq.requestMethod.equals("PUT") &&
                            !this.currentRq.requestMethod.equals("POST") &&
                            !this.currentRq.requestMethod.equals("DELETE") &&
                            !this.currentRq.requestMethod.equals("HEAD") ) {
                        //bad request
                        // flush the entire buffer!
                        // no transition because con is closed anyway...
                        PinkyRequest rq = new PinkyRequest(HTTPUtils.SC_NOT_IMPLEMENTED);
                        rq.setResponse(HTTPUtils.SC_NOT_IMPLEMENTED);
                        rq.register(this);
                        rq.setClose(true);
                        rq.responseSet = true;
                        Logging.logMessage(Logging.LEVEL_DEBUG,this,"Not Implemented: Close channel for "
                                + this.channel.socket()
                                .getRemoteSocketAddress()+" line is "+ftLine);
                        this.data.limit(0);
                        return rq;
                    }

                    if (length > 0) {
                        // TRANISTION
                        this.status = STATUS_READ_BODY;
                        this.currentRq.requestBody = BufferPool.allocate(length);
                    } else {
                        // TRANSITION
                        this.status = STATUS_IDLE;
                        //String hdrs = this.requestHeaders.toString();
                        //set to null to allow GC
                        this.requestHeaders = null;

                        PinkyRequest tmp = currentRq;
                        currentRq = null;
                        tmp.register(this);
                        return tmp;
                    }
                    break;
                }

                case STATUS_READ_BODY: {
                    // we assume the body to be raw data
                    if (data.remaining() <= this.currentRq.requestBody.remaining()) {
                        this.currentRq.requestBody.put(data);
                    } else {
                        int oldLimit = data.limit();
                        data.limit(data.position()+this.currentRq.requestBody.remaining());
                        assert(oldLimit > data.limit());
                        this.currentRq.requestBody.put(data);
                        data.limit(oldLimit);
                    }
                    if (!this.currentRq.requestBody.hasRemaining()) {
                        // TRANSITION
                        this.status = STATUS_IDLE;
                        //String hdrs = this.requestHeaders.toString();
                        this.requestHeaders = null;
                        PinkyRequest rq = currentRq;
                        currentRq = null;
                        rq.register(this);
                        return rq;
                    }
                    break;
                }

                default : {
                    Logging.logMessage(Logging.LEVEL_ERROR,this,"Programmatic ERROR!");
                    return null;
                }
            }
        }
        return null;
    }

    public void freeBuffers() {
        if (this.closed)
            return;
        BufferPool.free(this.data);
        for (PinkyRequest rq : this.pipeline) {
            rq.freeBuffer();
        }
        this.closed = true;
    }

}
