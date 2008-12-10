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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyRequest;

/**
 * Generic RPC over HTTP client. Can send JSON and binary requests via HTTP.
 * Can also be used as a very simple HTTP client.
 * @author bjko
 */
public class RPCClient {

   /**
     * The speedy used by the client.
     */
    private final MultiSpeedy speedy;

    /**
     * timeout to use for RPCs
     */
    private int timeout;

    /**
     * default timeout used
     */
    public static final int DEFAULT_TIMEOUT = 10000;

    /**
     * Creates a new client with a private speedy instance.
     * @throws java.io.IOException if speedy cannot be started.
     */
    public RPCClient() throws IOException {
        this(null);
    }

    /**
     * Creates a new instance of the RPCClient
     * @param sharedSpeedy a speedy shared among several clients. If null, a new speedy instance is created.
     * @throws java.io.IOException if speedy cannot be started
     */
    public RPCClient(final MultiSpeedy sharedSpeedy)
            throws IOException {
        
        this.timeout = DEFAULT_TIMEOUT;

        if (sharedSpeedy != null) {
            speedy = sharedSpeedy;
        } else {
            speedy = new MultiSpeedy();
            speedy.start();
        }

        Thread.yield();
    }

    /**
     * Creates a new instance of the RPCClient
     * @param sharedSpeedy a speedy shared among several clients. If null, a new speedy instance is created.
     * @throws java.io.IOException if speedy cannot be started
     */
    public RPCClient(MultiSpeedy sharedSpeedy, int timeout)
            throws IOException {
        this(sharedSpeedy);
        this.timeout = timeout;
    }

    /**
     * Creates a new instance of the RPCClient
     * A new speedy instance with SSL support will be created.
     * @param sslOptions options for ssl connection, null for no SSL
     * @throws java.io.IOException if speedy cannot be started
     */
    public RPCClient(int timeout, SSLOptions sslOptions)
    		throws IOException {
        speedy = new MultiSpeedy(sslOptions);
        speedy.start();

        this.timeout = timeout;
        Thread.yield();
    }

    /**
     * Shuts down the speedy used by this client.
     * @attention Shuts down the speedy also if it is shared!
     */
    public void shutdown() {
        speedy.shutdown();
    }

    public void waitForShutdown() {
        try {
            speedy.waitForShutdown();
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
        }
    }

    /**
     * Sends an xtreemfs JSON RPC request.
     * @param authString authentication string to send to the remote server
     * @param addHdrs additional headers to include in the request.
     * Cannot override headers set by the HTTP client automatically.
     * @param server the server to send the request to
     * @param method the RPC method to call (which is the URI sent as part of the HTTP request).
     * @param data The request's parameters. If null, an empty body is sent. If data is a ReusableBuffer
     * the data is sent as a binary body. Everything else is sent as a JSON encoded
     * object.
     * @return a RPCResponse for asynchrous requests
     * @throws org.xtreemfs.foundation.json.JSONException if data cannot be translated into a JSON object.
     * @throws java.io.IOException if the request cannot be sent.
     */
    public RPCResponse sendRPC(InetSocketAddress server,
            String method, Object data, String authString,
            HTTPHeaders addHdrs) throws IOException,JSONException {
        
        if (data == null) {
            return send(server, method, null, addHdrs, authString, HTTPUtils.DATA_TYPE.JSON,HTTPUtils.POST_TOKEN);
        } else {
            ReusableBuffer body = null;
            HTTPUtils.DATA_TYPE type = HTTPUtils.DATA_TYPE.JSON;
            if (data instanceof ReusableBuffer) {
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"request body contains binary data");
                body = (ReusableBuffer)data;
                type = HTTPUtils.DATA_TYPE.BINARY;
            } else {
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"request body contains JSON data");
                String json = JSONParser.writeJSON(data);
                body = ReusableBuffer.wrap(json.getBytes(HTTPUtils.ENC_UTF8));
            }
            return send(server, method, body, addHdrs,authString, type,HTTPUtils.POST_TOKEN);
        }
    }

    /**
     * Uses the underlying speedy to check if the server is blocked because it is not
     * responding.
     * @param server the server to check
     * @return true, if server is not blocked, false otherwise
     * @see MultiSpeedy
     */
    public boolean serverIsAvailable(InetSocketAddress server) {
        return speedy.serverIsAvailable(server);
    }


    /**
     * internal method for sending requests.
     */
    protected RPCResponse send(InetSocketAddress server, String uri,
            ReusableBuffer body, HTTPHeaders headers, String authString,
            HTTPUtils.DATA_TYPE type, String httpMethod)
            throws IOException {

        assert(uri != null);
        //FIXME: should be activated
        //assert(authString != null);

        SpeedyRequest sr = null;

        if (body != null) {
            if (headers != null) {
                sr = new SpeedyRequest(httpMethod, uri, null, authString, body,
                        type, headers);
            } else {
                sr = new SpeedyRequest(httpMethod, uri, null, authString, body,
                        type);
            }
        } else {
            if (headers != null) {
                sr = new SpeedyRequest(httpMethod, uri, null, authString, null,
                        type, headers);
            } else {
                sr = new SpeedyRequest(httpMethod, uri, null, authString );
            }
        }
        sr.setTimeout(timeout);
        RPCResponse resp = new RPCResponse(sr,server);
        sr.listener = resp;
        synchronized (speedy) {
            speedy.sendRequest(sr, server);
        }
        return resp;
    }

    public MultiSpeedy getSpeedy() {
        return speedy;
    }

    /**Generates a HashMap from the arguments passed.
     * e.g. generateMap("key1",value1,"key2",value2)
     */
    public static Map<String,Object> generateMap(Object ...args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("require even number of arguments (key1,value1,key2,value2...)");
        }
        Map<String,Object> m = new HashMap(args.length/2);
        for (int i = 0; i < args.length; i = i+2) {
            m.put((String)args[i],args[i+1]);
        }
        return m;
    }

    /** Generates a list from the arguments passed.
     */
    public static List<Object> generateList(Object ...args) {
        List<Object> l = new ArrayList(args.length);
        for (int i = 0; i < args.length; i++) {
            l.add(args[i]);
        }
        return l;
    }

    /** Generates a list from the string arguments passed.
     */
    public static List<String> generateStringList(String ...args) {
        List<String> l = new ArrayList(args.length);
        for (int i = 0; i < args.length; i++) {
            l.add(args[i]);
        }
        return l;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return this.timeout;
    }
    
    public static String createAuthResponseHeader(SpeedyRequest response,
            String username, String password) {
        //check header...
        
        final String authRequestHeader = response.responseHeaders.getHeader(HTTPHeaders.HDR_WWWAUTH);
        if ((authRequestHeader == null) || (authRequestHeader.length() == 0))
                return null;
        
        try {
            System.out.println("header: "+authRequestHeader);


            Pattern p = Pattern.compile("nonce=\\\"(\\S+)\\\"");
            Matcher m = p.matcher(authRequestHeader);
            m.find();
            final String cNonce = m.group(1);
        
        
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update((username+":xtreemfs:"+password).getBytes());
            byte[] digest = md5.digest();
            final String HA1 = OutputUtils.byteArrayToHexString(digest).toLowerCase();

            md5.update((response.getMethod()+":"+response.getURI()).getBytes());
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
