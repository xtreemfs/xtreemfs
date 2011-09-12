/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Interface for evaluation of performance information for overload protection.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/09/11
 */
interface PerformanceInformationReceiver {

    /**
     * @return a unique identifier for the stage represented by this receiver.
     */
    int getStageId();
    
    /**
     * <p>Method that evaluates processing information send by succeeding stages.</p> 
     * 
     * @param performanceInformation
     */
    void processPerformanceInformation(PerformanceInformation performanceInformation);
}
