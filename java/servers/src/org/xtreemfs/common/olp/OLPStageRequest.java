/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.StageRequest;

/**
 * <p>Subclass of {@link StageRequest} that contains routines to measure execution time. During a processing step the 
 * processing time is collected additively until it's summarized and send to the OLP monitor.</p>
 * <p>Methods are assumed to be accessed single-threaded.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/29/11
 */
public final class OLPStageRequest<R extends AugmentedRequest> extends StageRequest<R> {

    /**
     * <p>Bandwidth occupied by this request.</p>
     */
    private final long                             size;
    
    /**
     * <p>Measured processing time that is fix for requests of this <code>type</code>.
     * Especially processing time that is variable to request in-/output is not measured by this field.</br>
     * Value is reset by its getter.</p>
     */
    private long                                   generalProcessingTime     = 0L;
    
    /**
     * <p>Measured processing time that scales proportional with size of the request's in/output.
     * Value is reset by its getter.</p>
     */
    private long                                   variableProcessingTime    = 0L;
    
    /**
     * <p>Time stamp in nanoseconds marking the begin of a measurement. Will be reset on end
     * of the measurement.</p>
     */
    private long                                   currentGeneralMeasurement = -1L;
    
    /**
     * <p>Time stamp in nanoseconds marking the begin of a custom measurement. Will be reset on end
     * of the measurement.</p>
     */
    private long                                   currentCustomMeasurement  = -1L;
    
    /**
     * <p>Flag to determine if currently measured monitoring information are valid or have been voided by a processing
     * cancellation.</p>
     */
    private boolean                                voided                    = false;
    
    /**
     * <p>Flag that determines if this stage request is being recycled, or not.
     */
    private boolean                                recycled                  = false;
    
    /**
     * <p>Optional field for registering a receiver for performance information that are send piggyback, after the 
     * request finishes the processing step.</p>
     */
    private final PerformanceInformationReceiver[] piggybackPerformanceReceiver;

    /**
     * <p>Constructor without registering a predecessor for the processing step monitored by this.</p>
     * 
     * @param stageMethodId
     * @param args
     * @param request
     * @param callback
     * @see StageRequest
     */
    OLPStageRequest(int stageMethodId, Object[] args, R request, Callback callback) {
        this(0L, stageMethodId, args, request, callback, new PerformanceInformationReceiver[0]);
    }
    
    /**
     * <p>Constructor without registering a predecessor for the processing step monitored by this.</p>
     * 
     * @param size
     * @param stageMethodId
     * @param args
     * @param request
     * @param callback
     * @see StageRequest
     */
    OLPStageRequest(long size, int stageMethodId, Object[] args, R request, Callback callback) {
        this(size, stageMethodId, args, request, callback, new PerformanceInformationReceiver[0]);
    }
    
    /**
     * <p>Constructor with piggybackPerformanceInformationReceiver as predecessor for the processing step monitored by 
     * this.</p>
     * 
     * @param size
     * @param stageMethodId
     * @param args
     * @param request
     * @param callback
     *
     * @param piggybackPerformanceInformationReceiver - the processing predecessor of the request monitored by this.
     */
    OLPStageRequest(long size, int stageMethodId, Object[] args, R request, Callback callback, 
            PerformanceInformationReceiver[] piggybackPerformanceInformationReceiver) {
        super(stageMethodId, args, request, callback);
        
        assert (piggybackPerformanceInformationReceiver != null);
        
        this.size = size;
        this.piggybackPerformanceReceiver = piggybackPerformanceInformationReceiver;
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
        
        assert(size > 0L) : "The request requires no Bandwidth, that's why no custom measurements are allowed.";
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
   
    /**
     * <p>Method to void measurement results triggered by a request cancellation.</p>
     */
    public void voidMeasurments() {
        
        voided = true;
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
     * <p>Providing an alternative measurement begin enables the request's processing time measurement to be postponed 
     * to the end of the preceding request. This way it is possible to subtract out the waiting time the request had at
     * the queue.</p>
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
        
        final long time = System.nanoTime();
        
        // check, if there is a measurement in progress
        if (currentGeneralMeasurement > -1L) {
            
            if (currentGeneralMeasurement > measurementBegin) {
                
                generalProcessingTime += time - currentGeneralMeasurement;
            } else {
                
                generalProcessingTime += time - measurementBegin;
            }
            currentGeneralMeasurement = -1L;
        }
        return time;
    }
    
    /**
     * <p>Gathers the collected measurements of processing time that has occurred independent from the request bandwidth
     * size. Measurement history will be reset.</p>
     * 
     * @return the overall fixed processing time in ms, that has been measured since the last call of this method.
     */
    double getFixedProcessingTime() {
        
        assert(currentGeneralMeasurement == -1L) : "Currently there is a measurement in progress.";
        
        return ((double) (generalProcessingTime - variableProcessingTime)) / 1000000.0;
    }
    
    /**
     * <p>Gathers the collected measurements of variable processing time. Result will be normalized. 
     * Measurement history will be reset.</p>
     * 
     * @return the variable processing time in ms/byte, that has been measured since the last call of this method.
     */
    
    double getVariableProcessingTime() {
        
        assert(currentCustomMeasurement == -1L) : "Currently there is a measurement in progress.";
      
        double result = 0.0;
        if (size > 0L) {
            
            result = ((double) (variableProcessingTime / size)) / 1000000.0;
        } else {
            
            variableProcessingTime = 0L;
        }
        
        return result;
    }
    
    /**
     * @return true if measurement results are valid, false if they have been voided during processing of the request.
     */
    boolean hasValidMonitoringInformation() {
        
        return !voided;
    }
    
    /**
     * <p>Sets the recycled flag.</p>
     */
    void markRecycle() {
        
        this.recycled = true;
    }
    
    /**
     * <p>Will reset the recycled flag.</p>
     * 
     * @return true if stage request is currently recycled.
     */
    boolean isRecycled() {
        
        final boolean r = recycled;
        this.recycled = false;
        return r;
    }
    
    /**
     * @return size of this request defined by the bandwidth needed to process it.
     */
    long getSize() {
        
        return size;
    }

    /**
     * @return the receiver for performance information that are provided piggyback.
     */
    PerformanceInformationReceiver[] getPiggybackPerformanceInformationReceiver() {
        
        return piggybackPerformanceReceiver;
    }
}