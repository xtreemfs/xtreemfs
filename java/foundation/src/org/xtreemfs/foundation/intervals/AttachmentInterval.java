/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

/**
 * AttachmentIntervals store an attachment besides an interval.
 */
public class AttachmentInterval extends Interval {
    private final Interval interval;
    private final Object   attachment;

    public AttachmentInterval(Interval interval, Object attachment) {
        this.interval = interval;
        this.attachment = attachment;
    }

    public AttachmentInterval(long start, long end, long version, long id, Object attachment) {
        this.interval = new ObjectInterval(start, end, version, id);
        this.attachment = attachment;
    }

    @Override
    public long getStart() {
        return interval.getStart();
    }

    @Override
    public long getEnd() {
        return interval.getEnd();
    }

    @Override
    public long getVersion() {
        return interval.getVersion();
    }

    @Override
    public long getId() {
        return interval.getId();
    }

    @Override
    public Object getAttachment() {
        return attachment;
    }

    public Interval getInterval() {
        return interval;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    // TODO (jdillmann): hashCode ?
}