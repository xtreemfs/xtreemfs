/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.stage;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * <p>Generalized stage to be used at all services.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/05/11
 * 
 * @param <R> - requests processed by this stage (from a global view).
 */
public abstract class Stage<R> extends LifeCycleThread implements AutonomousComponent<StageRequest<R>> {
    
    /**
     * <p>Buffers requests before they are being processed.</p>
     */
    private final StageQueue<R> queue;
    
    /**
     * <p>Set to true if stage should shut down.</p>
     */
    private volatile boolean    quit = false;
    
    /**
     * <p>Delay in ms between two cron-Jobs.</p>
     */
    private final long          period;
    
    /**
     * <p>Initializes a stage with given name and queue implementation.</p>
     * 
     * @param name
     * @param queue
     */
    public Stage(String name, StageQueue<R> queue) {
        
        this(name, queue, 0);
    }
    
    /**
     * <p>Initializes a stage with given name and queue implementation.</p>
     * 
     * @param name
     * @param queue
     * @param period - delay in ms between two cron-Jobs.
     */
    public Stage(String name, StageQueue<R> queue, long period) {
        
        super(name);
        this.queue = queue;
        this.period = period;
    }
    
    /**
     * <p>Enqueues a request including all necessary information for its processing at this stage.</p>
     * 
     * @param request - the original request.
     * @param callback - for postprocessing the request, may be null.
     */
    public void enter(R request, Callback callback) {
        
        enter(null, request, callback);
    }
    
    /**
     * <p>Enqueues a request including all necessary information for its processing at this stage.</p>
     * 
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request, may be null.
     */
    public void enter(Object[] args, R request, Callback callback) {
        
        enter(0, args, request, callback);
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
        
        enter(generateStageRequest(stageMethodId, args, request, 
                (callback != null) ? callback : Callback.NullCallback.INSTANCE));
    }
    
    /**
     * <p>Hidden method to actually generate a {@link StageRequest} from the request parameters.
     * 
     * @param stageMethodId - the identifier for the method to use during processing.
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request, may be not be null!
     */
    protected abstract <S extends StageRequest<R>> S generateStageRequest(int stageMethodId, Object[] args, R request, 
            Callback callback);
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#enter(java.lang.Object)
     */
    @Override
    public void enter(StageRequest<R> request) {
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, 
                    "'%s' enters stage [%s].", request.toString(), getName());
        }
        
        queue.enqueue(request);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#exit(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public void exit(StageRequest<R> request) {
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, 
                    "'%s' exits stage [%s].", request.toString(), getName());
        }
    }
    
    /**
     * <p>Handles the actual execution of a stage method. Must be implemented by
     * all stages.</p>
     * 
     * @param stageRequest - the stage method to execute.
     * 
     * @return true, if the request was finished {@link Stage#exit(StageRequest)} should be called, false if it was
     *         re-queued.
     */
    protected abstract <S extends StageRequest<R>> boolean processMethod(S stageRequest);
    
    /**
     * <p>Optional method that is executed if a period was defined within the constructor and is overridden by a 
     * stage implementation.</p>
     */
    protected void chronJob() {
        
        throw new UnsupportedOperationException();
    }
    
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
        long lastCronJob = 0;
        long nextCronJob = period;
        
        while (!quit) {
            try {
                
                StageRequest<R> request = queue.take(nextCronJob);
                
                // calculate the delay for the next cron-job
                if (period > 0) {
                    final long timePassed = TimeSync.getLocalSystemTime() - lastCronJob;
                    nextCronJob = (nextCronJob > timePassed) ? nextCronJob - timePassed : 0L;
                    if (nextCronJob == 0L) {
                    
                        chronJob();
                        lastCronJob = TimeSync.getLocalSystemTime();
                        nextCronJob = nextChronJobDelay();
                    }
                }
                
                if (request != null) {
                    if (processMethod(request)) exit(request);
                }
                
            } catch (InterruptedException ex) {
                
                break;
            } catch (Exception ex) {
                
                notifyCrashed(ex);
                break;
            }
        }
        
        notifyStopped();
    }
    
    /**
     * @return the delay until the next chron-job is executed.
     */
    protected long nextChronJobDelay() {
        
        return period;
    }
}