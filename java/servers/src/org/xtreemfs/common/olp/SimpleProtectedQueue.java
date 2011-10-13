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
import org.xtreemfs.common.stage.AutonomousComponent.AdmissionRefusedException;
import org.xtreemfs.foundation.logging.Logging;

/**
 * <p>Basically this queue implementation follows a FIFO-ordering considering high priority requests
 * to be processed before low priority requests.</p>
 *  
 * @author flangner
 * @version 1.00, 09/01/11
 * @see StageQueue
 * <R> - type for the queued requests. Has to extend {@link AugmentedRequest}.
 */
class SimpleProtectedQueue<R extends AugmentedRequest> implements StageQueue<R> {
    
    /**
     * <p>The queue to buffer the high priority requests before their processing.</p>
     */
    private final Queue<OLPStageRequest<R>> high = new LinkedList<OLPStageRequest<R>>();
    
    /**
     * <p>The queue to buffer the low priority requests before their processing.</p>
     */
    private final Queue<OLPStageRequest<R>> low = new LinkedList<OLPStageRequest<R>>();
    
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
        final boolean hasPriority = request.hasHighPriority() || rq.hasHighPriority();
        
        try {
            
            if (!rq.isRecycled()) {
                olp.hasAdmission(request, rq.getSize());
            }
            olp.obtainAdmission(request.getType(), rq.getSize(), hasPriority, request.isNativeInternalRequest());
            if (hasPriority) {
                
                high.add(rq);
                
                // check the outdistanced low priority requests 
                Iterator<OLPStageRequest<R>> iter = low.iterator();
                while (iter.hasNext()) {
                    
                    OLPStageRequest<R> next = iter.next();
                    try {
                        
                        olp.hasAdmission(next.getRequest(), next.getSize());
                    } catch (AdmissionRefusedException error) {
                        
                        iter.remove();
                        next.voidMeasurments();
                        next.getCallback().failed(error);
                        olp.depart(next);
                    }
                }
            } else {
                
                low.add(rq);
            }
            
            // wake up the stage if necessary
            if ((high.size() == 0 && low.size() == 1) || (high.size() == 1 && low.size() == 0)) {
                
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

        if (high.size() == 0 && low.size() == 0) {
            wait(timeout);
        }
        
        OLPStageRequest<R> result = high.poll();
        return (S) ((result == null) ? low.poll() : result);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.StageQueue#getLength()
     */
    @Override
    public synchronized int getLength() {
        
        return high.size() + low.size();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {
        
        final StringBuilder builder = new StringBuilder();
        
        builder.append("High priority requests:\n");
        for (OLPStageRequest<R> rq : high) {
            builder.append(rq.toString() + "\n");
        }
        builder.append("\n");
        
        builder.append("Low priority requests:\n");
        for (OLPStageRequest<R> rq : low) {
            builder.append(rq.toString() + "\n");
        }
        builder.append("\n");
        
        return builder.toString();
    }
}