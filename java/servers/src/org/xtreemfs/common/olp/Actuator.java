/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>The actuator will throttle influx of requests by rejecting those that have not been marked as unrefusable and will
 * not be processed without a timeout at the client.</p>
 * 
 * @author fx.langner
 * @version 1.00, 08/25/11
 */
class Actuator {
        
    /**
     * <p>Method to check whether admission requirements are fulfilled or not.</p>
     * 
     * @param remainingProcessingTime
     * @param estimatedProcessingTime
     * @return true, if admission is granted, and false otherwise.
     */
    boolean hasAdmission(double remainingProcessingTime, double estimatedProcessingTime)  {
        assert (remainingProcessingTime >= 0.0);
        
        return remainingProcessingTime >= estimatedProcessingTime;
    }
}
