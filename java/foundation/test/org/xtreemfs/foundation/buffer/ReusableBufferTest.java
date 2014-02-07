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
}
