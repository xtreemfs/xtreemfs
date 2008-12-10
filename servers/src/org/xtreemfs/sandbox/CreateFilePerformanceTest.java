/*
 * CreateFile.java
 * JUnit based test
 *
 * Created on January 17, 2007, 8:39 AM
 */

package org.xtreemfs.sandbox;

import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.mrc.MRCConfig;

/**
 * Simple test to check the performance of the MRC for file
 * creates.
 * @author bjko
 */
public class CreateFilePerformanceTest  {

    private MRCConfig brainConfig;

    private DIRConfig dirServiceConfig;

    public static volatile int numRq = 0;

    public static boolean error = false;


    public CreateFilePerformanceTest() throws Exception {
    }


    // TODO add test methods here. The name must begin with 'test'. For example:
    // public void testHello() {}

    public void testCreateFile() throws Exception {

        final InetSocketAddress endpoint = new InetSocketAddress(
                "localhost", 32636);
        final MultiSpeedy client = new MultiSpeedy();
        client.registerListener(new SpeedyResponseListener() {
            int numR = 0;

            public void receiveRequest(SpeedyRequest resp) {
                try {
                    if (resp.status == SpeedyRequest.RequestStatus.FAILED) {
                        System.out.println("HTTP request failed for unknown reason");
                    } else if (resp.status ==  SpeedyRequest.RequestStatus.FINISHED) {
                        if (resp.statusCode == 200) {
                            numRq++;
                            //System.out.println("RQID = "+resp.responseHeaders.getHeader("X-DEBUG-RQID"));
                            //System.out.println("SC 200: "+resp.responseBody);
                        } else {
                            error = true;
                            byte bdy[] = null;

                            if (resp.responseBody.hasArray()) {
                                bdy = resp.responseBody.array();
                            } else {
                                bdy = new byte[resp.responseBody.capacity()];
                                resp.responseBody.position(0);
                                resp.responseBody.get(bdy);
                            }

                            String body = new String(bdy, "ascii");
                            System.out.println("ERROR: "+body);
                        }
                    } else {
                        System.out.println("strange status: "+resp.status);
                    }


                } catch (Exception ex) {
                    System.out.println("Exception occurred in responseListener: "+ex);
                    ex.printStackTrace();
                }
            }
        }, endpoint);

        Thread test = new Thread(client);
        test.start();
        Thread.currentThread().yield();

        Thread secCheck = new Thread(new Runnable() {
            public void run() {
                int lastNum = 0;
                while (true) {
                    try {
                        synchronized (this) {
                            this.wait(999);
                        }
                    } catch (InterruptedException ex) {
                        System.out.println("interrupted...");
                    }
                    int copyNRQ = numRq;
                    System.out.println(">>>>>>> creates/sec = "+(copyNRQ-lastNum));
                    lastNum = copyNRQ;
                }
            }
        });
        secCheck.setPriority(Thread.MAX_PRIORITY);
        secCheck.start();

        ReusableBuffer bdy = ReusableBuffer.wrap(("[\"Blup\"]").getBytes());
        SpeedyRequest sr = new SpeedyRequest("GET","createVolume",null,"nullauth 1 1",bdy,HTTPUtils.DATA_TYPE.JSON);
        //client.sendRequest(sr,endpoint);

        try {
            synchronized (this) {
                this.wait(5000);
            }
        } catch (InterruptedException ex2) {
        }
        //System.exit(1);
        bdy = ReusableBuffer.wrap(("[\"testVolume\"]").getBytes());
        for (int i = 0; i < 20000; i++) {
            bdy = ReusableBuffer.wrap(("[\"Blup/t3_"+(i+10)+"\"]").getBytes());
            sr = new SpeedyRequest("GET","createFile",null,"nullauth 1 1",bdy,HTTPUtils.DATA_TYPE.JSON);

            //bdy = ByteBuffer.wrap(("[\"TestVolume/test4_120\",false,false,false]").getBytes());
            //sr = new SpeedyRequest("GET","stat",null,"nullauth 1 1",bdy,HTTPUtils.DATA_TYPE.TEXT);
            try {
                //sr = new SpeedyRequest("GET","readDir",null,bdy,HTTPUtils.DATA_TYPE.TEXT);
                client.sendRequest(sr,endpoint);
            } catch (IllegalStateException ex) {
                System.out.println("QQQQQ Q FULL");
                try {
                    synchronized (this) {
                        this.wait(20);
                    }
                } catch (InterruptedException ex2) {
                }
            }
            if (i%10 == 0) {
                try {
                    synchronized (this) {
                        this.wait(1);
                    }
                } catch (InterruptedException ex2) {
                }
            }

            if (error) {
                System.out.println("Error occurred, abort!");
                break;
            }
            //System.out.println("testX"+i);
            Thread.currentThread().yield();
        }

        if (error)
            System.exit(1);


    }

    public static void main(String[] args) {
        Logging.start(Logging.LEVEL_WARN);
        try {
            CreateFilePerformanceTest cft = new CreateFilePerformanceTest();
            cft.testCreateFile();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
