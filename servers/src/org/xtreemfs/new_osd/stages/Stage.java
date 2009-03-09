/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.new_osd.stages;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.new_osd.OSDRequest;

public abstract class Stage extends LifeCycleThread {

    /**
     * queue containing all requests
     */
    protected BlockingQueue<StageRequest> q;

    /**
     * set to true if stage should shut down
     */
    protected volatile boolean           quit;

    public AtomicInteger         _numRq, _maxRqTime, _minRqTime;
    public AtomicLong            _sumRqTime;

    public Stage(String stageName) {
        super(stageName);
        q = new LinkedBlockingQueue<StageRequest>();
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
    protected void enqueueOperation(int stageOp, Object[] args, OSDRequest request, Object callback) {
        //rq.setEnqueueNanos(System.nanoTime());
        q.add(new StageRequest(stageOp, args, request, callback));
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
            try {
                final StageRequest op = q.take();

                processMethod(op);

            } catch (InterruptedException ex) {
                break;
            } catch (Exception ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                this.notifyCrashed(ex);
                break;
            }
        }

        notifyStopped();
    }

    protected void calcRequestDuration(OSDRequest rq) {
        /*long d = (System.nanoTime()-rq.getEnqueueNanos())/100000l;
        _numRq.incrementAndGet();
        if (_minRqTime.get() > d)
            _minRqTime.set((int)d);
        if (_maxRqTime.get() < d)
            _maxRqTime.set((int)d);
        _sumRqTime.addAndGet(d);*/
    }

    /**
     * Handles the actual execution of a stage method. Must be implemented by
     * all stages.
     *
     * @param method
     *            the stage method to execute
     */
    protected abstract void processMethod(StageRequest method);

    public static interface NullCallback {
        public void callback(Exception ex);
    }

    protected static final class StageRequest {

        private int                    stageMethod;

        private Object                 callback;

        private Object[]               args;

        private final OSDRequest       request;

        public StageRequest(int stageMethod, Object[] args, OSDRequest request, Object callback) {
            this.args = args;
            this.stageMethod = stageMethod;
            this.callback = callback;
            this.request = request;
        }

        public int getStageMethod() {
            return stageMethod;
        }

        public Object[] getArgs() {
            return args;
        }

        public Object getCallback() {
            return callback;
        }

        public void sendInternalServerError(Throwable cause) {
            if (request != null) {
                request.sendInternalServerError(cause);
            } else {
                Logging.logMessage(Logging.LEVEL_ERROR, this,"internal server error in internal event: "+cause);
                Logging.logMessage(Logging.LEVEL_ERROR, this,cause);
            }
        }
    }

}
