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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.*;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.RPCResponseListener;
import org.xtreemfs.common.logging.Logging;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 *
 * @author bjko
 */
public class RPCClientTest extends TestCase {

    public static final int PORT     = 12345;

    public static final int PORT500  = 12346;

    public static final int PORTWAIT = 12347;

    HttpServer              serverOK, server500, serverwait;

    RPCClient               client;

    public RPCClientTest(String testName) {
        super(testName);
        Logging.start(Logging.LEVEL_DEBUG);
    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "."
            + getName());

        serverOK = HttpServer.create(new InetSocketAddress("localhost", PORT),
            0);
        serverOK.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content = "simpleContents".getBytes("ascii");
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"request received: ");
                httpExchange.sendResponseHeaders(200,content.length);
                httpExchange.getResponseBody().write(content);
                httpExchange.getResponseBody().close();
            }
        });
        serverOK.start();

        server500 = HttpServer.create(new InetSocketAddress("localhost",
            PORT500), 0);
        server500.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content = "simpleContents".getBytes("ascii");
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"request received: ");
                httpExchange.sendResponseHeaders(500,content.length);
                httpExchange.getResponseBody().write(content);
                httpExchange.getResponseBody().close();
            }
        });
        server500.start();

        serverwait = HttpServer.create(new InetSocketAddress("localhost",
            PORTWAIT), 0);
        serverwait.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"request received: ");
                synchronized (this) {
                    try {
                        this.wait(5000);
                    } catch (InterruptedException ex) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
                    }
                }
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"request answered ");
                byte[] content = "simpleContents".getBytes("ascii");
                httpExchange.sendResponseHeaders(500, content.length);
                httpExchange.getResponseBody().write(content);
                httpExchange.getResponseBody().close();
            }
        });
        serverwait.start();

        client = new RPCClient(null, 2000);
    }

    protected void tearDown() throws Exception {
        client.shutdown();
        client.waitForShutdown();
        serverOK.stop(0);
        server500.stop(0);
        serverwait.stop(0);
    }

    // TODO add test methods here. The name must begin with 'test'. For example:
    // public void testHello() {}

    public void testErrorCases() throws Exception {

        RPCResponse rp = null;
        try {
            InetSocketAddress nonexiting = new InetSocketAddress(
                "yabba-brabbel.zib.de", 80);
            rp = client.sendRPC(nonexiting, "bla", null, "bla", null);
            rp.waitForResponse();
            fail("IOException should have been thrown.");
        } catch (UnresolvedAddressException ex) {
        } finally {
            if (rp != null)
                rp.freeBuffers();
        }

        InetSocketAddress local = new InetSocketAddress("localhost", PORT);
        rp = client.sendRPC(local, "/bla", null, "bla", null);
        assertEquals(rp.getStatusCode(), 200);
        rp.freeBuffers();

        InetSocketAddress local500 = null;
        try {
            local500 = new InetSocketAddress("localhost",PORT500);
            rp = client.sendRPC(local500,"/bla",null,"bla",null);
            rp.waitForResponse();
            fail("HttpErrorException should have been thrown.");
        } catch (HttpErrorException ex) {
            assertEquals(ex.getStatusCode(), 500);
        } finally {
            if (rp != null)
                rp.freeBuffers();
        }

        InetSocketAddress localWait = null;
        try {
            localWait = new InetSocketAddress("localhost",PORTWAIT);
            rp = client.sendRPC(localWait,"/bla",null,"bla",null);
            rp.waitForResponse();
            fail("IOException should have been thrown.");
        } catch (IOException ex) {
        } finally {
            if (rp != null)
                rp.freeBuffers();
        }

        rp = client.sendRPC(local,"/bla",null,"bla",null);
        final AtomicBoolean hasResponse = new AtomicBoolean(false);
        final Object me = this;
        rp.setResponseListener(new RPCResponseListener() {

            @Override
            public void responseAvailable(RPCResponse response) {
                hasResponse.set(true);
                synchronized (me) {
                    me.notify();
                }
            }
        });
        synchronized (this) {
            try {
                this.wait(1000);

            } catch (InterruptedException interruptedException) {
            }

        }
        assertTrue(hasResponse.get());

        rp = client.sendRPC(localWait,"/bla",null,"bla",null);
        final AtomicBoolean hasNoResponse = new AtomicBoolean(true);
        rp.setResponseListener(new RPCResponseListener() {

            @Override
            public void responseAvailable(RPCResponse response) {
                hasNoResponse.set(false);
                synchronized (me) {
                    me.notify();
                }
            }
        });
        synchronized (this) {
            try {
                this.wait(500);

            } catch (InterruptedException interruptedException) {
            }

        }
        rp.freeBuffers();
        assertTrue(hasNoResponse.get());
        System.out.println("wait for response!");
        synchronized (this) {
            try {
                this.wait(10000);

            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }

        }
        System.out.println("waiting done");

    }
}
