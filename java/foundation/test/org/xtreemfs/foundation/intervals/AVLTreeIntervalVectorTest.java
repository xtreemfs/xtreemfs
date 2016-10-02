/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector.IntervalNode;

public class AVLTreeIntervalVectorTest {
    @Test
    public final void testBasicNonOverlappingIntervals() {
        LinkedList<ObjectInterval> expected = new LinkedList<ObjectInterval>();
        List<Interval> versions;

        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 1024, 1));
        expected.add(new ObjectInterval(0, 1024, 1));
        expected.add(ObjectInterval.empty(1024, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        tree.insert(new ObjectInterval(1024, 2048, 2));
        expected.removeLast();
        expected.add(new ObjectInterval(1024, 2048, 2));
        expected.add(ObjectInterval.empty(2048, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        tree.insert(new ObjectInterval(2048, 4096, 3));
        expected.removeLast();
        expected.add(new ObjectInterval(2048, 4096, 3));
        expected.add(ObjectInterval.empty(4096, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        tree.insert(new ObjectInterval(0, 1024, 4));
        expected.set(0, new ObjectInterval(0, 1024, 4));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        expected.clear();
        expected.add(new ObjectInterval(2048, 4096, 3));
        versions = tree.getOverlapping(2048, 4096);
        assertEquals(expected, versions);
    }

    @Test
    public final void testInsertOfOverlappingIntervals() {
        LinkedList<ObjectInterval> expected = new LinkedList<ObjectInterval>();
        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        List<Interval> versions;

        tree.insert(new ObjectInterval(1024, 4096, 1));
        expected.add(ObjectInterval.empty(0, 1024));
        expected.add(new ObjectInterval(1024, 4096, 1));
        expected.add(ObjectInterval.empty(4096, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        tree.insert(new ObjectInterval(0, 512, 1));
        expected.clear();
        expected.add(new ObjectInterval(0, 512, 1));
        expected.add(new ObjectInterval(512, 1024, -1));
        expected.add(new ObjectInterval(1024, 4096, 1));
        expected.add(ObjectInterval.empty(4096, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
    }

    @Test
    public final void testInsertOfIntervalInsideOfExisting() {
        LinkedList<ObjectInterval> expected = new LinkedList<ObjectInterval>();
        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        List<Interval> versions;

        tree.insert(new ObjectInterval(0, 2048, 0));
        tree.insert(new ObjectInterval(512, 1536, 1));
        expected.add(new ObjectInterval(0, 512, 0));
        expected.add(new ObjectInterval(512, 1536, 1));
        expected.add(new ObjectInterval(1536, 2048, 0));
        expected.add(ObjectInterval.empty(2048, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
    }

    @Test
    public final void testInsertOfSurroundingInterval() {
        LinkedList<ObjectInterval> expected = new LinkedList<ObjectInterval>();
        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 2048, 0));
        List<Interval> versions;

        tree.insert(new ObjectInterval(512, 1536, 1));
        tree.insert(new ObjectInterval(500, 1800, 2));
        expected.add(new ObjectInterval(0, 500, 0));
        expected.add(new ObjectInterval(500, 1800, 2));
        expected.add(new ObjectInterval(1800, 2048, 0));
        expected.add(ObjectInterval.empty(2048, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        
        expected = new LinkedList<ObjectInterval>();
        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 2048, 0));

        tree.insert(new ObjectInterval(512, 1536, 1));
        tree.insert(new ObjectInterval(0, 5, 1));
        tree.insert(new ObjectInterval(500, 512, 1));
        tree.insert(new ObjectInterval(500, 1800, 2));
        expected.add(new ObjectInterval(0, 5, 1));
        expected.add(new ObjectInterval(5, 500, 0));
        expected.add(new ObjectInterval(500, 1800, 2));
        expected.add(new ObjectInterval(1800, 2048, 0));
        expected.add(ObjectInterval.empty(2048, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        
        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 1024, 0));
        
        expected = new LinkedList<ObjectInterval>();
        expected.add(new ObjectInterval(0, 2048, 1));
        expected.add(ObjectInterval.empty(2048, 8192));
        tree.insert(new ObjectInterval(0, 2048, 1));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        // Test with full tree of height 2, where the first and the last leaf are shrinked
        tree = createFullTestTree();
        expected.clear();
        expected.add(new ObjectInterval(0, 500, 0));
        expected.add(new ObjectInterval(500, 4000, 7));
        expected.add(new ObjectInterval(4000, 4096, 6));
        expected.add(ObjectInterval.empty(4096, 8192));
        tree.insert(new ObjectInterval(500, 4000, 7));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        // Test with tree of height 1, where the whole range is overwritten
        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 1024, 0));
        tree.insert(new ObjectInterval(1024, 2048, 2));
        tree.insert(new ObjectInterval(0, 2048, 3));
        expected.clear();
        expected.add(new ObjectInterval(0, 2048, 3));
        expected.add(ObjectInterval.empty(2048, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
    }

    @Test
    public void testRetrieve() {
        LinkedList<ObjectInterval> expected = new LinkedList<ObjectInterval>();
        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 2048, 0));
        List<Interval> versions;

        tree.insert(new ObjectInterval(512, 1536, 1));

        // Retrieve the whole tree
        expected.add(new ObjectInterval(0, 512, 0));
        expected.add(new ObjectInterval(512, 1536, 1));
        expected.add(new ObjectInterval(1536, 2048, 0));
        expected.add(ObjectInterval.empty(2048, 8192));
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);

        // Retrieve only the root
        expected.clear();
        expected.add(new ObjectInterval(512, 1536, 1));
        versions = tree.getOverlapping(512, 1536);
        assertEquals(expected, versions);

        // Retrieve from right tree, but not the root
        expected.clear();
        expected.add(new ObjectInterval(1536, 2048, 0));
        versions = tree.getOverlapping(1536, 2048);
        assertEquals(expected, versions);

        // Retrieve from left tree, but not the root
        expected.clear();
        expected.add(new ObjectInterval(0, 512, 0));
        versions = tree.getOverlapping(0, 512);
        assertEquals(expected, versions);
    }

    @Test
    public final void testSlice() {
        LinkedList<ObjectInterval> expected = new LinkedList<ObjectInterval>();
        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 2048, 0));
        List<Interval> versions;

        tree.insert(new ObjectInterval(512, 1536, 1));
        expected.add(new ObjectInterval(0, 512, 0));
        expected.add(new ObjectInterval(512, 1536, 1));
        expected.add(new ObjectInterval(1536, 2000, 0));
        versions = tree.getSlice(0, 2000);
        assertEquals(expected, versions);

        // Retrieve from right tree, but not the root
        expected.clear();
        expected.add(new ObjectInterval(1550, 2048, 0));
        versions = tree.getSlice(1550, 2048);
        assertEquals(expected, versions);

        // Retrieve from left tree, but not the root
        expected.clear();
        expected.add(new ObjectInterval(0, 500, 0));
        versions = tree.getSlice(0, 500);
        assertEquals(expected, versions);

        // Retrieve surrounding
        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(1024, 2048, 1));
        expected.clear();
        expected.add(new ObjectInterval(0, 1024, -1));
        expected.add(new ObjectInterval(1024, 2048, 1));
        expected.add(new ObjectInterval(2048, 3072, -1));
        versions = tree.getSlice(0, 3072);
        assertEquals(expected, versions);
    }

    @Test
    public final void testResultAccumulation() {
        LinkedList<ObjectInterval> expected = new LinkedList<ObjectInterval>();
        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        List<Interval> versions;

        // Test to retrieve from empty tree
        versions = tree.getOverlapping(512, 768);
        expected.add(ObjectInterval.empty(512, 768));
        assertEquals(expected, versions);

        versions = tree.getSlice(512, 768);
        expected.clear();
        expected.add(ObjectInterval.empty(512, 768));
        assertEquals(expected, versions);

        // Test if empty intervals will be joined
        tree.insert(ObjectInterval.empty(0, 512));
        tree.insert(ObjectInterval.empty(512, 1000));
        tree.insert(ObjectInterval.empty(1000, 1024));
        // tree.insert(ObjectInterval.empty(512, 1024));
        expected.clear();
        expected.add(ObjectInterval.empty(0, 1024));
        versions = tree.getOverlapping(0, 1024);
        assertEquals(expected, versions);

        // Test to retrieve from tree with empty interval
        tree.insert(ObjectInterval.empty(0, 2048));
        expected.clear();
        expected.add(ObjectInterval.empty(0, 1024));
        versions = tree.getOverlapping(0, 1024);
        assertEquals(expected, versions);

        versions = tree.getSlice(0, 1024);
        expected.clear();
        expected.add(ObjectInterval.empty(0, 1024));
        assertEquals(expected, versions);

        // Test if results will be joined
        tree.insert(new ObjectInterval(0, 512, 1, 1, 0, 1024));
        tree.insert(new ObjectInterval(512, 1024, 1, 1, 0, 1024));
        expected.clear();
        expected.add(new ObjectInterval(0, 1024, 1, 1, 0, 1024));
        versions = tree.getOverlapping(0, 1024);
        assertEquals(expected, versions);
        versions = tree.getSlice(0, 1024);
        assertEquals(expected, versions);

        // Test on errors if a intervals version/id match, but not the operation range
        tree.insert(new ObjectInterval(0, 512, 1, 1, 0, 512));
        tree.insert(new ObjectInterval(512, 1024, 1, 1, 512, 1024));
        try {
            tree.getOverlapping(0, 1024);
            fail();
        } catch (AssertionError ex) {
            // expected
        }

        try {
            tree.getSlice(0, 1024);
            fail();
        } catch (AssertionError ex) {
            // expected
        }
        
    }

    @Test
    public final void testBalance() {
        AVLTreeIntervalVector tree;
                
        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 2048, 0));

        tree.insert(new ObjectInterval(0, 1024, 1));
        tree.insert(new ObjectInterval(0, 512, 2));
        tree.insert(new ObjectInterval(0, 256, 3));
        tree.insert(new ObjectInterval(0, 128, 4));
        tree.insert(new ObjectInterval(0, 64, 5));
        tree.insert(new ObjectInterval(0, 32, 6));
        assertEquals(0, tree.root.balance);

        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 2048, 0));

        tree.insert(new ObjectInterval(512, 1536, 1));
        tree.insert(new ObjectInterval(0, 5, 1));
        tree.insert(new ObjectInterval(500, 511, 1));
        tree.insert(new ObjectInterval(500, 2047, 2));
        assertEquals(1, Math.abs(tree.root.balance));
        
        
        // simple examples for double rotation
        // 1              3
        //  \_    =>    _/ \_ 
        //     5      1       5 
        // 3 _/
        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(1, 2, 1));
        tree.insert(new ObjectInterval(5, 6, 1));
        tree.insert(new ObjectInterval(3, 4, 1));
        assertEquals(0, tree.root.balance);
        
        //     5          3
        //   _/   =>    _/ \_ 
        // 1          1       5 
        //  \_3
        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(5, 6, 1));
        tree.insert(new ObjectInterval(1, 2, 1));
        tree.insert(new ObjectInterval(3, 4, 1));
        assertEquals(0, tree.root.balance);
        

        // complicated case with more levels
        tree = createFullTestTree();
        tree.insert(new ObjectInterval(0, 128, 7));
        tree.insert(new ObjectInterval(128, 256, 8));
        assertEquals(1, Math.abs(tree.root.balance));
    }

    @Test
    public void testSerialize() {
        LinkedList<ObjectInterval> expected = new LinkedList<ObjectInterval>();
        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();

        // Test if fragmented Intervals will be joined.
        tree.insert(new ObjectInterval(0, 1024, 0, 0, 0, 4096));
        tree.insert(new ObjectInterval(1024, 2048, 0, 0, 0, 4096));
        tree.insert(new ObjectInterval(2048, 4096, 0, 0, 0, 4096));
        expected.add(new ObjectInterval(0, 4096, 0, 0, 0, 4096));
        assertEquals(expected, tree.serialize());

        // Test if gaps are filled
        tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 512, 0));
        tree.insert(new ObjectInterval(1024, 1536, 0));
        expected.clear();
        expected.add(new ObjectInterval(0, 512, 0));
        expected.add(new ObjectInterval(512, 1024, -1));
        expected.add(new ObjectInterval(1024, 1536, 0));
        assertEquals(expected, tree.serialize());
    }

    @Test
    public void testTruncate() {
        List<Interval> expected = new LinkedList<Interval>();
        List<Interval> versions;

        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 10, 0));
        expected.add(new ObjectInterval(0, 10, 0));
        expected.add(ObjectInterval.empty(10, 20));

        // truncate without effect
        tree.truncate(20);
        assertEquals(10, tree.end);
        versions = tree.getOverlapping(0, 20);
        assertEquals(expected, versions);

        tree.truncate(10);
        assertEquals(10, tree.end);
        versions = tree.getOverlapping(0, 20);
        assertEquals(expected, versions);

        // truncate to less then 0 should throw an exception
        try {
            tree.truncate(-1);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        // simple truncate on a single interval
        tree.truncate(5);
        expected.clear();
        expected.add(new ObjectInterval(0, 5, 0));
        expected.add(ObjectInterval.empty(5, 20));
        assertEquals(5, tree.end);
        versions = tree.getOverlapping(0, 20);
        assertEquals(expected, versions);

        // simple truncate on a single interval to the begin
        tree.truncate(1);
        expected.clear();
        expected.add(new ObjectInterval(0, 1, 0));
        expected.add(ObjectInterval.empty(1, 20));
        assertEquals(1, tree.end);
        versions = tree.getOverlapping(0, 20);
        assertEquals(expected, versions);

        // test dropping the right tree
        tree = createFullTestTree();
        expected.clear();
        expected.addAll(tree.getSlice(0, 2048));
        expected.add(ObjectInterval.empty(2048, 8192));
        tree.truncate(2048);
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
        assertEquals(2, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on rightmost node
        tree = createFullTestTree();
        expected.clear();
        expected.addAll(tree.getSlice(0, 4000));
        expected.add(ObjectInterval.empty(4000, 8192));
        tree.truncate(4000);
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
        assertEquals(2, tree.root.height);
        assertEquals(0, tree.root.balance);

        // test truncating on leftmost right node (drops parent and its right tree)
        tree = createFullTestTree();
        expected.clear();
        expected.addAll(tree.getSlice(0, 2500));
        expected.add(ObjectInterval.empty(2500, 8192));
        tree.truncate(2500);
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
        assertEquals(2, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on the leftmost node (drops root and every other node)
        tree = createFullTestTree();
        expected.clear();
        expected.addAll(tree.getSlice(0, 500));
        expected.add(ObjectInterval.empty(500, 8192));
        tree.truncate(500);
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
        assertEquals(0, tree.root.height);
        assertEquals(0, tree.root.balance);

        // test truncating on the left root node (drops root and nodes right tree)
        tree = createFullTestTree();
        expected.clear();
        expected.addAll(tree.getSlice(0, 1000));
        expected.add(ObjectInterval.empty(1000, 8192));
        tree.truncate(1000);
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
        assertEquals(1, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on the rightmost left node
        tree = createFullTestTree();
        expected.clear();
        expected.addAll(tree.getSlice(0, 1500));
        expected.add(ObjectInterval.empty(1500, 8192));
        tree.truncate(1500);
        versions = tree.getOverlapping(0, 8192);
        assertEquals(expected, versions);
        assertEquals(1, tree.root.height);
        assertEquals(0, tree.root.balance);
    }

    @Test
    public void testOverwrites() {
        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        tree.insert(new ObjectInterval(0, 1024, 0));
        assertEquals(0, tree.getOverwrites());

        tree.insert(new ObjectInterval(1024, 2048, 1));
        assertEquals(0, tree.getOverwrites());

        tree.insert(new ObjectInterval(0, 1024, 2));
        assertEquals(1, tree.getOverwrites());

        tree.resetOverwrites();
        tree.insert(new ObjectInterval(0, 2048, 3));
        assertEquals(2, tree.getOverwrites());

        tree = createFullTestTree();
        tree.insert(new ObjectInterval(0, 4096, 7));
        assertEquals(7, tree.getOverwrites());

        tree = createFullTestTree();
        tree.insert(new ObjectInterval(500, 4000, 7));
        assertEquals(5, tree.getOverwrites());

        tree = createFullTestTree();
        tree.insert(new ObjectInterval(500, 4000, 7));
        assertEquals(5, tree.getOverwrites());

        // TODO(jdillmann): Test on unbalanced trees
    }

    AVLTreeIntervalVector createFullTestTree() {
        IntervalNode root = new IntervalNode(new ObjectInterval(1536, 2048, 3));
        IntervalNode node;

        node = new IntervalNode(new ObjectInterval(512, 1024, 1));
        node.left = new IntervalNode(new ObjectInterval(0, 512, 0));
        node.right = new IntervalNode(new ObjectInterval(1024, 1536, 2));
        node.checkHeight();
        root.left = node;

        node = new IntervalNode(new ObjectInterval(2560, 3584, 5));
        node.left = new IntervalNode(new ObjectInterval(2048, 2560, 4));
        node.right = new IntervalNode(new ObjectInterval(3584, 4096, 6));
        node.checkHeight();
        root.right = node;

        root.checkHeight();

        AVLTreeIntervalVector tree = new AVLTreeIntervalVector();
        tree.root = root;
        tree.maxVersion = 6;
        tree.end = 4096;

        return tree;
    }

    public static void printTree(AVLTreeIntervalVector vector) {
        printTree(vector.root);
    }

    public static void printTree(IntervalNode root) {
        List<List<String>> lines = new ArrayList<List<String>>();

        List<IntervalNode> level = new ArrayList<IntervalNode>();
        List<IntervalNode> next = new ArrayList<IntervalNode>();

        level.add(root);
        int nn = 1;

        int widest = 0;

        while (nn != 0) {
            List<String> line = new ArrayList<String>();

            nn = 0;

            for (IntervalNode n : level) {
                if (n == null) {
                    line.add(null);

                    next.add(null);
                    next.add(null);
                } else {
                    String aa = n.toString();
                    line.add(aa);
                    if (aa.length() > widest)
                        widest = aa.length();

                    next.add(n.left);
                    next.add(n.right);

                    if (n.left != null)
                        nn++;
                    if (n.right != null)
                        nn++;
                }
            }

            if (widest % 2 == 1)
                widest++;

            lines.add(line);

            List<IntervalNode> tmp = level;
            level = next;
            next = tmp;
            next.clear();
        }

        int perpiece = lines.get(lines.size() - 1).size() * (widest + 4);
        for (int i = 0; i < lines.size(); i++) {
            List<String> line = lines.get(i);
            int hpw = (int) Math.floor(perpiece / 2f) - 1;

            if (i > 0) {
                for (int j = 0; j < line.size(); j++) {

                    // split node
                    char c = ' ';
                    if (j % 2 == 1) {
                        if (line.get(j - 1) != null) {
                            c = (line.get(j) != null) ? '┴' : '┘';
                        } else {
                            if (j < line.size() && line.get(j) != null)
                                c = '└';
                        }
                    }
                    System.out.print(c);

                    // lines and spaces
                    if (line.get(j) == null) {
                        for (int k = 0; k < perpiece - 1; k++) {
                            System.out.print(" ");
                        }
                    } else {

                        for (int k = 0; k < hpw; k++) {
                            System.out.print(j % 2 == 0 ? " " : "─");
                        }
                        System.out.print(j % 2 == 0 ? "┌" : "┐");
                        for (int k = 0; k < hpw; k++) {
                            System.out.print(j % 2 == 0 ? "─" : " ");
                        }
                    }
                }
                System.out.println();
            }

            // print line of numbers
            for (int j = 0; j < line.size(); j++) {

                String f = line.get(j);
                if (f == null)
                    f = "";
                int gap1 = (int) Math.ceil(perpiece / 2f - f.length() / 2f);
                int gap2 = (int) Math.floor(perpiece / 2f - f.length() / 2f);

                // a number
                for (int k = 0; k < gap1; k++) {
                    System.out.print(" ");
                }
                System.out.print(f);
                for (int k = 0; k < gap2; k++) {
                    System.out.print(" ");
                }
            }
            System.out.println();

            perpiece /= 2;
        }

    }
}
