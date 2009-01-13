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

package org.xtreemfs.new_mrc.metadata;

import org.xtreemfs.common.util.OutputUtils;

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
