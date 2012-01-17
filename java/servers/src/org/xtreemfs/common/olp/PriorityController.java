/*
 * Copyright (c) 2012 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to 
 * be single threaded. For root nodes of the processing tree only.</p>
 * 
 * @author fx.langner
 * @version 1.01, 08/25/11
 */
class PriorityController extends Controller {

    /**
     * <p>Composition of external high-priority requests queued at the controlled stage only.</p>
     */
    private final AtomicIntegerArray              priorityQueueComposition;
    /**
     * <p>Processing volume according to the composition of external high-priority requests.</p>
     */
    private final AtomicLongArray                 priorityQueueBandwidthComposition;
    
    /**
     * 
     * @param numTypes
     * @param numInternalTypes
     * @param numSubsequentStages
     */
    PriorityController(int numTypes, int numInternalTypes, int numSubsequentStages) {
        super(numTypes, numInternalTypes, numSubsequentStages);
        
        priorityQueueComposition = new AtomicIntegerArray(numTypes);
        priorityQueueBandwidthComposition = new AtomicLongArray(numTypes);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Controller#estimateWaitingTime(boolean)
     */
    @Override
    public double estimateWaitingTime(final boolean hasPriority) {
                
        if (hasPriority) {
            
            return estimatePriorityWaitingTime();
        } else {
            
            return super.estimateWaitingTime(hasPriority);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Controller#enterRequest(int, long, boolean, boolean)
     */
    @Override
    void enterRequest(int type, long size, boolean hasPriority, boolean isInternalRequest) {
        super.enterRequest(type, size, hasPriority, isInternalRequest);
        
        if (!isInternalRequest && hasPriority) {
                
            priorityQueueComposition.incrementAndGet(type);
            priorityQueueBandwidthComposition.addAndGet(type, size);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Controller#quitRequest(int, long, boolean, boolean)
     */
    @Override
    void quitRequest(int type, long size, boolean hasPriority, boolean isInternalRequest) {
        super.quitRequest(type, size, hasPriority, isInternalRequest);
        
        long check;
        if (!isInternalRequest && hasPriority) {
            
            check = priorityQueueComposition.decrementAndGet(type);
            assert(check >= 0);
            check = priorityQueueBandwidthComposition.addAndGet(type, -size);
            assert(check >= 0);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Controller#composePerformanceInformation(int)
     */
    @Override
    PerformanceInformation composePerformanceInformation(int id) {
        
        throw new UnsupportedOperationException("Priority controller has to be assigned to a root node and thereby " +
        		"has no use-case for composing performance information at all!");
    }

/*
 * private methods
 */
    
    /**
     * @return the estimated waiting time for a high-priority request approaching the stage.
     */
    private double estimatePriorityWaitingTime() {
        
        double result = successorPerformanceInformation.getWaitingTime();
        
        final int numTypes = fixedProcessingTimeAverages.length();
        for (int i = 0; i < numTypes; i++) {
            
            double avg = Double.longBitsToDouble(fixedProcessingTimeAverages.get(i));
            long rqs = priorityQueueComposition.get(i);
            
            result += avg * rqs;
            
            avg = Double.longBitsToDouble(variableProcessingTimeAverages.get(i));
            rqs = priorityQueueBandwidthComposition.get(i);
            
            result += avg * rqs;
        }
        
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Controller#toString()
     */
    @Override
    public String toString() {
        
        final StringBuilder builder = new StringBuilder(super.toString());
        
        final int numTypes = fixedProcessingTimeAverages.length();
        
        builder.append("Priority queue composition (external/internal):\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(priorityQueueComposition.get(i) + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Priority queue bandwidth composition (external/internal):\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(priorityQueueBandwidthComposition.get(i) + "\t");
        }
        builder.append("\n\n");
        
        return builder.toString();
    }
}
