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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.checksums.ChecksumFactory;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;

/**
 * Represents a request sent by the client and the response generated.
 *
 * @author bjko
 */
public class PinkyRequest {

    /**
     * the URI the client requested
     */
    public String requestURI;

    /**
     * the request HTTP method
     */
    public String requestMethod;

    /**
     * the headers sent by the client
     */
    public HTTPHeaders requestHeaders;

    /**
     * the body sent by the client
     */
    public ReusableBuffer requestBody;

    /**
     * request body length (can be smaller than requestBody.capacity() because of buffer reuse)
     */
    public int requestBdyLength;

    /**
     * if set to true the request is
     * requeued after Pinky has sent
     * the byte buffer. The requestBuffer is
     * not freed after sending!
     */
    public boolean    streaming;

    /**
     * This can be used by the
     * handler for streaming requests. This values
     * is not modified by Pinky.
     */
    public long       streamPosition;

    // -----

    public ReusableBuffer responseHeaders;

    public ReusableBuffer responseBody;


    int statusCode;

    boolean closeConnection;

    HTTPUtils.DATA_TYPE responseType;

    // -----
    // stuff used by the pinky server
    protected ConnectionState client;

    protected volatile boolean ready;

    public boolean responseSet;

    // for debugging only
    public int debugRqId = 0;
    long _receiveTime = -1;

    /** Creates a new instance of PinkyRequest */
    public PinkyRequest() {
        this.requestURI = "";
        this.requestMethod = "";
        this.requestHeaders = new HTTPHeaders();
        this.requestBody = null;

        this.responseHeaders = null;
        this.responseBody = null;
        this.statusCode = HTTPUtils.SC_OKAY;
        this.closeConnection = false;
        this.responseType = HTTPUtils.DATA_TYPE.JSON;

        this.client = null;
        this.ready = false;
        this.streaming = false;
        this.streamPosition = 0;
        this.responseSet = false;
    }

    /**
     * creates a new request with just a HTTP status code
     *
     * @param statusCode
     *            HTTP status code
     */
    public PinkyRequest(int statusCode) {
        this();
        this.statusCode = statusCode;
    }

    /**
     * creates a new request.
     *
     * @param requestMethod
     *            HTTP method
     * @param requestURI
     *            uri requested
     * @param requestByteRange
     *            byte raneg requested
     * @param requestHeaders
     *            headers sent by client
     */
    public PinkyRequest(String requestMethod, String requestURI,
            String requestByteRange, HTTPHeaders requestHeaders) {
        this();
        this.requestURI = requestURI;
        this.requestMethod = requestMethod;
        this.requestHeaders = requestHeaders;
    }

    /**
     * creates a new request with a body
     *
     * @param requestMethod
     *            HTTP method
     * @param requestURI
     *            uri requested
     * @param requestByteRange
     *            byte raneg requested
     * @param requestHeaders
     *            headers sent by client
     * @param requestBody
     *            the body data sent by the client
     */
    public PinkyRequest(String requestMethod, String requestURI,
            String requestByteRange, HTTPHeaders requestHeaders,
            ReusableBuffer requestBody) {
        this();
        this.requestURI = requestURI;
        this.requestMethod = requestMethod;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
    }

    /**
     * Sets the request URI and the request body.
     *
     * WARNING: The method also frees the former request body! All references to
     * the former request body become invalid.
     *
     * @param uri
     * @param body
     */
    public void setURIAndBody(String requestURI, ReusableBuffer requestBody) {
        this.requestURI = requestURI;

        BufferPool.free(this.requestBody);
        this.requestBody = requestBody;
    }

    /**
     * Associates the request with a connection
     *
     * @param client
     *            the client's connection
     */
    public void register(ConnectionState client) {
        this.client = client;
        client.pipeline.add(this);
    }

    /**
     * sets the status code to send back to the client
     *
     * @param statusCode
     *            HTTP status code
     */
    public void setResponse(int statusCode) {
        assert(this.responseSet == false) : "response already set";
        this.statusCode = statusCode;
        this.responseBody = null;
        this.responseHeaders = ReusableBuffer.wrap(HTTPUtils.getHeaders(
                this.statusCode, 0, this.responseType));
        this.responseSet = true;
    }

