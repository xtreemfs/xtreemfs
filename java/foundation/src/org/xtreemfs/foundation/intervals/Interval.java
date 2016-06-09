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
public class Interval {
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

    /**
     * Special Interval subclasses could have an attachment.
     * 
     * @return the attachment if existent or null
     */
    public Object getAttachment() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        // if (getClass() != obj.getClass())
        // return false;

        if (!(obj instanceof Interval))
            return false;

        Interval other = (Interval) obj;
        if (end != other.end)
            return false;
        if (id != other.id)
            return false;
        if (start != other.start)
            return false;
        if (version != other.version)
            return false;
        return true;
    }

    public boolean equalsVersionId(Interval o) {
        if (o == null) {
            return false;
        }
        return (version == o.version && id == o.id);
    }

    @Override
    public String toString() {
        return String.format("([%d:%d], %d, %d)", start, end, version, id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (end ^ (end >>> 32));
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (int) (start ^ (start >>> 32));
        result = prime * result + (int) (version ^ (version >>> 32));
        return result;
    }

    /**
     * Interval implementation that allows to add an attachment.<br>
     * Attachments are ignored when checking on equality or generating hashCodes.<br>
     * TODO (jdillmann): Maybe merge to Interval
     */
    public static class IntervalWithAttachment extends Interval {
        final Object attachment;

        public IntervalWithAttachment(long start, long end, long version, long id, Object attachment) {
            super(start, end, version, id);
            this.attachment = attachment;
        }

        @Override
        public Object getAttachment() {
            return attachment;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }
}
