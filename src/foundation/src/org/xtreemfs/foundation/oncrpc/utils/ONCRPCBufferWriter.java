/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.oncrpc.utils;

import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.XDRUtils;

import yidl.runtime.Map;
import yidl.runtime.Marshaller;
import yidl.runtime.Sequence;
import yidl.runtime.Struct;

/**
 *
 * @author bjko
 */
public class ONCRPCBufferWriter extends Marshaller {

    public static final int BUFF_SIZE = 1024*8;

    private final int bufSize;

    private final List<ReusableBuffer> buffers;

    private int   currentBuffer;

    private boolean inMap;

    public ONCRPCBufferWriter(int bufSize) {
        this.bufSize = bufSize;
        buffers = new ArrayList<ReusableBuffer>(15);
        buffers.add(BufferPool.allocate(bufSize));
        currentBuffer = 0;
        inMap = false;
    }

    private ReusableBuffer checkAndGetBuffer(int requiredSpace) {
        final ReusableBuffer currentBuf = buffers.get(currentBuffer);
        if (currentBuf.remaining() >= requiredSpace) {
            return currentBuf;
        } else {
            currentBuffer++;
            final int newBufSize = (bufSize >= requiredSpace) ? bufSize : requiredSpace;
            final ReusableBuffer buf = BufferPool.allocate(newBufSize);
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

    public void put(ReusableBuffer otherBuffer) {
        currentBuffer++;
        /*final ReusableBuffer vb = otherBuffer.createViewBuffer();
        vb.position(otherBuffer.limit());
        buffers.add(vb);*/
        otherBuffer.position(otherBuffer.limit());
        buffers.add(otherBuffer);
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

    @Override
    public void writeBoolean(Object key, boolean value) {
        checkAndGetBuffer(4).putInt(value ?  1 : 0);
    }

    @Override
    public void writeDouble(Object key, double value) {
        checkAndGetBuffer(8).putDouble(value);
    }

    @Override
    public void writeInt64(Object key, long value) {
        checkAndGetBuffer(8).putLong(value);
    }

    @Override
    public void writeInt32(Object key, int value) {
        checkAndGetBuffer(4).putInt(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeMap(Object key, Map value) {
        checkAndGetBuffer(4).putInt(value.size());
        inMap = true;
        value.marshal(this);
        inMap = false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeSequence(Object key, Sequence value) {
        checkAndGetBuffer(4).putInt(value.size());
        value.marshal(this);
    }

    @Override
    public void writeString(Object key, String value) {
        if (inMap)
            XDRUtils.serializeString((String)key, this);
        XDRUtils.serializeString(value, this);
    }

    @Override
    public void writeStruct(Object key, Struct value) {
        value.marshal(this);
    }

    public void writeBuffer(Object key, Object value) {
        if (value != null) {
            ReusableBuffer rb = (ReusableBuffer)value;
            XDRUtils.serializeSerializableBuffer(rb, this);
        } else {
            checkAndGetBuffer(4).putInt(0);
        }
    }







}
