/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.olp.ProtectionAlgorithmCore.RequestExpiredException;

/**
 * <p>Contains metadata for a request that will be recognized by the overload-protection algorithm. Beside configuration 
 * parameter it also contains routines to measure execution time and enable piggybacking for preceding components.
 * During a processing step the processing time is collected additively until it's summarized and send to the OLP 
 * monitor.</p>
 * 
 * <p>Has to be reset after each processing step executed through a stage or component.</p>
 * 
 * <p>This is assumed to be accessed single-threaded.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/14/2011
 */
public class RequestMetadata {
    
    /**
     * <p>Measured processing time that is fix for requests of this <code>type</code>.
     * Especially processing time that is variable to request in-/output is not measured by this field.</br>
     * Value is reset by its getter.</p>
     */
    private long                                generalProcessingTime     = 0L;
    
    /**
     * <p>Measured processing time that scales proportional with size of the request's in/output.
     * Value is reset by its getter.</p>
     */
    private long                                variableProcessingTime    = 0L;
    
    /**
     * <p>Time stamp in nanoseconds marking the begin of a measurement. Will be reset on end
     * of the measurement.</p>
     */
    private long                                currentGeneralMeasurement = -1L;
    
    /**
     * <p>Time stamp in nanoseconds marking the begin of a custom measurement. Will be reset on end
     * of the measurement.</p>
     */
    private long                                currentCustomMeasurement  = -1L;
    
    /**
     * <p>Identifier for requests of this type.</p>
     */
    private final int                           type;
    
    /**
     * <p>Bandwidth occupied by this request.</p>
     */
    private final long                          size;
    
    /**
     * <p>Maximal response time delta for this request.</p>
     */
    private final long                          deltaMaxTime;
    
    /**
     * <p>The Unix time stamp this request was initially received.</p>
     */
    private final long                          startTime;
    
    /**
     * <p>Flag to determine whether this request has high priority or not.</p>
     */
    private final boolean                       highPriority;
    
    /**
     * <p>Optional field for registering a receiver for performance information that are send piggyback, after the 
     * request finishes the processing step.</p>
     */
    private PerformanceInformationReceiver      piggybackPerformanceReceiver;
    
    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed and do not have high priority.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     */
    public RequestMetadata(int type, long deltaMaxTime) {
        
        this(type, deltaMaxTime, false);
    }
    
    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     */
    public RequestMetadata(int type, long deltaMaxTime, boolean highPriority) {
        
        this(type, 0L, deltaMaxTime, highPriority, null);
    }
    
    /**
     * <p>Constructor for requests that will require a specific amount of bandwidth during processing, that is 
     * proportional to the processing time.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param size - amount of bandwidth occupied during processing of this request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     * @param piggybackPerformanceReceiver - receiver of performance information send piggyback.
     */
    public RequestMetadata(int type, long size, long deltaMaxTime, boolean highPriority, 
            PerformanceInformationReceiver piggybackPerformanceReceiver) {
                
        assert (deltaMaxTime > 0);
        assert (type > -1);
        
        this.type = type;
        this.size = size;
        this.deltaMaxTime = deltaMaxTime;
        this.startTime = System.currentTimeMillis();
        this.highPriority = highPriority;
    }
    
    /**
     * <p>Method to begin the measurement of processing time for this request.</p>
     * 
     * <b>Usage:</b></br>
     * process(RequestMetadata metadata, ...) {</br>
     * &nbsp;metadata.beginMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[bandwidth proportional processing]</br>
     * &nbsp;...</br>
     * &nbsp;metadata.endMeasurement();</br>
     * }</br>
     */
    public void beginMeasurement() {
        
        assert(currentCustomMeasurement == -1) : "Currently there is a measurement in progress.";
        
        currentCustomMeasurement = System.nanoTime();
    }
    
    /**
     * <p>Method to end the measurement of processing time for this request. For this measurement a processing time 
     * proportional to the bandwidth occupied by this request is assumed.</p>
     * 
     * <b>Usage:</b></br>
     * process(RequestMetadata metadata, ...) {</br>
     * &nbsp;metadata.beginMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[bandwidth proportional processing]</br>
     * &nbsp;...</br>
     * &nbsp;metadata.endMeasurement();</br>
     * }</br>
     */
    public void endMeasurement() {
        
        assert(currentCustomMeasurement > -1) : "Currently there is no measurement in progress.";
        
        variableProcessingTime += System.nanoTime() - currentCustomMeasurement;
        currentCustomMeasurement = -1L;
    }
    
/*
 * package internal methods
 */
    
