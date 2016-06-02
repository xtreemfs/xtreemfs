/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

/**
 * Interval Objects containing the start, end and version of an interval. <br>
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class Interval implements Comparable<Interval> {
    public final static Interval COMPLETE = new Interval(-1, -1);

    public final long            start;
    public final long            end;
    public final long            version;
    public final long            id;

    /**
     * Create an interval from start to end.
     * 
     * @param start
     *            inclusive
     * @param end
     *            exclusive
     */
    // Used for testing / internal
    Interval(long start, long end) {
        this(start, end, -1, -1);
    }

    // Used for testing / internal
    Interval(long start, long end, long version) {
        this(start, end, version, -1);
    }

    /**
     * Create an interval from start to end.
     * 
     * @param start
     *            inclusive
     * @param end
     *            exclusive
     * @param version
     * @param id
     */
    public Interval(long start, long end, long version, long id) {
        if (end <= start && start >= 0) {
            throw new IllegalArgumentException("Intervals must be at least of length 1");
        }

        this.start = start;
        this.end = end;
        this.version = version;
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (other == this)
            return true;
        if (!(other instanceof Interval))
            return false;
        Interval interval = (Interval) other;
        return this.start == interval.start 
                && this.end == interval.end 
                && this.version == interval.version
                && this.id == interval.id;
    }

    @Override
    // FIXME (jdillmann): This doesn't make sense!
    public int compareTo(Interval o) {
        if (start < o.start || end > o.end) {
            // If this intervals range is greater it's versions are greater, too
            // FIXME (jdillmann): What to to on truncates?
            return 1;
        }

        // Java 6 equivalent of Long.compare(this.version, o.version)
        return Long.valueOf(this.version).compareTo(Long.valueOf(o.version));
    }

    @Override
    public String toString() {
        return String.format("([%d:%d], %d, %d)", start, end, version, id);
    }
}
