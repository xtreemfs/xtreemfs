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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

        IntervalVersionTree tree = new IntervalVersionTree(0, 1024);
        expected.add(new Interval(0, 1024));
        versions = tree.getVersions(0, 1024);
        assertTrue(expected.containsAll(versions));

        tree.insert(1024, 2048, 1);
        expected.add(new Interval(1024, 2048, 1));
        versions = tree.getVersions(0, 2048);
        assertTrue(expected.containsAll(versions));

        tree.insert(2048, 4096, 2);
        expected.add(new Interval(2048, 4096, 2));
        versions = tree.getVersions(0, 4096);
        assertTrue(expected.containsAll(versions));

        tree.insert(0, 1024, 3);
        expected.get(0).version = 3;
        versions = tree.getVersions(0, 4096);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public final void testInsertOfOverlappingIntervals() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(1024, 4096, 1);
        expected.add(new Interval(0, 1024, 0));
        expected.add(new Interval(1024, 4096, 1));
        versions = tree.getVersions(0, 4096);
        assertTrue(expected.containsAll(versions));

        tree.insert(0, 512, 1);
        expected.get(0).begin = 512;
        expected.add(new Interval(0, 512, 1));
        versions = tree.getVersions(0, 4096);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public final void testInsertOfIntervalInsideOfExisting() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(512, 1536, 1);
        expected.add(new Interval(0, 512, 0));
        expected.add(new Interval(512, 1536, 1));
        expected.add(new Interval(1536, 2048, 0));
        versions = tree.getVersions(0, 2048);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public final void testInsertOfSurroundingInterval() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(512, 1536, 1);
        tree.insert(500, 1800, 2);
        expected.add(new Interval(0, 500, 0));
        expected.add(new Interval(500, 1800, 2));
        expected.add(new Interval(1800, 2048, 0));
        versions = tree.getVersions(0, 2048);
        assertTrue(expected.containsAll(versions));

        expected = new LinkedList<Interval>();
        tree = new IntervalVersionTree(0, 2048);
        tree.insert(512, 1536, 1);
        tree.insert(0, 5, 1);
        tree.insert(500, 512, 1);
        tree.insert(500, 1800, 2);
        expected.add(new Interval(0, 5, 1));
        expected.add(new Interval(5, 500, 0));
        expected.add(new Interval(500, 1800, 2));
        expected.add(new Interval(1800, 2048, 0));
        versions = tree.getVersions(0, 2048);
        assertTrue(expected.containsAll(versions));

        tree = new IntervalVersionTree(0, 1024);
        expected = new LinkedList<Interval>();
        expected.add(new Interval(0, 2048, 1));
        tree.insert(0, 2048, 1);
        versions = tree.getVersions(0, 2048);
        assertTrue(expected.containsAll(versions));
    }

    @Test
    public void testRetrieve() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(512, 1536, 1);

        // Retrieve the whole tree
        expected.add(new Interval(0, 512, 0));
        expected.add(new Interval(512, 1536, 1));
        expected.add(new Interval(1536, 2048, 0));
        versions = tree.getVersions(0, 2048);
        assertEquals(expected, versions);

        // Retrieve only the root
        expected.clear();
        expected.add(new Interval(512, 1536, 1));
        versions = tree.getVersions(512, 1536);
        assertEquals(expected, versions);

        // Retrieve from right tree, but not the root
        expected.clear();
        expected.add(new Interval(1536, 2048, 0));
        versions = tree.getVersions(1536, 2048);
        assertEquals(expected, versions);

        // Retrieve from left tree, but not the root
        expected.clear();
        expected.add(new Interval(0, 512, 0));
        versions = tree.getVersions(0, 512);
        assertEquals(expected, versions);
    }

    @Test
    public final void testPartialRetrieve() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(512, 1536, 1);
        expected.add(new Interval(0, 512, 0));
        expected.add(new Interval(512, 1536, 1));
        expected.add(new Interval(1536, 2000, 0));
        versions = tree.getVersions(0, 2000);
        assertEquals(expected, versions);

        // Retrieve from right tree, but not the root
        expected.clear();
        expected.add(new Interval(1550, 2048, 0));
        versions = tree.getVersions(1550, 2048);
        assertEquals(expected, versions);

        // Retrieve from left tree, but not the root
        expected.clear();
        expected.add(new Interval(0, 500, 0));
        versions = tree.getVersions(0, 500);
        assertEquals(expected, versions);
    }

    @Test
    public final void testBalance() {
        IntervalVersionTree tree = new IntervalVersionTree(0, 2048);
        tree.insert(0, 1024, 1);
        tree.insert(0, 512, 2);
        tree.insert(0, 256, 3);
        tree.insert(0, 128, 4);
        tree.insert(0, 64, 5);
        tree.insert(0, 32, 6);
        assertEquals(0, tree.root.balance);

        tree = new IntervalVersionTree(0, 2048);

        tree.insert(512, 1536, 1);
        tree.insert(0, 5, 1);
        tree.insert(500, 511, 1);
        tree.insert(500, 2047, 2);

    }

    @Test
    public void testCompactRetrieve() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        LinkedList<Interval> versions;

        IntervalVersionTree tree = new IntervalVersionTree(0, 1024);
        tree.insert(1024, 2048, 0);
        tree.insert(2048, 4096, 0);

        expected.add(new Interval(0, 4096, 0));
        versions = tree.getVersions(0, 4096);
        assertEquals(expected, versions);
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
            tree.truncate(-1);
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

        LinkedList<Interval> expected = new LinkedList<Interval>();
        LinkedList<Interval> result;

        // test dropping the right tree
        tree = createFullTestTree();
        expected = tree.getVersions(0, 2048);
        tree.truncate(2048);
        assertEquals(expected, tree.getVersions(0, 4096));
        assertEquals(2, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on rightmost node
        tree = createFullTestTree();
        expected = tree.getVersions(0, 4000);
        tree.truncate(4000);
        assertEquals(expected, tree.getVersions(0, 4096));
        assertEquals(2, tree.root.height);
        assertEquals(0, tree.root.balance);

        // test truncating on leftmost right node (drops parent and its right tree)
        tree = createFullTestTree();
        expected = tree.getVersions(0, 2500);
        tree.truncate(2500);
        assertEquals(expected, tree.getVersions(0, 4096));
        assertEquals(2, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on the leftmost node (drops root and every other node)
        tree = createFullTestTree();
        expected = tree.getVersions(0, 500);
        tree.truncate(500);
        assertEquals(expected, tree.getVersions(0, 4096));
        assertEquals(0, tree.root.height);
        assertEquals(0, tree.root.balance);

        // test truncating on the left root node (drops root and nodes right tree)
        tree = createFullTestTree();
        expected = tree.getVersions(0, 1000);
        tree.truncate(1000);
        assertEquals(expected, tree.getVersions(0, 4096));
        assertEquals(1, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on the rightmost left node
        tree = createFullTestTree();
        expected = tree.getVersions(0, 1500);
        tree.truncate(1500);
        assertEquals(expected, tree.getVersions(0, 4096));
        assertEquals(1, tree.root.height);
        assertEquals(0, tree.root.balance);
    }

    IntervalVersionTree createFullTestTree() {
        IntervalNode root = new IntervalNode(1536, 2048, 3);
        IntervalNode node;

        node = new IntervalNode(512, 1024, 1);
        node.left = new IntervalNode(0, 512, 0);
        node.right = new IntervalNode(1024, 1536, 2);
        node.checkHeight();
        root.left = node;

        node = new IntervalNode(2560, 3584, 5);
        node.left = new IntervalNode(2048, 2560, 4);
        node.right = new IntervalNode(3584, 4096, 6);
        node.checkHeight();
        root.right = node;

        root.checkHeight();

        IntervalVersionTree tree = new IntervalVersionTree(0, 4096);
        tree.root = root;
        return tree;
    }

}
