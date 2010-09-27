/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.common.buffer;

import java.util.LinkedList;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

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
        
        Assert.assertEquals("Yagga", result);
        
    }
    
    public void testBufferPool() throws Exception {
        
        Thread t1 = new Thread() {
            
            public void run() {
                for (int i = 0; i < 5000; i++) {
                    LinkedList<ReusableBuffer> bufs = new LinkedList<ReusableBuffer>();
                    for (int j = 0; j < 10; j++)
                        bufs.add(BufferPool.allocate((int) (Math.random() * 100000 + 1)));
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
                    for (ReusableBuffer buf : bufs)
                        BufferPool.free(buf);
                }
            }
        };
        
        Thread t2 = new Thread() {
            
            public void run() {
                for (int i = 0; i < 5000; i++) {
                    LinkedList<ReusableBuffer> bufs = new LinkedList<ReusableBuffer>();
                    for (int j = 0; j < 10; j++)
                        bufs.add(BufferPool.allocate((int) (Math.random() * 100000 + 1)));
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
                    for (ReusableBuffer buf : bufs)
                        BufferPool.free(buf);
                }
            }
        };
        
        Thread t3 = new Thread() {
            
            public void run() {
                for (int i = 0; i < 5000; i++) {
                    LinkedList<ReusableBuffer> bufs = new LinkedList<ReusableBuffer>();
                    for (int j = 0; j < 10; j++)
                        bufs.add(BufferPool.allocate((int) (Math.random() * 100000 + 1)));
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
                    for (ReusableBuffer buf : bufs)
                        BufferPool.free(buf);
                }
            }
        };
        
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();
        
        System.out.println(BufferPool.getStatus());
        
    }
    
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
}