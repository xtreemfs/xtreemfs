/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.Request;

/**
 * <p>Interface for instrumented requests that provide monitoring and control information for the 
 * <b>O</b>ver<b>l</b>oad-<b>P</b>rotection algorithm.</p> 
 * 
 * @author fx.langner
 * @version 1.00, 08/18/11
 */
public abstract class AugmentedRequest implements Request {
    
    /**
     * <p>Metadata for applying overload protection to the application using control information gathered by this 
     * field.</p>
     */
    private final RequestMetadata   metadata;
    
    /**
     * <p>Monitoring information of the current processing of this request.</p>
     */
    private final RequestMonitoring monitoring;
    
    /**
     * <p>Constructor for requests that will require a specific amount of bandwidth during processing, that is 
     * proportional to the processing time.</p>
     * 
     * @param type - identifier for this kind of request.
     * @param size - amount of bandwidth occupied during processing of this request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     * @param piggybackPerformanceReceiver - receiver of performance information send piggyback.
     */
    public AugmentedRequest(int type, long size, long deltaMaxTime, boolean highPriority, 
            PerformanceInformationReceiver piggybackPerformanceReceiver) {
        
        this(new RequestMetadata(type, size, deltaMaxTime, highPriority), 
             new RequestMonitoring(piggybackPerformanceReceiver));
    }
    
    /**
     * <p>Creates a new request object based on the request metadata used for the preceding processing step.</p>
     * 
     * @param metadata - of the source request.
     * @param piggybackPerformanceReceiver - receiver of performance information send piggyback.
     */
    public AugmentedRequest(RequestMetadata metadata, PerformanceInformationReceiver piggybackPerformanceReceiver) {
        
        this(metadata, new RequestMonitoring(piggybackPerformanceReceiver));
    }
    
    /**
     * <p>Hidden constructor.</p>
     * 
     * @param metadata
     * @param monitoring
     */
    private AugmentedRequest(RequestMetadata metadata, RequestMonitoring monitoring) {
        
        this.metadata = metadata;
        this.monitoring = monitoring;
    }
    
    /**
     * @return {@link RequestMetadata}.
     */
    public final RequestMetadata getMetadata() {
        return metadata;
    }
    
    /**
     * @return {@link RequestMonitoring}.
     */
    public final RequestMonitoring getMonitoring() {
        return monitoring;
    }
}