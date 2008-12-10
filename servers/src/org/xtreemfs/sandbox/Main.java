/*
 * Main.java
 *
 * Created on 8. Dezember 2006, 10:21
 *
 * @author Bjoern Kolbeck, Zuse Institute Berlin (kolbeck@zib.de)
 *
 */

package org.xtreemfs.sandbox;

import java.io.IOException;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

/**
 * A simple (manual) and stupid test for the Pinky Server. Receives JSON data,
 * parses it, writes it to JSON and returns it. Used for performance tests.
 *
 * @author bjko
 */
public class Main {

    /** Creates a new instance of Main */
    public Main() {
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        Thread test;
        try {
            // create a new Pinky server
            final PipelinedPinky sthr = new PipelinedPinky(12203, null, null);

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
            test = new Thread(sthr);
            test.start();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

}
