/*
 * SpeedyMain.java
 *
 * Created on December 22, 2006, 1:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;

import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;

/**
 * Simple (manual) test case for Pinky and Speedy.
 *
 * @author bjko
 */
public class SpeedyMain {

    /** Creates a new instance of SpeedyMain */
    public SpeedyMain() {
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        Thread test;

        final int NREQ = 50;

        try {
            final InetSocketAddress endpoint = new InetSocketAddress(
                    "farnsworth.zib.de", 32636);
            final MultiSpeedy client = new MultiSpeedy();
            client.registerListener(new SpeedyResponseListener() {
                int numR = 0;

                public void receiveRequest(SpeedyRequest resp) {
                    try {
                        if (resp.status == SpeedyRequest.RequestStatus.FAILED) {
                            System.out.println("!!! request failed!");
                        } else {
                            Long rTime = System.currentTimeMillis();
                            byte bdy[] = null;

                            if (resp.responseBody.hasArray()) {
                                bdy = resp.responseBody.array();
                            } else {
                                bdy = new byte[resp.responseBody.capacity()];
                                resp.responseBody.position(0);
                                resp.responseBody.get(bdy);
                            }

                            String body = new String(bdy, "ascii");
                        }

                        numR++;
                        System.out.println("<<<< response " + numR + " took "
                                + (resp.received - resp.sendStart) + "ms");
                        if (numR == NREQ) {
                            System.exit(1);
                        }
                    } catch (Exception ex) {
                        System.out.println("ex: " + ex);
                        System.exit(1);
                    }
                }
            }, endpoint);

            test = new Thread(client);
            test.start();
            Thread.currentThread().yield();

            String json = "[\"myVolume/newDir/\"]";
            ReusableBuffer rqBdy = ReusableBuffer.wrap(json.getBytes("utf-8"));
            int errCnt = 0;
            for (int i = 0; i < NREQ; i++) {
                SpeedyRequest sr = null;
                try {
                    sr = new SpeedyRequest("GET", "readDir", null, null, rqBdy,
                            HTTPUtils.DATA_TYPE.JSON);
                    client.sendRequest(sr, endpoint);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    break;
                }

                System.out.println(">>>> request " + (i + 1));

                rqBdy.position(0);

                if (i % 5 == 0) {
                    try {
                        Thread.currentThread().sleep(1);
                    } catch (InterruptedException ex) {
                    }
                }
                // Thread.currentThread().yield();
            }

            // client.shutdown();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
