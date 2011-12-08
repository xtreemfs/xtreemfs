/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Implementation of {@link Monitor} that will use a slow-start like algorithm to determine the best sample amount 
 * before average-calculation.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/08/11
 */
class AdvancedMonitor extends Monitor {
        
    /**
     * <p>Time in ms for the difference between two measurement averages.</p>
     */
    private final static double  AVERAGE_DIFFERENCE_THRESHOLD = 1.0;
    
    /**
     * <p>Factor of the difference between two measurement averages.</p>
     */
    private final static double  AVERAGE_QUOTIEN_THRESHOLD = 2.0;
    
    /**
     * <p>Contains the current counts of samples to measure before aggregation for different types of requests.</p>
     */
    private final int[]          samplesToMeasure;
    
    /**
     * <p>Contains historical fixed time averages of the last summary for comparison.</p>
     */
    private final double[]       fixedTimeAverages; 
        
    /**
     * <p>Constructor initializing necessary fields for collecting measurement samples.</p>
     * 
     * @param isForInternalRequests - true if the performance averages are measured for internal requests, 
     *                            false otherwise.
     * @param numTypes - amount of different request types expected.
     * @param listener - to send the summarized performance information to.
     * 
     * @see Monitor
     */
    AdvancedMonitor(PerformanceMeasurementListener listener, int numTypes, boolean isForInternalRequests) {
        super(listener, numTypes, isForInternalRequests);
        
        samplesToMeasure = new int[numTypes];
        
        fixedTimeAverages = new double[numTypes];
        
        for (int i = 0; i < numTypes; i++) {
            
            samplesToMeasure[i] = INITIAL_SAMPLE_AMOUNT;
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor#record(int, long, double)
     */
    @Override
    void record(int type, long size, double processingTime) {

        // record measurement
        processingTimeMeasurements[type].add(processingTime);
        sizeMeasurements[type].add((double) size);
        
        // summarize samples if necessary
        if(processingTimeMeasurements[type].size() == samplesToMeasure[type]) {
            
            double avgT = avg(processingTimeMeasurements[type]);
            final double avgS = avg(sizeMeasurements[type]);
            
            // if we can assume this request-type has no variable processing time component
            if (avgS == 0.0 && historicalVariableProcessingTimes[type] == 0.0) {
                
                avgT = slowStart(avgT, type);
                
            // we have to reset to the initial sample amount to prevent overreaction on the first encounter of variable
            // processing time
            } else if (historicalVariableProcessingTimes[type] == 0.0 && 
                       samplesToMeasure[type] < INITIAL_SAMPLE_AMOUNT) {
                
                samplesToMeasure[type] = INITIAL_SAMPLE_AMOUNT;
                return;
            }
            
            final double[] result = estimateLeastSquares(type, avgT, avgS);
            
            slowStart(result, type);
            
            publishCollectedData(type, result);

            // clear measurements
            processingTimeMeasurements[type].clear();
            sizeMeasurements[type].clear();
        }        
    }
    
/*
 * private methods
 */
    
    /**
     * <p>Method to apply a slow-start alike mechanism to the amount of samples to measure before summary.</p>
     * 
     * @param current
     * @param type
     * @return the average value to use after the evaluation of the slow-start criterion.
     */
    private final double slowStart(double current, int type) {
        
        // incomplete historical data - nothing to compare to
        if (fixedTimeAverages[type] == 0.0) {
            
            return current;
        }
        
        double result = current;
     
        // processing time decreased
        if (current < fixedTimeAverages[type]) {
            
            // difference becomes inconclusive -> decrease sample amount to measure to increase sensitivity
            if ((fixedTimeAverages[type] - current) < AVERAGE_DIFFERENCE_THRESHOLD) {
                
                samplesToMeasure[type] = (samplesToMeasure[type] > 1) ? (samplesToMeasure[type] - 1) : 1;
            // difference is big -> increase sample amount to decrease jitter
            } else if ((fixedTimeAverages[type] / current) > AVERAGE_QUOTIEN_THRESHOLD) {
                
                samplesToMeasure[type] *= 2;
                result = (current + fixedTimeAverages[type]) / 2.0;
            }
        
        // processing time increased or remained unchanged
        } else {
            
            // difference becomes inconclusive -> decrease sample amount to measure to increase sensitivity
            if ((current - fixedTimeAverages[type]) < AVERAGE_DIFFERENCE_THRESHOLD) {
                
                samplesToMeasure[type] = (samplesToMeasure[type] > 1) ? (samplesToMeasure[type] / 2) : 1;
            // difference is big -> increase sample amount to decrease jitter
            } else if ((current / fixedTimeAverages[type]) > AVERAGE_QUOTIEN_THRESHOLD) {
                
                samplesToMeasure[type]++;
            }
        }
        
        return result;
    }
    
    /**
     * <p>Method to apply a slow-start alike mechanism to the amount of samples to measure before summary.</p>
     * 
     * @param current
     * @param type
     * @return the average value to use after the evaluation of the slow-start criterion.
     */
    private final double[] slowStart(double[] current, int type) {
                
        // incomplete historical data - nothing to compare to
        if (historicalFixedProcessingTimes[type] == 0.0 || historicalVariableProcessingTimes[type] == 0.0) {
            return current;
        }
        
        final double fixedDiff = current[0] - historicalFixedProcessingTimes[type];
        final double variableDiff = current[1] - historicalVariableProcessingTimes[type];
        double[] result = current;
        
        // processing time decreased -> check deviation difference
        if (fixedDiff < 0 && variableDiff < 0) {
        
            // difference becomes inconclusive -> decrease sample amount to measure to increase sensitivity
            if (Math.abs(fixedDiff) < AVERAGE_DIFFERENCE_THRESHOLD && 
                Math.abs(variableDiff) < AVERAGE_DIFFERENCE_THRESHOLD) {
                
                samplesToMeasure[type] = (samplesToMeasure[type] > 1) ? (samplesToMeasure[type] - 1) : 1;
            // difference is big -> increase sample amount to decrease jitter
            } else if ((historicalFixedProcessingTimes[type] / current[0]) > AVERAGE_QUOTIEN_THRESHOLD &&
                    (historicalVariableProcessingTimes[type] / current[1]) > AVERAGE_QUOTIEN_THRESHOLD) {
                
                samplesToMeasure[type] *= 2;
                result[0] = (current[0] + historicalFixedProcessingTimes[type]) / 2.0;
                result[1] = (current[1] + historicalVariableProcessingTimes[type]) / 2.0;
            }
            
        // processing time deviation is inconclusive -> increase amount of samples to measure   
        } else if ((fixedDiff < 0 && variableDiff > 0) || (fixedDiff > 0 && variableDiff < 0)) {
            
            samplesToMeasure[type]++;
        // processing time increased -> check deviation difference
        } else if (fixedDiff > 0 && variableDiff > 0){
            
            // difference becomes inconclusive -> decrease sample amount to measure to increase sensitivity
            if (fixedDiff < AVERAGE_DIFFERENCE_THRESHOLD && variableDiff < AVERAGE_DIFFERENCE_THRESHOLD) {
                
                samplesToMeasure[type] = (samplesToMeasure[type] > 1) ? (samplesToMeasure[type] / 2) : 1;
            // difference is big -> increase sample amount to decrease jitter
            } else if ((current[0] / historicalFixedProcessingTimes[type]) > AVERAGE_QUOTIEN_THRESHOLD &&
                    (current[1] / historicalVariableProcessingTimes[type]) > AVERAGE_QUOTIEN_THRESHOLD) {
                
                samplesToMeasure[type]++;
            }
        }
        
        return result;
    }
}
