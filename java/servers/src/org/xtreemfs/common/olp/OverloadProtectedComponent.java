/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.olp.ProtectionAlgorithmCore.AdmissionRefusedException;
import org.xtreemfs.common.stage.AutonomousComponent;

/**
 * <p>Facade for threads that used to have a protection against overload. This class connects all essential methods to 
 * integrate the <b>O</b>ver<b>l</b>oad-<b>P</b>rotection algorithm <b>OLP</b> into an existing application.</p>
 *  
 * @author flangner
 * @version 1.00, 08/31/11
 * @see ProtectionAlgorithmCore
 * @param <R> - request type.
 */
public abstract class OverloadProtectedComponent<R> implements AutonomousComponent<R> {
            
    /**
     * <p>Core of the overload-protection algorithm.</p>
     */
    private final ProtectionAlgorithmCore       olp;
    
    /**
     * <p>Counts requests that have approached this component and have not yet been finished.</p>
     */
    private AtomicInteger                       requestCounter = new AtomicInteger(0);  
    
    /**
     * <p>Determines when the last request was finished.</p>
     */
    private long                                lastRequestFinishedTime = -1L;
        
    /**
     * <p>Constructor for initializing the OLP algorithm for a component. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     */
    OverloadProtectedComponent(int stageId, int numTypes) {
        
        this (stageId, numTypes, 0, new boolean[numTypes], new PerformanceInformationReceiver[]{});
    }
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a component. Is is used, if no request-type is 
     * unrefusable.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     */
    OverloadProtectedComponent(int stageId, int numTypes, int numSubsequentStages) {
        
        this (stageId, numTypes, numSubsequentStages, new boolean[numTypes], new PerformanceInformationReceiver[]{});
    }
    
    /**
     * <p>Default constructor for initializing the OLP algorithm for a component.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param unrefusableTypes - array that decides which types of requests are treated unrefusable and which not.
     * @param performanceInformationReceiver - receiver of performance information concerning this component.
     */
    OverloadProtectedComponent(int stageId, int numTypes, int numSubsequentStages, boolean[] unrefusableTypes, 
            PerformanceInformationReceiver[] performanceInformationReceiver) {
                
        this.olp = new ProtectionAlgorithmCore(stageId, numTypes, numSubsequentStages, unrefusableTypes, 
                performanceInformationReceiver);
    }
    
    /**
     * <p>Method to delegate a request to this protected component.</p>
     * 
     * @param request - the original request.
     * @param metadata - its statistical and configuring metadata.
     * @throws AdmissionRefusedException if a request could not approach the component due violation of timeout 
     *                                   restrictions.
     */
    public void enter(R request, RequestMetadata metadata) throws AdmissionRefusedException {
        
        olp.hasAdmission(metadata);
        enter(request);
        metadata.beginGeneralMeasurement();
        requestCounter.incrementAndGet();
    }
    
    /**
     * <p>Method to signalize that a request leaves this component. Measurements are monitored and piggyback information
     * are prepared.</p>
     * 
     * @param request - the original request.
     * @param metadata - its statistical and configuring metadata.
     */
    public void exit(R request, RequestMetadata metadata) {
                
        requestCounter.decrementAndGet();
        lastRequestFinishedTime = metadata.endGeneralMeasurement(lastRequestFinishedTime);
        olp.finishRequest(metadata);
        exit(request);
    }
    
    /**
     * <p>Method to shut down this component and end the overload-protection algorithm.</p>
     */
    public void shutdown() {
        
        olp.shutdown();
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#getNumberOfRequests()
     */
    @Override
    public int getNumberOfRequests() {
        
        return requestCounter.get();
    }
}
