/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Interval;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalOrBuilder;

public class ECHelper {
    public static Interval interval2proto(org.xtreemfs.foundation.intervals.Interval interval) {
        return Interval.newBuilder()
                .setStart(interval.start)
                .setEnd(interval.end)
                .setVersion(interval.version)
                .setId(interval.id)
                .build();
    }

    public static org.xtreemfs.foundation.intervals.Interval proto2interval(IntervalOrBuilder msg) {
        return new org.xtreemfs.foundation.intervals.Interval(msg.getStart(), msg.getEnd(), msg.getVersion(),
                msg.getId());
    }
}
