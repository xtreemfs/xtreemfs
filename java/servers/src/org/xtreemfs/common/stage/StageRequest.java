/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

/**
 * <p>Wrapper for client requests providing additional information for their processing at the stage.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/05/11
 * 
 * <R> - general interface for requests attached to this stage request. Extends {@link Request}.
 */    
public final class StageRequest<R extends Request> {
    
    /**
     * <p>Identifier for the logic to use to process the request.</p>
     */
    private final int      stageMethodId;
    
    /**
     * <p>A callback for the postprocessing of the request.</p>
     */
    private final Callback callback;
    
    /**
     * <p>Additional arguments for the request.</p>
     */
    private final Object[] args;
    
    /**
     * <p>The original request.</p>
     */
    private final R        request;
    
    /**
     * <p>Constructor to initialize the wrapper with all necessary information.</p>
     * 
     * @param stageMethodId - the identifier for the method to use during processing.
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request.
     */
    StageRequest(int stageMethodId, Object[] args, R request, Callback callback) {
        
        this.request = request;
        this.args = args;
        this.stageMethodId = stageMethodId;
        this.callback = callback;
    }
    
    /**
     * @return the identifier for the method to use during processing.
     */
    public int getStageMethod() {
        return stageMethodId;
    }
    
    /**
     * @return additional arguments for the request.
     */
    public Object[] getArgs() {
        return args;
    }
    
    /**
     * @return for postprocessing the request.
     */
    public Callback getCallback() {
        return callback;
    }
    
    /** 
     * @return the original request.
     */
    public R getRequest() {
        return request;
    }
}
