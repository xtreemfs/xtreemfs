/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.List;

/**
 * <p>Interface for monitors that record request performance information. Implementations may differ in the amount of 
 * samples recorded and the metric used to summarize these samples.</p>
 * 
 * @author flangner
 * @version 1.00, 08/18/11
 */
abstract class Monitor {
    
    /**
     * <p>Listener to notify about new summaries of collected samples.</p>
     */
    protected final PerformanceMeasurementListener listener;
    
    /**
     * <p>Flag that determines whether the performance averages are monitored for internal requests, or not.</p>
     */
    protected final boolean                        isForInternalRequests;
    
    /**
     * <p>Abstract constructor ensuring the essential connection with the receiver of the measured and summarized 
     * performance information.</p>
     * 
     * @param listener - to send the summarized performance information to.
     * @param isForInternalRequests - true if the performance averages are measured for internal requests, false 
     *                                otherwise.
     */
    Monitor(PerformanceMeasurementListener listener, boolean isForInternalRequests) {
        
        this.listener = listener;
        this.isForInternalRequests = isForInternalRequests;
    }
    
    /**
     * <p>Records monitoring information of the request of the given type.</p>
     * 
     * @param type - processing-time-class for this request.
     * @param fixedProcessingTime - the processing of the request required in ms.
     * @param variableProcessingTime - the duration of the processing normalized to a bandwidth of one in ms/byte.
     */
    abstract void record(int type, double fixedProcessingTime, double variableProcessingTime);
    
    /**
     * <p>Method implementing an algorithm to sum up measured samples.</p>
     * 
     * @param measurements - measurements to summarize all in ms or ms/byte.
     * @return a representative summarization of the given measurements in ms or ms/byte.
     */
    abstract double summarizeMeasurements(double[] measurements); 
    
    /**
     * <p>Method implementing an algorithm to sum up measured samples.</p>
     * 
     * @param measurements - measurements to summarize all in ms or ms/byte.
     * @return a representative summarization of the given measurements in ms or ms/byte.
     */
    abstract double summarizeMeasurements(List<Double> measurements); 
    
    /**
     * <p>Receiver of summarized performance information measured by the {@link Monitor}.</p> 
     * 
     * @author flangner
     * @version 1.00, 09/02/11
     */
    interface PerformanceMeasurementListener {
        
        /**
         * <p>Updates the currently valid performance estimations made by the monitor for the fixed time effort.</p>
         * 
         * @param type - request type.
         * @param value - the summarized performance estimation in ms.
         * @param internal - true if the performance average was measured for internal requests, false otherwise.
         */
        void updateFixedProcessingTimeAverage(int type, double value, boolean internal);
        
        /**
         * <p>Updates the currently valid performance estimations made by the monitor for the variable time effort.</p>
         * 
         * @param type - request type.
         * @param value - the summarized performance estimation in ms/bytes.
         * @param internal - true if the performance average was measured for internal requests, false otherwise.
         */
        void updateVariableProcessingTimeAverage(int type, double value, boolean internal);
    }
}