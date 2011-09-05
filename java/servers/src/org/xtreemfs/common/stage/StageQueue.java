/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

/**
 * <p>Interface for queues to be used to buffer requests at stages.</p> 
 * 
 * @author fx.langner
 * @version 1.00, 09/02/11
 * 
 * <R> - type for the queued requests. Has to extend {@link Request}.
 */
public interface StageQueue<R extends Request> {

    /**
     * <p>The given request will be attached to the queue.</p>
     * 
     * @param request - the request to enqueue.
     */
    void enqueue(StageRequest<R> request);

    /**
     * <p>Method will return the request next request to process. Waits synchronously for a request to become available,
     * if there is currently none.</p>
     * 
     * @return the request to be processed next.
     * @throws InterruptedException if waiting for a request was interrupted.
     */
    StageRequest<R> take() throws InterruptedException;
    
    /**
     * @return the count of requests currently queued.
     */
    int getLength();
}