/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.stages;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.mrc.MRCRequest;

public abstract class MRCStage extends LifeCycleThread {
    
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
        FINISH
        
    }
    
    /**
     * queue containing all requests
     */
    protected BlockingQueue<StageMethod> q;
    
    /**
     * set to true if stage should shut down
     */
    protected volatile boolean           quit;
    
    public AtomicInteger                 _numRq, _maxRqTime, _minRqTime;
    
    public AtomicLong                    _sumRqTime;
    
    public MRCStage(String stageName) {
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
    public void enqueueOperation(MRCRequest rq, int method, MRCStageCallbackInterface callback) {
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
            MRCRequest rq = null;
            try {
                final StageMethod op = q.take();
                
                if (op.isInternalRequest()) {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                                "processing internal request method %d", op.getStageMethod());
                    processInternalRequest(op);

                } else {
                    rq = op.getRq();

                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                                "processing request XID=%d method %d", rq.getRPCRequest().getHeader().getCallId(),
                                op.getStageMethod());

                    processMethod(op);
                }
                
            } catch (InterruptedException ex) {
                break;
            } catch (Throwable ex) {
                this.notifyCrashed(ex);
                break;
            }
        }
        
        notifyStopped();
    }
    
    /**
     * Handles the actual execution of a stage method. Must be implemented by
     * all stages.
     * 
     * @param method
     *            the stage method to execute
     */
    protected abstract void processMethod(StageMethod method);
    
    /**
     * Handles the actual execution of an internal request method. Must be implemented by
     * all stages.
     * 
     * @param method
     *            the stage method to execute
     */
    protected abstract void processInternalRequest(StageMethod method);

    protected static final class StageMethod {
        private MRCRequest                rq;

        private MRCInternalRequest        internalRq;

        private int                       stageMethod;

        private MRCStageCallbackInterface callback;
        
        public StageMethod(MRCRequest rq, int stageMethod, MRCStageCallbackInterface callback) {
            this.rq = rq;
            this.internalRq = null;
            this.stageMethod = stageMethod;
            this.callback = callback;
        }

        public StageMethod(MRCInternalRequest internalRq, int stageMethod, MRCStageCallbackInterface callback) {
            this.rq = null;
            this.internalRq = internalRq;
            this.stageMethod = stageMethod;
            this.callback = callback;
        }
        
        public int getStageMethod() {
            return stageMethod;
        }
        
        public void setStageMethod(int stageMethod) {
            this.stageMethod = stageMethod;
        }
        
        public boolean isInternalRequest() {
            return (internalRq != null);
        }

        public MRCInternalRequest getInternalRequest() {
            return internalRq;
        }

        public MRCRequest getRq() {
            return rq;
        }
        
        public void setRq(MRCRequest rq) {
            this.rq = rq;
        }
        
        public MRCStageCallbackInterface getCallback() {
            return callback;
        }
        
        public void setCallback(MRCStageCallbackInterface callback) {
            this.callback = callback;
        }
    }
    
}
