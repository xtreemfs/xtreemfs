/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.olp.ProtectionAlgorithmCore.RequestExpiredException;

/**
 * <p>Contains metadata for a request that will be recognized by the overload-protection algorithm. Beside configuration 
 * parameter it also enables piggybacking for preceding components. This component is supposed to be stateless.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/14/2011
 */
public class RequestMetadata {
    
    /**
     * <p>Identifier for requests of this type.</p>
     */
    private final int  type;
    
    /**
     * <p>Bandwidth occupied by this request.</p>
     */
    private long       size;
    
    /**
     * <p>Maximal response time delta for this request.</p>
     */
    private final long deltaMaxTime;
    
    /**
     * <p>The Unix time stamp this request was initially received.</p>
     */
    private final long startTime;
    
    /**
     * <p>Flag to determine whether this request has high priority or not.</p>
     */
    private boolean    highPriority;
        
    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed and do not have high priority.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     */
    public RequestMetadata(int type, long deltaMaxTime) {
        
        this(type, deltaMaxTime, false);
    }
    
    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     */
    public RequestMetadata(int type, long deltaMaxTime, boolean highPriority) {
        
        this(type, 0L, deltaMaxTime, highPriority);
    }
    
    /**
     * <p>Constructor for requests that will require a specific amount of bandwidth during processing, that is 
     * proportional to the processing time.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param size - amount of bandwidth occupied during processing of this request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     */
    public RequestMetadata(int type, long size, long deltaMaxTime, boolean highPriority) {
                
        assert (deltaMaxTime > 0);
        assert (type > -1);
        
        this.type = type;
        this.size = size;
        this.deltaMaxTime = deltaMaxTime;
        this.startTime = System.currentTimeMillis();
        this.highPriority = highPriority;
    }
    
    /**
     * <p>Method to update the processing size for the next step in request processing.</p>
     * 
     * @param newSize
     */
    public void updateSize(long newSize) {
        size = newSize;
    }
    
    /**
     * <p>Increases priority for the next processing step, if possible.</p>
     */
    public void increasePriority() {
        highPriority = true;
    }
    
    /**
     * <p>Decreases priority for the next processing step, if possible.</p>
     */
    public void decreasePriority() {
        highPriority = false;
    }
    
/*
 * package internal methods
 */
    
    /**
     * @return the identifier of the type of this request.
     */
    int getType() {
        
        return type;
    }
    
    /**
     * @return size of this request defined by the bandwidth needed to process it.
     */
    long getSize() {
        
        return size;
    }
    
    /**
     * @return true if this request has high priority, false otherwise.
     */
    boolean hasHighPriority() {
        
        return highPriority;
    }
        
    /**
     * <p>Calculates the remaining processing time for this request. If the request has already been expired an 
     * Exception is thrown.</p>
     * 
     * @return the remaining processing time for this request in ms.
     * @throws RequestExpiredException if the processing time for this request has already been exhausted.
     */
    double getRemainingProcessingTime() throws RequestExpiredException {
        
        long remaining = (deltaMaxTime + startTime) - System.currentTimeMillis();
        if (remaining < 0) {
            throw new RequestExpiredException();
        }
        return remaining;
    }
}
