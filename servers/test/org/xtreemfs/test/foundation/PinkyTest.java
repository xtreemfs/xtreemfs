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

package org.xtreemfs.test.foundation;

import junit.framework.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class PinkyTest extends TestCase {

    Thread test;

    PipelinedPinky sthr;

    public static final int PORT = 12345;

	private static final String URL = "http://localhost:"+PORT+"/";

    HttpClient client;

    public PinkyTest(String testName) {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        client = new HttpClient();

        // create a new Pinky server
        sthr = new PipelinedPinky(PORT, null, null);

        // register a request listener that is called by pinky when
        // receiving a request
        sthr.registerListener(new PinkyRequestListener() {
            public void receiveRequest(PinkyRequest theRequest) {
                try {
                    // unpack body, parse it, write back JSON and send that
                    // back to the client
                    if (theRequest.requestBody != null) {
                        byte bdy[] = null;
                        if (theRequest.requestBody.hasArray()) {
                            bdy = theRequest.requestBody.array();
                        } else {
                            bdy = new byte[theRequest.requestBody
                                    .capacity()];
                            theRequest.requestBody.position(0);
                            theRequest.requestBody.get(bdy);
                        }

                        String body = new String(bdy, "utf-8");
                        Object o = JSONParser
                                .parseJSON(new JSONString(body));
                        String respBdy = JSONParser.writeJSON(o);
                        theRequest.setResponse(HTTPUtils.SC_OKAY,
                                ReusableBuffer.wrap(respBdy.getBytes("utf-8")),
                                HTTPUtils.DATA_TYPE.JSON);
                    } else {
                        theRequest.setResponse(HTTPUtils.SC_OKAY);
                    }
                    sthr.sendResponse(theRequest);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    try {
                        theRequest.setResponse(HTTPUtils.SC_SERVER_ERROR);
                    } catch (Exception e) {
                        // ignore that
                        e.printStackTrace();
                    }
                    theRequest.setClose(true);
                    sthr.sendResponse(theRequest);
                }
            }
        });
        // start the Pinky server in a new thread
//        test = new Thread(sthr);
//        test.start();
        sthr.start();
    }

    protected void tearDown() throws Exception {
        sthr.shutdown();
        sthr.waitForShutdown();
//        synchronized (this) {
//            this.wait(2000);
//        }

    }

    // TODO add test methods here. The name must begin with 'test'. For example:
    // public void testHello() {}

    public void testJSONEcho() throws Exception {

        PostMethod method = new PostMethod(URL);
        String content = "[\"Hallo\"]";
        method.setRequestEntity(new StringRequestEntity(content,"text/plain","utf-8"));
        int rc = client.executeMethod(method);
        assertEquals(rc,200);
        String response = method.getResponseBodyAsString();
        assertEquals(response,content);

    }

    public void testEmptyJSON() throws Exception {

        PostMethod method = new PostMethod(URL);
        String content = "";
        method.setRequestEntity(new StringRequestEntity(content,"text/plain","utf-8"));
        int rc = client.executeMethod(method);
        assertEquals(rc,200);
        String response = method.getResponseBodyAsString();
        assertEquals(response,content);

    }

    public void testEmptyBody() throws Exception {

        PostMethod method = new PostMethod(URL);
        int rc = client.executeMethod(method);
        assertEquals(rc,200);
        String response = method.getResponseBodyAsString();
        assertEquals(response.length(),0);

    }

    public void testGet() throws Exception {

        GetMethod method = new GetMethod(URL);
        int rc = client.executeMethod(method);
        assertEquals(rc,200);
        String response = method.getResponseBodyAsString();
        assertEquals(response.length(),0);

    }

    public void testMultipleJSONEcho() throws Exception {
        String content = "[\"Hallo\"]";
        StringRequestEntity request = new StringRequestEntity(content,"text/plain","utf-8");

        int NUMBER_OF_CLIENTS = 100;
        int rc;

        for(int i=0; i < NUMBER_OF_CLIENTS; i++){
            PostMethod method = new PostMethod(URL);
			method.setRequestEntity(request);

            client = new HttpClient();
            rc = client.executeMethod(method);

            assertEquals(rc,200);
            String response = method.getResponseBodyAsString();
            assertEquals(response,content);
        }
    }

}
