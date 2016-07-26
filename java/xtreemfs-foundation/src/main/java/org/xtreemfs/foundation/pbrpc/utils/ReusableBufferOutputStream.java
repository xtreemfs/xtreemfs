/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class ReusableBufferOutputStream extends OutputStream {

    public static final int BUFF_SIZE = 1024*8;

    private final int bufSize;

    private final ReusableBuffer firstBuffer;

    private List<ReusableBuffer> buffers;

    private ReusableBuffer currentBuffer;

    private int            length;

    public ReusableBufferOutputStream(int bufSize) {
        this.bufSize = bufSize;
        
        firstBuffer = BufferPool.allocate(bufSize);
        currentBuffer = firstBuffer;
        length = 0;
    }

    private ReusableBuffer checkAndGetBuffer(int requiredSpace) {
        if (currentBuffer.remaining() < requiredSpace) {
            if (buffers == null)
                buffers = new ArrayList<ReusableBuffer>(15);
            final int newBufSize = (bufSize >= requiredSpace) ? bufSize : requiredSpace;
            final ReusableBuffer buf = BufferPool.allocate(newBufSize);
            buffers.add(buf);
            currentBuffer = buf;
        }
        return currentBuffer;
    }

    public void appendBuffer(ReusableBuffer buffer) {
        currentBuffer = buffer;
        if (buffers == null)
            buffers = new ArrayList<ReusableBuffer>(15);
        buffer.position(buffer.limit());
        buffers.add(buffer);
        length += buffer.remaining();
    }

    public void flip() {
        firstBuffer.flip();
        if (buffers != null) {
            for (ReusableBuffer buffer : buffers) {
                buffer.flip();
            }
        }
        currentBuffer = firstBuffer;
    }

    public void freeBuffers() {
        BufferPool.free(firstBuffer);
        if (buffers != null) {
            for (ReusableBuffer buffer : buffers) {
                BufferPool.free(buffer);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        checkAndGetBuffer(1).put((byte)b);
        length++;
    }

    public void write(byte b[], int off, int len) throws IOException {
        checkAndGetBuffer(len).put(b, off, len);
        length += len;
    }

    public ReusableBuffer[] getBuffers() {
        if (buffers == null) {
            return new ReusableBuffer[]{firstBuffer};
        } else {
            ReusableBuffer[] arr = new ReusableBuffer[buffers.size()+1];
            arr[0] = firstBuffer;
            for (int i = 1; i <= buffers.size(); i++)
                arr[i] = buffers.get(i-1);
            return arr;
        }
    }

    public int length() {
        return length;
    }

}
