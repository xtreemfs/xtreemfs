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

import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

public class BufferBackedStripingPolicy extends BufferBackedMetadata implements StripingPolicy {
    
    private static final int SIZE_INDEX         = 0;
    
    private static final int WIDTH_INDEX        = 4;
    
    private static final int DYNAMIC_PART_INDEX = 8;
    
    private ASCIIString      pattern;
    
    public BufferBackedStripingPolicy(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        super(buffer, copy, freeOnDestroy);
    }
    
    public BufferBackedStripingPolicy(String pattern, int stripeSize, int width) {
        
        super(null, false, true);
        
        buffer = BufferPool.allocate(pattern.getBytes().length + 12);
        buffer.putInt(stripeSize);
        buffer.putInt(width);
        buffer.putString(pattern);
    }
    
    public ASCIIString getPattern() {
        
        if (pattern == null) {
            
            buffer.position(DYNAMIC_PART_INDEX);
            int len = buffer.getInt() + 4;
            
            assert (len > 3);
            ReusableBuffer tmpBuf = buffer.createViewBuffer();
            tmpBuf.range(DYNAMIC_PART_INDEX, len);
            tmpBuf.position(0);
            pattern = tmpBuf.getBufferBackedASCIIString();
            BufferPool.free(tmpBuf);
        }
        
        return pattern;
    }
    
    public void setPattern(String pattern) {
        
        buffer.position(DYNAMIC_PART_INDEX);
        int strLen = buffer.getInt();
        delete(DYNAMIC_PART_INDEX, strLen + 4);
        
        ReusableBuffer newBuf = BufferPool.allocate(pattern.length() + 4);
        newBuf.putString(pattern);
        
        insert(DYNAMIC_PART_INDEX, newBuf);
        BufferPool.free(newBuf);
        
        this.pattern = null;
    }
    
    public int getStripeSize() {
        buffer.position(SIZE_INDEX);
        return buffer.getInt();
    }
    
    public void setStripeSize(int stripeSize) {
        buffer.position(SIZE_INDEX);
        buffer.putInt(stripeSize);
    }
    
    public int getWidth() {
        buffer.position(WIDTH_INDEX);
        return buffer.getInt();
    }
    
    public void setWidth(int width) {
        buffer.position(WIDTH_INDEX);
        buffer.putInt(width);
    }
    
}
