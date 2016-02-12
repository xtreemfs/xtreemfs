/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * 
 * @author bjko
 */
public final class ReusableBuffer {
    
    private static final Charset ENC_UTF8 = Charset.forName("utf-8");
    
    /**
     * A view buffer of parentBuffer with the requested size. For non-reusable
     * buffers this is the buffer itself
     */
    private ByteBuffer           buffer;
    
    /**
     * A parent buffer which is returned to the pool
     */
    private final ByteBuffer     parentBuffer;
    
    /**
     * True if the buffer can be returned to the pool
     */
    private final boolean        reusable;
    
    /**
     * set to true after a buffer was returned to the pool
     */
    protected volatile boolean   returned;
    
    /**
     * size (as requested), might be smaller than parentBuffer size but is
     * always equal to the (view) buffer size.
     */
    private int                  size;
    
    protected ReusableBuffer     viewParent;
    
    protected String             freeStack, allocStack;
    
    /**
     * reference count
     */
    AtomicInteger                refCount;
    
    /**
     * Creates a new instance of ReusableBuffer. A view buffer of size is created.
     * 
     * @param buffer
     *            the parent buffer
     * @param size
     *            the requested size
     */
    protected ReusableBuffer(ByteBuffer buffer, int size) {
        buffer.position(0);
        buffer.limit(size);
        this.buffer = buffer.slice();
        this.parentBuffer = buffer;
        this.size = size;
        this.reusable = true;
        this.refCount = new AtomicInteger(1);
        returned = false;
        viewParent = null;
    }
    
    /**
     * A wrapper for a non-reusable buffer. The buffer is not used by the pool
     * when returned.
     */
    public ReusableBuffer(ByteBuffer nonManaged) {
        this.buffer = nonManaged;
        this.size = buffer.limit();
        this.reusable = false;
        this.parentBuffer = null;
        returned = false;
        this.refCount = new AtomicInteger(1);
        viewParent = null;
    }
    
    /**
     * Creates a non-reusable buffer around a byte array. Uses the
     * ByteBuffer.wrap method.
     * 
     * @param data
     *            the byte array containing the data
     * @return
     */
    public static ReusableBuffer wrap(byte[] data) {
        return new ReusableBuffer(ByteBuffer.wrap(data));
    }
    
    public static ReusableBuffer wrap(byte[] data, int offset, int length) {
        assert (offset >= 0);
        assert (length >= 0);
        if (offset + length > data.length)
            throw new IllegalArgumentException("offset+length > buffer size (" + offset + "+" + length
                + " > " + data.length);
        ByteBuffer tmp = ByteBuffer.wrap(data);
        tmp.position(offset);
        tmp.limit(offset + length);
        return new ReusableBuffer(tmp.slice());
    }
    
    /**
     * Creates a new view buffer. This view buffer shares the same data (i.e.
     * backing byte buffer) but has independent position, limit etc.
     */
    public ReusableBuffer createViewBuffer() {
        
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        
        if (this.viewParent == null) {
            
            if (parentBuffer == null) {
                // wrapped buffers
                ReusableBuffer view = new ReusableBuffer(this.buffer.slice());
                view.viewParent = this;
                
                return view;
                
            } else {
                // regular buffer
                ReusableBuffer view = new ReusableBuffer(this.parentBuffer, this.size);
                view.viewParent = this;
                this.refCount.incrementAndGet();
                
                if (BufferPool.recordStackTraces) {
                    view.allocStack = "\n";
                    StackTraceElement[] stackTrace = new Exception().getStackTrace();
                    for (int i = 0; i < stackTrace.length; i++)
                        view.allocStack += stackTrace[i].toString() + (i < stackTrace.length - 1 ? "\n" : "");
                }
                
                return view;
            }
            
        } else {
            
            if (parentBuffer == null) {
                // wrapped buffers
                ReusableBuffer view = new ReusableBuffer(this.buffer.slice());
                view.viewParent = this.viewParent;
                
                return view;
                
            } else {
                // regular buffer: use the parent to create a view buffer
                ReusableBuffer view = new ReusableBuffer(this.buffer, this.size);
                view.viewParent = this.viewParent;
                this.viewParent.refCount.incrementAndGet();
                
                if (BufferPool.recordStackTraces) {
                    view.allocStack = "\n";
                    StackTraceElement[] stackTrace = new Exception().getStackTrace();
                    for (int i = 0; i < stackTrace.length; i++)
                        view.allocStack += stackTrace[i].toString() + (i < stackTrace.length - 1 ? "\n" : "");
                }
                
                return view;
            }
        }
    }
    
