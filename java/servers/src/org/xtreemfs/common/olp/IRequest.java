/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.olp.Actuator.RequestExpiredException;

/**
 * <p>Superclass for instrumented requests that provide monitoring information for the 
 * <b>O</b>ver<b>l</b>oad-<b>P</b>rotection algorithm. During processing of this request at one stage, the processing 
 * time is collected additive until it is collected by the OLP monitor. Fixed and variable parts of the processing 
 * time are measured independently.</p> 
 * 
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to be single threaded.</p>
 * 
 * @since 08/18/2011
 * @author flangner
 * @version 1.0
 */
public abstract class IRequest {
    
    /**
     * <p>Measured processing time that is fix for requests of this <code>type</code>.
     * Especially processing time that is variable to request in-/output is not measured by this field.</br>
     * Value is reset by its getter.</p>
     */
    private long        fixedProcessingTime    = 0L;
    
    /**
     * <p>Measured processing time that scales proportional with size of the request's in/output.
     * Value is reset by its getter.</p>
     */
    private long        variableProcessingTime = 0L;
    
    /**
     * <p>Time stamp in nanoseconds marking the begin of a measurement. Will be reset on end
     * of the measurement.</p>
     */
    private long        currentMeasurement     = -1L;
    
    /**
     * <p>Identifier for requests of this type.</p>
     */
    private final int   type;
    
    /**
     * <p>Bandwidth occupied by this request.</p>
     */
    private final long  size;
    
    /**
     * <p>Maximal response time delta for this request.</p>
     */
    private final long  deltaMaxTime;
    
    /**
     * <p>The Unix time stamp this request was initially received.</p>
     */
    private final long  startTime;

    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     */
    public IRequest(int type, long deltaMaxTime) {
        this(type, 0L, deltaMaxTime);
    }
    
    /**
     * <p>Constructor for requests that will require a specific amount of bandwidth during processing, that is 
     * proportional to the processing time.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param size - amount of bandwidth occupied during processing of this request.
     * @param deltaMaxTime - the time to live for this request.
     */
    public IRequest(int type, long size, long deltaMaxTime) {
        assert (deltaMaxTime > 0);
        assert (type > -1);
        
        this.type = type;
        this.size = size;
        this.deltaMaxTime = deltaMaxTime;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * <p>Method to begin the measurement of processing time for this request.</p>
     * 
     * <b>Usage:</b></br>
     * process(IRequest request) {</br>
     * &nbsp;request.beginMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[processing]</br>
     * &nbsp;...</br>
     * &nbsp;request.endFixedTimeMeasurement();</br>
     * &nbsp;request.beginMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[bandwidth proportional processing]</br>
     * &nbsp;...</br>
     * &nbsp;request.endVariableTimeMeasurement();</br>
     * }</br>
     */
    public void beginMeasurement() {
        assert(currentMeasurement == -1) : "Currently there is a measurement in progress.";
        
        currentMeasurement = System.nanoTime();
    }
    
    /**
     * <p>Method to end the measurement of processing time for this request.</p>
     * 
     * <b>Usage:</b></br>
     * process(IRequest request) {</br>
     * &nbsp;request.beginMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[processing]</br>
     * &nbsp;...</br>
     * &nbsp;request.endFixedTimeMeasurement();</br>
     * &nbsp;request.beginMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[bandwidth proportional processing]</br>
     * &nbsp;...</br>
     * &nbsp;request.endVariableTimeMeasurement();</br>
     * }</br>
     */
    public void endFixedTimeMeasurement() {
        assert(currentMeasurement != -1) : "Currently there is no measurement in progress.";
        
        fixedProcessingTime += System.nanoTime() - currentMeasurement;
        currentMeasurement = -1L;
    }
    
    /**
     * <p>Method to end the measurement of processing time for this request. For this measurement a processing time 
     * proportional to the bandwidth occupied by this request is assumed.</p>
     * 
     * <b>Usage:</b></br>
     * process(IRequest request) {</br>
     * &nbsp;request.beginMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[processing]</br>
     * &nbsp;...</br>
     * &nbsp;request.endFixedTimeMeasurement();</br>
     * &nbsp;request.beginMeasurement();</br>
     * &nbsp;...</br>
     * &nbsp;[bandwidth proportional processing]</br>
     * &nbsp;...</br>
     * &nbsp;request.endVariableTimeMeasurement();</br>
     * }</br>
     */
    public void endVariableTimeMeasurement() {
        assert(currentMeasurement != -1) : "Currently there is no measurement in progress.";
        
        variableProcessingTime += System.nanoTime() - currentMeasurement;
        currentMeasurement = -1L;
    }
    
/*
 * package internal methods
 */
    
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
        assert(currentMeasurement == -1) : "Currently there is a measurement in progress.";
        
        double result = (double) fixedProcessingTime / 1000000.0;
        fixedProcessingTime = 0L;
        return result;
    }
    
    /**
     * <p>Gathers the collected measurements of variable processing time. Result will be normalized. 
     * Measurement history will be reset.</p>
     * 
     * @return the variable processing time in ms/byte, that has been measured since the last call of this method.
     */
    double getVariableProcessingTime() {
        assert(currentMeasurement == -1) : "Currently there is a measurement in progress.";
        assert(size > 0L) : "For proportional processing time measurement size has to be greater 0.";
        
        double result = ((double) (variableProcessingTime / size)) / 1000000.0;
        variableProcessingTime = 0L;
        return result;
    }
}