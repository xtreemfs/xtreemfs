/*
 * Copyright (c) 2010 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.foundation.buffer;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class ReusableBufferTest extends TestCase {

    public ReusableBufferTest(String testName) {
        super(testName);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testArray() throws Exception {

        ReusableBuffer rb = ReusableBuffer.wrap("Yagga Yagga".getBytes());
        ReusableBuffer vb = rb.createViewBuffer();

        vb.position(0);
        vb.limit(5);
        String result = new String(vb.array());

        Assert.assertEquals("Yagga",result);


    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

}