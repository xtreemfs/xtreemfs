/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;

public class ECHelper {
    public static IntervalMsg interval2proto(Interval interval) {
        IntervalMsg.Builder msg = IntervalMsg.newBuilder();
        msg.setStart(interval.getStart())
           .setEnd(interval.getEnd())
           .setVersion(interval.getVersion())
           .setId(interval.getId());

        if (!interval.isOpComplete()) {
            msg.setOpStart(interval.getOpStart());
            msg.setOpEnd(interval.getOpEnd());
        }

        return msg.build();
    }
}
