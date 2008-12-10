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
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPUtils;

/**
 * Exception thrown by RPCClient for invalid
 * server responses (i.e. other than status code 200).
 * @author bjko
 */
public class HttpErrorException extends IOException {

    /**
     * The status code returned by the server.
     */
    protected final int statusCode;

    /**
     * The response body sent by the server or null if none was sent.
     */
    protected final byte[] responseBody;

    /**
     * Creates a new instance of HttpErrorException
     * @param statusCode the status code sent by the server
     * @param responseBody the body sent by the server
     */
    public HttpErrorException(int statusCode, byte[] responseBody) {
        super("status code is "+statusCode + ", error=" + new String(responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

     /**
     * Creates a new instance of HttpErrorException
     * @param statusCode the status code sent by the server
     */
    public HttpErrorException(int statusCode) {
        super("status code is "+statusCode);
        this.statusCode = statusCode;
        this.responseBody = null;
    }

    /**
     * Returns the status code sent by the server.
     * @return the status code sent by the server
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Returns the response body sent by the server.
     * @return the response body sent by the server
     */
    public byte[] getResponseBody() {
        return responseBody;
    }

    /**
     * Returns the response body's content parsed by the JSON parser.
     * @throws org.xtreemfs.foundation.json.JSONException if the body does not contain valid JSON
     * @return the object read from the body
     */
    public Object parseJSONResponse() throws JSONException {
        String body = new String(responseBody, HTTPUtils.ENC_UTF8);
        return JSONParser.parseJSON(new JSONString(body));
    }

    /**
     * A string representation of the exception.
     * @return a string representation of the exception.
     */
    public String toString() {
        if (responseBody != null)
            return this.getClass().getSimpleName()+": status code is "+statusCode+", error=" + new String(responseBody);
        else
            return this.getClass().getSimpleName()+": status code is "+statusCode;
    }
    
    public boolean authenticationRequest() {
        return this.statusCode == HTTPUtils.SC_UNAUTHORIZED;
    }

}
