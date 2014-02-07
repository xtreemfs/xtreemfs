/*
 * Copyright (c) 2014 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.buffer;

import static org.junit.Assert.*;

import org.junit.Test;

public class BufferPoolTest {
    public static final int TEST_BUFFER_SIZE = 8192;

    @Test
    public final void testSimpleAllocateAndFree() {
        ReusableBuffer buf = null;

        assertEquals(
                "BufferPool must be empty because no buffers were returned yet and therefore nothing can be pooled yet.",
                0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals("BufferPool must be empty because a buffer was only allocated but not returned so far.", 0,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals("One buffer must have been returned and pooled.", 1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals("Pooled buffer must have been re-allocated.", 0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals("One buffer must have been returned and pooled again.", 1,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
    }

    @Test
    public final void testReusableViewBuffers() {
        ReusableBuffer buf = null;

        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals(1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        ReusableBuffer viewBuffer = buf.createViewBuffer();

        BufferPool.free(viewBuffer);
        assertEquals("Buffer not returned to pool yet since one reference is left.", 0,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        BufferPool.free(buf);
        assertEquals("Buffer must have been returned to pool since no reference is left.", 1,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
    }

    @Test
    public final void testReusableViewBuffersOfReusableViewBuffers() {
        ReusableBuffer buf = null;

        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals(1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        ReusableBuffer viewBuffer = buf.createViewBuffer();
        // Create a view buffer of a view buffer.
        ReusableBuffer viewBuffer2 = viewBuffer.createViewBuffer();

        BufferPool.free(viewBuffer2);
        assertEquals("Buffer not returned to pool yet since two references are left.", 0,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        BufferPool.free(viewBuffer);
        assertEquals("Buffer not returned to pool yet since one reference is left.", 0,
                BufferPool.getPoolSize(TEST_BUFFER_SIZE));
        BufferPool.free(buf);
        assertEquals("Buffer must have been returned to pool since no reference is left.", 1,
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

        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals(1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        // Double free will trigger assertion.
        BufferPool.free(buf);
    }

    @Test(expected = AssertionError.class)
    public final void testDoubleFreeOfRecursiveViewBuffersThrows() {
        assertThatAssertionsAreEnabled();

        ReusableBuffer buf = null;

        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        BufferPool.free(buf);
        assertEquals(1, BufferPool.getPoolSize(TEST_BUFFER_SIZE));

        buf = BufferPool.allocate(TEST_BUFFER_SIZE);
        assertEquals(0, BufferPool.getPoolSize(TEST_BUFFER_SIZE));
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
