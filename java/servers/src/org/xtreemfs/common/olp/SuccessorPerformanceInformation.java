/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 
 * 
 * @author flangner
 * @version 1.00, 09/01/11
 */
class SuccessorPerformanceInformation {
    
    private final AtomicLongArray[] fixedProcessingTimeAverages;
    private final AtomicLongArray[] variableProcessingTimeAverages;
    private final AtomicLongArray   waitingTimes;
    private final AtomicLongArray   priorityWaitingTimes;
    private final int               resultIndex;
    
    SuccessorPerformanceInformation(int numTypes, int numSubsequentStages) {
        
        fixedProcessingTimeAverages = new AtomicLongArray[numTypes];
        variableProcessingTimeAverages = new AtomicLongArray[numTypes];
        waitingTimes = new AtomicLongArray(numSubsequentStages+1);
        priorityWaitingTimes = new AtomicLongArray(numSubsequentStages+1);
        resultIndex = numSubsequentStages;
        
        for (int i = 0; i < numTypes; i++) {
            fixedProcessingTimeAverages[i] = new AtomicLongArray(numSubsequentStages+1);
            variableProcessingTimeAverages[i] = new AtomicLongArray(numSubsequentStages+1);   
        }
    }
    
/*
 * methods to be accessed by the Controller
 */

    double getWaitingTime() {
        
        return Double.longBitsToDouble(waitingTimes.get(resultIndex));
    }
    
    double getPriorityWaitingTime() {
        
        return Double.longBitsToDouble(priorityWaitingTimes.get(resultIndex));
    }
    
    double getFixedProcessingTime(int type) {
        
        return Double.longBitsToDouble(fixedProcessingTimeAverages[type].get(resultIndex));
    }
    
    double getVariableProcessingTime(int type) {
        
        return Double.longBitsToDouble(variableProcessingTimeAverages[type].get(resultIndex));
    }
    
    void updatePerformanceInformation(PerformanceInformation performanceInformation) {
        
        updateArray(performanceInformation.id, 
                    performanceInformation.waitingTime, 
                    waitingTimes);
        updateArray(performanceInformation.id, 
                    performanceInformation.priorityWaitingTime, 
                    priorityWaitingTimes);
        updateProcessingTime(performanceInformation.id, 
                             performanceInformation.fixedProcessingTimeAverages, 
                             fixedProcessingTimeAverages);
        updateProcessingTime(performanceInformation.id, 
                             performanceInformation.variableProcessingTimeAverages, 
                             variableProcessingTimeAverages);
    }
    
/*
 * private methods    
 */
    
    /**
     * @param id
     * @param processingTime
     * @param processingTimeAverages
     */
    private void updateProcessingTime(int id, double[] processingTime, AtomicLongArray[] processingTimeAverages) {
        int numTypes = processingTime.length;
        for (int i = 0; i < numTypes; i++) {
            updateArray(id, processingTime[i], processingTimeAverages[i]);
        }
    }

    /**
     * 
     * @param id
     * @param newValue
     * @param array
     */
    private void updateArray(int id, double newValue, AtomicLongArray array) {
        
        long newValueL = Double.doubleToLongBits(newValue);
        
        // the specific idle time of the stage identified by id is updated single-threaded
        // esp. there are no concurrency implications
        long oldL = array.getAndSet(id, newValueL);
        double old = Double.doubleToLongBits(oldL);
        
        // try to update the resultIndex value. retry if a concurrent modification is observed
        for (;;) {
            
            // use the currently recognized maximal idle time as concurrent modification indicator
            long currentL = array.get(resultIndex);
            double current = Double.doubleToLongBits(currentL);
            
            if (old == current) {
                // newValue is the new maximum
                if (newValue >= old) {
                    if (array.compareAndSet(resultIndex, currentL, newValueL)) {
                        break;
                    }
                // we need to find a new maximum value
                } else if (newValue < old) {
                    
                    int lengthSubsequentStages = resultIndex - 1;
                    double newMaximum = newValue;
                    for (int i = 0; i < lengthSubsequentStages; i++) {
                        double val = Double.longBitsToDouble(array.get(i));
                        newMaximum = (val > newMaximum) ? val : newMaximum;
                    }
                    
                    if (array.compareAndSet(resultIndex, currentL, Double.doubleToLongBits(newMaximum))) {
                        break;
                    }
                }
            // the newValue is greater than the current value, thats why we update to
            } else if (current < newValue) {
                if (array.compareAndSet(resultIndex, currentL, newValueL)) {
                    break;
                }
            // newValue is smaller than the current value and current value has not changed, then it will be ignored
            } else if (array.compareAndSet(resultIndex, currentL, currentL)) {
                break;
            }
        }
    }
}
