/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * <p>Structure to maintain the performance and waiting times of subsequent stages. Synchronization is achieved through
 * atomic spin-locks for a maximal throughput. Because lost updates on reads are allowed we only need to synchronize 
 * updates on the maintained information.</p>
 * 
 * @author flangner
 * @version 1.00, 09/01/11
 */
class SuccessorPerformanceInformation {
    
    /**
     * <p>Structure to maintain the fixed processing time averages of straight following successors of this stage and 
     * their maximum (worst-case assessment).</p>
     */
    private final AtomicLongArray[]               fixedProcessingTimeAverages;
    
    /**
     * <p>Structure to maintain the variable processing time averages of straight following successors of this stage and 
     * their maximum (worst-case assessment).</p>
     */
    private final AtomicLongArray[]               variableProcessingTimeAverages;
    
    /**
     * <p>Structure to maintain the waiting times of straight following successors of this stage and their maximum 
     * (worst-case assessment).</p>
     */
    private final AtomicLongArray                 waitingTimes;
    
    /**
     * <p>Structure to maintain the waiting times for priority requests of straight following successors of this stage 
     * and their maximum (worst-case assessment).</p>
     */
    private final AtomicLongArray                 priorityWaitingTimes;
    
    /**
     * <p>Pointer for the index of maximal values of all collected performance information of the successors.</p>
     */
    private final int                             resultIndex;
    
    /**
     * <p>Mapping of unique stageID to local successor {@link PerformanceInformation} management.</p>
     */
    private final ConcurrentMap<Integer, Integer> stageIDMapping;
    private final AtomicInteger                   currentStageID;
    
    /**
     * <p>Initializes fields for collecting performance information of straight subsequent stages.</p>
     * 
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel subsequent stages.
     */
    SuccessorPerformanceInformation(int numTypes, int numSubsequentStages) {
        
        currentStageID = new AtomicInteger(0);
        stageIDMapping = new ConcurrentHashMap<Integer, Integer>(numSubsequentStages);
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

    /**
     * <p>Method to get the worst case waiting time for requests of low priority, that would now approach the subsequent
     * stages.</p>
     * 
     * @return the longest waiting time for a low priority request that would leave this stage right now.
     */
    double getWaitingTime() {
        
        return Double.longBitsToDouble(waitingTimes.get(resultIndex));
    }
    
    /**
     * <p>Method to get the worst case waiting time for requests of high priority, that would now approach the 
     * subsequent stages.</p>
     * 
     * @return the longest waiting time for a high priority request that would leave this stage right now.
     */
    double getPriorityWaitingTime() {
        
        return Double.longBitsToDouble(priorityWaitingTimes.get(resultIndex));
    }
    
    /**
     * <p>Method to get the worst case fixed processing time for requests, that would now approach the subsequent 
     * stages.</p>
     * 
     * @return the longest fixed processing time for a request that would leave this stage right now.
     */
    double getFixedProcessingTime(int type) {
        
        return Double.longBitsToDouble(fixedProcessingTimeAverages[type].get(resultIndex));
    }
    
    /**
     * <p>Method to get the worst case variable processing time for requests, that would now approach the subsequent 
     * stages.</p>
     * 
     * @return the longest variable processing time for a request that would leave this stage right now.
     */
    double getVariableProcessingTime(int type) {
        
        return Double.longBitsToDouble(variableProcessingTimeAverages[type].get(resultIndex));
    }
    
    /**
     * <p>Method to handle {@link PerformanceInformation} received by straight subsequent parallel stages.</p>
     * 
     * @param performanceInformation - {@link PerformanceInformation} to process.
     */
    void updatePerformanceInformation(PerformanceInformation performanceInformation) {
        
        Integer id = stageIDMapping.get(performanceInformation.id);
        if (id == null) {
            id = currentStageID.getAndIncrement();
            Integer suc = stageIDMapping.put(performanceInformation.id, id);
            assert (suc == null);
        }
        
        updateArray(id, performanceInformation.waitingTime, waitingTimes);
        updateArray(id, performanceInformation.priorityWaitingTime, priorityWaitingTimes);
        updateProcessingTime(id, performanceInformation.fixedProcessingTimeAverages, fixedProcessingTimeAverages);
        updateProcessingTime(id, performanceInformation.variableProcessingTimeAverages, variableProcessingTimeAverages);
    }
    
/*
 * private methods    
 */
    
    /**
     * <p>Will update the maintained processing times to the new received ones.</p>
     * 
     * @param id - identifier of the stage that is sender of the received processing information.
     * @param processingTimes - the new processing times.
     * @param processingTimeAverages - structures that maintains the old processing times and their maximum.
     */
    private void updateProcessingTime(int id, double[] processingTimes, AtomicLongArray[] processingTimeAverages) {
        
        int numTypes = processingTimes.length;
        for (int i = 0; i < numTypes; i++) {
            updateArray(id, processingTimes[i], processingTimeAverages[i]);
        }
    }

    /**
     * <p>Thread safe method to update the given value at the given id of the given array. Will compute a new maximum
     * if necessary. Synchronization is realized using a spin-lock mechanism to avoid the loss of performance on 
     * updating the information with non or low concurrency.</p>
     * 
     * @param id - identifier of the stage and pointer to the field to update.
     * @param newValue - the new value to use.
     * @param array - structure that maintains the old values and their maximum.
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