/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.foundation;

import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.speedy.MultiSpeedy;

/**
 *
 * @author bjko
 */
public class DigestAuthTest extends TestCase {
    
    private PipelinedPinky pinky;
    private MultiSpeedy    speedy;
    
    private InetSocketAddress me;
    
    public DigestAuthTest(String testName) {
        super(testName);
        me = new InetSocketAddress("localhost",12121);
        Logging.start(Logging.LEVEL_DEBUG);
    }            

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pinky = new PipelinedPinky(12121, null, new PinkyRequestListener() {

            public void receiveRequest(PinkyRequest theRequest) {
                if (theRequest.requestAuthentication("test", "test")) {
                    theRequest.setResponse(200);
                    System.out.println("got authenticated request...");
                }
                pinky.sendResponse(theRequest);
            }
        });
        pinky.start();
        
        speedy = new MultiSpeedy();
        speedy.start();
        
        pinky.waitForStartup();
        speedy.waitForStartup();
        
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        pinky.shutdown();
        pinky.waitForShutdown();
        
        speedy.shutdown();
        speedy.waitForShutdown();
    }

    public void testAuth() throws Exception {
/*        RPCClient c = new RPCClient(speedy);
        RPCResponse r = null;
        try {
            r = c.sendRPC(me, "", null, "yagga yagag", null);
            r.waitForResponse();
        } catch (HttpErrorException ex) {
            if (ex.authenticationRequest()) {
                HTTPHeaders addHdr = new HTTPHeaders();
                addHdr.addHeader(HTTPHeaders.HDR_AUTHORIZATION, RPCClient.createAuthResponseHeader(r.getSpeedyRequest(),"test","test"));
                r = c.sendRPC(me, "", null, "", addHdr);
                r.waitForResponse();
            }
        }*/
    }

}
