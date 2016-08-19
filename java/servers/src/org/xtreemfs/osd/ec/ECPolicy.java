/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;

public class ECPolicy {
    final static WriteQuorumConfig DEFAULT_WRITE_QUORUM = WriteQuorumConfig.MAX;

    enum WriteQuorumConfig {
        MAX, // requires to write on n devices
        MIN, // requires to write on k devices
        MEAN, // requires to write on k + (m/2) devices
    }

    final private StripingPolicyImpl sp;
    final private int n;
    final private int k;
    final private int qw;
    final private int qr;

    public ECPolicy(StripingPolicyImpl sp) {
        this.sp = sp;
        this.n = sp.getWidth() + sp.getParityWidth();
        this.k = sp.getWidth();
        
        StripingPolicy spMsg = sp.getPolicy();
        if (spMsg.hasEcWriteQuorum()) {
            qw = spMsg.getEcWriteQuorum();
            // FIXME (jdillmann): Check this!
            assert (qw >= k) : "Write Quorum has to be >=k";

            int a = n - qw;
            qr = k + a;

        } else {
            switch (DEFAULT_WRITE_QUORUM) {
            default:
            case MAX:
                this.qw = n;
                this.qr = k;
                break;

            case MEAN:

                this.qw = n;
                this.qr = k;
                break;

            case MIN:
                int a = (int) Math.ceil((n - k) / 2);
                this.qw = k + a;
                this.qr = n - a;
                break;
            }
        }
    }

    public ECPolicy(StripingPolicyImpl sp, WriteQuorumConfig wqConf) {
        this.sp = sp;
        this.n = sp.getWidth() + sp.getParityWidth();
        this.k = sp.getWidth();
        
        switch (wqConf) {
        default:
        case MAX:
            this.qw = n;
            this.qr = k;
            break;

        case MEAN:
            
            this.qw = n;
            this.qr = k;
            break;

        case MIN:
            int a = (int) Math.ceil((n-k) / 2);
            this.qw = k + a;
            this.qr = n - a;
            break;        
        }

    }

    public int getWriteQuorum() {
        return qw;
    }

    public int getReadQuorum() {
        return qr;
    }
    
    public int getDataWidth() {
        return k;
    }

    public StripingPolicyImpl getStripingPolicy() {
        return sp;
    }

    public boolean recoverVector(int responseCount, List<Interval>[] curVectors, List<Interval>[] nextVectors,
            IntervalVector result) throws Exception {
        List<MutableInterval> curResult = recoverVector(curVectors, null);
        List<MutableInterval> nextResult = recoverVector(nextVectors, curResult);

        // FIXME(jdillmann): Throw more specific Exceptions.
        
        for (MutableInterval interval : curResult) {
            if (interval.count < k && !interval.isEmpty()) {
                throw new Exception("There are not enough servers available to recover the data at interval "
                        + interval.toString() + ". Need: " + k + " Is: " + interval.count);
            }

            // FIXME(jdillmann): Transform to ObjectInterval?
            result.insert(interval);
        }

        boolean needsCommit = false;
        for (MutableInterval interval : nextResult) {
            if (interval.isEmpty()) {
                // Ignore
                continue;
            }

            // Partial Write
            if (interval.count < k) {
                // Ignore
                continue;
            }
            // Partial Write
            if (interval.count >= k && (interval.count + (n - responseCount)) < qw) {
                // Ignore
                continue;
            }

            // Complete | Degraded Write
            if (interval.count >= k && interval.count >= qw) {
                // FIXME(jdillmann): Transform to ObjectInterval?
                result.insert(interval);
            }
            // Degraded | Partial Write
            else if (interval.count >= k && (interval.count + (n - responseCount)) >= qw) {
                needsCommit = true;
                // FIXME(jdillmann): Transform to ObjectInterval?
                result.insert(interval);
            }
        }

        return needsCommit;
    }

