/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestHelper;

public class ECHelperTest extends ECTestCommon {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    @Test
    public void testXOR() {
        ReusableBuffer a, b, result, expected;
        int length;
        
        // Test aligned multiple of 8
        length = 64;
        a = SetupUtils.generateData(length, (byte) 1);
        b = SetupUtils.generateData(length, (byte) 2);
        expected = SetupUtils.generateData(length, (byte) 3);
        result = BufferPool.allocate(length);
        
        ECHelper.xor(result, a, b);
        result.flip();
        assertBufferEquals(expected, result);
        freeAll(a, b, result, expected);
        
        // Test unaligned length
        length = 61;
        a = SetupUtils.generateData(length, (byte) 1);
        b = SetupUtils.generateData(length, (byte) 2);
        expected = SetupUtils.generateData(length, (byte) 3);
        result = BufferPool.allocate(length);
        
        ECHelper.xor(result, a, b);
        result.flip();
        assertBufferEquals(expected, result);
        freeAll(a, b, result, expected);
        
        
        // Test combined dst/src buffer
        length = 64;
        a = SetupUtils.generateData(length, (byte) 1);
        b = SetupUtils.generateData(length, (byte) 2);
        expected = SetupUtils.generateData(length, (byte) 3);
        result = a;

        ECHelper.xor(a, b);
        result.flip();
        assertBufferEquals(expected, result);
        freeAll(a, b, result, expected);
        

        // Test different size buffers
        length = 64 * 2;
        a = SetupUtils.generateData(64, (byte) 1);
        for (int i = 0; i < 32; i++) {
            a.put(i, (byte) 8);
        }
        a.position(32);

        b = SetupUtils.generateData(length, (byte) 2);
        for (int i = 0; i < 64; i++) {
            b.put(i, (byte) 16);
        }
        b.position(64);

        expected = SetupUtils.generateData(32, (byte) 3);
        result = BufferPool.allocate(64);

        result.position(32);
        ECHelper.xor(result, a, b);
        result.position(32);
        assertBufferEquals(expected, result);
        freeAll(a, b, result, expected);
        
    }
}
