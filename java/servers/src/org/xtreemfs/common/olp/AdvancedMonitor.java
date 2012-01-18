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
 * @version 1.01, 09/08/11
 */
class AdvancedMonitor extends Monitor {
        
    /**
     * <p>Time in ms for the difference between two measurement averages. (1 µs)</p>
     */
    private final static double  AVERAGE_DIFFERENCE_THRESHOLD = 0.001;
    
    /**
     * <p>Factor of the difference between two measurement averages.</p>
     */
    private final static double  AVERAGE_QUOTIEN_THRESHOLD = 2.0;
        
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
        
        fixedTimeAverages = new double[numTypes];
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor#record(int, long, double)
     */
    @Override
    void record(int type, long size, double processingTime) {

        // record measurement
        processingTimeMeasurements[type].put(processingTime);
        sizeMeasurements[type].put((double) size);
            
        if (processingTimeMeasurements[type].isCharged() && sizeMeasurements[type].isCharged()) {
        
            double avgT = processingTimeMeasurements[type].avg();
            final double avgS = sizeMeasurements[type].avg();
            
            // if we can assume this request-type has no variable processing time component
            if (avgS == 0.0 && historicalVariableProcessingTimes[type] == 0.0) {
                
                slowStart(avgT, type);
                
            // we have to reset to the initial sample amount to prevent overreaction on the first encounter of variable
            // processing time
            } else if (historicalVariableProcessingTimes[type] == 0.0) {
                
                boolean resetAvg = processingTimeMeasurements[type].resetCapacity() ||
                                   sizeMeasurements[type].resetCapacity();
                
                if (resetAvg) return;
            }
            
            final double[] result = estimateLeastSquares(type, avgT, avgS);
            
            slowStart(result, type);
            
            publishCollectedData(type, result);
    
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
    private final void slowStart(final double current, final int type) {
        
        // incomplete historical data - nothing to compare to
        if (fixedTimeAverages[type] == 0.0) { 
            return; 
        }
     
        // processing time decreased
        if (current < fixedTimeAverages[type]) {
            
            // difference becomes inconclusive -> decrease sample amount to measure to increase sensitivity
            if ((fixedTimeAverages[type] - current) < AVERAGE_DIFFERENCE_THRESHOLD) {
                
                processingTimeMeasurements[type].shrinkCapacity();
                sizeMeasurements[type].shrinkCapacity();
            // difference is big -> increase sample amount to decrease jitter
            } else if ((fixedTimeAverages[type] / current) > AVERAGE_QUOTIEN_THRESHOLD) {
                
                processingTimeMeasurements[type].doubleCapacity();
                sizeMeasurements[type].doubleCapacity();
            }
        // processing time increased or remained unchanged
        } else {
            
            // difference becomes inconclusive -> decrease sample amount to measure to increase sensitivity
            if ((current - fixedTimeAverages[type]) < AVERAGE_DIFFERENCE_THRESHOLD) {
                
                processingTimeMeasurements[type].halveCapacity();
                sizeMeasurements[type].halveCapacity();
            // difference is big -> increase sample amount to decrease jitter
            } else if ((current / fixedTimeAverages[type]) > AVERAGE_QUOTIEN_THRESHOLD) {
                
                processingTimeMeasurements[type].enlargeCapacity();
                sizeMeasurements[type].enlargeCapacity();
            }
        }
    }
    
    /**
     * <p>Method to apply a slow-start alike mechanism to the amount of samples to measure before summary.</p>
     * 
     * @param current
     * @param type
     * @return the average value to use after the evaluation of the slow-start criterion.
     */
    private final void slowStart(final double[] current, final int type) {
                
        // incomplete historical data - nothing to compare to
        if (historicalFixedProcessingTimes[type] == 0.0 || historicalVariableProcessingTimes[type] == 0.0) {
            return;
        }
        
        final double fixedDiff = current[0] - historicalFixedProcessingTimes[type];
        final double variableDiff = current[1] - historicalVariableProcessingTimes[type];
        
        // processing time decreased -> check deviation difference
        if (fixedDiff < 0 && variableDiff < 0) {
        
            // difference becomes inconclusive -> decrease sample amount to measure to increase sensitivity
            if (Math.abs(fixedDiff) < AVERAGE_DIFFERENCE_THRESHOLD && 
                Math.abs(variableDiff) < AVERAGE_DIFFERENCE_THRESHOLD) {
                
                processingTimeMeasurements[type].shrinkCapacity();
                sizeMeasurements[type].shrinkCapacity();
            // difference is big -> increase sample amount to decrease jitter
            } else if ((historicalFixedProcessingTimes[type] / current[0]) > AVERAGE_QUOTIEN_THRESHOLD &&
                    (historicalVariableProcessingTimes[type] / current[1]) > AVERAGE_QUOTIEN_THRESHOLD) {
                
                processingTimeMeasurements[type].doubleCapacity();
                sizeMeasurements[type].doubleCapacity();
            }
        // processing time deviation is inconclusive -> increase amount of samples to measure   
        } else if ((fixedDiff < 0 && variableDiff > 0) || (fixedDiff > 0 && variableDiff < 0)) {
            
            processingTimeMeasurements[type].enlargeCapacity();
            sizeMeasurements[type].enlargeCapacity();
        // processing time increased -> check deviation difference
        } else if (fixedDiff > 0 && variableDiff > 0){
            
            // difference becomes inconclusive -> decrease sample amount to measure to increase sensitivity
            if (fixedDiff < AVERAGE_DIFFERENCE_THRESHOLD && variableDiff < AVERAGE_DIFFERENCE_THRESHOLD) {
                
                processingTimeMeasurements[type].halveCapacity();
                sizeMeasurements[type].halveCapacity();
            // difference is big -> increase sample amount to decrease jitter
            } else if ((current[0] / historicalFixedProcessingTimes[type]) > AVERAGE_QUOTIEN_THRESHOLD &&
                    (current[1] / historicalVariableProcessingTimes[type]) > AVERAGE_QUOTIEN_THRESHOLD) {
                
                processingTimeMeasurements[type].enlargeCapacity();
                sizeMeasurements[type].enlargeCapacity();
            }
        }
    }
}
