/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.stage.AutonomousComponent;
import org.xtreemfs.common.stage.Callback;

/**
 * <p>Facade for threads that used to have a protection against overload. This class connects all essential methods to 
 * integrate the <b>O</b>ver<b>l</b>oad-<b>P</b>rotection algorithm <b>OLP</b> into an existing application.</p>
 *  
 * @author flangner
 * @version 1.00, 08/31/11
 * @see ProtectionAlgorithmCore
 * @param <R> - global request type.
 */
public abstract class OverloadProtectedComponent<R extends AugmentedRequest> 
        implements AutonomousComponent<OLPStageRequest<R>> {
            
    /**
     * <p>Core of the overload-protection algorithm.</p>
     */
    private final ProtectionAlgorithmCore olp;
    
    /**
     * <p>Counts requests that have approached this component and have not yet been finished.</p>
     */
    private AtomicInteger                 requestCounter = new AtomicInteger(0);  
    
    /**
     * <p>Determines when the last request was finished.</p>
     */
    private long                          lastRequestFinishedTime = -1L;
        
    /**
     * <p>Constructor for initializing the OLP algorithm for a component. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     */
    protected OverloadProtectedComponent(int stageId, int numTypes, int numInternalTypes) {
        
        this (stageId, numTypes, numInternalTypes, 0, new boolean[numTypes]);
    }
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a component. Is is used, if no request-type is 
     * unrefusable.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     */
    protected OverloadProtectedComponent(int stageId, int numTypes, int numInternalTypes, int numSubsequentStages) {
        
        this (stageId, numTypes, numInternalTypes, numSubsequentStages, new boolean[numTypes]);
    }
    
    /**
     * <p>Default constructor for initializing the OLP algorithm for a component.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param unrefusableTypes - array that decides which types of requests are treated unrefusable and which not.
     */
    protected OverloadProtectedComponent(int stageId, int numTypes, int numInternalTypes, int numSubsequentStages, 
            boolean[] unrefusableTypes) {
                
        this.olp = new ProtectionAlgorithmCore(stageId, numTypes, numInternalTypes, numSubsequentStages, 
                unrefusableTypes);
    }
    
    /**
     * <p>Method to initially register receiver of {@link PerformanceInformation} collected by the OLP algorithm.</p>
     * 
     * @param performanceInformationReceiver - receiver of performance information concerning this component.
     */
    public void registerPerformanceInformationReceiver(
            PerformanceInformationReceiver[] performanceInformationReceiver) {
        
        this.olp.addPerformanceInformationReceiver(performanceInformationReceiver);
    }
    
    /**
     * <p>Method to announce a temporary interruption of the processing of the request represented by its monitoring 
     * information. The request will remain at the component until its execution is resumed and finished.</p>
     * 
     * @param request - {@link OLPStageRequest} containing monitoring information for the processing step provided by 
     *                  this {@link AutonomousComponent}.
     * 
     * @see OverloadProtectedComponent#resumeRequestProcessing(RequestMonitoring)
     */
    public final void suspendRequestProcessing(OLPStageRequest<R> request) {
        
        lastRequestFinishedTime = request.endGeneralMeasurement(lastRequestFinishedTime);
    }
    
    /**
     * <p>Method resume a temporarily interrupted request represented by its monitoring information.</p>
     * 
     * @param request - {@link OLPStageRequest} containing monitoring information for the processing step provided by 
     *                  this {@link AutonomousComponent}.
     * 
     * @see OverloadProtectedComponent#suspendRequestProcessing(RequestMonitoring)
     */
    public final void resumeRequestProcessing(OLPStageRequest<R> request) {
        
        request.beginGeneralMeasurement();
    }
    
    /**
     * <p>Method to delegate a request to this protected component.</p>
     * 
     * @param stageMethodId
     * @param args
     * @param request
     * @param callback
     * @param performanceInformationReceiver
     * 
     * @throws AdmissionRefusedException if a request could not approach the component due violation of timeout 
     *                                   restrictions.
     */
    public final void enter(int stageMethodId, Object[] args, R request, Callback callback, 
            PerformanceInformationReceiver[] performanceInformationReceiver) throws AdmissionRefusedException {
        
        final OLPStageRequest<R> rq = new OLPStageRequest<R>(stageMethodId, args, request, callback, 
                performanceInformationReceiver);
        
        olp.obtainAdmission(request);
        resumeRequestProcessing(rq);
        requestCounter.incrementAndGet();
        enter(rq);
    }
    
    /**
     * <p>Method to signalize that a request leaves this component. Measurements are monitored and piggyback information
     * are prepared.</p>
     * 
     * @param request - {@link OLPStageRequest} containing monitoring information for the processing step provided by 
     *                  this {@link AutonomousComponent}.
     */
    @Override
    public final void exit(OLPStageRequest<R> request) {
                
        requestCounter.decrementAndGet();
        suspendRequestProcessing(request);
        olp.depart(request);
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
    public final int getNumberOfRequests() {
        
        return requestCounter.get();
    }
}
