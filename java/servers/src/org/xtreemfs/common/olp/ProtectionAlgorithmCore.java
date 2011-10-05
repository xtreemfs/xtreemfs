/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.AutonomousComponent.AdmissionRefusedException;

/**
 * <p>The algorithm core controlling the interactions between the different parts of the overload-protection algorithm.
 * </p>
 * 
 * @author fx.langner
 * @version 1.00, 09/15/2011
 */
final class ProtectionAlgorithmCore {
    
    /**
     * <p>Identifier that is unique among parallel stages that follow the same predecessor, this stage belongs to.</p>
     */
    final int                                   id;
    
    /**
     * <p>Array that indicates whether a request is unrefusable or not.</p>
     */
    private final boolean[]                     unrefusableTypes;
    
    /**
     * <p>Component that throttles the influx of requests by providing admission control.</p>
     */
    private final Actuator                      actuator;
    
    /**
     * <p>Component to interpret measurements and combine them with {@link PerformanceInformation} 
     * from subsequent stages.</p>
     */
    private final Controller                    controller;
    
    /**
     * <p>Component to measure request processing performance of this stage.</p>
     */
    private final Monitor                       monitor;
    
    /**
     * <p>Component to measure request processing performance of internal requests of this stage. Internal request 
     * performance information is not shared among connected stages.</p>
     */
    private final Monitor                       internalRequestMonitor;
    
    /**
     * <p>Component that will continuously send {@link PerformanceInformation} to preceding stages.</p>
     */
    private final PerformanceInformationSender  sender;
    
    /**
     * <p>Default constructor for initializing the OLP algorithm.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param unrefusableTypes - array that decides which types of requests are treated unrefusable and which not.
     */
    ProtectionAlgorithmCore(int stageId, int numTypes, int numInternalTypes, int numSubsequentStages, 
            boolean[] unrefusableTypes) {
        
        assert (unrefusableTypes.length == numTypes);
        
        this.unrefusableTypes = unrefusableTypes;
        this.id = stageId;
        this.actuator = new Actuator();
        this.controller = new Controller(numTypes, numInternalTypes, numSubsequentStages);
        this.monitor = new SimpleMonitor(false, numTypes, controller);
        this.internalRequestMonitor = new SimpleMonitor(true, numInternalTypes, controller);
        this.sender = new PerformanceInformationSender(this);
    }
    
    /**
     * <p>Method to add receiver of {@link PerformanceInformation} collected by the OLP algorithm.</p>
     * 
     * @param performanceInformationReceiver - receiver of performance information concerning this component.
     */
    void addPerformanceInformationReceiver(PerformanceInformationReceiver[] performanceInformationReceiver) {
        
        assert (performanceInformationReceiver != null);
        
        for (PerformanceInformationReceiver receiver : performanceInformationReceiver) {
            sender.addReceiver(receiver);
        }
    }
    
    /**
     * <p>Method to stop daemons connected with the overload protection algorithm.</p>
     */
    void shutdown() {
        
        sender.cancel();
    }
    
    /**
     * <p>Checks whether a request can be processed before its time is running out or not.</p>
     * 
     * @param request - the request to check.
     * @throws AdmissionRefusedException if requests has already expired or will expire before processing has finished.
     */
    <R extends AugmentedRequest> void hasAdmission(R request) throws AdmissionRefusedException {
        
        final int type = request.getType();
        if (!request.isInternalRequest() && !unrefusableTypes[type] && 
            !actuator.hasAdmission(request.getRemainingProcessingTime(), 
             controller.estimateResponseTime(type, request.getSize(), request.hasHighPriority()))) {
                
            throw new AdmissionRefusedException();
        }
    }
    
    /**
     * <p>Method to be executed to determine whether the request represented by its metadata may be processed or 
     * not.</p>
     * 
     * @param request - the request to process.
     * @throws AdmissionRefusedException if requests has already expired or will expire before processing has finished.
     */
    <R extends AugmentedRequest> void obtainAdmission(R request) throws AdmissionRefusedException {
                
        hasAdmission(request);
        controller.enterRequest(request.getType(), request.getSize(), request.hasHighPriority(), 
                request.isInternalRequest());
    }
    
    /**
     * <p>If the processing of the given request has finished, its departure has to be notified to the algorithm by this
     * method.</p>
     * 
     * @param stageRequest - {@link OLPStageRequest} containing monitoring information for one processing step.
     */
    <R extends AugmentedRequest> void depart(OLPStageRequest<R> stageRequest) {
        
        final AugmentedRequest request = stageRequest.getRequest();
        controller.quitRequest(request.getType(), request.getSize(), request.hasHighPriority(), 
                request.isInternalRequest());
        
        if (stageRequest.hasValidMonitoringInformation()) {
            
            final double varProcessingTime = stageRequest.getVariableProcessingTime(request.getSize());
            final double fixProcessingTime = stageRequest.getFixedProcessingTime();
            
            if (request.isInternalRequest()) {

                internalRequestMonitor.record(request.getType(), fixProcessingTime, varProcessingTime);                
            } else {
                
                monitor.record(request.getType(), fixProcessingTime, varProcessingTime);
            }
        }
        final PerformanceInformationReceiver[] receiver = stageRequest.getPiggybackPerformanceInformationReceiver();
        for (PerformanceInformationReceiver r : receiver) {
            
            r.processPerformanceInformation(composePerformanceInformation());
            sender.performanceInformationUpdatedPiggyback(r);
        }
    }
        
    /**
     * <p>Method to pass aggregated performance information about successive stages to the OLP of this stage.</p>
     * 
     * @param performanceInformation - information received by another stage.
     */
    void addPerformanceInformation(PerformanceInformation performanceInformation) {
        
        controller.updateSuccessorInformation(performanceInformation);
    }
    
    /**
     * <p>Method to retrieve aggregate performance information about this and successive stages.</p>
     * 
     * @return performance information for the protected stage.
     */
    PerformanceInformation composePerformanceInformation() {
        
        return controller.composePerformanceInformation(id);
    }
    
/*
 * Exceptions
 */
    
    /**
     * <p>Exception that is thrown if admission could not have been granted to a request entering the application,
     * because the request has already been expired.</p>
     * 
     * @author flangner
     * @version 1.00, 08/31/11
     */
    public final static class RequestExpiredException extends AdmissionRefusedException {
        
        private static final long serialVersionUID = 6042472641208133509L;       
    }
}
