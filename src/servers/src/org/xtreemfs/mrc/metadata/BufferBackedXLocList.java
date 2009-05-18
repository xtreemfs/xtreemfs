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
import java.util.Iterator;

public class BufferBackedXLocList extends BufferBackedMetadata implements XLocList {
    
    private XLoc[] replicaCache;
    
    public BufferBackedXLocList(byte[] buffer, int offset, int length) {
        
        super(buffer, offset, length);
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        int numReplicas = tmp.getInt(offset + 4);
        
        replicaCache = new XLoc[numReplicas];
    }
    
    public BufferBackedXLocList(BufferBackedXLoc[] replicas, String replUpdatePolicy, int version) {
        
        super(null, 0, 0);
        
        byte[] replUpdatePolicyBytes = replUpdatePolicy.getBytes();
        
        // determine required buffer size
        replicaCache = new XLoc[replicas.length];
        int bufSize = 12; // 1st & 2nd & 3rd 4 bytes: version + #replicas +
        // update pol offs
        for (BufferBackedXLoc repl : replicas)
            // size + 4 length bytes
            bufSize += repl.len + 4;
        
        // add the length of the update policy string
        bufSize += replUpdatePolicyBytes.length;
        
        // allocate a new buffer and fill it with the given data
        buffer = new byte[bufSize];
        ByteBuffer tmp = ByteBuffer.wrap(buffer);
        
        // version
        tmp.putInt(version);
        
        // # replicas
        tmp.putInt(replicas.length);
        
        // jump 4 bytes ahead, leave the space for the update policy offset
        tmp.position(tmp.position() + 4);
        
        // replica offsets
        int replOffset = tmp.position() - offset + replicas.length * Integer.SIZE / 8;
        for (BufferBackedXLoc replica : replicas) {
            tmp.putInt(replOffset);
            replOffset += replica.getLength();
        }
        
        // replicas
        for (BufferBackedXLoc replica : replicas)
            tmp.put(replica.getBuffer(), replica.getOffset(), replica.getLength());
        
        // insert the repl update policy offset
        int offs = tmp.position();
        tmp.position(8);
        tmp.putInt(replOffset);
        
        tmp.position(offs);
        tmp.put(replUpdatePolicyBytes);
        
        offset = 0;
        len = buffer.length;
    }
    
    public int getReplicaCount() {
        return replicaCache.length;
    }
    
    public XLoc getReplica(int replicaIndex) {
        
        if (replicaCache[replicaIndex] == null) {
            
            // find the correct offset position in the buffer
            int index = getBufferOffset(replicaIndex);
            
            // find the following offset position
            int nextIndex = replicaIndex >= getReplicaCount() - 1 ? getReplUpdatePolicyOffset()
                : getBufferOffset(replicaIndex + 1);
            
            int length = nextIndex - index;
            if (length == 0)
                return null;
            
            replicaCache[replicaIndex] = new BufferBackedXLoc(buffer, index, length);
        }
        
        return replicaCache[replicaIndex];
    }
    
    public String getReplUpdatePolicy() {
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        int offs = tmp.getInt(offset + 8);
        
        return new String(buffer, offset + offs, len - offs);
    }
    
    public int getVersion() {
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        return tmp.getInt(offset);
    }
    
    public Iterator<XLoc> iterator() {
        
        return new Iterator<XLoc>() {
            
            private int index = 0;
            
            public boolean hasNext() {
                return index < getReplicaCount();
            }
            
            public XLoc next() {
                return getReplica(index++);
            }
            
            public void remove() {
                throw new UnsupportedOperationException("remove not implemented");
            }
        };
    }
    
    private int getBufferOffset(int replicaIndex) {
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        return offset + tmp.getInt(offset + 12 + replicaIndex * Integer.SIZE / 8);
    }
    
    private int getReplUpdatePolicyOffset() {
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        return offset + tmp.getInt(offset + 8);
    }
    
}
