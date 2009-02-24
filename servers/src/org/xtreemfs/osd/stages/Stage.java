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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd.stages;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.common.Request;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;

public abstract class Stage extends LifeCycleThread {

    /**
     * global stage response codes. These codes are sent back by a stage to
     * indicate to the operation's state machine, which action to take next.
     *
     * This list contains some basic codes as well as all specialized codes used
     * by individual stages.
     *
     */
    public enum StageResponseCode {
        /**
         * go to next operation
         */
        OK,
        /**
         * request failed, send error
         */
        FAILED,
        /**
         * stay in current state and wait for next event
         */
        WAIT,
        /**
         * finish request by sending the response
         */
        FINISH,
    }

    /**
     * queue containing all requests
     */
    protected BlockingQueue<StageMethod> q;

    /**
     * set to true if stage should shut down
     */
    protected volatile boolean           quit;

    public AtomicInteger         _numRq, _maxRqTime, _minRqTime;
    public AtomicLong            _sumRqTime;

    public Stage(String stageName) {
        super(stageName);
        q = new LinkedBlockingQueue<StageMethod>();
        this.quit = false;

        _numRq = new AtomicInteger(0);
        _maxRqTime = new AtomicInteger(0);
        _minRqTime = new AtomicInteger(Integer.MAX_VALUE);
        _sumRqTime = new AtomicLong(0);
    }

    /**
     * send an request for a stage operation
     *
     * @param rq
     *            the request
     * @param the
     *            method in the stage to execute
     */
    public void enqueueOperation(OSDRequest rq, int method, StageCallbackInterface callback) {
        rq.setEnqueueNanos(System.nanoTime());
        q.add(new StageMethod(rq, method, callback));
    }

    /**
     * shut the stage thread down
     */
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }

    /**
     * Get current number of requests in the queue.
     *
     * @return queue length
     */
    public int getQueueLength() {
        return q.size();
    }

    @Override
    public void run() {

        notifyStarted();

        while (!quit) {
            Request rq = null;
            try {
                final StageMethod op = q.take();

                rq = op.getRq();

                if (Logging.tracingEnabled())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "processing request #"
                        + rq.getRequestId() + " method: " + op.getStageMethod());

                processMethod(op);

            } catch (InterruptedException ex) {
                break;
            } catch (Exception ex) {
                if (rq != null)
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "exception occurred while processing:" + rq);
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                this.notifyCrashed(ex);
                break;
            }
        }

        notifyStopped();
    }

    /**
     * Operation was not successful, returns an error to the client.
     *
     * @param rq
     *            the request
     * @param err
     *            the error details to send to the client
     */
    protected void methodExecutionFailed(StageMethod m, ErrorRecord err) {
        assert (m != null);
        if (m.getCallback() == null) {
            if (Logging.tracingEnabled()) {
                Logging.logMessage(Logging.LEVEL_TRACE, this, "event dropped (possibly finished) "
                    + m.getRq().getRequestId() + " with error: " + err);
            }
            return;
        }
        assert (m.getRq() != null);
        assert (err != null);

        if (err.getErrorClass() == ErrorClass.INTERNAL_SERVER_ERROR)
            Logging.logMessage(Logging.LEVEL_ERROR, this, err.toString());

        m.getRq().setError(err);
        if (StatisticsStage.measure_request_times)
            calcRequestDuration(m.getRq());
        m.getCallback().methodExecutionCompleted(m.getRq(), StageResponseCode.FAILED);
    }

    /**
     * Stage operation was executed successful.
     *
     * @param rq
     * @param code
     */
    protected void methodExecutionSuccess(StageMethod m, StageResponseCode code) {
        assert (m != null);
        assert (m.getRq() != null);
        if (m.getCallback() == null) {
            if (Logging.tracingEnabled()) {
                Logging.logMessage(Logging.LEVEL_TRACE, this, "event dropped (possibly finished) "
                    + m.getRq().getRequestId());
            }
            return;
        }
        if (StatisticsStage.measure_request_times)
            calcRequestDuration(m.getRq());
        m.getCallback().methodExecutionCompleted(m.getRq(), code);
    }

    protected void calcRequestDuration(Request rq) {
        long d = (System.nanoTime()-rq.getEnqueueNanos())/100000l;
        _numRq.incrementAndGet();
        if (_minRqTime.get() > d)
            _minRqTime.set((int)d);
        if (_maxRqTime.get() < d)
            _maxRqTime.set((int)d);
        _sumRqTime.addAndGet(d);
    }

    /**
     * Handles the actual execution of a stage method. Must be implemented by
     * all stages.
     *
     * @param method
     *            the stage method to execute
     */
    protected abstract void processMethod(StageMethod method);

    protected static final class StageMethod {
        private OSDRequest                rq;

        private int                    stageMethod;

        private StageCallbackInterface callback;

        public StageMethod(OSDRequest rq, int stageMethod, StageCallbackInterface callback) {
            this.rq = rq;
            this.stageMethod = stageMethod;
            this.callback = callback;
        }

        public int getStageMethod() {
            return stageMethod;
        }

        public void setStageMethod(int stageMethod) {
            this.stageMethod = stageMethod;
        }

        public OSDRequest getRq() {
            return rq;
        }

        public void setRq(OSDRequest rq) {
            this.rq = rq;
        }

        public StageCallbackInterface getCallback() {
            return callback;
        }

        public void setCallback(StageCallbackInterface callback) {
            this.callback = callback;
        }
    }

}
