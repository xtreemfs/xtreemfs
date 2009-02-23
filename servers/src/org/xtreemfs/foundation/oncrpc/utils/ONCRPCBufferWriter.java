/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.utils;

import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class ONCRPCBufferWriter {

    public static final int BUFF_SIZE = 1024*8;

    private final int bufSize;

    private final List<ReusableBuffer> buffers;

    private int   currentBuffer;

    public ONCRPCBufferWriter(int bufSize) {
        this.bufSize = bufSize;
        buffers = new ArrayList(15);
        buffers.add(BufferPool.allocate(bufSize));
        currentBuffer = 0;
    }

    private ReusableBuffer checkAndGetBuffer(int requiredSpace) {
        final ReusableBuffer currentBuf = buffers.get(currentBuffer);
        if (currentBuf.remaining() >= requiredSpace) {
            return currentBuf;
        } else {
            currentBuffer++;
            final ReusableBuffer buf = BufferPool.allocate(bufSize);
            buffers.add(buf);
            return buf;
        }
    }
    
    public void flip() {
        for (ReusableBuffer buffer : buffers) {
            buffer.flip();
        }
        currentBuffer = 0;
    }
    
    public void freeBuffers() {
        for (ReusableBuffer buffer : buffers) {
            BufferPool.free(buffer);
        }
    }
    
    public void put(byte data) {
        checkAndGetBuffer(Byte.SIZE/8).put(data);
    }

    public void put(byte[] data) {
        checkAndGetBuffer(Byte.SIZE/8*data.length).put(data);
    }

    public void putInt(int data) {
        checkAndGetBuffer(Integer.SIZE/8).putInt(data);
    }

    public void putLong(long data) {
        checkAndGetBuffer(Long.SIZE/8).putLong(data);
    }

    public void put(ReusableBuffer otherBuffer) {
        currentBuffer++;
        final ReusableBuffer vb = otherBuffer.createViewBuffer();
        vb.position(otherBuffer.limit());
        buffers.add(vb);
    }

    public List<ReusableBuffer> getBuffers() {
        return this.buffers;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ONCRPCBufferWriter with "+this.buffers.size()+" buffers\n");
        for (ReusableBuffer buffer : buffers) {
            sb.append("buffer position="+buffer.position()+" limit="+buffer.limit()+"\n");
        }
        return sb.toString();
    }







}
