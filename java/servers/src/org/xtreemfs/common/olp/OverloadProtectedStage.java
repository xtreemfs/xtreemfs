/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.Stage;
import org.xtreemfs.common.stage.StageRequest;

/**
 * <p>Stage that is protected by overload control.</p>
 * 
 * @author flangner
 * @version 1.00, 08/31/11
 * @see ProtectionAlgorithmCore
 * 
 * @param <R> - the application's original request.
 */
public abstract class OverloadProtectedStage<R extends AugmentedRequest> extends Stage<R>
        implements PerformanceInformationReceiver {

    /**
     * <p>Reference to the overload-protection algorithm core.</p>
     */
    private final ProtectionAlgorithmCore olp;
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes) {
        
        this(stageName, stageId, numTypes, 0);
    }
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes, int numSubsequentStages) {
        
        this(stageName, stageId, numTypes, numSubsequentStages, new boolean[numTypes], 
                new PerformanceInformationReceiver[] {});
    }
    
    /**
     * <p>Default constructor for initializing the OLP algorithm for a single stage.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param unrefusableTypes - array that decides which types of requests are treated unrefusable and which not.
     * @param performanceInformationReceiver - receiver of performance information concerning this component.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes, int numSubsequentStages, 
            boolean[] unrefusableTypes, PerformanceInformationReceiver[] performanceInformationReceiver) {
        
        this(stageName, new ProtectionAlgorithmCore(stageId, numTypes, numSubsequentStages, unrefusableTypes, 
                performanceInformationReceiver));
    }
    
    /**
     * <p>Hidden constructor initializing a stage with the given name and the already initialized Overload-protection 
     * algorithm.</p>
     * 
     * @param name - of the stage.
     * @param olp - the initialized algorithm.
     */
    private OverloadProtectedStage(String name, ProtectionAlgorithmCore olp) {
        
        super(name, new SimpleProtectedQueue<R>(olp));
        this.olp = olp;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.PerformanceInformationReceiver#getStageId()
     */
    @Override
    public int getStageId() {
        
        return olp.id;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.PerformanceInformationReceiver#processPerformanceInformation(
     *          org.xtreemfs.common.olp.PerformanceInformation)
     */
    @Override
    public void processPerformanceInformation(PerformanceInformation performanceInformation) {
        
        olp.addPerformanceInformation(performanceInformation);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#processMethod(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public final void processMethod(StageRequest<R> method) {
        
        method.getRequest().getMonitoring().beginGeneralMeasurement();
        _processMethod(method);
    }
    
    /**
     * @see Stage#processMethod(StageRequest)
     */
    public abstract void _processMethod(StageRequest<R> method);
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#exit(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public final void exit(StageRequest<R> request) {
        
        request.getRequest().getMonitoring().endGeneralMeasurement();
        olp.depart(request.getRequest().getMetadata(), request.getRequest().getMonitoring());
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#shutdown()
     */
    @Override
    public void shutdown() throws Exception {
        
        olp.shutdown();
        super.shutdown();
    }
}