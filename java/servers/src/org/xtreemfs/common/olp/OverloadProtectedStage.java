/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.AutonomousComponent;
import org.xtreemfs.common.stage.Callback;
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
    private final ProtectionAlgorithmCore          olp;
    
    /**
     * <p>Receiver of {@link PerformanceInformation} initially registered at the stage.</p>
     */
    private PerformanceInformationReceiver[]       initialPredecessors;
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following. Default constructor for inner nodes in the processing tree.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param period - delay in ms between two cron-Jobs.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes, int numInternalTypes, 
            int numSubsequentStages, long period) {
        
        this(stageName, stageId, numTypes, numInternalTypes, numSubsequentStages, new boolean[numTypes], period);
    }
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following. Default constructor for inner nodes in the processing tree.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes, int numInternalTypes, 
            int numSubsequentStages) {
        
        this(stageName, stageId, numTypes, numInternalTypes, numSubsequentStages, new boolean[numTypes], 0L);
    }

    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following. Default constructor for a leaf in the processing tree.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     * @param period - delay in ms between two cron-Jobs.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes, int numInternalTypes, long period) {
        
        this(stageName, stageId, numTypes, numInternalTypes, 0, new boolean[numTypes], period);
    }
    
    /**
     * <p>Constructor for initializing the OLP algorithm for a single stage. Is is used, if no request-type is 
     * unrefusable and no subsequent stages following. Default constructor for a leaf in the processing tree.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes, int numInternalTypes) {
        
        this(stageName, stageId, numTypes, numInternalTypes, 0, new boolean[numTypes], 0L);
    }
    
    /**
     * <p>Default constructor for initializing the OLP algorithm for a single stage.</p>
     * 
     * @param stageName - human-readable identifier for this stage.
     * @param stageId - a identifier that is unique among parallel stages that follow the same predecessor.
     * @param numTypes - amount of different types of requests.
     * @param numInternalTypes - amount of different internal types of requests.
     * @param numSubsequentStages - amount of parallel stages following directly behind this stage.
     * @param unrefusableTypes - array that decides which types of requests are treated unrefusable and which not.
     * @param period - delay in ms between two cron-Jobs.
     */
    public OverloadProtectedStage(String stageName, int stageId, int numTypes, int numInternalTypes, 
            int numSubsequentStages, boolean[] unrefusableTypes, long period) {
        
        this(stageName, new ProtectionAlgorithmCore(stageId, numTypes, numInternalTypes, numSubsequentStages, 
                unrefusableTypes), period);
    }
    
    /**
     * <p>Hidden constructor initializing a stage with the given name and the already initialized Overload-protection 
     * algorithm.</p>
     * 
     * @param name - of the stage.
     * @param olp - the initialized algorithm.
     * @param period - delay in ms between two cron-Jobs.
     */
    private OverloadProtectedStage(String name, ProtectionAlgorithmCore olp, long period) {
        super(name, new SimpleProtectedQueue<R>(olp), period);
        
        this.olp = olp;
    }
    
    /**
     * <p>Method to initially register receiver of {@link PerformanceInformation} collected by the OLP algorithm.</p>
     * 
     * @param performanceInformationReceiver - receiver of performance information concerning this component.
     */
    public void registerPerformanceInformationReceiver(
            PerformanceInformationReceiver[] performanceInformationReceiver) {
        
        this.olp.addPerformanceInformationReceiver(performanceInformationReceiver);
        this.initialPredecessors = performanceInformationReceiver;
    }
    
    /**
     * <p>Hidden constructor initializing a stage with the given name and the already initialized Overload-protection 
     * algorithm.</p>
     * 
     * @param name - of the stage.
     * @param olp - the initialized algorithm.
     * @param performanceInformationReceiver - receiver of performance information concerning this component.
     */
    private OverloadProtectedStage(String name, ProtectionAlgorithmCore olp, 
            PerformanceInformationReceiver[] performanceInformationReceiver) {
        super(name, new SimpleProtectedQueue<R>(olp));
        
        this.initialPredecessors = performanceInformationReceiver;
        this.olp = olp;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.PerformanceInformationReceiver#getStageId()
     */
    @Override
    public int getStageId() {
        
        return olp.id;
    }
    
    /**
     * <p>Method to announce a temporary interruption of the processing of the request represented by its monitoring 
     * information. The request will remain at the component until its execution is resumed and finished.</p>
     * 
     * @param stageRequest - {@link OLPStageRequest} containing monitoring information for the processing step provided 
     *                       by this {@link AutonomousComponent}.
     * 
     * @see OverloadProtectedComponent#resumeRequestProcessing(OLPStageRequest)
     */
    public void suspendRequestProcessing(OLPStageRequest<R> stageRequest) {
        
        stageRequest.endGeneralMeasurement();
    }
    
    /**
     * <p>Method resume a temporarily interrupted request represented by its monitoring information.</p>
     * 
     * @param stageRequest - {@link OLPStageRequest} containing monitoring information for the processing step provided 
     *                       by this {@link AutonomousComponent}.
     * 
     * @see OverloadProtectedComponent#suspendRequestProcessing(OLPStageRequest)
     */
    public void resumeRequestProcessing(OLPStageRequest<R> stageRequest) {
        
        stageRequest.beginGeneralMeasurement();
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.PerformanceInformationReceiver#processPerformanceInformation(
     *          org.xtreemfs.common.olp.PerformanceInformation)
     */
    @Override
    public void processPerformanceInformation(PerformanceInformation performanceInformation) {
        
        olp.addPerformanceInformation(performanceInformation);
    }
    
    /**
     * <p>Enqueues a request including all necessary information for its processing at this stage. Includes a receiver
     * for OLP {@link PerformanceInformation}.</p>
     * 
     * @param stageMethodId - the identifier for the method to use during processing.
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request, may be null.
     * @param piggybackPerformanceInformationReceiver - receiver of OLP {@link PerformanceInformation}.
     */
    public final void enter(int stageMethodId, Object[] args, R request, Callback callback, 
            PerformanceInformationReceiver[] piggybackPerformanceInformationReceiver) {
        enter(0L, stageMethodId, args, request, callback, piggybackPerformanceInformationReceiver);
    }
    
    /**
     * <p>Enqueues a request including all necessary information for its processing at this stage. Includes a receiver
     * for OLP {@link PerformanceInformation}.</p>
     * 
     * @param size
     * @param stageMethodId - the identifier for the method to use during processing.
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request, may be null.
     * @param piggybackPerformanceInformationReceiver - receiver of OLP {@link PerformanceInformation}.
     */
    public final void enter(long size, int stageMethodId, Object[] args, R request, Callback callback, 
            PerformanceInformationReceiver[] piggybackPerformanceInformationReceiver) {
        
        enter(new OLPStageRequest<R>(size, stageMethodId, args, request, callback, 
                piggybackPerformanceInformationReceiver));
    }
    
    /**
     * <p>Enqueues a request including all necessary information for its processing at this stage. Includes a receiver
     * for OLP {@link PerformanceInformation}.</p>
     * 
     * @param stageMethodId - the identifier for the method to use during processing.
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request, may be null.
     * @param piggybackPerformanceInformationReceiver - receiver of OLP {@link PerformanceInformation}.
     */
    public final void enter(int stageMethodId, Object[] args, R request, Callback callback, 
            PerformanceInformationReceiver piggybackPerformanceInformationReceiver) {
        
        enter(0L, stageMethodId, args, request, callback, new PerformanceInformationReceiver[] {
                piggybackPerformanceInformationReceiver });
    }
    
    /**
     * <p>Enqueues a request including all necessary information for its processing at this stage. Includes a receiver
     * for OLP {@link PerformanceInformation}.</p>
     * 
     * @param size
     * @param stageMethodId - the identifier for the method to use during processing.
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request, may be null.
     * @param piggybackPerformanceInformationReceiver - receiver of OLP {@link PerformanceInformation}.
     */
    public final void enter(long size, int stageMethodId, Object[] args, R request, Callback callback, 
            PerformanceInformationReceiver piggybackPerformanceInformationReceiver) {
        
        enter(new OLPStageRequest<R>(size, stageMethodId, args, request, callback, 
                new PerformanceInformationReceiver[] { piggybackPerformanceInformationReceiver }));
    }
    
    /**
     * <p>Method to reschedule given stageRequest that has been processed by this stage before.</p>
     * 
     * @param stageRequest
     * @param newMethodId
     * @param newArgs
     * @param newCallback
     * @param highPriority
     */
    public final void recycle(OLPStageRequest<R> stageRequest, int newMethodId, Object[] newArgs, 
            Callback newCallback, boolean highPriority) {
        
        olp.depart(stageRequest, true);
        stageRequest.update(highPriority, newMethodId, newArgs, newCallback);
        enter(stageRequest);
    }
    
    /**
     * <p>Method to reschedule given stageRequest that has been processed by this stage before.</p>
     * 
     * @param stageRequest
     * @param highPriority
     */
    public final void recycle(OLPStageRequest<R> stageRequest, boolean highPriority) {
        
        olp.depart(stageRequest, true);
        if (highPriority) {
            stageRequest.increasePriority();
        } else {
            stageRequest.decreasePriority();
        }
        enter(stageRequest);
    }
    
    /**
     * @return initially registered receiver of {@link PerformanceInformation} collected by the stage.
     */
    public final PerformanceInformationReceiver[] getInitialPredecessors() {
        
        return initialPredecessors;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#generateStageRequest(int, java.lang.Object[], java.lang.Object, 
     *          org.xtreemfs.common.stage.Callback)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected final <S extends StageRequest<R>> S generateStageRequest(int stageMethodId, Object[] args, R request, 
            Callback callback) {
        
        return (S) new OLPStageRequest<R>(stageMethodId, args, request, callback);
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#processMethod(org.xtreemfs.common.stage.StageRequest)
     */
    @Override    
    protected final <S extends StageRequest<R>> boolean processMethod(S stageRequest) {
        
        resumeRequestProcessing((OLPStageRequest<R>) stageRequest);
        boolean exitCode = _processMethod((OLPStageRequest<R>) stageRequest);
        suspendRequestProcessing((OLPStageRequest<R>) stageRequest);
        
        return exitCode;
    }
    
    /**
     * @see Stage#processMethod(StageRequest)
     */
    protected abstract boolean _processMethod(OLPStageRequest<R> stageRequest);
        
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.Stage#exit(org.xtreemfs.common.stage.StageRequest)
     */
    @Override
    public final void exit(StageRequest<R> stageRequest) {
        
        olp.depart((OLPStageRequest<R>) stageRequest);
        super.exit(stageRequest);
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