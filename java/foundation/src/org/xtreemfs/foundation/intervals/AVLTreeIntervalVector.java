/*
 * Copyright (c) 2015 by Jan Fajerski, Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

import java.util.LinkedList;
import java.util.List;

public class AVLTreeIntervalVector extends IntervalVector {

    IntervalNode root;
    long         start;
    long         end;
    long         maxVersion;

    /** Number of intervals (approximate) that have been overwritten by subsequent inserts. */
    int          overwrites = 0;

    public AVLTreeIntervalVector() {
        this.root = null;
        this.start = -1;
        this.end = 0;
        this.maxVersion = -1;
    }

    @Override
    public void insert(Interval interval) {
        if (interval.getEnd() > end) {
            end = interval.getEnd();
        }
        root = insert(interval, root);
    }

    /**
     * Insert interval into the tree and balance.
     */
    IntervalNode insert(Interval interval, IntervalNode node) {
        long start = interval.getStart();
        long end = interval.getEnd();

        if (node == null) {
            return new IntervalNode(interval);

        } else if (start == node.interval.getStart() && end == node.interval.getEnd()) {
            // same interval...just overwrite
            node.interval = interval;
            overwrites++;

        } else if (end <= node.interval.getStart()) {
            // new interval is left of current
            node.left = insert(interval, node.left);

        } else if (start <= node.interval.getStart() && end < node.interval.getEnd()) {
            // new interval overlaps with a left portion of the current interval...relinquish overlapping part pass new
            // interval to the left
            node.left = insert(interval, node.left);
            node.interval = new ObjectInterval(end, node.interval.getEnd(), node.interval.getVersion(),
                    node.interval.getId());

            // Special case to merge two intervals, if they are from the same operation
            // FIXME (jdillmann): Problem if vector is [-2-][-1-][-2-] and should become [----2----], then it would
            // either become [-2-][--2--] or [--2--][-2-]
            // } else if (start == node.interval.end && interval.equalsVersionId(node.interval)) {

        } else if (start >= node.interval.getEnd()) {
            // new interval is left of current
            node.right = insert(interval, node.right);

        } else if (start > node.interval.getStart() && end >= node.interval.getEnd()) {
            // new interval overlaps with a right portion of the current interval...relinquish overlapping part pass new
            // interval to the right
            node.right = insert(interval, node.right);
            node.interval = new ObjectInterval(node.interval.getStart(), start, node.interval.getVersion(),
                    node.interval.getId());

        } else if (start > node.interval.getStart() && end < node.interval.getEnd()) {
            // new interval fits into current interval
            ObjectInterval leftTail = new ObjectInterval(node.interval.getStart(), start, node.interval.getVersion(),
                    node.interval.getId());
            ObjectInterval rightTail = new ObjectInterval(end, node.interval.getEnd(), node.interval.getVersion(),
                    node.interval.getId());

            IntervalNode newNode = new IntervalNode(interval);
            newNode.left = insert(leftTail, node.left);
            newNode.right = insert(rightTail, node.right);
            node = newNode;

            overwrites++;

        } else if (start <= node.interval.getStart() && end >= node.interval.getEnd()) {
            // new interval surrounds current interval
            if (node.left != null && start < node.interval.getStart()) {
                node.left = truncateMax(start, node.left);
            }
            if (node.right != null && end > node.interval.getEnd()) {
                node.right = truncateMin(end, node.right);
            }
            node.interval = interval;

            overwrites++;
        }

        return rotate(node);
    }

    IntervalNode rotate(IntervalNode node) {
        node.checkHeight();

        if (node.balance == -2) {
            if (node.right.balance == -1) {
                // single (left) rotation
                node = rotateLeft(node);

            } else {
                // double rotation on the right
                node.right = rotateRight(node.right);
                node = rotateLeft(node);
            }

        } else if (node.balance == 2) {
            if (node.left.balance == 1) {
                // single (right) rotation
                node = rotateRight(node);
            } else {
                // double rotation on the left
                node.left = rotateLeft(node.left);
                node = rotateRight(node);
            }
        }

        return node;
    }

    IntervalNode rotateRight(IntervalNode node) {
        IntervalNode h = node.left;
        node.left = node.left.right;
        h.right = node;

        node.checkHeight();
        h.checkHeight();

        return h;
    }

    IntervalNode rotateLeft(IntervalNode node) {
        IntervalNode h = node.right;
        node.right = node.right.left;
        h.left = node;

        node.checkHeight();
        h.checkHeight();

        return h;
    }

    IntervalNode truncateMax(long max, IntervalNode node) {
        if (node == null) {
            // The whole tree got truncated.
            return node;
        }

        if (max <= node.interval.getStart()) {
            // The max is left of the current interval.
            // Drop the node and its right subtree and truncate the remaining left subtree.
            overwrites = overwrites + 1 + maxIntervals(node.right);
            return truncateMax(max, node.left);

        } else if (max > node.interval.getEnd()) {
            // The max is right of the current interval.
            // Truncate the right subtree and return the rotated current interval.
            node.right = truncateMax(max, node.right);
            return rotate(node);

        } else { // if (max > node.interval.begin && max <= node.interval.end) {
            // The max is within the current interval.
            // Adjust it and drop its right subtree and return it.
            overwrites = overwrites + maxIntervals(node.right);
            ObjectInterval truncated = new ObjectInterval(node.interval.getStart(), max, node.interval.getVersion(),
                    node.interval.getId());
            node.interval = truncated;
            node.right = null;
            return rotate(node);
        }
    }

    IntervalNode truncateMin(long min, IntervalNode node) {
        if (node == null) {
            // The whole tree got truncated.
            return node;
        }

        if (min >= node.interval.getEnd()) {
            // The min is right of the current interval.
            // Drop the node and its left subtree and truncate the remaining right subtree.
            overwrites = overwrites + 1 + maxIntervals(node.left);
            return truncateMin(min, node.right);

        } else if (min < node.interval.getStart()) {
            // The min is left of the current interval.
            // Truncate the left subtree and return the rotated current interval.
            node.left = truncateMin(min, node.left);
            return rotate(node);

        } else { // if (min >= node.interval.begin && min < node.interval.end) {
            // The min is within the current interval.
            // Adjust it and drop its left subtree and return it.
            overwrites = overwrites + maxIntervals(node.left);
            ObjectInterval truncated = new ObjectInterval(min, node.interval.getEnd(), node.interval.getVersion(),
                    node.interval.getId());
            node.interval = truncated;
            node.left = null;
            return rotate(node);
        }
    }

    @Override
    public List<Interval> serialize() {
        LinkedList<Interval> intervals = new LinkedList<Interval>();
        serialize(root, intervals);

        // Fill gap at the beginning of the vector.
        if (intervals.size() > 0) {
            Interval first = intervals.getFirst();
            if (first.getStart() > 0) {
                ObjectInterval gap = new ObjectInterval(0, first.getStart());
                intervals.addFirst(gap);
            }
        }


        return intervals;
    }

    void serialize(IntervalNode node, LinkedList<Interval> acc) {
        if (node == null) {
            return;
        }

        serialize(node.left, acc);
        addInterval(acc, node.interval);
        serialize(node.right, acc);
    }

    @Override
    public IntervalVector getSlice(long start, long end) {
        LinkedList<Interval> versions = new LinkedList<Interval>();
        getOverlapping(start, end, this.root, versions);

        sliceIntervalList(versions, start, end);
        return new ListIntervalVector(versions);
    }

    @Override
    public IntervalVector getOverlapping(long start, long end) {
        LinkedList<Interval> versions = new LinkedList<Interval>();
        getOverlapping(start, end, this.root, versions);

        if (versions.size() > 0) {
            Interval first = versions.getFirst();
            if (first.getStart() > start) {
                // Pad from the beginning
                ObjectInterval pad = new ObjectInterval(0, first.getStart());
                versions.addFirst(pad);
            }
        }

        return new ListIntervalVector(versions);
    }

    void getOverlapping(long begin, long end, IntervalNode node, LinkedList<Interval> acc) {
        if (node == null) {
            return;
        }

        // Descend left if the lookup starts left of the current node
        if (begin < node.interval.getStart()) {
            getOverlapping(begin, end, node.left, acc);
        }

        // Add the current node if it overlaps with the lookup
        if (begin < node.interval.getEnd() && node.interval.getStart() < end) {
            addInterval(acc, node.interval);
        }

        // Descend right if the lookup end right of the current node
        if (end > node.interval.getEnd()) {
            getOverlapping(begin, end, node.right, acc);
        }
    }

    /**
     * Truncate the IntervalTree to end at max.
     * 
     * @param max
     */
    // TODO (jdillmann): Rename to truncateMax
    public void truncate(long max) {
        if (max < 0) {
            throw new IllegalArgumentException("Cannot truncate to negative values.");
        }

        if (max < this.end) {
            this.end = max;
            root = truncateMax(max, root);
        }
    }

    /**
     * Number of intervals that have been overwritten since the last reset. <br>
     * The number of overwrites is a good indicator to trigger the truncation of an IntervalVersionTree log.
     * 
     * @return number of overwrites since last reset
     */
    public int getOverwrites() {
        return overwrites;
    }

    /**
     * Reset the internal overwrite counter to 0.
     */
    public void resetOverwrites() {
        overwrites = 0;
    }

    @Override
    public long getMaxVersion() {
        return maxVersion;
    }

    @Override
    public boolean isMaxVersionGreaterThen(IntervalVector v) {
        // FIXME (jdillmann): Implement here
        return (new ListIntervalVector(this)).isMaxVersionGreaterThen(v);
    }

    @Override
    public boolean compareLEQThen(IntervalVector o) {
        // FIXME (jdillmann): Implement here
        return (new ListIntervalVector(this)).compareLEQThen(o);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(root, sb);
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    void toString(IntervalNode node, StringBuilder sb) {
        if (node == null) {
            return;
        }

        toString(node.left, sb);

        sb.append(node.interval.toString());
        sb.append(" ");

        toString(node.right, sb);
    }

    // Helper methods

    /**
     * Worst case guess of the number of intervals in this tree (including the node itself)
     */
    int maxIntervals(IntervalNode node) {
        if (node == null)
            return 0;

        return (int) (1 + (Math.pow(2, node.height) - 1) + (Math.pow(2, (node.height - Math.abs(node.balance))) - 1));
    }

    /*
     * A node contains the left and the right branch, the middle index of this node and a lists of intervals.
     * The list contains all intervals intersecting with the index.
     */
    static class IntervalNode {
        Interval     interval;
        IntervalNode left;
        IntervalNode right;
        int          balance;
        int          height;

        public IntervalNode(Interval interval) {
            this.interval = interval;
            balance = 0;
            height = 0;
        }

        void checkHeight() {
            int l_h = this.left == null ? 0 : this.left.height + 1;
            int r_h = this.right == null ? 0 : this.right.height + 1;
            this.height = Math.max(l_h, r_h);
            this.balance = l_h - r_h;
        }

        @Override
        public String toString() {
            return interval.toString();
        }
    }
}
