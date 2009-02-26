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
 * AUTHORS: Christian Lorenz (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.foundation.ssl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLParameters;

import junit.framework.*;
import junit.textui.TestRunner;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author clorenz
 */
public class SSLSpeedyTest extends TestCase {

    public static final int PORT = 12345;

    HttpsServer server;

    MultiSpeedy client;

    String response = null;

    int returncode = 0;

	private static final String URL = "https://localhost:"+PORT+"/";

	private String PATH = "servers/test/";

    public SSLSpeedyTest(String testName) {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);

        TimeSync.initialize(null, 100000, 50, null);
        
        File testfile = new File("testfile");
        if (testfile.getAbsolutePath().endsWith("java/testfile")) {
            PATH = "../"+PATH;
        } else {
            PATH = "./"+PATH;
        }
    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        server = HttpsServer.create(new InetSocketAddress("localhost",PORT),0);
        SSLOptions sslOptions = new SSLOptions(PATH + "service1.jks",
				"passphrase", SSLOptions.JKS_CONTAINER, PATH + "trust.jks",
				"passphrase", SSLOptions.JKS_CONTAINER, false);
        server.setHttpsConfigurator (new HttpsConfigurator(sslOptions.getSSLContext()) {
            public void configure (HttpsParameters params) {
                // get the default parameters
                SSLParameters sslParams = getSSLContext().getDefaultSSLParameters();

                // set ssl params for speedy
                sslParams.setProtocols(getSSLContext().getSupportedSSLParameters().getProtocols());
    			sslParams.setCipherSuites(getSSLContext().getSupportedSSLParameters().getCipherSuites());
    			sslParams.setNeedClientAuth(true);

                params.setSSLParameters(sslParams);
            }
        });
//        server.setHttpsConfigurator(new HttpsConfigurator(sslOptions.getSSLContext()));
        server.createContext("/",new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content = "simpleContents".getBytes("ascii");
                httpExchange.sendResponseHeaders(200,content.length);
                httpExchange.getResponseBody().write(content);
                httpExchange.getResponseBody().close();
            }
        });
        server.start();

        SSLOptions sslOptions2 = new SSLOptions(PATH + "service2.jks",
				"passphrase", SSLOptions.JKS_CONTAINER, PATH + "trust.jks",
				"passphrase", SSLOptions.JKS_CONTAINER, false);

        client = new MultiSpeedy(sslOptions2);
    }

    protected void tearDown() throws Exception {
        client.shutdown();
        client.waitForShutdown();
        server.stop(0);
    }

    public void testSpeedy() throws Exception {

        final InetSocketAddress endpoint = new InetSocketAddress(
                "localhost", PORT);

        client.registerListener(new SpeedyResponseListener() {
            int numR = 0;

            public void receiveRequest(SpeedyRequest resp) {
                try {
                    if (resp.status == SpeedyRequest.RequestStatus.FAILED) {
                        fail("HTTP request failed for unknown reason");
                    } else {
                        byte bdy[] = null;
                        returncode = resp.statusCode;
//                        System.out.println("sc="+resp.statusCode+" / "+resp.responseBody);
                        if (resp.responseBody == null) {
                            response = null;
                        } else {
                            if (resp.responseBody.hasArray()) {
                                bdy = resp.responseBody.array();
                            } else {
                                bdy = new byte[resp.responseBody.capacity()];
                                resp.responseBody.position(0);
                                resp.responseBody.get(bdy);
                            }

                            response = new String(bdy, "ascii");
                        }

                        synchronized (resp) {
                            resp.notifyAll();
                        }
                    }
                } catch (Exception ex) {
                    fail("Exception occurred in responseListener: "+ex);
                } finally {
                    if (resp != null)
                        resp.freeBuffer();
                }
            }
        }, endpoint);

        Thread test = new Thread(client);
        test.start();
        Thread.currentThread().yield();

        SpeedyRequest sr = new SpeedyRequest("GET","/",null,null);

        client.sendRequest(sr,endpoint);

        synchronized (sr) {
            sr.wait(5000);
        }
        assertEquals(returncode,200);
        assertEquals(response,"simpleContents");
    }

    public static void main(String[] args) {
        TestRunner.run(SSLSpeedyTest.class);
    }
    
}
