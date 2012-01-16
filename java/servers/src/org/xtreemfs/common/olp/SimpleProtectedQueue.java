/*
 * Copyright (c) 2012 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.LinkedList;
import java.util.Queue;

import org.xtreemfs.common.stage.StageQueue;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.common.stage.AutonomousComponent.AdmissionRefusedException;
import org.xtreemfs.foundation.logging.Logging;

/**
 * <p>Basically this queue implementation follows a FIFO-ordering.</p>
 *  
 * @author flangner
 * @version 1.01, 09/01/11
 * @see StageQueue
 * <R> - type for the queued requests. Has to extend {@link AugmentedRequest}.
 */
class SimpleProtectedQueue<R extends AugmentedRequest> implements StageQueue<R> {
        
    /**
     * <p>The queue to buffer the low priority requests before their processing.</p>
     */
    private final Queue<OLPStageRequest<R>> queue = new LinkedList<OLPStageRequest<R>>();
    
    /**
     * <p>The interface to the Overload-Protection algorithm.</p>
     */
    private final ProtectionAlgorithmCore   olp;
    
    /**
     * <p>Initializes an empty queue with Overload-Protection enabled.</p>
     * 
     * @param olp - the interface to the Overload-Protection algorithm.
     */
    SimpleProtectedQueue(ProtectionAlgorithmCore olp) {
        
        this.olp = olp;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.StageQueue#enqueue(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public synchronized <S extends StageRequest<R>> void enqueue(S stageRequest) {
        
        final OLPStageRequest<R> rq = (OLPStageRequest<R>) stageRequest;
        final R request = rq.getRequest();
        
        try {
            
            if (!rq.isRecycled()) {
                olp.hasAdmission(request, rq.getSize());
            }
            olp.obtainAdmission(request.getType(), rq.getSize(), request.isNativeInternalRequest());
            queue.add(rq);
            
            // wake up the stage if necessary
            if (queue.size() == 1) {
                
                notify();
            }
        } catch (AdmissionRefusedException error) {
            
            rq.getCallback().failed(error);
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "%s @stage OLP state: %s with queue %s", 
                        error.getMessage(), olp.toString(), toString());
            }
        }
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.SimpleProtectedQueue#take(long)
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized <S extends StageRequest<R>> S take(long timeout)
            throws InterruptedException {

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
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {
        
        final StringBuilder builder = new StringBuilder();
                
        builder.append("Low priority requests:\n");
        for (OLPStageRequest<R> rq : queue) {
            builder.append(rq.toString() + "\n");
        }
        builder.append("\n");
        
        return builder.toString();
    }
}