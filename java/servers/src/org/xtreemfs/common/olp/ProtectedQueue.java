/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.LinkedList;
import java.util.Queue;

import org.xtreemfs.common.olp.Actuator.AdmissionRefusedException;

/**
 * <p>This queue is protected by the Overload-Protection algorithm. Requests that could not be processed in a limited 
 * time will be rejected. Basically this queue implementation follows a FIFO-ordering.</p> 
 * 
 * @author flangner
 * @since 09/01/2011
 * @version 1.0
 */
public class ProtectedQueue<R extends IRequest> {
    
    /**
     * <p>The queue to buffer the requests before their processing.</p>
     */
    private final Queue<R>                      queue = new LinkedList<R>();
    
    /**
     * <p>The interface to the Overload-Protection algorithm.</p>
     */
    private final OverloadProtectionInterface   olp;
    
    /**
     * <p>Initializes an empty queue with Overload-Protection enabled.</p>
     * 
     * @param olp - the interface to the Overload-Protection algorithm.
     */
    public ProtectedQueue(OverloadProtectionInterface olp) {
        this.olp = olp;
    }
    
    /**
     * <p>If possible the request will be attached to the end of the queue. If admission could not be obtain by the 
     * Overload-Protection algorithm the request will be rejected throwing a RequestExpiredException.</p>
     * 
     * @param request - the request to enqueue.
     * @throws AdmissionRefusedException if the request has no admission to be enqueued and processed.
     */
    public synchronized void enqueue(R request) throws AdmissionRefusedException {
        
        olp.obtainAdmission(request);
        queue.add(request);
        
        if (queue.size() == 1) {
            notify();
        }
    }
    
    /**
     * <p>Method will return the request next request to process. Waits synchronously for a request to become available,
     * if there is currently none.</p>
     * 
     * @return the request to be processed next.
     * @throws InterruptedException if waiting for a request was interrupted.
     */
    public synchronized R take() throws InterruptedException {
        
        if (queue.size() == 0) {
            wait();
        }
        
        return queue.poll();
    }
}
