/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation;

import java.util.List;

public interface IntervalVersionTreeInterface {

    /**
     * Insert the Interval i into the current tree.
     * 
     * @param i
     *            Interval to insert
     */
    public void insert(Interval i);

    /**
     * Insert the Interval given as begin, end and version to the current tree.
     * 
     * @param begin
     * @param end
     * @param version
     */
    public void insert(long begin, long end, long version);

    /**
     * Returns all overlapping Intervals
     * 
     * @param i
     *            Interval to check for overlaps. * @return List of Intervals
     */
    public List<Interval> getVersions(Interval i);

    /**
     * Returns all overlapping Intervals
     * 
     * @param begin
     * @param end
     * @return List of Intervals
     */
    public List<Interval> getVersions(long begin, long end);

    /**
     * Interval Objects containing the begin, end and version of an interval.
     */
    public static class Interval {
        long begin;
        long end;
        long version;

        public Interval(long begin, long end) {
            this(begin, end, 0);
        }

        public Interval(long begin, long end, long version) {
            this.begin = begin;
            this.end = end;
            this.version = version;
        }

        // used in testing
        @Override
        public boolean equals(Object other) {
            if (other == null)
                return false;
            if (other == this)
                return true;
            if (!(other instanceof Interval))
                return false;
            Interval interval = (Interval) other;
            return this.begin == interval.begin && this.end == interval.end && this.version == interval.version;
        }

        @Override
        public String toString() {
            return String.format("([%d:%d], %d)", begin, end, version);
        }
    }
}