    /**
     * sets the response with a body (contents)
     *
     * @param statusCode
     *            HTTP status code to deliver to client
     * @param responseBody
     *            data to send to the client
     * @param responseType
     *            content type
     */
    public void setResponse(int statusCode, ReusableBuffer responseBody,
            HTTPUtils.DATA_TYPE responseType) {
        assert(this.responseSet == false) : "response already set";
        this.statusCode = statusCode;
        this.responseType = responseType;
        this.responseBody = responseBody;
        this.responseHeaders = ReusableBuffer.wrap(HTTPUtils.getHeaders(
                this.statusCode,
                (responseBody != null) ? responseBody.capacity() : 0,
                this.responseType));
        this.responseSet = true;
    }

    /**
     * sets the response with a string (contents)
     *
     * @param statusCode
     *            HTTP status code to deliver to client
     * @param responseBody
     *            data to send to the client
     * @param responseType
     *            content type
     */
    public void setResponse(int statusCode, String responseText) {
        assert(this.responseSet == false) : "response already set";
        this.statusCode = statusCode;
        this.responseType = HTTPUtils.DATA_TYPE.JSON;
        if (responseText != null)
            this.responseBody = ReusableBuffer.wrap(responseText.getBytes(HTTPUtils.ENC_UTF8));
        this.responseHeaders = ReusableBuffer.wrap(HTTPUtils.getHeaders(
                this.statusCode,
                (responseBody != null) ? responseBody.capacity() : 0,
                this.responseType));
        this.responseSet = true;
    }

    /**
     * sets the response with a body (contents)
     *
     * @param statusCode
     *            HTTP status code to deliver to client
     * @param responseBody
     *            data to send to the client
     * @param responseType
     *            content type
     * @param additionalheaders
     *            HTTPHeaders containing additional headers
     */
    public void setResponse(int statusCode, ReusableBuffer responseBody,
            HTTPUtils.DATA_TYPE responseType, HTTPHeaders additionalHeaders) {
        assert(this.responseSet == false) : "response already set";
        this.statusCode = statusCode;
        this.responseType = responseType;
        this.responseBody = responseBody;
        this.responseHeaders = ReusableBuffer.wrap(HTTPUtils.getHeaders(
                this.statusCode,
                (responseBody != null) ? responseBody.capacity() : 0,
                this.responseType,
                additionalHeaders == null? null: additionalHeaders.toString()));
        this.responseSet = true;
    }

    /**
     * Indicates if connection should be closed after delivering the response.
     *
     * @param closeAfterSend
     *            if true, connection is closed
     */
    public void setClose(boolean closeAfterSend) {
        this.closeConnection = closeAfterSend;
    }

    /** It provides the byte array of the body
     *  @return The array of bytes contained in the body of the request or null if there wasn't body in the message
     *
     *  @author Jesús Malo (jmalo)
     */
    public byte[] getBody() {
        byte[] body = null;

        if (requestBody != null) body = requestBody.array();

        return body;
    }

    /** check if the request has a response and can be sent
     */
    public boolean isReady() {
        return responseSet;
    }

    public void active() {
        this.client.active.set(true);
    }
    
    public InetSocketAddress getClientAddress() {
    	SocketAddress addr = this.client.channel.socket().getRemoteSocketAddress();
    	if(addr instanceof InetSocketAddress)
    		return (InetSocketAddress) addr;
    	else
    		return null;
    }

    public String toString() {
        String origin;
        try {
            origin = this.client.channel.socket().getRemoteSocketAddress().toString();
        }
        catch (NullPointerException ex) {
            origin = "UNKNOWN";
        }

        String resp = "null";
        if (this.responseBody != null) {
            if (this.responseBody.capacity() < 256) {
                resp = new String(this.responseBody.array());
            } else {
                resp = "too large ("+this.responseBody.capacity()+"), stream position: "+this.responseBody.position();
            }
        }

        String rq = "null";
        if (this.requestBody != null) {
            if (this.requestBody.capacity() < 256) {
                rq = new String(this.requestBody.array());
            } else {
                rq = "too large ("+this.requestBody.capacity()+"), stream position: "+this.requestBody.position();
            }
        }

        String respHdrs = null;
        if(responseHeaders != null)
            respHdrs = new String(responseHeaders.array());

        return "PinkyRequest from "+ origin +"\n"+
                "\tURI        "+this.requestURI+"\n"+
                "\tMethod     "+this.requestMethod+"\n"+
                "\t-----------\n"+
                "\tRqHdrs     "+this.requestHeaders+"\n"+
                "\tRqBody     "+rq+"\n"+
                "\t-----------\n"+
                "\tRespHdrs   "+respHdrs+"\n"+
                "\tRespBody   "+resp;

    }

