/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to 
 * be single threaded.</p>
 *
 * TODO mechanism for unrefusable requests --> constructor parameter (list of request IDs) 
 *              | for unrefusables there will be no check at all!
 * 
 * @author flangner
 * @since 08/25/2011
 * @version 1.0
 */
public class Actuator implements OverloadProtectionInterface {

    private final Monitor       monitor;
    private final Controller    controller;
    private final int           id;
    
    /**
     * 
     * @param stageId
     * @param numTypes
     * @param numSubsequentStages
     */
    public Actuator(int stageId, int numTypes, int numSubsequentStages) {
        
        id = stageId;
        controller = new Controller(numTypes, numSubsequentStages);
        monitor = new SimpleMonitor(numTypes, controller);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectionInterface#obtainAdmission(org.xtreemfs.common.olp.IRequest)
     */
    @Override
    public void obtainAdmission(IRequest request) throws AdmissionRefusedException {
        
        int type = request.getType();
        long size = request.getSize();
        if (request.getRemainingProcessingTime() < controller.estimateProcessingTime(type, size)) {
            throw new AdmissionRefusedException();
        }
        controller.enterRequest(type, size);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectionInterface#depart(org.xtreemfs.common.olp.IRequest)
     */
    @Override
    public void depart(IRequest request) {
        
        int type = request.getType();
        monitor.record(type, request.getFixedProcessingTime(), request.getVariableProcessingTime());
        controller.quitRequest(type, request.getSize());
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectionInterface#addPerformanceInformation(org.xtreemfs.common.olp.PerformanceInformation)
     */
    @Override
    public void addPerformanceInformation(PerformanceInformation performanceInformation) {
        controller.updateSuccessorInformation(performanceInformation);
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectionInterface#composePerformanceInformation()
     */
    @Override
    public PerformanceInformation composePerformanceInformation() {
        return controller.composePerformanceInformation(id);
    }
    
    /**
     * <p>Exception that is thrown if admission could not have been granted to a request entering the application,
     * because the request has already been expired.</p>
     * 
     * @author flangner
     * @since 08/31/2011
     */
    public final static class RequestExpiredException extends AdmissionRefusedException {
        private static final long serialVersionUID = 6042472641208133509L;       
    }
    
    /**
     * <p>Exception that is thrown if admission could not have been granted to a request entering the application.</p>
     * 
     * @author flangner
     * @since 08/31/2011
     */
    public static class AdmissionRefusedException extends Exception {
        private static final long serialVersionUID = -1182382280938989776L;      
    }
}
