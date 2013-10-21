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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class WeightedFairQueueTest {
    WeightedFairQueue<String, Integer> queue;

    @Before
    public void setUp() throws Exception {
        queue = new WeightedFairQueue<String, Integer>(100, new WeightedFairQueue.WFQElementInformationProvider<String, Integer>() {
            @Override
            public int getRequestCost(Integer element) {
                return 1;
            }

            @Override
            public String getQualityClass(Integer element) {
                return "test";
            }

            @Override
            public int getWeight(String element) {
                return 1;
            }
        });
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(queue.isEmpty());
        queue.add(123);
        assertFalse(queue.isEmpty());
        queue.take();
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testTake() throws Exception {
        int element = 1;

        queue.add(element);
        assertEquals(element, queue.take());
    }

    @Test
    public void testAdd() throws Exception {
        try {
            queue.add(2);
        } catch(IllegalArgumentException e) {
            assertTrue(false);
        }
    }

    @Test
    public void testClear() throws Exception {
        queue.add(1);
        queue.add(2);
        queue.add(3);
        queue.clear();
        assertEquals(queue.size(), 0);
    }
}
