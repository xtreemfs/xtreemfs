/*
 * Copyright (c) 2015 by Jan Fajerski, Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.xtreemfs.foundation.IntervalVersionAVLTree.IntervalNode;
import org.xtreemfs.foundation.IntervalVersionTree.Interval;

/**
 * @author Jan Fajerski
 */
public class IntervalVersionAVLTreeTest {
    @Test
    public final void testBasicNonOverlappingIntervals() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        LinkedList<Interval> versions;

        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 1024);
        expected.add(new Interval(0, 1024));
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);

        tree.insert(1024, 2048, 1);
        expected.add(new Interval(1024, 2048, 1));
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);

        tree.insert(2048, 4096, 2);
        expected.add(new Interval(2048, 4096, 2));
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);

        tree.insert(0, 1024, 3);
        expected.get(0).version = 3;
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);
    }

    @Test
    public final void testInsertOfOverlappingIntervals() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(1024, 4096, 1);
        expected.add(new Interval(0, 1024, 0));
        expected.add(new Interval(1024, 4096, 1));
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);

        tree.insert(0, 512, 1);
        expected.addFirst(new Interval(0, 512, 1));
        expected.get(1).begin = 512;
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);
    }

    @Test
    public final void testInsertOfIntervalInsideOfExisting() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(512, 1536, 1);
        expected.add(new Interval(0, 512, 0));
        expected.add(new Interval(512, 1536, 1));
        expected.add(new Interval(1536, 2048, 0));
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);
    }

    @Test
    public final void testInsertOfSurroundingInterval() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(512, 1536, 1);
        tree.insert(500, 1800, 2);
        expected.add(new Interval(0, 500, 0));
        expected.add(new Interval(500, 1800, 2));
        expected.add(new Interval(1800, 2048, 0));
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);

        expected = new LinkedList<Interval>();
        tree = new IntervalVersionAVLTree(0, 2048);
        tree.insert(512, 1536, 1);
        tree.insert(0, 5, 1);
        tree.insert(500, 512, 1);
        tree.insert(500, 1800, 2);
        expected.add(new Interval(0, 5, 1));
        expected.add(new Interval(5, 500, 0));
        expected.add(new Interval(500, 1800, 2));
        expected.add(new Interval(1800, 2048, 0));
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);

        tree = new IntervalVersionAVLTree(0, 1024);
        expected = new LinkedList<Interval>();
        expected.add(new Interval(0, 2048, 1));
        tree.insert(0, 2048, 1);
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);

        // Test with full tree of height 2, where the first and the last leaf are shrinked
        tree = createFullTestTree();
        expected.clear();
        expected.add(new Interval(0, 500, 0));
        expected.add(new Interval(500, 4000, 7));
        expected.add(new Interval(4000, 4096, 6));
        tree.insert(new Interval(500, 4000, 7));
        assertEquals(expected, tree.getVersions(0, 8192));
        
        // Test with tree of height 1, where the whole range is overwritten
        tree = new IntervalVersionAVLTree();
        tree.insert(new Interval(0, 1024, 0));
        tree.insert(new Interval(1024, 2048, 2));
        tree.insert(new Interval(0, 2048, 3));
        expected.clear();
        expected.add(new Interval(0, 2048, 3));
        assertEquals(expected, tree.getVersions(0, 8192));
    }


    @Test
    public void testRetrieve() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 2048);
        LinkedList<Interval> versions;

        tree.insert(512, 1536, 1);

        // Retrieve the whole tree
        expected.add(new Interval(0, 512, 0));
        expected.add(new Interval(512, 1536, 1));
        expected.add(new Interval(1536, 2048, 0));
        versions = tree.getVersions(0, 8192);
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
        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 2048);
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
        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 2048);
        tree.insert(0, 1024, 1);
        tree.insert(0, 512, 2);
        tree.insert(0, 256, 3);
        tree.insert(0, 128, 4);
        tree.insert(0, 64, 5);
        tree.insert(0, 32, 6);
        assertEquals(0, tree.root.balance);

        tree = new IntervalVersionAVLTree(0, 2048);
        tree.insert(512, 1536, 1);
        tree.insert(0, 5, 1);
        tree.insert(500, 511, 1);
        tree.insert(500, 2047, 2);
        assertEquals(1, Math.abs(tree.root.balance));
        
        tree = createFullTestTree();
        tree.insert(new Interval(0, 128, 7));
        tree.insert(new Interval(128, 256, 8));
        assertEquals(1, Math.abs(tree.root.balance));

    }

    @Test
    public void testCompact() {
        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 1);
        tree.insert(new Interval(1, 2, 0));
        tree.insert(new Interval(2, 3, 0));

        tree = createFullTestTree();
        printTree(tree.root);

        System.out.println(tree);
        tree = tree.compact();
        System.out.println(tree);
        fail();
    }

    @Test
    public void testCompactRetrieve() {
        LinkedList<Interval> expected = new LinkedList<Interval>();
        LinkedList<Interval> versions;

        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 1024);
        tree.insert(1024, 2048, 0);
        tree.insert(2048, 4096, 0);

        expected.add(new Interval(0, 4096, 0));
        versions = tree.getVersions(0, 8192);
        assertEquals(expected, versions);
    }

    @Test
    public void testTruncate() {
        LinkedList<Interval> expected = new LinkedList<Interval>();

        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 10);
        expected.add(new Interval(0, 10, 0));

        // truncate without effect
        tree.truncate(20);
        assertEquals(10, tree.highest);
        assertEquals(expected, tree.getVersions(0, 20));

        tree.truncate(10);
        assertEquals(10, tree.highest);
        assertEquals(expected, tree.getVersions(0, 20));

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
        expected.add(new Interval(0, 5, 0));
        assertEquals(5, tree.highest);
        assertEquals(expected, tree.getVersions(0, 20));

        // simple truncate on a single interval to the begin
        tree.truncate(1);
        expected.clear();
        expected.add(new Interval(0, 1, 0));
        assertEquals(1, tree.highest);
        assertEquals(expected, tree.getVersions(0, 20));


        // test dropping the right tree
        tree = createFullTestTree();
        expected = tree.getVersions(0, 2048);
        tree.truncate(2048);
        assertEquals(expected, tree.getVersions(0, 8192));
        assertEquals(2, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on rightmost node
        tree = createFullTestTree();
        expected = tree.getVersions(0, 4000);
        tree.truncate(4000);
        assertEquals(expected, tree.getVersions(0, 8192));
        assertEquals(2, tree.root.height);
        assertEquals(0, tree.root.balance);

        // test truncating on leftmost right node (drops parent and its right tree)
        tree = createFullTestTree();
        expected = tree.getVersions(0, 2500);
        tree.truncate(2500);
        assertEquals(expected, tree.getVersions(0, 8192));
        assertEquals(2, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on the leftmost node (drops root and every other node)
        tree = createFullTestTree();
        expected = tree.getVersions(0, 500);
        tree.truncate(500);
        assertEquals(expected, tree.getVersions(0, 8192));
        assertEquals(0, tree.root.height);
        assertEquals(0, tree.root.balance);

        // test truncating on the left root node (drops root and nodes right tree)
        tree = createFullTestTree();
        expected = tree.getVersions(0, 1000);
        tree.truncate(1000);
        assertEquals(expected, tree.getVersions(0, 8192));
        assertEquals(1, tree.root.height);
        assertEquals(1, Math.abs(tree.root.balance));

        // test truncating on the rightmost left node
        tree = createFullTestTree();
        expected = tree.getVersions(0, 1500);
        tree.truncate(1500);
        assertEquals(expected, tree.getVersions(0, 8192));
        assertEquals(1, tree.root.height);
        assertEquals(0, tree.root.balance);
    }

    IntervalVersionAVLTree createFullTestTree() {
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

        IntervalVersionAVLTree tree = new IntervalVersionAVLTree(0, 4096);
        tree.root = root;
        return tree;
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