    /**
     * @see java.nio.Buffer#capacity
     */
    public int capacity() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return this.size;
    }
    
    /**
     * May be higher than {@link #capacity()} if the parent buffer is from the {@link BufferPool} which may
     * have returned a larger buffer.
     */
    public int capacityUnderlying() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return parentBuffer != null ? parentBuffer.capacity() : this.size;
    }

    /**
     * @see java.nio.ByteBuffer#hasArray
     */
    public boolean hasArray() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.hasArray();
    }
    
    /**
     * Returns the byte array of the buffer, creating a copy if the buffer is
     * not backed by an array
     * 
     * @return a byte array with a copy of the data
     */
    public byte[] array() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        byte[] array = null;
        
        if (this.hasArray() && (this.viewParent == null)) {
            array = buffer.array();
        } else {
            array = new byte[this.limit()];
            final int oldPos = this.position();
            this.position(0);
            this.get(array);
            this.position(oldPos);
        }
        
        return array;
    }
    
    /**
     * @see java.nio.Buffer#flip
     */
    public void flip() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.flip();
    }
    
    /**
     * @see java.nio.ByteBuffer#compact
     */
    public void compact() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.compact();
    }
    
    /**
     * @see java.nio.Buffer#limit(int)
     */
    public void limit(int l) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.limit(l);
    }
    
    /**
     * @see java.nio.Buffer#limit()
     */
    public int limit() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.limit();
    }
    
    /**
     * @see java.nio.Buffer#position(int)
     */
    public void position(int p) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.position(p);
    }
    
    /**
     * @see java.nio.Buffer#position()
     */
    public int position() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.position();
    }
    
    /**
     * @see java.nio.Buffer#hasRemaining
     */
    public boolean hasRemaining() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.hasRemaining();
    }
    
    /**
     * Returns the view buffer encapsulated by this ReusableBuffer.
     * 
     * @return the view buffer
     */
    public ByteBuffer getBuffer() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return this.buffer;
    }
    
    /**
     * Returns true, if this buffer is re-usable and can be returned to the
     * pool.
     * 
     * @return true, if this buffer is re-usable
     */
    public boolean isReusable() {
        // assert(!returned) :
        // "Buffer was already freed and cannot be used anymore"+this.freeStack;
        return this.reusable;
    }
    
    /**
     * Returns the parent buffer.
     * 
     * @return the parent buffer
     */
    protected ByteBuffer getParent() {
        return this.parentBuffer;
    }
    
    /**
     * @see java.nio.ByteBuffer#get()
     */
    public byte get() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.get();
    }
    
    public byte get(int index) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.get(index);
    }
    
    /**
     * @see java.nio.ByteBuffer#get(byte[])
     */
    public ReusableBuffer get(byte[] dst) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.get(dst);
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#get(byte[], int offset, int length)
     */
    public ReusableBuffer get(byte[] dst, int offset, int length) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.get(dst, offset, length);
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#put(byte)
     */
    public ReusableBuffer put(byte b) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.put(b);
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#put(byte[])
     */
    public ReusableBuffer put(byte[] src) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.put(src);
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#put(byte[],int,int)
     */
    public ReusableBuffer put(byte[] src, int offset, int len) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.put(src, offset, len);
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#put(ByteBuffer)
     */
    public ReusableBuffer put(ByteBuffer src) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.put(src);
        return this;
    }
    
    /**
     * Writes the content of src into this buffer.
     * 
     * @param src
     *            the buffer to read from
     * @return this ReusableBuffer after reading
     * @see java.nio.ByteBuffer#put(ByteBuffer)
     */
    public ReusableBuffer put(ReusableBuffer src) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.put(src.buffer);
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#getInt
     */
    public int getInt() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.getInt();
    }
    
    /**
     * @see java.nio.ByteBuffer#putInt(int)
     */
    public ReusableBuffer putInt(int i) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.putInt(i);
        return this;
    }
    
    public long getLong() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.getLong();
    }
    
    public ReusableBuffer putLong(long l) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.putLong(l);
        return this;
    }
    
    public double getDouble() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.getDouble();
    }
    
    public ReusableBuffer putDouble(double d) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.putDouble(d);
        return this;
    }
    
    public String getString() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        int length = buffer.getInt();
        if (length > 0) {
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes, ENC_UTF8);
        } else if (length == 0) {
            return "";
        } else {
            return null;
        }
    }
    
    public ReusableBuffer putString(String str) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        if (str != null) {
            byte[] bytes = str.getBytes(ENC_UTF8);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        } else {
            buffer.putInt(-1);
        }
        return this;
    }
    
    public ReusableBuffer putShortString(String str) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        assert (str.length() <= Short.MAX_VALUE);
        if (str != null) {
            byte[] bytes = str.getBytes(ENC_UTF8);
            buffer.putShort((short) bytes.length);
            buffer.put(bytes);
        } else {
            buffer.putInt(-1);
        }
        return this;
    }
    
    public ASCIIString getBufferBackedASCIIString() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return ASCIIString.unmarshall(this);
    }
    
    public ReusableBuffer putBufferBackedASCIIString(ASCIIString str) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        if (str != null) {
            str.marshall(this);
        } else {
            buffer.putInt(-1);
        }
        return this;
    }
    
    public ReusableBuffer putShort(short s) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.putShort(s);
        return this;
    }
    
    public short getShort() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.getShort();
    }
    
    /**
     * @see java.nio.ByteBuffer#isDirect
     */
    public boolean isDirect() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.isDirect();
    }
    
    /**
     * @see java.nio.Buffer#remaining
     */
    public int remaining() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.remaining();
    }
    
    /**
     * @see java.nio.Buffer#clear
     */
    public void clear() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.clear();
    }
    
    public byte[] getData() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        byte[] array = new byte[this.limit()];
        this.position(0);
        this.get(array);
        return array;
    }
    
    public void shrink(int newSize) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        if (newSize > size) {
            throw new IllegalArgumentException("new size must not be larger than old size");
        }
        this.size = newSize;
        int oldPos = buffer.position();
        if (oldPos > newSize)
            oldPos = 0;
        
        // save parent position and limit
        ByteBuffer originalBuffer;
        if (parentBuffer != null) {
            originalBuffer = parentBuffer;
        } else {
            originalBuffer = buffer;
        }
        int position = originalBuffer.position();
        int limit = originalBuffer.limit();
        
        originalBuffer.position(0);
        originalBuffer.limit(newSize);
        this.buffer = originalBuffer.slice();
        buffer.position(oldPos);
        
        // restore parent position and limit
        originalBuffer.position(position);
        originalBuffer.limit(limit);
    }
    
    /*
     * Increases the capacity of this buffer. Returns false if {@code newSize} is bigger than the capacity of
     * the underlying buffer.
     * 
     * The underlying buffer can be a reusable buffer (parentBuffer != 0) or non-reusable buffer (viewParent
     * != 0).
     */
    public boolean enlarge(int newSize) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        if (newSize == this.size) {
            return true;
        }
        if (parentBuffer == null && viewParent == null) {
            return false;
        }
        ByteBuffer underlyingBuffer = parentBuffer != null ? parentBuffer : viewParent.getBuffer();
        if (newSize > underlyingBuffer.capacity()) {
            return false;
        } else {
            this.size = newSize;
            int oldPos = buffer.position();
            if (oldPos > newSize)
                oldPos = 0;
            
            // save parent position and limit
            int position = underlyingBuffer.position();
            int limit = underlyingBuffer.limit();
            
            underlyingBuffer.position(0);
            underlyingBuffer.limit(newSize);
            this.buffer = underlyingBuffer.slice();
            buffer.position(oldPos);
            
            // restore parent position and limit
            underlyingBuffer.position(position);
            underlyingBuffer.limit(limit);
            
            return true;
        }
    }
    
    /*
     * Sets the new range of the buffer starting from {@code offset} going for {@code length} bytes.
     * 
     * The new position of the buffer will be 0 and the size/capacity will equal {@code length}.
     */
    public void range(int offset, int length) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        
        // useless call!
        if ((offset == 0) && (length == this.size))
            return;
        
        if (offset >= size) {
            throw new IllegalArgumentException("offset must be < size. offset=" + offset + " size=" + size);
        }
        if (offset + length > size) {
            throw new IllegalArgumentException("offset+length must be <= size. size=" + size + " offset="
                + offset + " length=" + length);
        }
        
        this.size = length;
        
        // save parent position and limit
        ByteBuffer originalBuffer;
        if (parentBuffer != null) {
            originalBuffer = parentBuffer;
        } else {
            originalBuffer = buffer;
        }
        int position = originalBuffer.position();
        int limit = originalBuffer.limit();

        // ensure that the subsequent 'position' does not fail
        if (offset > limit)
            originalBuffer.limit(offset);

        originalBuffer.position(offset);
        originalBuffer.limit(offset + length);
        this.buffer = originalBuffer.slice();
        assert (this.buffer.capacity() == length);

        // restore parent position and limit
        originalBuffer.position(position);
        originalBuffer.limit(limit);
    }
    
    public ReusableBuffer putBoolean(boolean bool) {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        buffer.put(bool ? (byte) 1 : (byte) 0);
        return this;
    }
    
    public boolean getBoolean() {
        assert (!returned) : "Buffer was already freed and cannot be used anymore" + this.freeStack;
        return buffer.get() == 1;
    }
    
    public int getRefCount() {
        if (this.viewParent == null) {
            return this.refCount.get();
        } else {
            return this.viewParent.refCount.get();
        }
    }
    
    @Override
    protected void finalize() {
        
        if (!returned && reusable) {
            
            Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this,
                "buffer was finalized but not freed before! buffer = %s, refCount=%d", this.toString(), getRefCount());
            
            if (allocStack != null) {
                
                Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this, "stacktrace: %s", allocStack);
                if (this.viewParent != null)
                    Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this, "parent stacktrace: %s",
                            viewParent.allocStack);
            }
            
            if (freeStack != null) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this, "freed at: %s", freeStack);
            } else if (viewParent != null && viewParent.freeStack != null) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this, "freed at: %s", viewParent.freeStack);
            }
            
            if (Logging.isDebug()) {
                
                byte[] data = new byte[(this.capacity() > 128) ? 128 : this.capacity()];
                this.position(0);
                this.limit(this.capacity());
                this.get(data);
                String content = new String(data);
                
                Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this, "content: %s", content);
                
                if (this.viewParent != null) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this, "view parent: %s",
                        this.viewParent.toString());
                    Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this, "ref count: %d",
                        this.viewParent.refCount.get());
                } else {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.buffer, this, "ref count: %d",
                        this.refCount.get());
                }
                
            }
            
            BufferPool.free(this);
            
        }
        
    }
    
    @Override
    public String toString() {
        return "ReusableBuffer( capacity=" + this.capacity() + " limit=" + this.limit() + " position="
            + this.position() + ")";
    }
    
}
