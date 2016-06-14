/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsgOrBuilder;

public class ECHelper {
    public static IntervalMsg interval2proto(Interval interval) {
        return IntervalMsg.newBuilder()
                .setStart(interval.getStart())
                .setEnd(interval.getEnd())
                .setVersion(interval.getVersion())
                .setId(interval.getId())
                .build();
    }

    public static ObjectInterval proto2interval(IntervalMsgOrBuilder msg) {
        return new ObjectInterval(msg.getStart(), msg.getEnd(), msg.getVersion(),
                msg.getId());
    }
}
