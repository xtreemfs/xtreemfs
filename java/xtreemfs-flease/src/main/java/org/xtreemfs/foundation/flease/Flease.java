/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;

/**
 * Object represents a lease.
 * @author bjko
 */
public class Flease {

    public static final Flease EMPTY_LEASE = new Flease(null,null, 0,FleaseMessage.IGNORE_MASTER_EPOCH);

    private final ASCIIString leaseHolder;
    private final long leaseTimeout_ms;
    private final ASCIIString cellId;
    private final long masterEpochNumber;


    public Flease(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long masterEpochNumber) {
        this.cellId = cellId;
        this.leaseHolder = leaseHolder;
        this.leaseTimeout_ms = leaseTimeout_ms;
        this.masterEpochNumber = masterEpochNumber;
    }

    /**
     * @return the leaseHolder
     */
    public ASCIIString getLeaseHolder() {
        return leaseHolder;
    }

    /**
     * @return the leaseTimeout_ms
     */
    public long getLeaseTimeout_ms() {
        return leaseTimeout_ms;
    }

    public boolean isValid() {
        return (TimeSync.getGlobalTime() < leaseTimeout_ms);
    }

    public boolean isEmptyLease() {
        return leaseTimeout_ms == 0;
    }

    public boolean equals(Object other) {
        try {
            Flease o = (Flease) other;

            boolean sameTo = o.leaseTimeout_ms == this.leaseTimeout_ms;
            return isSameLeaseHolder(o) && sameTo;

        } catch (ClassCastException ex) {
            return false;
        }
    }

    public boolean isSameLeaseHolder(Flease o) {
        return (o.leaseHolder == this.leaseHolder) ||
                    (o.leaseHolder != null) && (this.leaseHolder != null) && (o.leaseHolder.equals(this.leaseHolder));
    }

    /**
     * @return the cellId
     */
    ASCIIString getCellId() {
        return cellId;
    }

    public String toString() {
        return (cellId == null ? "" : cellId.toString())+": "+
                (leaseHolder == null ? "null" : leaseHolder.toString())+"/"+
                leaseTimeout_ms;
    }

    /**
     * @return the masterEpochNumber
     */
    public long getMasterEpochNumber() {
        return masterEpochNumber;
    }
}
