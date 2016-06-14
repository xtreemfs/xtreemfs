/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;

/** Interval that wraps a ProtoBuf IntervalMsg */
public class ProtoInterval extends Interval {
    private final IntervalMsg intervalMsg;

    public ProtoInterval(IntervalMsg intervalMsg) {
        this.intervalMsg = intervalMsg;
    }

    @Override
    public long getStart() {
        return intervalMsg.getStart();
    }

    @Override
    public long getEnd() {
        return intervalMsg.getEnd();
    }

    @Override
    public long getVersion() {
        return intervalMsg.getVersion();
    }

    @Override
    public long getId() {
        return intervalMsg.getId();
    }

    @Override
    public long getOpStart() {
        if (intervalMsg.hasOpStart()) {
            return intervalMsg.getOpStart();
        }
        return intervalMsg.getStart();
    }

    @Override
    public long getOpEnd() {
        if (intervalMsg.hasOpEnd()) {
            return intervalMsg.getOpEnd();
        }
        return intervalMsg.getEnd();
    }

    // TODO (jdillmann): hashCode / equals?

}
