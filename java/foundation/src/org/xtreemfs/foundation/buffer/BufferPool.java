/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A concurrent pool for buffer recycling.
 * 
 * @author bjko
 */
public final class BufferPool {
    
    /**
     * size of buffers for each class.
     */
    public static final int[]                         BUFF_SIZES        = { 8192, 65536, 131072, 524288,
        2097152                                                        };
    
    /**
     * max pool size for each class
     */
    public static final int[]                         MAX_POOL_SIZES    = { 2000, 200, 100, 10, 5 };
    
    /**
     * queues to store buffers in
     */
    private final ConcurrentLinkedQueue<ByteBuffer>[] pools;
    
    /**
     * pool sizes to avoid counting elements on each access
     */
    private final AtomicInteger[]                     poolSizes;
    
    /**
     * stats for num requests and creates of buffers per class
     */
    private final AtomicLong[]                              requests, creates, deletes;
    
    /**
     * singleton pattern.
     */
    private static final BufferPool                   instance          = new BufferPool();
    
    /**
     * if true all allocate/free operations record the stack trace. Useful to
     * find memory leaks but slow.
     */
    protected static boolean                          recordStackTraces = false;
    
    /**
     * Creates a new instance of BufferPool
     */
    @SuppressWarnings("unchecked")
    private BufferPool() {
        
        pools = new ConcurrentLinkedQueue[BUFF_SIZES.length];
        
        creates = new AtomicLong[BUFF_SIZES.length];
        for (int i = 0; i < creates.length; i++) {
            creates[i] = new AtomicLong();
        }
        
        requests = new AtomicLong[BUFF_SIZES.length + 1];
        deletes = new AtomicLong[BUFF_SIZES.length + 1];
        for (int i = 0; i < BUFF_SIZES.length + 1; i++) {
            requests[i] = new AtomicLong();
            deletes[i] = new AtomicLong();
        }
        
        poolSizes = new AtomicInteger[BUFF_SIZES.length];
        for (int i = 0; i < BUFF_SIZES.length; i++) {
            pools[i] = new ConcurrentLinkedQueue<ByteBuffer>();
            poolSizes[i] = new AtomicInteger(0);
        }
    }
    
    /**
     * Get a new buffer. The Buffer is taken from the pool or created if none is
     * available or the size exceeds the largest class.
     * 
     * @param size
     *            the buffer's size in bytes
     * @return a buffer of requested size
     * @throws OutOfMemoryError
     *             if a buffer cannot be allocated
     */
    public static ReusableBuffer allocate(int size) {
        ReusableBuffer tmp = instance.getNewBuffer(size);
        assert (tmp.refCount.get() == 1): "newly allocated buffer has invalid reference count: " + tmp.refCount.get();
        
        if (recordStackTraces) {
            tmp.allocStack = "\n";
            for (StackTraceElement elem : new Exception().getStackTrace())
                tmp.allocStack += elem.toString() + "\n";
        }
        return tmp;
    }
    
    /**
     * Returns a buffer to the pool, if the buffer is reusable. Other buffers
     * are ignored.
     * 
     * @param buf
     *            the buffer to return
     */
    public static void free(ReusableBuffer buf) {
        if (buf != null) {
            instance.returnBuffer(buf);
        }
    }
    
    /**
     * Returns a buffer which has at least size bytes.
     * 
     * @attention The returned buffer can be larger than requested!
     */
    private ReusableBuffer getNewBuffer(int size) {
        
        try {
            
            // if there is a pooled buffer with sufficient capacity ...
            for (int i = 0; i < BUFF_SIZES.length; i++) {
                
                if (size <= BUFF_SIZES[i]) {
                    
                    ByteBuffer buf = pools[i].poll();
                    
                    // if no free buffer is available in the pool ...
                    if (buf == null) {
                        
                        // ... create
                        // - a direct buffer if the pool is not full yet,
                        // - a non-direct buffer if the pool is full
                        // 
                        // Thus, the first MAX_POOL_SIZES[i] buffers will be
                        // pooled, whereas any additional buffers will be
                        // allocated on demand and freed by the garbage
                        // collector.
                        
                        buf = creates[i].get() < MAX_POOL_SIZES[i] ? ByteBuffer.allocateDirect(BUFF_SIZES[i])
                            : ByteBuffer.allocate(BUFF_SIZES[i]);
                        creates[i].incrementAndGet();
                    }

                    // otherwise, decrement the pool size to indicate that the
                    // pooled buffer was handed out to the application
                    else {
                        poolSizes[i].decrementAndGet();
                    }
                    
                    requests[i].incrementAndGet();
                    return new ReusableBuffer(buf, size);
                    
                }
                
            }
            
            // ... otherwise, create an unpooled buffer
            requests[BUFF_SIZES.length].incrementAndGet();
            
            ByteBuffer buf = ByteBuffer.allocate(size);
            return new ReusableBuffer(buf, size);
            
        } catch (OutOfMemoryError ex) {
            System.out.println(getStatus());
            throw ex;
        }
    }
    
