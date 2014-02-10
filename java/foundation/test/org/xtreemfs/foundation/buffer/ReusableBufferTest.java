/*
 * Copyright (c) 2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *               2014 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.buffer;

import static org.junit.Assert.*;

import org.junit.Test;

public class ReusableBufferTest {

    @Test
    public final void testArray() {
        ReusableBuffer rb = ReusableBuffer.wrap("Yagga Yagga".getBytes());
        ReusableBuffer vb = rb.createViewBuffer();

        vb.position(0);
        vb.limit(5);
        String result = new String(vb.array());

        assertEquals("Yagga", result);
    }

    @Test
    public final void testRecursiveNonResuableViewBuffer() {
        ReusableBuffer rb = ReusableBuffer.wrap("Yagga Yagga".getBytes());
        assertFalse(rb.isReusable());

        ReusableBuffer viewBuffer = rb.createViewBuffer();
        assertFalse(viewBuffer.isReusable());

        ReusableBuffer viewBuffer2 = viewBuffer.createViewBuffer();
        assertFalse(viewBuffer2.isReusable());
    }

    @Test
    public final void testNonResuableViewBufferHasIndependentPositions() {
        ReusableBuffer rb = ReusableBuffer.wrap("Yagga Yagga".getBytes());
        assertFalse(rb.isReusable());
        rb.position(0);
        rb.limit(5);

        ReusableBuffer viewBuffer = rb.createViewBuffer();
        assertFalse(viewBuffer.isReusable());
        viewBuffer.position(1);
        viewBuffer.limit(4);

        assertEquals(0, rb.position());
        assertEquals(5, rb.limit());
        assertEquals(1, viewBuffer.position());
        assertEquals(4, viewBuffer.limit());

        ReusableBuffer viewBufferOfViewBuffer = viewBuffer.createViewBuffer();
        assertFalse(viewBufferOfViewBuffer.isReusable());
        viewBufferOfViewBuffer.position(2);
        viewBufferOfViewBuffer.limit(3);

        assertEquals(0, rb.position());
        assertEquals(5, rb.limit());
        assertEquals(1, viewBuffer.position());
        assertEquals(4, viewBuffer.limit());
        assertEquals(2, viewBufferOfViewBuffer.position());
        assertEquals(3, viewBufferOfViewBuffer.limit());
    }

    @Test
    public final void testRange() {
        // Test with a view buffer and with a reusable buffer.
        ReusableBuffer[] buffers = new ReusableBuffer[] { ReusableBuffer.wrap("Yagga Yagga".getBytes()),
                BufferPool.allocate(12) };
        for (ReusableBuffer buf : buffers) {
            buf.range(1, 4);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            assertEquals(4, buf.capacity());
            assertEquals(4, buf.remaining());

            // Now create a view buffer which will have a different range.
            ReusableBuffer viewBuf = buf.createViewBuffer();
            viewBuf.range(2, 2);
            assertEquals(0, viewBuf.position());
            assertEquals(2, viewBuf.limit());
            assertEquals(2, viewBuf.capacity());
            assertEquals(2, viewBuf.remaining());

            // The range of the original buffer was not affected.
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            assertEquals(4, buf.capacity());
            assertEquals(4, buf.remaining());

            if (buf.isReusable()) {
                BufferPool.free(buf);
            }
        }
    }

    @Test
    public final void testShrink() {
        // Test with a view buffer and with a reusable buffer.
        ReusableBuffer[] buffers = new ReusableBuffer[] { ReusableBuffer.wrap("Yagga Yagga".getBytes()),
                BufferPool.allocate(12) };
        for (ReusableBuffer buf : buffers) {
            buf.shrink(4);
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            assertEquals(4, buf.capacity());
            assertEquals(4, buf.remaining());

            // Now create a view buffer which will have a different range.
            ReusableBuffer viewBuf = buf.createViewBuffer();
            viewBuf.shrink(2);
            assertEquals(0, viewBuf.position());
            assertEquals(2, viewBuf.limit());
            assertEquals(2, viewBuf.capacity());
            assertEquals(2, viewBuf.remaining());

            // The range of the original buffer was not affected.
            assertEquals(0, buf.position());
            assertEquals(4, buf.limit());
            assertEquals(4, buf.capacity());
            assertEquals(4, buf.remaining());

            if (buf.isReusable()) {
                BufferPool.free(buf);
            }
        }
    }

    @Test
    public final void testEnlarge() {
        // Test with a view buffer and with a reusable buffer.
        ReusableBuffer[] buffers = new ReusableBuffer[] { ReusableBuffer.wrap("Yagga Yagga".getBytes()),
                BufferPool.allocate(11) };
        for (ReusableBuffer buf : buffers) {
            int originalBufSize = buf.capacity();

            // When there is no underlying, larger buffer, enlarge won't have an effect (the capacity of the
            // underlying buffer can be higher than the current buffer size.)
            assertFalse(buf.enlarge(buf.capacityUnderlying() + 1));
            // Enlarging to the current size should always work.
            assertTrue(buf.enlarge(originalBufSize));
            assertEquals(originalBufSize, buf.capacity());

            // Create a view buffer first which is smaller than the original one.
            buf.limit(originalBufSize / 2);
            ReusableBuffer smallerBuf = buf.createViewBuffer();

            // Enlarge it, but not larger than the underlying buffer.
            int newBufSize = smallerBuf.capacity() * 2;
            assertTrue(smallerBuf.enlarge(newBufSize));
            assertEquals(0, buf.position());
            assertEquals(newBufSize, smallerBuf.limit());
            assertEquals(newBufSize, smallerBuf.capacity());
            assertEquals(newBufSize, smallerBuf.remaining());

            // You cannot enlarge it over the capacity of the original buffer.
            assertFalse(smallerBuf.enlarge(buf.capacityUnderlying() + 1));

            // The range of the original buffer was not affected.
            assertEquals(0, buf.position());
            assertEquals(originalBufSize / 2, buf.limit());
            assertEquals(originalBufSize, buf.capacity());
            assertEquals(originalBufSize / 2, buf.remaining());

            if (buf.isReusable()) {
                BufferPool.free(buf);
            }
        }
    }
}
