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
     * Insert the Interval i into the current IntervalVector.<br>
     * Note: This is an optional operation.
     * 
     * @param i
     *            Interval to insert
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
     * Sorted
     * copy
     * zusammenhängend
     * @return
     */
    public abstract List<Interval> serialize();

    // gdw. alle Interval equal sind
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
        if (last != null && interval.start > last.end) {
            Interval gap = new Interval(last.end, interval.start);
            acc.add(gap);
        }

        if (last != null && interval.start <= last.end && interval.version == last.version && interval.id == last.id) {
            // There is no gap between the last and current interval and their version and id match: Merge them!
            Interval merged = new Interval(last.start, interval.end, last.version, last.id);
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
            if (first.start > start) {
                // Pad from the beginning
                Interval pad = new Interval(start, first.start);
                intervals.addFirst(pad);
            } else if (first.start < start) {
                intervals.removeFirst();
                first = new Interval(start, first.end, first.version, first.id);
                intervals.addFirst(first);
            }

            Interval last = intervals.getLast();
            if (last.end < end) {
                // Pad from the end
                Interval pad = new Interval(last.end, end);
                intervals.addLast(pad);
            } else if (last.end > end) {
                intervals.removeLast();
                last = new Interval(last.start, end, last.version, last.id);
                intervals.addLast(last);
            }
        } else {
            Interval empty = new Interval(start, end);
            intervals.add(empty);
        }
    }

}
