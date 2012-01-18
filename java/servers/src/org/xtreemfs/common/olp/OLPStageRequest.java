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
     * <p>Field to temporary give a request high priority. Will only apply as long as the request does not leave its 
     * stage.</p>
     */
    private boolean                                highPriority       = false;
    
    /**
     * <p>Bandwidth occupied by this request.</p>
     */
    private final long                             size;
    
    /**
     * <p>Measured processing time that is fix for requests of this <code>type</code>.
     * Especially processing time that is variable to request in-/output is not measured by this field.</br>
     * Value is reset by its getter.</p>
     */
    private long                                   processingTime     = 0L;
        
    /**
     * <p>Time stamp in nanoseconds marking the begin of a measurement. Will be reset on end
     * of the measurement.</p>
     */
    private long                                   currentMeasurement = -1L;
        
    /**
     * <p>Flag to determine if currently measured monitoring information are valid or have been voided by a processing
     * cancellation.</p>
     */
    private boolean                                voided             = false;
    
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
        
        assert(currentMeasurement == -1) : "Currently there is a measurement in progress.";
        
        currentMeasurement = System.nanoTime();
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
        
        return endGeneralMeasurement(currentMeasurement);
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
        if (currentMeasurement > -1L) {
            
            if (currentMeasurement > measurementBegin) {
                
                processingTime += time - currentMeasurement;
            } else {
                
                processingTime += time - measurementBegin;
            }
            currentMeasurement = -1L;
        }
        return time;
    }
    
    /**
     * <p>Gathers the collected measurements of processing time that has occurred independent from the request bandwidth
     * size. Measurement history will be reset.</p>
     * 
     * @return the overall fixed processing time in ms, that has been measured since the last call of this method.
     */
    double getProcessingTime() {
        
        assert(currentMeasurement == -1L) : "Currently there is a measurement in progress.";
        
        return ((double) processingTime) / 1000000.0;
    }
    
    /**
     * @return true if measurement results are valid, false if they have been voided during processing of the request.
     */
    boolean hasValidMonitoringInformation() {
        
        return !voided;
    }
    
    /**
     * @return size of this request defined by the bandwidth needed to process it.
     */
    long getSize() {
        
        return size;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.StageRequest#update(int, java.lang.Object[], org.xtreemfs.common.stage.Callback)
     */
    public void update(boolean highPriority, int newMethodId, Object[] newArgs, Callback newCallback) {
        super.update(newMethodId, newArgs, newCallback);
        
        if (highPriority) 
            increasePriority();
        else
            decreasePriority();
    }
    
    /**
     * <p>Increases priority for the next processing step, if possible.</p>
     */
    public void increasePriority() {
        
        highPriority = true;
    }
    
    /**
     * <p>Decreases priority for the next processing step, if possible.</p>
     */
    public void decreasePriority() {
        
        highPriority = false;
    }
    
    /**
     * @return true, if this request has temporary high priority. false otherwise.
     */
    boolean hasHighPriority() {
        
        return highPriority;
    }
    
    /**
     * @return the receiver for performance information that are provided piggyback.
     */
    PerformanceInformationReceiver[] getPiggybackPerformanceInformationReceiver() {
        
        return piggybackPerformanceReceiver;
    }
}