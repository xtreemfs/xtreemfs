/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Simple Monitor implementation that uses the average-metric on a fixed amount of samples for calculating 
 * performance information of this stage.</p>
 * 
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to be single threaded.</p>
 * 
 * @author flangner
 * @version 1.00, 08/18/11
 * @see Monitor
 */
class SimpleMonitor extends Monitor {
        
    /**
     * <p>Constructor initializing necessary fields for collecting measurement samples.</p>
     * 
     * @param isForInternalRequests - true if the performance averages are measured for internal requests, false 
     *                                otherwise.
     * @param numTypes - amount of different request types expected.
     * @param listener - to send the summarized performance information to.
     * @see Monitor
     */
    SimpleMonitor(PerformanceMeasurementListener listener, int numTypes, boolean isForInternalRequests) {
        super(listener, numTypes, isForInternalRequests);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor#record(int, long, double)
     */
    @Override
    public void record(int type, long size, double processingTime) {
                
        // record measurement
        processingTimeMeasurements[type].add(processingTime);
        sizeMeasurements[type].add((double) size);
        
        // summarize samples if necessary
        if(processingTimeMeasurements[type].size() == INITIAL_SAMPLE_AMOUNT) {
            
            publishCollectedData(type, estimateLeastSquares(type));
            
            processingTimeMeasurements[type].clear();
            sizeMeasurements[type].clear();
        }
    }
}