    /**
     * <p>Method to begin the measurement of processing time for this request.</p>
     * 
     * <b>Usage:</b></br>
     * process(RequestMetadata metadata) {</br>
     * &nbsp;metadata.beginGeneralMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[processing]</br>
     * &nbsp;...</br>
     * &nbsp;metadata.endGeneralMeasurement();</br>
     * }</br>
     */
    void beginGeneralMeasurement() {
        
        assert(currentGeneralMeasurement == -1) : "Currently there is a measurement in progress.";
        
        currentGeneralMeasurement = System.nanoTime();
    }
    
    /**
     * <p>Method to end the measurement of processing time for this request.</p>
     * 
     * <b>Usage:</b></br>
     * process(RequestMetadata metadata) {</br>
     * &nbsp;metadata.beginGeneralMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[processing]</br>
     * &nbsp;...</br>
     * &nbsp;metadata.endGeneralMeasurement();</br>
     * }</br>
     * 
     * @return current timestamp in nanoseconds.
     */
    long endGeneralMeasurement() {
        
        return endGeneralMeasurement(currentGeneralMeasurement);
    }
    
    /**
     * <p>Method to end the measurement of processing time for this request. If the given measurementBegin is later
     * than the timestamp registered by the beginGeneralMeasurement() method, it replaces that timestamp.</p>
     * 
     * <b>Usage:</b></br>
     * process(RequestMetadata metadata) {</br>
     * &nbsp;metadata.beginGeneralMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[processing]</br>
     * &nbsp;...</br>
     * &nbsp;metadata.endGeneralMeasurement();</br>
     * }</br>
     * 
     * @param measurementBegin - the measurement begin in nanoseconds.
     * 
     * @return current timestamp in nanoseconds.
     */
    long endGeneralMeasurement(long measurementBegin) {
        
        assert(measurementBegin > -1) : "Currently there is no measurement in progress.";
        
        long time = System.nanoTime();
        if (currentGeneralMeasurement > measurementBegin) {
            generalProcessingTime = time - currentGeneralMeasurement;
        } else {
            generalProcessingTime = time - measurementBegin;
        }
        return time;
    }
    
    /**
     * @return the identifier of the type of this request.
     */
    int getType() {
        
        return type;
    }
    
    /**
     * @return size of this request defined by the bandwidth needed to process it.
     */
    long getSize() {
        
        return size;
    }
    
    /**
     * @return true if this request has high priority, false otherwise.
     */
    boolean hasHighPriority() {
        
        return highPriority;
    }
        
    /**
     * <p>Calculates the remaining processing time for this request. If the request has already been expired an 
     * Exception is thrown.</p>
     * 
     * @return the remaining processing time for this request in ms.
     * @throws RequestExpiredException if the processing time for this request has already been exhausted.
     */
    double getRemainingProcessingTime() throws RequestExpiredException {
        
        long remaining = (deltaMaxTime + startTime) - System.currentTimeMillis();
        if (remaining < 0) {
            throw new RequestExpiredException();
        }
        return remaining;
    }
    
    /**
     * <p>Gathers the collected measurements of processing time that has occurred independent from the request bandwidth
     * size. Measurement history will be reset.</p>
     * 
     * @return the overall fixed processing time in ms, that has been measured since the last call of this method.
     */
    double getFixedProcessingTime() {
        
        assert(currentGeneralMeasurement == -1) : "Currently there is a measurement in progress.";
        
        double result = (double) (generalProcessingTime - variableProcessingTime) / 1000000.0;
        return result;
    }
    
    /**
     * <p>Gathers the collected measurements of variable processing time. Result will be normalized. 
     * Measurement history will be reset.</p>
     * 
     * @return the variable processing time in ms/byte, that has been measured since the last call of this method.
     */
    double getVariableProcessingTime() {
        
        assert(currentGeneralMeasurement == -1) : "Currently there is a measurement in progress.";
      
        double result = 0.0;
        if (size > 0L) {
            result =((double) (variableProcessingTime / size)) / 1000000.0;
        } else {
            assert(variableProcessingTime == 0L) : 
                "For proportional processing time measurement size has to be greater 0.";
        }
        
        return result;
    }
    
    /**
     * @return the receiver for performance information that are provided piggyback.
     */
    PerformanceInformationReceiver getPiggybackPerformanceInformationReceiver() {
        
        return piggybackPerformanceReceiver;
    }

    /**
     * <p>Resets monitoring information and registers a new piggyback performance information receiver as preparation 
     * for the next processing step.</p>
     * 
     * @param receiver - the receiver for performance information that are provided piggyback.
     */
    void reset(PerformanceInformationReceiver receiver) {
        
        piggybackPerformanceReceiver = receiver;
        currentGeneralMeasurement    = -1L;
        currentCustomMeasurement     = -1L;
        generalProcessingTime        = 0L;
        variableProcessingTime       = 0L;
    }
}
