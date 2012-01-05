/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Interface for monitors that record request performance information. Implementations may differ in the amount of 
 * samples recorded and the metric used to summarize these samples.</p>
 * 
 * @author flangner
 * @version 1.00, 08/18/11
 */
abstract class Monitor {
    
    /**
     * <p>Number of samples to collect there averages from.</p>
     */
    private final static int                     INITIAL_SAMPLE_AMOUNT = 10;
    
    /**
     * <p>Listener to notify about new summaries of collected samples.</p>
     */
    private final PerformanceMeasurementListener listener;
    
    /**
     * <p>Flag that determines whether the performance averages are monitored for internal requests, or not.</p>
     */
    private final boolean                        isForInternalRequests;
    
    /**
     * <p>Contains samples of fixed time measurements in ms of different types of requests.</p>
     */
    protected final RingBuffer[]                 processingTimeMeasurements;
    
    /**
     * <p>Contains samples of variable time measurements in ms/byte of different types of requests.</p>
     */
    protected final RingBuffer[]                 sizeMeasurements;
    
    /**
     * <p>Previously calculated fixed processing times for different types of requests.</p>
     */
    protected final double[]                     historicalFixedProcessingTimes; 
    
    /**
     * <p>Previously calculated fixed processing times for different types of requests.</p>
     */
    protected final double[]                     historicalVariableProcessingTimes; 
    
    /**
     * <p>Abstract constructor ensuring the essential connection with the receiver of the measured and summarized 
     * performance information.</p>
     * 
     * @param listener - to send the summarized performance information to.
     * @param isForInternalRequests - true if the performance averages are measured for internal requests, false 
     *                                otherwise.
     * @param numTypes - amount of different request types expected.
     */
    Monitor(PerformanceMeasurementListener listener, int numTypes, boolean isForInternalRequests) {
        
        this.listener = listener;
        this.isForInternalRequests = isForInternalRequests;
        
        processingTimeMeasurements = new RingBuffer[numTypes];
        sizeMeasurements = new RingBuffer[numTypes];
        
        // historical data
        historicalFixedProcessingTimes = new double[numTypes];
        historicalVariableProcessingTimes = new double[numTypes];
        
        for (int i = 0; i < numTypes; i++) {
            processingTimeMeasurements[i] = new RingBuffer(INITIAL_SAMPLE_AMOUNT);
            sizeMeasurements[i] = new RingBuffer(INITIAL_SAMPLE_AMOUNT);
        }
    }
    
    /**
     * <p>Records monitoring information of the request of the given type.</p>
     * 
     * @param type - processing-time-class for this request.
     * @param size - usually the size of the request data in bytes, which is used to determine coefficients of a linear 
     *               equation using linear regression.
     * @param processingTime - the processing of the request required in ms.
     */
    abstract void record(int type, long size, double processingTime);
    
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
    
    /**
     * <p>Method to send summarized collected data to the listener.</p>
     * 
     * @param type
     * @param data
     */
    protected final void publishCollectedData(int type, double[] data) {
        
        // save historical data
        historicalFixedProcessingTimes[type] = data[0];
        historicalVariableProcessingTimes[type] = data[1];
        
        listener.updateFixedProcessingTimeAverage(type, data[0], isForInternalRequests);
        listener.updateVariableProcessingTimeAverage(type, data[1], isForInternalRequests);
    }
    
/*
 * Methods to calculate the least square estimations for the recorded samples.
 */

    /**
     * <p>Estimates the coefficients a1 and a0 of the linear equation t(s)= a1 * s + a0 for samples of the given type 
     * and passes them to the listener.<p>
     * 
     * @param type
     * 
     * @return a tuple of fixed processing time (fst) and variable processing time (snd).
     */
    protected final double[] estimateLeastSquares(int type) {
        
        return estimateLeastSquares(type, processingTimeMeasurements[type].avg(), sizeMeasurements[type].avg());
    }
    
    /**
     * <p>Estimates the coefficients a1 and a0 of the linear equation t(s)= a1 * s + a0 for samples of the given type 
     * and passes them to the listener.<p>
     * 
     * @param type
     * @param avgT
     * @param avgS
     * 
     * @return a tuple of fixed processing time (fst) and variable processing time (snd).
     */
    protected final double[] estimateLeastSquares(int type, double avgT, double avgS) {
        
        final int length = processingTimeMeasurements[type].size();
        
        assert (sizeMeasurements[type].size() == length);
                
        // simplification to avoid costively measurements whether there is no size and therefore a plain simple linear
        // approximation
        if (avgS > 0.0) {
            
            double varSS = 0.0;
            double varTS = 0.0;
            double varS  = 0.0;
            for (int i = 0; i < length; i++) {
                
                varS = sizeMeasurements[type].get(i) - avgS;
                varTS += (processingTimeMeasurements[type].get(i) - avgT) * varS;
                varSS += Math.pow(varS, 2.0);
            }
            
            // in case we measured only requests of the same size
            final double fixed;
            final double variable;
            if (varSS == 0.0) {
                
                fixed = historicalFixedProcessingTimes[type];
                variable = (avgT - fixed) / avgS;
            } else {
             
                variable = varTS / varSS;
                fixed = avgT - (variable * avgS);
            }
            
            //Logging.logMessage(Logging.LEVEL_DEBUG, this, "Least-squares-estimation: %s leads to est. fixed " +
            //        "processing %.2f ms and variable processing %.2f ms/bytes", debug, fixed, variable);
            
            return new double[] { (fixed > 0.0) ? fixed : 0.0, (variable > 0.0) ? variable : 0.0 };
        } else {
            
            //Logging.logMessage(Logging.LEVEL_DEBUG, this, "Least-squares-estimation: %s", debug);
            
            return new double[] { avgT, historicalVariableProcessingTimes[type] };
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        
        StringBuilder builder = new StringBuilder();
        
        builder.append("Monitor capacity:\n" +
        	       "type\tprocessingTime\tsize\n");
        for (int i = 0; i < processingTimeMeasurements.length; i++) {
            builder.append(i + "\t" + processingTimeMeasurements[i].toString() + "\t" + sizeMeasurements[i].toString() +
                    "\n");
        }
        builder.append("\n");
        return builder.toString();
    }
}