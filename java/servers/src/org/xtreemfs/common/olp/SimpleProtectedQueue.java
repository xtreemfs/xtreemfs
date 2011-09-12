/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.xtreemfs.common.stage.StageQueue;
import org.xtreemfs.common.stage.StageRequest;

/**
 * <p>Basically this queue implementation follows a FIFO-ordering considering high priority requests
 * to be processed before low priority requests.</p>
 *  
 * @author flangner
 * @version 1.00, 09/01/11
 * @see StageQueue
 * <R> - type for the queued requests. Has to extend {@link IRequest}.
 */
class SimpleProtectedQueue<R extends IRequest> implements StageQueue<R> {
    
    /**
     * <p>The queue to buffer the high priority requests before their processing.</p>
     */
    private final Queue<StageRequest<R>> high = new LinkedList<StageRequest<R>>();
    
    /**
     * <p>The queue to buffer the low priority requests before their processing.</p>
     */
    private final Queue<StageRequest<R>> low = new LinkedList<StageRequest<R>>();
    
    /**
     * <p>The interface to the Overload-Protection algorithm.</p>
     */
    private final OverloadProtection     olp;
    
    /**
     * <p>Initializes an empty queue with Overload-Protection enabled.</p>
     * 
     * @param olp - the interface to the Overload-Protection algorithm.
     */
    SimpleProtectedQueue(OverloadProtection olp) {
        this.olp = olp;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.StageQueue#enqueue(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public synchronized void enqueue(StageRequest<R> stageRequest) {
        
        try {
            olp.obtainAdmission(stageRequest.getRequest());
            
            if (stageRequest.getRequest().hasHighPriority()) {
                high.add(stageRequest);
                
                // check the outdistanced low priority requests 
                Iterator<StageRequest<R>> iter = low.iterator();
                while (iter.hasNext()) {
                    StageRequest<R> next = iter.next();
                    try {
                        olp.hasAdmission(stageRequest.getRequest());
                    } catch (Exception error) {
                        
                        iter.remove();
                        next.getCallback().failed(error);
                        olp.depart(next.getRequest());
                    }
                }
            } else {
                
                low.add(stageRequest);
            }
            
            // wake up the stage if necessary
            if ((high.size() == 0 && low.size() == 1) ||
                (high.size() == 1 && low.size() == 0)) {
                notify();
            }
        } catch (Exception error) {
            
            stageRequest.getCallback().failed(error);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.InstrumentedQueue#take()
     */
    @Override
    public synchronized StageRequest<R> take() throws InterruptedException {
        
        if (high.size() == 0 && low.size() == 0) {
            wait();
        }
        
        StageRequest<R> result = high.poll();
        return (result == null) ? low.poll() : result;
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.StageQueue#getLength()
     */
    @Override
    public synchronized int getLength() {
        return high.size() + low.size();
    }
}
