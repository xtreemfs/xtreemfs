/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import org.xtreemfs.foundation.util.OutputUtils;

public abstract class BufferBackedMetadata {
    
    protected byte[] buffer;
    
    protected int    offset;
    
    protected int    len;
    
    /**
     * Creates a new metadata object backed by the given buffer.
     * 
     * @param buffer
     *            the backing buffer
     * @param offset
     *            the starting offset in the buffer
     * @param len
     *            the number of bytes from the starting offset
     */
    protected BufferBackedMetadata(byte[] buffer, int offset, int len) {
        this.buffer = buffer;
        this.offset = offset;
        this.len = len;
    }
    
    /**
     * Returns the backing buffer.
     * 
     * @return the backing buffer
     */
    public byte[] getBuffer() {
        return buffer;
    }
    
    /**
     * Returns the offset in the backing buffer where the data starts.
     * 
     * @return the offset in the backing buffer where the data starts
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * Returns the number of bytes from the starting offset.
     * 
     * @return the number of bytes from the starting offset
     */
    public int getLength() {
        return len;
    }
    
    /**
     * Generates a formatted hex string from the backing buffer.
     */
    public String toString() {
        return OutputUtils.byteArrayToFormattedHexString(buffer, offset, len);
    }
    
}
