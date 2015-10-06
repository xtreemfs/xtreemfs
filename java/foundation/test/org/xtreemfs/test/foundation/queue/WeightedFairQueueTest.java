/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
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
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class WeightedFairQueueTest {
    WeightedFairQueue<String, Integer> queue;
    int capacity = 10000;
    @Before
    public void setUp() throws Exception {
        queue = new WeightedFairQueue<String, Integer>(capacity, 10000,
                new WeightedFairQueue.WFQElementInformationProvider<String, Integer>() {
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
        assertEquals((long) element, (long) queue.take());
    }

    @Test
    public void testPoll() throws Exception {
        assertTrue(queue.poll() == null);
        queue.add(1);
        assertEquals((long) queue.poll(), 1);
        assertTrue(queue.poll() == null);
    }

    @Test
    public void testPollWithTimeout() throws Exception {
        assertTrue(queue.poll(100, TimeUnit.MILLISECONDS) == null);
        queue.add(1);
        assertEquals((long) queue.poll(100, TimeUnit.MILLISECONDS), 1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    queue.add(2);
                } catch (InterruptedException e) {}
            }
        }).start();

        assertEquals((long) queue.poll(200, TimeUnit.MILLISECONDS), 2);
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
    public void testOfferWithTimeout() throws Exception {
        for(int i = 0; i < capacity; i++) {
            assertTrue(queue.offer(i, 100, TimeUnit.MILLISECONDS));
        }
        assertFalse(queue.offer(1, 100, TimeUnit.MILLISECONDS));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    queue.take();
                } catch (InterruptedException e) {}
            }
        }).start();

        assertTrue(queue.offer(1, 1000, TimeUnit.MILLISECONDS));
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
        assertEquals((long) it.next(), 3);
        assertTrue(it.hasNext());
        assertEquals((long) it.next(), 2);
        assertTrue(it.hasNext());
        assertEquals((long) it.next(), 1);
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

        System.out.println("testProportions():");
        System.out.println("Weight 1: " + count1);
        System.out.println("Weight 2: " + count2);
        assertTrue(count2 > count1);
        assertTrue(count1 > 10);
    }

    @Test
    public void testMultiTenantProportions() throws Exception {
        int count1 = 0;
        int count2 = 0;
        int count3 = 0;

        for(int i = 1; i < capacity / 3; i++) {
            assertTrue(queue.add(1));
            assertTrue(queue.add(2));
            assertTrue(queue.add(3));
        }

        for(int i = 0; i < capacity / 3; i++) {
            int x = queue.take();
            if(x == 1)
                count1++;
            if(x == 2)
                count2++;
            if(x == 3)
                count3++;
        }

        System.out.println("testMultiTenantProportions():");
        System.out.println("Weight 1: " + count1);
        System.out.println("Weight 2: " + count2);
        System.out.println("Weight 3: " + count3);
        assertTrue(count3 > count2);
        assertTrue(count2 > count1);
        assertTrue(count1 > 0);
    }

    @Test
    public void testThroughput() throws Exception {
        long t1 = System.currentTimeMillis();

        for(int i = 0; i < capacity; i++) {
            queue.add(i%100); // assume 100 quality classes
        }

        while(!queue.isEmpty()) {
            queue.take();
        }

        long t2 = System.currentTimeMillis();

        System.out.println("testThroughput()");
        System.out.println("Runtime for " + capacity + " elements: " + (t2-t1) + "ms");

        assertTrue(true);
    }
}
