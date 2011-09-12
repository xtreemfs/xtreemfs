/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.Callback;

/**
 * <p>Callback extension that provides {@link PerformanceInformation} to a preceding stage.</p>
 * 
 * TODO mechanism for PBRPC piggyback is required
 * 
 * @author fx.langner
 * @version 0.50, 09/06/11
 */
public abstract class PerformanceAugmentedCallback implements Callback, PerformanceInformationReceiver {

    private final PerformanceInformationReceiver receiver;
        
    public PerformanceAugmentedCallback(PerformanceInformationReceiver receiver) {
        
        assert(receiver != null);
        
        this.receiver = receiver;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.PerformanceInformationReceiver#processPerformanceInformation(
     *          org.xtreemfs.common.olp.PerformanceInformation)
     */
    @Override
    public void processPerformanceInformation(PerformanceInformation performanceInformation) {
        receiver.processPerformanceInformation(performanceInformation);
    }
}
