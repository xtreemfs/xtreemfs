/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

import java.util.Iterator;
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
    public abstract List<Interval> getOverlapping(long start, long end);

    public abstract List<Interval> getSlice(long start, long end);

    public boolean contains(Interval interval) {
        List<Interval> overlapping = getSlice(interval.getStart(), interval.getEnd());
        if (overlapping.size() != 1) {
            return false;
        }

        // equals: start, end, version, id
        // Note: not opStart/opEnd
        return overlapping.get(0).equals(interval);
    }

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
     * @param o
     *            IntervalVector to compare with
     * @return result of test
     */
    public boolean isMaxVersionGreaterThen(IntervalVector o) {
        long otherMax = o.getMaxVersion();

        boolean isGreater = false;
        for (Interval i : o.serialize()) {
            if (i.getVersion() > otherMax) {
                // Tests if the maxversion of any range of a version vector
                // is greater then that of the passed version vector
                isGreater = true;
            } else if (i.getVersion() < otherMax) {
                // The maxversion of all ranges has to be at least greater or equal
                return false;
            }
        }

        return isGreater;
    }

    /**
     * Nur definiert auf gleichen längen!
     */
    public boolean compareLEQThen(IntervalVector o) {
        Iterator<Interval> thisIt = serialize().iterator();
        Iterator<Interval> otherIt = o.serialize().iterator();
        Interval thisIv = null;
        Interval otherIv = null;

        if (thisIt.hasNext()) {
            thisIv = thisIt.next();
        }

        if (otherIt.hasNext()) {
            otherIv = otherIt.next();
        }

        // If both vectors are empty, they are by definition the same.
        if (otherIv == null && thisIv == null) {
            return true;
        }

        // If the vectors do not have the same start, the comparison is undefined.
        if (!(thisIv != null && otherIv != null) || (thisIv.getStart() != otherIv.getStart())) {
            // FIXME (jdillmann): Serialize does now always start from 0, so this case can not happen
            throw new IllegalArgumentException("IntervalVectors to compare have to be aligend.");
        }

        while (thisIv != null && otherIv != null) {

            if (thisIv.getVersion() > otherIv.getVersion()) {
                // This vector element version is greater than the other.
                return false;

            } else if (thisIv.getVersion() == otherIv.getVersion() && !(thisIv.getStart() == otherIv.getStart()
                    && thisIv.getEnd() == otherIv.getEnd() && thisIv.getId() == otherIv.getId())) {
                // The vector element versions are the same, but the elements are not equal.
                return false;
            }

            if (thisIv.getEnd() > otherIv.getEnd()) {
                // Advance other vector
                otherIv = null;
            } else if (thisIv.getEnd() < otherIv.getEnd()) {
                // Advance this vector
                thisIv = null;
            } else {
                // Advance both vectors
                thisIv = null;
                otherIv = null;
            }

            if (thisIv == null && thisIt.hasNext()) {
                thisIv = thisIt.next();
            }

            if (otherIv == null && otherIt.hasNext()) {
                otherIv = otherIt.next();
            }
        }

        if (!(thisIv == null && otherIv == null)) {
            throw new IllegalArgumentException("IntervalVectors to compare have to be aligend.");
        }

        return true;
    }

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
            ObjectInterval gap = ObjectInterval.empty(last.getEnd(), interval.getStart());
            acc.add(gap);
        }

        if (last != null && interval.getStart() <= last.getEnd() && interval.equalsVersionId(last)) {
            // There is no gap between the last and current interval and their version and id match: Merge them!
            ObjectInterval merged;
            if (!interval.isEmpty()) {
                assert (last.getOpStart() == interval.getOpStart() && last.getOpEnd() == interval.getOpEnd());
                merged = new ObjectInterval(last.getStart(), interval.getEnd(), last.getVersion(), last.getId(),
                        last.getOpStart(), last.getOpEnd());
            } else {
                merged = ObjectInterval.empty(last.getStart(), interval.getEnd());
            }
            acc.removeLast();
            acc.add(merged);

        } else {
            acc.add(interval);
        }
    }

    static void trimEmpty(LinkedList<Interval> intervals) {
        // Remove empty intervals from the beginning
        while (!intervals.isEmpty() && intervals.peekFirst().isEmpty()) {
            intervals.removeFirst();
        }

        // Remove empty intervals from the end
        while (!intervals.isEmpty() && intervals.peekLast().isEmpty()) {
            intervals.removeLast();
        }
    }

    // TODO (jdillmann): Doc
    static void sliceIntervalList(LinkedList<Interval> intervals, long start, long end) {
        if (intervals.size() > 0) {
            Interval first = intervals.getFirst();
            if (first.getStart() > start) {
                // Pad from the beginning
                ObjectInterval pad = ObjectInterval.empty(start, first.getStart());
                intervals.addFirst(pad);
            } else if (first.getStart() < start) {
                intervals.removeFirst();
                first = new ObjectInterval(start, first.getEnd(), first.getVersion(), first.getId(), first.getOpStart(),
                        first.getOpEnd());
                intervals.addFirst(first);
            }

            Interval last = intervals.getLast();
            if (last.getEnd() < end) {
                // Pad from the end
                ObjectInterval pad = ObjectInterval.empty(last.getEnd(), end);
                intervals.addLast(pad);
            } else if (last.getEnd() > end) {
                intervals.removeLast();
                last = new ObjectInterval(last.getStart(), end, last.getVersion(), last.getId(), last.getOpStart(),
                        last.getOpEnd());
                intervals.addLast(last);
            }
        } else if ((end - start) > 0) {
            ObjectInterval empty = ObjectInterval.empty(start, end);
            intervals.add(empty);
        }
    }

}
