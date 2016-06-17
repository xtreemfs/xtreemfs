/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.AttachmentInterval;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.osd.ec.ECPolicy.MutableInterval;
import org.xtreemfs.test.TestHelper;

public class VectorRecoveryTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    @Test
    public void testSimpleRecovery() {
        LinkedList<AttachmentInterval> expected = new LinkedList<AttachmentInterval>();
        IntervalVector[] curVectors;
        List<? extends Interval> result;
        Iterator curVectorPerms;

        AVLTreeIntervalVector iv1 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv2 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv3 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv4 = new AVLTreeIntervalVector();

        ObjectInterval interval;
        interval = new ObjectInterval(0, 12, 1, 1);
        iv1.insert(interval);
        iv2.insert(interval);
        iv3.insert(interval);
        iv4.insert(interval);

        // Test prefix overwrite
        interval = new ObjectInterval(0, 2, 2, 2);
        iv2.insert(interval);
        iv4.insert(interval);

        expected.clear();
        expected.add(new AttachmentInterval(0, 2, 2, 2, 1));
        expected.add(new AttachmentInterval(2, 12, 1, 1, 2));

        curVectors = new IntervalVector[] { iv1, iv2 };
        curVectorPerms = new Permute(curVectors);
        while (curVectorPerms.hasNext()) {
            IntervalVector[] curVectorPerm = (IntervalVector[]) curVectorPerms.next();
            result = ECPolicy.recoverVector(curVectorPerm, null);
            assertEqualsWithAttachment(expected, result);
        }

        // Test suffix overwrite
        interval = new ObjectInterval(10, 12, 2, 3);
        iv3.insert(interval);
        iv4.insert(interval);

        expected.clear();
        expected.add(new AttachmentInterval(0, 10, 1, 1, 2));
        expected.add(new AttachmentInterval(10, 12, 2, 3, 1));
        curVectors = new IntervalVector[] { iv1, iv3 };
        curVectorPerms = new Permute(curVectors);
        while (curVectorPerms.hasNext()) {
            IntervalVector[] curVectorPerm = (IntervalVector[]) curVectorPerms.next();
            result = ECPolicy.recoverVector(curVectorPerm, null);
            assertEqualsWithAttachment(expected, result);
        }

        // Test suffix and prefix overwrite from different vectors
        expected.clear();
        expected.add(new AttachmentInterval(0, 2, 2, 2, 1));
        expected.add(new AttachmentInterval(2, 10, 1, 1, 3));
        expected.add(new AttachmentInterval(10, 12, 2, 3, 1));
        curVectors = new IntervalVector[] { iv1, iv2, iv3 };

        curVectorPerms = new Permute(curVectors);
        while (curVectorPerms.hasNext()) {
            IntervalVector[] curVectorPerm = (IntervalVector[]) curVectorPerms.next();
            result = ECPolicy.recoverVector(curVectorPerm, null);
            assertEqualsWithAttachment(expected, result);
        }

        // Test overlapping overwrite of every other interval
        interval = new ObjectInterval(1, 11, 3, 4);
        iv4.insert(interval);

        expected.clear();
        expected.add(new AttachmentInterval(0, 1, 2, 2, 2));
        expected.add(new AttachmentInterval(1, 11, 3, 4, 1));
        expected.add(new AttachmentInterval(11, 12, 2, 3, 2));
        curVectors = new IntervalVector[] { iv1, iv2, iv3, iv4 };

        curVectorPerms = new Permute(curVectors);
        while (curVectorPerms.hasNext()) {
            IntervalVector[] curVectorPerm = (IntervalVector[]) curVectorPerms.next();
            result = ECPolicy.recoverVector(curVectorPerm, null);
            assertEqualsWithAttachment(expected, result);
        }
        
        // Test complete overwrite (needs to merge some results)
        interval = new ObjectInterval(0, 12, 4, 5);
        iv4.insert(interval);

        expected.clear();
        expected.add(new AttachmentInterval(0, 12, 4, 5, 1));
        curVectors = new IntervalVector[] { iv1, iv2, iv3, iv4 };

        curVectorPerms = new Permute(curVectors);
        while (curVectorPerms.hasNext()) {
            IntervalVector[] curVectorPerm = (IntervalVector[]) curVectorPerms.next();
            result = ECPolicy.recoverVector(curVectorPerm, null);
            assertEqualsWithAttachment(expected, result);
        }
    }

    static ArrayList<MutableInterval> cloneResultList(ArrayList<MutableInterval> result) {
        ArrayList<MutableInterval> clone = new ArrayList<MutableInterval>(result.size());
        for (MutableInterval i : result)
            clone.add(i.clone());
        return clone;
    }


    @Test
    public void testSimpleRecoveryWithExisting() {
        LinkedList<AttachmentInterval> expected = new LinkedList<AttachmentInterval>();
        LinkedList<AttachmentInterval> exExpected = new LinkedList<AttachmentInterval>();
        IntervalVector[] curVectors;
        ArrayList<MutableInterval> exResult;
        List<? extends Interval> result;
        Iterator curVectorPerms;

        AVLTreeIntervalVector iv1 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv2 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv3 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv4 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv5 = new AVLTreeIntervalVector();

        ObjectInterval interval;
        interval = new ObjectInterval(0, 12, 1, 1);
        iv1.insert(interval);
        iv2.insert(interval);
        iv3.insert(interval);
        iv4.insert(interval);

        // prefix overwrite
        interval = new ObjectInterval(0, 2, 2, 2);
        iv2.insert(interval);
        iv4.insert(interval);

        // suffix overwrite
        interval = new ObjectInterval(10, 12, 2, 3);
        iv3.insert(interval);
        iv4.insert(interval);

        // middle overwrite
        interval = new ObjectInterval(4, 6, 2, 4);
        iv4.insert(interval);

        exExpected.clear();
        exExpected.add(new AttachmentInterval(0, 2, 2, 2, 2));
        exExpected.add(new AttachmentInterval(2, 4, 1, 1, 4));
        exExpected.add(new AttachmentInterval(4, 6, 2, 4, 1));
        exExpected.add(new AttachmentInterval(6, 10, 1, 1, 4));
        exExpected.add(new AttachmentInterval(10, 12, 2, 3, 2));

        ArrayList<MutableInterval> exResultBase = ECPolicy.recoverVector(
                new IntervalVector[] { iv1, iv2, iv3, iv4, iv5 }, null);
        assertEqualsWithAttachment(exExpected, exResultBase);
        // System.out.println(exResultBase);

        AVLTreeIntervalVector niv1 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector niv2 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector niv3 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector niv4 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector niv5 = new AVLTreeIntervalVector();

        // Since they are non overlapping, add every interval to the next vector
        interval = new ObjectInterval(0, 2, 2, 2);
        niv1.insert(interval);
        interval = new ObjectInterval(10, 12, 2, 3);
        niv1.insert(interval);
        interval = new ObjectInterval(4, 6, 2, 4);
        niv1.insert(interval);

        // Add the first (overlapping and in the result splitted) interval to next 5
        interval = new ObjectInterval(0, 12, 1, 1);
        niv5.insert(interval);

        expected.clear();
        expected.add(new AttachmentInterval(0, 12, -1, -1, -1));

        exExpected.clear();
        exExpected.add(new AttachmentInterval(0, 2, 2, 2, 3));
        exExpected.add(new AttachmentInterval(2, 4, 1, 1, 5));
        exExpected.add(new AttachmentInterval(4, 6, 2, 4, 2));
        exExpected.add(new AttachmentInterval(6, 10, 1, 1, 5));
        exExpected.add(new AttachmentInterval(10, 12, 2, 3, 3));

        curVectors = new IntervalVector[] { niv1, niv5 };
        curVectorPerms = new Permute(curVectors);
        while (curVectorPerms.hasNext()) {
            IntervalVector[] curVectorPerm = (IntervalVector[]) curVectorPerms.next();
            exResult = cloneResultList(exResultBase);
            result = ECPolicy.recoverVector(curVectorPerm, exResult);
            // System.out.println(Arrays.deepToString(curVectorPerm));
            // System.out.println(result);
            // System.out.println(exResult);
            assertEqualsWithAttachment(expected, result);
            assertEqualsWithAttachment(exExpected, exResult);
        }

        // Add the first (overlapping and in the result splitted) interval to next 5
        interval = new ObjectInterval(0, 12, 1, 1);
        niv5.insert(interval);

        // Test overlapping overwrite of every other interval
        niv5.insert(new ObjectInterval(0, 12));
        interval = new ObjectInterval(1, 11, 3, 4);
        niv5.insert(interval);

        expected.clear();
        expected.add(new AttachmentInterval(0, 1, -1, -1, -1));
        expected.add(new AttachmentInterval(1, 11, 3, 4, 1));
        expected.add(new AttachmentInterval(11, 12, -1, -1, -1));

        exExpected.clear();
        exExpected.add(new AttachmentInterval(0, 2, 2, 2, 3));
        exExpected.add(new AttachmentInterval(2, 4, 1, 1, 4));
        exExpected.add(new AttachmentInterval(4, 6, 2, 4, 2));
        exExpected.add(new AttachmentInterval(6, 10, 1, 1, 4));
        exExpected.add(new AttachmentInterval(10, 12, 2, 3, 3));

        // System.out.println(niv5.serialize());

        curVectors = new IntervalVector[] { niv1, niv5 };
        curVectorPerms = new Permute(curVectors);
        while (curVectorPerms.hasNext()) {
            IntervalVector[] curVectorPerm = (IntervalVector[]) curVectorPerms.next();
            exResult = cloneResultList(exResultBase);
            result = ECPolicy.recoverVector(curVectorPerm, exResult);
            // System.out.println(Arrays.deepToString(curVectorPerm));
            // System.out.println(result);
            // System.out.println(exResult);
            assertEqualsWithAttachment(expected, result);
            assertEqualsWithAttachment(exExpected, exResult);
        }

    }

    @Test
    public void testIncompleteOp() {
        class OpObjectInterval extends ObjectInterval {
            final long opStart;
            final long opEnd;

            public OpObjectInterval(long start, long end, long version, long id, long opStart, long opEnd) {
                super(start, end, version, id);
                this.opStart = opStart;
                this.opEnd = opEnd;
            }

            @Override
            public long getOpStart() {
                return opStart;
            }

            @Override
            public long getOpEnd() {
                return opEnd;
            }
        }

        LinkedList<AttachmentInterval> expected = new LinkedList<AttachmentInterval>();
        LinkedList<AttachmentInterval> exExpected = new LinkedList<AttachmentInterval>();
        IntervalVector[] curVectors;
        ArrayList<MutableInterval> exResult;
        List<? extends Interval> result;
        Iterator curVectorPerms;
        ObjectInterval interval;

        interval = new OpObjectInterval(0, 6, 1, 1, 0, 12);
        assertFalse(interval.isOpComplete());



        AVLTreeIntervalVector iv1 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector iv2 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector niv1 = new AVLTreeIntervalVector();
        AVLTreeIntervalVector niv2 = new AVLTreeIntervalVector();

        interval = new ObjectInterval(0, 6, 1, 1);
        iv1.insert(interval);
        iv2.insert(interval);

        interval = new ObjectInterval(6, 12, 1, 2);
        iv1.insert(interval);
        iv2.insert(interval);

        interval = new OpObjectInterval(0, 6, 2, 3, 0, 12);
        niv1.insert(interval);


        exExpected.clear();
        exExpected.add(new AttachmentInterval(0, 6, 1, 1, 2));
        exExpected.add(new AttachmentInterval(6, 12, 1, 2, 2));

        ArrayList<MutableInterval> exResultBase = ECPolicy
                .recoverVector(new IntervalVector[] { iv1, iv2 }, null);
        assertEqualsWithAttachment(exExpected, exResultBase);

        expected.clear();
        expected.add(new AttachmentInterval(0, 6, -1, -1, -1));

        curVectors = new IntervalVector[] { niv1, niv2 };
        curVectorPerms = new Permute(curVectors);
        while (curVectorPerms.hasNext()) {
            IntervalVector[] curVectorPerm = (IntervalVector[]) curVectorPerms.next();
            exResult = cloneResultList(exResultBase);
            result = ECPolicy.recoverVector(curVectorPerm, exResult);
            assertEqualsWithAttachment(expected, result);
            assertEqualsWithAttachment(exExpected, exResult);
        }
    }



    void assertEqualsWithAttachment(List<? extends Interval> expected, List<? extends Interval> actual) {
        int i = -1;
        try {
            assertEquals(expected.size(), actual.size());
            for (i = 0; i < actual.size(); i++) {
                // IntervalMsg i1 = expected.get(i);
                // IntervalMsg i2 = result.get(i);
                assertEquals(expected.get(i), actual.get(i));
                assertEquals(expected.get(i).getAttachment(), actual.get(i).getAttachment());
            }
        } catch (AssertionError ex) {
            System.out.println(expected);
            System.out.println(actual);
            throw new AssertionError("Results first differ at element [" + i + "]", ex);
        }
    }


    /*
     * Found at http://stackoverflow.com/a/2920349 Originally from:
     * http://cs.fit.edu/~ryan/java/programs/combinations/Permute-java.html
     */
    static class Permute implements Iterator {

       private final int size;
       private final Object [] elements;  // copy of original 0 .. size-1
       private final Object ar;           // array for output,  0 .. size-1
       private final int [] permutation;  // perm of nums 1..size, perm[0]=0

       private boolean next = true;

       // int[], double[] array won't work :-(
       public Permute (Object [] e) {
          size = e.length;
          elements = new Object [size];    // not suitable for primitives
          System.arraycopy (e, 0, elements, 0, size);
          ar = Array.newInstance (e.getClass().getComponentType(), size);
          System.arraycopy (e, 0, ar, 0, size);
          permutation = new int [size+1];
          for (int i=0; i<size+1; i++) {
             permutation [i]=i;
          }
       }

       private void formNextPermutation () {
          for (int i=0; i<size; i++) {
             // i+1 because perm[0] always = 0
             // perm[]-1 because the numbers 1..size are being permuted
             Array.set (ar, i, elements[permutation[i+1]-1]);
          }
       }

       public boolean hasNext() {
          return next;
       }

       public void remove() throws UnsupportedOperationException {
          throw new UnsupportedOperationException();
       }

       private void swap (final int i, final int j) {
          final int x = permutation[i];
          permutation[i] = permutation [j];
          permutation[j] = x;
       }

       // does not throw NoSuchElement; it wraps around!
       public Object next() throws NoSuchElementException {

          formNextPermutation ();  // copy original elements

          int i = size-1;
          while (permutation[i]>permutation[i+1]) i--;

          if (i==0) {
             next = false;
             for (int j=0; j<size+1; j++) {
                permutation [j]=j;
             }
             return ar;
          }

          int j = size;

          while (permutation[i]>permutation[j]) j--;
          swap (i,j);
          int r = size;
          int s = i+1;
          while (r>s) { swap(r,s); r--; s++; }

          return ar;
       }
    }

}
