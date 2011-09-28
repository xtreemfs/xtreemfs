/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.stages;

import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.common.olp.PerformanceInformationReceiver;

/**
 * <p>Stage-internal custom requests, that may be defined to circumvent architectural restrictions.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/27/2011
 */
public final class StageInternalRequest extends AugmentedRequest {

    /**
     * @param type
     * @param size
     * @param deltaMaxTime
     * @param highPriority
     * @param piggybackPerformanceReceiver
     */
    public StageInternalRequest(int type, long size, long deltaMaxTime, boolean highPriority,
            PerformanceInformationReceiver piggybackPerformanceReceiver) {
        super(type, size, deltaMaxTime, highPriority, piggybackPerformanceReceiver);
    }
    
    /**
     * @param request
     * @param piggybackPerformanceReceiver
     */
    public StageInternalRequest(AugmentedRequest request, PerformanceInformationReceiver piggybackPerformanceReceiver) {
        super(request.getMetadata(), piggybackPerformanceReceiver);
    }
}