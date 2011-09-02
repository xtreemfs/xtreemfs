/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.olp.OverloadProtection.AdmissionRefusedException;

/**
 * <p>Queue that is protected by the Overload-Protection algorithm. Requests that could not be processed in a limited 
 * time will be rejected.</p> 
 * 
 * @author fx.langner
 * @version 1.00, 09/02/11
 * @param <R> - type for the queued requests. Have to extend instrumented {@link IRequest}.
 */
public interface ProtectedQueue<R extends IRequest> {

    /**
     * <p>If possible the request will be attached to the end of the queue. If admission could not be obtain by the 
     * Overload-Protection algorithm the request will be rejected throwing a RequestExpiredException.</p>
     * 
     * @param request - the request to enqueue.
     * @throws AdmissionRefusedException if the request has no admission to be enqueued and processed.
     */
    public void enqueue(R request) throws AdmissionRefusedException;

    /**
     * <p>Method will return the request next request to process. Waits synchronously for a request to become available,
     * if there is currently none.</p>
     * 
     * @return the request to be processed next.
     * @throws InterruptedException if waiting for a request was interrupted.
     */
    public R take() throws InterruptedException;
}