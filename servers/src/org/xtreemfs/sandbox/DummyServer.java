/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.pinky.PipelinedPinky;

/**
 *
 * @author bjko
 */
public class DummyServer implements PinkyRequestListener {


    public static final int size = 1024 * 1024;

    public static final int delay = 0;

    public ReusableBuffer buff;

    public PipelinedPinky pinky;

    public DummyServer(PipelinedPinky pinky) {

        buff = BufferPool.allocate(size);

        this.pinky = pinky;
    }

    public void setPinky(PipelinedPinky pinky) {
        this.pinky = pinky;
    }

    public void receiveRequest(PinkyRequest theRequest) {
        theRequest.setResponse(HTTPUtils.SC_OKAY, buff.createViewBuffer(), HTTPUtils.DATA_TYPE.BINARY);
        if (delay > 0) {
            synchronized(this) {
                try {
                    Thread.currentThread().sleep(0, delay);
                } catch (InterruptedException ex) {
                    Logger.getLogger(DummyServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        pinky.sendResponse(theRequest);
    }

    public static void main(String[] args) {
        try {

            Logging.start(Logging.LEVEL_ERROR);

            DummyServer me = new DummyServer(null);

            PipelinedPinky pinky = new PipelinedPinky(32641, null, me);


            me.setPinky(pinky);
            pinky.start();
            System.out.println("pinky running on 32641 with "+size+"bytes/rq and delay of "+delay+"ns");
        } catch (IOException ex) {
            Logger.getLogger(DummyServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }



}
