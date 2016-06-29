/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;

public class BufferBackedXLoc extends BufferBackedMetadata implements XLoc {
    
    private String[]       osdCache;
    
    private StripingPolicy stripingPolicy;
    
    public BufferBackedXLoc(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }
    
    public BufferBackedXLoc(byte[] buffer, int offset, int len) {
        
        super(buffer, offset, len);
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        short numOSDs = tmp.getShort(offset + tmp.getShort(offset));
        
        osdCache = new String[numOSDs];
    }
    
    public BufferBackedXLoc(BufferBackedStripingPolicy stripingPolicy, String[] osds, int replFlags) {
        
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
        
        int bufSize = spolSize + osdListSize + 6;
        
        // allocate a new buffer
        len = bufSize;
        buffer = new byte[len];
        ByteBuffer tmp = ByteBuffer.wrap(buffer);
        
        // first 6 bytes: osd list offset + replication flags
        tmp.putShort((short) (spolSize + 6));
        
        // next 4 bytes: replication flags
        tmp.putInt(replFlags);
        
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
            
            if (osdListStart == 6)
                return null;
            
            // create the target object from a view buffer (skip the len bytes)
            stripingPolicy = new BufferBackedStripingPolicy(buffer, offset + 6, osdListStart - 6);
        }
        
        return stripingPolicy;
    }
    
    public int getReplicationFlags() {
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        return tmp.getInt(offset + 2);
    }
    
    public void setReplicationFlags(int replFlags) {
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        tmp.putInt(offset + 2, replFlags);
    }
    
    private int getOSDBufferOffset(int osdPosition) {
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        int osdOffset = offset
            + (osdPosition >= osdCache.length ? len : tmp.getShort(offset + tmp.getShort(offset) + 2
                + osdPosition * Short.SIZE / 8));
        
        return osdOffset;
    }
    
}
