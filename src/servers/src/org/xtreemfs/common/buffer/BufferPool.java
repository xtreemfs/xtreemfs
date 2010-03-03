/*  Copyright (c) 2008-2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.common.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A concurrent pool for buffer recycling.
 * @author bjko
 */
public final class BufferPool {

    /** size of buffers for each class.
     */
    public static final int[] BUFF_SIZES = { 8192, 65536, 524288, 2097152 };

    /** max pool size for each class
     */
    public static final int[] MAX_POOL_SIZES = { 2000, 6, 10, 5 };

    /** queues to store buffers in
     */
    private final ConcurrentLinkedQueue<ByteBuffer>[] pools;

    /** pool sizes to avoid counting elements on each access
     */
    private final AtomicInteger[] poolSizes;

    /** stats for num requests and creates of buffers per class
     */
    private long[] requests, creates, deletes;

    /** singleton pattern.
     */
    private static final BufferPool instance = new BufferPool();
    
    /**
     * if true all allocate/free operations record the stack trace.
     * Useful to find memory leaks but slow.
     */
    protected static final boolean recordStackTraces = false;

    /**
     * Creates a new instance of BufferPool
     */
    private BufferPool() {
        pools = new ConcurrentLinkedQueue[BUFF_SIZES.length];
        requests = new long[BUFF_SIZES.length+1];
        creates = new long[BUFF_SIZES.length];
        deletes = new long[BUFF_SIZES.length+1];
        poolSizes = new AtomicInteger[BUFF_SIZES.length];
        for (int i = 0; i < BUFF_SIZES.length; i++) {
            pools[i] = new ConcurrentLinkedQueue();
            poolSizes[i] = new AtomicInteger(0);
        }
    }

    /** Get a new buffer. The Buffer is taken from the pool or created if none
     *  is available or the size exceedes the largest class.
     *  @param size the buffer's size in bytes
     *  @return a buffer of requested size
     *  @throws OutOfMemoryError if a buffer cannot be allocated
     */
    public static ReusableBuffer allocate(int size) {
        ReusableBuffer tmp = instance.getNewBuffer(size);
        
        if (recordStackTraces) {
            try {
                throw new Exception("allocate stack trace");
            } catch (Exception e) {
                tmp.allocStack = "\n";
                for (StackTraceElement elem : e.getStackTrace())
                    tmp.allocStack += elem.toString()+"\n";
            }
        }
        return tmp;
    }

    /** Returns a buffer to the pool, if the buffer is reusable. Other
     *  buffers are ignored.
     *  @param buf the buffer to return
     */
    public static void free(ReusableBuffer buf) {
        if (buf != null) {
            instance.returnBuffer(buf);
        }
    }

    /** Returns a buffer which has at least size bytes.
     *  @attention The returned buffer can be larger than requested!
     */
    private ReusableBuffer getNewBuffer(int size) {
        try {
            ByteBuffer buf = null;

            if (size <= BUFF_SIZES[0]) {
                buf = pools[0].poll();
                if (buf == null) {
                    buf = ByteBuffer.allocateDirect(BUFF_SIZES[0]);
                    creates[0]++;
                } else {
                    poolSizes[0].decrementAndGet();
                }
                requests[0]++;
                return new ReusableBuffer(buf,size);
            } else if (size <= BUFF_SIZES[1]) {
                buf = pools[1].poll();
                if (buf == null) {
                    buf = ByteBuffer.allocateDirect(BUFF_SIZES[1]);
                    creates[1]++;
                } else {
                    poolSizes[1].decrementAndGet();
                }
                requests[1]++;
                return new ReusableBuffer(buf,size);
            } else if (size <= BUFF_SIZES[2]) {
                buf = pools[2].poll();
                if (buf == null) {
                    buf = ByteBuffer.allocateDirect(BUFF_SIZES[2]);
                    creates[2]++;
                } else {
                    poolSizes[2].decrementAndGet();
                }
                requests[2]++;
                return new ReusableBuffer(buf,size);
            } else if (size <= BUFF_SIZES[3]) {
                buf = pools[3].poll();
                if (buf == null) {
                    buf = ByteBuffer.allocateDirect(BUFF_SIZES[3]);
                    creates[3]++;
                } else {
                    poolSizes[3].decrementAndGet();
                }
                requests[3]++;
                return new ReusableBuffer(buf,size);
            } else {
                requests[4]++;
                buf = ByteBuffer.allocateDirect(size);
                return new ReusableBuffer(buf,size);
            }
        } catch (OutOfMemoryError ex) {
            System.out.println(this.getStatus());
            throw ex;
        }
    }

    /** return a buffer to the pool
     */
    private void returnBuffer(ReusableBuffer buffer) {
        if (!buffer.isReusable())
            return;

        if (buffer.viewParent != null) {
            // view buffer
            if (recordStackTraces) {
                try {
                    throw new Exception("free stack trace");
                } catch (Exception e) {
                    buffer.freeStack = "\n";
                    for (StackTraceElement elem : e.getStackTrace())
                        buffer.freeStack += elem.toString()+"\n";
                }
            }
            assert(!buffer.returned) : "buffer was already released: "+buffer.freeStack;
            buffer.returned = true;
            returnBuffer(buffer.viewParent);
        
        } else {

            if (buffer.refCount.getAndDecrement() > 1) {
                return;
            }

            assert(!buffer.returned) : "buffer was already released: "+buffer.freeStack;
            buffer.returned = true;

            
            if (recordStackTraces) {
                try {
                    throw new Exception("free stack trace");
                } catch (Exception e) {
                    buffer.freeStack = "\n";
                    for (StackTraceElement elem : e.getStackTrace())
                        buffer.freeStack += elem.toString()+"\n";
                }
            }

            ByteBuffer buf = buffer.getParent();

            buf.clear();
            if (buf.capacity() == BUFF_SIZES[0]) {
                if (poolSizes[0].get() < MAX_POOL_SIZES[0]) {
                    poolSizes[0].incrementAndGet();
                    pools[0].add(buf);
                } else {
                    deletes[0]++;
                }
            } else if (buf.capacity() == BUFF_SIZES[1]) {
                if (poolSizes[1].get() < MAX_POOL_SIZES[1]) {
                    poolSizes[1].incrementAndGet();
                    pools[1].add(buf);
                } else {
                    deletes[1]++;
                }
            } else if (buf.capacity() == BUFF_SIZES[2]) {
                if (poolSizes[2].get() < MAX_POOL_SIZES[2]) {
                    poolSizes[2].incrementAndGet();
                    pools[2].add(buf);
                } else {
                    deletes[2]++;
                }
            } else if (buf.capacity() == BUFF_SIZES[3]) {
                if (poolSizes[3].get() < MAX_POOL_SIZES[3]) {
                    poolSizes[3].incrementAndGet();
                    pools[3].add(buf);
                } else {
                    deletes[3]++;
                }
            } else {
                deletes[4]++;
            }
        }
    }

    /** Returns a textual representation of the pool status.
     *  @return a textual representation of the pool status.
     */
    public static String getStatus() {

        String str = "";
        for (int i = 0; i < 4; i++) {
            str += String.format("%8d:      poolSize = %5d    numRequests = %8d    creates = %8d   deletes = %8d\n",
                                        instance.BUFF_SIZES[i], instance.poolSizes[i].get(),
                                        instance.requests[i], instance.creates[i], instance.deletes[i]);
        }
        str += String.format("unpooled (> %8d)    numRequests = creates = %8d   deletes = %8d",instance.BUFF_SIZES[3],instance.requests[4],instance.deletes[4]);
        return str;
    }

}
