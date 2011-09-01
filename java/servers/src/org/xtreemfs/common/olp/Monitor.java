/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Interface for monitors that record request performance information. Implementations may differ in the amount of 
 * samples recorded and the metric used to summarize these samples.</p>
 * 
 * @author flangner
 * @since 08/18/2011
 * @version 1.0
 */
interface Monitor {
    
    /**
     * <p>Records monitoring information of the request of the given type.</p>
     * 
     * @param type - processing-time-class for this request.
     * @param fixedProcessingTime - the processing of the request required in ms.
     * @param variableProcessingTime - the duration of the processing normalized to a bandwidth of one in ms/byte.
     */
    void record(int type, double fixedProcessingTime, double variableProcessingTime);
}