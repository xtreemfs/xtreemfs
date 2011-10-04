/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

/**
 * <p>Abstraction for components that will independently process requests of type R. Requests may enter the component 
 * and pass exit after their processing. Because autonomous components might be able to buffer a certain amount of 
 * requests or execute multiple requests at once, it is possible comprehend how many requests are currently located at
 * this component.</p> 
 * 
 * @author fx.langner
 * @version 1.00, 09/13/2011
 * @param <R> - type for requests handled by this component.
 */
public interface AutonomousComponent<R> {

    /**
     * <p>Enqueues a request including all necessary information for its processing at this component.</p>
     * 
     * @param request - the request.
     * 
     * @throws AdmissionRefusedException - if request was refused by this {@link AutonomousComponent}.
     */
    public void enter(R request) throws AdmissionRefusedException;

    /**
     * <p>Method that is executed when request is going to leave this component.</p>
     * 
     * @param request - the request that leaves this stage.
     */
    public void exit(R request);

    /**
     * <p>Get current number of requests buffered by this component.</p>
     * 
     * @return number of requests currently processed by this component.
     */
    public int getNumberOfRequests();
    
/*
 * Exceptions
 */
    
    /**
     * <p>Exception that is thrown if admission could not have been granted to a request entering the application.</p>
     * 
     * @author flangner
     * @version 1.00, 08/31/11
     */
    public static class AdmissionRefusedException extends Exception {
        
        private static final long serialVersionUID = -1182382280938989776L;      
    }
}