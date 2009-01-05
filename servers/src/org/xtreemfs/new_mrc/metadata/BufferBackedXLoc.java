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

import java.nio.ByteBuffer;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

public class BufferBackedXLoc extends BufferBackedMetadata implements XLoc {
    
    private static final int SP_DYN_INDEX   = 0;
    
    private static final int OSDS_DYN_INDEX = 1;
    
    private String[]         osdCache;
    
    private StripingPolicy   stripingPolicy;
    
    public BufferBackedXLoc(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }
    
    public BufferBackedXLoc(byte[] buffer, int offset, int len) {
        
        super(buffer, offset, len);
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        short numOSDs = tmp.getShort(getBufferIndex(OSDS_DYN_INDEX));
        
        osdCache = new String[numOSDs];
    }
    
    public BufferBackedXLoc(BufferBackedStripingPolicy stripingPolicy, String[] osds) {
        
        super(null, 0, 0);
        
        assert (osds.length <= Short.MAX_VALUE);
        
        // determine required buffer size
        osdCache = new String[osds.length];
        
        int osdListSize = 2; // 2 bytes for # OSDs
        for (String osd : osds)
            // size + 2 length bytes
            osdListSize += osd.getBytes().length + 2;
        
        int spolSize = stripingPolicy.getLength();
        assert (spolSize <= Short.MAX_VALUE);
        
        // + 2 len bytes for sp
        int bufSize = 2 + spolSize + osdListSize;
        
        // allocate a new buffer and fill it with the given data
        ReusableBuffer tmp = BufferPool.allocate(bufSize);
        tmp.putShort((short) spolSize);
        tmp.getBuffer().put(stripingPolicy.getBuffer(), stripingPolicy.getOffset(),
            stripingPolicy.getLength());
        
        tmp.putShort((short) osds.length);
        for (String osd : osds)
            tmp.putShortString(osd);
        
        BufferPool.free(tmp);
    }
    
    public short getOSDCount() {
        return (short) osdCache.length;
    }
    
    public String getOSD(int osdIndex) {
        
        if (osdCache[osdIndex] == null) {
            
            // find the correct index position in the buffer
            int bufOffset = getOSDBufferIndex(osdIndex);
            
            ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
            
            // total length = string length + # length bytes
            short strLen = tmp.getShort(bufOffset);
            assert (strLen >= 0);
            
            osdCache[osdIndex] = new String(buffer, offset + bufOffset + Short.SIZE / 8, strLen);
        }
        
        return osdCache[osdIndex];
    }
    
    public StripingPolicy getStripingPolicy() {
        
        if (stripingPolicy == null) {
            
            // find the correct index position in the buffer
            int bufOffset = getBufferIndex(SP_DYN_INDEX);
            
            ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
            short bufLen = tmp.getShort(bufOffset);
            assert (bufLen >= 0);
            
            if (bufLen == 0)
                return null;
            
            // create the target object from a view buffer (skip the len bytes)
            stripingPolicy = new BufferBackedStripingPolicy(buffer, offset + bufOffset, bufLen);
        }
        
        return stripingPolicy;
    }
    
    private int getBufferIndex(int entityIndex) {
        
        switch (entityIndex) {
        
        case SP_DYN_INDEX:
            return 0;
            
        case OSDS_DYN_INDEX:
            ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
            short bufLen = tmp.getShort(0);
            assert (bufLen > 0);
            return bufLen + 2;
            
        default:
            return -1;
        }
    }
    
    private int getOSDBufferIndex(int osdPosition) {
        
        // calculate the index; skip the first 2 bytes (# OSDs)
        int index = getBufferIndex(OSDS_DYN_INDEX) + 2;
        for (int i = 0; i < osdPosition; i++) {
            ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
            int bufLen = tmp.getShort(index);
            assert (bufLen > 0);
            
            index += bufLen + 2;
        }
        
        return index;
    }
    
}