    static List<MutableInterval> recoverVector(List<Interval>[] vectors, List<MutableInterval> existingResult) {
        LinkedList<MutableInterval> result = new LinkedList<MutableInterval>();

        // Compare every vector with the current result and adapt it if necessary.
        for (int i = 0; i < vectors.length; i++) {
            // Skip missing result vectors.
            if (vectors[i] == null) {
                continue;
            }

            // Iterator over the intervals from the result vector.
            ListIterator<MutableInterval> resIterator = result.listIterator();
            // The active interval in the result vector.
            // If null, the next interval from the result vector will be processed.
            MutableInterval resInterval = null;


            // Iterator over the intervals from the currently processed vector.
            List<Interval> curIntervals = vectors[i];
            Iterator<Interval> curIt = curIntervals.iterator();
            // The active interval in the currently processed vector.
            // If null, the next interval from the current vector will be processed.
            Interval curInterval = null;

            // Iterator over the active interval in the existing/previous result vector.
            Iterator<MutableInterval> exResIt = (existingResult != null) ? existingResult.iterator() : null;
            MutableInterval exResInterval = (existingResult != null && exResIt.hasNext()) ? exResIt.next() : null;

            // The position of the last event and also the end of the last interval in the result vector.
            long lastEnd = 0;

            // Marks the curInterval if it has been found and counted in the existing result vector.
            boolean curIntervalEx = false;

            // Loop as long as there are intervals from the current vector left.
            while (curInterval != null || curIt.hasNext()) {
                // Get the next interval from the current vector if the active one has been processed.
                if (curInterval == null) {
                    curInterval = curIt.next();
                    curIntervalEx = false;

                    // Just ignore incomplete intervals by replacing them with empty ones
                    if (!curInterval.isOpComplete()) {
                        curInterval = ObjectInterval.empty(curInterval);
                    }
                }

                // Get the currently active interval in the result vector.
                if (resInterval == null && resIterator.hasNext()) {
                    resInterval = resIterator.next();
                }

                // The position of the current event. Can be from the result or the current vector.
                long eventEnd = (resInterval == null || curInterval.getEnd() < resInterval.getEnd())
                        ? curInterval.getEnd() : resInterval.getEnd();

                        
                // Handle the aggregation of results to the existing results first
                if (exResInterval != null && exResInterval.getEnd() <= eventEnd) {
                    if (exResInterval.equalsVersionId(curInterval)) {
                        // Add the current interval to the existing result segments counter
                        int newCount = exResInterval.count + 1;
                        exResInterval.count = newCount;
                        // Mark the current interval as already existing
                        curIntervalEx = true;
                    }

                    // Move to the next existing result
                    // Get the currently active interval in the existing result vector.
                    exResInterval = exResIt.hasNext() ? exResIt.next() : null;
                    continue;
                }


                // Compare the current interval to the active result.
                int cmp = (resInterval != null) ? curInterval.compareVersionId(resInterval) : 1;

                if (resInterval == null) { // end of result list
                    assert (cmp > 0);
                    // If the result is null, the end of the result list is reached and the current interval can just be
                    // added to the result

                    int newCount = !curInterval.isEmpty() ? 1 : -1;
                    

                    MutableInterval newInterval;
                    if (curIntervalEx || (exResInterval != null && curInterval.compareVersionId(exResInterval) <= 0)) {
                        // If the current interval is already in the existing result, add only an empty interval
                        Interval empty = ObjectInterval.empty(lastEnd, eventEnd);
                        newInterval = new MutableInterval(empty, -1);
                    } else {
                        newInterval = new MutableInterval(lastEnd, eventEnd, curInterval.getVersion(),
                                curInterval.getId(), newCount);
                    }
                    
                    result.add(newInterval);
                    resIterator = result.listIterator(result.size());
                }

                else if (cmp == 0) { // both are equal
                    // Note: intervals that are in the result can not be in the existingResult
                    assert (!curIntervalEx);

                    // Split the active result at the current position and increase the counter for the active segment.
                    int newCount = resInterval.count >= 0 ? resInterval.count + 1 : -1;

                    // Add a new element at the current position
                    MutableInterval newInterval = new MutableInterval(resInterval.getStart(), eventEnd,
                            curInterval.getVersion(), curInterval.getId(), newCount);

                    if (resInterval.end > eventEnd) {
                        // If the active result ends right of the event, it has to be shrinked but kept.
                        resInterval.start = eventEnd;

                        // Add the new interval before the currently active result.
                        resIterator.previous();
                        resIterator.add(newInterval);
                        resIterator.next();

                    } else {
                        // Otherwise the intervals have to match and the current results count can be overwritten
                        assert (resInterval.end == eventEnd);
                        assert (resInterval.start == lastEnd);
                        resInterval.count = newCount;
                    }

                }

                else if (cmp > 0) { // current is greater
                    // Split the active result at the current position and increase the counter for the active segment.

                    // Add the current interval or a gap (if it has already been counted to existing results)
                    if (curIntervalEx || (exResInterval != null && curInterval.compareVersionId(exResInterval) <= 0)) {
                        // Note: The current interval can only be <= to the active existing interval and > to the active
                        // result interval if the result interval is an empty one.
                        // If the current interval is greater then the result interval, it follows that it overwrote a
                        // previous interval with a smaller version which has to be in the existing result, since only
                        // complete intervals get committed when overlapping with a newer version.
                        // Further it is assured, that only intervals which are greater then the related interval in the
                        // existing result are added.
                        assert (resInterval.version == -1);

                        // Note: An interval with a newer version can never end before the current result, if it is
                        // found in the existing result.
                        // FIXME (jdillmann): Why?
                        assert (resInterval.end >= eventEnd);

                        // Since it is assured, that the result interval is already an empty one and it ends at or right
                        // of the current event, the current interval can be ignored without the danger of creating
                        // loops.

                    } else {
                        // Create the new result interval and add it at the current position.
                        MutableInterval newInterval = new MutableInterval(lastEnd, eventEnd, curInterval.getVersion(),
                                curInterval.getId(), 1);

                        if (resInterval.end > eventEnd) {
                            // If the active result ends right of the event, it has to be shrinked but kept.
                            resInterval.start = eventEnd;

                            // Add the new interval before the currently active result.
                            resIterator.previous();
                            resIterator.add(newInterval);
                            resIterator.next();

                        } else {
                            // Otherwise the intervals have to match and the current result can be overwritten
                            assert (resInterval.end == eventEnd);
                            assert (resInterval.start == lastEnd);
                            resIterator.set(newInterval);
                        }
                    }
                    
                    // TODO (jdillmann): Mark every j < i as outdated / missing
                }

                else { // (cmp < 0) current is less
                       // TODO (jdillmann): Mark this curInterval as missing
                }

                // If the currently active interval ends on this events position,
                // it has to be marked as done (null) to advance to the next interval.
                if (curInterval.getEnd() == eventEnd) {
                    curInterval = null;
                }

                // If the active result interval ends on this events position,
                // advance to the the next result interval.
                if (resInterval != null && resInterval.getEnd() == eventEnd) {
                    resInterval = null;
                }

                // Set the marker of the processed range to the position of this event.
                lastEnd = eventEnd;

            }
        }

        // Merge subsequent intervals that equal in version, id and count.
        mergeResults(result);
        if (existingResult != null) {
            mergeResults(existingResult);
        }

        return result;
    }

