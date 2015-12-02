/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.rwre;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.RedundantFileState;
import org.xtreemfs.osd.rwre.RWReplicationStage.GetReplicatedFileStateCallback;

/** This class is used to get an immutable and simple ReplicatedFileState that can be returned to callers. */
public class ReplicatedFileStateSimple {

    /** Future object of a ReplicatedFileStateSimple */
    public static class ReplicatedFileStateSimpleFuture implements GetReplicatedFileStateCallback {
        ReplicatedFileStateSimple state = null;
        boolean                   available = false;

        public ReplicatedFileStateSimpleFuture(RWReplicationStage rwrStage, String fileId) {
            rwrStage.getReplicatedFileState(fileId, this, null);
        }

        @Override
        synchronized public void getReplicatedFileStateComplete(ReplicatedFileStateSimple state) {
            this.state = state;
            available = true;
            notifyAll();
        }

        @Override
        synchronized public void failed(ErrorResponse ex) {
            available = true;
            notifyAll();
        };

        synchronized public ReplicatedFileStateSimple get() throws InterruptedException {
            while (!available) {
                wait();
            }
            return state;
        }
    }

    private final ReplicatedFileState state;

    public ReplicatedFileStateSimple(ReplicatedFileState state) {
        this.state = state;
    }

    /**
     * Returns primary OSD UUID for the file (if OSD is primary or backup) or null (if the lease is not valid).
     */
    public String getPrimary() {
        if (state.getLease() != null && !state.getLease().isEmptyLease() && state.getLease().isValid()) {
            return state.getLease().getLeaseHolder().toString();
        }

        return null;
    }

    public RedundantFileState.LocalState getState() {
        return state.getState();
    }

    public boolean isInvalidated() {
        return state.isInvalidated();
    }

    public boolean isInvalidatedReset() {
        return state.isInvalidatedReset();
    }
}
