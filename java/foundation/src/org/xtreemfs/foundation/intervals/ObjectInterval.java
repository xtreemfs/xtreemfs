/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

/**
 * ObjectInterval Objects containing the start, end, version, id and opStart, opEnd of an interval.
 */
public class ObjectInterval extends Interval {
    private final long start;
    private final long end;
    private final long version;
    private final long id;
    private final long opStart;
    private final long opEnd;

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
        this(start, end, version, id, start, end);
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
     * @param opStart
     *            inclusive
     * @param opEnd
     *            exclusive
     */
    public ObjectInterval(long start, long end, long version, long id, long opStart, long opEnd) {
        if (end <= start && start >= 0) {
            throw new IllegalArgumentException("Intervals must be at least of length 1");
        }

        if (opStart > start || opEnd < end) {
            throw new IllegalArgumentException("Operation range must be at least the intervals range");
        }

        this.start = start;
        this.end = end;
        this.version = version;
        this.id = id;
        this.opStart = opStart;
        this.opEnd = opEnd;
    }

    // Used for testing / internal
    ObjectInterval(long start, long end, long version) {
        this(start, end, version, version);
    }

    public static ObjectInterval empty(long start, long end) {
        return new ObjectInterval(start, end, -1, -1, start, end);
    }

    public static ObjectInterval empty(long start, long end, long opStart, long opEnd) {
        return new ObjectInterval(start, end, -1, -1, opStart, opEnd);
    }

    public static ObjectInterval empty(Interval interval) {
        return new ObjectInterval(interval.getStart(), interval.getEnd(), -1, -1, interval.getOpStart(),
                interval.getOpEnd());
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
    public long getOpStart() {
        return opStart;
    }

    @Override
    public long getOpEnd() {
        return opEnd;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (getEnd() ^ (getEnd() >>> 32));
        result = prime * result + (int) (getId() ^ (getId() >>> 32));
        result = prime * result + (int) (getStart() ^ (getStart() >>> 32));
        result = prime * result + (int) (getVersion() ^ (getVersion() >>> 32));
        result = prime * result + (int) (getOpStart() ^ (getOpStart() >>> 32));
        result = prime * result + (int) (getOpEnd() ^ (getOpEnd() >>> 32));
        return result;
    }
}
