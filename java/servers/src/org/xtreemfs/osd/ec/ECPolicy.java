/*
 * Copyright (c) 2016 by Johannes Dillmann,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.Interval.IntervalWithAttachment;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;

/**
 * This class is responsible for the communication between the OSDs
 */
public class ECPolicy {

    public static interface ExecuteViewResetCallback {
        public void finished(final List<Interval> curVersions);
        public void failed(ErrorResponse error);
    }

    void executeViewReset(final FileCredentials credentials, final List<Interval> curVersionsLocal,
            final List<Interval> nextVersionsLocal, final ExecuteViewResetCallback callback) {

    }

    static class SweepEvent implements Comparable<SweepEvent> {
        final Interval interval;
        final int      i;

        public SweepEvent(Interval interval, int i) {
            this.interval = interval;
            this.i = i;
        }

        @Override
        public int compareTo(SweepEvent o) {
            // The interval's end is the main criteria. Intervals which end first, will be handled first.
            long cmp = this.interval.end - o.interval.end;

            // If both events end at the same position, the event with the higher version is handled first.
            if (cmp == 0) {
                // note: higher versions are considered first
                // cmp = o.interval.id - this.interval.id;
                cmp = o.interval.version - this.interval.version;
            }

            // TODO (jdillmann): Maybe check on id. This will be relevant for the nextVector where only the interval
            // with the greatest id should be valid.

            return Long.signum(cmp);
        }

        @Override
        public String toString() {
            return interval.toString();
        }
    }


    public static List<Interval> recoverCurrentIntervalVector(IntervalVector[] curVectors,
            IntervalVector[] nextVectors) {

        PriorityQueue<SweepEvent> prioq = new PriorityQueue<SweepEvent>(curVectors.length);
        // Queue<Long> maxQueue = new ArrayDeque<Long>(curVectors.length);
        
        
        Iterator<Interval>[] intervalIts = new Iterator[curVectors.length];
        for (int i = 0; i < curVectors.length; i++) {
            intervalIts[i] = curVectors[i].serialize().iterator();

            // Add the first element
            if (intervalIts[i].hasNext()) {
                Interval interval = intervalIts[i].next();
                prioq.offer(new SweepEvent(interval, i));
            }
        }

        LinkedList<Interval> result = new LinkedList<Interval>();
        long lastEnd = 0;

        // As long as there are intervals left handle them according to their order
        while (!prioq.isEmpty()) {
            final SweepEvent event = prioq.poll();
            final int i = event.i;
            final Interval interval = event.interval;
            
            // If this intervals end is less or equal to an already handled range, it can be ignored, because it is
            // guaranteed, that it has already been handled and counted.
            if (interval.end > lastEnd) {
                int count = 1;

                // if (last != null && interval.start < last.end) {
                // // not enough!
                // System.out.println("will drop");
                // }

                // Compare the current interval to every interval currently active in the sweepline state.
                for (SweepEvent otherEvent : prioq) {
                    Interval otherInterval = otherEvent.interval;

                    // If the current interval's version is less then another's, the current interval can be ignored.
                    if (interval.version < otherInterval.version) {
                        // System.out.println("drop: " + event);
                        count = -1;
                        break;
                    }

                    // If the other interval has the same version and the same id count it.
                    if (interval.equalsVersionId(otherInterval)) {
                        count++;
                    }
                }
                
                // If the current interval's version has been greater or equal to the others, the count variable
                // contains is positive and the current range can be added to the result.
                if (count > 0) {
                    IntervalWithAttachment resultInterval = new IntervalWithAttachment(lastEnd, interval.end,
                            interval.version, interval.id, count);
                    lastEnd = interval.end;
                    result.add(resultInterval);

                    // System.out.println("res: " + interval + " => " + resultInterval);
                }

            }
            // else {
            // // Ignore this interval, as its range has already been handled.
            // System.out.println("ign:" + event);
            // }
            
            // If there are intervals left in the current interval vector add the next to the sweepline state.
            if (intervalIts[i].hasNext()) {
                Interval next = intervalIts[i].next();
                prioq.offer(new SweepEvent(next, i));
            }
        }

        return result;
    }
        

}
