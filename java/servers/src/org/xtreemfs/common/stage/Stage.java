/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.stage;

import org.xtreemfs.common.stage.StageRequest.Callback;
import org.xtreemfs.foundation.LifeCycleThread;

/**
 * <p>Generalized stage to be used at all services.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/05/11
 */
public abstract class Stage<R extends Request> extends LifeCycleThread {
    
    /**
     * <p>Buffers requests before they are being processed.</p>
     */
    private final StageQueue<R>      queue;
    
    /**
     * <p>Set to true if stage should shut down.</p>
     */
    protected volatile boolean  quit = false;
    
    /**
     * <p>Initializes a stage with given name and queue implementation.</p>
     * 
     * @param name
     * @param queue
     */
    public Stage(String name, StageQueue<R> queue) {
        super(name);
        this.queue = queue;
    }
    
    /**
     * <p>Enqueues a request including all necessary information for its processing at this stage.</p>
     * 
     * @param stageMethodId - the identifier for the method to use during processing.
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request.
     */
    public void enqueueOperation(int stageMethodId, Object[] args, R request, Callback callback) {
        queue.enqueue(new StageRequest<R>(stageMethodId, args, request, callback));
    }
    
    /**
     * <p>Handles the actual execution of a stage method. Must be implemented by
     * all stages.</p>
     * 
     * @param method - the stage method to execute.
     */
    public abstract void processMethod(StageRequest<R> method);
    
    /**
     * <p>Get current number of requests in the queue.</p>
     * 
     * @return queue length.
     */
    public final int getQueueLength() {
        return queue.getLength();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Thread#start()
     */
    @Override
    public synchronized void start() {
        quit = false;
        super.start();
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.foundation.LifeCycleThread#shutdown()
     */
    @Override
    public void shutdown() {
        quit = true;
        interrupt();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        
        notifyStarted();
        
        while (!quit) {
            try {
                processMethod(queue.take());
                
            } catch (InterruptedException ex) {
                break;
            } catch (Exception ex) {
                notifyCrashed(ex);
                break;
            }
        }
        
        notifyStopped();
    }
}
