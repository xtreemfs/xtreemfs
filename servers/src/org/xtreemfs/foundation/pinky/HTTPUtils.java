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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * Utilities for assembling HTTP messages.
 *
 * @author bjko
 */
public class HTTPUtils {

    /**
     * UTF8 encoding, used for JSON messages
     */
    public static final Charset ENC_UTF8           = Charset.forName("utf-8");

    /**
     * ASCII encoding, used for headers
     */
    public static final Charset ENC_ASCII          = Charset.forName("ascii");

    /**
     * HTTP Protocol version. Always
     *
     * <PRE>
     *
     * HTTP/1.1
     *
     * </PRE>
     */
    public static final String  HTTP_VER           = "HTTP/1.1";

    /**
     * Space character
     */
    public static final char    SP                 = ' ';

    /**
     * Newline in HTTP.
     */
    public static final String  CRLF               = "\r\n";

    /**
     * Content type for JSON content.
     */
    public static final String  JSON_TYPE          = "text/plain; charset=UTF-8";

    /**
     * Default encoding for textual data (like JSON).
     */
    public static final String  JSON_ENCODING      = HTTPHeaders.HDR_CONTENT_TYPE + ": "
                                                       + JSON_TYPE;

    /**
     * Content type for HTML content.
     */
    public static final String  HTML_TYPE          = "text/html; charset=UTF-8";

    /**
     * Default encoding for textual data (like JSON).
     */
    public static final String  HTML_ENCODING      = HTTPHeaders.HDR_CONTENT_TYPE + ": "
                                                       + HTML_TYPE;

    /**
     * Content type for binary content.
     */
    public static final String  BIN_TYPE           = "application/octet-stream";

    /**
     * Encoding for binary data.
     */
    public static final String  BIN_ENCODING       = HTTPHeaders.HDR_CONTENT_TYPE + ": " + BIN_TYPE;

    /**
     * Status code for success.
     */
    public static final int     SC_OKAY            = 200;

    /**
     * Status code for see other (redirect)
     */
    public static final int     SC_SEE_OTHER       = 303;

    /**
     * Status code for bad requests.
     */
    public static final int     SC_BAD_REQUEST     = 400;

    /**
     * Status code for 404.
     */
    public static final int     SC_NOT_FOUND       = 404;

    /**
     * Status code returned if a procedure threw a user exception (i.e. not a
     * server error). This is NOT standard HTTP/1.1 but custom codes are
     * possibel according to the standard.
     */
    public static final int     SC_USER_EXCEPTION  = 420;

    /**
     * Status code for internal server errors.
     */
    public static final int     SC_SERVER_ERROR    = 500;

    /**
     * Status code for methods not implemented in this server. These are nearly
     * all methods ;-)
     */
    public static final int     SC_NOT_IMPLEMENTED = 501;

    /**
     * Status code for service unavailable.
     */
    public static final int     SC_SERV_UNAVAIL    = 503;
    
    public static final int     SC_UNAUTHORIZED    = 401;

    /**
     * Content type
     */
    public static enum DATA_TYPE {
        /**
         * for binary data
         */
        BINARY(BIN_TYPE),
        /**
         * for JSON messages
         */
        JSON(JSON_TYPE),
        /**
         * for HTML messages
         */
        HTML(HTML_TYPE);
            
        private String name;
        DATA_TYPE(String name) {
            this.name = name;
        }
        @Override
        public String toString() {
            return name;
        }
    };

    // Tokens of the HTTP methods
    /**
     * Token of the GET method
     */
    public static final String GET_TOKEN    = "GET";

    /**
     * Token of the PUT method
     */
    public static final String PUT_TOKEN    = "PUT";

    /**
     * Token of the POST method
     */
    public static final String POST_TOKEN   = "POST";

    /**
     * Token of the DELETE method
     */
    public static final String DELETE_TOKEN = "DELETE";

    /**
     * Generate the textual message for a status code.
     *
     * @param statusCode
     *            status code
     * @return The String representing the status code.
     */
    public static String getReasonPhrase(int statusCode) {
        switch (statusCode) {
        case SC_OKAY:
            return "OK";
        case SC_SEE_OTHER:
            return "See Other";
        case SC_BAD_REQUEST:
            return "Bad Request";
        case SC_NOT_FOUND:
            return "Not Found";
        case SC_SERVER_ERROR:
            return "Internal Server Error";
        case SC_NOT_IMPLEMENTED:
            return "Not Implemented";
        default:
            return "";
        }
    }

