/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.util.List;

import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.ec.ECWorker.ECWorkerEvent;
import org.xtreemfs.osd.stages.Stage.StageRequest;

public interface ECWorker<EVENT extends ECWorkerEvent> {
    enum TYPE {
        READ, WRITE
    };

    TYPE getType();

    Interval getRequestInterval();

    StageRequest getRequest();

    void start();

    void abort();

    void abort(EVENT event);

    void processEvent(EVENT event);

    boolean hasFinished();

    boolean hasFailed();

    boolean hasSucceeded();

    ErrorResponse getError();

    Object getResult();

    public interface ECWorkerEventProcessor<EVENT extends ECWorkerEvent> {
        void signal(ECWorker<EVENT> worker, EVENT event);
    }


    public static interface ECWorkerEvent {
        boolean needsStripeInterval();
        void setStripeInterval(List<Interval> stripeInterval);
        long getStripeNo();
    }

}
