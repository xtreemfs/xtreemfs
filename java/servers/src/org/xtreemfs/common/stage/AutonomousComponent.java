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
     */
    public void enter(R request);

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
}