    /**
     * Sends a response to a client.
     *
     * @param conn
     *            client
     * @param statusCode
     *            code to send
     * @param body
     *            the message body to send to the client
     * @param close
     *            if true the connection will be closed afterwards
     * @throws java.io.IOException
     *             passes all IOException from underlying IO primitives
     */
    public static void sendResponse(SocketChannel conn, int statusCode, String body, boolean close)
        throws IOException {
        sendResponse(conn, statusCode, body.getBytes(ENC_UTF8), close);
    }

    /**
     * Sends a response to a client.
     *
     * @param conn
     *            client
     * @param statusCode
     *            code to send
     * @param body
     *            the message body to send to the client
     * @param close
     *            if true the connection will be closed afterwards
     * @throws java.io.IOException
     *             passes all IOException from underlying IO primitives
     */
    public static void sendResponse(SocketChannel conn, int statusCode, byte[] body, boolean close)
        throws IOException {
        String hdr = HTTP_VER + SP + statusCode + SP + getReasonPhrase(statusCode) + CRLF;
        hdr += JSON_ENCODING + CRLF;

        // hdr += "Cache-Control: no-cache"+CRLF;
        if (body != null) {
            hdr += "Content-Length:" + SP + body.length + CRLF;

        } else
            hdr += "Content-Length: 0" + CRLF;
        if (close)
            hdr += "Connection: close" + CRLF;

        hdr += CRLF;

        // System.out.println("RQCONTENTS "+hdr);

        if (body == null) {
            conn.write(ByteBuffer.wrap(hdr.getBytes(ENC_ASCII)));
        } else {
            conn.write(ByteBuffer.wrap(hdr.getBytes(ENC_ASCII)));
            conn.write(ByteBuffer.wrap(body));
        }
    }

    /**
     * Sends a response to a client.
     *
     * @param bdyLen
     *            length of body data
     * @param type
     *            contents
     * @param conn
     *            client
     * @param statusCode
     *            code to send
     * @throws java.io.IOException
     *             passes all IOException from underlying IO primitives
     */
    public static void sendHeaders(SocketChannel conn, int statusCode, long bdyLen, DATA_TYPE type)
        throws IOException {
        String hdr = HTTP_VER + SP + statusCode + SP + getReasonPhrase(statusCode) + CRLF;
        switch (type) {
        case JSON:
            hdr += JSON_ENCODING + CRLF;
            break;
        case HTML:
            hdr += HTML_ENCODING + CRLF;
            break;
        default:
            hdr += BIN_ENCODING + CRLF;
        }

        // hdr += "Cache-Control: no-cache"+CRLF;
        hdr += "Content-Length:" + SP + bdyLen + CRLF;

        hdr += CRLF;
        // System.out.println("RQCONTENTS "+hdr);
        conn.write(ByteBuffer.wrap(hdr.getBytes(ENC_ASCII)));

    }

    /**
     * Sends a response to a client.
     *
     * @param bdyLen
     *            length of data
     * @param type
     *            content type
     * @param statusCode
     *            code to send
     * @return the headers to send
     */
    public static byte[] getHeaders(int statusCode, long bdyLen, DATA_TYPE type) {
        String hdr = HTTP_VER + SP + statusCode + SP + getReasonPhrase(statusCode) + CRLF;
        switch (type) {
        case JSON:
            hdr += JSON_ENCODING + CRLF;
            break;
        case HTML:
            hdr += HTML_ENCODING + CRLF;
            break;
        default:
            hdr += BIN_ENCODING + CRLF;
        }

        // hdr += "Cache-Control: no-cache"+CRLF;
        hdr += "Content-Length:" + SP + bdyLen + CRLF;

        // hdr += "X-DEBUG-RQID: "+rqID.getAndIncrement()+CRLF;

        hdr += CRLF;
        // System.out.println("RQCONTENTS "+hdr);
        return hdr.getBytes(ENC_ASCII);

    }

