/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.stages.Stage.StageRequest;

public interface ECWorker<EVENT> {
    enum TYPE {
        READ, WRITE
    };

    TYPE getType();

    Interval getRequestInterval();

    StageRequest getRequest();

    void start();

    void processEvent(EVENT event);

    boolean hasFinished();

    boolean hasFailed();

    boolean hasSucceeded();

    ErrorResponse getError();

    Object getResult();

    public interface ECWorkerEventProcessor<EVENT> {
        void signal(ECWorker<EVENT> worker, EVENT event);
    }

}
