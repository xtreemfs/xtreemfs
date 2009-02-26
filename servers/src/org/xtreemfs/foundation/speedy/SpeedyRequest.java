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

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xtreemfs.common.Request;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.new_mrc.MRCRequest;

/**
 * An HTTP request
 *
 * @author bjko
 */
public class SpeedyRequest {

    /**
     * The request URI
     */
    String  requestURI;

    /**
     * The request HTTP method (GET, PUT...)
     */
    String  requestMethod;

    /**
     * All headers included in the request part
     */
    HTTPHeaders requestHttpHeaders;
    
    /**
     * The request header including the request line and separating double CRLF.
     */
    ReusableBuffer requestHeaders;
    
    /**
     * The request body, can be null if there is no body data to be sent.
     */
    ReusableBuffer requestBody;

    public long sendStart;

    /**
     * A listener for a single request. This is not directly supported by Speedy
     * but might be by clients built using Speedy
     */
    public SpeedyResponseListener listener;

    /**
     * time in milliseconds when the response was received (for debugging
     * purposes only)
     */
    public long       received;

    // -----

    /**
     * the headers sent by the server as response
     */
    public HTTPHeaders     responseHeaders;

    /**
     * the body the server sent as response
     */
    public ReusableBuffer responseBody;

    /**
     * status code sent by the server
     *
     * @see org.xtreemos.wp34.mrc.pinky.HTTPUtils
     */
    public int        statusCode;

    public boolean    listenerNotified;

    /* for debugging only */
    public int        _debug_contentLength;
    long              _id;

    /**
     * Status of a request
     */
    public enum RequestStatus {
        /**
         * Request is being processed but not sent yet.
         */
        PENDING,
        /**
         * The request is currently transferred to the server and Speedy is
         * waiting for the response.
         */
        SENDING,
        /**
         * The request was sent and Speedy is waiting for a response
         */
        WAITING,
        /**
         * Speedy has received a valid response from the server. The request hs
         * reached the end of it's lifecycle.
         */
        FINISHED,
        /**
         * Speedy has received an invalid response from the server or other
         * problems occured while executing the request. The request hs reached
         * the end of it's lifecycle.
         */
        FAILED };
    /**
     * the current status of the request
     */
    public RequestStatus    status;

    public MRCRequest          attachment;

    public Object           genericAttatchment;

    /**
     * original OSD-request (used for subrequests)
     */
    private Request originalRequest;

    // -----
    // stuff used by the speedy client

    boolean               ready;

    ConnectionState       con;

    public int            timeout;

    public int            waited;


    /**
     * creates a new request
     *
     * @param requestMethod
     *            HTTP method
     * @param requestURI
     *            uri to request from server
     * @param range
     *            byte range to request or null
     */
    public SpeedyRequest(String requestMethod, String requestURI, String range, String authString) {
        this(requestMethod,requestURI,range,authString,null,null,null);
    }

    /**
     * creates a new request
     *
     * @param requestMethod
     *            HTTP method
     * @param requestURI
     *            uri to request from server
     * @param range
     *            byte range to request or null
     * @param requestBody
     *            the body to send as part of the request, or null
     * @param type
     *            the content type
     */
    public SpeedyRequest(String requestMethod, String requestURI, String range, String authString, ReusableBuffer requestBody, HTTPUtils.DATA_TYPE type) {
        this(requestMethod,requestURI,range,authString,requestBody,type,null);
    }

    /** Creates a new request
     *  @param method Requested method
     *  @param URI Requested URI
     *  @param headers Headers for the request
     *  @autor Jesús Malo (jmalo)
     */
    public SpeedyRequest(String method, HTTPHeaders headers, String URI) {
        this(method,URI,null,null,null,null,headers);
    }

    /**
     * creates a new request
     *
     * @param requestMethod
     *            HTTP method
     * @param requestURI
     *            uri to request from server
     * @param range
     *            byte range to request or null
     * @param requestBody
     *            the body to send as part of the request, or null
     * @param type
     *            the content type
     */
    public SpeedyRequest(String requestMethod, String requestURI,
                         String range, String authString,
                         ReusableBuffer requestBody,
                         HTTPUtils.DATA_TYPE type,
                         HTTPHeaders additionalHeaders) {
        
        assert(requestMethod != null);
        assert(requestURI != null);
        
        this.requestURI = requestURI;
        this.requestMethod = requestMethod;
        
        //prepare header fields
        this.requestHttpHeaders = new HTTPHeaders();
            
        if (range != null)
            this.requestHttpHeaders.addHeader(HTTPHeaders.HDR_CONTENT_RANGE, range);
        
        if (authString != null)
            this.requestHttpHeaders.addHeader(HTTPHeaders.HDR_AUTHORIZATION, authString);
     
        if (requestBody != null) {
            this.requestHttpHeaders.addHeader(HTTPHeaders.HDR_CONTENT_TYPE, type.toString());
            this.requestHttpHeaders.addHeader(HTTPHeaders.HDR_CONTENT_LENGTH, requestBody.capacity());
        }
        
        //additional headers can override
        if (additionalHeaders != null)
            this.requestHttpHeaders.copyFrom(additionalHeaders);
        
        
        this.requestHeaders = constructRequest();
        
        this.requestBody = requestBody;

        initializeInternals();

        _debug_contentLength = (requestBody == null) ? 0 : requestBody.capacity();
    }