    /**
     * Sends a response to a client.
     *
     * @param bdyLen
     *            length of data
     * @param type
     *            content type
     * @param statusCode
     *            code to send
     * @return the headers to send
     */
    public static byte[] getHeaders(int statusCode, long bdyLen, DATA_TYPE type,
        String additionalHeaders) {
        String hdr = HTTP_VER + SP + statusCode + SP + getReasonPhrase(statusCode) + CRLF;
        switch (type) {
        case JSON:
            hdr += JSON_ENCODING + CRLF;
            break;
        case HTML:
            hdr += HTML_ENCODING + CRLF;
            break;
        default:
            hdr += BIN_ENCODING + CRLF;
        }

        // hdr += "Cache-Control: no-cache"+CRLF;
        hdr += "Content-Length:" + SP + bdyLen + CRLF;

        // hdr += "X-DEBUG-RQID: "+rqID.getAndIncrement()+CRLF;

        if (additionalHeaders != null)
            hdr += additionalHeaders;

        hdr += CRLF;
        // System.out.println("RQCONTENTS "+hdr);
        return hdr.getBytes(ENC_ASCII);

    }

    /**
     * Create a request.
     *
     * @param method
     *            HTTP method,
     *
     * <PRE>
     *
     * GET
     *
     * </PRE>
     *
     * or
     *
     * <PRE>
     *
     * PUT
     *
     * </PRE>
     *
     * @param URI
     *            the uri to request
     * @param byteRange
     *            the range to request, or null
     * @param bdyLen
     *            length of request body
     * @param type
     *            body content type
     * @return the headers to send before the body
     */
    public static byte[] getRequest(String method, String URI, String byteRange, String authString,
        long bdyLen, DATA_TYPE type, HTTPHeaders aHdrs) {
        String hdr = method + SP + URI + SP + HTTP_VER + CRLF;
        switch (type) {
        case JSON:
            hdr += JSON_ENCODING + CRLF;
            break;
        case HTML:
            hdr += HTML_ENCODING + CRLF;
            break;
        default:
            hdr += BIN_ENCODING + CRLF;
        }

        // hdr += "Cache-Control: no-cache"+CRLF;
        // hdr += "Host: localhost"+CRLF;
        hdr += HTTPHeaders.HDR_CONTENT_LENGTH + ":" + SP + bdyLen + CRLF;
        if (authString != null)
            hdr += HTTPHeaders.HDR_AUTHORIZATION + ":" + SP + authString + CRLF;
        if (byteRange != null) {
            hdr += HTTPHeaders.HDR_CONTENT_RANGE + ":" + SP + byteRange + CRLF;
        }

        if (aHdrs != null)
            hdr += aHdrs.toString();

        hdr += CRLF;
        // System.out.println("RQCONTENTS "+hdr);
        return hdr.getBytes(ENC_ASCII);

    }

    /**
     * It creates a request without body. The user must specify the required
     * headers.
     *
     * @param method
     *            The HTTP method of the request
     * @param URI
     *            URI field of the new request
     * @param headers
     *            Headers of the request
     */
    public static byte[] getRequest(String method, String URI, HTTPHeaders headers) {
        String hdr = method + SP + URI + SP + HTTP_VER + CRLF;
        hdr += headers.toString();

        hdr += CRLF;
        return hdr.getBytes(ENC_ASCII);
    }

    /**
     * @header header to look for. Must be UPPER CASE!
     */
    public static boolean compareHeaderName(String header, String line) {
        if (header.length() > line.length())
            return false;

        for (int i = 0; i < header.length(); i++) {
            if (header.charAt(i) != Character.toUpperCase(line.charAt(i)))
                return false;
        }
        return true;
    }

    public static boolean isContentLength(String line) {
        // Content-length
        return (line.charAt(0) == 'C' || line.charAt(0) == 'c')
            && (line.charAt(6) == 't' || line.charAt(6) == 'T')
            && (line.charAt(13) == 'h' || line.charAt(13) == 'H');

    }

    public static boolean isContentRange(String line) {
        return (line.charAt(0) == 'C' || line.charAt(0) == 'c')
            && (line.charAt(6) == 't' || line.charAt(6) == 'T')
            && (line.charAt(12) == 'e' || line.charAt(12) == 'E');

    }

}
