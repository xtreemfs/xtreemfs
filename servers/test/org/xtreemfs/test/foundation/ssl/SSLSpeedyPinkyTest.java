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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.pinky.SSLOptions;

/**
 * 
 * @author clorenz
 */
public class SSLSpeedyPinkyTest extends TestCase {
    
    public static final int     PORT = 12345;
    
    private static final String URL  = "https://localhost:" + PORT + "/";
    
    PipelinedPinky              pinky;
    
    RPCClient                   speedy;
    
    public SSLSpeedyPinkyTest(String testName) {
        super(testName);
        Logging.start(Logging.LEVEL_DEBUG);
        
        TimeSync.initialize(null, 100000, 50, null);
    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        SSLOptions pinkySslOptions = createSSLOptions("service1.jks", "passphrase", SSLOptions.JKS_CONTAINER,
            "trust.jks", "passphrase", SSLOptions.JKS_CONTAINER);
        
        pinky = new PipelinedPinky(PORT, null, null, pinkySslOptions);
        // pinky = new PipelinedPinky(PORT, null);
        
        // register a request listener that is called by pinky when
        // receiving a request
        pinky.registerListener(new PinkyRequestListener() {
            public void receiveRequest(PinkyRequest theRequest) {
                try {
                    // unpack body, parse it, write back JSON and send that
                    // back to the client
                    if (theRequest.requestBody != null) {
                        byte bdy[] = null;
                        if (theRequest.requestBody.hasArray()) {
                            bdy = theRequest.requestBody.array();
                        } else {
                            bdy = new byte[theRequest.requestBody.capacity()];
                            theRequest.requestBody.position(0);
                            theRequest.requestBody.get(bdy);
                        }
                        
                        String body = new String(bdy, "utf-8");
                        Object o = JSONParser.parseJSON(new JSONString(body));
                        String respBdy = JSONParser.writeJSON(o);
                        theRequest.setResponse(HTTPUtils.SC_OKAY, ReusableBuffer.wrap(respBdy
                                .getBytes("utf-8")), HTTPUtils.DATA_TYPE.JSON);
                    } else {
                        theRequest.setResponse(HTTPUtils.SC_OKAY);
                    }
                    pinky.sendResponse(theRequest);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    try {
                        theRequest.setResponse(HTTPUtils.SC_SERVER_ERROR);
                    } catch (Exception e) {
                        // ignore that
                        e.printStackTrace();
                    }
                    theRequest.setClose(true);
                    pinky.sendResponse(theRequest);
                }
            }
        });
        pinky.start();
        
        SSLOptions speedySslOptions = createSSLOptions("client1.p12", "passphrase",
            SSLOptions.PKCS12_CONTAINER, "trust.jks", "passphrase", SSLOptions.JKS_CONTAINER);
        
        speedy = new RPCClient(500000000, speedySslOptions);
        // speedy = new RPCClient(null, 5000);
        
    }
    
    protected void tearDown() throws Exception {
        pinky.shutdown();
        speedy.shutdown();
        pinky.waitForShutdown();
        speedy.waitForShutdown();
    }
    
    // TODO add test methods here. The name must begin with 'test'. For example:
    // public void testHello() {}
    
    public void testSimple() throws Exception {
        
        RPCResponse rp = null;
        
        InetSocketAddress local = new InetSocketAddress("localhost", PORT);
        rp = speedy.sendRPC(local, "/bla", null, "bla", null);
        assertEquals(rp.getStatusCode(), 200);
        rp.freeBuffers();
        
        RPCResponse rp2 = null;
        
        rp2 = speedy.sendRPC(local, "/bla", null, "bla", null);
        assertEquals(rp2.getStatusCode(), 200);
        rp2.freeBuffers();
        
        RPCResponse rp3 = null;
        
        rp3 = speedy.sendRPC(local, "/bla", null, "bla", null);
        assertEquals(rp3.getStatusCode(), 200);
        rp3.freeBuffers();
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(SSLSpeedyPinkyTest.class);
    }
    
    public static SSLOptions createSSLOptions(String keyStoreName, String ksPassphrase,
        String ksContainerType, String trustStoreName, String tsPassphrase, String tsContainerType)
        throws IOException {
        
        ClassLoader cl = SSLSpeedyPinkyTest.class.getClassLoader();
        
        InputStream ks = cl.getResourceAsStream(keyStoreName);
        if (ks == null)
            ks = new FileInputStream("test/" + keyStoreName);
        
        InputStream ts = cl.getResourceAsStream(trustStoreName);
        if (ts == null)
            ts = new FileInputStream("test/" + trustStoreName);
        
        return new SSLOptions(ks, ksPassphrase, ksContainerType, ts, tsPassphrase, tsContainerType, false);
    }
    
    /*
     * public void testErrorCases() throws Exception {
     * 
     * RPCResponse rp = null; try { InetSocketAddress nonexiting = new
     * InetSocketAddress( "yabba-brabbel.zib.de", 80); rp =
     * speedy.sendRPC(nonexiting, "bla", null, "bla", null);
     * rp.waitForResponse(); fail("IOException should have been thrown."); }
     * catch (UnresolvedAddressException ex) { } finally { if (rp != null)
     * rp.freeBuffers(); }
     * 
     * InetSocketAddress local = new InetSocketAddress("localhost", PORT); rp =
     * speedy.sendRPC(local, "/bla", null, "bla", null);
     * assertEquals(rp.getStatusCode(), 200); rp.freeBuffers();
     * 
     * InetSocketAddress local500 = null; try { local500 = new
     * InetSocketAddress("localhost",PORT); rp =
     * speedy.sendRPC(local500,"/bla",null,"bla",null); rp.waitForResponse();
     * fail("HttpErrorException should have been thrown."); } catch
     * (HttpErrorException ex) { assertEquals(ex.getStatusCode(), 500); }
     * finally { if (rp != null) rp.freeBuffers(); }
     * 
     * InetSocketAddress localWait = null; try { localWait = new
     * InetSocketAddress("localhost",PORT); rp =
     * speedy.sendRPC(localWait,"/bla",null,"bla",null); rp.waitForResponse();
     * fail("IOException should have been thrown."); } catch (IOException ex) {
     * } finally { if (rp != null) rp.freeBuffers(); }
     * 
     * rp = speedy.sendRPC(local,"/bla",null,"bla",null); final AtomicBoolean
     * hasResponse = new AtomicBoolean(false); final Object me = this;
     * rp.setResponseListener(new RPCResponseListener() {
     * 
     * @Override public void responseAvailable(RPCResponse response) {
     * hasResponse.set(true); synchronized (me) { me.notify(); } } });
     * synchronized (this) { try { this.wait(1000);
     * 
     * } catch (InterruptedException interruptedException) { }
     * 
     * } assertTrue(hasResponse.get());
     * 
     * rp = speedy.sendRPC(localWait,"/bla",null,"bla",null); final
     * AtomicBoolean hasNoResponse = new AtomicBoolean(true);
     * rp.setResponseListener(new RPCResponseListener() {
     * 
     * @Override public void responseAvailable(RPCResponse response) {
     * hasNoResponse.set(false); synchronized (me) { me.notify(); } } });
     * synchronized (this) { try { this.wait(500);
     * 
     * } catch (InterruptedException interruptedException) { }
     * 
     * } rp.freeBuffers(); assertTrue(hasNoResponse.get());
     * System.out.println("wait for response!"); synchronized (this) { try {
     * this.wait(10000);
     * 
     * } catch (InterruptedException interruptedException) {
     * interruptedException.printStackTrace(); }
     * 
     * } System.out.println("waiting done");
     * 
     * }
     */
}
