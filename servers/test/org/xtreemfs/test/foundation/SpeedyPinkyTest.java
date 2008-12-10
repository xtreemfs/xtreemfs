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

import java.net.InetSocketAddress;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.test.SetupUtils;

public class SpeedyPinkyTest extends TestCase {

    private static final int PORT = 12345;

    MultiSpeedy              speedy;

    PipelinedPinky           pinky;

    int                      responses;

    public SpeedyPinkyTest(String testName) {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);
        TimeSync.initialize(null, 10000000, 50, null);
    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        speedy = new MultiSpeedy();
        speedy.start();
        speedy.waitForStartup();

        PinkyRequestListener listener = new PinkyRequestListener() {

            public void receiveRequest(PinkyRequest theRequest) {

                int length = Integer.parseInt(new String(theRequest.requestBody.array()));

                ReusableBuffer body = generateData(length);

                HTTPHeaders headers = new HTTPHeaders();
                headers.addHeader(HTTPHeaders.HDR_CONTENT_TYPE, HTTPUtils.JSON_TYPE);

                theRequest.setResponse(HTTPUtils.SC_OKAY, body, HTTPUtils.DATA_TYPE.JSON, headers);
                pinky.sendResponse(theRequest);
            }

        };

        pinky = new PipelinedPinky(PORT, null, listener);
        pinky.start();
        pinky.waitForStartup();
    }

    protected void tearDown() throws Exception {

        speedy.shutdown();
        pinky.shutdown();

        speedy.waitForShutdown();
        pinky.waitForShutdown();
    }

    /**
     * Sends a large amount of requests for responses of varying sizes, without
     * waiting before sending the next request.
     *
     * @throws Exception
     */
    public void testAsync() throws Exception {

        final InetSocketAddress endpoint = new InetSocketAddress("localhost", PORT);

        responses = 0;

        speedy.registerListener(new SpeedyResponseListener() {

            public void receiveRequest(SpeedyRequest resp) {

                responses++;
                checkResponse(resp);

                synchronized (speedy) {
                    speedy.notify();
                }
            }

        }, endpoint);

        final int numReqs = 2000;

        for (int i = 0; i < numReqs; i++) {
            ReusableBuffer body = ReusableBuffer.wrap(Integer.toString(i).getBytes());
            SpeedyRequest sr = new SpeedyRequest("GET", "/", null, null, body, DATA_TYPE.JSON);
            speedy.sendRequest(sr, endpoint);
        }

        synchronized (speedy) {
            while (responses < numReqs)
                speedy.wait();
        }

    }

    /**
     * Sends a large amount of requests for responses of varying sizes, and
     * waits for the response for the next k requests before sending the next k
     * requests.
     *
     * @throws Exception
     */
    public void testkSync() throws Exception {

        final InetSocketAddress endpoint = new InetSocketAddress("localhost", PORT);

        responses = 0;

        speedy.registerListener(new SpeedyResponseListener() {

            public void receiveRequest(SpeedyRequest resp) {

                responses++;
                checkResponse(resp);

                synchronized (speedy) {
                    speedy.notify();
                }
            }

        }, endpoint);

        final int numReqs = 10000;
        final int k = 5;

        for (int i = 0; i < numReqs; i += k) {

            // send the next k requests
            for (int j = 0; j < k; j++) {
                ReusableBuffer body = ReusableBuffer.wrap(Integer.toString(i + j).getBytes());
                SpeedyRequest sr = new SpeedyRequest("GET", "/", null, null, body, DATA_TYPE.JSON);
                speedy.sendRequest(sr, endpoint);
            }

            // wait for the next k responses
            synchronized (speedy) {
                while (responses < i + k)
                    speedy.wait();
            }
        }
    }

    private ReusableBuffer generateData(int length) {

        byte[] len = Integer.toString(length).getBytes();
        ReusableBuffer buf = BufferPool.allocate(length + len.length + 1);
        buf.position(0);
        buf.put(len);
        buf.put((byte) 0x20); // space

        for (int i = 0; i < length; i++)
            buf.put((byte) 65);

        return buf;
    }

    private void checkResponse(SpeedyRequest resp) {
        try {
            if (resp.status == SpeedyRequest.RequestStatus.FAILED) {
                fail("HTTP request failed for unknown reason");
            } else {
                int returncode = resp.statusCode;
                assertEquals(returncode, 200);

                byte bdy[] = resp.responseBody.array();
                String response = new String(bdy, "ascii");
                int len = Integer.parseInt(response.substring(0, response.indexOf(' ')));
                String content = response.substring(response.indexOf(' ') + 1);
                assertEquals(content.length(), len);
            }

        } catch (Throwable ex) {
            fail("Exception occurred in responseListener: " + ex);
        } finally {
            if (resp != null)
                resp.freeBuffer();
        }
    }

    public static void main(String[] args) {
        TestRunner.run(SpeedyPinkyTest.class);
    }
}
