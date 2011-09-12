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
 * @see OverloadProtection
 */
public abstract class OverloadProtectedStage<R extends IRequest> extends Stage<R> 
        implements PerformanceInformationReceiver {

    /**
     * <p>Reference to the overload-protection algorithm core.</p>
     */
    private final OverloadProtection olp;
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes) {
        this(stageName, new OverloadProtection(stageId, numTypes));
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
        this(stageName, new OverloadProtection(stageId, numTypes, numSubsequentStages));
    }
    
    /**
     * <p>Default constructor for initializing the OLP algorithm for a single stage.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param unrefusableTypes - array that decides which types of requests are treated unrefusable and which not.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes, boolean[] unrefusableTypes) {
        this(stageName, new OverloadProtection(stageId, numTypes));
    }
    
    /**
     * <p>Hidden constructor initializing a stage with the given name and the already initialized Overload-protection 
     * algorithm.</p>
     * 
     * @param name - of the stage.
     * @param olp - the initialized algorithm.
     */
    private OverloadProtectedStage(String name, OverloadProtection olp) {
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
     * @see org.xtreemfs.common.stage.Stage#exit(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public void exit(StageRequest<R> request) {
        olp.depart(request.getRequest());
        
        if (request.getCallback() instanceof PerformanceAugmentedCallback) {
            PerformanceInformationReceiver receiver = (PerformanceInformationReceiver) request.getCallback();
            receiver.processPerformanceInformation(olp.composePerformanceInformation());
            olp.sender.performanceInformationUpdatedPiggyback(receiver);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#shutdown()
     */
    @Override
    public void shutdown() {
        olp.shutdown();
        super.shutdown();
    }
}