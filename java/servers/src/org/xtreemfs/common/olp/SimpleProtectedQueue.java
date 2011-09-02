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

import org.xtreemfs.common.olp.OverloadProtection.AdmissionRefusedException;

/**
 * <p>Basically this queue implementation follows a FIFO-ordering considering high priority requests
 * to be processed before low priority requests.</p>
 *  
 * @author flangner
 * @version 1.00, 09/01/11
 * @see ProtectedQueue
 */
public class SimpleProtectedQueue<R extends IRequest> implements ProtectedQueue<R> {
    
    /**
     * <p>The queue to buffer the high priority requests before their processing.</p>
     */
    private final Queue<R>              high = new LinkedList<R>();
    
    /**
     * <p>The queue to buffer the low priority requests before their processing.</p>
     */
    private final Queue<R>              low = new LinkedList<R>();
    
    /**
     * <p>The interface to the Overload-Protection algorithm.</p>
     */
    private final OverloadProtection    olp;
    
    /**
     * <p>Initializes an empty queue with Overload-Protection enabled.</p>
     * 
     * @param olp - the interface to the Overload-Protection algorithm.
     */
    public SimpleProtectedQueue(OverloadProtection olp) {
        this.olp = olp;
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.ProtectedQueue#enqueue(R)
     */
    @Override
    public synchronized void enqueue(R request) throws AdmissionRefusedException {
        
        olp.obtainAdmission(request);
        if (request.hasHighPriority()) {
            high.add(request);
        } else {
            low.add(request);
        }
        
        // wake up the stage if necessary
        if ((high.size() == 0 && low.size() == 1) ||
            (high.size() == 1 && low.size() == 0)) {
            notify();
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.ProtectedQueue#take()
     */
    @Override
    public synchronized R take() throws InterruptedException {
        
        if (high.size() == 0 && low.size() == 0) {
            wait();
        }
        
        R result = high.poll();
        return (result == null) ? low.poll() : result;
    }
}
