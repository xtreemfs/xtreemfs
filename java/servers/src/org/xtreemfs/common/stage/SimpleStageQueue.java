/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>Simple queue based on a {@link LinkedList} with optional length limitation.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/13/2011
 * 
 * @param <R> - global request type.
 */
public class SimpleStageQueue<R> implements StageQueue<R> {

    private final Queue<StageRequest<R>> queue = new LinkedList<StageRequest<R>>();
    private final int                    maxLength;
    
    /**
     * <p>Default constructor for unlimited queue-size.</p>
     */
    public SimpleStageQueue() {
        
        this.maxLength = 0;
    }
    
    /**
     * <p>Constructor with a limit for the queue-size at <code>maxQueueLength</code>.</p>
     * 
     * @param maxQueueLength - maximal amount of requests the queue can hold, because of main-memory limitations.
     */
    public SimpleStageQueue(int maxQueueLength) {
        
        assert (maxQueueLength > 0);
        
        this.maxLength = maxQueueLength;  
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.StageQueue#enqueue(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public synchronized <S extends StageRequest<R>> void enqueue(S request) {
        
        if (request.isRecycled() || maxLength == 0 || queue.size() < maxLength) {
            
            queue.add(request);
            
            if (queue.size() == 1) notify();
        } else {
            
            /*
             * IllegalStateException if the element cannot be added at this
             *                       time due to capacity restrictions
             */
            request.getCallback().failed(new IllegalStateException("The element cannot be added at this time due to " +
                        "capacity restrictions."));
        }
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.StageQueue#take(long)
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized <S extends StageRequest<R>> S take(long timeout) throws InterruptedException {

        if (queue.size() == 0) {
            wait(timeout);
        }
        return (S) queue.poll();
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.StageQueue#getLength()
     */
    @Override
    public synchronized int getLength() {
        
        return queue.size();
    }
}