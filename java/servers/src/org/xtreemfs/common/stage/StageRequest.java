/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

/**
 * <p>Interface for a stage-local representation of a global request of type R. Provides additional fields such as 
 * {@link Callback}, method identifier or request arguments.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/05/11
 * 
 * @param <R> - general interface for requests attached to this stage request.
 */    
public abstract class StageRequest<R> {
    
    /**
     * <p>Identifier for the logic to use to process the request.</p>
     */
    private int       stageMethodId;
    
    /**
     * <p>A callback for the postprocessing of the request.</p>
     */
    private Callback  callback;
    
    /**
     * <p>Additional arguments for the request.</p>
     */
    private Object[]  args;
    
    /**
     * <p>The original request.</p>
     */
    protected final R request;
    
    /**
     * <p>Flag that determines if this stage request is being recycled, or not.
     */
    private boolean   recycled = false;
    
    /**
     * <p>Constructor to initialize the wrapper with all necessary information.</p>
     * 
     * @param stageMethodId - the identifier for the method to use during processing.
     * @param args - additional arguments for the request.
     * @param request - the original request.
     * @param callback - for postprocessing the request.
     */
    protected StageRequest(int stageMethodId, Object[] args, R request, Callback callback) {
                      
        this.request = request;
        this.args = args;
        this.stageMethodId = stageMethodId;
        this.callback = (callback == null) ? Callback.NullCallback.INSTANCE : callback;
    }
    
    /**
     * @return the identifier for the method to use during processing.
     */
    public int getStageMethod() {
        
        return stageMethodId;
    }
    
    /**
     * <p>Method to update the request on re-queue.</p>
     * 
     * @param newMethodId
     * @param newArgs
     * @param newCallback
     */
    public void update(int newMethodId, Object[] newArgs, Callback newCallback) {
        
        stageMethodId = newMethodId;
        args = newArgs;
        callback = newCallback;
        recycled = true;
    }
    
    /**
     * <p>Will reset the recycled flag. Recycled requests may not be denied.</p>
     * 
     * @return true if stage request is currently recycled.
     */
    public boolean isRecycled() {
        
        final boolean result = recycled;
        recycled = false;
        return result;
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
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        
        // XXX " with arguments '" + Arrays.toString(args) + has to exclude reusable buffer (because they might already
        // be freed 
        return "StageRequest " + stageMethodId + "' based on external request: " + 
            ((request != null) ? request.toString() : "null");
    }
}