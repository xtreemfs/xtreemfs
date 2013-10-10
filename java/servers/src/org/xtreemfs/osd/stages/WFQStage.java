/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import org.xtreemfs.foundation.queue.WeightedFairQueue;
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

    private WeightedFairQueue<String, WFQRequest> wfqQueue;

    public WFQStage(OSDRequestDispatcher master, int queueCapacity) {
        super("WFQ Stage", queueCapacity);
        this.master = master;

        this.wfqQueue = new WeightedFairQueue<String, WFQRequest>(queueCapacity, new WeightedFairQueue.WFQElementInformationProvider<String, WFQRequest>() {
            @Override
            public int getRequestCost(WFQRequest element) {
                return 1;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getQualityClass(WFQRequest element) {
                return "1";  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public int getWeight(WFQRequest element) {
                return 1;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
    }

    public void handleRequest(StageRequest request, Object callback) {
        // TODO(ckleineweber): set quality class
        this.wfqQueue.add(new WFQRequest(request, callback));
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
