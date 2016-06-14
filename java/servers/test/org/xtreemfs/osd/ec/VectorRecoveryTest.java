/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.AttachmentInterval;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.test.TestHelper;

public class VectorRecoveryTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    @Test
    public void testCurVectorRecovery() throws Exception {
        AVLTreeIntervalVector iv1 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv2 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv3 = new AVLTreeIntervalVector();

        ObjectInterval interval;
        interval = new ObjectInterval(0, 12, 1, 1);
        iv1.insert(interval);
        iv2.insert(interval);
        iv3.insert(interval);

        interval = new ObjectInterval(3, 9, 2, 2);
        iv2.insert(interval);
        iv3.insert(interval);

        interval = new ObjectInterval(2, 6, 3, 3);
        iv3.insert(interval);

        interval = new ObjectInterval(0, 1, 2, 4);
        iv1.insert(interval);

        LinkedList<Interval> expected = new LinkedList<Interval>();
        expected.add(new AttachmentInterval(0, 1, 2, 4, 1));
        expected.add(new AttachmentInterval(1, 2, 1, 1, 3));
        expected.add(new AttachmentInterval(2, 6, 3, 3, 1));
        expected.add(new AttachmentInterval(6, 9, 2, 2, 2));
        expected.add(new AttachmentInterval(9, 12, 1, 1, 3));

        IntervalVector[] curVectors = new IntervalVector[] { iv1, iv2, iv3 };
        List<AttachmentInterval> result = ECPolicy.recoverCurrentIntervalVector(curVectors, null);

        System.out.println(result);

        assertEquals(expected.size(), result.size());
        for (int i = 0; i < result.size(); i++) {
            // IntervalMsg i1 = expected.get(i);
            // IntervalMsg i2 = result.get(i);
            assertEquals(result.get(i), expected.get(i));
            assertEquals(result.get(i).getAttachment(), expected.get(i).getAttachment());
        }

    }

    // static void clonePrint(PriorityQueue<SweepEvent> prioq) {
    // PriorityQueue<SweepEvent> prioq2 = new PriorityQueue<SweepEvent>();
    //
    // for (SweepEvent e : prioq.toArray(new SweepEvent[0])) {
    // prioq2.add(e);
    // }
    // for (SweepEvent e = prioq2.poll(); e != null; e = prioq2.poll()) {
    // System.out.println(e);
    // }
    // }
    //
//    static class SweepEvent implements Comparable<SweepEvent> {
//        long     position;
//        IntervalMsg interval;
//        int      i;
//
//        public SweepEvent(int i, long position, IntervalMsg interval) {
//            this.i = i;
//            this.position = position;
//            this.interval = interval;
//        }
//
//        boolean isStart() {
//            return position == interval.start;
//        }
//
//        boolean isEnd() {
//            return position == interval.end;
//        }
//
//        @Override
//        public int compareTo(SweepEvent o) {
//            // if (o == null) ?
//
//            long cmp;
//            // The event position is the main criteria
//            cmp = this.position - o.position;
//
//            // If both events happen at the same position, that event which ends first will be handled first.
//            // This implies, that end events are handled before start events.
//            if (cmp == 0) {
//                cmp = this.interval.end - o.interval.end;
//            }
//
//            // If both events end at the same position, the event with the higher version is handled first.
//            if (cmp == 0) {
//                // note: higher versions are considered first
//                // cmp = o.interval.id - this.interval.id;
//                cmp = o.interval.version - this.interval.version;
//            }
//
//            // If both events end at the same point and have the same version, the event which starts second will be
//            // handled first.
//            // If an event A with the same version and end position as event B, starts after B's start, there has to be
//            // a third event C which ends just at A's start and which has been missed in the sequence B is resulting
//            // from.
//            if (cmp == 0) {
//                cmp = o.interval.start - this.interval.start;
//            }
//
//            return Long.signum(cmp);
//        }
//
//        @Override
//        public String toString() {
//            return position + ": " + (isStart() ? "<" : " ") + interval + (isEnd() ? ">" : "");
//        }
//    }


}
