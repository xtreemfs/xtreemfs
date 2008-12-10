/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.brain.metadata;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.util.OutputUtils;

public abstract class BufferBackedMetadata {
    
    protected ReusableBuffer buffer;
    
    protected boolean        freeOnDestroy;
    
    /**
     * Creates a new metadata object backed by the given buffer.
     * 
     * @param buffer
     *            the backing buffer
     * @param copy
     *            specifies whether a copy of the backing buffer will be created
     *            and used
     * @param freeOnDestroy
     *            specifies whether the backing buffer will be freed when
     *            <code>destroy()</code> is invoked
     */
    protected BufferBackedMetadata(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        
        this.freeOnDestroy = freeOnDestroy;
        
        if (copy) {
            buffer.position(0);
            this.buffer = BufferPool.allocate(buffer.limit());
            this.buffer.put(buffer);
        } else
            this.buffer = buffer;
    }
    
    /**
     * Returns the backing buffer.
     * 
     * @return the backing buffer
     */
    public ReusableBuffer getBuffer() {
        return buffer;
    }
    
    /**
     * Destroys the metadata object. Depending on whether
     * <code>freeOnDestroy</code> was true when creating the metadata object,
     * the backing buffer will also be freed. <br/> On <code>destroy()</code>,
     * derived classes should additionally free any buffer that has been created
     * internally.
     * 
     */
    public void destroy() {
        if (freeOnDestroy && buffer != null)
            BufferPool.free(buffer);
    }
    
    /**
     * Returns the size of the backing buffer.
     * 
     * @return the size of the backing buffer
     */
    public int size() {
        return buffer.capacity();
    }
    
    /**
     * Inserts a buffer in the backing buffer at a given object.
     * 
     * @param offset
     *            the offset
     * @param buf
     *            the buffer to insert
     */
    protected void insert(int offset, ReusableBuffer buf) {
        
        buf.position(0);
        buffer.position(0);
        
        // if the existing buffer has enough capacity, use it ...
        if (buffer.capacity() >= buffer.limit() + buf.limit()) {
            
            // create a view buffer encapsulating the trailing part of the
            // backing buffer
            ReusableBuffer tmp = buffer.createViewBuffer();
            tmp.range(offset, buffer.limit() - offset);
            tmp.position(0);
            
            // shift the limit
            buffer.limit(buffer.limit() + buf.limit());
            
            // copy the view buffer to the new end of the backing buffer
            buffer.position(offset + buf.limit());
            buffer.put(tmp);
            BufferPool.free(tmp);
            
            // insert the argument buffer at the offset
            buffer.position(offset);
            buffer.put(buf);
        }

        // otherwise, allocate and fill new sufficiently sized buffer
        else {
            
            // allocate a new sufficiently-sized buffer
            ReusableBuffer newBuf = BufferPool.allocate(buffer.limit() + buf.limit());
            
            // create a view buffer from 0 to offset
            ReusableBuffer tmp = buffer.createViewBuffer();
            tmp.range(0, offset);
            tmp.position(0);
            
            // copy the leading buffer to the new buffer
            newBuf.put(tmp);
            BufferPool.free(tmp);
            
            // insert the argument buffer
            newBuf.put(buf);
            
            // create view buffer from offset to limit
            tmp = buffer.createViewBuffer();
            tmp.range(offset, buffer.limit() - offset);
            tmp.position(0);
            
            // insert the trailing buffer
            newBuf.put(tmp);
            BufferPool.free(tmp);
            
            // replace the backing buffer w/ the new buffer, and relinquish the
            // old backing buffer's resources
            BufferPool.free(buffer);
            buffer = newBuf;
        }
    }
    
    /**
     * Deletes the given amount of bytes at the given offset in the backing
     * buffer.
     * 
     * @param offset
     *            the offset at which to delete the bytes
     * @param count
     *            the amount of bytes to delete
     */
    protected void delete(int offset, int count) {
        
        ReusableBuffer tmp = buffer.createViewBuffer();
        tmp.range(offset + count, buffer.limit() - (offset + count));
        tmp.position(0);
        
        buffer.position(offset);
        buffer.put(tmp);
        buffer.flip();
        
        BufferPool.free(tmp);
    }
    
    /**
     * Generates a formatted hex string from the backing buffer.
     */
    public String toString() {
        return OutputUtils.byteArrayToFormattedHexString(buffer.array());
    }
    
}