    static void mergeResults(List<MutableInterval> result) {
        ListIterator<MutableInterval> resultIt = result.listIterator();
        MutableInterval prev = null;
        while (resultIt.hasNext()) {
            MutableInterval current = resultIt.next();
            if (prev != null && prev.equalsVersionId(current) && prev.count == current.count) {
                prev.end = current.end;
                resultIt.remove();
            } else {
                prev = current;
            }
        }
    }


    static boolean calculateIntervalsToCommit(List<Interval> commitIntervals, Interval reqInterval,
            List<Interval> curVecIntervals, List<Interval> nextVecIntervals, List<Interval> toCommitAcc)
            throws IOException {
        return calculateIntervalsToCommit(commitIntervals, reqInterval, curVecIntervals, nextVecIntervals,
                toCommitAcc, null);
    }

    /**
     * Checks for each interval in commitIntervals if it is present in the curVecIntervals or the nextVecIntervals.<br>
     * If it is present in nextVecIntervals, it is added to the toCommitAcc accumulator.<br>
     * If it isn't present in neither, false is returned immediately.<br>
     * If it's version is lower then an overlapping one from curVecIntervals, an exception is thrown.
     * 
     * @param commitIntervals
     *            List of intervals to commit. Not necessarily over the range of the whole file.
     * @param reqInterval
     *            The interval currently processed. Every interval in nextVecIntervals with the same version and op id
     *            will be ignored (neither committed or aborted). May be null.
     * @param curVecIntervals
     *            List of intervals from the currently stored data. Maybe sliced to the commitIntervals range.
     * @param nextVecIntervals
     *            List of intervals from the next buffer. Maybe sliced to the commitIntervals range.
     * @param toCommitAcc
     *            Accumulator used to return the intervals to be committed from the next buffer.
     * @param missingAcc
     *            Accumulator used to return the intervals that are missing.
     * @return true if an interval from commitIntervals can not be found. false otherwise.
     * @throws IOException
     *             if an interval from commitIntervals contains a lower version version, then an overlapping interval
     *             from curVecIntervals.
     */
    static boolean calculateIntervalsToCommit(List<Interval> commitIntervals, Interval reqInterval,
            List<Interval> curVecIntervals, List<Interval> nextVecIntervals, List<Interval> toCommitAcc,
            List<Interval> missingAcc) throws IOException {
        assert (!commitIntervals.isEmpty());

        Interval emptyInterval = ObjectInterval.empty(commitIntervals.get(0).getOpStart(),
                commitIntervals.get(commitIntervals.size() - 1).getOpEnd());

        Iterator<Interval> curIt = curVecIntervals.iterator();
        Iterator<Interval> nextIt = nextVecIntervals.iterator();

        Interval curInterval = curIt.hasNext() ? curIt.next() : emptyInterval;
        Interval nextInterval = nextIt.hasNext() ? nextIt.next() : emptyInterval;


        // Check for every commit interval if it is available.
        for (Interval commitInterval : commitIntervals) {
            // Advance to the next interval that could be a possible match
            // or set an empty interval as a placeholder
            while (curInterval.getEnd() <= commitInterval.getStart() && curIt.hasNext()) {
                curInterval = curIt.next();
            }
            if (curInterval.getEnd() <= commitInterval.getStart()) {
                curInterval = emptyInterval;
            }

            // Advance to the next interval that could be a possible match
            // or set an empty interval as a placeholder
            while (nextInterval.getEnd() <= commitInterval.getStart() && nextIt.hasNext()) {
                nextInterval = nextIt.next();
            }
            if (nextInterval.getEnd() <= commitInterval.getStart()) {
                nextInterval = emptyInterval;
            }

            // Check if the interval exists in the current vector.
            // It could be, that the interval in the current vector is larger then the current one, because it has
            // not been split yet.
            // req:  |--1--|-2-| or |-2-|--1--| or |-1-|-2-|-1-|
            // cur:  |----1----|    |----1----|    |-----1-----|
            // next: |     |-2-|    |-2-|     |    |   |-2-|   |
            // It is obvious, that intervals from next must have matching start/end positions also.

            if (commitInterval.equalsVersionId(curInterval)) {
                // If the version and the id match, they have to overlap
                assert (commitInterval.overlaps(curInterval));
                
                // if (commitInterval.getStart() != curInterval.getStart() || commitInterval.getEnd() !=
                // curInterval.getEnd()) {
                // // During reconstruction partial intervals can end up in the curVector if the reconstruction failed
                // // It is required to start the reconstruction again
                // if (missingAcc != null) {
                // missingAcc.add(commitInterval);
                // } else {
                // return true;
                // }
                // }

            } else if (!commitInterval.isEmpty()) {
                if (commitInterval.overlaps(curInterval) && commitInterval.getVersion() < curInterval.getVersion()) {
                    // FAILED (should never happen)
                    // TODO (jdillmann): Log with better message.
                    throw new XtreemFSException("request interval is older then the current interval");
                }

                if (commitInterval.equals(nextInterval)) {
                    // COMMIT nextInterval
                    toCommitAcc.add(nextInterval);
                } else {
                    // FAILED (go into recovery)
                    if (missingAcc != null) {
                        missingAcc.add(commitInterval);
                    } else {
                        return true;
                    }
                }
            }
        }


        return (missingAcc != null && !missingAcc.isEmpty());
    }

    @Override
    public String toString() {
        return String.format("ECPolicy [n=%s, k=%s, m=%s, qw=%s, qr=%s]", n, k, (n - k), qw, qr);
    }

    /**
     * The MutableInterval allows the modification of the intervals members. It is used solely for the vector recovery
     */
    static class MutableInterval extends Interval {
        long start;
        long end;
        long version;
        long id;
        int  count;

        public MutableInterval(Interval interval, int count) {
            this(interval.getStart(), interval.getEnd(), interval.getVersion(), interval.getId(), count);
        }

        public MutableInterval(long start, long end, long version, long id, int count) {
            this.start = start;
            this.end = end;
            this.version = version;
            this.id = id;
            this.count = count;
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
        public Object getAttachment() {
            return count;
        }

        @Override
        public String toString() {
            return String.format("([%d:%d], %d, %d, '%d')", getStart(), getEnd(), getVersion(), getId(), count);
        }

        @Override
        protected MutableInterval clone() {
            return new MutableInterval(start, end, version, id, count);
        }
    }
}
