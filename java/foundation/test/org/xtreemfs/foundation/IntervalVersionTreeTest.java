/*
 * Copyright (c) 2015 by Jan Fajerski, Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;

import org.junit.Test;
import org.xtreemfs.foundation.IntervalVersionTree.IntervalNode;
import org.xtreemfs.foundation.IntervalVersionTreeInterface.Interval;

/**
 * @author Jan Fajerski
 */
public class IntervalVersionTreeTest {
    @Test
    public final void testBasicNonOverlappingIntervals() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        LinkedList<Interval> versions;

        IntervalVersionTree tree = new IntervalVersionTree(0, 1023);
        expected.add(new Interval(0, 1023));
        versions = tree.getVersions(0, 1023);
        assertTrue(expected.containsAll(versions));

        tree.insert(1024, 2047, 1);
        expected.add(new Interval(1024, 2047, 1));
        versions = tree.getVersions(0, 2047);
        assertTrue(expected.containsAll(versions));

        tree.insert(2048, 4095, 1);
        expected.add(new Interval(2048, 4095, 1));
        versions = tree.getVersions(0, 4095);
        assertTrue(expected.containsAll(versions));

        tree.insert(0, 1023, 1);
        expected.get(0).version = 1;
        versions = tree.getVersions(0,4095);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public final void testInsertOfOverlappingIntervals() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);
        LinkedList<Interval> versions;

        tree.insert(1024, 4095, 1);
        expected.add(new Interval(0, 1023, 0));
        expected.add(new Interval(1024, 4095, 1));
        versions = tree.getVersions(0, 4095);
        assertTrue(expected.containsAll(versions));

        tree.insert(0, 511, 1);
        expected.get(0).begin = 512;
        expected.add(new Interval(0, 511, 1));
        versions = tree.getVersions(0, 4095);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public final void testInsertOfIntervalInsideOfExisting() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);
        LinkedList<Interval> versions;

        tree.insert(512, 1535, 1);
        expected.add(new Interval(0, 511, 0));
        expected.add(new Interval(512, 1535, 1));
        expected.add(new Interval(1536, 2047, 0));
        versions = tree.getVersions(0, 2047);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public final void testInsertOfSurroundingInterval() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);
        LinkedList<Interval> versions;

        tree.insert(512, 1535, 1);
        tree.insert(500, 1800, 2);
        expected.add(new Interval(0, 499, 0));
        expected.add(new Interval(500, 1800, 2));
        expected.add(new Interval(1801, 2047, 0));
        versions = tree.getVersions(0, 2047);
        assertTrue(expected.containsAll(versions));

        expected = new LinkedList<Interval>();
        tree = new IntervalVersionTree(0, 2047);
        tree.insert(512, 1535, 1);
        tree.insert(0, 5, 1);
        tree.insert(500, 511, 1);
        tree.insert(500, 1800, 2);
        expected.add(new Interval(0, 5, 1));
        expected.add(new Interval(6, 499, 0));
        expected.add(new Interval(500, 1800, 2));
        expected.add(new Interval(1801, 2047, 0));
        versions = tree.getVersions(0, 2047);
        assertTrue(expected.containsAll(versions));

        tree = new IntervalVersionTree(0, 1023);
        expected = new LinkedList<Interval>();
        expected.add(new Interval(0, 2047, 1));
        tree.insert(0, 2047, 1);
        versions = tree.getVersions(0, 2047);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public final void testPartialRetrieve() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);
        LinkedList<Interval> versions;

        tree.insert(512, 1535, 1);
        expected.add(new Interval(0, 511, 0));
        expected.add(new Interval(512, 1535, 1));
        expected.add(new Interval(1536, 2000, 0));
        versions = tree.getVersions(0, 2000);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public final void testBalance() {
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);

        tree.insert(0, 1023, 1);
        tree.insert(0, 512, 2);
        tree.insert(0, 256, 3);
        tree.insert(0, 128, 4);
        tree.insert(0, 64, 5);
        tree.insert(0, 32, 6);

        tree = new IntervalVersionTree(0, 2047);
        tree.insert(0, 1023, 1);
        tree.insert(0, 512, 2);
        tree.insert(0, 256, 3);
        tree.insert(0, 128, 4);
        tree.insert(0, 64, 5);
        tree.insert(0, 32, 6);

        tree = new IntervalVersionTree(0, 2047);
        tree.insert(512, 1535, 1);
        tree.insert(0, 5, 1);
        tree.insert(500, 511, 1);
        tree.insert(500, 2047, 2);
    }

    @Test
    public void testTruncate() {
        IntervalVersionTree tree = new IntervalVersionTree(0, 10);
        // truncate without effect
        tree.truncate(20);
        assertEquals(10, tree.highest);
        assertEquals("([0:10], 0)", tree.toString());

        tree.truncate(10);
        assertEquals(10, tree.highest);
        assertEquals("([0:10], 0)", tree.toString());

        // truncate to 0 should throw an exception
        try {
            tree.truncate(0);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        // simple truncate on a single interval
        tree.truncate(5);
        assertEquals(5, tree.highest);
        assertEquals("([0:5], 0)", tree.toString());

        // simple truncate on a single interval to the begin
        tree.truncate(1);
        assertEquals(1, tree.highest);
        assertEquals("([0:1], 0)", tree.toString());

        
        // test droping the right tree
        tree = createFullTestTree();
        tree.truncate(11);
        assertEquals("([0:2], 0) ([3:5], 0) ([6:8], 0) ([9:11], 0)", tree.toString());
        assertEquals(2, tree.root.height);
        assertEquals(-1, tree.root.balance);

        // test truncating on rightmost node
        tree = createFullTestTree();
        tree.truncate(19);
        assertEquals("([0:2], 0) ([3:5], 0) ([6:8], 0) ([9:11], 0) ([12:14], 0) ([15:17], 0) ([18:19], 0)",
                tree.toString());
        assertEquals(2, tree.root.height);
        assertEquals(0, tree.root.balance);

        // test truncating on leftmost right node (drops parent and its right tree)
        tree = createFullTestTree();
        tree.truncate(14);
        assertEquals("([0:2], 0) ([3:5], 0) ([6:8], 0) ([9:11], 0) ([12:14], 0)", tree.toString());
        assertEquals(2, tree.root.height);
        assertEquals(1, tree.root.balance);

        // test truncating on the leftmost node (drops root and every other node)
        tree = createFullTestTree();
        tree.truncate(1);
        assertEquals("([0:1], 0)", tree.toString());
        assertEquals(0, tree.root.height);
        assertEquals(0, tree.root.balance);

        // test truncating on the left root node (drops root and nodes right tree)
        tree = createFullTestTree();
        tree.truncate(4);
        assertEquals("([0:2], 0) ([3:4], 0)", tree.toString());
        assertEquals(1, tree.root.height);
        assertEquals(1, tree.root.balance);

        // test truncating on the rightmost left node
        tree = createFullTestTree();
        tree.truncate(7);
        assertEquals("([0:2], 0) ([3:5], 0) ([6:7], 0)", tree.toString());
        assertEquals(1, tree.root.height);
        assertEquals(0, tree.root.balance);
    }

    IntervalVersionTree createFullTestTree() {
        IntervalNode root = new IntervalNode(9, 11);
        IntervalNode node;

        node = new IntervalNode(3, 5);
        node.left = new IntervalNode(0, 2);
        node.right = new IntervalNode(6, 8);
        node.checkHeight();
        root.left = node;

        node = new IntervalNode(15, 17);
        node.left = new IntervalNode(12, 14);
        node.right = new IntervalNode(18, 20);
        node.checkHeight();
        root.right = node;

        root.checkHeight();

        IntervalVersionTree tree = new IntervalVersionTree(0, 20);
        tree.root = root;
        return tree;
    }

}
