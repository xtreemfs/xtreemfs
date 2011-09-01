/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.olp.Actuator.AdmissionRefusedException;

/**
 * <p>This interface provides all essential method signatures to integrate the <b>O</b>ver<b>l</b>oad-<b>P</b>rotection 
 * algorithm <b>OLP</b> into an existing application.</p>
 * 
 * 
 * @author flangner
 * @since 08/31/2011
 * @version 1.0
 */
public interface OverloadProtectionInterface {
    
    /**
     * <p>Method to be executed to determine whether the given request may be processed or not.</p>
     * 
     * @param request - the request to be processed.
     * @throws AdmissionRefusedException if processing of the given request is not permitted.
     */
    public void obtainAdmission(IRequest request) throws AdmissionRefusedException;
    
    /**
     * <p>If the processing of the given request has finished, its departure has to be notified to the algorithm by this
     * method.</p>
     * 
     * @param request - the request that has been processed.
     */
    public void depart(IRequest request);
    
    /**
     * <p>Method to pass aggregated performance information about successive stages to the OLP of this stage.</p>
     * 
     * @param performanceInformation - information received by another stage.
     */
    public void addPerformanceInformation(PerformanceInformation performanceInformation);
    
    /**
     * <p>Method to retrieve aggregate performance information about this and successive stages.</p>
     * 
     * @return performance information for the protected stage.
     */
    public PerformanceInformation composePerformanceInformation();
}
