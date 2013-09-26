/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import org.xtreemfs.foundation.queue.AbstractWeightedFairQueue;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class WFQStage extends Stage {

    private class WFQRequest {
        private StageRequest rq;
        private Object callback;

        WFQRequest(StageRequest rq, Object callback) {
            this.rq = rq;
            this.callback = callback;
        }

        private StageRequest getRequest() {
            return rq;
        }

        private Object getCallback() {
            return callback;
        }
    }

    public final static int         WFQ_STAGE_OPERATION = 1;

    private OSDRequestDispatcher    master;

    private AbstractWeightedFairQueue<WFQRequest> wfqQueue;

    public WFQStage(OSDRequestDispatcher master, int queueCapacity) {
        super("WFQ Stage", queueCapacity);
        this.master = master;


    }

    public void handleRequest(StageRequest request, Object callback) {
        // TODO(ckleineweber): set quality class
        this.wfqQueue.add(1, new WFQRequest(request, callback));
    }

    @Override
    public int getQueueLength() {
        return wfqQueue.size();
    }

    @Override
    public void run() {

        notifyStarted();

        while (!quit) {
            try {
                final StageRequest op = wfqQueue.take().getRequest();

                processMethod(op);

            } catch (InterruptedException ex) {
                break;
            } catch (Throwable ex) {
                this.notifyCrashed(ex);
                break;
            }
        }

        notifyStopped();
    }

    @Override
    protected void processMethod(StageRequest method) {
        final OSDRequest request = (OSDRequest) method.getArgs()[0];
        request.getOperation().startRequest(request);
    }
}
