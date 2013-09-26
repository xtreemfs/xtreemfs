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
import org.xtreemfs.foundation.queue.AbstractWeightedFairQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class AbstractWeightedFairQueueTest {
    AbstractWeightedFairQueue<Integer> queue;

    @Before
    public void setUp() throws Exception {
        queue = new AbstractWeightedFairQueue<Integer>(100) {
            public int getElementSize(Integer element) {
                return 1;
            }
        };
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(queue.isEmpty());
        queue.addQualityClass("123", 123);
        queue.add("123", 123);
        assertFalse(queue.isEmpty());
        queue.take();
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testTake() throws Exception {
        int element = 1;

        queue.addQualityClass("123", 123);
        queue.add("123", element);
        assertEquals(element,  queue.take());
    }

    @Test
    public void testAdd() throws Exception {
        queue.addQualityClass("1", 1);
        try {
            queue.add("1", 2);
        } catch(IllegalArgumentException e) {
            assertTrue(false);
        }

        try {
            queue.add("2", 3);
            assertTrue(false);
        } catch(IllegalArgumentException e) {
            assertTrue(true);
        }
    }
}
