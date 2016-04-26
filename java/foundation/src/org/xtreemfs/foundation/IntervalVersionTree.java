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
public class IntervalVersionTree implements IntervalVersionTreeInterface {

    IntervalNode root;
    long         highest;

    public IntervalVersionTree(long begin, long end) {
        if (begin != 0) {
            throw new IllegalArgumentException("IntervalVersionTree must start at 0");
        }
        this.root = new IntervalNode(begin, end);
        this.highest = end;
    }

    @Override
    public void insert(Interval i) {
        this.insert(i.begin, i.end, i.version);
    }

    @Override
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

    /*
     * insert into tree and balance after
     */
    static IntervalNode insert(long begin, long end, long version, IntervalNode node) {
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

        } else if (begin > node.interval.end) {
            // new interval is left of current
            node.right = insert(begin, end, version, node.right);

        } else if (begin > node.interval.begin && end >= node.interval.end) {
            // new interval overlaps with a right portion of the current interval...relinquish overlapping part pass new interval to the right
            node.right = insert(begin, end, version, node.right);
            node.interval.end = begin - 1;

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

        return rotate(node);
    }

    static IntervalNode rotate(IntervalNode node) {
        node.checkHeight();

        if (node.balance < -1) {
            node = rotateLeft(node);
        }

        if (node.balance > 1) {
            node = rotateRight(node);
        }

        return node;
    }

    static IntervalNode rotateRight(IntervalNode node) {
        IntervalNode h = node.left;
        node.left = node.left.right;
        h.right = node;

        node.checkHeight();
        h.checkHeight();

        return h;
    }

    static IntervalNode rotateLeft(IntervalNode node) {
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
    static IntervalNode shrinkSubTree(long begin, long end, IntervalNode node) {
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

    @Override
    public LinkedList<Interval> getVersions(Interval i) {
        return getVersions(i.begin, i.end, this.root, new LinkedList<Interval>());
    }

    @Override
    public  LinkedList<Interval> getVersions(long begin, long end) {
        return getVersions(begin, end, this.root, new LinkedList<Interval>());
    }

    static LinkedList<Interval> getVersions(long begin, long end, IntervalNode node, LinkedList<Interval> acc) {

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

    /**
     * Truncate the IntervalTree to end at max.
     * 
     * @param max
     */
    public void truncate(long max) {
        if (max < 1) {
            throw new IllegalArgumentException("Cannot truncate to 0 since Intervals have to be at least of length 1.");
        }

        if (max < this.highest) {
            this.highest = max;
            root = truncate(max, root);
        }
    }

    static IntervalNode truncate(long max, IntervalNode node) {
        if (max < node.interval.begin) {
            // The max is left of the current interval.
            // Drop the root and its right subtree and truncate the remaining left subtree.
            // This will work, as every IntervalVersionTree is beginning at 0
            return truncate(max, node.left);

        } else if (max <= node.interval.end) {
            // && max >= node.interval.begin
            // The max is within the current interval.
            // Adjust it and drop its right subtree and return it.
            node.interval.end = max;
            node.right = null;

            return rotate(node);

        } else if (node.right != null) {
            // && max > node.interval.end
            // The max is right of the current interval and there are intervals right of it.
            // Truncate the right subtree and return the rotated current interval.
            node.right = truncate(max, node.right);

            return rotate(node);
        } else {
            // max > node.interval.end && node.right == null
            // The max is right of the current interval and there are no intervals right of it.
            // Return the current node.
            return node;
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(root, sb);
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    static void toString(IntervalNode node, StringBuilder sb) {
        if (node == null) {
            return;
        }

        if (node.left != null) {
            toString(node.left, sb);
        }
        
        sb.append(node.interval.toString());
        sb.append(" ");

        if (node.right != null) {
            toString(node.right, sb);
        }
    }

    /*
     * A node contains the left and the right branch, the middle index of this node and a lists of intervals.
     * The list contains all intervals intersecting with the index.
     */
    static class IntervalNode {
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

        @Override
        public String toString() {
            return interval.toString();
        }
    }
}
