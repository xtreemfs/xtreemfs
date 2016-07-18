/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.util.Iterator;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.ObjectInterval;

public final class Stripe {
    final long     stripeNo;
    final int      relIdx;

    final long     firstObj;
    final long     lastObj;

    final long     firstObjWithData;
    final long     lastObjWithData;
    final Interval stripeInterval;

    public Stripe(long stripeNo, int relIdx, long firstObj, long lastObj, long firstObjWithData,
            long lastObjWithData, Interval stripeInterval) {
        this.stripeNo = stripeNo;
        this.relIdx = relIdx;
        this.firstObj = firstObj;
        this.lastObj = lastObj;
        this.firstObjWithData = firstObjWithData;
        this.lastObjWithData = lastObjWithData;
        this.stripeInterval = stripeInterval;
    }

    public long getStripeNo() {
        return stripeNo;
    }

    public int getRelIdx() {
        return relIdx;
    }

    public long getFirstObj() {
        return firstObj;
    }

    public long getLastObj() {
        return lastObj;
    }

    public long getFirstObjWithData() {
        return firstObjWithData;
    }

    public long getLastObjWithData() {
        return lastObjWithData;
    }

    public Interval getStripeInterval() {
        return stripeInterval;
    }


    public static Iterable<Stripe> getIterable(final Interval interval, final StripingPolicyImpl sp) {
        return new Iterable<Stripe>() {
            @Override
            public Iterator<Stripe> iterator() {
                return new StripeIterator(interval, sp);
            }
        };
    }

    final static class StripeIterator implements Iterator<Stripe> {
        final StripingPolicyImpl sp;
        final Interval           interval;
        final long               firstObjNo;
        final long               lastObjNo;
        final long               firstStripeNo;
        final long               lastStripeNo;

        int                      nextStripeIdx;

        public StripeIterator(Interval interval, StripingPolicyImpl sp) {
            this.sp = sp;
            this.interval = interval;

            firstObjNo = sp.getObjectNoForOffset(interval.getOpStart());
            lastObjNo = sp.getObjectNoForOffset(interval.getOpEnd() - 1); // interval.end is exclusive

            firstStripeNo = sp.getRow(firstObjNo);
            lastStripeNo = sp.getRow(lastObjNo);

            nextStripeIdx = 0;
        }

        @Override
        public boolean hasNext() {
            return (firstStripeNo + nextStripeIdx <= lastStripeNo);
        }

        @Override
        public Stripe next() {
            long stripeNo = firstStripeNo + nextStripeIdx;

            long firstObj = stripeNo * sp.getWidth();
            long lastObj = (stripeNo + 1) * sp.getWidth() - 1;

            long firstObjWithData = Math.max(firstObj, firstObjNo);
            long lastObjWithData = Math.min(lastObj, lastObjNo);

            long stripeDataStart = Math.max(sp.getObjectStartOffset(firstObjWithData), interval.getOpStart());
            long stripeDataEnd = Math.min(sp.getObjectEndOffset(lastObjWithData) + 1, interval.getOpEnd());

            Interval stripeInterval = new ObjectInterval(stripeDataStart, stripeDataEnd, interval.getVersion(),
                    interval.getId(), interval.getOpStart(), interval.getOpEnd());

            Stripe stripe = new Stripe(stripeNo, nextStripeIdx, firstObj, lastObj, firstObjWithData, lastObjWithData,
                    stripeInterval);
            nextStripeIdx++;

            return stripe;
        }

        @Override
        public void remove() {
            // Not implemented
        }
    }
}