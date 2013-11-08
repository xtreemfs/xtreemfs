/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.stages;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.scheduler.SchedulerRequest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class Stage<Args, Callback> extends LifeCycleThread {
    
    /**
     * queue containing all requests
     */
    protected BlockingQueue<StageRequest> stageQueue;
    
    private final int queueCapacity;

    /**
     * set to true if stage should shut down
     */
    protected volatile boolean            quit;


    public Stage(String stageName, int queueCapacity) {
        
        super(stageName);
        stageQueue = new LinkedBlockingQueue<StageRequest>();
        this.queueCapacity = queueCapacity;
        this.quit = false;
        

    }
    
    /**
     * send an request for a stage operation
     *
     * @param stageOp
     * @param args
     * @param request
     * @param callback
     */
    protected void enqueueOperation(int stageOp, Args args, SchedulerRequest request, Callback callback) {
        enqueueOperation(stageOp, args, request, null, callback);
    }

    /**
     * 
     * send an request for a stage operation
     * 
     * @param stageOp
     *            stage op number
     * @param args
     *            arguments
     * @param request
     *            request
     * @param callback
     *            callback
     * @param createdViewBuffer
     *            an optional additional view buffer to the data, which will be
     *            freed if the request needs to be dropped due to overload
     */
    protected void enqueueOperation(int stageOp, Args args, SchedulerRequest request, ReusableBuffer createdViewBuffer,
            Callback callback) {

        if (request == null) {
            try {
                stageQueue.put(new StageRequest(stageOp, args, request, callback));
            } catch (InterruptedException e) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                        "Failed to queue internal request due to InterruptedException:");
                Logging.logError(Logging.LEVEL_DEBUG, this, e);
            }
        } else {
            if (stageQueue.size() < queueCapacity) {
                try {
                    stageQueue.put(new StageRequest(stageOp, args, request, callback));
                } catch (InterruptedException e) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                            "Failed to queue external request due to InterruptedException:");
                    Logging.logError(Logging.LEVEL_DEBUG, this, e);
                }
            } else {
                // Make sure that the data buffer is returned to the pool if
                // necessary, as some operations create view buffers on the
                // data. Otherwise, a 'finalized but not freed before' warning
                // may occur.
                if (createdViewBuffer != null) {
                    assert (createdViewBuffer.getRefCount() >= 2);
                    BufferPool.free(createdViewBuffer);
                }
//                Logging.logMessage(Logging.LEVEL_WARN, this, "stage is overloaded, request %d for %s dropped",
//                        request.getRequestId(), request.getFileId());
//                Todo implement requestId and FileId in SchedulerRequest ? What else to use?
                request.sendInternalServerError(new IllegalStateException("server overloaded, request dropped"));
            }
        }
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
        return stageQueue.size();
    }
    
    @Override
    public void run() {
        
        notifyStarted();
        
        while (!quit) {
            try {
                final StageRequest op = stageQueue.take();
                
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



    /**
     * Handles the actual execution of a stage request. Must be implemented by
     * all stages.
     * 
     * @param stageRequest
     *            the stage method to execute
     */
    protected abstract void processMethod(StageRequest<Args, Callback> stageRequest);
    
    public static interface NullCallback {
        public void callback(ErrorResponse ex);
    }


    /**
     * Generic StageRequest.
     *
     * @param <Args> the type of the arguments
     * @param <Callback> the type of the callback
     */
    public static final class StageRequest<Args,Callback> {
        
        private int      stageMethod;

        private Args     args;

        private Callback callback;

        private SchedulerRequest schedRequest;


        /**
         * Constructor for StageRequest
         * @param stageMethod the stage method to be used on the StageRequest
         * @param args the arguments of the StageRequest
         * @param request the SchedulerRequest (if any)
         * @param callback the callback object
         */
        public StageRequest(int stageMethod, Args args, SchedulerRequest request, Callback callback) {
            this.args = args;
            this.stageMethod = stageMethod;
            this.schedRequest = request;
            this.callback = callback;
        }
        
        public int getStageMethod() {
            return stageMethod;
        }
        
        public Args getArgs() {
            return args;
        }
        
        public Callback getCallback() {
            return callback;
        }

        public SchedulerRequest getSchedRequest() {
            return schedRequest;
        }

        public void sendInternalServerError(Throwable cause) {
            if (schedRequest != null) {
                schedRequest.sendInternalServerError(cause);
            } else {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "internal server error in internal event: %s",
                    cause.toString());
                Logging.logError(Logging.LEVEL_ERROR, this, cause);
            }
        }
    }

}
