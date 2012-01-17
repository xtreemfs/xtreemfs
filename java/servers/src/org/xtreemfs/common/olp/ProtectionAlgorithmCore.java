/*
 * Copyright (c) 2012 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.AutonomousComponent.AdmissionRefusedException;
import org.xtreemfs.foundation.logging.Logging;

/**
 * <p>The algorithm core controlling the interactions between the different parts of the overload-protection algorithm.
 * </p>
 * 
 * @author fx.langner
 * @version 1.01, 09/15/2011
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
        this.monitor = new AdvancedMonitor(controller, numTypes, false);
        this.internalRequestMonitor = new AdvancedMonitor(controller, numInternalTypes, true);
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
        Logging.logMessage(Logging.LEVEL_INFO, this, "Final state of the Overload-Protection Core:\n %s", toString());
    }
    
    /**
     * <p>Checks whether a request can be processed before its time is running out or not.</p>
     * 
     * @param size
     * @param request - the request to check.
     * @throws AdmissionRefusedException if requests has already expired or will expire before processing has finished.
     */
    <R extends AugmentedRequest> void hasAdmission(R request, long size) throws AdmissionRefusedException {
        
        final int type = request.getType();
        if (!request.isNativeInternalRequest() && !request.isUnrefusable() && !unrefusableTypes[type]) {
            
            final boolean hasPriority = request.hasHighPriority();
            final double remainingProcessingTime = request.getRemainingProcessingTime();
            final double estProcessingTime = controller.estimateProcessingTime(type, size);
            final double estWaitingTime = controller.estimateWaitingTime(request.hasHighPriority());
            final double estTime = estProcessingTime + estWaitingTime;
            
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Estimated response time for type (%d) @size %d with %s " +
                    "priority: %.4f processing + %.4f waiting = %.4f", type, size, 
                    String.valueOf(hasPriority), estProcessingTime, estWaitingTime, estTime);
            
            if(!actuator.hasAdmission(remainingProcessingTime, estTime)) {
                
                throw new AdmissionRefusedException(request, remainingProcessingTime, estTime);
            
            // update request meta-data
            } else {
                
                request.setEstimatedRemainingProcessingTime(estProcessingTime);
                request.setSlackTime(remainingProcessingTime - estTime);
            }
        }
    }
    
    /**
     * <p>Method to be executed to determine whether the request represented by its metadata may be processed or 
     * not.</p>
     * 
     * @param type
     * @param size
     * @param isInternal
     */
    <R extends AugmentedRequest> void obtainAdmission(int type, long size, boolean isInternal)  {
        
        obtainAdmission(type, size, false, isInternal);
    }
    
    /**
     * <p>Method to be executed to determine whether the request represented by its metadata may be processed or 
     * not.</p>
     * 
     * @param type
     * @param size
     * @param hasPriority
     * @param isInternal
     */
    <R extends AugmentedRequest> void obtainAdmission(int type, long size, boolean hasPriority, boolean isInternal) {
        
        controller.enterRequest(type, size, hasPriority, isInternal);
    }
    
    
    
    /**
     * <p>If the processing of the given request has finished, its departure has to be notified to the algorithm by this
     * method.</p>
     * 
     * @param stageRequest - {@link OLPStageRequest} containing monitoring information for one processing step.
     */
    <R extends AugmentedRequest> void depart(OLPStageRequest<R> stageRequest) {
        
        depart(stageRequest, false);
    }
    
    /**
     * <p>If the processing of the given request has finished, its departure has to be notified to the algorithm by this
     * method.</p>
     * 
     * @param stageRequest - {@link OLPStageRequest} containing monitoring information for one processing step.
     * @param recycle - if true no monitoring data shall be collected and performance information receiver are not
     *                  provided any information. false otherwise.
     */
    <R extends AugmentedRequest> void depart(OLPStageRequest<R> stageRequest, boolean recycle) {
       
        final AugmentedRequest request = stageRequest.getRequest();
        controller.quitRequest(request.getType(), stageRequest.getSize(), 
                request.hasHighPriority() || stageRequest.hasHighPriority(), request.isNativeInternalRequest());
        if (!recycle) {
            monitor(stageRequest);
            sendPiggybackPerformanceInformation(stageRequest.getPiggybackPerformanceInformationReceiver());
        }
    }
    
    /**
     * <p>Method that composes and sends {@link PerformanceInformation} to preceding components.</p>
     * 
     * @param predecessors - receiver of piggyback {@link PerformanceInformation}.
     */
    void sendPiggybackPerformanceInformation(PerformanceInformationReceiver[] predecessors) {
        
        if (predecessors.length > 0) {
            
            final PerformanceInformation pI = composePerformanceInformation();
            
            for (PerformanceInformationReceiver predecessor : predecessors) {
                
                predecessor.processPerformanceInformation(pI);
                sender.performanceInformationUpdatedPiggyback(predecessor);
            }
        }
    }
    
    /**
     * <p>Method to collect monitoring information of the given stageRequest if available.</p>
     * 
     * @param <R>
     * @param stageRequest - to collect monitoring information from.
     */
    <R extends AugmentedRequest> void monitor(OLPStageRequest<R> stageRequest) {
        
        final AugmentedRequest request = stageRequest.getRequest();
        
        if (stageRequest.hasValidMonitoringInformation()) {
            
            if (request.isNativeInternalRequest()) {

                internalRequestMonitor.record(request.getType(), stageRequest.getSize(), 
                        stageRequest.getProcessingTime());                
            } else {
                
                monitor.record(request.getType(), stageRequest.getSize(), stageRequest.getProcessingTime());
            }
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
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        
        final StringBuilder builder = new StringBuilder("StageID: " + id + "\n");
        
        builder.append("Monitor state:\n");
        builder.append(monitor.toString() + "\n");
        builder.append(internalRequestMonitor.toString() + "\n\n");
        
        builder.append("Controller state:\n");
        builder.append(controller.toString() + "\n\n");
        
        return builder.toString();
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
        
        public RequestExpiredException(Object request) {
            super("Request [" + String.valueOf(request) + "] has already been expired.");
        }
    }
}
