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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.utils;

import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.json.JSONCharBufferString;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.UserException;


/**
 * Routines for marshalling and unmarshalling JSON in request bodies.
 * 
 * @author bjko
 */
public class MessageUtils {
    
    public static void marshallResponse(MRCRequest req, Object res) {
        
        assert (req != null);
        
        try {
            ReusableBuffer bbuf = ReusableBuffer.wrap(JSONParser.writeJSON(res).getBytes(
                HTTPUtils.ENC_UTF8));
            req.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY, bbuf, HTTPUtils.DATA_TYPE.JSON);
            
        } catch (JSONException exc) {
            marshallException(req, exc);
        }
    }
    
    public static void marshallResponse(MRCRequest req, Object res, HTTPHeaders additionalHeaders) {
        
        assert (req != null);
        
        try {
            ReusableBuffer bbuf = ReusableBuffer.wrap(JSONParser.writeJSON(res).getBytes(
                HTTPUtils.ENC_UTF8));
            req.getPinkyRequest()
                    .setResponse(HTTPUtils.SC_OKAY, bbuf, HTTPUtils.DATA_TYPE.JSON,
                        additionalHeaders);
            
        } catch (JSONException exc) {
            marshallException(req, exc);
        }
    }
    
    public static void marshallException(MRCRequest req, Map<String, Object> excMap,
        boolean userException) {
        try {
            
            ReusableBuffer body = ReusableBuffer.wrap(JSONParser.writeJSON(excMap).getBytes(
                HTTPUtils.ENC_UTF8));
            
            req.getPinkyRequest().setResponse(userException ? HTTPUtils.SC_USER_EXCEPTION
                : HTTPUtils.SC_SERVER_ERROR, body, HTTPUtils.DATA_TYPE.JSON);
        } catch (JSONException ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG, null, "cannot marshall exception");
            Logging.logMessage(Logging.LEVEL_ERROR, null, ex);
            req.getPinkyRequest().setResponse(HTTPUtils.SC_SERVER_ERROR);
        }
    }
    
    public static void marshallException(MRCRequest req, Throwable exc) {
        
        String stackTrace = null;
        
        // encapsulate the stack trace in a string, unless the exception is a
        // user exception
        if (!(exc instanceof UserException))
            stackTrace = OutputUtils.stackTraceToString(exc);
        
        Map<String, Object> excMap = new HashMap<String, Object>();
        excMap.put("exceptionName", exc.toString());
        excMap.put("errorMessage", exc.getMessage());
        excMap.put("stackTrace", stackTrace);
        if (exc instanceof UserException)
            excMap.put("errno", ((UserException) exc).getErrno());
        
        marshallException(req, excMap, exc instanceof UserException);
    }
    
    public static void setRedirect(MRCRequest req, String target) {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_LOCATION);
        req.getPinkyRequest().setResponse(HTTPUtils.SC_SEE_OTHER, null, DATA_TYPE.JSON, headers);
    }
    
    public static Object unmarshallRequestOld(MRCRequest request) throws JSONException {
        String body = null;
        
        assert (request != null);
        assert (request.getPinkyRequest() != null);
        
        if (request.getPinkyRequest().requestBody != null) {
            byte bdy[] = null;
            if (request.getPinkyRequest().requestBody.hasArray()) {
                bdy = request.getPinkyRequest().requestBody.array();
            } else {
                bdy = new byte[request.getPinkyRequest().requestBody.capacity()];
                request.getPinkyRequest().requestBody.position(0);
                request.getPinkyRequest().requestBody.get(bdy);
            }
            
            body = new String(bdy, HTTPUtils.ENC_UTF8);
            return JSONParser.parseJSON(new JSONString(body));
        } else {
            return null;
        }
        
    }
    
    public static Object unmarshallRequest(MRCRequest request) throws JSONException {
        String body = null;
        
        assert (request != null);
        assert (request.getPinkyRequest() != null);
        
        if (request.getPinkyRequest().requestBody != null) {
            request.getPinkyRequest().requestBody.position(0);
            CharBuffer utf8buf = HTTPUtils.ENC_UTF8.decode(request.getPinkyRequest().requestBody.getBuffer());
            return JSONParser.parseJSON(new JSONCharBufferString(utf8buf));
        } else {
            return null;
        }
        
    }
    
    public static InetSocketAddress addrFromString(String hostAndPort)
        throws IllegalArgumentException {
        int dpoint = hostAndPort.lastIndexOf(':');
        if (dpoint == -1) {
            throw new IllegalArgumentException("InetSocketAddress as String needs a : character");
        }
        String host = hostAndPort.substring(0, dpoint);
        int port = 0;
        try {
            port = Integer.valueOf(hostAndPort.substring(dpoint + 1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Port is not a number in " + hostAndPort);
        }
        return new InetSocketAddress(host, port);
    }
    
}