    private void returnBuffer(ReusableBuffer buffer) {
        returnBuffer(buffer, false);
    }
    
    /**
     * return a buffer to the pool
     */
    private void returnBuffer(ReusableBuffer buffer, boolean callFromView) {
        
        if (!buffer.isReusable())
            return;
        
        if (buffer.viewParent != null) {
            
            // view buffer
            if (recordStackTraces) {
                
                if (buffer.freeStack == null)
                    buffer.freeStack = "";
                buffer.freeStack += "\n";
                
                StackTraceElement[] stackTrace = new Exception().getStackTrace();
                for (int i = 0; i < stackTrace.length; i++)
                    buffer.freeStack += stackTrace[i].toString() + "\n";
            }
            
            assert (!buffer.returned) : "buffer was already released: " + buffer.freeStack;
            buffer.returned = true;
            returnBuffer(buffer.viewParent, true);
            
        } else {
            
            assert (!buffer.returned || callFromView) : "buffer was already released: " + buffer.freeStack;
            
            if (recordStackTraces) {
                
                if (buffer.freeStack == null)
                    buffer.freeStack = "";
                buffer.freeStack += "\n";
                
                StackTraceElement[] stackTrace = new Exception().getStackTrace();
                for (int i = 0; i < stackTrace.length; i++)
                    buffer.freeStack += stackTrace[i].toString() + "\n";
            }
            
            if (!callFromView) {
                buffer.returned = true;
            }
            
            if (buffer.refCount.getAndDecrement() > 1) {
                return;
            }
                                    
            ByteBuffer buf = buffer.getParent();
            buf.clear();
            
            // determine the pool to which the buffer is supposed to be
            // returned
            // ...
            for (int i = 0; i < BUFF_SIZES.length; i++) {
                
                if (buf.capacity() == BUFF_SIZES[i]) {
                    
                    // return direct buffers to the pool
                    if (buf.isDirect()) {
                        
                        poolSizes[i].incrementAndGet();
                        pools[i].add(buf);
                        
                        // since only direct buffers will be returned to the
                        // pool, which have been counted on allocation, there is
                        // no need to check the pool size here
                        
                        return;
                    }

                    // if the buffer is non-direct, increment the delete counter
                    // and implicitly make the buffer subject to garbage
                    // collection
                    else {
                        deletes[i].incrementAndGet();
                        return;
                    }
                    
                }
                
            }
            
            assert (!buf.isDirect()) : "encountered direct buffer that does not fit in any of the pools (size="
                + buf.capacity() + "): " + buffer.freeStack;
            
            // if the buffer did not fit in any of the pools,
            // increment the delete counter for the unpooled buffers
            deletes[deletes.length - 1].incrementAndGet();
            
        }
    }
    
    /**
     * Get the current pool size for a specific buffer size.
     * 
     * @throws IllegalArgumentException
     *             when bufferSize is not in the pool
     */
    public static int getPoolSize(int bufferSize) {
        for (int i = 0; i < BUFF_SIZES.length; i++) {
            if (BUFF_SIZES[i] == bufferSize) {
                return instance.poolSizes[i].get();
            }
        }
        throw new IllegalArgumentException("Specified buffer size is not pooled. Check BufferPool configuration.");
    }

    /**
     * Returns a textual representation of the pool status.
     * 
     * @return a textual representation of the pool status.
     */
    public static String getStatus() {
        
        String str = "";
        for (int i = 0; i < BUFF_SIZES.length; i++) {
            str += String.format(
                "%8d:      poolSize = %5d    numRequests = %8d    creates = %8d   deletes = %8d\n",
                BUFF_SIZES[i], instance.poolSizes[i].get(), instance.requests[i].get(), instance.creates[i]
                        .get(), instance.deletes[i].get());
        }
        str += String.format("unpooled (> %8d)    numRequests = creates = %8d   deletes = %8d",
            BUFF_SIZES[BUFF_SIZES.length - 1], instance.requests[instance.requests.length - 1].get(),
            instance.deletes[instance.deletes.length - 1].get());
        return str;
    }
    
    /**
     * Specifies whether stack traces shall be recorded when allocating and
     * freeing buffers. Since recording stack traces leads to some overhead, it
     * should only be enabled for debugging purposes.
     * 
     * @param record
     */
    public static void enableStackTraceRecording(boolean record) {
        recordStackTraces = record;
    }
    
}
