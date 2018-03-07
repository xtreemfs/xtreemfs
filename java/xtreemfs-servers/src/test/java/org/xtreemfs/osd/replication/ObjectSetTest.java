/*
 * Copyright (c) 2009-2011 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.TestHelper;

/**
 * 
 * <br>
 * 02.07.2009
 */
public class ObjectSetTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    List<ObjectSet> changeableSets;
    List<ObjectSet> fixedSets;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        changeableSets = new ArrayList<ObjectSet>();
        fixedSets = new ArrayList<ObjectSet>();
        // set 1
        changeableSets.add(new ObjectSet(1, 0, 100));
        changeableSets.add(new ObjectSet(1, 0, 100));
        // set 2
        fixedSets.add(new ObjectSet(1, 0, 100));
        fixedSets.add(new ObjectSet(1, 0, 100));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSerialization() throws Exception {
        // test if equals after serialization
        for (ObjectSet set : changeableSets) {
            fillObjectSetRandom(set, 100, 100);
            byte[] serialized = set.getSerializedBitSet();

            ObjectSet deserialized = new ObjectSet(set.getStripeWidth(), 0, serialized);
            assertTrue(set.equals(deserialized));

            // test wrong deserialize
            ArrayList list = new ArrayList();
            byte[] otherObject = serialize(list);
            try {
                deserialized = new ObjectSet(set.getStripeWidth(), 0, otherObject);
                fail("should throw");
            } catch (Exception e) {
                // correct
            }
        }
    }

    @Test
    public void testStripeWidth() throws Exception {
        long[] stripeWidth1 = { 1, 2, 4, 5, 6, 7, 8, 10 };
        long[] stripeWidth2 = { 4, 6, 8, 10, 14, 20, 22, 24 };
        long[] stripeWidth3_1 = { 1, 4, 7, 10, 13, 16, 22, 31 };
        long[] stripeWidth3_2 = { 0, 3, 6, 12, 21, 24, 33, 36 };

        ObjectSet setWidth1 = new ObjectSet(1, 0, 100);
        ObjectSet setWidth2 = new ObjectSet(2, 0, 100);
        ObjectSet setWidth3_1 = new ObjectSet(3, 1, 100);
        ObjectSet setWidth3_2 = new ObjectSet(3, 0, 100);

        // fill sets
        for (long object : stripeWidth1)
            setWidth1.add(object);
        for (long object : stripeWidth2)
            setWidth2.add(object);
        for (long object : stripeWidth3_1)
            setWidth3_1.add(object);
        for (long object : stripeWidth3_2)
            setWidth3_2.add(object);

        // check if the sets only contain the correct values
        // set 1
        assertEquals(stripeWidth1.length, setWidth1.size());
        Iterator<Long> it = setWidth1.iterator();
        for (long object : stripeWidth1)
            if (it.hasNext())
                assertEquals(object, it.next().longValue());

        // set 2
        assertEquals(stripeWidth2.length, setWidth2.size());
        it = setWidth2.iterator();
        for (long object : stripeWidth2)
            if (it.hasNext())
                assertEquals(object, it.next().longValue());

        // set 3
        assertEquals(stripeWidth3_1.length, setWidth3_1.size());
        it = setWidth3_1.iterator();
        for (long object : stripeWidth3_1)
            if (it.hasNext())
                assertEquals(object, it.next().longValue());

        // set 4
        assertEquals(stripeWidth3_2.length, setWidth3_2.size());
        it = setWidth3_2.iterator();
        for (long object : stripeWidth3_2)
            if (it.hasNext())
                assertEquals(object, it.next().longValue());

        // test false firstObjectNo
        ObjectSet setWidth3_false = new ObjectSet(3, 0, 100);
        for (long object : stripeWidth3_1)
            setWidth3_false.add(object);
        assertEquals(stripeWidth3_1.length, setWidth3_false.size());
        it = setWidth3_false.iterator();
        for (long object : stripeWidth3_1)
            if (it.hasNext())
                assertNotSame(object, it.next().longValue());
    }

    /**
     * Serializes this object.
     * 
     * @param set
     * @return
     * @throws IOException
     */
    protected static byte[] serialize(Object set) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(set);
        oos.flush();
        oos.close();
        bos.close();
        return bos.toByteArray();
    }

    @Test
    public void testEquals() throws Exception {
        for (ObjectSet set : changeableSets)
            fillObjectSetRandom(set, 100, 100);
        for (ObjectSet set : fixedSets)
            fillObjectSetRandom(set, 100, 100);

        for (ObjectSet set : changeableSets) {
            for (ObjectSet set2 : fixedSets) {
                assertTrue(set.equals(set2));
            }
        }
        ObjectSet falseSet = new ObjectSet(2, 0, 10);
        fillObjectSetRandom(falseSet, 20, 20);
        assertFalse(falseSet.equals(changeableSets.get(0)));
    }

    @Test
    public void testIntersection() throws Exception {
        long[] objectsInList1 = { 10, 20, 40, 50, 60, 70, 80, 100 };
        long[] objectsInList2 = { 0, 30, 50, 70, 90, 100 };
        long[] intersection = { 50, 70, 100 };

        // fill set2
        for (ObjectSet set : fixedSets)
            for (long object : objectsInList2)
                set.add(object);

        // intersection
        for (ObjectSet set2 : fixedSets) {
            for (ObjectSet set : changeableSets) {
                // renew set1
                set.clear();
                for (long object : objectsInList1)
                    set.add(object);

                set.intersection(set2);
                assertEquals(intersection.length, set.size());
                for (long object : intersection)
                    assertTrue(set.contains(object));
            }
        }
    }

    @Test
    public void testUnion() throws Exception {
        long[] objectsInList1 = { 10, 20, 40, 50, 60, 70, 80, 100 };
        long[] objectsInList2 = { 0, 30, 50, 70, 90, 100 };
        long[] union = { 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        // fill set2
        for (ObjectSet set : fixedSets)
            for (long object : objectsInList2)
                set.add(object);

        // intersection
        for (ObjectSet set2 : fixedSets) {
            for (ObjectSet set : changeableSets) {
                // renew set1
                set.clear();
                for (long object : objectsInList1)
                    set.add(object);

                set.union(set2);
                assertEquals(union.length, set.size());
                for (long object : union)
                    assertTrue(set.contains(object));
            }
        }
    }

    /**
     * @param fillRate
     *            in percent
     * @throws Exception
     */
    public static void fillObjectSetRandom(ObjectSet set, long objectsCount, int fillRate) {
        Random random = new Random();
        // System.out.println("fill object set ...");
        for (long i = 0; i < objectsCount; i++) {
            if (random.nextInt(100) < fillRate) // approx. x% filled
                set.add(i);
        }
    }
}
