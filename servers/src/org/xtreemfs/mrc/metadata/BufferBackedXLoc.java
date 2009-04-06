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

package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;

public class BufferBackedXLoc extends BufferBackedMetadata implements XLoc {

    private String[]         osdCache;
    
    private StripingPolicy   stripingPolicy;
    
    public BufferBackedXLoc(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }
    
    public BufferBackedXLoc(byte[] buffer, int offset, int len) {
        
        super(buffer, offset, len);
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        short numOSDs = tmp.getShort(offset + tmp.getShort(offset));
        
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
        
        int bufSize = spolSize + osdListSize + 2;
        
        // allocate a new buffer
        len = bufSize;
        buffer = new byte[len];
        ByteBuffer tmp = ByteBuffer.wrap(buffer);
        
        // first 2 bytes: osd list offset
        tmp.putShort((short) (spolSize + 2));
        
        // next bytes: striping policy
        tmp.put(stripingPolicy.getBuffer(), stripingPolicy.getOffset(), stripingPolicy.getLength());
        
        // next 2 bytes: # OSDs
        tmp.putShort((short) osds.length);
        
        // next bytes: osd offsets
        byte[][] osdBytes = new byte[osds.length][];
        int ofs0 = tmp.position() - offset;
        int ofs = 0;
        for (int i = 0; i < osds.length; i++) {
            osdBytes[i] = osds[i].getBytes();
            assert (ofs0 + ofs <= Short.MAX_VALUE);
            tmp.putShort((short) (ofs0 + osds.length * Short.SIZE / 8 + ofs));
            ofs += osdBytes[i].length;
        }
        
        // next bytes: osd bytes
        for (byte[] b : osdBytes)
            tmp.put(b);
    }
    
    public short getOSDCount() {
        return (short) osdCache.length;
    }
    
    public String getOSD(int osdIndex) {
        
        if (osdCache[osdIndex] == null) {
            
            // find the correct index offset in the buffer
            int bufOffset = getOSDBufferOffset(osdIndex);
            int nextBufOffset = getOSDBufferOffset(osdIndex + 1);
            
            osdCache[osdIndex] = new String(buffer, bufOffset, nextBufOffset - bufOffset);
        }
        
        return osdCache[osdIndex];
    }
    
    public StripingPolicy getStripingPolicy() {
        
        if (stripingPolicy == null) {
            
            ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
            short osdListStart = tmp.getShort(offset);
            assert (osdListStart >= 0);
            
            if (osdListStart == 2)
                return null;
            
            // create the target object from a view buffer (skip the len bytes)
            stripingPolicy = new BufferBackedStripingPolicy(buffer, offset + 2, osdListStart - 2);
        }
        
        return stripingPolicy;
    }
    
    private int getOSDBufferOffset(int osdPosition) {
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        int osdOffset = offset
            + (osdPosition >= osdCache.length ? len : tmp.getShort(offset + tmp.getShort(offset) + 2
                + osdPosition * Short.SIZE / 8));
        
        return osdOffset;
    }
    
}
