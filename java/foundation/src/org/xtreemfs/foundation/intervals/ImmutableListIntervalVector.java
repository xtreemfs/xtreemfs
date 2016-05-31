/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ImmutableListIntervalVector extends IntervalVector {

    final List<Interval> intervals;
    // TODO (jdillmann): Store/Cache maxversion

    public ImmutableListIntervalVector(IntervalVector vector) {
        this.intervals = vector.serialize();
    }

    public ImmutableListIntervalVector(List<Interval> intervals) {
        // FIXME (jdillmann): Sicherstellen, dass keine Lücken existieren
        this.intervals = new ArrayList<Interval>(intervals);
        // Collections.sort(this.intervals);
    }

    @Override
    public IntervalVector getOverlapping(long start, long end) {
        LinkedList<Interval> result = new LinkedList<Interval>();

        for (Interval i : intervals) {
            if (i.end > start && i.start <= end) {
                addInterval(result, i);
            }
        }

        return new ImmutableListIntervalVector(result);
    }

    @Override
    public IntervalVector getSlice(long start, long end) {
        LinkedList<Interval> result = new LinkedList<Interval>();

        for (Interval i : intervals) {
            if (i.end > start && i.start <= end) {
                addInterval(result, i);
            }
        }

        sliceIntervalList(result, start, end);
        return new ImmutableListIntervalVector(result);
    };

    @Override
    public void insert(Interval i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMaxVersion() {
        long max = -1;
        for (Interval i : intervals) {
            if (i.version > max) {
                max = i.version;
            }
        }
        return max;
    }

    @Override
    public boolean isMaxVersionGreaterThen(IntervalVector o) {
        long otherMax = o.getMaxVersion();

        boolean isGreater = false;
        for (Interval i : intervals) {
            if (i.version > otherMax) {
                // Tests if the maxversion of any range of a version vector
                // is greater then that of the passed version vector
                isGreater = true;
            } else if (i.version < otherMax) {
                // The maxversion of all ranges has to be at least greater or equal
                return false;
            }
        }

        return isGreater;
    }

    @Override
    public boolean compareLEQThen(IntervalVector otherVector) {
        List<Interval> other = otherVector.serialize();

        Iterator<Interval> thisIt = intervals.iterator();
        Iterator<Interval> otherIt = other.iterator();
        Interval thisIv = null;
        Interval otherIv = null;
        
        if (thisIt.hasNext()) {
            thisIv = thisIt.next();
        }

        if (otherIt.hasNext()) {
            otherIv = otherIt.next();
        }

        // If both vectors are empty, they are by definition the same.
        if (otherIv == null && thisIv == null) {
            return true;
        }

        // If the vectors do not have the same start, the comparison is undefined.
        if (!(thisIv != null && otherIv != null) || (thisIv.start != otherIv.start)) {
            throw new IllegalArgumentException("IntervalVectors to compare have to be aligend.");
        }

        while (thisIv != null && otherIv != null) {


            if (thisIv.version > otherIv.version) {
                // This vector element version is greater than the other.
                return false;

            } else if (thisIv.version == otherIv.version
                    && !(thisIv.start == otherIv.start && thisIv.end == otherIv.end && thisIv.id == otherIv.id)) {
                // The vector element versions are the same, but the elements are not equal.
                return false;

            }

            if (thisIv.end > otherIv.end) {
                // Advance other vector
                otherIv = null;
            } else if (thisIv.end < otherIv.end) {
                // Advance this vector
                thisIv = null;
            } else {
                // Advance both vectors
                thisIv = null;
                otherIv = null;
            }

            if (thisIv == null && thisIt.hasNext()) {
                thisIv = thisIt.next();
            }

            if (otherIv == null && otherIt.hasNext()) {
                otherIv = otherIt.next();
            }
        }

        if (!(thisIv == null && otherIv == null)) {
            throw new IllegalArgumentException("IntervalVectors to compare have to be aligend.");
        }

        return true;
    }

    @Override
    public List<Interval> serialize() {
        // TODO (jdillmann): Think about not copying, as this will be much faster and I now what to do. And i would
        // document it
        return new ArrayList<Interval>(intervals);
    }
}
