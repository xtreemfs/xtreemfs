/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.stage;

import org.xtreemfs.foundation.LifeCycleThread;

/**
 * <p>Generalized stage to be used at all services.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/05/11
 * 
 * @param <R> - general interface for requests processed by this stage. Extends {@link Request}.
 */
public abstract class Stage<R extends Request> extends LifeCycleThread implements AutonomousComponent<StageRequest<R>> {
    
    /**
     * <p>Buffers requests before they are being processed.</p>
     */
    private final StageQueue<R>  queue;
    
    /**
     * <p>Set to true if stage should shut down.</p>
     */
    private volatile boolean     quit = false;
    
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
     * @param callback - for postprocessing the request, may be null.
     */
    public void enter(int stageMethodId, Object[] args, R request, Callback callback) {
        
        enter(new StageRequest<R>(stageMethodId, args, request, 
                (callback != null) ? callback : Callback.NullCallback.INSTANCE));
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#enter(java.lang.Object)
     */
    @Override
    public void enter(StageRequest<R> request) {
        
        queue.enqueue(request);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#exit(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public void exit(StageRequest<R> request) {}
    
    /**
     * <p>Handles the actual execution of a stage method. Must be implemented by
     * all stages.</p>
     * 
     * @param method - the stage method to execute.
     */
    public abstract void processMethod(StageRequest<R> method);
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#getNumberOfRequests()
     */
    @Override
    public final int getNumberOfRequests() {
        
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
    public void shutdown() throws Exception {
        
        quit = true;
        interrupt();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public final void run() {
        
        notifyStarted();
        
        while (!quit) {
            try {
                
                StageRequest<R> request = queue.take();
                processMethod(request);
                exit(request);
                
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