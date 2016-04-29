/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation;

import java.util.List;

public abstract class IntervalVersionTree {

    /**
     * Insert the Interval i into the current tree.
     * 
     * @param i
     *            Interval to insert
     */
    public void insert(Interval i) {
        insert(i.begin, i.end, i.version);
    }

    /**
     * Insert the Interval given as begin (inclusive), end (exclusive) and version to the current tree.
     * 
     * @param begin
     *            inclusive
     * @param end
     *            exclusive
     * @param version
     */
    public abstract void insert(long begin, long end, long version);

    /**
     * Returns all overlapping Intervals. <br>
     * Adjacent intervals with the same version will be merged.
     * 
     * @param i
     *            Interval to check for overlaps.
     * @return List of Intervals
     */
    public List<Interval> getVersions(Interval i) {
        return getVersions(i.begin, i.end);
    }

    /**
     * Returns all overlapping intervals between begin (inclusive) and end (exclusive). <br>
     * Adjacent intervals with the same version will be merged.
     * 
     * @param begin
     *            inclusive
     * @param end
     *            exclusive
     * @return List of Intervals
     */
    public abstract List<Interval> getVersions(long begin, long end);

    /**
     * Returns all intervals stored in the the tree in ascending order. <br>
     * Adjacent intervals with the same version will be merged.
     * 
     * @return List of Intervals
     */
    public abstract List<Interval> serialize();


    /**
     * Interval Objects containing the begin, end and version of an interval.
     */
    public static class Interval {
        public long begin;
        public long end;
        public long version;

        /**
         * Create an interval from begin to end.
         * 
         * @param begin
         *            inclusive
         * @param end
         *            exclusive
         */
        public Interval(long begin, long end) {
            this(begin, end, 0);
        }

        /**
         * Create an interval from begin to end.
         * 
         * @param begin
         *            inclusive
         * @param end
         *            exclusive
         * @param version
         */
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
