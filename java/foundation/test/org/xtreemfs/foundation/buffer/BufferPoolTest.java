/*
 * Copyright (c) 2014 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.buffer;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests different sequences of {@link BufferPool#allocate(int)} and {@link BufferPool#free(ReusableBuffer)}
 * operations, some also regarding view buffers.
 * 
 * Please note that {@link BufferPool} is a singleton and therefore the pool size increases as the number of
 * run tests does. Therefore, each test has to evaluate changes in the pool size relative to the pool size at
 * the start of test.
 * 
 */
public class BufferPoolTest {
    public static final int TEST_BUFFER_SIZE = 8192;

    @Test
    public final void testSimpleAllocateAndFree() {
        ReusableBuffer buf = null;
        // There may be already a buffer pooled. If not, the pool size will stay 0 after an allocate().
        int currentPoolSize = Math.max(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE) - 1);

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals("BufferPool must be empty because a buffer was only allocated but not returned so far.",
                currentPoolSize,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals("One buffer must have been returned and pooled.", currentPoolSize + 1,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals("Pooled buffer must have been re-allocated.", currentPoolSize,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals("One buffer must have been returned and pooled again.", currentPoolSize + 1,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
    }

    @Test
    public final void testReusableViewBuffers() {
        ReusableBuffer buf = null;
        // There may be already a buffer pooled. If not, the pool size will stay 0 after an allocate().
        int currentPoolSize = Math.max(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE) - 1);

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(currentPoolSize, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals(currentPoolSize + 1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(currentPoolSize, BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        ReusableBuffer viewBuffer = buf.createViewBuffer();

        BufferPool.free(viewBuffer);
        assertEquals("Buffer not returned to pool yet since one reference is left.", currentPoolSize,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        BufferPool.free(buf);
        assertEquals("Buffer must have been returned to pool since no reference is left.", currentPoolSize + 1,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
    }

    @Test
    public final void testReusableViewBuffersOfReusableViewBuffers() {
        ReusableBuffer buf = null;
        // There may be already a buffer pooled. If not, the pool size will stay 0 after an allocate().
        int currentPoolSize = Math.max(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE) - 1);

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(currentPoolSize, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals(currentPoolSize + 1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(currentPoolSize, BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        ReusableBuffer viewBuffer = buf.createViewBuffer();
        // Create a view buffer of a view buffer.
        ReusableBuffer viewBuffer2 = viewBuffer.createViewBuffer();

        BufferPool.free(viewBuffer2);
        assertEquals("Buffer not returned to pool yet since two references are left.", currentPoolSize,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        BufferPool.free(viewBuffer);
        assertEquals("Buffer not returned to pool yet since one reference is left.", currentPoolSize,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        BufferPool.free(buf);
        assertEquals("Buffer must have been returned to pool since no reference is left.", currentPoolSize + 1,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
    }

    private void assertThatAssertionsAreEnabled() {
        boolean assertOn = false;
        // *assigns* true if assertions are on.
        assert assertOn = true;
        assertTrue("Enable assertions or this test won't work correctly.", assertOn);
    }

    @Test(expected = AssertionError.class)
    public final void testDoubleFreeThrows() {
        assertThatAssertionsAreEnabled();

        ReusableBuffer buf = null;
        // There may be already a buffer pooled. If not, the pool size will stay 0 after an allocate().
        int currentPoolSize = Math.max(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE) - 1);

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(currentPoolSize, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals(currentPoolSize + 1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        // Double free will trigger assertion.
        BufferPool.free(buf);
    }

    @Test(expected = AssertionError.class)
    public final void testDoubleFreeOfRecursiveViewBuffersThrows() {
        assertThatAssertionsAreEnabled();

        ReusableBuffer buf = null;
        // There may be already a buffer pooled. If not, the pool size will stay 0 after an allocate().
        int currentPoolSize = Math.max(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE) - 1);

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(currentPoolSize, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals(currentPoolSize + 1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(currentPoolSize, BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        ReusableBuffer viewBuffer = buf.createViewBuffer();
        // Create a view buffer of a view buffer.
        ReusableBuffer viewBuffer2 = viewBuffer.createViewBuffer();

        BufferPool.free(viewBuffer2);
        BufferPool.free(viewBuffer);
        BufferPool.free(buf);

        // Double free will trigger assertion.
        BufferPool.free(viewBuffer2);
    }
}
