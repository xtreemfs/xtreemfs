/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.concurrent.atomic.AtomicLongArray;

import org.xtreemfs.common.olp.Monitor.PerformanceMeasurementListener;

/**
 * 
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to 
 * be single threaded.</p>
 * 
 * @author fx.langner
 * @version 1.00, 08/25/11
 */
class Controller implements PerformanceMeasurementListener {
    
    private final int[]                           queueComposition;
    private final long[]                          queueBandwidthComposition;
    
    private final AtomicLongArray                 fixedProcessingTimeAverages;
    private final AtomicLongArray                 variableProcessingTimeAverages;
    
    private final SuccessorPerformanceInformation successorPerformanceInformation;
    
    /**
     * 
     * @param numTypes
     * @param numSubsequentStages
     */
    Controller(int numTypes, int numSubsequentStages) {
        
        queueComposition = new int[numTypes];
        queueBandwidthComposition = new long[numTypes];
        
        fixedProcessingTimeAverages = new AtomicLongArray(numTypes);
        variableProcessingTimeAverages = new AtomicLongArray(numTypes);
        
        successorPerformanceInformation = new SuccessorPerformanceInformation(numTypes, numSubsequentStages);
    }
      
    double estimateProcessingTime(int type, long size, boolean hasPriority) {
        
        double result = Double.longBitsToDouble(fixedProcessingTimeAverages.get(type));
        if (size > 0L) {
            result += Double.longBitsToDouble(variableProcessingTimeAverages.get(type)) * size;
        }
        result += estimateIdleTime();
        return result;
    }
    
    void enterRequest(int type, long size, boolean hasPriority) {
        
        queueComposition[type]++;
        queueBandwidthComposition[type] += size;
    }
    
    void quitRequest(int type, long size, boolean hasPriority) {
        
        queueComposition[type]--;
        queueBandwidthComposition[type] -= size;
    }
    
    void updateSuccessorInformation(PerformanceInformation performanceInformation) {
        
        successorPerformanceInformation.updatePerformanceInformation(performanceInformation);
    }
    
    PerformanceInformation composePerformanceInformation(int id) {
        
        int numTypes = fixedProcessingTimeAverages.length();
        double[] fixedProcessingTime = new double[numTypes];
        double[] variableProcessingTime = new double[numTypes];
        
        for (int i = 0; i < numTypes; i++) {
            fixedProcessingTime[i] = Double.longBitsToDouble(fixedProcessingTimeAverages.get(i));
            variableProcessingTime[i] = Double.longBitsToDouble(variableProcessingTimeAverages.get(i));
        }

        return new PerformanceInformation(id, 
                                          fixedProcessingTime, 
                                          variableProcessingTime, 
                                          estimateIdleTime(fixedProcessingTime, variableProcessingTime));
    }

/*
 * methods to be accessed by the Monitor
 */

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor.PerformanceMeasurementListener#updateFixedProcessingTimeAverage(int, double)
     */
    @Override
    public void updateFixedProcessingTimeAverage(int type, double value) {
        
        fixedProcessingTimeAverages.set(type, 
                Double.doubleToLongBits(value + successorPerformanceInformation.getFixedProcessingTime(type)));
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor.PerformanceMeasurementListener#
     *          updateVariableProcessingTimeAverage(int, double)
     */
    @Override
    public void updateVariableProcessingTimeAverage(int type, double value) {
        
        variableProcessingTimeAverages.set(type, 
                Double.doubleToLongBits(value + successorPerformanceInformation.getVariableProcessingTime(type)));
    }
    
/*
 * private methods
 */
    
    /**
     * 
     * @return
     */
    private double estimateIdleTime() {
        double result = successorPerformanceInformation.getIdleTime();
        
        int numTypes = fixedProcessingTimeAverages.length();
        for (int i = 0; i < numTypes; i++) {
            result += Double.longBitsToDouble(fixedProcessingTimeAverages.get(numTypes)) * queueComposition[numTypes];
            result += Double.longBitsToDouble(
                    variableProcessingTimeAverages.get(numTypes)) * queueBandwidthComposition[numTypes];
        }
        
        return result;
    }
    
    /**
     * 
     * @param fixedProcessingTime
     * @param variableProcessingTime
     * @return
     */
    private double estimateIdleTime(double[] fixedProcessingTime, double[] variableProcessingTime) {
        double result = successorPerformanceInformation.getIdleTime();
        
        int numTypes = fixedProcessingTime.length;
        for (int i = 0; i < numTypes; i++) {
            result += fixedProcessingTime[numTypes] * queueComposition[numTypes];
            result += variableProcessingTime[numTypes] * queueBandwidthComposition[numTypes];
        }
        
        return result;
    }
}
