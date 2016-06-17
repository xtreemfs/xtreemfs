/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

/**
 * ObjectInterval Objects containing the start, end and version of an interval.
 */
public class ObjectInterval extends Interval {
    private final long            start;
    private final long            end;
    private final long            version;
    private final long            id;

    /**
     * Create an interval from start to end.
     * 
     * @param start
     *            inclusive
     * @param end
     *            exclusive
     */
    public ObjectInterval(long start, long end) {
        this(start, end, -1, -1);
    }

    // Used for testing / internal
    ObjectInterval(long start, long end, long version) {
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
    public ObjectInterval(long start, long end, long version, long id) {
        if (end <= start && start >= 0) {
            throw new IllegalArgumentException("Intervals must be at least of length 1");
        }

        this.start = start;
        this.end = end;
        this.version = version;
        this.id = id;
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getEnd() {
        return end;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (getEnd() ^ (getEnd() >>> 32));
        result = prime * result + (int) (getId() ^ (getId() >>> 32));
        result = prime * result + (int) (getStart() ^ (getStart() >>> 32));
        result = prime * result + (int) (getVersion() ^ (getVersion() >>> 32));
        return result;
    }
}