    private void initializeInternals() {
        this.responseHeaders = null;
        this.responseBody = null;
        this.statusCode = HTTPUtils.SC_OKAY;
        this.ready = false;
        this.listenerNotified = false;
        this.timeout = 0;
    }
    
    private ReusableBuffer constructRequest() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.requestMethod);
        sb.append(" ");
        sb.append(this.requestURI);
        sb.append(" ");
        sb.append(HTTPUtils.HTTP_VER);
        sb.append(HTTPUtils.CRLF);
        this.requestHttpHeaders.append(sb);
        sb.append(HTTPUtils.CRLF);
        return ReusableBuffer.wrap(sb.toString().getBytes(HTTPUtils.ENC_ASCII));
        
    }
    
    /**
     * Associates a request with a connection
     *
     * @param con
     *            connection to associate this request with
     */
    public void registerConnection(ConnectionState con) {
        this.con = con;
    }

    public InetSocketAddress getServer() {
        if (this.con != null) {
            return this.con.endpoint;
        } else {
            return null;
        }
    }

    public void registerListener(SpeedyResponseListener rl) {
        this.listener = rl;
    }

    /** It provides the byte array of the body
     *  @return The array of bytes contained in the body of the request or null if there wasn't body in the message
     *
     *  @author Jesús Malo (jmalo)
     */
    public byte[] getBody() {
        byte body[] = null;

        if (requestBody != null) {
            if (requestBody.hasArray()) {
                body = requestBody.array();
            } else {
                body = new byte[requestBody.capacity()];
                requestBody.position(0);
                requestBody.get(body);
            }
        }

        return body;
    }

    /** It provides the byte array of the body
     *  @return The array of bytes contained in the body of the response or null if there wasn't body in the message
     *
     *  @author Jesús Malo (jmalo)
     */
    public byte[] getResponseBody() {

        if (responseBody != null) {
            return responseBody.array();
        }

        return null;
    }

    public void freeBuffer() {
        BufferPool.free(requestBody);
        requestBody = null;
        BufferPool.free(requestHeaders);
        requestHeaders = null;
        BufferPool.free(responseBody);
        responseBody = null;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public String toString() {
        String hdrs = (this.requestHeaders == null) ? null : new String(this.requestHeaders.array());
        String bdy = (this.requestBody == null) ? null : new String(this.requestBody.array());
        return this.requestURI+"\n"+
               hdrs+"\n"+
               bdy+"\n"+
               this.responseHeaders+"\n"+
                (this.responseBody != null ?
               new String(this.responseBody.array()) : "empty");
    }

    public Request getOriginalRequest() {
            return this.originalRequest;
    }

    public void setOriginalRequest(Request osdRequest) {
            this.originalRequest = osdRequest;
    }
    
    public String getURI() {
        return requestURI;
    }
    
    public String getMethod() {
        return this.requestMethod;
    }
    
    /**
     * Prepares the request for being resent with digest authentication
     * @param username
     * @param password
     */
    public void addDigestAuthentication(String username, String password) {
        //reset request & buffers
        requestBody.flip();
        assert(requestBody.position() == 0);
        initializeInternals();
        
        
        String serverAuthHdr = responseHeaders.getHeader(HTTPHeaders.HDR_WWWAUTH);
        
        //parse headers
        final String hdrStr = new String(requestHeaders.array());
        
        final String credentials = createCredentials(serverAuthHdr,username,password);
        
        this.requestHttpHeaders.setHeader(HTTPHeaders.HDR_AUTHORIZATION, credentials);
        
        this.constructRequest();
    }
    
    private String createCredentials(String authHeader, String authUsername, String authPassword) {
        //check header...
        if ((authHeader == null) || (authHeader.length() == 0))
                return null;
        
        try {
            System.out.println("header: "+authHeader);

            final String cURI = this.requestURI;

            Pattern p = Pattern.compile("nonce=\\\"(\\S+)\\\"");
            Matcher m = p.matcher(authHeader);
            m.find();
            final String cNonce = m.group(1);
        
        
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update((authUsername+":xtreemfs:"+authPassword).getBytes());
            byte[] digest = md5.digest();
            final String HA1 = OutputUtils.byteArrayToHexString(digest).toLowerCase();

            md5.update((this.requestMethod+":"+cURI).getBytes());
            digest = md5.digest();
            final String HA2 = OutputUtils.byteArrayToHexString(digest).toLowerCase();

            md5.update((HA1+":"+cNonce+":"+HA2).getBytes());
            digest = md5.digest();
            return OutputUtils.byteArrayToHexString(digest).toLowerCase();
        } catch (Exception ex) {
            return null;
        }
    }
    
}
