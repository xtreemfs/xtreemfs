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

public class BufferBackedXLoc extends BufferBackedMetadata implements XLoc {
    
    private static final int SP_DYN_INDEX   = 0;
    
    private static final int OSDS_DYN_INDEX = 1;
    
    private ASCIIString[]    osdCache;
    
    private StripingPolicy   stripingPolicy;
    
    public BufferBackedXLoc(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        
        super(buffer, copy, freeOnDestroy);
        
        buffer.position(getBufferIndex(OSDS_DYN_INDEX));
        osdCache = new ASCIIString[buffer.getShort()];
    }
    
    public BufferBackedXLoc(BufferBackedStripingPolicy stripingPolicy, String[] osds) {
        
        super(null, false, true);
        
        assert (osds.length <= Short.MAX_VALUE);
        
        // determine required buffer size
        osdCache = new ASCIIString[osds.length];
        
        int osdListSize = 2; // 2 bytes for # OSDs
        for (String osd : osds)
            // size + 4 length bytes
            osdListSize += osd.getBytes().length + 4;
        
        int spolSize = stripingPolicy.size();
        assert (spolSize <= Short.MAX_VALUE);
        
        // + 2 len bytes for sp
        int bufSize = 2 + spolSize + osdListSize;
        
        // allocate a new buffer and fill it with the given data
        buffer = BufferPool.allocate(bufSize);
        buffer.putShort((short) spolSize);
        stripingPolicy.getBuffer().position(0);
        buffer.put(stripingPolicy.getBuffer());
        
        buffer.putShort((short) osds.length);
        for (String osd : osds)
            buffer.putString(osd);
        
    }
    
    public void destroy() {
        
        if (stripingPolicy != null)
            stripingPolicy.destroy();
        
        super.destroy();
    }
    
    public short getOSDCount() {
        buffer.position(getBufferIndex(OSDS_DYN_INDEX));
        return buffer.getShort();
    }
    
    public ASCIIString getOSD(int osdIndex) {
        
        if (osdCache[osdIndex] == null) {
            
            // find the correct index position in the buffer
            int index = getOSDBufferIndex(osdIndex);
            buffer.position(index);
            
            // total length = string length + # length bytes
            int len = buffer.getInt() + 4;
            assert (len >= 0);
            
            // create the string from a view buffer
            ReusableBuffer buf = buffer.createViewBuffer();
            buf.range(index, len);
            buf.position(0);
            osdCache[osdIndex] = buf.getBufferBackedASCIIString();
            BufferPool.free(buf);
        }
        
        return osdCache[osdIndex];
    }
    
    public StripingPolicy getStripingPolicy() {
        
        if (stripingPolicy == null) {
            
            // find the correct index position in the buffer
            int index = getBufferIndex(SP_DYN_INDEX);
            buffer.position(index);
            
            short len = buffer.getShort();
            assert (len >= 0);
            
            if (len == 0)
                return null;
            
            // create the target object from a view buffer (skip the len bytes)
            ReusableBuffer buf = buffer.createViewBuffer();
            buf.range(index + 2, len);
            buf.position(0);
            stripingPolicy = new BufferBackedStripingPolicy(buf, false, true);
        }
        
        return stripingPolicy;
        
    }
    
    private int getBufferIndex(int entityIndex) {
        
        switch (entityIndex) {
        
        case SP_DYN_INDEX:
            return 0;
            
        case OSDS_DYN_INDEX:
            buffer.position(0);
            short len = buffer.getShort();
            assert (len > 0);
            return len + 2;
            
        default:
            return -1;
        }
    }
    
    private int getOSDBufferIndex(int osdPosition) {
        
        // calculate the index; skip the first 2 bytes (# OSDs)
        int index = getBufferIndex(OSDS_DYN_INDEX) + 2;
        for (int i = 0; i < osdPosition; i++) {
            buffer.position(index);
            int len = buffer.getInt();
            assert (len > 0);
            
            index += len + 4;
        }
        
        return index;
    }
    
}
