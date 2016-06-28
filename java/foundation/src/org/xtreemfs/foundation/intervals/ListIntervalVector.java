/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ListIntervalVector extends IntervalVector {

    final List<Interval> intervals;
    // TODO (jdillmann): Store/Cache maxversion

    public ListIntervalVector(IntervalVector vector) {
        this.intervals = vector.serialize();
    }

    /**
     * Initialize an empty ListIntervalVector, to which sorted, gapless and non overlapping intervals can be appended
     * with {@link #append(Interval)}.
     */
    public ListIntervalVector() {
        this.intervals = new ArrayList<Interval>();
    }

    /**
     * Initialize an empty ListIntervalVector, to which sorted, gapless and non overlapping intervals can be appended
     * with {@link #append(Interval)}.
     */
    public ListIntervalVector(int initialCapacity) {
        this.intervals = new ArrayList<Interval>(initialCapacity);
    }

    /**
     * Initialize a ListIntervalVector from the interval list. <br>
     * Note: The interval list has to be sorted and may not contain any gaps or overlaps.
     * 
     * @param intervals
     */
    public ListIntervalVector(List<Interval> intervals) {
        this.intervals = intervals;
    }

    @Override
    public List<Interval> getOverlapping(long start, long end) {
        LinkedList<Interval> result = new LinkedList<Interval>();

        for (Interval i : intervals) {
            if (i.overlaps(start, end)) {
                addInterval(result, i);
            }
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public List<Interval> getSlice(long start, long end) {
        LinkedList<Interval> result = new LinkedList<Interval>();

        for (Interval i : intervals) {
            if (i.overlaps(start, end)) {
                addInterval(result, i);
            }
        }

        sliceIntervalList(result, start, end);
        return Collections.unmodifiableList(result);
    };

    @Override
    public void insert(Interval i) {
        throw new UnsupportedOperationException();
    }

    /**
     * Append the interval to this vector if it does not overlap with any segment.<br>
     * Note: Gaps will be filled.
     * 
     * @param interval
     *            to append.
     * @throws IllegalArgumentException
     *             if the interval overlaps
     */
    public void append(Interval interval) {
        int n = intervals.size();
        if (n == 0) {
            intervals.add(interval);
            return;
        }

        Interval last = intervals.get(n - 1);
        if (last.getEnd() > interval.getStart()) {
            throw new IllegalAccessError("Interval to append has to start after the last interval in this vector.");
        }

        if (last.getEnd() < interval.getStart()) {
            // add gap interval
            ObjectInterval gap = ObjectInterval.empty(last.getEnd(), interval.getStart());
            intervals.add(gap);
        }

        intervals.add(interval);
    }

    @Override
    public long getMaxVersion() {
        long max = -1;
        for (Interval i : intervals) {
            if (i.getVersion() > max) {
                max = i.getVersion();
            }
        }
        return max;
    }

    @Override
    public List<Interval> serialize() {
        return Collections.unmodifiableList(intervals);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Interval interval : intervals) {
            sb.append(interval.toString()).append(" ");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
