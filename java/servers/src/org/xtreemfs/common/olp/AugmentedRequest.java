/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.Request;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;

/**
 * <p>Superclass for instrumented requests that provide monitoring and control information for the 
 * <b>O</b>ver<b>l</b>oad-<b>P</b>rotection algorithm.</p> 
 * 
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to be single threaded.</p>
 * 
 * @author fx.langner
 * @version 1.00, 08/18/11
 */
public abstract class AugmentedRequest extends Request {

    /**
     * <p>Metadata for applying overload protection to the application by using monitoring and control information
     * gathered by this field.</p>
     */
    private final RequestMetadata metadata;
    
    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed and do not have high priority.</p>
     * 
     * @param rpcRequest - the original request received by a client.
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     */
    public AugmentedRequest(RPCServerRequest rpcRequest, int type, long deltaMaxTime) {
        
        this(rpcRequest, type, deltaMaxTime, false);
    }
    
    /**
     * <p>Constructor for requests that do not require a certain amount of bandwidth for being
     * processed.</p>
     * 
     * @param rpcRequest - the original request received by a client.
     * @param type - identifier for this kind of request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     */
    public AugmentedRequest(RPCServerRequest rpcRequest, int type, long deltaMaxTime, boolean highPriority) {
        
        this(rpcRequest, type, 0L, deltaMaxTime, highPriority, null);
    }
    
    /**
     * <p>Constructor for requests that will require a specific amount of bandwidth during processing, that is 
     * proportional to the processing time.</p>
     * 
     * @param rpcRequest - the original request received by a client.
     * @param type - identifier for this kind of request.
     * @param size - amount of bandwidth occupied during processing of this request.
     * @param deltaMaxTime - the time to live for this request.
     * @param highPriority - if true request will be treated as with high priority, false otherwise.
     * @param piggybackPerformanceReceiver - receiver of performance information send piggyback.
     */
    public AugmentedRequest(RPCServerRequest rpcRequest, int type, long size, long deltaMaxTime, boolean highPriority, 
            PerformanceInformationReceiver piggybackPerformanceReceiver) {
        
        super(rpcRequest);
        this.metadata = new RequestMetadata(type, size, deltaMaxTime, highPriority, piggybackPerformanceReceiver);
    }
    
    /**
     * @return {@link RequestMetadata}.
     */
    RequestMetadata getRequestMetadata() {
        return metadata;
    }
    
    /**
     * @see RequestMetadata#reset(PerformanceInformationReceiver receiver)
     */
    public void reset() {
        reset(null);
    }
    
    /**
     * @param receiver - performance information receiver (piggyback) for a succeeding processing step.
     * 
     * @see RequestMetadata#reset(PerformanceInformationReceiver receiver)
     */
    public void reset(PerformanceInformationReceiver receiver) {
        metadata.reset(receiver);
    }
}