    void freeBuffer() {
        BufferPool.free(this.requestBody);
        BufferPool.free(this.responseBody);
        BufferPool.free(this.responseHeaders);
        
        this.requestBody = null;
        this.responseHeaders = null;
        this.responseBody = null;
    }
    
    public ChannelIO getChannelIO() {
        return client.channel;
    }
    
    public boolean requestAuthentication(String username, String password) {
        if (client.httpDigestNounce == null) {
            client.httpDigestNounce = Long.toHexString(System.currentTimeMillis()+(long)(Math.random()*10000.0))+
                    Long.toHexString((long) (Math.random() * Long.MAX_VALUE));
        }
        if (client.userName == null) {
            String authHeader = this.requestHeaders.getHeader(HTTPHeaders.HDR_AUTHORIZATION);
            if (authHeader == null) {
                HTTPHeaders hdrs = new HTTPHeaders();
                hdrs.addHeader(HTTPHeaders.HDR_WWWAUTH, "Digest realm=\"xtreemfs\",opaque=\"ignoreme\",algorithm=\"MD5\",nonce=\""+client.httpDigestNounce+"\"");
                this.setResponse(HTTPUtils.SC_UNAUTHORIZED, null, HTTPUtils.DATA_TYPE.HTML,hdrs);
                return false;
            } else {
                System.out.println("hdr: "+authHeader);
                
                if (!authHeader.startsWith("Digest")) {
                    HTTPHeaders hdrs = new HTTPHeaders();
                    hdrs.addHeader(HTTPHeaders.HDR_WWWAUTH, "Digest realm=\"xtreemfs\",opaque=\"ignoreme\",algorithm=\"MD5\",nonce=\""+client.httpDigestNounce+"\"");
                    this.setResponse(HTTPUtils.SC_UNAUTHORIZED, null, HTTPUtils.DATA_TYPE.HTML,hdrs);
                    return false;
                }
                
                //check header...
                Pattern p = Pattern.compile("username=\\\"(\\S+)\\\"");
                Matcher m = p.matcher(authHeader);
                m.find();
                final String cUsername = m.group(1);
                
                p = Pattern.compile("uri=\\\"(\\S+)\\\"");
                m = p.matcher(authHeader);
                m.find();
                final String cURI = m.group(1);
                
                
                p = Pattern.compile("response=\\\"(\\S+)\\\"");
                m = p.matcher(authHeader);
                m.find();
                final String cResponse = m.group(1);
                
                p = Pattern.compile("nonce=\\\"(\\S+)\\\"");
                m = p.matcher(authHeader);
                m.find();
                final String cNonce = m.group(1);
                
                if (!cNonce.equals(client.httpDigestNounce)) {
                    HTTPHeaders hdrs = new HTTPHeaders();
                    hdrs.addHeader(HTTPHeaders.HDR_WWWAUTH, "Digest realm=\"xtreemfs\",opaque=\"ignoreme\",algorithm=\"MD5\",stale=true,nonce=\""+client.httpDigestNounce+"\"");
                    this.setResponse(HTTPUtils.SC_UNAUTHORIZED, null, HTTPUtils.DATA_TYPE.HTML,hdrs);
                    return false;
                }
                
                try {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    md5.update((username+":xtreemfs:"+password).getBytes());
                    byte[] digest = md5.digest();
                    final String HA1 = OutputUtils.byteArrayToHexString(digest).toLowerCase();

                    md5.update((this.requestMethod+":"+cURI).getBytes());
                    digest = md5.digest();
                    final String HA2 = OutputUtils.byteArrayToHexString(digest).toLowerCase();

                    md5.update((HA1+":"+client.httpDigestNounce+":"+HA2).getBytes());
                    digest = md5.digest();
                    final String response = OutputUtils.byteArrayToHexString(digest).toLowerCase();

                    if (!response.equals(cResponse)) {
                        HTTPHeaders hdrs = new HTTPHeaders();
                        hdrs.addHeader(HTTPHeaders.HDR_WWWAUTH, "Digest realm=\"xtreemfs\",opaque=\"ignoreme\",algorithm=\"MD5\",nonce=\""+client.httpDigestNounce+"\"");
                        this.setResponse(HTTPUtils.SC_UNAUTHORIZED, null, HTTPUtils.DATA_TYPE.HTML,hdrs);
                        return false;
                    }

                    client.userName = username;
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"channel authenticated as user "+client.userName);
                    return true;
                } catch (NoSuchAlgorithmException ex) {
                    this.setResponse(HTTPUtils.SC_SERVER_ERROR);
                    return false;
                }
            }
        } else {
            return true;
        }
    }

}

