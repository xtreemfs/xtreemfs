/*
 * Copyright (c) ${year} by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation;

import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import org.xtreemfs.foundation.IntervalVersionTree.Interval;

import java.util.LinkedList;

/**
 * @author Jan Fajerski
 */
public class IntervalVersionTreeTest {
    @Test
    public final void testBasicNonOverlappingIntervals() {
        LinkedList<Interval> expected = new LinkedList<>();
        LinkedList<Interval> versions;

        IntervalVersionTree tree = new IntervalVersionTree(0, 1023);
        expected.add(new Interval(0, 1023));
        versions = tree.getVersions(0, 1023);
        assertThat(expected, equalTo(versions));

        tree.insert(1024, 2047, 1);
        expected.add(new Interval(1024, 2047, 1));
        versions = tree.getVersions(0, 2047);
        assertThat(expected, equalTo(versions));

        tree.insert(2048, 4095, 1);
        expected.add(new Interval(2048, 4095, 1));
        versions = tree.getVersions(0, 4095);
        assertThat(expected, equalTo(versions));

        tree.insert(0, 1023, 1);
        expected.get(0).version = 1;
        versions = tree.getVersions(0,4095);
        assertThat(expected, equalTo(versions));
    }

    @Test
    public final void testInsertOfOverlappingIntervals() {
        LinkedList<Interval> expected = new LinkedList<>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);
        LinkedList<Interval> versions;

        tree.insert(1024, 4095, 1);
        expected.add(new Interval(0, 1023, 0));
        expected.add(new Interval(1024, 4095, 1));
        versions = tree.getVersions(0, 4095);
        assertThat(expected, equalTo(versions));

        tree.insert(0, 511, 1);
        expected.get(0).begin = 512;
        expected.add(new Interval(0, 511, 1));
        versions = tree.getVersions(0, 4095);
        assert(expected.containsAll(versions));
    }

    @Test
    public final void testInsertOfIntervalInsideOfExisting() {
        LinkedList<Interval> expected = new LinkedList<>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);
        LinkedList<Interval> versions;

        tree.insert(512, 1535, 1);
        expected.add(new Interval(0, 511, 0));
        expected.add(new Interval(512, 1535, 1));
        expected.add(new Interval(1536, 2047, 0));
        versions = tree.getVersions(0, 2047);
        assert(expected.containsAll(versions));
    }

    @Test
    public final void testInsertOfSurroundingInterval() {
        LinkedList<Interval> expected = new LinkedList<>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);
        LinkedList<Interval> versions;

        tree.insert(512, 1535, 1);
        tree.insert(500, 1800, 2);
        expected.add(new Interval(0, 499, 0));
        expected.add(new Interval(500, 1800, 2));
        expected.add(new Interval(1801, 2047, 0));
        versions = tree.getVersions(0, 2047);
        assert(expected.containsAll(versions));

        expected = new LinkedList<>();
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
        assert(expected.containsAll(versions));
    }

    @Test
    public final void testPartialRetrieve() {
        LinkedList<Interval> expected = new LinkedList<>();
        IntervalVersionTree tree = new IntervalVersionTree(0, 2047);
        LinkedList<Interval> versions;

        tree.insert(512, 1535, 1);
        expected.add(new Interval(0, 511, 0));
        expected.add(new Interval(512, 1535, 1));
        expected.add(new Interval(1536, 2000, 0));
        versions = tree.getVersions(0, 2000);
        assert(expected.containsAll(versions));
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
    }
}
