/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Main class provides all essential methods to integrate the <b>O</b>ver<b>l</b>oad-<b>P</b>rotection algorithm 
 * <b>OLP</b> into an existing application.</p>
 * 
 * <p>This class has to be separated from OverloadProtectedStage, because otherwise its methods would not have been 
 * available to ProtectedQueue.</p>
 * 
 * @author flangner
 * @version 1.00, 08/31/11
 * @see ProtectedQueue
 */
public class OverloadProtection {

    /**
     * <p>Identifier that is unique among parallel stages that follow the same predecessor, this stage belongs to.</p>
     */
    protected final int                         id;
    
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
    final PerformanceInformationSender  sender;
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     */
    OverloadProtection(int stageId, int numTypes) {
        this (stageId, numTypes, 0, new boolean[numTypes]);
    }
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     */
    OverloadProtection(int stageId, int numTypes, int numSubsequentStages) {
        this (stageId, numTypes, numSubsequentStages, new boolean[numTypes]);
    }
    
    /**
     * <p>Default constructor for initializing the OLP algorithm for a single stage.</p>
     * 
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param unrefusableTypes - array that decides which types of requests are treated unrefusable and which not.
     */
    OverloadProtection(int stageId, int numTypes, int numSubsequentStages, boolean[] unrefusableTypes) {
        
        assert (unrefusableTypes.length == numTypes);
        
        this.unrefusableTypes = unrefusableTypes;
        this.id = stageId;
        this.actuator = new Actuator();
        this.controller = new Controller(numTypes, numSubsequentStages);
        this.monitor = new SimpleMonitor(numTypes, controller);
        this.sender = new PerformanceInformationSender(this);
    }
    
    /**
     * <p>Method to stop daemons connected with the overload protection algorithm.</p>
     */
    void shutdown() {
        sender.cancel();
    }
    
    /**
     * <p>Checks whether request can be processed before its timing out.</p>
     * 
     * @param request - the request to check.
     * @throws AdmissionRefusedException if requests has already expired or will expire before processing has finished.
     */
    void hasAdmission(IRequest request) throws AdmissionRefusedException {
        
        int type = request.getType();
        if (!unrefusableTypes[type] && 
            !actuator.hasAdmission(request.getRemainingProcessingTime(), 
             controller.estimateProcessingTime(type, request.getSize(), request.hasHighPriority()))) {
                
            throw new AdmissionRefusedException();
        }
    }
    
    /**
     * <p>Method to be executed to determine whether the given request may be processed or not.</p>
     * 
     * @param request - the request to be processed.
     * @throws AdmissionRefusedException if requests has already expired or will expire before processing has finished.
     */
    void obtainAdmission(IRequest request) throws AdmissionRefusedException {
                
        hasAdmission(request);
        controller.enterRequest(request.getType(), request.getSize(), request.hasHighPriority());
    }
    
    /**
     * <p>If the processing of the given request has finished, its departure has to be notified to the algorithm by this
     * method.</p>
     * 
     * @param request - the request that has been processed.
     */
    void depart(IRequest request) {
        
        int type = request.getType();
        monitor.record(type, request.getFixedProcessingTime(), request.getVariableProcessingTime());
        controller.quitRequest(type, request.getSize(), request.hasHighPriority());
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
