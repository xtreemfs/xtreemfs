/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

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
     * <p>Component that will continuously send {@link PerformanceInformation} to preceding stages.</p>
     */
    private final PerformanceInformationSender  sender;
    
    /**
     * <p>Default constructor for initializing the OLP algorithm.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param unrefusableTypes - array that decides which types of requests are treated unrefusable and which not.
     * @param performanceInformationReceiver - receiver of performance information concerning this component.
     */
    ProtectionAlgorithmCore(int stageId, int numTypes, int numSubsequentStages, boolean[] unrefusableTypes, 
            PerformanceInformationReceiver[] performanceInformationReceiver) {
        
        assert (unrefusableTypes.length == numTypes);
        assert (performanceInformationReceiver != null);
        
        this.unrefusableTypes = unrefusableTypes;
        this.id = stageId;
        this.actuator = new Actuator();
        this.controller = new Controller(numTypes, numSubsequentStages);
        this.monitor = new SimpleMonitor(numTypes, controller);
        this.sender = new PerformanceInformationSender(this);
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
     * @param metadata - of the request to check.
     * @throws AdmissionRefusedException if requests has already expired or will expire before processing has finished.
     */
    void hasAdmission(RequestMetadata metadata) throws AdmissionRefusedException {
        
        int type = metadata.getType();
        if (!unrefusableTypes[type] && 
            !actuator.hasAdmission(metadata.getRemainingProcessingTime(), 
             controller.estimateProcessingTime(type, metadata.getSize(), metadata.hasHighPriority()))) {
                
            throw new AdmissionRefusedException();
        }
    }
    
    /**
     * <p>Method to be executed to determine whether the request represented by its metadata may be processed or 
     * not.</p>
     * 
     * @param metadata - of the request to be processed.
     * @throws AdmissionRefusedException if requests has already expired or will expire before processing has finished.
     */
    void obtainAdmission(RequestMetadata metadata) throws AdmissionRefusedException {
                
        hasAdmission(metadata);
        controller.enterRequest(metadata.getType(), metadata.getSize(), metadata.hasHighPriority());
    }
    
    /**
     * <p>If the processing of the given request has finished, its departure has to be notified to the algorithm by this
     * method.</p>
     * 
     * @param metadata - the request that has been processed.
     * @param monitoring - information about the processing time of the request.
     */
    void depart(RequestMetadata metadata, RequestMonitoring monitoring) {
        
        controller.quitRequest(metadata.getType(), metadata.getSize(), metadata.hasHighPriority());
        if (monitoring.isValid()) {
            monitor.record(metadata.getType(), monitoring.getFixedProcessingTime(), 
                           monitoring.getVariableProcessingTime(metadata.getSize()));
        }
        PerformanceInformationReceiver receiver = monitoring.getPiggybackPerformanceInformationReceiver();
        if (receiver != null) {
            receiver.processPerformanceInformation(composePerformanceInformation());
            sender.performanceInformationUpdatedPiggyback(receiver);
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
    
    /**
     * <p>Exception that is thrown if admission could not have been granted to a request entering the application.</p>
     * 
     * @author flangner
     * @version 1.00, 08/31/11
     */
    public static class AdmissionRefusedException extends Exception {
        private static final long serialVersionUID = -1182382280938989776L;      
    }
}
