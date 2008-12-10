/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.common;

import java.util.Iterator;
import junit.framework.TestCase;
import org.xtreemfs.common.RingBuffer;

/**
 *
 * @author bjko
 */
public class RingBufferTest extends TestCase {
    
    public RingBufferTest(String testName) {
        super(testName);
    }            

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRingBuffer() throws Exception {
        
        RingBuffer<Long> b = new RingBuffer(100);
        for (long i = 1; i < 101; i++) {
            b.insert(i);
        }
        Iterator<Long> iter = b.iterator();
        for (long i = 1; i < 101; i++) {
            assertTrue(iter.hasNext());
            assertEquals(i, iter.next().longValue());
        }
        assertFalse(iter.hasNext());
        System.out.println(b);
        
        b.insert(101l);
        iter = b.iterator();
        for (long i = 2; i < 102; i++) {
            assertTrue(iter.hasNext());
            assertEquals(i, iter.next().longValue());
        }
        assertFalse(iter.hasNext());
        System.out.println(b);
        
    }

}
