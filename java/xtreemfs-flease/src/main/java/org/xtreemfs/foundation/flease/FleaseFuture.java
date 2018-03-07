/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease;

import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.flease.proposer.FleaseListener;

/**
 *
 * @author bjko
 */
public class FleaseFuture implements FleaseListener {

    private volatile Flease result;

    private volatile FleaseException error;

    FleaseFuture() {
        result = null;
    }

    public Flease get() throws FleaseException,InterruptedException {

        synchronized (this) {
            if ((result == null) && (error == null))
                this.wait();

            if (error != null)
                throw error;

            return result;
        }
        
    }
    public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long masterEpochNumber) {
        synchronized (this) {
            result = new Flease(cellId,leaseHolder,leaseTimeout_ms,masterEpochNumber);
            this.notifyAll();
        }
    }

    public void proposalFailed(ASCIIString cellId, Throwable cause) {
        synchronized (this) {
            if (cause instanceof FleaseException) {
                error = (FleaseException) cause;
            } else {
                error = new FleaseException(cause.getMessage(), cause);
            }
            this.notifyAll();
        }
    }

}
