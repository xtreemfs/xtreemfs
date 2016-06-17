/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

import java.util.LinkedList;
import java.util.List;

// FIXME (jdillmann): Doc
public abstract class IntervalVector {

    /**
     * Returns all overlapping intervals between begin (inclusive) and end (exclusive).
     * 
     * @param start
     *            inclusive
     * @param end
     *            exclusive
     * @return IntervalVector of overlapping Intervals
     */
    public abstract IntervalVector getOverlapping(long start, long end);

    public abstract IntervalVector getSlice(long start, long end);

    /**
     * Insert the ObjectInterval i into the current IntervalVector.<br>
     * Note: This is an optional operation.
     * 
     * @param i
     *            ObjectInterval to insert
     * 
     * @throws UnsupportedOperationException
     *             if the operation is not supported
     */
    public abstract void insert(Interval i);

    /**
     * Returns the highest version in the given IntervalVector.
     * 
     * @return max version
     */
    public abstract long getMaxVersion();

    /**
     * Tests if the max version of any interval of this vector is greater then that of the passed vector v
     * and if the max version of all intervals of this vector is at least greater or equal then that
     * of the passed vector v.
     * 
     * @param v
     *            IntervalVector to compare with
     * @return result of test
     */
    public abstract boolean isMaxVersionGreaterThen(IntervalVector v);

    /**
     * Nur definiert auf gleichen längen!
     */
    public abstract boolean compareLEQThen(IntervalVector o);

    /**
     * Sorted, zusammenhängend, von 0 startend
     * 
     * Note: The returned List will be immutable, but not necessarily thread safe.
     * 
     * @return
     */
    public abstract List<Interval> serialize();

    // gdw. alle ObjectInterval equal sind
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof IntervalVector))
            return false;

        IntervalVector other = (IntervalVector) obj;

        // Since both IntervalVectors have to have the same elements, as they are serialized and compacted, we can
        // compare them pairwise
        return this.serialize().equals(other.serialize());
    }

    /**
     * Add the interval to the list. If the intervals lines up with the last interval in the list, and their version and
     * id matches, they are merged. Gaps will be filled.
     * 
     * @param acc
     *            list of intervals
     * @param i
     *            interval to add
     */
    static void addInterval(LinkedList<Interval> acc, Interval interval) {
        Interval last = acc.peekLast();

        // Fill the gaps
        if (last != null && interval.getStart() > last.getEnd()) {
            ObjectInterval gap = new ObjectInterval(last.getEnd(), interval.getStart());
            acc.add(gap);
        }

        if (last != null && interval.getStart() <= last.getEnd() && interval.getVersion() == last.getVersion() && interval.getId() == last.getId()) {
            // There is no gap between the last and current interval and their version and id match: Merge them!
            ObjectInterval merged = new ObjectInterval(last.getStart(), interval.getEnd(), last.getVersion(), last.getId());
            acc.removeLast();
            acc.add(merged);
        } else {
            acc.add(interval);
        }
    }

    // TODO (jdillmann): Doc
    static void sliceIntervalList(LinkedList<Interval> intervals, long start, long end) {
        if (intervals.size() > 0) {
            Interval first = intervals.getFirst();
            if (first.getStart() > start) {
                // Pad from the beginning
                ObjectInterval pad = new ObjectInterval(start, first.getStart());
                intervals.addFirst(pad);
            } else if (first.getStart() < start) {
                intervals.removeFirst();
                first = new ObjectInterval(start, first.getEnd(), first.getVersion(), first.getId());
                intervals.addFirst(first);
            }

            Interval last = intervals.getLast();
            if (last.getEnd() < end) {
                // Pad from the end
                ObjectInterval pad = new ObjectInterval(last.getEnd(), end);
                intervals.addLast(pad);
            } else if (last.getEnd() > end) {
                intervals.removeLast();
                last = new ObjectInterval(last.getStart(), end, last.getVersion(), last.getId());
                intervals.addLast(last);
            }
        } else {
            ObjectInterval empty = new ObjectInterval(start, end);
            intervals.add(empty);
        }
    }

}
