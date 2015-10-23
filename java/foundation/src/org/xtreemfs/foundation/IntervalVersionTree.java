/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation;

import java.util.LinkedList;

/**
 * @author Jan Fajerski
 */
public class IntervalVersionTree {

    private IntervalNode root;
    private long highest;

    public IntervalVersionTree(long begin, long end) {
        if (begin != 0) {
            throw new IllegalArgumentException("IntervalVersionTree must start at 0");
        }
        this.root = new IntervalNode(begin, end);
        this.highest = end;
    }

    public void insert(Interval i) {
        this.insert(i.begin, i.end, i.version);
    }

    public void insert(long begin, long end, long version) {
        if (begin < 0) {
            throw new IllegalArgumentException("Intervals may only start at 0 or greater, not " + begin);
        }

        if (end <= begin) {
            throw new IllegalArgumentException("Intervals must be bigger the 1");
        }

        if (end >= highest) {
            this.highest = end;
            if (begin == 0) {
                this.root = new IntervalNode(begin, end, version);
            }
        }

        this.root = insert(begin, end, version, this.root);
    }

    private IntervalNode insert(long begin, long end, long version, IntervalNode node) {
        if (node == null) {
            return new IntervalNode(begin, end, version);

        } else if (begin == node.interval.begin && end == node.interval.end) {
            // same interval...just set version
            node.interval.version = version;

        } else if (end < node.interval.begin) {
            // new interval is left of current
            node.left = insert(begin, end, version, node.left);

        } else if (begin <= node.interval.begin && end < node.interval.end) {
            // new interval overlaps with a left portion of the current interval...relinquish overlapping part pass new interval to the left
            node.left = insert(begin, end, version, node.left);
            node.interval.begin = end + 1;

            // check for colour change or need to rotate
        } else if (begin > node.interval.end) {
            // new interval is left of current
            node.right = insert(begin, end, version, node.right);

        } else if (begin > node.interval.begin && end >= node.interval.end) {
            // new interval overlaps with a right portion of the current interval...relinquish overlapping part pass new interval to the right
            node.right = insert(begin, end, version, node.right);
            node.interval.end = begin - 1;

            // check for colour change or need to rotate
        } else if (begin > node.interval.begin && end < node.interval.end) {
            // new interval fits into current interval
            IntervalNode newNode = new IntervalNode(begin, end, version);
            newNode.left = insert(node.interval.begin, begin - 1, node.interval.version, node.left);
            newNode.right = insert(end + 1, node.interval.end, node.interval.version, node.right);
            node = newNode;

        } else if (begin <= node.interval.begin && end >= node.interval.end) {
            // new interval surrounds current interval
            if (node.left != null && begin < node.interval.begin) {
                node.left = shrinkSubTree(begin, node.interval.begin, node.left);
            }
            if (node.right != null && end > node.interval.end) {
                node.right = shrinkSubTree(node.interval.end, end, node.right);
            }
            node.interval.begin = begin;
            node.interval.end = end;
            node.interval.version = version;
        }

        node.checkHeight();
        if (node.balance < -1) node = rotateLeft(node);
        if (node.balance > 1) node = rotateRight(node);
        return node;
    }

    public IntervalNode rotateRight(IntervalNode node) {
        IntervalNode h = node.left;
        node.left = node.left.right;
        h.right = node;

        node.checkHeight();
        h.checkHeight();

        return h;
    }

    public IntervalNode rotateLeft(IntervalNode node) {
        IntervalNode h = node.right;
        node.right = node.right.left;
        h.left = node;

        node.checkHeight();
        h.checkHeight();

        return h;
    }

    /*
     * shrink will always be called from a node that wants to grow. so the subtree will shrink to one side, not "in the middle"
     */
    private IntervalNode shrinkSubTree(long begin, long end, IntervalNode node) {
        if (node == null) {
            return node;
        } else if (begin > node.interval.end) {
            // right subtree needs to shrink
            assert(node.right != null);
            node.right = shrinkSubTree(begin, end, node.right);

        } else if (end < node.interval.begin) {
            // left subtree needs to shrink
            assert(node.left != null);
            node.left = shrinkSubTree(begin, end, node.left);

        } else if (begin > node.interval.begin) {
            // node has to give up part of its interval and the whole right subtree
            node.interval.end = begin - 1;
            node.right = null;

        } else if (end < node.interval.end) {
            // node has to give up part of its interval and maybe left subtree
            node.interval.begin = end + 1;
            node.left = null;
        } else if (begin <= node.interval.begin){
            // the node interval and its complete left subtree is absorbed by some other node
            return shrinkSubTree(begin, end, node.left);

        } else if (end >= node.interval.end) {
            // the node interval and its complete right subtree is absorbed by some other node
            return shrinkSubTree(begin, end, node.right);

        }
        return node;
    }

    private IntervalNode checkColour(IntervalNode node) {
        if (node == null) {
            return node;
        }
        return node;
    }

    public  LinkedList<Interval> getVersions(long begin, long end) {
        return getVersions(begin, end, this.root, new LinkedList<Interval>());
    }

    private LinkedList<Interval> getVersions(long begin, long end, IntervalNode node, LinkedList<Interval> acc) {

        if (node == null) {
            return acc;
        } else if (begin >= node.interval.begin && end <= node.interval.end) {
            acc.add(new Interval(begin, end, node.interval.version));
        } else if (begin < node.interval.begin && end <= node.interval.end) {
            acc.add(new Interval(node.interval.begin, end, node.interval.version));
            return getVersions(begin, end, node.left, acc);
        } else if (begin >= node.interval.begin && end > node.interval.end) {
            acc.add(new Interval(begin, node.interval.end, node.interval.version));
            return getVersions(begin, end, node.right, acc);
        } else if (begin < node.interval.begin && end > node.interval.end) {
            acc.add(new Interval(node.interval.begin, node.interval.end, node.interval.version));
            return getVersions(begin, end, node.right, getVersions(begin, end, node.left, acc));
        }
        return acc;
    }

    public static class Interval{
        long begin;
        long end;
        long version;

        public Interval(long begin, long end) {
            this(begin, end, 0);
        }

        public Interval(long begin, long end, long version) {
            this.begin = begin;
            this.end = end;
            this.version = version;
        }

        @Override
        public boolean equals (Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Interval))return false;
            Interval interval = (Interval)other;
            return this.begin == interval.begin
                    && this.end == interval.end
                    && this.version == interval.version;
        }
    }

    /*
     * A node contains the left and the right branch, the middle index of this node and a lists of intervals.
     * The list contains all intervals intersecting with the index.
     */
    private class IntervalNode {
        Interval interval;
        IntervalNode left;
        IntervalNode right;
        int balance;
        int height;

        IntervalNode(long begin, long end) {
            this.interval = new Interval(begin, end);
            this.left = null;
            this.right = null;
            height = 0;
            this.balance = 0;
        }

        IntervalNode(long begin, long end, long version) {
            this.interval = new Interval(begin, end, version);
            this.left = null;
            this.right = null;
            height = 0;
            this.balance = 0;
        }

        void checkHeight() {
            int l_h = this.left == null ? 0 : this.left.height + 1;
            int r_h = this.right == null ? 0 : this.right.height + 1;
            this.height = Math.max(l_h, r_h);
            this.balance = l_h - r_h;
        }
    }
}
