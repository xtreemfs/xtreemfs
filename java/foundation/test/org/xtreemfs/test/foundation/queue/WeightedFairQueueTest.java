/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.foundation.queue;

import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.foundation.queue.WeightedFairQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class WeightedFairQueueTest {
    WeightedFairQueue<String, Integer> queue;
    int capacity = 100;
    @Before
    public void setUp() throws Exception {
        queue = new WeightedFairQueue<String, Integer>(capacity, new WeightedFairQueue.WFQElementInformationProvider<String, Integer>() {
            @Override
            public int getRequestCost(Integer element) {
                return 1;
            }

            @Override
            public String getQualityClass(Integer element) {
                return element.toString();
            }

            @Override
            public int getWeight(String element) {
                return Integer.parseInt(element);
            }
        });
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(queue.isEmpty());
        assertTrue(queue.add(123));
        assertFalse(queue.isEmpty());
        queue.take();
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(queue.size(), 0);

        for(int i = 0; i < capacity; i++) {
            assertEquals(queue.size(), i);
            queue.add(1);
        }

        for(int i = capacity; i > 0; i--) {
            assertEquals(queue.size(), i);
            queue.take();
        }

        assertEquals(queue.size(), 0);
    }

    @Test
    public void testRemainingCapacity() throws Exception {
        int capacity = 100;
        assertEquals(queue.remainingCapacity(), capacity);

        for(int i = 0; i < capacity; i++) {
            assertTrue(queue.add(i));
            assertEquals(queue.remainingCapacity(), capacity - i - 1);
        }

        try{
            queue.add(1);
        } catch (IllegalStateException e) {
            assertEquals(queue.remainingCapacity(), 0);
        }
    }

    @Test
    public void testTake() throws Exception {
        int element = 1;

        assertTrue(queue.add(element));
        assertEquals(element, queue.take());
    }

    @Test
    public void testAdd() throws Exception {
        // Add elements to reach capacity limits
        for(int i = 0; i < capacity; i++) {
            try {
                assertTrue(queue.add(i));
            } catch(IllegalArgumentException e) {
                assertTrue(false);
            }
        }

        // Queue capacity reached, exception should be thrown
        try{
            queue.add(1);
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testContains() throws Exception {
        assertFalse(queue.contains(1));
        queue.add(1);
        assertTrue(queue.contains(1));
        queue.take();
        assertFalse(queue.contains(1));
    }

    @Test
    public void testOffer() throws Exception {
        for(int i = 0; i < capacity; i++) {
            assertTrue(queue.offer(i));
        }
        assertFalse(queue.offer(1));
    }

    @Test
    public void testClear() throws Exception {
        assertTrue(queue.add(1));
        assertTrue(queue.add(2));
        assertTrue(queue.add(3));
        queue.clear();
        assertEquals(queue.size(), 0);
    }

    @Test
    public void testDrainTo() throws Exception {
        Collection<Integer> target = new ArrayList<Integer>();
        assertTrue(queue.add(1));
        assertTrue(queue.add(2));
        assertTrue(queue.add(3));
        queue.drainTo(target);
        assertEquals(target.size(), 3);
        assertTrue(target.contains(1));
        assertTrue(target.contains(2));
        assertTrue(target.contains(3));
    }

    @Test
    public void testDrainToWithLimit() throws Exception {
        Collection<Integer> target = new ArrayList<Integer>();
        assertTrue(queue.add(1));
        assertTrue(queue.add(2));
        assertTrue(queue.add(3));
        queue.drainTo(target, 2);
        assertEquals(target.size(), 2);
    }

    @Test
    public void testRemoveAll() throws Exception {
        List<Integer> l = new ArrayList<Integer>();
        l.add(1);
        l.add(2);

        queue.add(1);
        queue.add(2);
        queue.add(3);

        queue.removeAll(l);

        assertFalse(queue.contains(1));
        assertFalse(queue.contains(2));
        assertTrue(queue.contains(3));
    }

    @Test
    public void testRetainAll() throws Exception {
        List<Integer> l = new ArrayList<Integer>();
        l.add(1);
        l.add(2);

        queue.add(1);
        queue.add(2);
        queue.add(3);

        queue.retainAll(l);

        assertTrue(queue.contains(1));
        assertTrue(queue.contains(2));
        assertFalse(queue.contains(3));
    }

    @Test
    public void testIterator() throws Exception {
        queue.add(1);
        queue.add(2);
        queue.add(3);

        Iterator<Integer> it = queue.iterator();
        assertTrue(it.hasNext());
        assertEquals(it.next(), 3);
        assertTrue(it.hasNext());
        assertEquals(it.next(), 2);
        assertTrue(it.hasNext());
        assertEquals(it.next(), 1);
        assertFalse(it.hasNext());
    }

    @Test
    public void testProportions() throws Exception {
        int count1 = 0;
        int count2 = 0;

        for(int i = 1; i < capacity / 2; i++) {
            assertTrue(queue.add(1));
            assertTrue(queue.add(2));
        }

        for(int i = 0; i < capacity / 2; i++) {
            int x = queue.take();
            if(x == 1)
                count1++;
            if(x == 2)
                count2++;
        }

        System.out.println("Weight 1: " + count1);
        System.out.println("Weight 2: " + count2);
        assertTrue(count2 > count1);
        assertTrue(count1 > 10);
    }
}
